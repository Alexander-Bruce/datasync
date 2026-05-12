FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=7860
RUN mkdir -p /app/db /data/sync

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 7860 8443
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
