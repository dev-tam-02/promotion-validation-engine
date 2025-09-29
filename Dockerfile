FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/validation-engine-*.jar app.jar
ENTRYPOINT ["java","-jar", "app.jar"]
