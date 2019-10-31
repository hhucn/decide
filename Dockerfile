FROM clojure:openjdk-11-lein AS clj-build

RUN apt-get update &&\
    apt-get install -y curl
RUN curl -sL https://deb.nodesource.com/setup_13.x | bash &&\
    apt-get install -y nodejs &&\
    npm install -g sass
RUN curl -O https://download.clojure.org/install/linux-install-1.10.1.478.sh &&\
    chmod +x linux-install-1.10.1.478.sh &&\
    ./linux-install-1.10.1.478.sh

COPY package.json scss/
RUN npm install &&\
    sass scss/main.scss resources/public/css/main.css --no-source-map --style compressed

COPY . .
RUN lein uberjar

FROM openjdk:11-jre-slim
COPY src/main/config/prod.edn /config/production.edn
COPY --from=clj-build /tmp/target/decide.jar decide.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dconfig=/config/production.edn", "-Dfulcro.logging=info", "-jar", "decide.jar"]