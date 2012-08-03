#!/bin/bash
#set -x

FILE="$1"
if [ -z "$FILE" ]; then
  echo "Config file name is required" 1>&2
  exit 2
fi

get_value() {
  awk -v var="$1" -v repl_var="$2" -v repl_val="$3" '{
  line = $0;
  sub("^[ 	]*", "", line); sub("[ 	]*$", "", line);
  if (match(line, "^#") || length(line) == 0) {
    next;
  }
  if (match(line, "^" var "[ 	]*=")) {
    sub("^" var "[ 	]*=[ 	]*", "", line);
    gsub("\\${java.io.tmpdir}", "/tmp", line);
    gsub("\\${felix.work}", "${FELIX_WORK_DIR}", line);
    gsub("\\${" repl_var "}", repl_val, line);
    print line;
    exit 0;
  }
}' "$FILE"
}

VALUE=`get_value "$2"`
[ $? -ne 0 ] && exit 1
if [ "$VALUE" != "${VALUE#*$\{}" ]; then
  # resolve reference to another variable value
  REPL_VAR="${VALUE#*$\{}"
  REPL_VAR="${REPL_VAR%%\}*}"
  SH_VAR="\${$REPL_VAR}"
  REPL_VAL=`eval echo "$SH_VAR"`
  if [ -z "$REPL_VAL" ]; then
    REPL_VAL=`get_value "$REPL_VAR"`
    [ $? -ne 0 ] && exit 1
    [ -z "$REPL_VAL" ] && REPL_VAR=""
  fi
  VALUE=`get_value "$2" "$REPL_VAR" "$REPL_VAL"`
  [ $? -ne 0 ] && exit 1
fi
[ -n "$VALUE" ] && echo "$VALUE"

exit 0
