# Frontend: Node 22 cumple el requisito >=22.12 de Vite 8.
FROM node:22-bookworm-slim AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Descarga solo durante la construcción; versiones y SHA-256 fijados en Git.
FROM debian:bookworm-slim AS models
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*
COPY deployment/ /deployment/
RUN sh /deployment/prepare-model.sh minilm /opt/models/minilm \
    && sh /deployment/prepare-model.sh bge /opt/models/bge \
    && cp /deployment/ATTRIBUTIONS.md /opt/models/ATTRIBUTIONS.md

# Empaqueta con pruebas activas. Sin variables de modelos se omiten únicamente
# las cuatro pruebas reales que ya tienen esa condición en el proyecto.
FROM maven:3.9.11-eclipse-temurin-21 AS backend
WORKDIR /build/backend
COPY backend/pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY backend/src/ ./src/
COPY backend/datos/ ./datos/
COPY --from=frontend /frontend/dist/ ./src/main/resources/static/
RUN mvn -B -ntp package \
    && cp target/bibliometria-analisis-algoritmos-0.0.1-SNAPSHOT.jar /build/app.jar

# Solo JRE, aplicación, datos y modelos. Sin Node, Maven ni PowerShell.
FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends libstdc++6 libgomp1 fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --create-home --home-dir /home/app app
WORKDIR /app
COPY --from=backend /build/app.jar /app/app.jar
COPY backend/datos/ /app/datos/
COPY --from=models /opt/models/ /opt/models/
ENV MINILM_MODEL_DIR=/opt/models/minilm \
    BGE_MODEL_DIR=/opt/models/bge \
    HOME=/home/app \
    JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=10.0 -XX:MaxRAMPercentage=40.0 -XX:+ExitOnOutOfMemoryError"
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
