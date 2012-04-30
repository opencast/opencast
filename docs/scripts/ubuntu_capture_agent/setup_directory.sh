#! /bin/bash

#########################################
# Setup the directory to install the CA #
#########################################

. ${FUNCTIONS}

# Setup opencast storage directories
# TODO: Uncomment the following lines -and remove the next two- once the correct defaults for the directories are set in the config files in svn
## Read default from the config file
#default_dir=$(grep "^org\.opencastproject\.storage\.dir=.*$" $GEN_PROPS | cut -d '=' -f 2)
#ask -d "$default_dir" "Where would you like the matterhorn directories to be stored?" oc_dir
#: ${oc_dir:=$OC_DIR}
ask -d "$OC_DIR" "Where would you like the matterhorn directories to be stored?" oc_dir
echo

export OC_DIR=$oc_dir
export FELIX_HOME="$oc_dir/felix"
export CONF_DIR=$FELIX_HOME/etc
export GEN_PROPS=$CONF_DIR/config.properties
export CAPTURE_PROPS=$CONF_DIR/services/org.opencastproject.capture.impl.ConfigurationManager.properties

echo "OC DIR: $OC_DIR"
echo "Felix Home: $FELIX_HOME"
