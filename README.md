<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-66%2F100-green) ![Aprobar](https://img.shields.io/badge/Aprobar-no-red) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey) ![Build](https://img.shields.io/badge/Build-OK-green) ![Duplicación](https://img.shields.io/badge/Duplicaci%C3%B3n-0.6%25-green) ![%POO](https://img.shields.io/badge/%25POO-65%25-yellow) ![Sueltas](https://img.shields.io/badge/Sueltas-7-red) ![D1](https://img.shields.io/badge/D1-2-red) ![D2](https://img.shields.io/badge/D2-100-green) ![D4](https://img.shields.io/badge/D4-90-green) ![D5](https://img.shields.io/badge/D5-0-red) ![D6](https://img.shields.io/badge/D6-100-green) ![D7](https://img.shields.io/badge/D7-100-green)

**Calidad (heurístico, 7 dimensiones):** índice **66/100** · build **OK** · aprobar **no (6/11)** · destacar **no**
Detalle completo en [`docs/EVALUACION.md`](docs/EVALUACION.md).
<!-- calidad:fin -->

# Backend-Turnos

[![CI](https://github.com/prog-2-SQR/Backend-Turnos/actions/workflows/ci.yml/badge.svg)](https://github.com/prog-2-SQR/Backend-Turnos/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=prog-2-SQR_Backend-Turnos&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=prog-2-SQR_Backend-Turnos)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=prog-2-SQR_Backend-Turnos&metric=coverage)](https://sonarcloud.io/summary/new_code?id=prog-2-SQR_Backend-Turnos)

Servicio de **turnos y reservas** del proyecto integrador de Programación 2 (Universidad de Mendoza, 2026).

Gestiona los usuarios finales de la app (registro y login con JWT), calcula la disponibilidad de turnos combinando el catálogo con las ocupaciones de la cátedra y lleva adelante el proceso de reserva (bloqueo, confirmación vía REST y Kafka, y seguimiento hasta su estado final).

## Estado

Completo para la entrega: usuarios con JWT, disponibilidad, el proceso de reserva con la cátedra (hold, teléfono por Kafka con outbox y resultados), listado y cancelación de reservas propias, y reconciliación con la cátedra. El SSE quedó recortado: la app consulta el estado de la reserva.

- [SPEC](docs/SPEC.md): qué hace el servicio, su API, los estados del proceso y el modelo de datos.
- [PLAN](docs/PLAN.md): arquitectura, etapas, decisiones y estrategia de pruebas.
- [TASKS](docs/TASKS.md): las issues de cada etapa con su PR.

## Requisitos

- **JDK 25**
- **Docker** con Docker Compose (para la base de datos y para los tests)
- **Git**

No hace falta instalar Maven: el wrapper (`./mvnw`) descarga la versión correcta la primera vez.

## Cómo ejecutarlo

```bash
git clone https://github.com/prog-2-SQR/Backend-Turnos.git
cd Backend-Turnos

# 1. Configuración local: copiar el ejemplo, poner una password propia en DB_PASSWORD,
#    el mismo JWT_BASE64_SECRET que el catálogo y los datos de la cuenta técnica de la cátedra (CATEDRA_*)
cp .env.example .env

# 2. Levantar PostgreSQL (puerto 5433)
docker compose up -d

# 3. Levantar el servicio (puerto 8081)
./mvnw spring-boot:run
```

La aplicación lee el archivo `.env` al arrancar. Liquibase aplica las migraciones automáticamente.

Para comprobar que está vivo y conectado a la base:

```bash
curl http://localhost:8081/actuator/health
# {"components":{"db":{"status":"UP"}, ...},"status":"UP"}
```

Para detener la base sin perder los datos: `docker compose down` (los datos quedan en el volumen `turnos-db-data`).

En Windows, usar `mvnw.cmd` en lugar de `./mvnw`.

### Con Docker

El `Dockerfile` arma la imagen en dos etapas: compila con el JDK 25 y deja solo el jar sobre un JRE 25, con un usuario sin privilegios. Los tests no corren en la imagen (los corre el CI) y el `.env` no se copia: los secretos se pasan al arrancar.

```bash
# Con la base levantada (docker compose up -d)
docker build -t turnos .
docker run --rm --network host --env-file .env turnos
```

`--network host` (Linux o WSL) deja que el contenedor use la red de la máquina: llega a la base por `localhost` y a la cátedra por ZeroTier, igual que con `./mvnw spring-boot:run`. Para levantar los dos servicios juntos, con sus bases, está el compose del repositorio [Documentacion](https://github.com/prog-2-SQR/Documentacion#levantar-toda-la-solución).

## Tests

```bash
./mvnw verify
```

Los tests levantan su propio PostgreSQL descartable con **Testcontainers** y usan **WireMock** en lugar del catálogo y de la cátedra (y Kafka en Testcontainers): no necesitan el `.env` ni el compose, solo Docker funcionando.

En cada push y PR, el CI además analiza el código con **[SonarQube Cloud](https://sonarcloud.io/summary/new_code?id=prog-2-SQR_Backend-Turnos)**: quality gate, bugs, vulnerabilidades, duplicación y la cobertura de JaCoCo. El token está en el secret `SONAR_TOKEN` del repositorio.

## Configuración

| Variable | Por defecto | Descripción |
|---|---|---|
| `SERVER_PORT` | `8081` | Puerto HTTP del servicio (el catálogo usa 8080) |
| `DB_HOST` | `localhost` | Host de PostgreSQL |
| `DB_PORT` | `5433` | Puerto de PostgreSQL |
| `DB_NAME` | `turnos` | Nombre de la base |
| `DB_USERNAME` | `turnos` | Usuario |
| `DB_PASSWORD` | (obligatoria) | Password. Sin valor por defecto: se define en `.env` |
| `JWT_BASE64_SECRET` | (obligatoria) | Secreto para firmar los JWT de los usuarios (HS512, Base64 de al menos 64 bytes). Generarlo con `openssl rand -base64 64 \| tr -d '\n'`. **El mismo valor en el catálogo**, que valida esos tokens |
| `CATEDRA_API_URL` | (obligatoria) | URL base de la API REST de la cátedra (llega por ZeroTier) |
| `CATEDRA_JWT` | (obligatoria) | JWT técnico de la cuenta del proyecto. Nunca se commitea |
| `CATALOGO_API_URL` | `http://localhost:8080` | URL del servicio de catálogo |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Broker de Kafka de la cátedra |
| `KAFKA_CONSUMER_GROUP_ID` | `alumnos` | Consumer group de la integración; Turnos le agrega `-turnos` |
| `KAFKA_APPOINTMENT_ACTIONS_TOPIC` | (obligatoria) | Topic de acciones de reserva (`alumnos.turnos.acciones.<groupId>`) |
| `KAFKA_APPOINTMENT_PHONE_TOPIC` | (obligatoria) | Topic donde se publica el teléfono (`catedra.turnos.telefono.<groupId>`) |
| `CATEDRA_GROUP_ID` | (obligatoria) | `groupId` de la integración: va en cada evento del teléfono |
| `CATEDRA_RECONCILIATION_INTERVAL` | `PT5M` | Cada cuánto se reconcilian los procesos en curso con la cátedra |
| `TURNOS_TIME_ZONE` | `America/Argentina/Buenos_Aires` | Zona de las horas de atención: define qué turnos de hoy ya empezaron |

Los usuarios se registran con `POST /api/register` e inician sesión con `POST /api/authenticate`, que devuelve `{"id_token": "..."}` (contrato de JHipster). El resto de las APIs requiere `Authorization: Bearer <id_token>`; sin token responden `401`.

## Disponibilidad

`GET /api/availability?professionalId=201&date=2026-11-02` devuelve los turnos libres de ese día:

```json
{"professionalId": 201, "date": "2026-11-02",
 "slots": [{"startTime": "12:00:00", "endTime": "12:30:00"}, {"startTime": "12:30:00", "endTime": "13:00:00"}]}
```

Turnos no guarda el catálogo: en cada consulta pide el profesional y sus horarios al servicio de catálogo, **reenviando el token del usuario** (*token relay*), y las ocupaciones a la cátedra con el JWT técnico. Arma los turnos de cada horario del día y saca los ocupados (reservas confirmadas y holds activos) y, si la fecha es hoy, los que ya empezaron. Errores: `400 INVALID_DATE` (fecha pasada), `404 PROFESSIONAL_NOT_FOUND`, `400 PROFESSIONAL_DISABLED`, `503 CATALOG_UNAVAILABLE` o `503 CATEDRA_UNAVAILABLE` (después de 2 reintentos). Detalle en el [ADR 0003](docs/adr/0003-consulta-al-catalogo-con-token-relay.md).

## Reservas

`POST /api/reservations` inicia una reserva del usuario del token:

```json
{"professionalId": 201, "date": "2026-11-02", "startTime": "12:30"}
```

Revalida el turno con la disponibilidad vigente, pide el hold a la cátedra y lo confirma. Responde `201` con el proceso en `WAITING_FOR_PHONE` y su `expiresAt`; la cátedra pide el teléfono por Kafka. `GET /api/reservations/{id}` devuelve un proceso propio (uno ajeno da `404`).

`GET /api/reservations?page=0&size=20` lista las reservas propias, en curso y terminadas, de la más nueva a la más vieja: `{"content": [...], "page": 0, "size": 20, "totalElements": 3, "totalPages": 1}`. `size` va de 1 a 100.

`POST /api/reservations/{id}/cancel` cancela una reserva propia **confirmada**, con un motivo opcional (`{"reason": "..."}`, hasta 500 caracteres). Primero se cancela en la cátedra (anexo §12) y después en Turnos; responde `200` con el proceso en `CANCELLED`. Una ya cancelada se devuelve igual; en otro estado → `409 RESERVATION_NOT_CANCELLABLE`.

- Un usuario tiene como mucho **una reserva en curso**: lo garantiza un índice único parcial de PostgreSQL (`409 RESERVATION_ALREADY_PENDING`).
- El proceso se guarda antes de llamar a la cátedra y después de cada respuesta. Los rechazos de la cátedra llegan con su mismo code (`409 SLOT_ALREADY_HELD`, `400 INVALID_SLOT`...) y el proceso queda `FAILED` o `EXPIRED`.
- La cátedra exige nombre y apellido del paciente: salen de la cuenta, o de `patientFirstName` y `patientLastName` en el pedido (`400 PATIENT_NAME_REQUIRED` si faltan).

Después la cátedra pide el teléfono por Kafka (`AdditionalInformationRequested`, topic `KAFKA_APPOINTMENT_ACTIONS_TOPIC`). Turnos lo registra en el proceso y `GET /api/reservations/{id}` lo muestra con `phoneRequested: true` y el mensaje de la cátedra. Un evento repetido (mismo `eventId`) no tiene efecto, uno de un proceso terminado se ignora, y uno que llega antes de que se guarde la confirmación del hold se reintenta. Turnos usa su propio consumer group (`KAFKA_CONSUMER_GROUP_ID` + `-turnos`), para no rebalancearse con el catálogo.

Estados, reintentos y alternativas en el [ADR 0004](docs/adr/0004-proceso-de-reserva.md).

### Teléfono y resultado

`POST /api/reservations/{id}/phone` responde el pedido de teléfono:

```json
{"phoneNumber": "+54 9 261 555-1234"}
```

Valida el teléfono con la regla de la cátedra (`400 INVALID_PHONE_NUMBER`) y que la cátedra lo haya pedido (`409 PHONE_NOT_REQUESTED`). Responde `202` con el proceso en `PHONE_SUBMITTED`. El teléfono se guarda en un **outbox** en la misma transacción, y un proceso programado lo publica cada 2 s en el topic del teléfono (`KAFKA_APPOINTMENT_PHONE_TOPIC`) como `AdditionalInformationSubmitted`. Si Kafka no confirma el envío, se reintenta con el **mismo `eventId`**, hasta 10 veces.

El resultado llega por el topic de acciones y se ve en `GET /api/reservations/{id}`:

| Evento | Estado |
|---|---|
| `AppointmentConfirmed` | `CONFIRMED` |
| `AdditionalInformationRejected` | `WAITING_FOR_PHONE`, con `phoneRejectionMessage`: se puede mandar otro teléfono |
| `AppointmentProcessExpired` | `EXPIRED` |
| `AppointmentProcessInvalid` | `INVALID` |
| `AppointmentCancelled` | `CANCELLED` |

Un evento repetido no tiene efecto y uno tardío no reabre un estado final. Detalle en el [ADR 0005](docs/adr/0005-telefono-con-outbox-y-resultados.md).

### Reconciliación con la cátedra

Si se perdió una respuesta, Turnos se reinició o la cátedra no avisó por Kafka, un proceso puede quedar en curso aunque la cátedra ya lo haya resuelto. Por eso, poco después de arrancar y cada 5 minutos (`CATEDRA_RECONCILIATION_INTERVAL`), Turnos consulta `GET /api/appointments?externalPatientId=user-<id>` por cada paciente con procesos en curso y los pone al día (`CONFIRMED`, `FAILED`, o `WAITING_FOR_PHONE` si se perdió la respuesta de la confirmación). No usa `from`/`to`, que en la cátedra responden 500. Detalle en el [ADR 0006](docs/adr/0006-recuperacion-y-reconciliacion.md).

## Stack

| Componente | Versión |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.2 |
| Maven (wrapper) | 3.10.0 |
| PostgreSQL | 18 |
| Liquibase | gestionado por Spring Boot |
| Dependencias | Spring Web MVC, Validation, Spring Data JPA, Spring Security (resource server), RestClient, Spring Kafka, Bouncy Castle (Argon2id), Lombok, Actuator |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL y Kafka), WireMock, JaCoCo |

## Decisiones de arquitectura

- [ADR 0001: Motor de base de datos](docs/adr/0001-motor-de-base-de-datos.md)
- [ADR 0002: Hash de contraseñas con Argon2id](docs/adr/0002-hash-de-contrasenas-con-argon2id.md)
- [ADR 0003: Consulta al catálogo en el momento, con token relay](docs/adr/0003-consulta-al-catalogo-con-token-relay.md)
- [ADR 0004: Proceso de reserva: estados, hold y confirmación](docs/adr/0004-proceso-de-reserva.md)
- [ADR 0005: Teléfono con outbox y resultados del proceso](docs/adr/0005-telefono-con-outbox-y-resultados.md)
- [ADR 0006: Recuperación de procesos en curso: reconciliación con la cátedra](docs/adr/0006-recuperacion-y-reconciliacion.md)
- [ADR 0007: Autenticación y autorización](docs/adr/0007-autenticacion-y-autorizacion.md)

## Repositorios del proyecto

- [Backend-Catalogo](https://github.com/prog-2-SQR/Backend-Catalogo): catálogo de profesionales y sincronización con la cátedra
- [Backend-Turnos](https://github.com/prog-2-SQR/Backend-Turnos): este servicio
- [Frontend-KMP](https://github.com/prog-2-SQR/Frontend-KMP): app Android con Kotlin Multiplatform
- [Documentacion](https://github.com/prog-2-SQR/Documentacion): casos de uso, diagramas y decisiones de arquitectura
