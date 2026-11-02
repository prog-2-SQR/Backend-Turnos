# ADR 0006: Recuperación de procesos en curso: reconciliación con la cátedra

- **Estado:** aceptada
- **Fecha:** 2026-11-02
- **Issue:** #37

## Contexto

Un proceso de reserva puede quedar en curso en Turnos aunque la cátedra ya sepa cómo terminó (enunciado §8; anexo §18.4):
- se perdió la respuesta de la confirmación del hold (timeout, caída de Turnos);
- Turnos se reinició en el medio;
- la cátedra no publicó el evento por Kafka.

El último caso ocurrió de verdad. El 02/11, las dos reservas de prueba figuraban `FAILED` en `GET /api/appointments`, pero el topic de acciones seguía vacío, y Turnos las veía en `WAITING_FOR_PHONE`.

Ese día también comprobamos que `GET /api/appointments` responde 500 en cuanto se le pasa `from` o `to` (anexo §11), y 200 sin fechas, incluso filtrando por `externalPatientId` (`Documentacion/integracion/pruebas-con-la-catedra.md`).

En Slack, a Perassi, el profe respondió que si se pierde la respuesta del POST del hold no hay forma de recuperarlo: hay que abandonar ese intento y mirar las ocupaciones; un turno `HELD` no se asume propio, y el hold dura 10 minutos.

## Decisión

1. **Reconciliación periódica.** `ReconciliationScheduler` corre poco después de arrancar (15 s) y después cada 5 minutos (`CATEDRA_RECONCILIATION_INTERVAL`).
2. **Qué se reconcilia:** los procesos en curso que ya tienen un `reservationProcessId` de la cátedra. Un proceso en `REQUESTED` no tiene ninguno; si la respuesta del hold se perdió, se cierra como abandonado (ADR 0004).
3. **Cómo se consulta:** un pedido por paciente, `GET /api/appointments?externalPatientId=user-<id>`, **sin `from`/`to`** porque dan 500. Se reintenta ante caídas, porque leer no tiene efectos.
4. **Qué se aplica:** el estado de la cátedra se traduce al local (`PHONE_PENDING → WAITING_FOR_PHONE`, `CONFIRMED`, `CANCELLED`, `FAILED`). Se aplica con `ReservationProcess.reconcileWith`, solo si la máquina de estados permite la transición. Un estado final no retrocede, y un estado igual no cambia nada.
   - `FAILED` se permite desde `WAITING_FOR_PHONE` y `PHONE_SUBMITTED`, con `failureCode = CATEDRA_FAILED`.
   - `PHONE_PENDING` sobre un proceso que quedó en `HELD` recupera una confirmación cuya respuesta se perdió.
5. **Si la cátedra no responde**, la vuelta se corta, queda en el log y se reintenta en la siguiente.

## Alternativas consideradas

| Opción | Por qué no |
|---|---|
| Consultar todo el listado con `from`/`to` | Responde 500 (probado el 02/11). |
| Traer todo el listado sin filtros y paginar | Trae las reservas de toda la cuenta técnica; por paciente son pocas y alcanza un pedido. |
| Esperar solo a Kafka | La cátedra cerró procesos sin publicar eventos: Turnos nunca se enteraría. |
| Reconciliar solo al arrancar | Los procesos que cambian sin evento mientras Turnos está levantado quedarían viejos hasta el próximo reinicio. |

## Consecuencias

- Un proceso puede tardar hasta 5 minutos en reflejar un cambio que no llegó por Kafka.
- La transición a `FAILED` desde la espera del teléfono existe solo porque la cátedra la produce; los eventos de Kafka siguen usando `EXPIRED` e `INVALID`.
- Probado contra la cátedra real el 02/11: al arrancar, Turnos pasó las dos reservas de prueba de `WAITING_FOR_PHONE` a `FAILED`.
