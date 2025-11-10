FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN mkdir -p /app/logs /tmp && chown -R 1000:users /app /tmp

COPY --from=build /app/target/*.jar /app/app.jar
USER 1000:users
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
