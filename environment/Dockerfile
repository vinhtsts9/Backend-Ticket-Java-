# Use pre-built jar from host machine to avoid test compilation issues
FROM eclipse-temurin:23-jdk-alpine

WORKDIR /app

COPY Ticket-start/target/Ticket-start-1.0-SNAPSHOT.jar app.jar

EXPOSE 1122

ENTRYPOINT ["java", "-jar", "app.jar"]
