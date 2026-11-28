# Interfaz v1 y API en una sola imagen: comparten origen, como exige la cookie de sesion.
# Se construye desde la raiz del repositorio:  docker build -f backend-springboot/Dockerfile .

FROM node:20-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build:v1

FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /src
COPY backend-springboot/pom.xml ./
COPY backend-springboot/src ./src
# Las pruebas corren en el CI; aqui solo se empaqueta
RUN mvn -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S barboya && adduser -S barboya -G barboya \
    && mkdir -p /data/files && chown -R barboya:barboya /data
WORKDIR /app
COPY --from=api /src/target/barboya-backend-0.1.0.jar app.jar
COPY --from=web /web/dist-v1 web/
ENV SPRING_PROFILES_ACTIVE=prod \
    SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/app/web/ \
    FILES_DIRECTORY=/data/files
USER barboya
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8081/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
