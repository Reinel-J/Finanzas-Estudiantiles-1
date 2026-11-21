# syntax=docker/dockerfile:1

# ---------- Etapa 1: compilar el JAR con Maven ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Descargar dependencias primero (se cachean mientras pom.xml no cambie)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

# Compilar el código fuente
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/*.jar /app/app.jar

# ---------- Etapa 2: imagen de ejecución liviana ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/app.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
