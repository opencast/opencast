#! /bin/bash

#########################################
# Setup the directory to install the CA #
#########################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

# Setup opencast storage directories
ask -d "$OC_DIR" "Where would you like the matterhorn directories to be stored?" oc_dir
echo

export OC_DIR=$oc_dir
export FELIX_HOME="$oc_dir/felix"
export CONF_DIR=$FELIX_HOME/etc
export GEN_PROPS=$CONF_DIR/config.properties
export CAPTURE_PROPS=$CONF_DIR/services/org.opencastproject.capture.impl.ConfigurationManager.properties
