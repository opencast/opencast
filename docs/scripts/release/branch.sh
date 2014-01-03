#!/bin/bash

FUNCTIONS="functions.sh"
. ${FUNCTIONS}

#THIS SCRIPT SHOULD NOT BE USED IF YOU ARE CREATING A FEATURE BRANCH

#The name of the new branch, without the release branch prefix
#E.g. BRANCH_NAME=1.4.2
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

WORK_DIR=../../../

#Reset this script so that the modifications do not get committed
git checkout -- branch.sh

#Make sure we are on develop, then create a branch
git checkout develop

echo "Replacing POM file version in the POMs."
sed -i "s/<version>$OLD_POM_VER/<version>$NEW_POM_VER/" $WORK_DIR/pom.xml

for i in $WORK_DIR/modules/matterhorn-*
do
    echo " Module: $i"
    if [ -f $i/pom.xml ]; then
        sed -i "s/<version>$OLD_POM_VER/<version>$NEW_POM_VER/" $i/pom.xml
    fi
done

while [[ true ]]; do
  yesno -d no "NOTE: This script has made changes to your POM files.  Please ensure that it only made changes to the Matterhorn version number.  In rare cases some of the dependencies have the same version numbers, and the modification done above does *not* understand that it should not also change those versions.  Manual inspection of the changeset is required before continuing.  Have you finished checking all of the modifications?" has_checked
  if [[ "$has_checked" ]]; then 
      break
  fi
done

git checkout -b r/$BRANCH_NAME

git commit -a -m "$JIRA_TICKET Updated pom.xml files to reflect correct version.  Done via docs/scripts/release/branch.sh"

echo "Summary:"
echo "-Created r/$BRANCH_NAME from develop"
echo "We can push these changes to the public repo."
yesno -d no "Do you want this script to do that automatically for you?" push
if [[ "$push" ]]; then
    git push origin r/$BRANCH_NAME
fi
