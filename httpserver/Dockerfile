FROM registry.access.redhat.com/ubi8/openjdk-17:1.14 as builder
WORKDIR /home/jboss
COPY pom.xml .
COPY src ./src
RUN mvn package


FROM registry.access.redhat.com/ubi8/openjdk-17-runtime:1.14
WORKDIR /home/jboss
COPY --from=builder /home/jboss/target/httpserver-1.0-SNAPSHOT.jar ./
EXPOSE 8080
CMD ["java", "-cp", "httpserver-1.0-SNAPSHOT.jar",  "demo.HTTPServerDemo"]
