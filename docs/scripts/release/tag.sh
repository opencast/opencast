#!/bin/bash

FUNCTIONS="functions.sh"
. ${FUNCTIONS}

#The comments in this file assume you are creating a tag from a release branch
#This script does *not* push any changes, so it should be safe to experiment with locally

#The version the POMs are in the develop branch.
#E.g. BRANCH_POM_VER=1.3-SNAPSHOT
BRANCH_POM_VER=

#The new version of our release as it will show up in the tags
#E.g. RELEASE_VER=1.3-rc5
RELEASE_VER=

#The jira ticket this work is being done under (must be open)
JIRA_TICKET=

#=======You should not need to modify anything below this line=================

WORK_DIR=$(echo `pwd` | sed 's/\(.*\/.*\)\/docs\/scripts\/release/\1/g')

#Reset this script so that the modifications do not get committed
git checkout -- tag.sh

#Get the current branch name
curBranch=`git status | grep "On branch" | sed 's/\# On branch \(.*\)/\1/'`

choose -t "Are you cutting an RC, or a final release of $curBranch?" "RC" "Final Release" RELEASE_TYPE

#The version we want the poms to be, usually the same as RELEASE_VER
TAG_POM_VER=$RELEASE_VER

echo "Replacing POM file version in main POM."
sed -i "s/<version>$BRANCH_POM_VER/<version>$TAG_POM_VER/" $WORK_DIR/pom.xml

for i in $WORK_DIR/modules/matterhorn-*
do
    echo " Module: $i"
    if [ -f $i/pom.xml ]; then
        sed -i "s/<version>$BRANCH_POM_VER/<version>$TAG_POM_VER/" $i/pom.xml
    fi
done
case "$RELEASE_TYPE" in
0)
    # Release candidate
    git checkout -b r/$RELEASE_VER
    git commit -a -m "Creating $RELEASE_VER branch to contain POM changes and tag"
    git tag -s $RELEASE_VER -m "Creating $RELEASE_VER branch and tag as part of $JIRA_TICKET"
    git checkout $curBranch
    ;;
1)
    #Final release
    git commit -a -m "Committing $RELEASE_VER directly to $curBranch in preparation for git flow release command."
    yesno -d no "We are about to push the changes we just made to the public repo, do you wish to proceed?  Kiling this script and rerunning it with the same options is safe, if you want to look around first." cont
    if [[ ! "$cont" ]]; then 
      exit 1
    fi
    git push --all
    git flow release finish `echo $curBranch | sed 's/r\/\(.*\)/\1/'`
    ;;
esac

echo "Please verify that things look correct, and then push the new branch and tag upstream!"
