#! /bin/bash

###################################
# Set matterhorn to start on boot #
###################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

# Copy startup scripts to the bin directory. 
cp $FELIX_HOME/docs/scripts/init/matterhorn_init_d.sh $FELIX_HOME/bin/
ACTUAL_SCRIPT=$FELIX_HOME/bin/matterhorn_init_d.sh

ln -s $ACTUAL_SCRIPT $STARTUP_SCRIPT

# Set up the init script variables
sed -i "s#^MATTERHORN=.*\$#MATTERHORN=$OC_DIR#g" $ACTUAL_SCRIPT
sed -i "s#^MATTERHORN_USER=.*#MATTERHORN_USER=$USERNAME#g" $ACTUAL_SCRIPT
sed -i "s#^CA=.*#CA=$CA_DIR#g" $ACTUAL_SCRIPT
sed -i "s#^M2_REPOSITORY=.*#M2_REPOSITORY=$M2_REPO#g" $ACTUAL_SCRIPT
sed -i "s/^IS_CA=false/IS_CA=true/g" $ACTUAL_SCRIPT

# Set the appropriate permissions
chown root:root $STARTUP_SCRIPT
chmod 755 $STARTUP_SCRIPT
update-rc.d "${STARTUP_SCRIPT##*/}" defaults 99 01
