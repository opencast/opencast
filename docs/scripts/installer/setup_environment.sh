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

# Setup opencast storage directories
# TODO: Uncomment the following lines -and remove the next two- once the correct defaults for the directories are set in the config files in svn
## Read default from the config file
#default_dir=$(grep "^org\.opencastproject\.storage\.dir=.*$" $GEN_PROPS | cut -d '=' -f 2)
#ask -d "$default_dir" "Where would you like the matterhorn directories to be stored?" oc_dir
#: ${oc_dir:=$OC_DIR}
ask -d "$OC_DIR" "In which directory would you like to install matterhorn?" oc_dir
echo
ask -d "$CONTENT_DIR" "Where would you like the matterhorn content to be stored?" content_dir
echo

export OC_DIR=$oc_dir;
export CONTENT_DIR=$content_dir

# Create the directories
mkdir -p "$content_dir"/cache
mkdir -p "$content_dir"/config
mkdir -p "$content_dir"/volatile
mkdir -p "$content_dir"/cache/captures

# Point the CA's fileservice at the right dir
sed -i "s#/tmp/opencast#${oc_dir//#/\\#}#" $SOURCE/modules/matterhorn-capture-agent-impl/src/main/resources/OSGI-INF/capture-files.xml

# Establish their permissions
chown -R $USERNAME:$USERNAME "$oc_dir"
chmod -R 770 "$oc_dir"
chown -R $USERNAME:$USERNAME "$content_dir"
chmod -R 770 "$content_dir"


# Write the directory name to the agent's config file
#                 this->_______<- escapes the dots in the property key, so that sed doesn't not interpret them as wildcards
sed -i "s#^${STORAGE_KEY//./\\.}=.*\$#${STORAGE_KEY}=$content_dir#" "$GEN_PROPS"

# Define capture agent name by using the hostname
if [ "$LINUX_DIST" == "Ubuntu" ] && ( [ "$MATTERHORN_PROFILE" == "all" ] || [ $MATTERHORN_PROFILE == "capture" ] ); then
  unset agentName
  ask -d "$(hostname)" -f '^[a-zA-Z0-9_\-]*$' -e "Please use only alphanumeric characters, hyphen(-) and underscore(_)"\
      "Please enter the agent name" agentName

  sed -i "s/^${AGENT_NAME_KEY//./\\.}=.*$/${AGENT_NAME_KEY}=$agentName/" "$CAPTURE_PROPS"
  echo
fi

# Prompt for the agent IP address
echo "If your network uses a proxy you may have to set the capture agent manually in order to use the Matterhorn confidence monitoring interfaces.
If you are not using a proxy you do not have to configure the network address, it will be autodetected in normal operation."
echo
yesno -d yes "Configure network address now?" config

