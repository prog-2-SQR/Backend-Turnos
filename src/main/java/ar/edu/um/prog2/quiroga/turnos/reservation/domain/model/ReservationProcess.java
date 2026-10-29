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
