#!/bin/bash

######################################
# Configure Matterhorn Capture Agent #
######################################

# Variables section ####################################################################################################################

# General variables

# Maximum number of attempts to stablish the matterhorn user password
MAX_PASSWD_ATTEMPTS=3

# Default name for the matterhorn user 
export USERNAME=matterhorn
# Storage directory for the matterhorn-related files
export OC_DIR=/opt/matterhorn
# Name for the directory where the matterhorn-related files will be stored
export CA_DIR=$OC_DIR/capture-agent
# Location of the felix docs in the source code
export FELIX_DOCS="docs/felix"

# Path from where this script is run initially
export START_PATH=$PWD
# Directory where this script will be run
export WORKING_DIR=/tmp/cainstallscript

# Root for the source code repository
export SVN_URL=http://opencast.jira.com/svn/MH
# Extension for the SVN_URL to reach the trunk
export TRUNK_URL=$SVN_URL/trunk
# Extension for the SVN_URL to reach the branches
export BRANCHES_URL=$SVN_URL/branches/1.3.x
# Extension for the SVN_URL to reach the tags
export TAGS_URL=$SVN_URL/tags

# Default URL from where scripts and java source will be dowloaded
export SRC_DEFAULT=$TRUNK_URL

# File containing the rules to be applied by udev to the configured devices -- not a pun!
export DEV_RULES=/etc/udev/rules.d/matterhorn.rules
# File name for the bash script containing the device configuration routine
export CONFIG_SCRIPT=device_config.sh
# Default value for the core url
export DEFAULT_CORE_URL=http://localhost:8080
# Location of the file 'sources.list'
export SRC_LIST=/etc/apt/sources.list
# Suffix to be appended to the backup file for sources.list
export BKP_SUFFIX=backup
# Path of the script which is set up to configure and run felix upon startup
export STARTUP_SCRIPT=/etc/init.d/matterhorn
# URL of the default Ubuntu mirror where the packages will be downloaded from
export DEFAULT_MIRROR=http://archive.ubuntu.com/ubuntu
# URL of the default Ubuntu 'security' mirror
export DEFAULT_SECURITY=http://security.ubuntu.com/ubuntu
# URL of the default Ubuntu 'partner' mirror
export DEFAULT_PARTNER=http://archive.canonical.com/ubuntu

# Logging file
export LOG_FILE=$START_PATH/install_info.txt
# URL where the log file is sent if the user consent
export TRACKING_URL=http://www.opencastproject.org/form/tracking

# Third-party dependencies variables
# Packages that are installed by default (one per line --please note the quotation mark at the end!!!)
# These may be changed by the $SETUP_DEPENDENCIES script depending on the version of Ubuntu. 
export PKG_LIST="acpid
alsa-utils
curl
gcc
gstreamer0.10-alsa
gstreamer0.10-plugins-base
gstreamer0.10-plugins-good
gstreamer0.10-plugins-ugly
gstreamer0.10-plugins-ugly-multiverse
gstreamer0.10-ffmpeg
ivtv-utils
libglib2.0-dev
maven2
ntp
openssh-server
subversion
openjdk-6-jdk
v4l-conf
wget"

# Versions of Ubuntu that we have package lists for. The above defaults will be used
export UBUNTU_10_10="Ubuntu 10.10 \n \l"
export UBUNTU_10_10_PACKAGES_FILE="Ubuntu-10-10.packages"
export UBUNTU_11_04="Ubuntu 11.04 \n \l"
export UBUNTU_11_04_PACKAGES_FILE="Ubuntu-11-04.packages"
export UBUNTU_11_10="Ubuntu 11.10 \n \l"
export UBUNTU_11_10_PACKAGES_FILE="Ubuntu-11-10.packages"
export PACKAGE_LIST_DEFAULT_FILE="default.packages"


# Packages that require the user approval to be installed (Please note the quotation mark at the end!!!)
# There should be one package per line, but several packages may be included if they need to be treated 'as a block'
# Those lines ending with a "+" will be interpreted as required by the system. If the user choose not to install them, the installation will exit.
export BAD_PKG_LIST="gstreamer0.10-plugins-bad gstreamer0.10-plugins-bad-multiverse +"
# Reasons why each of the "bad" packages should be installed (one per line, in the same order as the bad packages)
export BAD_PKG_REASON="Provide support for h264 and mpeg2 codecs, which are patent-encumbered. Temporarily required for a basic system"

