#!/bin/sh

cd packages/launcher; lein uberjar && cp target/shadow-cljs-launcher*.jar ../../test-project/launcher.jar