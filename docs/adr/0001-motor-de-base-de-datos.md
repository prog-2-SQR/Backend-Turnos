# ADR 0001: Motor de base de datos

- **Estado:** aceptada
- **Fecha:** 2026-10-15
- **Issue:** #3

## Contexto

El servicio de turnos necesita persistir los usuarios finales, los procesos de reserva con su estado, las reservas asociadas a cada usuario y, más adelante, los eventos pendientes de publicar (outbox).

Restricciones del enunciado:

- Cada servicio es dueño exclusivo de sus datos y administra sus propias migraciones (§3).
- La persistencia principal debe usar un gestor de base de datos servidor: no se aceptan H2, SQLite ni bases embebidas o en memoria (§3).
- La infraestructura debe poder levantarse con Docker Compose (§3).
- El motor es decisión del alumno y debe justificarse (§10).

Además, el proceso de reserva exige que un estado final nunca retroceda, que no se generen reservas duplicadas y que cada proceso quede asociado a su usuario (enunciado §4.2 y §7).

## Decisión

Usar **PostgreSQL 18** como base de datos del servicio, con migraciones gestionadas por **Liquibase**. Es el mismo motor que el servicio de catálogo, pero con una instancia propia.

- Imagen `postgres:18-alpine`, la misma en `compose.yaml` y en los tests (Testcontainers), para que se comporten igual.
- Una instancia propia para este servicio, en su propio `compose.yaml` y en el puerto 5433. No comparte base ni usuario con el servicio de catálogo.
- Las credenciales llegan por variables de entorno; no hay valores secretos en el repositorio.

## Alternativas consideradas

| Opción | Por qué no |
|---|---|
| H2 / SQLite | Prohibidas por el enunciado como almacenamiento principal. |
| MySQL 8 | Cumple los requisitos y en lo básico es equivalente. PostgreSQL suma índices únicos parciales (MySQL no los tiene) y un tipo explícito para instantes con zona horaria (`timestamptz`). |
| Una sola instancia de PostgreSQL compartida con el catálogo (una base por servicio) | El enunciado lo permite, pero obliga a separar usuarios y permisos a mano. Con una instancia por servicio, la separación de datos se cumple por construcción y cada repositorio se levanta de forma independiente. |

## Consecuencias

**A favor**

- Índices únicos parciales: permiten garantizar en la base que un usuario tenga **un solo proceso de reserva pendiente** a la vez, sin que los procesos terminados cuenten.
- Transacciones ACID robustas: el cambio de estado de un proceso y el registro del evento en el outbox se guardan juntos o no se guardan.
- `SELECT ... FOR UPDATE SKIP LOCKED` para procesar el outbox sin que dos hilos tomen el mismo evento.
- `timestamptz` guarda instantes en UTC (`expiresAt`, `occurredAt`), que es el formato que usa el anexo.
- Soporte de primera clase en Liquibase, Testcontainers y Spring Boot (`@ServiceConnection`).

**En contra**

- Hace falta Docker para levantar la base y para correr los tests. Docker ya es requisito del proyecto (Compose y Testcontainers), así que no suma una dependencia nueva.
- Dos instancias de PostgreSQL (una por servicio) consumen más memoria en local que una sola.
