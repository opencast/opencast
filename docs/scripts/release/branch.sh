#!/bin/bash

#The name of the branch in SVN that we are looking to turn into a release
#E.g. 1.0.x if we are doing a 1.0.x maintenance release
BRANCH_NAME=1.3.x

#The version the POMs are in the development branch.  E.g. 1.0-SNAPSHOT
#if we are doing a 1.0.x maintenance release 
BRANCH_POM_VER=1.3-SNAPSHOT

#The new version of our release as it will show up in the tags directory in
#svn, e.g. 1.0.2 for a maintenance release on 1.0.2
RELEASE_VER=1.3-rc5

#The version we want the poms to be, usually the same as RELEASE_VER
TAG_POM_VER=$RELEASE_VER

#The jira ticket this work is being done under (must be open)
JIRA_TICKET=MH-8492

#Scratch directory where work should be done in, should not already include a
#subdirectory of $WORK_DIR/$JIRA_TICKET
WORK_DIR=/tmp/
WORK_DIR=$WORK_DIR/$JIRA_TICKET

#Matterhorn base URL
SVN_URL=https://opencast.jira.com/svn/MH

BRANCH_URL=$SVN_URL/branches/$BRANCH_NAME
TAG_URL=$SVN_URL/tags/$RELEASE_VER

#TODO: We should use an svn switch instead because while we are working on this
#tag to get it ready people might think it has been released.
echo "Creating new tag by copying " $BRANCH_URL " to " $TAG_URL "."
svn copy $BRANCH_URL $TAG_URL -m "$JIRA_TICKET Creating $TAG_NAME Tag"

echo "Creating scratch dir and checking out release sources"
pushd .
rm -rf $WORK_DIR
mkdir $WORK_DIR
cd $WORK_DIR
svn co $TAG_URL .

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
svn commit -m "$JIRA_TICKET Updated pom.xml files to reflect correct version.  Done via docs/scripts/release/tag.sh"

#Return to previous environment and cleanup
popd
rm -rf $WORK_DIR
