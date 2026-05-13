FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=7860
ENV NETTY_SERVER_PORT=8080
ENV NETTY_CLIENT_PORT=8080
RUN mkdir -p /app/db /data/sync

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 7860 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
