package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * El usuario se referencia por ID (sin clave foránea a jhi_user, que es de otro slice). @Version: si dos cambios
 * llegan a la vez (REST y Kafka), el segundo falla en lugar de pisar al primero.
 */
@Entity
@Table(name = "reservation_process")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationProcessEntity {

    @Id
    private UUID id;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false, length = 100)
    private String externalPatientId;
    @Column(nullable = false)
    private Long professionalId;
    @Column(name = "appointment_date", nullable = false)
    private LocalDate date;
    @Column(nullable = false)
    private LocalTime startTime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;
    @Column(length = 100)
    private String holdId;
    @Column(length = 100)
    private String catedraProcessId;
    private Instant expiresAt;
    @Column(length = 60)
    private String failureCode;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;
}
