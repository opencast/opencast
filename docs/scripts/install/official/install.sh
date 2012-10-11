#!/bin/bash

######################################
# Configure Matterhorn Capture Agent #
######################################

# Variables section ####################################################################################################################

# General variables

# Maximum number of attempts to stablish the matterhorn user password
MAX_PASSWD_ATTEMPTS=3

# Install Matterhorn with the following profile (all, capture, admin, work, engage)
export MATTERHORN_PROFILE="all";
# Use a shared workspace or workspace stub
export WORKSPACE_VERSION="workspace-stub"

# Default name for the matterhorn user 
export USERNAME=matterhorn
# Storage directory for the matterhorn-related files
export OC_DIR=/opt/matterhorn
# Storage directory for the matterhorn generated content
export CONTENT_DIR=/opt/matterhorn/content
# Name for the directory where the matterhorn-related files will be stored
export CA_DIR=$OC_DIR/capture-agent
# Directory where the source code will be downloaded to
export SOURCE=$CA_DIR/matterhorn-source
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
export BRANCHES_URL=$SVN_URL/branches
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
export PKG_LIST="alsa-utils
v4l-conf
ivtv-utils
curl
maven2
openjdk-6-jdk
subversion
wget
openssh-server
gcc
ntp
acpid"

# yum packages for CentOS
export CENTOS_PKG_LIST="ant ant-nodeps ntp curl openssh-server subversion gcc"

# yum Package to install the JDK under CentOS
export JAVA_CENTOS="java-1.6.0-devel"

# This is a backup file to preserve the list of installed packages in case something fails and the script is re-launched
# The name should start with a '.' so that it is a hidden file and it is not erased with the rest of the files when a new execution starts
export PKG_BACKUP=$WORKING_DIR/.installed_pkgs
# Default output file extension for a video capture device
export DEFAULT_VIDEO_EXTENSION="mpg"
# Default output file extension for an audio device
export DEFAULT_AUDIO_EXTENSION="mp2"
# 1-based index default option for the device flavor
export DEFAULT_FLAVOR=1
# Lists of flavors the user can choose from to assign to a certain device
export FLAVORS="presenter/source presentation/source"
# Default size for a capture device queue (in megabytes)
export DEFAULT_QUEUE_SIZE=512

# Name of the file containing the felix files
export FELIX_FILENAME=org.apache.felix.main.distribution-3.2.2.tar.gz
# URL where the previous file can be fetched
export FELIX_URL=http://archive.apache.org/dist/felix/$FELIX_FILENAME
# Subdir under the user home where FELIX_HOME is
export FELIX_HOME=$OC_DIR/felix
# Name of the file containing the maven 3 files
export MAVEN_FILENAME="apache-maven-3.0.4-bin.tar.gz"
# URL where the previous file can be fetched
export MAVEN_URL="http://www.apache.org/dist//maven/binaries/$MAVEN_FILENAME"
# Subdir under the user home where FELIX_HOME is
export MAVEN_HOME=/usr/local/bin/maven
# Memory settings for maven to build matterhorn
export MAVEN_OPTS='-Xms512m -Xmx960m -XX:PermSize=128m -XX:MaxPermSize=512m'
# Path under FELIX_HOME where the general matterhorn configuration
export GEN_PROPS=$FELIX_HOME/conf/config.properties
# Path under FELIX_HOME where the default multi-tenancy configuration is located
export TENANCY_DEFAULT_PROPS=$FELIX_HOME/load/org.opencastproject.organization-mh_default_org.cfg
# Path under FELIX_HOME where the capture agent properties are
export CAPTURE_PROPS=$FELIX_HOME/conf/services/org.opencastproject.capture.impl.ConfigurationManager.properties
# Directory UNDER FELIX HOME where the felix filex will be deployed
export DEPLOY_DIR=matterhorn