# This is a backup file to preserve the list of installed packages in case something fails and the script is re-launched
# The name should start with a '.' so that it is a hidden file and it is not erased with the rest of the files when a new execution starts
export PKG_BACKUP=$WORKING_DIR/.installed_pkgs
# Default output file extension for a video capture device
export DEFAULT_VIDEO_EXTENSION="mp4"
# Default output file extension for an audio device
export DEFAULT_AUDIO_EXTENSION="mp2"
# 1-based index default option for the device flavor
export DEFAULT_FLAVOR=1
# Lists of flavors the user can choose from to assign to a certain device
export FLAVORS="presenter/source presentation/source"
# Default size for a capture device queue (in megabytes)
export DEFAULT_QUEUE_SIZE=512

# Subdir under the user home where FELIX_HOME is
export FELIX_HOME=$OC_DIR/felix

# The location of the configuration files. 
export CONF_DIR=$FELIX_HOME/etc
# Path under FELIX_HOME where the general matterhorn configuration
export GEN_PROPS=$CONF_DIR/config.properties
# Path under FELIX_HOME where the capture agent properties are
export CAPTURE_PROPS=$CONF_DIR/services/org.opencastproject.capture.impl.ConfigurationManager.properties
# Directory UNDER FELIX HOME where the felix filex will be deployed
export DEPLOY_DIR=matterhorn

# Path to where the installed jvm's are
export JAVA_PREFIX=/usr/lib/jvm
# A regexp to filter the right jvm directory from among all the installed ones
# The chosen JAVA_HOME will be $JAVA_PREFIX/`ls $JAVA_PREFIX | grep $JAVA_PATTERN`
export JAVA_PATTERNS="java-6-sun java-7-oracle java-6-openjdk"
                           
# Path to the maven2 repository, under the user home
export M2_SUFFIX=.m2/repository

# Default ntp server
export DEFAULT_NTP_SERVER=ntp.ubuntu.com
# Location for the ntp configuration files
export NTP_CONF=/etc/ntp.conf

# Location of the jv4linfo jar
export JV4LINFO_URL=http://luniks.net/luniksnet/download/java/jv4linfo
# Name of the jv4linfo file
export JV4LINFO_JAR=jv4linfo-0.2.1-src.jar
# Shared object required by the jv4linfo jar to function
export JV4LINFO_LIB=libjv4linfo.so
# Directory where the shared object will be copied so that jvm can find it. In other words, it must be in the java.library.path
export JV4LINFO_PATH=/usr/lib
# Directory where the jv4linfo-related files are stored
export JV4LINFO_DIR=$CA_DIR/jv4linfo
                                                                         
## Help messages
# Help for the device friendly name prompt
export FRIENDLY_NAMES_HELP="The friendly name (e.g. \"screen\", or \"professor\") will identify the device in the system and will be displayed in the user interfaces for controlling this device.
It can't contain spaces or punctuation."
# Help for the device output file extension
export EXTENSIONS_HELP="Please see http://opencast.jira.com/browse/MH-4523 for more details about why the output file extension needs to be specified for each captured file as they are injested by the core."
# Help for the device flavor prompt
export FLAVORS_HELP="Devices that capture the screen are usually \"Presentation\", while devices that capture the instructor or audience are usually \"Presenter\".
Setting this value correctly is important to ensure video content is processed and distributed correctly."
# Help for the device queue prompt
export QUEUE_HELP="This value is used internally by the device pipeline and basically represents the amount of memory reserved for the device buffer.
A value bigger than the default may be selected, as long as the machine has enough memory to allocate it.
Please see Framerates, Bitrates and Queues at http://opencast.jira.com/wiki/display/MH/Tweaking+1.0+(Advanced) for more information."

# Help for the prompt for the time between two schedule polls
export POLL_HELP="A too short value will cause excessive polls to the core, while a too long value may cause some recordings are not notified to the agent before their expected starting time.
Testers should choose a value of 1 minute so that they can schedule recordings to start immediately"

