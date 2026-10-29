package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Estados del proceso de reserva (enunciado §7, anexo §9, §10 y §15). Cada estado dice a cuáles puede pasar:
 * un estado final no cambia más y CONFIRMED solo puede cancelarse, así un evento tardío nunca hace retroceder
 * un proceso (anexo §18.3).
 */
public enum ReservationStatus {
    /** Se registró el pedido y todavía no hay respuesta de la cátedra. */
    REQUESTED,
    /** La cátedra bloqueó el turno (hold) hasta expiresAt. */
    HELD,
    /** El hold se confirmó: la cátedra va a pedir el teléfono por Kafka. */
    WAITING_FOR_PHONE,
    /** Se envió el teléfono y se espera el resultado. */
    PHONE_SUBMITTED,
    CONFIRMED,
    /** No se pudo crear o confirmar el hold. */
    FAILED,
    EXPIRED,
    INVALID,
    CANCELLED;

    private static final Set<ReservationStatus> PENDING = EnumSet.of(REQUESTED, HELD, WAITING_FOR_PHONE, PHONE_SUBMITTED);
    private static final Map<ReservationStatus, Set<ReservationStatus>> NEXT = new EnumMap<>(ReservationStatus.class);

    static {
        NEXT.put(REQUESTED, EnumSet.of(HELD, FAILED));
        NEXT.put(HELD, EnumSet.of(WAITING_FOR_PHONE, FAILED, EXPIRED));
        NEXT.put(WAITING_FOR_PHONE, EnumSet.of(PHONE_SUBMITTED, CONFIRMED, EXPIRED, INVALID));
        // Un teléfono rechazado vuelve a WAITING_FOR_PHONE: se puede reintentar hasta expiresAt (anexo §15.7).
        NEXT.put(PHONE_SUBMITTED, EnumSet.of(WAITING_FOR_PHONE, CONFIRMED, EXPIRED, INVALID));
        NEXT.put(CONFIRMED, EnumSet.of(CANCELLED));
        for (ReservationStatus finalStatus : EnumSet.of(FAILED, EXPIRED, INVALID, CANCELLED)) {
            NEXT.put(finalStatus, EnumSet.noneOf(ReservationStatus.class));
        }
    }

    public static Set<ReservationStatus> pendingStatuses() {
        return EnumSet.copyOf(PENDING);
    }

    /** Pendiente: el proceso todavía puede terminar de cualquier forma. Un usuario tiene como mucho uno. */
    public boolean isPending() {
        return PENDING.contains(this);
    }

    public boolean canMoveTo(ReservationStatus next) {
        return NEXT.get(this).contains(next);
    }
}
