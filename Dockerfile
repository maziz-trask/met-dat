# Single deployable image: React build is served by Spring Boot (same origin => no CORS, one URL, one HTTPS endpoint).
# Build from the repository root:  docker build -t met-dat .
FROM node:22-alpine AS frontend
WORKDIR /fe
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /be
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /fe/dist ./src/main/resources/static
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && mkdir -p /data/attachments && chown -R app:app /data
USER app
WORKDIR /app
COPY --from=backend /be/target/met-dat.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70" \
    APP_STORAGE_TYPE=filesystem APP_STORAGE_PATH=/data/attachments
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
