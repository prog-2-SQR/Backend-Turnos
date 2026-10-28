# ADR 0003: Consulta al catálogo en el momento, con token relay

- **Estado:** aceptada
- **Fecha:** 2026-10-28
- **Issue:** #17
- **Contrato:** [Documentacion/contratos/turnos-catalogo.md](https://github.com/prog-2-SQR/Documentacion/blob/main/contratos/turnos-catalogo.md)

## Contexto

Para calcular la disponibilidad (enunciado §7), Turnos necesita saber si el profesional existe y está habilitado, y cuáles son sus horarios semanales vigentes. Esa información es del servicio de catálogo, que la sincroniza con la cátedra. El enunciado §4.2 pide que Turnos consulte la información vigente antes de cada operación y §8 que, si un servicio no responde, la operación no se inicie.

Además, el catálogo exige un JWT en todas sus APIs (Backend-Catalogo #27). Hay que decidir con qué credencial lo llama Turnos.

## Decisión

1. **Turnos no replica el catálogo.** En cada consulta pide `GET /api/professionals/{id}` y `GET /api/schedules?professionalId={id}`. No guarda copia ni caché.
2. **Token relay.** Turnos reenvía al catálogo el mismo JWT con el que el usuario lo llamó. Ese token lo emitió Turnos al iniciar sesión, y el catálogo lo valida con el secreto compartido `JWT_BASE64_SECRET`. Lo hace un interceptor del `RestClient` del catálogo (`shared/security/TokenRelayInterceptor`), así el slice no maneja tokens.
3. **El JWT técnico de la cátedra solo va a la cátedra** (ocupaciones, y más adelante holds). Nunca al catálogo ni a la app.
4. **Errores** (contrato entre servicios):

| Situación | Respuesta de Turnos |
|---|---|
| Catálogo: timeout, sin conexión o 5xx (después de 2 reintentos) | `503 CATALOG_UNAVAILABLE` |
| Catálogo: `404` | `404 PROFESSIONAL_NOT_FOUND` |
| Catálogo: profesional con `enabled: false` | `400 PROFESSIONAL_DISABLED` (no se consulta a la cátedra) |
| Catálogo: `401` | `401 CATALOG_UNAUTHORIZED` |
| Cátedra: timeout, sin conexión o 5xx (después de 2 reintentos) | `503 CATEDRA_UNAVAILABLE` |
| Otro `4xx` del catálogo o de la cátedra | `500`: es un error de Turnos y queda en el log |

Los timeouts son de 2 s de conexión y 5 s de lectura para el catálogo, y de 5 s y 10 s para la cátedra. Solo se reintentan lecturas (`GET`), que no tienen efectos.

## Alternativas consideradas

| Opción | Por qué no |
|---|---|
| Copia local del catálogo en Turnos (por Kafka o por consultas periódicas) | Duplica la sincronización que ya hace el catálogo y puede quedar desactualizada justo antes de reservar; el enunciado pide consultar la información vigente. |
| Que Turnos lea el Redis de la cátedra directamente | Saltea al catálogo, que es el dueño de esa información en el proyecto, y acopla Turnos al formato de la cátedra. |
| Usar el JWT técnico de la cátedra contra el catálogo | Es una credencial de la cátedra (anexo §2); el catálogo no la podría validar sin conocer el secreto de la cátedra. |
| Token de servicio propio de Turnos (otro rol, firmado con el secreto compartido) | Funciona, pero hoy siempre hay un usuario detrás de cada consulta. Queda anotado en el contrato para tareas sin usuario. |
