#!/bin/bash

find -name "*.java" > sources.txt

javac -cp "lib/servlet-api.jar" -d bin @sources.txt

rm sources.txt

jar cf framework.jar -C bin .