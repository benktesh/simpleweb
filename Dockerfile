FROM maven:3.6.3-jdk-8 AS MAVEN_TOOL_CHAIN
COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN mvn clean package

FROM openjdk:8
COPY --from=MAVEN_TOOL_CHAIN /tmp/target/SimpleWeb-1.0-jar-with-dependencies.jar /SimpleWeb-1.0.jar
CMD ["java", "-jar", "/LiveStreaming-1.0.jar"]
