#!/bin/bash
TMPFILE=`mktemp /tmp/${0##*/}.XXXXX`
trap 'rm -f $TMPFILE' 0
set -x
#
VAR_NAME="$1"
if [ -z "$VAR_NAME" ]; then
  echo "Variable name is required" 1>&2
  exit 2
fi
VAR_VALUE="$2"
if [ -z "$VAR_VALUE" ]; then
  echo "Variable value is required" 1>&2
  exit 2
fi
#
if [ ! -f ~/.profile ]; then
  touch ~/.profile
  [ $? -ne 0 ] && exit 1
fi
#
awk -v var="$VAR_NAME" -v value="$VAR_VALUE" 'BEGIN {
  found_var = 0; found_exp_var = 0; found_exp = 0; err = 0;
} {
  if (match($0, "^[ 	]*" var "=")) {
    found_var = 1; # found VAR=
    sub("=.*", "=\"" value "\"");
    print $0;
  }
  else if (match($0, "^[ 	]*export[ 	]+")) {
    if (match($0, "[ 	]+" var "=")) {
      found_exp_var = 1; # found export VAR=
      sub("=.*", "=\"" value "\"");
      print $0;
    }
    else if (match($0, "[ 	]+" var "([ 	]+|$)")) {
      if (match($0, "[ 	]+" var "[ 	]+=")) {
        err++;
        print "~/.profile: Syntax error in line " NR ": " $0 > "/dev/stderr"
        print $0;
      } else {
        found_exp = 1; # found export ... VAR ...
        if (!found_var && !found_exp_var) {
          found_var = 1;
          print var "=\"" value "\"";
        }
        print $0;
      }
    }
    else { print $0 }
  }
  else { print $0 }
} END {
  if (err) exit 2;
  if (!found_var) {
    if (!found_exp_var) {
      found_exp_var = 1;
      print "export " var "=\"" value "\"";
    }
  }
  else { # found_var
    if (!found_exp && !found_exp_var) {
      found_exp = 1;
      print "export " var;
    }
  }
}' ~/.profile > $TMPFILE
[ $? -ne 0 ] && exit 1
cp $TMPFILE ~/.profile
[ $? -ne 0 ] && exit 1
#
exit 0
