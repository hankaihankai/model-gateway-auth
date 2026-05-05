# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY .mvn/docker-settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /build/target/model-gateway-auth-0.0.1-SNAPSHOT.jar /app/model-gateway-auth.jar

EXPOSE 8188

ENTRYPOINT ["java", "-jar", "/app/model-gateway-auth.jar"]
