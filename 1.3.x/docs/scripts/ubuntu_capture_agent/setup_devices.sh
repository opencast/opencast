#!/bin/bash

############################
# Set up the video devices #
############################

if [[ -z "$INSTALL_RUN" ]]; then
    echo "You shouldn't run this script directly. Please use the install.sh instead"
    exit 1
fi

supportedDevices[0]="Hauppauge WinTV PVR-350"
supportedDevices[1]="BT878 video (ProVideo PV143)"
supportedDevices[2]="Epiphan VGA2USB"
supportedDevices[3]="Hauppauge HVR-1600"
supportedDevices[4]="Hauppauge WinTV PVR-150"
supportedDevices[5]="Hauppauge WinTV-HVR1300 DVB-T/H"
supportedDevices[6]="WinTV PVR USB2 Model Category 2"
supportedDevices[7]="Epiphan VGA2USB #[A-Z0-9]+"

# Include the utility functions
. ${FUNCTIONS}

# ls the dev directory, then grep for video devices
for line in `ls /dev/video* | grep '/dev/video[0-9][0-9]*$'`; do
    devlist[${#devlist[@]}]=$line
done

# Make sure that the epiphan cards have an input connected
echo -e "\n\nWARNING: Please, make sure that your VGA2USB cards, if any, have an input connected. Otherwise they will NOT be detected"
echo -e "Press any key to continue...\n\n"
read -n 1 -s

# Read each line in the file. Using this C-like structure because the traditional 'for var in $list' does not get well with whitespaces in the names
#FIXME: Some Hauppages, as they create two devices in the kernel, appear duplicated. They should only appear once.
for (( i = 0; i < ${#devlist[@]}; i++ )); do
    # The following line filters the first occurrence in the v4l-info output containing 'card' or 'name'
    # Then, it filters whatever string enclosed in double quotes, which in such lines correspond to the device name
    testLine=$(v4l-info ${devlist[$i]} 2> /dev/null | grep -e name -e card -m 1 | cut -d '"' -f 2)
    for (( j = 0; j < ${#supportedDevices[@]}; j++ )); do
	# Add the matches to an array. This construction avoids 'gaps' --unset positions
	# Note both arrays devices and devNames will have the same size!
	if [[ -n "`echo \"$testLine\" | grep -e \"${supportedDevices[$j]}\"`" ]]; then
	    device[${#device[@]}]="${devlist[$i]}"
	    devName[${#devName[@]}]="${supportedDevices[$j]}"
	fi
    done
done

# Audio device
#audioLine=$(arecord -l| grep Analog -m 1)
#device[${#device[@]}]="hw:$(echo $audioLine | cut -d ':' -f 1 | cut -d ' ' -f 2)"
# The syntax is cumbersome, but it just keeps the fields surrounded by "[" and "]" and outputs them in the form "first (second)"
#devName[${#devName[@]}]=$(echo $audioLine | sed 's/^[^[]*\[\([^]]*\)\][^[]*\[\([^]]*\)\]$/\1 \2/')

sed -i "/capture.device/d" $CAPTURE_PROPS

rules=tmp.rules

rm -f $rules
echo "#! /bin/bash" > $CONFIG_SCRIPT

# This converts the "$FLAVORS" string in an array, for convenience
flavors=($FLAVORS)

unset allDevices
unset cleanName
unset extensions

# Log statement to explain the log syntax
echo >> $LOG_FILE
echo "# Installed devices (Clean Name ; Device Name ; Symbolic Link ; Queue Size (MBytes))" >> $LOG_FILE

for (( i = 0; i < ${#device[@]}; i++ )); do

    # Ask the user whether or not they want to configure this device
    yesno -d yes "Device \"${devName[$i]}\" (${device[$i]}) has been found. Do you want to configure it for matterhorn?" response
    if [[ ! "$response" ]]; then
	echo
	continue
    fi

    # Take the system name of this device and substitute all the non-alphanumeric characters to underscores
    defaultName=${devName[$i]//[^a-zA-Z0-9]/_}

    # Check this name is not repeated
    suffix=0
    for (( t = 0; t < $i; t++ )); do
	if [[ -n "$(echo "${cleanName[$t]}" | grep -i "^$defaultName\(_[0-9][0-9]*\)\?$")" ]]; then
	    (( suffix += 1 ))
	fi
    done    
    if [[ $suffix -gt 0 ]]; then
	defaultName=${defaultName}_$suffix
    fi
    
    # Ask for a user-defined cleanName --the name this device will have in the config files 
    while [[ true ]]; do
	ask -f '^[a-zA-Z0-9_\-]*$' -d "$defaultName" -e "The friendly name should not contain parentheses, slashes or whitespaces"\
            -? "$FRIENDLY_NAMES_HELP" "Please enter the friendly name for the \"${devName[$i]}\"" cleanName[$i]
	# Check the name is not repeated
	if [[ "${cleanName[$i]}" != "$defaultName" ]]; then
	    for (( t = 0; t < $i; t++ )); do
		if [[ -n "$(echo ${cleanName[$i]} | grep -i "^${cleanName[$t]}$")" ]]; then
		    echo "The name ${cleanName[$t]} is already in use for the device ${device[$t]}."
		    break
		fi
	    done
	    if [[ $t -eq $i ]]; then
		break
	    fi
	else
	    break
	fi
    done
    echo
    
    # Set up the symbolic link name for this device
    symlinkName=$(echo ${cleanName[$i]} | tr "[:upper:]" "[:lower:]")

    # Setup device info using udevadm info (sed filters the <value> in ATTR{name}="<value>" and escapes the special characters --> []*? <-- )
    sysName=$(udevadm info --attribute-walk --name=${device[$i]} | sed -e '/ATTR{name}/!d' -e 's/^[^"]*"\(.*\)".*$/\1/' -e 's/\([][?\*]\)/\\\1/g')
    echo "KERNEL==\"video[0-9]*\", ATTR{name}==\"$sysName\", GROUP=\"video\", SYMLINK+=\"$symlinkName\"" >> $rules

    # Prompt for the output file extension for the video capture device. Please see MH-4523 for details.
    # The extension will be appended to the files and will be stored in the config files 
    ask -f '^[a-zA-Z0-9_\-]*$' -d "$DEFAULT_VIDEO_EXTENSION" -e "The extension should not contain periods, parentheses, slashes or whitespaces"\
        -? "$EXTENSIONS_HELP" "Please enter the output file extension for the media created by the video capture device" extension[$i]


    # Prompt for the flavor for this device
    if [[ ${#flavors[@]} -eq 0 ]]; then
	flavor=0
    else
	choose -t "Please select the flavor assigned to ${cleanName[$i]}" -d $DEFAULT_FLAVOR -? "$FLAVORS_HELP" ${flavors[@]} "User-defined" flavor
    fi
    
    if [[ $flavor -eq ${#flavors[@]} ]]; then
	# The user selected the "User-defined" option
        # The -f flag filters the answers allowing only those consisting of two fields of any characters but slashes, separated by a single slash '/
	ask -d "${flavors[$DEFAULT_FLAVOR]}" -e "Invalid syntax. The flavors follow the pattern <prefix>/<suffix>" -? "$FLAVORS_HELP"\
            -f '^[^/ ][^/ ]*/[^/ ][^/ ]*$' "Please enter the flavor for ${cleanName[$i]}" flavor
    else
	# The user selected any of the flavors in the list
	flavor=${flavors[$flavor]}
    fi
    echo

    # Prompt for choosing the video standard
    # First expression: filters the lines within the paragraph starting with the word "standards" and ending in a empty line
    # Second expression: filters the lines containing the word "name" (first line) or "id" (second line)
    # Third expression: matches the whole line, but substitutes it by only the standard name (1st line) or the id (2nd line)
    # Fourth expression: (1st line only) substitutes the whitespaces in the name by underscores, to avoid problems with arrays in bash 
    standards=( $(v4l-info ${device[$i]} 2> /dev/null | sed -e '/^standards/,/^$/!d' -e '/name/!d' -e 's/^\s*name\s*:\s*\"\(.*\)\"/\1/' -e 's/ /_/g') )

    if [[ ${#standards[@]} -gt 1 ]]; then
	choose -t "Please choose the output standard for the device ${devName[$i]}" ${standards[@]} std
	
	v4l2-ctl -s $std -d ${device[$i]} > /dev/null
	if [[ $? -ne 0 ]]; then
	    echo "Error. Standard ${standards[$std]} not set. Please try to set it manually"
	else
	    echo "v4l2-ctl -s $std -d /dev/${symlinkName}" >> $CONFIG_SCRIPT
	    echo "Standard ${standards[$std]} set for the device ${devName[$i]}"
	fi
	echo
    fi

    #Select input to use with the card
    # First expression: filters the lines within the paragraph "channels", ending in a emptyline
    # Second expression: filters the lines containing the word 'name'
    # Third expression: matches the whole line, but substitutes it by only the device name
    # Fourth expression: substitutes the whitespaces in the name by underscores, to avoid problems with arrays in bash
    inputs=( $(v4l-info ${device[$i]} 2> /dev/null | sed -e '/^channels/,/^$/!d' -e '/name/!d' -e 's/^\sname\s*:\s*\"\(.*\)\"/\1/' -e 's/ /_/g') )
    if [[ ${#inputs[@]} -gt 1 ]]; then 
	choose -t "Please select the input number to be used with the ${devName[$i]}" ${inputs[@]} input

	v4l2-ctl -d ${device[$i]} -i $input > /dev/null
	echo "v4l2-ctl -d /dev/${symlinkName} -i $input" >> $CONFIG_SCRIPT
	echo "Using ${inputs[$input]} input with the ${devName[$i]}."
	echo
    fi

    # Prompt the user for the size of the device queue (in megabytes)
    ask -d "$DEFAULT_QUEUE_SIZE" -f '^0*[1-9][0-9]*$' -? "$QUEUE_HELP" -e "The value must be an integer" -h "? - more info"\
        "What size of RAM should be reserved for device X (in megabytes)?" q_size

    # Writes device to the config file
    echo "$DEVICE_PREFIX.${cleanName[$i]}.$SOURCE_SUFFIX=/dev/$symlinkName" >> $CAPTURE_PROPS
    echo "$DEVICE_PREFIX.${cleanName[$i]}.$OUT_SUFFIX=${cleanName[$i]}.${extension[$i]}" >> $CAPTURE_PROPS
    echo "$DEVICE_PREFIX.${cleanName[$i]}.$FLAVOR_SUFFIX=$flavor" >> $CAPTURE_PROPS
    echo "$DEVICE_PREFIX.${cleanName[$i]}.$QUEUE_SUFFIX=$((q_size*1024*1024))" >> $CAPTURE_PROPS
    allDevices="${allDevices}${cleanName[$i]},"
    
    # Log this device
    echo "${cleanName[$i]} ; ${devName[$i]} ; $symlinkName ; $q_size" >> $LOG_FILE

    echo
done

# Moves the config files to their definitive locations
mv $rules $DEV_RULES
chown root:video $DEV_RULES
chown $USERNAME:$USERNAME $CONFIG_SCRIPT
mv $CONFIG_SCRIPT $CA_DIR

# Audio device
audioLine=$(arecord -l| grep Analog -m 1)
audioDevice="hw:$(echo $audioLine | cut -d ':' -f 1 | cut -d ' ' -f 2)"
# The syntax is cumbersome, but it just keeps the fields surrounded by "[" and "]" and outputs them in the form "first second"
audioDevName=$(echo $audioLine | sed 's/^[^[]*\[\([^]]*\)\][^[]*\[\([^]]*\)\]$/\1 \2/')

# Ask the user whether or not they want to configure this device
yesno -d yes "Audio device \"${audioDevName}\" has been found. Do you want to configure it for matterhorn?" response

if [[ "$response" ]]; then

    defaultName=${audioDevName//[^a-zA-Z0-9]/_}
    # Check this name is not repeated
    suffix=0
    for (( t = 0; t < $i; t++ )); do
	if [[ -n "$(echo "${cleanName[$t]}" | grep -i "^$defaultName\(_[0-9][0-9]*\)\?")" ]]; then
	    (( suffix += 1 ))
	fi
    done    
    if [[ $suffix -gt 0 ]]; then
	defaultName=${defaultName}_$suffix
    fi


    # Ask for a user-defined cleanName --the name this device will have in the config files 
    while [[ true ]]; do
	ask -f '^[a-zA-Z0-9_\-]*$' -d "$defaultName" -e "The friendly name should not contain parentheses, slashes or whitespaces"\
            -? "$FRIENDLY_NAMES_HELP" "Please enter the friendly name for the \"${audioDevName}\"" cleanName[$i]
	# Check the name is not repeated
	if [[ "${cleanName[$i]}" != "$defaultName" ]]; then
	    for (( t = 0; t < $i; t++ )); do
		if [[ -n "$(echo ${cleanName[$i]} | grep -i "^${cleanName[$t]}$")" ]]; then
		    echo "The name ${cleanName[$t]} is already in use for the device ${device[$t]}."
		    break
		fi
	    done
	    if [[ $t -eq $i ]]; then
		break
	    fi
	else
	    break
	fi
    done
    echo

    # Prompt for the output file extension for the audio capture device. Please see MH-4523 for details.
    # The extension will be appended to the files and will be stored in the config files 
    ask -f '^[a-zA-Z0-9_\-]*$' -d "$DEFAULT_AUDIO_EXTENSION" -e "The extension should not contain periods, parentheses, slashes or whitespaces"\
        -? "$EXTENSIONS_HELP" "Please enter the output file extension for the media created by the audio capture device" extension[$i]

    # Prompt for the flavor for this device
    if [[ ${#flavors[@]} -eq 0 ]]; then
	flavor=0
    else
	choose -t "Please select the flavor assigned to ${cleanName[$i]}" -d $DEFAULT_FLAVOR -? "$FLAVORS_HELP" ${flavors[@]} "User-defined" flavor
    fi
    
    if [[ $flavor -eq ${#flavors[@]} ]]; then
	# The user selected the "User-defined" option
        # The -f flag filters the answers allowing only those consisting of two fields of any characters but slashes, separated by a single slash '/
	ask -d "${flavors[$DEFAULT_FLAVOR]}" -e "Invalid syntax. The flavors follow the pattern <prefix>/<suffix>" -? "$FLAVORS_HELP"\
            -f '^[^/ ][^/ ]*/[^/ ][^/ ]*$' "Please enter the flavor for ${cleanName[$i]}" flavor
    else
	# The user selected any of the flavors in the list
	flavor=${flavors[$flavor]}
    fi
    echo
    
    # Prompt the user for the size of the device queue (in megabytes)
    ask -d "$DEFAULT_QUEUE_SIZE" -f '^0*[1-9][0-9]*$' -? "$QUEUE_HELP" -e "The value must be an integer" -h "? - more info"\
        "What size of RAM should be reserved for device X (in megabytes)?" q_size

    # Write the config values to the properties file
    echo "$DEVICE_PREFIX.${cleanName[$i]}.$SOURCE_SUFFIX=$audioDevice" >> $CAPTURE_PROPS
    echo "$DEVICE_PREFIX.${cleanName[$i]}.$OUT_SUFFIX=${cleanName[$i]}.${extension[$i]}" >> $CAPTURE_PROPS
    echo "$DEVICE_PREFIX.${cleanName[$i]}.$FLAVOR_SUFFIX=$flavor" >> $CAPTURE_PROPS
    echo "$DEVICE_PREFIX.${cleanName[$i]}.$QUEUE_SUFFIX=$((q_size*1024*1024))" >> $CAPTURE_PROPS

    allDevices="${allDevices}${cleanName[$i]}"

    # Log this device
    echo "${cleanName[$i]} ; ${audioDevName} ; $audioDevice" >> $LOG_FILE

    echo
fi

# Add the list of installed names
echo "$DEVICE_PREFIX.$LIST_SUFFIX=${allDevices}" >> $CAPTURE_PROPS
