# Simple image: build the jar first (./mvnw package -DskipTests), then docker build.
FROM eclipse-temurin:27-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
