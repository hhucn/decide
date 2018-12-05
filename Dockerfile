FROM clojure:openjdk-8-lein
COPY . .
RUN lein clean && lein uberjar

FROM openjdk:8-jre-alpine

COPY --from=0 /tmp/target/decidotron.jar decidotron.jar

EXPOSE 8080
CMD ["java", "-jar", "decidotron.jar"]