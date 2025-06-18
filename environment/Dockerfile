# Stage 1: Build với Maven + Java 17
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

# Stage 2: Chạy app với Java 23 SDK
FROM eclipse-temurin:23-jdk-alpine

WORKDIR /app

COPY --from=builder /app/Ticket-start/target/Ticket-start-1.0-SNAPSHOT.jar app.jar

EXPOSE 1122

ENTRYPOINT ["java", "-jar", "app.jar"]
