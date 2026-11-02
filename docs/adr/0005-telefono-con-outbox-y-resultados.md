# ADR 0005: Teléfono con outbox y resultados del proceso

- **Estado:** aceptada
- **Fecha:** 2026-11-02
- **Issue:** #31

## Contexto

Después de confirmar el hold (ADR 0004), la cátedra pide un teléfono por Kafka (`AdditionalInformationRequested`). El servicio tiene que responder con `AdditionalInformationSubmitted` en el topic del teléfono y procesar el resultado final que llega por el topic de acciones: confirmación, rechazo del teléfono, vencimiento, proceso inválido o cancelación (enunciado §5 puntos 11 y 12, §7; anexo §15.5 a §15.10).

Hay dos problemas de consistencia:
- **Guardar y publicar no son atómicos.** Si se guarda el proceso como `PHONE_SUBMITTED` y Kafka está caído, el teléfono se pierde. Si se publica primero y la base falla, la cátedra recibe un teléfono que Turnos no registró.
- **Kafka entrega al menos una vez y sin orden garantizado entre REST y Kafka** (anexo §15.1, §18.1 y §18.3): un resultado puede llegar repetido, o tarde, después de otro final.

En Slack, el profe confirmó que el outbox es correcto: en los reintentos se conserva el mismo `eventId`, la cátedra ignora los duplicados, y la confirmación de Kafka no es la confirmación funcional (esa llega con `AppointmentConfirmed`).

## Decisión

1. **Patrón outbox para el teléfono.** `POST /api/reservations/{id}/phone` guarda en **una sola transacción** el cambio del proceso a `PHONE_SUBMITTED` y una fila en `reservation_phone_outbox`, con un `eventId` nuevo. Kafka no se toca en el pedido HTTP: responde `202`.
2. **Publicación aparte.** Un proceso programado (`PhoneOutboxScheduler`, cada 2 s, `fixedDelay`) publica las filas sin enviar, con key = `reservationProcessId` de la cátedra (anexo §15.1). Cuando Kafka confirma la recepción, la fila queda con `sent_at`. Si falla, suma un intento y se reintenta **con el mismo `eventId`**, hasta 10 intentos (anexo §18.4: reintentos limitados y observables, en el log y en la columna `attempts`).
3. **Validación local del teléfono** con la regla de la cátedra (se sacan espacios, guiones, paréntesis y puntos; `^\+?[0-9]{7,15}$`): `400 INVALID_PHONE_NUMBER` en el momento, sin esperar el rechazo por Kafka. Sin pedido de la cátedra, `409 PHONE_NOT_REQUESTED`.
4. **Resultados con la máquina de estados del dominio.** Cada resultado lleva el proceso a un estado (`ReservationResultType`) y solo se aplica si `ReservationStatus` permite esa transición desde el estado actual:

   | Evento | Estado |
   |---|---|
   | `AppointmentConfirmed` | `CONFIRMED` (guarda el `reservationId` de la cátedra, para cancelar en M7) |
   | `AdditionalInformationRejected` | vuelve a `WAITING_FOR_PHONE` con el mensaje y el nuevo `expiresAt`; el usuario puede mandar otro teléfono, con otro `eventId` |
   | `AppointmentProcessExpired` | `EXPIRED` |
   | `AppointmentProcessInvalid` | `INVALID` |
   | `AppointmentCancelled` | `CANCELLED` (solo desde `CONFIRMED`) |

5. **Idempotencia por `eventId`**, igual que el pedido de teléfono: el cambio del proceso y el registro en `reservation_processed_events` van en la misma transacción. Un evento repetido no tiene efecto; uno tardío que no corresponde se registra y se ignora; uno que llega antes de que se guarde la confirmación del hold se reintenta (`CONFIRMATION_IN_FLIGHT`).

## Alternativas consideradas

| Opción | Por qué no |
|---|---|
| Publicar en Kafka dentro del pedido HTTP | Si Kafka está caído o tarda, el pedido falla o queda colgado, y un teléfono aceptado se puede perder. Además, base y Kafka no comparten transacción. |
| Generar un `eventId` nuevo en cada reintento | Si el primer envío llegó pero se perdió la confirmación, la cátedra recibiría dos teléfonos distintos para el mismo pedido. Con el mismo `eventId` descarta el duplicado. |
| Reintentar sin límite | Un error permanente quedaría reintentándose para siempre. Con el límite, el proceso vence con su hold y queda registrado. |
| Aplicar cada resultado sin mirar el estado actual | Un evento tardío (por ejemplo, un vencimiento después de la confirmación) haría retroceder un estado final (anexo §18.3). |

## Consecuencias

- El usuario ve `PHONE_SUBMITTED` apenas manda el teléfono; el resultado lo ve después con `GET /api/reservations/{id}` (o, más adelante, por SSE).
- Puede pasar hasta un intervalo del scheduler (2 s) entre que se acepta el teléfono y que sale a Kafka.
- Hasta que la cátedra publique en el topic de acciones (consultado por Slack el 29/10), el flujo se prueba con Kafka y PostgreSQL reales en Testcontainers (`ReservationPhoneKafkaTest`).
