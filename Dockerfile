# Use Maven official image with OpenJDK 21
FROM jelastic/maven:3.9.5-openjdk-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:21-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring Boot application JAR file into the container
COPY --from=build /app/target/nafaVerseBackend-0.0.1-SNAPSHOT.jar nafaVerseBackend-0.0.1-SNAPSHOT.jar

# Expose the port your Spring application will run on
EXPOSE 8080

# Command to run the Spring Boot application

ENTRYPOINT ["java", "-jar", "nafaVerseBackend-0.0.1-SNAPSHOT.jar"]