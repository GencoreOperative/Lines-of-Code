#!/usr/bin/env sh

# $1 = path to input jar
# $2 = path to output binary
# $3 = fully qualified main class name
# Example: ./executable.sh target/loc-1.0-SNAPSHOT.jar loc-by-user uk.org.gencoreoperative.commits.Main

INPUT_JAR="$1"
OUTPUT_BIN="$2"
MAIN_CLASS="$3"

# Prepend launcher to the jar
(cat <<SH
#!/usr/bin/env sh
exec java -cp "\$0" $MAIN_CLASS "\$@"
exit 0
SH
cat "$INPUT_JAR") > "$OUTPUT_BIN"

# Make executable
chmod +x "$OUTPUT_BIN"