# Path to where the installed jvm's are
export JAVA_PREFIX=/usr/lib/jvm
# A regexp to filter the right jvm directory from among all the installed ones
# The chosen JAVA_HOME will be $JAVA_PREFIX/`ls $JAVA_PREFIX | grep $JAVA_PATTERN`
export JAVA_PATTERN=java-6-openjdk                                           
                                                                         
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
# The admin server URL in the felix config file (config.properties)
export ADMIN_SERVER_KEY="org.opencastproject.admin.ui.url"
# The engage server URL in the felix config file (config.properties)
export ENGAGE_SERVER_KEY="org.opencastproject.engage.ui.url"
# Port where felix is running in the felix config file (config.properties)
export FELIX_PORT_KEY="org.osgi.service.http.port"
# URL for the machine hosting the service registry, in the felix config file (config.properties)
export SERVICE_REG_KEY="org.opencastproject.serviceregistry.url"
# Agent name in the capture properties file
export AGENT_NAME_KEY="capture.agent.name"
# URL of the core machine inthe capture properties file
export CORE_URL_KEY="org.opencastproject.capture.core.url"
# Database driver settings in the felix config file (config.properties)
export DATABASE_DRIVER_KEY="org.opencastproject.db.jdbc.driver"
# Database driver settings in the felix config file (config.properties)
export DATABASE_VENDOR_KEY="org.opencastproject.db.vendor"
# Database driver settings in the felix config file (config.properties)
export DATABASE_AUTOGEN_KEY="org.opencastproject.db.ddl.generation"
# Database driver settings in the felix config file (config.properties)
export DATABASE_URL_KEY="org.opencastproject.db.jdbc.url"
# Database driver settings in the felix config file (config.properties)
export DATABASE_USER_KEY="org.opencastproject.db.jdbc.user"
# Database driver settings in the felix config file (config.properties)
export DATABASE_PASSWORD_KEY="org.opencastproject.db.jdbc.pass"
# Streaming server URL settings in the felix config file (config.properties)
export STREAMING_URL_KEY="org.opencastproject.streaming.url"
# Streaming server URL settings in the felix config file (config.properties)
export STREAMING_DIRECTORY_KEY="org.opencastproject.streaming.directory"
# Streaming server URL settings in the felix config file (config.properties)
export DOWNLOAD_URL_KEY="org.opencastproject.download.url"
# Streaming server URL settings in the felix config file (config.properties)
export DOWNLOAD_DIRECTORY_KEY="org.opencastproject.download.directory"
# Mediainspect path settings in the felix config file (config.properties)
export MEDIA_INSPECTION_KEY="org.opencastproject.inspection.mediainfo.path"
# Tesseract path settings in the felix config file (config.properties)
export TEXTEXTRACTION_KEY="org.opencastproject.textanalyzer.tesseract.path"
# FFMPEG path settings in the felix config file (config.properties)
export FFMPEG_KEY="org.opencastproject.composer.ffmpegpath"
# OCROPUS path settings in the felix config file (config.properties)
export OCROPUS_KEY="org.opencastproject.textanalyzer.ocrocmd"
# QT embedder path settings in the felix config file (config.properties)
export QTEMBEDDER_KEY="org.opencastproject.composer.qtembedderpath"
# Streaming server URL settings in the felix config file (config.properties)
export DOWNLOAD_DIRECTORY_KEY="org.opencastproject.download.directory"
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
# Configuration key for the server URL/IP in an multi-tenancy file
export TENANT_SERVER_KEY="server"
# Configuration key for the server URL/IP in an multi-tenancy file
export TENANT_PORT_KEY="port"

# Required scripts for installation
SETUP_USER=./setup_user.sh
SETUP_DEVICES=./setup_devices.sh
INSTALL_DEPENDENCIES=./install_dependencies_common.sh
INSTALL_DEPENDENCIES_UBUNTU=./install_dependencies_ubuntu.sh
INSTALL_DEPENDENCIES_CENTOS=./install_dependencies_centos.sh
SETUP_SOURCE=./setup_source.sh
SETUP_ENVIRONMENT=./setup_environment.sh
SETUP_BOOT=./setup_boot.sh
# This one is exported so that the other scripts can include the functions if necessary
export FUNCTIONS=./functions.sh
# This one is exported because it has to be modified by another script
export CLEANUP=./cleanup.sh

SCRIPTS=( "$SETUP_USER" "$SETUP_DEVICES" "$INSTALL_DEPENDENCIES" "$SETUP_ENVIRONMENT"\
          "$SETUP_SOURCE" "$SETUP_BOOT" "$CLEANUP" "$FUNCTIONS" "$INSTALL_DEPENDENCIES_UBUNTU" "$INSTALL_DEPENDENCIES_CENTOS" )

# Location of these install scripts, relative to the source code's root directory
SCRIPTS_EXT=docs/scripts/install/official

# The subsidiary scripts will check for this variable to check they are being run from here
export INSTALL_RUN=true

# End of variables section########################################################################################

