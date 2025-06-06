#!/bin/bash

echo "Compilazione del progetto..."
mvn clean compile

echo "Generazione del classpath completo..."
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt

FULL_CP="target/classes:$(cat cp.txt)"

echo "Avvio EvoSuite..."
java -jar ~/isw2/tools/evosuite/evosuite-1.2.0.jar \
  -class org.apache.bookkeeper.bookie.BufferedChannel \
  -projectCP "$FULL_CP" \
  -Dsandbox=false \
  -Dsearch_budget=60
