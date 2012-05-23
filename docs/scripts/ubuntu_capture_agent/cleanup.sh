#! /bin/bash

###########################################
# Cleanup all traces of the capture agent #
###########################################

# Matterhorn's user name
USER=
# Location for the user's home
HOME=
# Location for sources.list
SRC_LIST=
# Location for the backup file
SRC_LIST_BKP=
# Location of the matterhorn install
MH_DIR=
# The directory where the capture agent capture files live
CA_FILES_DIR=
# Path to the matterhorn startup script
STARTUP_SCRIPT=
# Path to the file specifying rules for the installed devices
RULES_FILE=
# List of packages to be uninstalled
PKG_LIST=

# Checks if this script is being run with root privileges, exiting if it doesn't
if [[ `id -u` -ne 0 ]]; then
    echo "This script requires root privileges. Please run it with the sudo command or log in to the root user and try again"
    exit 1
fi

# Double check to make sure they aren't losing any vital information
read -p "Are you sure you want to remove the Matterhorn Capture Agent [yes/NO]? " response
while [[ -z $(echo "$response" | grep -i '^yes$') && -z $(echo "${response:-no}" | grep -i '^no$') ]]; do
    read -p "Please write yes or no [no]: " response
done

if [[ $(echo "${response:-no}" | grep -i '^no$') ]]; then
  exit 0
fi

# Double check to make sure they don't want to delete any captures they might have. 
read -p "Do you want to delete any capture files you may have in $CA_FILES_DIR [yes/NO]? " response
while [[ -z $(echo "$response" | grep -i '^yes$') && -z $(echo "${response:-no}" | grep -i '^no$') ]]; do
    read -p "Please write yes or no [no]: " response
done

if [[ $(echo "${response:-yes}" | grep -i '^yes$') ]]; then
  rm -rf $CA_FILES_DIR
fi

# Remove vga2usb driver
echo -n "Removing the vga2usb driver... "
rmmod vga2usb 2> /dev/null
echo "Done"

# Remove dependencies installed by the scripts
echo -n "Removing the packages installed by matterhorn (this may take a long time)... "
apt-get -y purge $PKG_LIST &> /dev/null
echo -n "Removing 'orphan' libraries... "
apt-get -y autoremove &> /dev/null
echo -n "Cleaning... "
apt-get -y autoclean &> /dev/null
echo "Done"

# Restore appropriate sources.list
echo -n "Restoring the sources.list backup... "
mv $SRC_LIST.$SRC_LIST_BKP $SRC_LIST &> /dev/null
apt-get -y -qq update
echo "Done"

# Remove the configuration that starts matterhorn on boot
echo -n "Deleting the startup script... "
rm -f $STARTUP_SCRIPT
update-rc.d `basename $STARTUP_SCRIPT` remove &> /dev/null
echo "Done"

# Remove the udev rules that manage the devices
echo -n "Deleting the device rules... "
rm -f $RULES_FILE
echo "Done"

# Remove the capture storage directory
echo -n "Deleting the matterhorn install directory... "
rm -rf $MH_DIR
echo "Done"

# Remove the jv4linfo library
echo -n "Deleting the jv4linfo library... "
rm -f /usr/lib/libjv4linfo.so
echo "Done"

# Remove the user and their home directory
read -p "Do you want to remove the matterhorn user '$USER' [y/N]? " response
until [[ $(echo ${response:-no} | grep -i '^[yn]') ]]; do
    read -p "Please answer [y]es or [N]o: " response
done

if [[ $(echo ${response:-no} | grep -i '^y') ]]; then
    echo -n "Deleting user $USER... "
    userdel -r -f $USER &> /dev/null
    echo "Done"
else 
    # Delete the soft link to the capture agent folder
    rm -f $HOME/${CA_DIR##*/}
fi


# Kills felix
kill -9 $(ps U $USER 2> /dev/null | grep java | cut -d ' ' -f 2) 2> /dev/null

echo -e "\n\nDone uninstalling Matterhorn Capture Agent.\n\n" 

# Prompts the user to reboot or not
read -p "Some matterhorn settings won't be completely removed until the system reboots. Do you wish to do it now [Y/n]? " response

while [[ -z "$(echo ${response:-Y} | grep -i '^[yn]')" ]]; do
    read -p "Please enter [Y]es or [n]o: " response
done

if [[ -n "$(echo ${response:-Y} | grep -i '^y')" ]]; then
    echo -e "\n\nRebooting... "
    reboot > /dev/null
else
    echo
    read -n 1 -s -p "Hit any key to exit..."
    clear
fi

# Self-destruction in 5, 4, 3, 2, 1...
rm -f $0
