# Use Maven image for building
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Use JDK for running
FROM eclipse-temurin:17-jdk-jammy

# Install FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create directories for video processing
RUN mkdir -p /app/processed /app/uploads

# Expose the port
EXPOSE 8080

# Set environment variables
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application with Maven
COPY --from=builder /usr/share/maven /usr/share/maven
ENV MAVEN_HOME=/usr/share/maven
ENV PATH=${MAVEN_HOME}/bin:${PATH}

# Copy source files for Maven
COPY pom.xml .
COPY src ./src

# Run with specific port
CMD ["mvn", "spring-boot:run", "-Dspring-boot.run.arguments=--server.port=8080"]