FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

COPY profile-service/pom.xml profile-service/pom.xml
COPY feedback-service/pom.xml feedback-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY api-gateway/src api-gateway/src

RUN chmod +x mvnw

RUN ./mvnw -pl api-gateway -am clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/api-gateway/target/*.jar app.jar

EXPOSE 9090

USER 10001:10001

ENTRYPOINT ["java", "-jar", "app.jar"]