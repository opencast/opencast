#!/bin/bash

#THIS SCRIPT SHOULD NOT BE USED IF YOU ARE CREATING A FEATURE BRANCH
#This script modifies the POM files for develop

#The name of the new branch, without the release branch prefix
#E.g. BRANCH_NAME=1.3.x
BRANCH_NAME=

#The version the POMs are in develop right now.
#E.g. OLD_POM_VER=1.3-SNAPSHOT
OLD_POM_VER=

#The new version for our POMs
#E.g. NEW_POM_VER=1.4-SNAPSHOT
NEW_POM_VER=

#The jira ticket this work is being done under (must be open)
JIRA_TICKET=

#=======You should not need to modify anything below this line=================

WORK_DIR=$(echo `pwd` | sed 's/\(.*\/.*\)\/docs\/scripts\/release/\1/g')

git checkout -- branch.sh
git flow release start $BRANCH_NAME
git checkout develop

echo "Replacing POM file version in main POM."
sed -i "s/<version>$OLD_POM_VER/<version>$NEW_POM_VER/" $WORK_DIR/pom.xml

for i in $WORK_DIR/modules/matterhorn-*
do
    echo " Module: $i"
    if [ -f $i/pom.xml ]; then
        sed -i "s/<version>$BRANCH_POM_VER/<version>$TAG_POM_VER/" $i/pom.xml
    fi
done
git commit -a -m "$JIRA_TICKET Updated pom.xml files to reflect correct version.  Done via docs/scripts/release/branch.sh"

echo ""
echo "Please verify that things look correct, and then push the new branch upstream!s"