## Keys for several properties in the config files
# Storage dir in the general felix config file (config.properties)
export STORAGE_KEY="org.opencastproject.storage.dir"
# CA's own url in the general felix config file (config.properties)
export SERVER_URL_KEY="org.opencastproject.server.url"
# Port where felix is running in the felix config file (config.properties)
export FELIX_PORT_KEY="org.osgi.service.http.port"
# URL for the machine hosting the service registry, in the felix config file (config.properties)
export SERVICE_REG_KEY="org.opencastproject.serviceregistry.url"
# Agent name in the capture properties file
export AGENT_NAME_KEY="capture.agent.name"
# URL of the core machine inthe capture properties file
export CORE_URL_KEY="org.opencastproject.capture.core.url"
# Time between two subsequent updates of the capture schedule in the capture properties file
export SCHEDULE_POLL_KEY="capture.schedule.remote.polling.interval"
# Prefix for properties related to a certain device in the capture properties file
export DEVICE_PREFIX="capture.device"
# Suffix for the service registry URL, used in the felix config file (config.properties)
export SERVICE_REG_SUFFIX="services"
# Suffix for the source location of a certain device in the capture properties file
export SOURCE_SUFFIX="src"
# Suffix for the output location of a certain device in the capture properties file
export OUT_SUFFIX="outputfile"
# Suffix for the flavor of a certain device in the capture properties file
export FLAVOR_SUFFIX="flavor"
# Suffix for the type of device in the capture properties file
export TYPE_SUFFIX="type"
# Suffix for the size of the queue for a certain device in the capture properties file
export QUEUE_SUFFIX="buffer.bytes"
# Suffix for the comma-separated list of all the devices attached to a capture agent in the capture properties file
export LIST_SUFFIX="names"

# Required scripts for installation
SETUP_USER=./setup_user.sh
SETUP_DEVICES=./setup_devices.sh
SETUP_DEPENDENCIES=./setup_dependencies.sh
INSTALL_DEPENDENCIES=./install_dependencies.sh
SETUP_DIRECTORY=./setup_directory.sh
SETUP_SOURCE=./setup_source.sh
SETUP_ENVIRONMENT=./setup_environment.sh
SETUP_BOOT=./setup_boot.sh
# This one is exported so that the other scripts can include the functions if necessary
export FUNCTIONS=./functions.sh
# This one is exported because it has to be modified by another script
export CLEANUP=./cleanup.sh

SCRIPTS=( "$SETUP_USER" "$SETUP_DEVICES" "$SETUP_DEPENDENCIES" "$INSTALL_DEPENDENCIES"\
          "$SETUP_ENVIRONMENT" "$SETUP_DIRECTORY" "$SETUP_SOURCE" "$SETUP_BOOT" "$CLEANUP" "$FUNCTIONS")
SCRIPTS_EXT=docs/scripts/ubuntu_capture_agent

# The subsidiary scripts will check for this variable to check they are being run from here
export INSTALL_RUN=true

# End of variables section########################################################################################



# Checks if this script is being run with root privileges, exiting if it doesn't
if [[ `id -u` -ne 0 ]]; then
    echo "This script requires root privileges. Please run it with the sudo command or log in to the root user and try again"
    exit 1
fi

