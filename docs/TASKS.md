# TASKS: Backend-Turnos

Las tareas del [PLAN](PLAN.md), una issue por tarea y un PR por issue, mergeado a `main` después de la revisión. ✅ terminada · 🔄 en curso · ✂️ recortada.

## M0 · Setup e infraestructura

- ✅ #1 Inicializar el proyecto Spring Boot 4.1 con Java 25 (PR #2)
- ✅ #3 PostgreSQL, Liquibase y `compose.yaml` (PR #4)
- ✅ #19 Analizar la calidad con SonarQube Cloud (PR #20)
- ✅ #21 Corregir los hallazgos de SonarQube Cloud (PR #22)
- ✅ #23 Actualizar dependencias con vulnerabilidades conocidas (PR #24)

## M5 · Usuarios y seguridad

- ✅ #5 Medir la cobertura con JaCoCo (PR #6)
- ✅ #7 Manejo global de errores en formato problem+json (PR #8)
- ✅ #9 Registrar usuarios con el contrato de JHipster (PR #10)
- ✅ #11 Iniciar sesión con JWT y proteger las APIs (PR #12)
- ✅ #13 Datos del usuario logueado en `GET /api/account` (PR #14)
- ✅ #15 Contraseñas con Argon2id (PR #16)

## M6 · Disponibilidad y reserva

- ✅ #18 Calcular los turnos disponibles (PR #17)
- ✅ #25 Crear y confirmar el hold de una reserva (PR #26)
- ✅ #27 Recibir el pedido de teléfono por Kafka (PR #28)
- ✅ #31 Enviar el teléfono con outbox y procesar los resultados (PR #32)

## M7 · Mis reservas y recuperación

- ✅ #33 Consultar las reservas propias (PR #34)
- ✅ #35 Cancelar una reserva confirmada (PR #36)
- ✅ #37 Reconciliar los procesos en curso con la cátedra (PR #38)
- ✂️ Notificar los cambios de estado por SSE: recortado. La app consulta el estado cada 3 s.

## M9 · Documentación y evidencias

- ✅ #29 Dockerfile del servicio (PR #30)
- ✅ #39 SPEC, PLAN, TASKS, ADR de seguridad y README al día
- ✅ Compose de toda la solución: en [Documentacion](https://github.com/prog-2-SQR/Documentacion)