# Change the working directory to a temp directory under /tmp
# Deletes its contents, in case it existed previously (MH-3797)
mkdir -p $WORKING_DIR
rm -f $WORKING_DIR/*
cd $WORKING_DIR


# Check for the necessary scripts and download them from the svn location
unset missing
for f in "${REQUIRED[@]}"; do
    # Check if the script is in the directory where the install.sh script was launched
    if [[ -e $START_PATH/$f ]]; then
	# ... and copies it to the working directory
	cp $START_PATH/$f $WORKING_DIR
	chmod +x $f
    else
	missing=("${missing[@]}" "$f")
    fi  
done

if [[ "${missing[@]}" ]]; then
    echo "Error. Some required files scripts are missing:"
    for f in "${missing[@]}"; do
	echo -e "\t$f"
    done
    echo -e "\nPlease make sure you got all the contents from the folder '$SCRIPTS_EXT'"
    exit 2
fi


# Include the functions
. ${FUNCTIONS}

# Choose/create the matterhorn user (WARNING: The initial perdiod (.) MUST be there so that the script can export several variables)
. ${SETUP_USER}

# Checks if this script is being run with root privileges, exiting if it doesn't
if [[ `id -u` -ne 0 ]]; then
    echo "This script requires root privileges. Please run it with the sudo command or log in to the root user and try again"
    exit 1
fi

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

# Log the distribution
echo "# Output of cat /etc/*-release" >> $LOG_FILE
cat /etc/*-release >> $LOG_FILE

# set the Distribution 

if [ "`cat /etc/*-release | grep -e Ubuntu -e Debian`" ] 
  then 
    export LINUX_DIST="Ubuntu"
else 
  if [ "`cat /etc/*-release | grep -e CentOS -e 'Red Hat Enterprise'`" ]
    then 
      export LINUX_DIST="Centos"
  else 
    echo "Unsupported Linux Distribution. Please install Matterhorn manually."
    exit 1
  fi
fi

choose -t "What kind of Matterhorn server do you want to install?" -? "Matterhorn can be build with different profiles." -o list -- "All-in-One installation" "Admin modules" "Worker modules" "Engage/Player modules" profile
case "$profile" in
        0) MATTERHORN_PROFILE="all"
        ;;
        1) MATTERHORN_PROFILE="admin"
        ;;
        2) MATTERHORN_PROFILE="work"
        ;;
        3) MATTERHORN_PROFILE="engage"
        ;;
esac

if ([ "$MATTERHORN_PROFILE" == "admin" ] || [ "$MATTERHORN_PROFILE" == "work" ] || [ "$MATTERHORN_PROFILE" == "engage" ]); then
        yesno -d no "Do you want to use a shared workspace between your Matterhorn servers?" workspace
        if [ $workspace ]; then
                WORKSPACE_VERSION="workspace"
	else 
	  WORKSPACE_VERSION="workspace-stub"
        fi
fi

# Create the directory where all the capture-agent-related files will be stored
mkdir -p $CA_DIR

# Install the 3rd party dependencies (WARNING: The initial perdiod (.) MUST be there so that the script can export several variables)
if [ "$LINUX_DIST" == "Centos" ]; then
  . ${INSTALL_DEPENDENCIES_CENTOS}
fi 
if [ "$LINUX_DIST" == "Ubuntu" ]; then
  . ${INSTALL_DEPENDENCIES_UBUNTU}
fi 
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
	echo "Error setting up the matterhorn code. Contact matterhorn-users@opencastproject.org for assistance."
	exit 1
    fi
    source_ok=true

    
    # Set up user environment
    ${SETUP_ENVIRONMENT}
    if [[ "$?" -ne 0 ]]; then
	echo "Error setting up the environment for $USERNAME. Contact matterhorn-users@opencastproject.org for assistance."
	exit 1
    fi
	
    yesno -d no "Do you want to build the third party tools after matterhorn has been build? This can take up to an hour depending on your computer and your internet-connection" build_3rdparty

    # Build matterhorn
    echo -e "\n\nProceeding to build the matterhorn source. This may take a long time. Press any key to continue...\n\n"
    read -n 1 -s
    
    unset build_ok
    while [[ ! "$build_ok" ]]; do
	cd $SOURCE
	if [ "$MATTERHORN_PROFILE" == "work" ]; then
	  PROFILE="-Pworker,ingest,serviceregistry,$WORKSPACE_VERSION"
	fi
	if [ "$MATTERHORN_PROFILE" == "admin" ]; then
	  PROFILE="-Padmin,dist-stub,engage-stub,worker-stub,workspace,serviceregistry"
	fi
	if [ "$MATTERHORN_PROFILE" == "engage" ]; then
	  PROFILE="-Pengage,dist,serviceregistry,$WORKSPACE_VERSION"
	fi
	su $USERNAME -c "mvn clean install $PROFILE -DdeployTo=${HOME}/${OC_DIR##*/}/${FELIX_HOME##*/}/${DEPLOY_DIR}"
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
		    echo -e "\nError building the matterhorn code. Contact matterhorn-users@opencastproject.org for assistance."
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

if [ $build_3rdparty ]; then 
  cd $SOURCE/docs/scripts/3rd_party/
  ./do-all
  if [[ "$?" -ne 0 ]]; then
  	echo "Installing the 3rd-party tools failed. Please run $SOURCE/docs/scripts/3rd_party/do-all. I f you still encounter problems Contact matterhorn-users@opencastproject.org for assistance.\nThe rest of Matterhorn system installed without further problems."
  fi
fi

# Log the contents of /etc/issue
echo >> $LOG_FILE
echo "# Contents in /etc/issue" >> $LOG_FILE
cat /etc/issue >> $LOG_FILE

echo -e "\n\n\nMatterhorn succesfully installed\n\n\n"

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
    echo -e "\n\nThe system will start automatically after rebooting the system."
    echo "However, you can start it manually by running ${FELIX_HOME}/bin/start_matterhorn.sh"
    echo "Please direct your questions / suggestions / etc. to the list: matterhorn@opencastproject.org"
    echo
    read -n 1 -s -p "Hit any key to exit..."
    clear

fi
