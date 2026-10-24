FROM node:22-alpine AS frontend

WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
COPY --from=frontend /workspace/src/main/resources/static/app ./src/main/resources/static/app
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system reviewdesk \
    && useradd --system --gid reviewdesk --home-dir /app reviewdesk

WORKDIR /app
COPY --from=build /workspace/target/review-desk-0.1.0-SNAPSHOT.jar /app/app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080

USER reviewdesk

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

