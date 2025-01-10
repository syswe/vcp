# Use OpenJDK as base image
FROM eclipse-temurin:17-jdk

# Install FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the Maven project
COPY pom.xml .
COPY src ./src

# Copy the Maven wrapper
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Build the application
RUN ./mvnw package -DskipTests

# Expose the port the app runs on
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/video-compression-0.0.1-SNAPSHOT.jar"] 