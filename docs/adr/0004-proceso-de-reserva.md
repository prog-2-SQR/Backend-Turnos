# ADR 0004: Proceso de reserva: estados, hold y confirmación

- **Estado:** aceptada
- **Fecha:** 2026-10-29
- **Issue:** #25

## Contexto

El enunciado (§4.2 y §7) pide que el servicio de turnos inicie y conserve el estado local de los procesos de reserva: pedir un bloqueo temporal (hold) a la cátedra, confirmarlo y seguir el intercambio asincrónico por Kafka hasta un resultado final. El anexo (§9, §10, §13.1, §15 y §18) define los contratos, y §8 del enunciado exige soportar timeouts, respuestas perdidas, reinicios y vencimientos sin estados imposibles de recuperar.

Para iniciar una reserva hacen falta datos de otros dos slices del servicio: la disponibilidad vigente (`availability`) y el usuario (`user`).

## Decisión

1. **Máquina de estados en el dominio** (`ReservationStatus`):
   `REQUESTED → HELD → WAITING_FOR_PHONE → PHONE_SUBMITTED → CONFIRMED → CANCELLED`, con salidas a `FAILED`, `EXPIRED` e `INVALID`. Cada estado declara a cuáles puede pasar: un estado final no cambia más y `CONFIRMED` solo puede cancelarse, así un evento tardío nunca hace retroceder un proceso (anexo §18.3).
2. **Un solo proceso pendiente por usuario**, garantizado por la base: índice único parcial `ux_reservation_one_pending_per_user` sobre `user_id` para los estados pendientes (anticipado en el ADR 0001). Si dos pedidos llegan a la vez, PostgreSQL deja pasar uno y el otro recibe `409 RESERVATION_ALREADY_PENDING`. Un pendiente cuyo hold venció, o que quedó sin respuesta más de 5 minutos, se cierra al iniciar uno nuevo.
3. **El proceso se guarda antes de llamar a la cátedra** (`REQUESTED`) y después de cada respuesta. El progreso queda persistido (enunciado §8) y no se tiene una conexión a la base abierta mientras se espera.
4. **Hold y confirmación en la misma operación** (`POST /api/reservations`), como el paso "iniciar reserva" del diagrama del anexo §17. El proceso queda en `WAITING_FOR_PHONE`.
5. **Reintentos según el efecto de cada llamada** (anexo §18.4):
   - crear el hold **no** se reintenta: repetirlo chocaría con el propio hold (`SLOT_ALREADY_HELD`) y su `holdId` no se puede recuperar. Si se pierde la respuesta, ese hold vence solo en la cátedra y el proceso queda `FAILED`;
   - confirmar **sí** se reintenta ante timeouts y 5xx: si ya estaba confirmado, la cátedra responde `RESERVATION_PROCESS_ALREADY_CONFIRMED` y eso cuenta como éxito.
6. **Paciente:** `externalPatientId` es `user-<id>`, un identificador opaco y estable que no expone el login. Nombre y apellido salen de la cuenta, o del pedido si vienen. Sin ellos la cátedra no confirma, así que se responde `400 PATIENT_NAME_REQUIRED` antes de pedir el hold.
7. **Otros slices por puertos de salida**, como en el catálogo (ADR 0004 y 0005 del catálogo): `reservation` declara `SlotAvailability` y `PatientDirectory`, y un adapter por cada uno usa el service del otro slice (`AvailabilityService`, `UserService`). Revalidar con la disponibilidad cumple "revalidar profesional y horario justo antes de pedir el hold" (contrato turnos-catalogo) y evita pedir un hold sobre un turno ocupado.

## Alternativas consideradas

| Opción | Por qué no |
|---|---|
| Pedir el hold y recién después guardar el proceso | Si dos pedidos del mismo usuario llegan juntos, los dos crean holds en la cátedra antes de que el índice frene a uno. Y una caída en el medio deja un hold sin registro local. |
| Chequear "un solo pendiente" solo en el código | Entre la consulta y el insert puede entrar otro pedido: solo la base lo garantiza ante pedidos simultáneos. |
| Reintentar también la creación del hold | El segundo intento devuelve `SLOT_ALREADY_HELD` por el propio hold, sin forma de recuperar su `holdId`. |
| Mandar el login como `externalPatientId` | Le pasa un dato del usuario al servicio central. `user-<id>` identifica igual y no cambia nunca. |
| Dejar que solo la cátedra valide el turno | No cumple el contrato entre servicios (revalidar contra el catálogo) ni el enunciado §8 (no iniciar operaciones sin validar la información vigente). |
