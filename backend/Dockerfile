FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
# CI runs the PostgreSQL integration tests before this packaging step.
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system opsdesk && useradd --system --gid opsdesk opsdesk
COPY --from=build /app/target/opsdesk-backend-0.1.0-SNAPSHOT.jar app.jar
USER opsdesk
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
