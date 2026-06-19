#!/bin/bash

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

find framework -name "*.java" > sources.txt

javac -cp "lib/servlet-api.jar" -d bin @sources.txt

rm sources.txt

jar cf framework.jar -C bin .

if [ -d "../test/src/main/webapp/WEB-INF/lib" ]; then
    cp framework.jar "../test/src/main/webapp/WEB-INF/lib/framework.jar"
fi
