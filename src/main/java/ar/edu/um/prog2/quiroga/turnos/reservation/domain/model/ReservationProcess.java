package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Proceso de reserva de un usuario (enunciado §4.2: "iniciar y conservar el estado local de procesos de reserva").
 * Cada cambio de estado pasa por ReservationStatus.canMoveTo: si no está permitido, es un error de programación.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationProcess {

    /** Un pedido sin respuesta de la cátedra por más que esto (por ejemplo, la app se cayó en el medio) se da por fallido. */
    static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);
    static final String ABANDONED = "ABANDONED";
    /** La cátedra dio el proceso por fallido sin avisar por Kafka (lo trajo la reconciliación). */
    static final String CATEDRA_FAILED = "CATEDRA_FAILED";
    // Largo de las columnas: un texto de la cátedra más largo se corta en lugar de fallar al guardar.
    static final int MAX_FAILURE_CODE = 60;
    static final int MAX_MESSAGE = 500;

    private UUID id;
    private Long userId;
    private String externalPatientId;
    private Long professionalId;
    private LocalDate date;
    private LocalTime startTime;
    private ReservationStatus status;
    private String holdId;
    private String catedraProcessId;
    private Instant expiresAt;
    private String failureCode;
    private String phoneRequestEventId;
    private String phoneRequestMessage;
    /** El mensaje del último teléfono rechazado (anexo §15.7), para mostrárselo al usuario. */
    private String phoneRejectionMessage;
    /** El id de la reserva en la cátedra, cuando se confirma: sirve para cancelarla (anexo §12). */
    private Long catedraReservationId;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    /** Nuevo proceso, todavía sin hold: se guarda antes de llamar a la cátedra (anexo §18.4). */
    public static ReservationProcess request(Patient patient, ReservationRequest request, Instant now) {
        return ReservationProcess.builder()
                .id(UUID.randomUUID())
                .userId(patient.getUserId())
                .externalPatientId(patient.externalId())
                .professionalId(request.getProfessionalId())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .status(ReservationStatus.REQUESTED)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void hold(Hold hold, Instant now) {
        moveTo(ReservationStatus.HELD, now);
        holdId = hold.getHoldId();
        catedraProcessId = hold.getCatedraProcessId();
        expiresAt = hold.getExpiresAt();
    }

    public void waitForPhone(Instant newExpiresAt, Instant now) {
        moveTo(ReservationStatus.WAITING_FOR_PHONE, now);
        if (newExpiresAt != null) {
            expiresAt = newExpiresAt;
        }
    }

    public void fail(String code, Instant now) {
        moveTo(ReservationStatus.FAILED, now);
        failureCode = code;
    }

    public void expire(Instant now) {
        moveTo(ReservationStatus.EXPIRED, now);
    }

    /**
     * La cátedra pidió el teléfono: se guarda el pedido y, si había un teléfono enviado (rechazado o pendiente de
     * resultado), el proceso vuelve a esperarlo. El vencimiento lo marca la cátedra.
     */
    public void receivePhoneRequest(PhoneRequest request, Instant now) {
        if (status != ReservationStatus.WAITING_FOR_PHONE) {
            moveTo(ReservationStatus.WAITING_FOR_PHONE, now);
        }
        phoneRequestEventId = request.getEventId();
        phoneRequestMessage = request.getMessage();
        if (request.getExpiresAt() != null) {
            expiresAt = request.getExpiresAt();
        }
        updatedAt = now;
    }

    /** El usuario respondió el pedido de teléfono: el proceso espera el resultado de la cátedra. */
    public void submitPhone(Instant now) {
        if (!isPhoneRequested()) {
            throw new IllegalStateException("El proceso " + id + " no tiene un pedido de teléfono para responder");
        }
        moveTo(ReservationStatus.PHONE_SUBMITTED, now);
        phoneRejectionMessage = null;
    }

    /**
     * Reconciliación (anexo §18.4): lleva el proceso al estado que informa la cátedra, si la máquina de estados lo
     * permite. Devuelve true si cambió. Un estado final no retrocede y un estado igual no cambia nada.
     */
    public boolean reconcileWith(CatedraReservation catedra, Instant now) {
        if (catedra.getStatus() == null) {
            return false;
        }
        ReservationStatus target = catedra.getStatus().getLocalStatus();
        if (target == status || !status.canMoveTo(target)) {
            return false;
        }
        moveTo(target, now);
        if (target == ReservationStatus.CONFIRMED) {
            catedraReservationId = catedra.getCatedraReservationId();
        } else if (target == ReservationStatus.FAILED) {
            failureCode = CATEDRA_FAILED;
        }
        return true;
    }

    /** Solo una reserva confirmada se puede cancelar (enunciado §5 punto 14; anexo §12). */
    public boolean isCancellable() {
        return status == ReservationStatus.CONFIRMED;
    }

    public void cancel(Instant now) {
        moveTo(ReservationStatus.CANCELLED, now);
    }

    /** El resultado se puede aplicar si lleva a un estado permitido desde el actual (un evento tardío no). */
    public boolean canApply(ReservationResult result) {
        return status.canMoveTo(result.getType().getTargetStatus());
    }

    /** Aplica un resultado de la cátedra. Antes hay que preguntar canApply: una transición no permitida es un error. */
    public void applyResult(ReservationResult result, Instant now) {
        moveTo(result.getType().getTargetStatus(), now);
        switch (result.getType()) {
            case CONFIRMED -> catedraReservationId = result.getCatedraReservationId();
            case PHONE_REJECTED -> rejectPhone(result);
            case EXPIRED, INVALID -> failureCode = cut(result.getReason(), MAX_FAILURE_CODE);
            case CANCELLED -> {
                // Solo cambia el estado.
            }
        }
    }

    // El proceso vuelve a esperar el teléfono, con el mismo pedido de la cátedra: se responde otra vez con un eventId nuevo.
    private void rejectPhone(ReservationResult result) {
        phoneRejectionMessage = cut(result.getMessage(), MAX_MESSAGE);
        if (result.getExpiresAt() != null) {
            expiresAt = result.getExpiresAt();
        }
    }

    private static String cut(String text, int maxLength) {
        return text == null || text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    /** Todavía no guardamos la confirmación del hold: un evento de la cátedra puede llegar antes que su respuesta. */
    public boolean isConfirmationInFlight() {
        return status == ReservationStatus.REQUESTED || status == ReservationStatus.HELD;
    }

    /** La app tiene que pedirle el teléfono al usuario. */
    public boolean isPhoneRequested() {
        return status == ReservationStatus.WAITING_FOR_PHONE && phoneRequestEventId != null;
    }

    public boolean isPending() {
        return status.isPending();
    }

    public boolean belongsTo(Long otherUserId) {
        return userId.equals(otherUserId);
    }

    /**
     * Un pendiente que ya no puede seguir: su hold venció o el pedido quedó sin respuesta. Se cierra para que el
     * usuario pueda empezar otro (el lugar del índice único se libera).
     */
    public boolean isAbandoned(Instant now) {
        if (expiresAt != null) {
            return !now.isBefore(expiresAt);
        }
        return status == ReservationStatus.REQUESTED && !now.isBefore(createdAt.plus(REQUEST_TIMEOUT));
    }

    public void close(Instant now) {
        if (status == ReservationStatus.REQUESTED) {
            fail(ABANDONED, now);
        } else {
            expire(now);
        }
    }

    private void moveTo(ReservationStatus next, Instant now) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("El proceso " + id + " no puede pasar de " + status + " a " + next);
        }
        status = next;
        updatedAt = now;
    }
}
