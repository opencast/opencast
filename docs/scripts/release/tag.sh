#!/bin/bash

. ${functions.sh}

#TODO: Handle release vs RC destinction (one uses git flow, one does not...)

#The comments in this file assume you are creating a tag from a release branch
#This script does *not* push any changes, so it should be safe to experiment with locally

#The version the POMs are in the development branch.
#E.g. BRANCH_POM_VER=1.3-SNAPSHOT
BRANCH_POM_VER=

#The new version of our release as it will show up in the tags directory
#E.g. RELEASE_VER=1.3-rc5
RELEASE_VER=

#The jira ticket this work is being done under (must be open)
JIRA_TICKET=

#=======You should not need to modify anything below this line=================

WORK_DIR=$(echo `pwd` | sed 's/\(.*\/.*\)\/docs\/scripts\/release/\1/g')

#Get the current branch name
curBranch=`git status | grep "On branch" | sed 's/\# On branch \(.*\)/\1/'`

echo "Warning, this script will create a mini branch off of $curBranch to do the POM version modification"
yesno -d "n" "Do you wish to continue" cont
if [[ ! "$ok" ]]; then
  return 1
fi

#The version we want the poms to be, usually the same as RELEASE_VER
TAG_POM_VER=$RELEASE_VER

echo "Replacing POM file version in main POM."
sed -i "s/<version>$BRANCH_POM_VER/<version>$TAG_POM_VER/" $WORK_DIR/pom.xml

for i in modules/matterhorn-*
do
    echo " Module: $i"
    if [ -f $WORK_DIR/$i/pom.xml ]; then
        sed -i "s/<version>$BRANCH_POM_VER/<version>$TAG_POM_VER/" $WORK_DIR/$i/pom.xml
        sleep 1
    fi
done
git checkout -b r/$JIRA_TICKET-$RELEASE_VER
git commit -a -m "Creating $RELEASE_VER branch to contain POM changes and tag"
git tag -s $RELEASE_VER -m "Creating $RELEASE_VER branch and tag as part of $JIRA_TICKET"

echo "Please verify that things look correct, and then push the new branch and tag upstream!"
