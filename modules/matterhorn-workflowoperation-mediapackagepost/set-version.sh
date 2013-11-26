#!/bin/sh

if [ $# -ne 1 ]
then
	echo "Usage: $0 version"
	exit
fi

sed -i "s/<version>MH_VERSION<\/version>/<version>$1<\/version>/" modules/matterhorn-workflowoperation-mediapackagepost/pom.xml
