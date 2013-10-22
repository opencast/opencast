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

while [[ true ]]; do
    echo
    ask -d "${SRC_DEFAULT}" "Enter develop (for trunk), or the name of the tag or branch you would like to install" url

    echo -n "Attempting to download matterhorn source from $url... "
    git checkout $url

    if [[ $? -eq 0 ]]; then
        break
    else
        ## Error
        echo "Error! Couldn't check out the matterhorn code. Please check to ensure you have the correct checkout name."
        yesno -d yes "Do you wish to retry?" retry
        [[ "$retry" ]] || exit 1
    fi
done
echo "Done"

# Log the URL downloaded -or already present-
echo >> $LOG_FILE
echo "# Source code URL" >> $LOG_FILE
echo "$url" >> $LOG_FILE

echo "Done"
