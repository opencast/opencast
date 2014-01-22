#!/bin/bash

FUNCTIONS="functions.sh"
. ${FUNCTIONS}

#The comments in this file assume you are creating a tag from a release branch
#This script does *not* push any changes, so it should be safe to experiment with locally

#The version the POMs are in the release branch.
#E.g. BRANCH_VER=1.3-SNAPSHOT
BRANCH_VER=

#The new version of our release as it will show up in the tags
#E.g. RELEASE_VER=1.3-rc5
RELEASE_VER=

#The version the POMs are in develop.  This is only relevant for final releases!
#E.g. DEVELOP_VER=1.3-SNAPSHOT
DEVELOP_VER=

#The version that develop should have.  This is only relevant for final releases!
#E.g. NEXT_VER=1.4-SNAPSHOT
NEXT_VER=

#The jira ticket this work is being done under (must be open)
JIRA_TICKET=

#=======You should not need to modify anything below this line=================

WORK_DIR=../../../

yesno -d no "We need a GPG key available.  If you have one setup please continue, otherwise create one.  Do you have a GPG key ready to go?" has_key
if [[ ! "$has_key" ]]; then 
  exit 1
fi

#Reset this script so that the modifications do not get committed
git checkout -- tag.sh

choose -t "Are you cutting an RC, or a final release of $curBranch?" "RC" "Final Release" RELEASE_TYPE

echo "Replacing POM file version in the POMs."
sed -i "s/<version>$BRANCH_VER/<version>$RELEASE_VER/" $WORK_DIR/pom.xml

for i in $WORK_DIR/modules/matterhorn-*
do
    echo " Module: $i"
    if [ -f $i/pom.xml ]; then
        sed -i "s/<version>$BRANCH_VER/<version>$RELEASE_VER/" $i/pom.xml
    fi
done

while [[ true ]]; do
  yesno -d no "NOTE: This script has made changes to your POM files.  Please ensure that it only made changes to the Matterhorn version number.  In rare cases some of the dependencies have the same version numbers, and the modification done above does *not* understand that it should not also change those versions.  Manual inspection of the changeset is required before continuing.  Have you finished checking all of the modifications?" has_checked
  if [[ "$has_checked" ]]; then 
      break
  fi
done

case "$RELEASE_TYPE" in
0)
    # Release candidate
    git commit -a -m "$JIRA_TICKET: Committing $RELEASE_VER to branch to contain POM changes and tag"
    git tag -s $RELEASE_VER -m "Release $RELEASE_VER"
    git revert --no-edit HEAD

    echo "Summary:"
    echo "-Modified pom files, and tagged $RELEASE_VER"
    echo "We can push these changes to the public repo if you want."
    yesno -d no "Do you want this script to do that automatically for you?" push
    if [[ "$push" ]]; then
        git push origin
        git push --tags origin
    fi
    ;;
1)
    #Final release
    git commit -a -m "$JIRA_TICKET: Committing $RELEASE_VER directly to $curBranch in preparation for final release."
    git tag -s $RELEASE_VER -m "Release $RELEASE_VER"
    git revert --no-edit HEAD

    git checkout master
    git merge --no-ff r/$RELEASE_VER
    git checkout develop
    git merge --no-ff r/$RELEASE_VER
    git branch -d r/$RELEASE_VER

    echo "Replacing POM file version in main POM."
    sed -i "s/<version>$DEVELOP_VER/<version>$NEXT_VER/" $WORK_DIR/pom.xml

    while [[ true ]]; do
      yesno -d no "NOTE: This script has made changes to your POM files.  Please ensure that it only made changes to the Matterhorn version number.  In rare cases some of the dependencies have the same version numbers, and the modification done above does *not* understand that it should not also change those versions.  Manual inspection of the changeset is required before continuing.  Have you finished checking all of the modifications?" has_checked
      if [[ "$has_checked" ]]; then 
          break
      fi
    done
    git commit -a -m "$JIRA_TICKET: Updating POM versions to $NEXT_VER in develop"

    echo "Summary:"
    echo "-Merged r/$RELEASE_VER into master"
    echo "-Merged r/$RELEASE_VER into develop"
    echo "-Deleted local r/$RELEASE_VER"
    echo "-Updated local develop POMs to $NEXT_VER"
    echo "We can push these changes to the public repo, and delete the remote branch for you as well."
    yesno -d no "Do you want this script to do that automatically for you?" push
    if [[ "$push" ]]; then
        git push origin develop
        git push origin master
        git push --tags origin
        git push origin :r/$RELEASE_VER
    fi
    ;;
esac
