FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

COPY profile-service/pom.xml profile-service/pom.xml
COPY feedback-service/pom.xml feedback-service/pom.xml
COPY feedback-service/src feedback-service/src
COPY api-gateway/pom.xml api-gateway/pom.xml

RUN chmod +x mvnw

RUN ./mvnw -pl feedback-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/feedback-service/target/*.jar app.jar

EXPOSE 9092

ENTRYPOINT ["java", "-jar", "app.jar"]