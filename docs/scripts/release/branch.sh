#!/bin/bash

FUNCTIONS="functions.sh"
. ${FUNCTIONS}

#THIS SCRIPT SHOULD NOT BE USED IF YOU ARE CREATING A FEATURE BRANCH

#The name of the new branch, without the release branch prefix
#E.g. BRANCH_NAME=1.4.2
BRANCH_NAME=

#The version the POMs are in develop right now.
#E.g. OLD_POM_VER=1.4-SNAPSHOT
OLD_POM_VER=

#The new version for our POMs
#E.g. BRANCH_POM_VER=1.4.2-SNAPSHOT
BRANCH_POM_VER=

#The version that develop should have.  This is only relevant for final releases!
#E.g. NEW_DEVELOP_POM_VER=1.5-SNAPSHOT
NEW_DEVELOP_POM_VER=

#The jira ticket this work is being done under (must be open)
JIRA_TICKET=

#=======You should not need to modify anything below this line=================

WORK_DIR=../../../

#Reset this script so that the modifications do not get committed
git checkout -- branch.sh

#Make sure we are on develop, then create a branch
git checkout develop

echo "Replacing POM file version in the POMs."
updatePomVersions -w $WORK_DIR -o $OLD_POM_VER -n $BRANCH_POM_VER

git commit -a -m "$JIRA_TICKET Updated pom.xml files to reflect new branch version.  Done via docs/scripts/release/branch.sh"

git checkout -b r/$BRANCH_NAME

git checkout develop
git revert --no-edit HEAD

echo "Replacing POM file version in main POM."
updatePomVersions -w $WORK_DIR -o $OLD_POM_VER -n $NEW_DEVELOP_VER

git commit -a -m "$JIRA_TICKET: Updating POM versions to $NEW_DEVELOP_VER in develop"

echo "Summary:"
echo "-Created r/$BRANCH_NAME from develop"
echo "-Updated local develop POMs to $NEW_DEVELOP_POM_VER"
echo "We can push these changes to the public repo."
yesno -d no "Do you want this script to do that automatically for you?" push
if [[ "$push" ]]; then
    git push origin r/$BRANCH_NAME
    git push origin develop
fi
