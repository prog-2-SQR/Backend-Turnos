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

# 1. Configuración local: copiar el ejemplo y poner una password propia en DB_PASSWORD
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

Los tests levantan su propio PostgreSQL descartable con **Testcontainers**: no necesitan el `.env` ni el compose, solo Docker funcionando.

## Configuración

| Variable | Por defecto | Descripción |
|---|---|---|
| `SERVER_PORT` | `8081` | Puerto HTTP del servicio (el catálogo usa 8080) |
| `DB_HOST` | `localhost` | Host de PostgreSQL |
| `DB_PORT` | `5433` | Puerto de PostgreSQL |
| `DB_NAME` | `turnos` | Nombre de la base |
| `DB_USERNAME` | `turnos` | Usuario |
| `DB_PASSWORD` | (obligatoria) | Password. Sin valor por defecto: se define en `.env` |

## Stack

| Componente | Versión |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Maven (wrapper) | 3.10.0 |
| PostgreSQL | 18 |
| Liquibase | gestionado por Spring Boot |
| Dependencias | Spring Web MVC, Validation, Spring Data JPA, Lombok, Actuator |
| Tests | JUnit 5, Testcontainers |

## Decisiones de arquitectura

- [ADR 0001: Motor de base de datos](docs/adr/0001-motor-de-base-de-datos.md)

## Repositorios del proyecto

- [Backend-Catalogo](https://github.com/prog-2-SQR/Backend-Catalogo): catálogo de profesionales y sincronización con la cátedra
- [Backend-Turnos](https://github.com/prog-2-SQR/Backend-Turnos): este servicio
- [Frontend-KMP](https://github.com/prog-2-SQR/Frontend-KMP): app Android con Kotlin Multiplatform
- [Documentacion](https://github.com/prog-2-SQR/Documentacion): casos de uso, diagramas y decisiones de arquitectura