if [[ "$config" ]]; then
    # User wants to set up the address
    default_ip=$(ifconfig | grep -m 1 -o '[012]*[0-9]*[0-9]\.[012]*[0-9]*[0-9]\.[012]*[0-9]*[0-9]\.[012]*[0-9]*[0-9]' | cut -d '
' -f 1)
    #Get the URL instead the IP
    default_url=`nslookup $default_ip | grep name | cut -d ' ' -f 3 | sed 's/\(.*\)./\1/'`
    
    if [ ! $default_url ]; then 
      default_url=$default_ip
    fi
    
    # Gets the osgi service port, where felix is running
    service_port=$(grep "^${FELIX_PORT_KEY//./\\.}=.*$" "$GEN_PROPS" | cut -d '=' -f 2)
    # Asks for the new IP
    # TODO: this property also admits a URL, rather than an IP. Remove the filter and check this still works for an arbitrary address.
    # > And how do we check if that URL is correct or not?
    ask -a -d "$default_url" "Detected URL is $default_url. Press [enter] to accept, or enter a new address" url
    
    ask -a -d "8080" "On which port is your server running?" port
    if [ $port = "80" ]; then
        server_port=""
    else
        server_port=":$port"
    fi

    matterhorn_url="http://$url$server_port"

    sed -i "s#^${SERVER_URL_KEY//./\\.}=.*\$#${SERVER_URL_KEY}=$matterhorn_url#" "$GEN_PROPS"

    # Configure Tenancy fileservice
    sed -i "s#^${TENANT_SERVER_KEY//./\\.}=.*\$#${TENANT_SERVER_KEY}=$url#" "$TENANCY_DEFAULT_PROPS"
    sed -i "s#^${TENANT_PORT_KEY//./\\.}=.*\$#${TENANT_PORT_KEY}=$port#" "$TENANCY_DEFAULT_PROPS"
fi

# Prompt for the distributed servers if needed

if [ $MATTERHORN_PROFILE != "all" ]; then
  # Admin Server
  ask -a -d $url "What is the URL of your admin server? " admin_url
  ask -a -d $port "On which port is your admin server running?" admin_port
  if [ $admin_port = "80" ]; then
      admin_port=""
  else
      admin_port=":$admin_port"
  fi
  admin_server="http://$admin_url$admin_port" 

  sed -i "s#^${ADMIN_SERVER_KEY//./\\.}=.*\$#${ADMIN_SERVER_KEY}=$admin_server#" "$GEN_PROPS"

  # Admin Server
  ask -a -d $url "What is the URL of your engage server? " engage_url
  ask -a -d $port "On which port is your engage server running?" engage_port
  if [ $engage_port = "80" ]; then
      engage_port=""
  else
      engage_port=":$engage_port"
  fi
  engage_server="http://$engage_url$engage_port" 

  sed -i "s#^${ENGAGE_SERVER_KEY//./\\.}=.*\$#${ENGAGE_SERVER_KEY}=$engage_server#" "$GEN_PROPS"
fi

if [ "$MATTERHORN_PROFILE" == "engage" ] || [ "$MATTERHORN_PROFILE" == "all" ]; then
  yesno -d yes "Do you want to use a streaming server?" streaming
  if [ $streaming ]; then
    rtmp_url="rtmp://$url/matterhorn-engage"
    ask -a -d $rtmp_url "Please enter the complete URL of your matterhorn streaming application?" streaming_url
    sed -i "s/#.*${STREAMING_URL_KEY//./\\.}=.*/${STREAMING_URL_KEY}=${streaming_url//\//\\/}/" "$GEN_PROPS"
    yesno -d no "Do you want to change the default directory where the files for the streaming server are stored?" change_streaming_dir
    if [ $change_streaming_dir ]; then
        ask -a "Please enter the directory where you want to store the streaming files?" streaming_dir
        sed -i "s/#.*${STREAMING_DIRECTORY_KEY//./\\.}=.*/${STREAMING_DIRECTORY_KEY}=${streaming_dir//\//\\/}/" "$GEN_PROPS"
    else
        sed -i "s/#.*${STREAMING_DIRECTORY_KEY//./\\.}=.*/${STREAMING_DIRECTORY_KEY}=\${org.opencastproject.storage.dir}\/streams/" "$GEN_PROPS"
    fi
  fi

  yesno -d yes "Do you want to use separate webserver for the media distribution?" downloading
  if [ $downloading ]; then
    http_url="http://$url/static"
    ask -a -d $http_url "Please enter the complete URL of your separate webserver?" downloading_url
    sed -i "s/.*${DOWNLOAD_URL_KEY//./\\.}=.*/${DOWNLOAD_URL_KEY}=${downloading_url//\//\\/}/" "$GEN_PROPS"
    yesno -d no "Do you want to change the default directory where the files for the webserver are stored?" change_download_dir
    if [ $change_download_dir ]; then
      ask -a "Please enter the directory where you want to store the files?" download_dir
      sed -i "s/.*${DOWNLOAD_DIRECTORY_KEY//./\\.}=.*/${DOWNLOAD_DIRECTORY_KEY}=${download_dir//\//\\/}/" "$GEN_PROPS"
    fi
  fi

fi

if [ "$MATTERHORN_PROFILE" == "all" ]; then
  yesno -d no "Do you want to use an external database server?" database
else 
  if [ "$MATTERHORN_PROFILE" != "capture" ]; then
    database="true";
  fi
fi 

if [ $database ]; then
  choose -t "Which database are you intend to use?" -? "The internal H2 database is not supported for distributed installations. If you want to use another database please configure it manually" -o list -- "MySQL 5.x" "PostgreSQL 8.x+" "Configure manually" database_brand

  case "$database_brand" in
    0) db_vendor="MySQL"
       db_driver="com.mysql.jdbc.Driver"
       db_ddl="true"
       db_url_prefix="jdbc:mysql://"
    ;;
    1) db_vendor="PostgreSQL"
       db_driver="org.postgresql.Driver"
       db_ddl="true"
       db_url_prefix="jdbc:postgresql://"
    ;;
    2) no_database="true"
    ;;
  esac

  if [ ! $no_database ]; then
    sed -i "s/#.*${DATABASE_VENDOR_KEY//./\\.}=.*/${DATABASE_VENDOR_KEY}=$db_vendor/" "$GEN_PROPS"
    sed -i "s/#.*${DATABASE_DRIVER_KEY//./\\.}=.*/${DATABASE_DRIVER_KEY}=$db_driver/" "$GEN_PROPS"
    sed -i "s/#.*${DATABASE_AUTOGEN_KEY//./\\.}=.*/${DATABASE_AUTOGEN_KEY}=$db_ddl/" "$GEN_PROPS"
    ask -a -d "localhost" "Database URL?" db_url
    sed -i "s/#.*${DATABASE_URL_KEY//./\\.}=.*/${DATABASE_URL_KEY}=${db_url_prefix//\//\\/}${db_url//./\\.}/" "$GEN_PROPS"
    ask -a -d "matterhorn" "Database user name?" db_user
    sed -i "s/#.*${DATABASE_USER_KEY//./\\.}=.*/${DATABASE_USER_KEY}=$db_user/" "$GEN_PROPS"
    ask -a -d "opencast" "Database password?" db_password
    sed -i "s/#.*${DATABASE_PASSWORD_KEY//./\\.}=.*/${DATABASE_PASSWORD_KEY}=$db_password/" "$GEN_PROPS"
  fi

