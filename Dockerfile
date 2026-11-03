FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system mpl && useradd --system --gid mpl mpl
COPY --from=build /workspace/target/mpl-one-0.0.1-SNAPSHOT.jar app.jar
USER mpl
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
