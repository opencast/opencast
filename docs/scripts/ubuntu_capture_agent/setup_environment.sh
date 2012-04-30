#! /bin/bash

#########################################
# Setup environment for matterhorn user #
#########################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

. "${FUNCTIONS}"

#Defines the regex we use in this script to determine if a URL is valid
VALID_URL_REGEX='^http://[a-zA-Z0-9\._\-]+(:[0-9]*[0-9]*[0-9]*[0-9]*[0-9])?$'

# Create the directories
mkdir -p "$OC_DIR"/cache
mkdir -p "$OC_DIR"/config
mkdir -p "$OC_DIR"/volatile
mkdir -p "$OC_DIR"/cache/captures

# Point the CA's fileservice at the right dir
sed -i "s#/tmp/opencast#${OC_DIR//#/\\#}#" $FELIX_HOME/modules/matterhorn-capture-agent-impl/src/main/resources/OSGI-INF/capture-files.xml

# Establish their permissions
chown -R $USERNAME:$USERNAME "$OC_DIR"
chmod -R 770 "$OC_DIR"

# Write the directory name to the agent's config file
#                 this->_______<- escapes the dots in the property key, so that sed doesn't not interpret them as wildcards
sed -i "s#^${STORAGE_KEY//./\\.}=.*\$#${STORAGE_KEY}=$OC_DIR#" "$GEN_PROPS"

# Define capture agent name by using the hostname
unset agentName
ask -d "$(hostname)" -f '^[a-zA-Z0-9_\-]*$' -e "Please use only alphanumeric characters, hyphen(-) and underscore(_)"\
    "Please enter the agent name" agentName

sed -i "s/^${AGENT_NAME_KEY//./\\.}=.*$/${AGENT_NAME_KEY}=$agentName/" "$CAPTURE_PROPS"
echo

# Prompt for the agent IP address
echo "If your network uses a proxy you may have to set the capture agent manually in order to use the Matterhorn confidence monitoring interfaces.
If you are not using a proxy you do not have to configure the network address, it will be autodetected in normal operation."
echo
yesno -d yes "Configure network address now?" config

