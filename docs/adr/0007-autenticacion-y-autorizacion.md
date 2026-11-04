# ADR 0007: Autenticación y autorización

- **Estado:** aceptada
- **Fecha:** 2026-11-03
- **Issues:** #9, #11, #13, #15 (las decisiones se tomaron ahí; este ADR las reúne)

## Contexto

El enunciado (§2, §3.2 y §9) pide que los usuarios finales se registren desde la app y usen un JWT para todo lo protegido; que cada usuario consulte y cancele solo sus propias reservas; que las contraseñas se guarden con un mecanismo criptográfico adecuado y que los secretos no queden en el código. El anexo §13 fija el formato de los errores.

Hay tres credenciales distintas en juego:
- el **JWT del usuario**, que emite Turnos y usa la app con Turnos y con el catálogo;
- el **JWT técnico de la cátedra**, con el que los dos servicios llaman al servicio central;
- el **secreto** con el que Turnos firma y el catálogo valida los JWT de los usuarios.

## Decisión

**Autenticación**
- Registro (`POST /api/register`) y login (`POST /api/authenticate` → `{"id_token": ...}`) con el contrato de JHipster, sin JHipster.
- Contraseñas con **Argon2id** ([ADR 0002](0002-hash-de-contrasenas-con-argon2id.md)). El login lo resuelve Spring Security (`DaoAuthenticationProvider`), y un usuario o una contraseña incorrectos dan el mismo `401 BAD_CREDENTIALS`, sin decir cuál falló.
- **JWT HS512** firmado por Turnos con `JwtEncoder` (Nimbus):
  - `sub` es el login;
  - el claim `auth` lleva los roles;
  - dura 24 h, o 30 días con `rememberMe`.
  - El secreto es `JWT_BASE64_SECRET`, Base64 de al menos 64 bytes. Sin él, la app no arranca.
- **Sin sesiones**: `SessionCreationPolicy.STATELESS` y CSRF desactivado. No hay cookies, cada request trae su token.

**Autorización**
- Públicos: `POST /api/register`, `POST /api/authenticate` y `/actuator/health`. Todo lo demás exige un JWT válido (`oauth2-resource-server`); sin él responde `401`.
- **Aislamiento entre usuarios**: el usuario sale siempre del token (`@AuthenticationPrincipal Jwt`, su `sub`), nunca de un parámetro. Las consultas de reservas filtran por ese usuario, y una reserva ajena responde `404`, igual que una inexistente, para no revelar que existe.
- Roles `ROLE_USER` y `ROLE_ADMIN`, como en JHipster. Hoy ninguna ruta distingue entre ellos: todas las operaciones son del propio usuario.

**Entre servicios**
- Turnos llama al catálogo **reenviando el JWT del usuario** (*token relay*, [ADR 0003](0003-consulta-al-catalogo-con-token-relay.md)). El catálogo lo valida con el mismo `JWT_BASE64_SECRET`. El JWT de la cátedra nunca sale hacia el catálogo ni hacia la app.

**Secretos y errores**
- `JWT_BASE64_SECRET`, `CATEDRA_JWT` y `DB_PASSWORD` llegan por variables de entorno (`.env`, ignorado por git; `.env.example` con valores falsos). La imagen de Docker no incluye el `.env`.
- Los errores salen como `application/problem+json` con un `code` estable. El handler global no devuelve trazas, mensajes de excepciones internas ni datos de la cátedra.
- SonarQube Cloud analiza la seguridad en cada PR, y las dependencias con vulnerabilidades conocidas se actualizaron (#23).

## Alternativas consideradas

- **JWT firmado con RSA** (clave privada en Turnos y pública en el catálogo): el catálogo no podría emitir tokens. Se descartó porque agrega manejo de claves, y con dos servicios propios en la misma entrega un secreto compartido alcanza.
- **Que Turnos llame al catálogo con su propia credencial**: el catálogo no sabría quién pidió. Se prefirió el *token relay* (ADR 0003).
- **Sesión con cookie**: no encaja con una app móvil que llama a dos servicios y obliga a manejar CSRF.

## Consecuencias

- Los dos servicios tienen que compartir el mismo `JWT_BASE64_SECRET`. Si se cambia, los tokens emitidos dejan de valer y los usuarios vuelven a iniciar sesión.
- No hay *logout* del lado del servidor: un token vale hasta que vence. La app, al cerrar sesión, borra el token guardado.
- Agregar una operación de administración es agregar una regla por rol, sin cambiar la emisión del token.
