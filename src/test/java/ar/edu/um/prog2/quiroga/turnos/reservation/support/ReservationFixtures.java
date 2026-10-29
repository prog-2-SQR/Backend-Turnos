package ar.edu.um.prog2.quiroga.turnos.reservation.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Hold;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Patient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationRequest;

/** Juan (usuario 42) pide el turno del lunes 2/11 a las 12:30 con Ana Torres (201), como en el anexo §9. */
public final class ReservationFixtures {

    public static final Instant NOW = Instant.parse("2026-10-29T15:00:00Z");
    public static final Instant HOLD_EXPIRES_AT = NOW.plusSeconds(600);
    public static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    public static final LocalTime TWELVE_THIRTY = LocalTime.of(12, 30);

    private ReservationFixtures() {
    }

    public static Patient juan() {
        return Patient.builder().userId(42L).firstName("Juan").lastName("Perez").build();
    }

    public static ReservationRequest mondayAtTwelveThirty() {
        return ReservationRequest.builder().professionalId(201L).date(MONDAY).startTime(TWELVE_THIRTY).build();
    }

    public static Hold hold() {
        return Hold.builder().holdId("hold-6c0c2f41").catedraProcessId("process-a92b47d0").expiresAt(HOLD_EXPIRES_AT).build();
    }

    public static ReservationProcess requested() {
        return ReservationProcess.request(juan(), mondayAtTwelveThirty(), NOW);
    }

    public static ReservationProcess waitingForPhone() {
        ReservationProcess process = held();
        process.waitForPhone(null, NOW);
        return process;
    }

    // Ejemplo del anexo §15.4.
    public static PhoneRequest phoneRequest() {
        return PhoneRequest.builder()
                .eventId("request-event-8a31")
                .catedraProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .requiredField("PHONE_NUMBER")
                .message("Ingrese un numero de telefono para continuar.")
                .expiresAt(HOLD_EXPIRES_AT.plusSeconds(300))
                .build();
    }

    public static ReservationProcess held() {
        ReservationProcess process = requested();
        process.hold(hold(), NOW);
        return process;
    }
}
