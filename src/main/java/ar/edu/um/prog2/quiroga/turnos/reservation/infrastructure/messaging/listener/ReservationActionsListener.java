package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.listener;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.service.ReservationService;
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
 * caso de uso, como indicó el profe. Cada eventType tiene su manejador; los que todavía no se procesan se registran
 * en el log. Un mensaje que no se puede leer se descarta, porque reintentarlo no lo arregla.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationActionsListener {

    static final String LISTENER_ID = "reservationActionsListener";
    static final String PHONE_REQUESTED = "AdditionalInformationRequested";
    private static final int MAX_LOGGED_LENGTH = 200;

    private final ReservationService reservationService;
    private final JsonMapper jsonMapper;
    private final ReservationActionMapper reservationActionMapper;

    // Un manejador por eventType. Las referencias a métodos leen los campos recién cuando se las llama.
    private final Map<String, Consumer<ReservationActionMessage>> handlers = Map.of(
            PHONE_REQUESTED, this::handlePhoneRequest);

    // idIsGroup = false: el consumer group es spring.kafka.consumer.group-id, no el id del listener.
    @KafkaListener(id = LISTENER_ID, idIsGroup = false, topics = "${catedra.appointment-actions-topic}")
    public void onAction(String payload) {
        Optional<ReservationActionMessage> message = parse(payload).filter(ReservationActionsListener::isIdentified);
        if (message.isEmpty()) {
            log.warn("Mensaje descartado del topic de acciones: {}", abbreviate(payload));
            return;
        }
        handlers.getOrDefault(message.get().getEventType(), ReservationActionsListener::notHandledYet).accept(message.get());
    }

    private void handlePhoneRequest(ReservationActionMessage message) {
        reservationService.handlePhoneRequest(reservationActionMapper.toPhoneRequest(message));
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

    private static void notHandledYet(ReservationActionMessage message) {
        log.info("Evento {} ({}) del proceso {}: todavía no se procesa",
                message.getEventType(), message.getEventId(), message.getReservationProcessId());
    }

    private static String abbreviate(String payload) {
        if (payload == null || payload.length() <= MAX_LOGGED_LENGTH) {
            return payload;
        }
        return payload.substring(0, MAX_LOGGED_LENGTH) + "…";
    }
}
