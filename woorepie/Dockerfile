FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY woorepie/build/libs/*.jar /app/

ENTRYPOINT ["java", "-jar", "woorepie-0.0.1-SNAPSHOT.jar"]
