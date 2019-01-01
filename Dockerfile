FROM clojure:openjdk-11-lein AS clj-build
# Install node
RUN apt-get update && apt-get install curl && curl -sL https://deb.nodesource.com/setup_11.x | bash && apt-get install -y nodejs

# Install JS deps
COPY package.json package.json
RUN npm install

# Install clj(s) deps
COPY project.clj project.clj
RUN lein with-profile cljs deps

# Compile
COPY . .
RUN lein uberjar

### Production
FROM openjdk:11-jre-slim
COPY --from=clj-build /tmp/target/decidotron.jar decidotron.jar

EXPOSE 8080
CMD ["java", "-jar", "decidotron.jar"]
