#! /bin/bash

###############################
# Setup Matterhorn from trunk #
###############################

# Checks this script is being run from install.sh
if [[ ! $INSTALL_RUN ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

. ${FUNCTIONS}

# Detect if the matterhorn source has been already checked out
url=$(svn info $FELIX_HOME 2> /dev/null | grep URL: | cut -d ' ' -f 2) 
if [[ "$url" ]]; then
    echo
    yesno -d yes "The source $url has been already checked out. Do you wish to keep it?" keep
else
    keep=
fi

if [[ -z "$keep" ]]; then
    # Get the necessary matterhorn source code (the whole trunk, as specified in MH-3211)
    while [[ true ]]; do
	echo
	ask -d "${SRC_DEFAULT}" "Enter the URL of the trunk, branch or tag you would like to download" url

	echo -n "Attempting to download matterhorn source from $url... "
	svn co --force $url $FELIX_HOME

	if [[ $? -eq 0 ]]; then
	    break
	else
	    ## Error
	    echo "Error! Couldn't check out the matterhorn code. Check your internet connection and/or the URL inputted."
	    yesno -d yes "Do you wish to retry?" retry
	    [[ "$retry" ]] || exit 1
	fi
    done
    echo "Done"
fi

# Log the URL downloaded -or already present-
echo >> $LOG_FILE
echo "# Source code URL" >> $LOG_FILE
echo "$url" >> $LOG_FILE

echo "Done"
