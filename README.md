<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-48%2F100-yellow) ![Aprobar](https://img.shields.io/badge/Aprobar-s%C3%AD-brightgreen) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey) ![Build](https://img.shields.io/badge/Build-OK-green) ![Duplicación](https://img.shields.io/badge/Duplicaci%C3%B3n-0.0%25-green) ![%POO](https://img.shields.io/badge/%25POO-50%25-orange) ![Sueltas](https://img.shields.io/badge/Sueltas-0-green) ![D1](https://img.shields.io/badge/D1-60-yellow) ![D2](https://img.shields.io/badge/D2-36-red) ![D4](https://img.shields.io/badge/D4-0-red) ![D5](https://img.shields.io/badge/D5-100-green) ![D6](https://img.shields.io/badge/D6-5-red) ![D7](https://img.shields.io/badge/D7-100-green)

**Calidad (heurístico, 7 dimensiones):** índice **48/100** · build **OK** · aprobar **sí (7/11)** · destacar **no**
Detalle completo en [`docs/EVALUACION.md`](docs/EVALUACION.md).
<!-- calidad:fin -->

# Backend-Turnos

[![CI](https://github.com/prog-2-SQR/Backend-Turnos/actions/workflows/ci.yml/badge.svg)](https://github.com/prog-2-SQR/Backend-Turnos/actions/workflows/ci.yml)

Servicio de **turnos y reservas** del proyecto integrador de Programación 2 (Universidad de Mendoza, 2026).

Gestiona los usuarios finales de la app (registro y login con JWT), calcula la disponibilidad de turnos combinando el catálogo con las ocupaciones de la cátedra y lleva adelante el proceso de reserva (bloqueo, confirmación vía REST y Kafka, y seguimiento hasta su estado final).

## Estado

Esqueleto con base de datos: proyecto Spring Boot conectado a su propio PostgreSQL, con migraciones Liquibase y health check. Los usuarios y las reservas se incorporan en las próximas issues (ver milestones del repositorio).

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

## Tests

```bash
./mvnw verify
```

Los tests levantan su propio PostgreSQL descartable con **Testcontainers** y usan **WireMock** en lugar del catálogo y de la cátedra: no necesitan el `.env` ni el compose, solo Docker funcionando.

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
| `TURNOS_TIME_ZONE` | `America/Argentina/Buenos_Aires` | Zona de las horas de atención: define qué turnos de hoy ya empezaron |

Los usuarios se registran con `POST /api/register` e inician sesión con `POST /api/authenticate`, que devuelve `{"id_token": "..."}` (contrato de JHipster). El resto de las APIs requiere `Authorization: Bearer <id_token>`; sin token responden `401`.

## Disponibilidad

`GET /api/availability?professionalId=201&date=2026-11-02` devuelve los turnos libres de ese día:

```json
{"professionalId": 201, "date": "2026-11-02",
 "slots": [{"startTime": "12:00:00", "endTime": "12:30:00"}, {"startTime": "12:30:00", "endTime": "13:00:00"}]}
```

Turnos no guarda el catálogo: en cada consulta pide el profesional y sus horarios al servicio de catálogo, **reenviando el token del usuario** (*token relay*), y las ocupaciones a la cátedra con el JWT técnico. Arma los turnos de cada horario del día y saca los ocupados (reservas confirmadas y holds activos) y, si la fecha es hoy, los que ya empezaron. Errores: `400 INVALID_DATE` (fecha pasada), `404 PROFESSIONAL_NOT_FOUND`, `400 PROFESSIONAL_DISABLED`, `503 CATALOG_UNAVAILABLE` o `503 CATEDRA_UNAVAILABLE` (después de 2 reintentos). Detalle en el [ADR 0003](docs/adr/0003-consulta-al-catalogo-con-token-relay.md).

## Stack

| Componente | Versión |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Maven (wrapper) | 3.10.0 |
| PostgreSQL | 18 |
| Liquibase | gestionado por Spring Boot |
| Dependencias | Spring Web MVC, Validation, Spring Data JPA, Spring Security (resource server), RestClient, Lombok, Actuator |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers, WireMock, JaCoCo |

## Decisiones de arquitectura

- [ADR 0001: Motor de base de datos](docs/adr/0001-motor-de-base-de-datos.md)
- [ADR 0002: Hash de contraseñas con Argon2id](docs/adr/0002-hash-de-contrasenas-con-argon2id.md)
- [ADR 0003: Consulta al catálogo en el momento, con token relay](docs/adr/0003-consulta-al-catalogo-con-token-relay.md)

## Repositorios del proyecto

- [Backend-Catalogo](https://github.com/prog-2-SQR/Backend-Catalogo): catálogo de profesionales y sincronización con la cátedra
- [Backend-Turnos](https://github.com/prog-2-SQR/Backend-Turnos): este servicio
- [Frontend-KMP](https://github.com/prog-2-SQR/Frontend-KMP): app Android con Kotlin Multiplatform
- [Documentacion](https://github.com/prog-2-SQR/Documentacion): casos de uso, diagramas y decisiones de arquitectura
