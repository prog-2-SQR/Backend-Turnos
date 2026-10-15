# Backend-Turnos

[![CI](https://github.com/prog-2-SQR/Backend-Turnos/actions/workflows/ci.yml/badge.svg)](https://github.com/prog-2-SQR/Backend-Turnos/actions/workflows/ci.yml)

Servicio de **turnos y reservas** del proyecto integrador de Programación 2 (Universidad de Mendoza, 2026).

Gestiona los usuarios finales de la app (registro y login con JWT), calcula la disponibilidad de turnos combinando el catálogo con las ocupaciones de la cátedra y lleva adelante el proceso de reserva (bloqueo, confirmación vía REST y Kafka, y seguimiento hasta su estado final).

## Estado

Esqueleto inicial: proyecto Spring Boot que compila, arranca y expone el health check. La base de datos, los usuarios y las reservas se incorporan en las próximas issues (ver milestones del repositorio).

## Requisitos

- **JDK 25**
- **Git**

No hace falta instalar Maven: el wrapper (`./mvnw`) descarga la versión correcta la primera vez.

## Cómo ejecutarlo

```bash
git clone https://github.com/prog-2-SQR/Backend-Turnos.git
cd Backend-Turnos

# Compilar y correr los tests
./mvnw verify

# Levantar el servicio (puerto 8080)
./mvnw spring-boot:run
```

Para comprobar que está vivo:

```bash
curl http://localhost:8080/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
```

En Windows, usar `mvnw.cmd` en lugar de `./mvnw`.

## Stack

| Componente | Versión |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Maven (wrapper) | 3.10.0 |
| Dependencias | Spring Web MVC, Validation, Lombok, Actuator |

## Repositorios del proyecto

- [Backend-Catalogo](https://github.com/prog-2-SQR/Backend-Catalogo): catálogo de profesionales y sincronización con la cátedra
- [Backend-Turnos](https://github.com/prog-2-SQR/Backend-Turnos): este servicio
- [Frontend-KMP](https://github.com/prog-2-SQR/Frontend-KMP): app Android con Kotlin Multiplatform
- [Documentacion](https://github.com/prog-2-SQR/Documentacion): casos de uso, diagramas y decisiones de arquitectura
