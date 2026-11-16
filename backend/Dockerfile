FROM maven:3.10.0-eclipse-temurin-21-noble AS build
WORKDIR /src
COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /src/target/backend-0.1.0-SNAPSHOT.jar /app/fixmate.jar
USER 10001:10001
ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-jar","/app/fixmate.jar"]
