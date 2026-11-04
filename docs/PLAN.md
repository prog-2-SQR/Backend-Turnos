# PLAN: Backend-Turnos

Cómo se construyó el servicio que describe [SPEC.md](SPEC.md): arquitectura, orden de trabajo y decisiones. Las tareas una por una están en [TASKS.md](TASKS.md).

## Arquitectura

Hexagonal con *vertical slicing*, según el modelo del profesor ([dqmdz/hexagonal](https://github.com/dqmdz/hexagonal)). Cada slice tiene tres capas:

- `domain/`: modelo, puertos (`ports/in`, un caso de uso por interfaz; `ports/out`, lo que necesita de afuera) y su excepción. Solo usa `java.*` y Lombok, sin Spring, JPA ni Jackson.
- `application/`: la implementación de cada caso de uso y un service que hace de fachada.
- `infrastructure/`: los adaptadores, que son controllers, JPA, clientes REST (`client/`), Kafka (`messaging/`) y tareas programadas (`scheduling/`), con sus DTO y mappers escritos a mano.

```
ar.edu.um.prog2.quiroga.turnos
├── config/          propiedades de la cátedra, RestClient, reintentos, Kafka y scheduling
├── shared/web/      handler global de errores (problem+json)
├── shared/security/ seguridad y JWT
├── user/            registro y cuenta (contrato de JHipster)
├── auth/            login y emisión del JWT
├── availability/    turnos libres (catálogo + ocupaciones de la cátedra)
└── reservation/     proceso de reserva: hold, teléfono (outbox), resultados, listado, cancelación y reconciliación
```

- **Dependencias entre slices**: se usa la referencia por id, o un puerto de salida que implementa el adaptador del otro slice. Por ejemplo, `reservation` revalida el turno con `availability` y lee el nombre del paciente de `user`, cada uno por su propio puerto (ADR 0004).
- **Transversales**: `config` y `shared` están fuera de los slices y no los importan (decisión del profesor en Slack).

## Etapas

| Milestone | Objetivo | Resultado |
|---|---|---|
| M0 · Setup e infraestructura | Proyecto base, PostgreSQL con Liquibase, CI, SonarQube Cloud | Spring Boot 4.1 con Java 25, compose, `ci.yml` y quality gate |
| M5 · Usuarios y seguridad | Usuarios con el contrato de JHipster y APIs protegidas con JWT | Registro, login, `/api/account`, Argon2id, problem+json y JaCoCo |
| M6 · Disponibilidad y reserva | Turnos libres y el proceso de reserva completo con la cátedra | Disponibilidad con token relay, hold y confirmación, teléfono con outbox y resultados por Kafka |
| M7 · Mis reservas y recuperación | Consultar y cancelar lo propio y recuperarse de fallas | Listado paginado, cancelación y reconciliación |
| M9 · Documentación y evidencias | Entrega reproducible y documentada | Dockerfile, SPEC/PLAN/TASKS, ADRs y compose de toda la solución (en Documentacion) |

## Decisiones

| ADR | Decisión |
|---|---|
| [0001](adr/0001-motor-de-base-de-datos.md) | PostgreSQL con Liquibase |
| [0002](adr/0002-hash-de-contrasenas-con-argon2id.md) | Contraseñas con Argon2id, en lugar de BCrypt |
| [0003](adr/0003-consulta-al-catalogo-con-token-relay.md) | Consultar el catálogo en cada operación, reenviando el JWT del usuario (*token relay*) |
| [0004](adr/0004-proceso-de-reserva.md) | Proceso de reserva: estados, hold y confirmación |
| [0005](adr/0005-telefono-con-outbox-y-resultados.md) | Teléfono con outbox y resultados por Kafka, con deduplicación por `eventId` |
| [0006](adr/0006-recuperacion-y-reconciliacion.md) | Reconciliación periódica con `GET /api/appointments` |
| [0007](adr/0007-autenticacion-y-autorizacion.md) | Autenticación y autorización: JWT propio, roles y aislamiento entre usuarios |

## Estrategia de pruebas

- **Casos de uso**: tests unitarios con Mockito sobre los puertos, que cubren el camino feliz y cada error.
- **Controllers**: MockMvc, que verifica la seguridad (`401` sin token), la validación y el formato problem+json.
- **Adaptadores**: JPA contra PostgreSQL en Testcontainers; clientes REST contra WireMock (catálogo y cátedra, con timeouts, `5xx` y reintentos); Kafka con un broker real en Testcontainers (pedido de teléfono, outbox y resultados).
- **CI**: `./mvnw verify` en cada push y PR, con JaCoCo y SonarQube Cloud. Un PR se mergea solo con el CI en verde.

## Recortes y pendientes

- **SSE**: el estado de una reserva no se empuja a la app. La app consulta cada 3 s mientras la reserva está en curso; es el recorte previsto en el plan.
- **Cátedra**: el topic de acciones no publica eventos, y `GET /api/appointments` con `from`/`to` responde 500. Los flujos de Kafka se prueban con Testcontainers, y la reconciliación consulta sin fechas. Detalle en [Documentacion/integracion/pruebas-con-la-catedra.md](https://github.com/prog-2-SQR/Documentacion/blob/main/integracion/pruebas-con-la-catedra.md).
