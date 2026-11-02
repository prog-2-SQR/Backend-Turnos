# Imagen del servicio de turnos, en dos etapas:
# 1. build: compila con el wrapper de Maven y Java 25 (JDK).
# 2. runtime: solo el jar sobre un JRE 25, sin el código fuente ni Maven, y con un usuario sin privilegios.
# Los secretos no van en la imagen: se pasan al arrancar (docker run --env-file .env, o el compose).

FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Primero el pom y el wrapper: mientras no cambien las dependencias, Docker reutiliza esta capa.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

# Los tests no corren acá: usan Testcontainers (necesitan Docker) y ya los corre el CI con ./mvnw verify.
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
    && cp target/turnos-*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /build/app.jar app.jar
USER app

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
