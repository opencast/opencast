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

if [ ! `which javac` ]; then
  echo "It is recommended to use the Sun/Oracle Java Development Kit (JDK) 6. This is unfortunately not provided in the CentOS/Red Hat repositories. You can try to use OpenJDK instead. If no JDK is installed at all the installation will fail"
  yesno -d yes "Do you want to use the OpenJDK?" install_open_jdk
  if [ $install_open_jdk ]; then 
    yum -y install $JAVA_CENTOS
    mkdir -p /usr/java
    ln -s /usr/lib/jvm/java /usr/java/default
  fi
fi 

if [ ! `which javac` ]; then
  echo "You need to install Java first to continue!"
  exit 1
fi

export JAVA_HOME="/usr/java/default"

echo "Installing CentOS packages..."
yum -y install $CENTOS_PKG_LIST

# Setup maven
echo -n "Downloading Maven... "
while [[ true ]]; do 
    if [[ ! -s ${MAVEN_FILENAME} ]]; then
	wget -q ${MAVEN_URL}
    fi
    # On success, uncompress the felix files in their location
    if [[ $? -eq 0 ]]; then
		echo -n "Uncompressing... "
		dir_name=$(tar tzf ${MAVEN_FILENAME} | grep -om1 '^[^/]*')
		tar xzf ${MAVEN_FILENAME}
		if [[ $? -eq 0 ]]; then
		    rm -rf $MAVEN_HOME
		    mv ${dir_name%/} -T $MAVEN_HOME
		    ln -s $MAVEN_HOME/bin/mvn /usr/local/bin/mvn
		    echo "Done"
		    break
		fi
    fi
    # Else, ask for the actions to take
    echo
    yesno -d yes "Error retrieving the Maven files from the web. Retry?" retry
    if [[ "$retry" ]]; then
    	echo -n "Retrying... "
    else
    	echo "You must download Maven manually and install it under $MAVEN_HOME, in order for matterhorn to be build"
	break
    fi
done

