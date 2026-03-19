# -- Build stage --
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
ARG MODULE
RUN chmod +x mvnw && ./mvnw -pl ${MODULE} -am package -DskipTests && \
    cp ${MODULE}/target/*.jar app.jar

# -- Runtime stage --
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr tesseract-ocr-deu && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
