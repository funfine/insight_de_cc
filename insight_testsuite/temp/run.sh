#!/bin/sh

cp src/pom.xml ./
mvn assembly:assembly
mvn exec:java

