#!/bin/bash

FUNCTIONS="functions.sh"
. ${FUNCTIONS}

#The comments in this file assume you are creating a tag from a release branch
#This script does *not* push any changes, so it should be safe to experiment with locally

#The version the POMs are in the develop branch.
#E.g. BRANCH_VER=1.3-SNAPSHOT
BRANCH_VER=

#The new version of our release as it will show up in the tags
#E.g. RELEASE_VER=1.3-rc5
RELEASE_VER=

#The next version that will be going into development.  This is only relevant for final releases!
#E.g. NEXT_VER=1.4-SNAPSHOT
NEXT_VER=

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
TAG_VER=$RELEASE_VER

echo "Replacing POM file version in main POM."
sed -i "s/<version>$BRANCH_VER/<version>$TAG_VER/" $WORK_DIR/pom.xml

#TODO: Make this into a function, wrap the has_checked in a loop to prevent premature exits
for i in $WORK_DIR/modules/matterhorn-*
do
    echo " Module: $i"
    if [ -f $i/pom.xml ]; then
        sed -i "s/<version>$BRANCH_VER/<version>$TAG_VER/" $i/pom.xml
    fi
done

yesno -d no "NOTE: This script has made changes to your POM files.  Please ensure that it only made changes to the Matterhorn version number.  In rare cases some of the dependencies have the same version numbers, and the modification done above does *not* understand that it should not also change those versions.  Manual inspection of the changeset is required before continuing.  Have you finished checking all of the modifications?" has_checked
if [[ ! "$has_checked" ]]; then 
    exit 1
fi

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
    yesno -d no "We need to push the changes we just made to the public repo so that the git flow command to finish the release will succeed, do you wish to proceed?  Answering no and rerunning this script with the same options is safe, if you want to look around first." cont
    if [[ ! "$cont" ]]; then 
      exit 1
    fi
    git push --all

    echo "Now we are going to finish the release.  Nothing further will be automatically pushed by this script, so again it is now safe to stop at any point."
    yesno -d no "We do, however need a GPG key available.  If you have one setup please continue, otherwise create one.  Do you have a GPG key ready to go?" has_key
    if [[ ! "$has_key" ]]; then 
      exit 1
    fi

    git flow release finish -s -m "Finishing release $TAG_VER" $TAG_VER

    echo "Replacing POM file version in main POM."
    sed -i "s/<version>$RELEASE_VER/<version>$NEXT_VER/" $WORK_DIR/pom.xml

    for i in $WORK_DIR/modules/matterhorn-*
    do
        echo " Module: $i"
        if [ -f $i/pom.xml ]; then
            sed -i "s/<version>$RELEASE_VER/<version>$NEXT_VER/" $i/pom.xml
        fi
    done
    yesno -d no "NOTE: This script has made changes to your POM files.  Please ensure that it only made changes to the Matterhorn version number.  In rare cases some of the dependencies have the same version numbers, and the modification done above does *not* understand that it should not also change those versions.  Manual inspection of the changeset is required before continuing.  Have you finished checking all of the modifications?" has_checked
    if [[ ! "$has_checked" ]]; then 
        exit 1
    fi
    git commit -a -m "Updating POM versions to $NEXT_VER as part of $JIRA_TICKET"
    ;;
esac

echo "Please verify that things look correct, and then push the new branch and tag upstream!"
echo "Run: git push --all && git push --tags"
