package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.service.ReservationService;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationResultType;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.dto.ReservationActionMessage;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.mapper.ReservationActionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escucha el topic de acciones de reserva de la cátedra (anexo §15.2). Va en la infraestructura del slice fachada del
 * caso de uso, como indicó el profe. Cada eventType tiene su manejador: el pedido de teléfono y los cinco resultados del
 * proceso. Un eventType desconocido se registra en el log. Un mensaje que no se puede leer se descarta, porque
 * reintentarlo no lo arregla.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationActionsListener {

    static final String LISTENER_ID = "reservationActionsListener";
    static final String PHONE_REQUESTED = "AdditionalInformationRequested";
    // Resultados del proceso (anexo §15.6 a §15.10): el eventType dice de qué tipo es.
    static final Map<String, ReservationResultType> RESULT_TYPES = Map.of(
            "AppointmentConfirmed", ReservationResultType.CONFIRMED,
            "AdditionalInformationRejected", ReservationResultType.PHONE_REJECTED,
            "AppointmentProcessExpired", ReservationResultType.EXPIRED,
            "AppointmentProcessInvalid", ReservationResultType.INVALID,
            "AppointmentCancelled", ReservationResultType.CANCELLED);
    private static final int MAX_LOGGED_LENGTH = 200;

    private final ReservationService reservationService;
    private final JsonMapper jsonMapper;
    private final ReservationActionMapper reservationActionMapper;

    // Un manejador por eventType. Las referencias a métodos leen los campos recién cuando se las llama.
    private final Map<String, Consumer<ReservationActionMessage>> handlers = handlers();

    // idIsGroup = false: el consumer group es spring.kafka.consumer.group-id, no el id del listener.
    @KafkaListener(id = LISTENER_ID, idIsGroup = false, topics = "${catedra.appointment-actions-topic}")
    public void onAction(String payload) {
        Optional<ReservationActionMessage> message = parse(payload).filter(ReservationActionsListener::isIdentified);
        if (message.isEmpty()) {
            log.warn("Mensaje descartado del topic de acciones: {}", abbreviate(payload));
            return;
        }
        handlers.getOrDefault(message.get().getEventType(), ReservationActionsListener::notHandled).accept(message.get());
    }

    private Map<String, Consumer<ReservationActionMessage>> handlers() {
        Map<String, Consumer<ReservationActionMessage>> byEventType = new HashMap<>();
        byEventType.put(PHONE_REQUESTED, this::handlePhoneRequest);
        RESULT_TYPES.keySet().forEach(eventType -> byEventType.put(eventType, this::handleResult));
        return Map.copyOf(byEventType);
    }

    private void handlePhoneRequest(ReservationActionMessage message) {
        reservationService.handlePhoneRequest(reservationActionMapper.toPhoneRequest(message));
    }

    private void handleResult(ReservationActionMessage message) {
        ReservationResultType type = RESULT_TYPES.get(message.getEventType());
        reservationService.handleReservationResult(reservationActionMapper.toResult(message, type));
    }

    private Optional<ReservationActionMessage> parse(String payload) {
        try {
            return Optional.ofNullable(jsonMapper.readValue(payload, ReservationActionMessage.class));
        } catch (JacksonException e) {
            return Optional.empty();
        }
    }

    // Sin eventId no se puede deduplicar; sin reservationProcessId no se sabe a qué proceso corresponde.
    private static boolean isIdentified(ReservationActionMessage message) {
        return isPresent(message.getEventId()) && isPresent(message.getEventType()) && isPresent(message.getReservationProcessId());
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private static void notHandled(ReservationActionMessage message) {
        log.info("Evento {} ({}) del proceso {}: tipo desconocido, se ignora",
                message.getEventType(), message.getEventId(), message.getReservationProcessId());
    }

    private static String abbreviate(String payload) {
        if (payload == null || payload.length() <= MAX_LOGGED_LENGTH) {
            return payload;
        }
        return payload.substring(0, MAX_LOGGED_LENGTH) + "…";
    }
}
