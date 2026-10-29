package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out;

/** Eventos de Kafka ya procesados: la entrega es al menos una vez y el eventId es la clave de idempotencia (anexo §15.1). */
public interface ProcessedEventRepository {

    boolean existsByEventId(String eventId);

    void saveProcessedEvent(String eventId, String eventType);
}
