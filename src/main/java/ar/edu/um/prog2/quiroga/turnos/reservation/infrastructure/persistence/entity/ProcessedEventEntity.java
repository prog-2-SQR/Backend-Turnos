package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Eventos de la cátedra ya procesados. El eventId es la clave primaria: no puede haber dos filas del mismo evento. */
@Entity
@Table(name = "reservation_processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEventEntity {

    @Id
    private String eventId;
    @Column(nullable = false, length = 60)
    private String eventType;
    @Column(nullable = false)
    private Instant processedAt;
}
