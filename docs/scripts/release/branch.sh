#!/bin/bash

FUNCTIONS="functions.sh"
. ${FUNCTIONS}

#THIS SCRIPT SHOULD NOT BE USED IF YOU ARE CREATING A FEATURE BRANCH

#The name of the new branch, without the release branch prefix
#E.g. BRANCH_NAME=1.3.x
BRANCH_NAME=

#The jira ticket this work is being done under (must be open)
JIRA_TICKET=

#=======You should not need to modify anything below this line=================

WORK_DIR=../../../

#Reset this script so that the modifications do not get committed
git checkout -- branch.sh

#Make sure we are on develop, then create a branch
git checkout develop
git checkout -b r/$BRANCH_NAME

git commit -a -m "$JIRA_TICKET Updated pom.xml files to reflect correct version.  Done via docs/scripts/release/branch.sh"

echo "Summary:"
echo "-Created r/$RELEASE_VER from develop"
echo "We can push these changes to the public repo, and delete the remote branch for you as well."
yesno -d no "Do you want this script to do that automatically for you?" push
if [[ "$push" ]]; then
    git push origin r/$RELEASE_VER
fi
