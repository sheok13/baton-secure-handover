FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -m batonapp
COPY --from=build /app/build/libs/app.jar app.jar
RUN chown batonapp:batonapp app.jar
USER batonapp
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s CMD curl -f http://localhost:8080/actuator/health || exit 1
CMD ["java", "-jar", "app.jar"]