FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

COPY profile-service/pom.xml profile-service/pom.xml
COPY profile-service/src profile-service/src

COPY feedback-service/pom.xml feedback-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml

RUN chmod +x mvnw

RUN ./mvnw -pl profile-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/profile-service/target/*.jar app.jar

EXPOSE 9091

ENTRYPOINT ["java", "-jar", "app.jar"]