## Stage: 1 Building JAR file -------
FROM maven:3.9.11-eclipse-temurin-8-alpine as builder

WORKDIR /app

COPY pom.xml ./

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

## Stage: 2 Run JAR file -------
FROM eclipse-temurin:8u462-b08-jre-alpine-3.22

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]