# Change the working directory to a temp directory under /tmp
# Deletes its contents, in case it existed previously (MH-3797)
mkdir -p $WORKING_DIR
rm -f $WORKING_DIR/*
cd $WORKING_DIR

# Log the technical outputs                                                                                                                                 
echo "# Output of uname -a" > $LOG_FILE
uname -a >> $LOG_FILE
echo >> $LOG_FILE
echo "# Total memory" >> $LOG_FILE
echo $(cat /proc/meminfo | grep -m 1 . | cut -d ':' -f 2) >> $LOG_FILE
echo >> $LOG_FILE
echo "# Processor(s) model name" >> $LOG_FILE
model_name="$(cat /proc/cpuinfo | grep -m 1 'model name' | cut -d ':' -f 2)"
physical="$(cat /proc/cpuinfo | grep -m 1 'cores' | cut -d ':' -f 2)"
virtual="$(cat /proc/cpuinfo | grep -m 1 'siblings' | cut -d ':' -f 2)"
echo "$model_name ($physical physical core(s), $virtual virtual cores)" >> $LOG_FILE

# If wget isn't installed, get it from the ubuntu software repo
wget foo &> /dev/null
if [ $? -eq 127 ]; then
    apt-get -y --force-yes install wget &>/dev/null
    if [ $? -ne 0 ]; then
	echo "Couldn't install the necessary command 'wget'. Please try to install it manually and re-run this script"
	exit 1
    fi
fi

# Check for the necessary scripts and download them from the svn location
# Using C-like syntax in case file names have whitespaces
for (( i = 0; i < ${#SCRIPTS[@]}; i++ )); do
    f=${SCRIPTS[$i]}
	# Check if the script is in the directory where the install.sh script was launched
	if [[ -e $START_PATH/$f ]]; then
	    # ... and copies it to the working directory
	    cp $START_PATH/$f $WORKING_DIR
	else
	    # The script is not in the initial directory, so try to download it from the opencast source page
	    wget $SRC_DEFAULT/$SCRIPTS_EXT/$f &> /dev/null	    
	    # Check the file is downloaded
	    if [[ $? -ne 0 ]]; then
		echo "Couldn't retrieve the script $f from the repository. Try to download it manually and re-run this script."
		exit 2
	    fi
	fi  
    chmod +x $f
done

# Include the functions
. ${FUNCTIONS}

# Choose/create the matterhorn user (WARNING: The initial perdiod (.) MUST be there so that the script can export several variables)
. ${SETUP_USER}

# Choose the directory to install the capture agent. 
. ${SETUP_DIRECTORY}

# Create the directory where all the capture-agent-related files will be stored
mkdir -p $CA_DIR

# Setup dependencies for individual versions of Ubuntu. 
. ${SETUP_DEPENDENCIES}

# Install the 3rd party dependencies (WARNING: The initial perdiod (.) MUST be there so that the script can export several variables)
. ${INSTALL_DEPENDENCIES}
if [[ "$?" -ne 0 ]]; then
    echo "Error installing the 3rd party dependencies."
    exit 1
fi

unset source_ok
while [[ ! "$source_ok" ]] ; do 
    # Set up the matterhorn code --doesn't build yet!
    ${SETUP_SOURCE}
    if [[ "$?" -ne 0 ]]; then
	echo "Error setting up the matterhorn code. Contact matterhorn@opencastproject.org for assistance."
	exit 1
    fi
    source_ok=true
    
    # Setup properties of the devices
    ${SETUP_DEVICES}
    if [[ "$?" -ne 0 ]]; then
	echo "Error setting up the capture devices. Contact matterhorn@opencastproject.org for assistance."
	exit 1
    fi
    
    # Set up user environment
    ${SETUP_ENVIRONMENT}
    if [[ "$?" -ne 0 ]]; then
	echo "Error setting up the environment for $USERNAME. Contact matterhorn@opencastproject.org for assistance."
	exit 1
    fi
    
    # Build matterhorn
    echo -e "\n\nProceeding to build the capture agent source. This may take a long time. Press any key to continue...\n\n"
    read -n 1 -s
    
    unset build_ok
    while [[ ! "$build_ok" ]]; do
	cd $FELIX_HOME
	su $USERNAME -c "mvn clean install -Pcapture,serviceregistry-stub -DdeployTo=${FELIX_HOME}"
	if [[ "$?" -ne 0 ]]; then
	    echo
	    choose -t "Error building the matterhorn code. What do you wish to do?" "Download another source" "Retry build" "Exit" src_opt
	    case "$src_opt" in
		0) 
		    # Forces repeating the outer loop (download sources and building)
		    unset source_ok
		    # This is to exit this loop only. Doesn't mean the code is correctly build.
		    build_ok=true
		    ;;
	        #1)
	        #    Does nothing. The inner loop repeats (building the source only)
                #    ;;
		2) 
		    echo -e "\nError building the matterhorn code. Contact matterhorn@opencastproject.org for assistance."
		    exit 3
		    ;;
	    esac
	else
	    build_ok=true
	fi
    done

    cd $WORKING_DIR
done

# Set up the file to run matterhorn automatically on startup
${SETUP_BOOT}

# Log the contents of /etc/issue
echo >> $LOG_FILE
echo "# Contents in /etc/issue" >> $LOG_FILE
cat /etc/issue >> $LOG_FILE

echo -e "\n\n\nCapture Agent succesfully installed\n\n\n"

# Prompt the user to send 
#yesno -d yes -? "This feedback includes information about the hardware you have installed Matterhorn on, and the configuration options you have used."\
#      -h "? - more info" "Would you like to provide anonymous feedback about your installation experience to the Matterhorn development team?" send

#while [[ "$send" ]]; do
#    curl -F "file=@$LOG_FILE" "$TRACKING_URL" > /dev/null
#    if [[ $? -ne 0 ]]; then
#	echo "Error. The feedback information couldn't be sent."
#	yesno -d yes "Do you wish to retry?" send
#    else
#	echo "Thanks for your input!"
#	break
#    fi
#done
#echo
echo

yesno -d yes "It is recommended to reboot the system after installation. Do you wish to do it now?" reboot

if [[ "$reboot" ]]; then 
    echo "Rebooting... "
    reboot > /dev/null
else
    echo -e "\n\nThe capture agent will start automatically after rebooting the system."
    echo "However, you can start it manually by running ${FELIX_HOME}/bin/start_matterhorn.sh"
    echo "Please direct your questions / suggestions / etc. to the list: matterhorn@opencastproject.org"
    echo
    read -n 1 -s -p "Hit any key to exit..."
    clear

fi
