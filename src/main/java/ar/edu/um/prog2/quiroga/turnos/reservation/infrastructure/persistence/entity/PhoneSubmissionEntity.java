package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fila del outbox: un teléfono para enviar a la cátedra. El eventId es la clave y no cambia entre reintentos. */
@Entity
@Table(name = "reservation_phone_outbox")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneSubmissionEntity {

    @Id
    @Column(length = 100)
    private String eventId;

    @Column(nullable = false)
    private UUID reservationProcessId;

    @Column(nullable = false, length = 100)
    private String catedraProcessId;

    @Column(nullable = false, length = 100)
    private String holdId;

    @Column(nullable = false, length = 100)
    private String requestEventId;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    @Column(nullable = false)
    private int attempts;
}
