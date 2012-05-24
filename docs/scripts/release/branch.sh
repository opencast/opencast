#!/bin/bash

#THIS SCRIPT SHOULD NOT BE USED IF YOU ARE CREATING A FEATURE BRANCH
#Instead, just svn copy from your source branch to your dev branch
#This script modifies the POM files for trunk.

#The comments in this file assume you are creating a branch from trunk
#Eg:  The defaults in the comments here will take the current version of 
#http://opencast.jira.com/svn/MH/trunk and tag it as 
#http://opencast.jira.com/svn/MH/tags/1.3.x

#The name of the new branch
#E.g. BRANCH_NAME=1.3.x
BRANCH_NAME=

#The version the POMs are in trunk right now.
#E.g. OLD_POM_VER=1.3-SNAPSHOT
OLD_POM_VER=

#The new version of our release as it will show up in the branch directory
#E.g. NEW_POM_VER=1.4-SNAPSHOT
NEW_POM_VER=

#The jira ticket this work is being done under (must be open)
JIRA_TICKET=

#The scratch directory where the work is performed.  Make sure you have enough
#space.  Should not already include a subdirectory of $WORK_DIR/$JIRA_TICKET
WORK_DIR=/tmp/

#=======You should not need to modify anything below this line=================

#The actual working dir
WORK_DIR=$WORK_DIR/$JIRA_TICKET

#Matterhorn base URL
SVN_URL=https://opencast.jira.com/svn/MH

TRUNK_URL=$SVN_URL/trunk
BRANCH_URL=$SVN_URL/branches/$BRANCH_NAME

#TODO: We should use an svn switch instead because while we are working on this
#tag to get it ready people might think it has been released.
echo "Creating new branch by copying $TRUNK_URL to $BRANCH_URL."
svn copy $TRUNK_URL $BRANCH_URL -m "$JIRA_TICKET Creating $BRANCH_NAME Branch"

echo "Creating scratch dir and checking out release sources"
pushd .
rm -rf $WORK_DIR
mkdir $WORK_DIR
cd $WORK_DIR
svn co $TRUNK_URL .

echo "Replacing POM file version in main POM."
sed -i "s/<version>$OLD_POM_VER/<version>$NEW_POM_VER/" $WORK_DIR/pom.xml

for i in modules/matterhorn-*
do
    echo " Module: $i"
    if [ -f $WORK_DIR/$i/pom.xml ]; then
        sed -i "s/<version>$OLD_POM_VER/<version>$NEW_POM_VER/" $WORK_DIR/$i/pom.xml
        sleep 1
    fi
done
svn commit -m "$JIRA_TICKET Updated pom.xml files to reflect correct version.  Done via docs/scripts/release/branch.sh"

#Return to previous environment and cleanup
popd
rm -rf $WORK_DIR