if [[ "$config" ]]; then
    # User wants to set up the address
    default_ip=$(ifconfig | grep -m 1 -o '[012]*[0-9]*[0-9]\.[012]*[0-9]*[0-9]\.[012]*[0-9]*[0-9]\.[012]*[0-9]*[0-9]' | cut -d '
' -f 1)
    # Gets the osgi service port, where felix is running
    service_port=$(grep "^${FELIX_PORT_KEY//./\\.}=.*$" "$GEN_PROPS" | cut -d '=' -f 2)
    # Asks for the new IP
    # TODO: this property also admits a URL, rather than an IP. Remove the filter and check this still works for an arbitrary address.
    # > And how do we check if that URL is correct or not?
    ask -a -f $VALID_URL_REGEX\
        "Detected IP is $default_ip. Press [enter] to accept, or enter a new address including protocol and optional port (eg: http://$default_ip or http://$default_ip:8080)" ip
    : ${ip:="http://"$default_ip}
    sed -i "s#^${SERVER_URL_KEY//./\\.}=.*\$#${SERVER_URL_KEY}=$ip#" "$GEN_PROPS"
fi

# Prompt for the URL where the core lives.
# TODO: (or maybe not) Support a distributed core would mean to set different URLs separately, rather than this centralized one
## Read default from the config file
#DEFAULT_CORE_URL=$(grep "^${CORE_URL_KEY//./\\.}=.*$" $CAPTURE_PROPS | cut -d '=' -f 2)
ask -d "$DEFAULT_CORE_URL" -f $VALID_URL_REGEX "Please enter the URL and port (if ingestion is not on port 80) of the machine hosting the ingestion service in the form of http://URL:PORT" core
sed -i "s#^${CORE_URL_KEY//./\\.}=.*\$#${CORE_URL_KEY}=$core#" "$CAPTURE_PROPS"
#Use this value to update the location of the service registry, too                                                                                          
sed -i "s#^${SERVICE_REG_KEY//./\\.}=.*\$#${SERVICE_REG_KEY}=$core/$SERVICE_REG_SUFFIX#" "$GEN_PROPS"

# Prompt for the time between two updates of the recording schedule
default_poll=$(grep "${SCHEDULE_POLL_KEY}" "$CAPTURE_PROPS" | cut -d '=' -f 2) #<-- This reads the default value from the config file
[[ $default_poll -lt 1 ]] && default_poll=1

# The value in the file is no longer in seconds, but in minutes, so no conversion needs to be done
ask -d "$default_poll" -f '^0*[1-9][0-9]*$' -h '? - more info' -e "Invalid value"\
    "Please enter the time (in minutes) between two updates of the agent's recording schedule" poll
# Write the value to the file, adjusting the value from minutes to seconds
sed -i "s/${SCHEDULE_POLL_KEY//./\\.}=.*$/${SCHEDULE_POLL_KEY}=$poll/" "$CAPTURE_PROPS"

# Set up maven and felix enviroment variables in the user session
echo -n "Setting up maven and felix enviroment for $USERNAME... "
EXPORT_M2_REPO="export M2_REPO=${M2_REPO}"
EXPORT_FELIX_HOME="export FELIX_HOME=${FELIX_HOME}"
EXPORT_JAVA_HOME="export JAVA_HOME=${JAVA_HOME}"
EXPORT_SOURCE_HOME="export MATTERHORN_SOURCE=${FELIX_HOME}"
ALIAS_DEPLOY_MACRO="alias deploy=\"mvn install -DdeployTo=$FELIX_HOME/matterhorn\""
ALIAS_REDEPLOY_MACRO="alias redeploy=\"mvn clean && deploy\""

grep -e "${EXPORT_M2_REPO}" "$HOME"/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${EXPORT_M2_REPO}" >> "$HOME"/.bashrc
fi
grep -e "${EXPORT_FELIX_HOME}" "$HOME"/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${EXPORT_FELIX_HOME}" >> "$HOME"/.bashrc
fi
grep -e "${EXPORT_JAVA_HOME}" "$HOME"/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${EXPORT_JAVA_HOME}" >> "$HOME"/.bashrc
fi
grep -e "${EXPORT_SOURCE_HOME}" "$HOME"/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${EXPORT_SOURCE_HOME}" >> "$HOME"/.bashrc
fi
grep -e "${ALIAS_DEPLOY_MACRO}" "$HOME"/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${ALIAS_DEPLOY_MACRO}" >> "$HOME"/.bashrc
fi
grep -e "${ALIAS_REDEPLOY_MACRO}" "$HOME"/.bashrc &> /dev/null
if [ "$?" -ne 0 ]; then
    echo "${ALIAS_REDEPLOY_MACRO}" >> "$HOME"/.bashrc
fi

#chown $USERNAME:$USERNAME $HOME/.bashrc

# Change permissions and owner of the $CA_DIR folder
chmod -R 770 "$OC_DIR"
chown -R $USERNAME:$USERNAME "$OC_DIR"

# Add a link to the capture agent folder in the user home folder
[[ -e "${HOME}/${OC_DIR##*/}" ]] || (ln -s $OC_DIR "$HOME" && chown -h $USERNAME:$USERNAME "$HOME"/"${OC_DIR##*/}")

echo "Done"

# Set up the deinstallation script
echo -n "Creating the cleanup script... "
SRC_LIST_BKP="$SRC_LIST"."$BKP_SUFFIX"
sed -i "s#^USER=.*\$#USER=$USERNAME#" "$CLEANUP"
sed -i "s#^HOME=.*\$#HOME=$HOME#" "$CLEANUP"
sed -i "s#^SRC_LIST=.*\$#SRC_LIST=$SRC_LIST#" "$CLEANUP"
sed -i "s#^SRC_LIST_BKP=.*\$#SRC_LIST_BKP=$SRC_LIST_BKP#" "$CLEANUP"
sed -i "s#^OC_DIR=.*\$#OC_DIR=$OC_DIR#" "$CLEANUP"
sed -i "s#^CA_DIR=.*\$#CA_DIR=$CA_DIR#" "$CLEANUP"
sed -i "s#^RULES_FILE=.*\$#RULES_FILE=$DEV_RULES#" "$CLEANUP"
sed -i "s#^CA_DIR=.*\$#CA_DIR=$CA_DIR#" "$CLEANUP"
sed -i "s#^STARTUP_SCRIPT=.*\$#STARTUP_SCRIPT=$STARTUP_SCRIPT#" "$CLEANUP"

# Write the uninstalled package list to the cleanup.sh template
sed -i "s#^PKG_LIST=.*\$#PKG_LIST=\"$(echo $(cat $PKG_BACKUP))\"#" "$CLEANUP"

echo "Done"

# Prompt for the location of the cleanup script
echo
echo "A cleanup script is being created that will uninstall the capture agent and undo any changes made by this installation process"
while [[ true ]]; do
    ask -d "$START_PATH" "Please enter the location to store the cleanup script" location
    if [[ -d "$location" ]]; then
        if [[ ! -e "$location/$CLEANUP" ]]; then
            break;
        fi
        yesno -d no "File $location/$CLEANUP already exists. Do you wish to overwrite it?" response
        if [[ "$response" ]]; then
            break;
        fi
    else
        echo -n "Invalid location. $location "
	if [[ -e "$location" ]]; then
	    echo "is not a directory"
	else 
	    echo "does not exist"
	fi
    fi
done


cp "$CLEANUP" "${location:=$START_PATH}"
chown --reference="$location" "$location"/"$CLEANUP"