fi

# Prompt for the URL where the core lives.
# TODO: (or maybe not) Support a distributed core would mean to set different URLs separately, rather than this centralized one
## Read default from the config file
#DEFAULT_CORE_URL=$(grep "^${CORE_URL_KEY//./\\.}=.*$" $CAPTURE_PROPS | cut -d '=' -f 2)
if [ "$LINUX_DIST" == "Ubuntu" ] && [ $MATTERHORN_PROFILE == "capture" ]; then
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
fi
# Set up maven and felix enviroment variables in the user session
echo -n "Setting up maven and felix enviroment for $USERNAME... "
EXPORT_M2_REPO="export M2_REPO=${M2_REPO}"
EXPORT_FELIX_HOME="export FELIX_HOME=${FELIX_HOME}"
EXPORT_JAVA_HOME="export JAVA_HOME=${JAVA_HOME}"
EXPORT_SOURCE_HOME="export MATTERHORN_SOURCE=${SOURCE}"
ALIAS_DEPLOY_MACRO="alias deploy=\"mvn install -DdeployTo=$FELIX_HOME/matterhorn\""
ALIAS_REDEPLOY_MACRO="alias redeploy=\"mvn clean && deploy\""

# Removing commented out settings for ffmpeg etc for CentOS
if [ "$LINUX_DIST" == "Centos" ]; then
  sed -i "s/#.*${MEDIA_INSPECTION_KEY}/${MEDIA_INSPECTION_KEY}/" "$GEN_PROPS"
  sed -i "s/#.*${TEXTEXTRACTION_KEY}/${TEXTEXTRACTION_KEY}/" "$GEN_PROPS"
  sed -i "s/#.*${OCROPUS_KEY}/${OCROPUS_KEY}/" "$GEN_PROPS"
  sed -i "s/#.*${FFMPEG_KEY}/${FFMPEG_KEY}/" "$GEN_PROPS"
  sed -i "s/#.*${QTEMBEDDER_KEY}/${QTEMBEDDER_KEY}/" "$GEN_PROPS"
fi 

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
chmod -R 770 "$CONTENT_DIR"
chown -R $USERNAME:$USERNAME "$CONTENT_DIR"

# Add a link to the capture agent folder in the user home folder
[[ -e "${HOME}/${OC_DIR##*/}" ]] || (ln -s $OC_DIR "$HOME" && chown -h $USERNAME:$USERNAME "$HOME"/"${OC_DIR##*/}")
[[ -e "${HOME}/${CONTENT_DIR##*/}" ]] || (ln -s $CONTENT_DIR "$HOME" && chown -h $USERNAME:$USERNAME "$HOME"/"${CONTENT_DIR##*/}")
echo "Done"

# Set up the deinstallation script
echo -n "Creating the cleanup script... "
SRC_LIST_BKP="$SRC_LIST"."$BKP_SUFFIX"
sed -i "s#^USER=.*\$#USER=$USERNAME#" "$CLEANUP"
sed -i "s#^HOME=.*\$#HOME=$HOME#" "$CLEANUP"
sed -i "s#^SRC_LIST=.*\$#SRC_LIST=$SRC_LIST#" "$CLEANUP"
sed -i "s#^SRC_LIST_BKP=.*\$#SRC_LIST_BKP=$SRC_LIST_BKP#" "$CLEANUP"
sed -i "s#^OC_DIR=.*\$#OC_DIR=$oc_dir#" "$CLEANUP"
sed -i "s#^CA_DIR=.*\$#CA_DIR=$CA_DIR#" "$CLEANUP"
sed -i "s#^RULES_FILE=.*\$#RULES_FILE=$DEV_RULES#" "$CLEANUP"
sed -i "s#^CA_DIR=.*\$#CA_DIR=$CA_DIR#" "$CLEANUP"
sed -i "s#^STARTUP_SCRIPT=.*\$#STARTUP_SCRIPT=$STARTUP_SCRIPT#" "$CLEANUP"

# Write the uninstalled package list to the cleanup.sh template
sed -i "s#^PKG_LIST=.*\$#PKG_LIST=\"$(echo $(cat $PKG_BACKUP))\"#" "$CLEANUP"

echo "Done"

# Prompt for the location of the cleanup script
if [ "$LINUX_DIST" == "Ubuntu" ]; then 
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
fi
