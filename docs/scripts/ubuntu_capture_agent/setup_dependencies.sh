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
      echo "In Ubuntu 11.10 we cannot install java automatically for you due to licensing issues. Please install java from oracle's website."
      ;;
    * )
      echo "Using Default Configuration of packages"
      PACKAGE_LIST_FILE=$PACKAGE_LIST_DEFAULT_FILE
      ;;
esac

# Grab the package list for this particular platform if it hasn't been downloaded. 
# Using C-like syntax in case file names have whitespaces
echo "Using package list file $PACKAGE_LIST_FILE"

# Check if the script is in the directory where the install.sh script was launched
if [[ -e $START_PATH/$PACKAGE_LIST_FILE ]]; then
    # ... and copies it to the working directory
    cp $START_PATH/$PACKAGE_LIST_FILE $WORKING_DIR
else
    # The script is not in the initial directory, so try to download it from the opencast source page
    wget $SRC_DEFAULT/$SCRIPTS_EXT/$PACKAGE_LIST_FILE &> /dev/null
    # Check the file is downloaded
    if [[ $? -ne 0 ]]; then
        echo "Couldn't retrieve the package list $PACKAGE_LIST_FILE from the repository. Try to download it manually and re-run this script."
        exit 2
    fi
fi

# Set the correct permissions on the file. 
chmod +x $PACKAGE_LIST_FILE

# Get the package list into a variable for future use. 
PKG_LIST=$(cat $PACKAGE_LIST_FILE | tr " " "\n")
