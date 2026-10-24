# ADR 0002: Hash de contraseñas con Argon2id

- **Estado:** aceptada
- **Fecha:** 2026-10-24
- **Issue:** #15

## Contexto

El servicio de turnos guarda las contraseñas de los usuarios finales. El enunciado (§3.2 y §9) exige almacenarlas con un mecanismo criptográfico adecuado, nunca en texto plano. El registro (#9) empezó con BCrypt, que es lo que usa JHipster.

El 24/10 el profesor pidió en Slack: *"estoy viendo en las issues y PR de seguridad que están usando bcrypt, desde la masificación de las IA eso pasó a ser casi obsoleto, los que puedan muden la seguridad a argon2id"*.

El riesgo que se busca cubrir es el de una base filtrada: el atacante prueba contraseñas contra los hashes con GPUs o hardware dedicado. Cuanto más caro sea cada intento, menos contraseñas puede probar.

## Decisión

Las contraseñas se guardan con **Argon2id** (`Argon2PasswordEncoder` de Spring Security, con BouncyCastle) y los parámetros mínimos que recomienda OWASP:

| Parámetro | Valor |
|---|---|
| Memoria | 19 MiB (19 456 KiB) |
| Iteraciones | 2 |
| Paralelismo | 1 |
| Sal | 16 bytes, aleatoria por usuario |
| Hash | 32 bytes |

- El hash queda autodescripto: `$argon2id$v=19$m=19456,t=2,p=1$<sal>$<hash>` (97 caracteres). Si en el futuro se suben los parámetros, los hashes viejos se siguen verificando con los suyos.
- `jhi_user.password_hash` pasa de `varchar(60)` (largo de BCrypt) a `varchar(128)` con un changeSet nuevo de Liquibase.
- El caso de uso no conoce el algoritmo: depende del puerto `PasswordHasher`; el adapter `PasswordEncoderHasher` usa el `PasswordEncoder` que define `shared/security/PasswordEncoderConfig`. El login (`DaoAuthenticationProvider`) usa el mismo bean para comparar.
- No se mantiene compatibilidad con BCrypt: todavía no hay usuarios reales que migrar.

## Alternativas consideradas

| Opción | Por qué no |
|---|---|
| BCrypt (lo anterior) | Solo es costoso en CPU y usa muy poca memoria, así que se paraleliza bien en GPU. Además trunca la contraseña a 72 bytes. |
| PBKDF2 | Tampoco usa memoria; OWASP lo recomienda solo cuando se necesita cumplir FIPS. |
| scrypt | También consume memoria, pero Argon2id es la primera recomendación de OWASP y ganó la Password Hashing Competition. |
| `DelegatingPasswordEncoder` (Argon2id para nuevos, BCrypt para viejos) | Agrega complejidad sin beneficio: no hay hashes BCrypt de usuarios reales. Queda como opción si alguna vez hay que migrar datos existentes. |

## Consecuencias

**A favor**

- Cada intento de adivinar una contraseña necesita 19 MiB de memoria: atacar con GPU es mucho más caro que con BCrypt.
- El cambio quedó en un solo bean: el resto del código depende de `PasswordEncoder` o del puerto `PasswordHasher`.

**En contra**

- Cada login y cada registro usa 19 MiB y unos 100–150 ms de CPU (medido localmente). Es aceptable para el volumen del proyecto; con mucho tráfico habría que limitar intentos o dimensionar memoria.
- Se suma la dependencia `bcprov-jdk18on`.

## Referencias

- Slack del curso, 24/10 (pedido del profesor).
- OWASP Password Storage Cheat Sheet (Argon2id: m=19 MiB, t=2, p=1).
- Spring Security: `Argon2PasswordEncoder`.
