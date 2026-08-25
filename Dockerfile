# Render uchun Docker build - Java 17 + Maven
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Pom ni avval ko'chirib dependency larni cache qilamiz (tez build)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kodni ko'chirib build
COPY src ./src
COPY mvnw ./mvnw
COPY .mvn ./.mvn
RUN chmod +x mvnw
RUN mvn package -DskipTests -B

# Runtime image - kichik va xavfsiz
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Build stage dan jar ni olamiz
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Render PORT ni beradi (10000), Spring ${PORT:8080} bilan o'qiydi
EXPOSE 8080

# Healthcheck uchun curl qo'shamiz
RUN apk add --no-cache curl

ENTRYPOINT ["java", "-jar", "app.jar"]
