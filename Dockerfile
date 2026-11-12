# Build stage: compile, run the test suite, and package the executable jar.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package

# Runtime stage: JRE only. Mount the input CSV and an output directory as volumes.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/aggregator.jar /app/aggregator.jar
ENTRYPOINT ["java", "-Xmx64m", "-jar", "/app/aggregator.jar"]
CMD ["--help"]
