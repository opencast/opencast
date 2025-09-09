#!/bin/bash

ls modules | while read line
do
  grep opencast-$line modules/jacoco-reports/pom.xml > /dev/null
  if [ $? -ne 0 ]; then
    echo "opencast-$line is missing from opencast-jacoco-reports"
    exit 1
  fi
done

grep artifactId modules/jacoco-reports/pom.xml | grep -o 'opencast-.*' | cut -f 1 -d '<' | cut -f 2- -d '-' | while read line
do
  if [ ! -d modules/$line ]; then
    echo "Module $line does not exist in the modules directory!"
    exit 1
  fi
done
