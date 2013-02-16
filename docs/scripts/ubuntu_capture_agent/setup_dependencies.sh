#!/bin/bash

##########################################################################
# Choose the necessary dependencies for the capture agent                #
##########################################################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

# The version of Ubuntu running on this system. 
ISSUE=`cat /etc/issue`
echo "Setting up dependencies for $ISSUE"
# Choose the correct dependencies for the Ubuntu version and run any platform specific commands.
case "$ISSUE" in
    "$UBUNTU_10_10" )
      echo "Using Configuration of Ubuntu 10.10 for packages."
      PACKAGE_LIST_FILE=$UBUNTU_10_10_PACKAGES_FILE
      ;;
    "$UBUNTU_11_04" )
      echo "Using Configuration of Ubuntu 11.04 for packages"
      PACKAGE_LIST_FILE=$UBUNTU_11_04_PACKAGES_FILE
      ;;
    "$UBUNTU_11_10" )
      echo "Using Configuration of Ubuntu 11.10 for packages"
      PACKAGE_LIST_FILE=$UBUNTU_11_10_PACKAGES_FILE
      ;;
    "$UBUNTU_12_04"* )
      echo "Using Configuration of Ubuntu 12.04 for packages"
      PACKAGE_LIST_FILE=$UBUNTU_12_04_PACKAGES_FILE
      ;;
    * )
      echo "Using Default Configuration of packages"
      PACKAGE_LIST_FILE=$PACKAGE_LIST_DEFAULT_FILE
      ;;
esac

echo "Using package list file $PACKAGE_LIST_FILE"

# Set the correct permissions on the file. 
chmod +r $PACKAGE_LIST_FILE

# Get the package list into a variable for future use. 
export PKG_LIST=$(cat $PACKAGE_LIST_FILE | tr " " "\n")
