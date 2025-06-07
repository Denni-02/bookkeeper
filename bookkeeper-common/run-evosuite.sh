EVOSUITE_JAR=~/isw2/tools/evosuite/evosuite-1.2.0.jar
TARGET_CLASS=org.apache.bookkeeper.common.util.MathUtils

# === GENERAZIONE CLASSPATH ===
echo "📦 Generazione del classpath per EvoSuite..."
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt || exit 1
FULL_CP="target/classes:$(cat cp.txt)"

# === ESECUZIONE EVOSUITE ===
echo "🚀 Avvio EvoSuite per $TARGET_CLASS..."
java -jar "$EVOSUITE_JAR" \
  -class "$TARGET_CLASS" \
  -projectCP "$FULL_CP" \
  -Dsandbox=false \
  -Dsearch_budget=60 || exit 1

echo "✅ Test generati in bookkeeper-server/evosuite-tests/"
