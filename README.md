<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-40%2F100-yellow) ![Aprobar](https://img.shields.io/badge/Aprobar-s%C3%AD-brightgreen) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey) ![Build](https://img.shields.io/badge/Build-OK-green) ![Duplicación](https://img.shields.io/badge/Duplicaci%C3%B3n-0.0%25-green) ![%POO](https://img.shields.io/badge/%25POO-56%25-orange) ![Sueltas](https://img.shields.io/badge/Sueltas-0-green) ![D1](https://img.shields.io/badge/D1-58-orange) ![D2](https://img.shields.io/badge/D2-36-red) ![D4](https://img.shields.io/badge/D4-0-red) ![D5](https://img.shields.io/badge/D5-95-green) ![D6](https://img.shields.io/badge/D6-2-red) ![D7](https://img.shields.io/badge/D7-62-yellow)

**Calidad (heurístico, 7 dimensiones):** índice **40/100** · build **OK** · aprobar **sí (7/11)** · destacar **no**
Detalle completo en [`docs/EVALUACION.md`](docs/EVALUACION.md).
<!-- calidad:fin -->

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
