#!/bin/bash
mvn clean package
java -jar target/polarupgradetool-1.0-SNAPSHOT.jar
