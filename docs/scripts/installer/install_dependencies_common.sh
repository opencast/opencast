#!/bin/bash

##########################################################################
# Install and configure the necessary dependencies for the capture agent #
##########################################################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

# Include the functions
. ${FUNCTIONS}

# Setup felix
echo -n "Downloading Felix... "
while [[ true ]]; do 
    if [[ ! -s ${FELIX_FILENAME} ]]; then
	wget -q ${FELIX_URL}
    fi
    # On success, uncompress the felix files in their location
    if [[ $? -eq 0 ]]; then
	echo -n "Uncompressing... "
	dir_name=$(tar tzf ${FELIX_FILENAME} | grep -om1 '^[^/]*')
	tar xzf ${FELIX_FILENAME}
	if [[ $? -eq 0 ]]; then
	    rm -rf $FELIX_HOME
	    mv ${dir_name%/} -T $FELIX_HOME
	    mv $FELIX_FILENAME $CA_DIR
	    #mkdir -p ${FELIX_HOME}/load
	    echo "Done"
	    break
	fi
    fi
    # Else, ask for the actions to take
    echo
    yesno -d yes "Error retrieving the Felix files from the web. Retry?" retry
    if [[ "$retry" ]]; then
    	echo -n "Retrying... "
    else
    	echo "You must download Felix manually and install it under $OC_DIR, in order for matterhorn to work"
	break
    fi
done

# Setup jv4linfo
if [[ ! -e "$JV4LINFO_PATH/$JV4LINFO_LIB" ]]; then
    mkdir -p $JV4LINFO_DIR
    cd $JV4LINFO_DIR

    echo -n "Installing jv4linfo... "
    if [[ ! -e "$JV4LINFO_JAR" ]]; then
	wget -q $JV4LINFO_URL/$JV4LINFO_JAR
    fi
    jar xf $JV4LINFO_JAR
    cd jv4linfo/src
    # The ant build script has a hardcoded path to the openjdk, this sed line will
    # switch it to be whatever is defined in JAVA_HOME
    sed -i '74i\\t<arg value="-fPIC"/>' build.xml
    sed -i "s#\"/usr/lib/jvm/java-6-openjdk/include\"#\"$JAVA_HOME/include\"#g" build.xml
    
    ant -lib ${JAVA_HOME}/lib &> /dev/null
    if [[ "$?" -ne 0 ]]; then
	echo "Error building libjv4linfo.so"
	exit 1
    fi
    cp ../lib/$JV4LINFO_LIB $JV4LINFO_PATH
    
    cd $WORKING_DIR
    
echo "Done"
else
    echo "libjv4linfo.so already installed"
fi

# Setup ntdp
echo 
ask -d "$DEFAULT_NTP_SERVER" "Which NTP server would you like to use?" server
sed -i "s#^server .*#server $server#" $NTP_CONF
echo "NTP server set to $server"
echo "Consider editing the file $NTP_CONF for manually changing the default NTP server or adding more servers to the list"
