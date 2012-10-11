#! /bin/bash

###################################
# Set matterhorn to start on boot #
###################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

ORIGINAL_SCRIPT=$SOURCE/docs/init/matterhorn_init_d.sh

cp $ORIGINAL_SCRIPT $STARTUP_SCRIPT

# Set up the init script variables
sed -i "s#^MATTERHORN=.*\$#MATTERHORN=$OC_DIR#g" $STARTUP_SCRIPT
sed -i "s#^FELIX=.*\$#FELIX=$FELIX_HOME#g" $STARTUP_SCRIPT
sed -i "s#^MATTERHORN_USER=.*#MATTERHORN_USER=$USERNAME#g" $STARTUP_SCRIPT
sed -i "s#^M2_REPOSITORY=.*#M2_REPOSITORY=$M2_REPO#g" $STARTUP_SCRIPT

# Set the appropriate permissions
chown root:root $STARTUP_SCRIPT
chmod 755 $STARTUP_SCRIPT

if [ $LINUX_DIST == "Ubuntu" ]; then
  update-rc.d "${STARTUP_SCRIPT##*/}" defaults 99 01
else 
  chkconfig matterhorn on
fi
