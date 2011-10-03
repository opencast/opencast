#!/bin/bash

server=
while getopts 's:' OPTION
do
echo "OPTION is ""$OPTION"
echo "OPTARG is ""$OPTARG"
  case $OPTION in
  s)	server="$OPTARG"
  		echo "server is ""$server"
		;;
  ?)	printf "Usage: %s: [-s value] args\n" $(basename $0) >&2
		exit 2
		;;
  esac
done
shift $(($OPTIND - 1))

sed -i .bak 's/${matterhornServer}/'${server}'/g' $FELIX_HOME/conf/config-tokenized.properties

mv -v $FELIX_HOME/conf/config-tokenized.properties $FELIX_HOME/conf/config.properties

#echo "remove " "$FELIX_HOME/conf/config-tokenized.properties.bak"
#rm $FELIX_HOME/conf/config-tokenized.properties.bak
