test: cljs-tests clj-tests

cljs-tests:
	npm install
	npx shadow-cljs compile ci-tests
	npx karma start --single-run

clj-tests:
	clojure -A:dev:clj-tests

production:
	@echo "Compiling SCSS";
	sass scss/main.scss resources/public/css/main.css --no-source-map --style compressed
	@echo ""; echo "Building JAR..."
	lein uberjar

.PHONY: test cljs-tests clj-tests production
