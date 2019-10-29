test: cljs-tests clj-tests

cljs-tests:
	npm install
	npx shadow-cljs compile ci-tests
	npx karma start --single-run

clj-tests:
	clj -A:dev:clj-tests

.PHONY: test cljs-tests clj-tests
