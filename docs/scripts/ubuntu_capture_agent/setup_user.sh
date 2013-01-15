#! /bin/bash

#########################################################################
# Create / choose the user for whom the capture agent will be installed #
#########################################################################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

. ${FUNCTIONS}

# Prompt for user name
ask -d "$USERNAME" "Enter the desired username for this system that will run the capture agent" input

# Add user and give sudo priveleges and permissions for accessing audio/video devices
useradd -m -s /bin/bash "$input"
var=$?
if [[ $var -eq 0 ]]; then
    # Ask for the user password
    for i in $(seq 1 $MAX_PASSWD_ATTEMPTS); do
	    passwd "$input"
	    if [[ $? -eq 0 ]]; then
	        echo "$input password updated succesfully"
	        break
	    elif [[ $i -eq $MAX_PASSWD_ATTEMPTS ]]; then
	        echo "Error. Too many password attempts. Aborting."
	        deluser "$input" --quiet --remove-home
	        exit 1
	    fi
    done
elif [[ $var -ne 9 ]]; then
    echo "Error when creating the $input user"
    exit 1
fi

# Setting up user's permissions by including it in the appropriate groups
adminGroupExists=$(grep '^admin:' /etc/group)
admGroupExists=$(grep '^adm:' /etc/group)

if [[ "$adminGroupExists" != "" ]]
then
        echo "$input will be added to the admin, video and audio groups."
	usermod -aG admin,video,audio "$input"
elif [[ "$admGroupExists" != "" ]]
then
        echo "$input will be added to the adm, video and audio groups."
	usermod -aG adm,video,audio "$input"
else
        echo "There isn't a admin or adm group for the user in the /etc/group file. Please add $input to whatever the admin group is for this system. They will only be added to the video and audio groups right now."
	usermod -aG video,audio "$input"
fi


# Exports the username, its home and other directories depending on them
export USERNAME="$input"
export HOME=$(grep "^${USERNAME}:" /etc/passwd | cut -d: -f 6)
if [[ -z "$HOME" ]]; then
    echo "Error: the specified user doesn't exist or doesn't have a HOME folder"
    exit 1
fi

export M2_REPO=$HOME/$M2_SUFFIX
