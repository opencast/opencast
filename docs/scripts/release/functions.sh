#! /bin/bash

####################################################################################################
## updatePomVersions
## Updates the pom files with a new version and is annoying about checking to make sure no accidental changes are made
## Syntax: 
##    updatePomVersions -w WORK_DIR -o OLD_POM_VERSION -n NEW_POM_VERSION
##       WORK_DIR is the root of the clone you want to modify
##       OLD_POM_VERSION is the version currently in the POM files
##       NEW_POM_VERSION is the version you wish the POM files to contain
##
## Return value:
##    0 - Success
##    1 - Wrong value for a flag
##    2 - Unknown flag
##    3 - Wrong number of parameters
##    
####################################################################################################
updatePomVersions() {
  local workDir
  local oldVersion
  local newVersion

  # Two parameters: one word for the question and another for the variable to set
  if [[ $# -ne 6 ]]; then
    echo "Wrong number of parameters"
    return 3
  fi

  # Process command line arguments
  while [[ true ]]; do
    case "$1" in
      -*) # Analyze the flags starting by - that require a value afterwards
        if [[ -z "$2" || $2 =~ ^-.$ ]]; then
          echo "Wrong value for argument $1: '$2'"
          return 1
        fi
        # Filters the - to decide what to do
        case "${1:1}" in 
          w)
            workDir="$2"
          ;;
          o)
            oldVersion="$2"
          ;;
          n)
            newVersion="$2"
          ;;
          *)  # Unknown
            echo "Unknown option: $1"
            return 2
          ;;
        esac
        shift
      ;;
      *) 
        break
      ;;    
    esac
    shift
  done

  echo "$workDir | $oldVersion | $newVersion"
  sed -i "s/<version>$oldVersion/<version>$newVersion/" $workDir/pom.xml
  for i in $workDir/modules/matterhorn-*
  do
      echo " Module: $i"
      if [ -f $i/pom.xml ]; then
          sed -i "s/<version>$oldVersion/<version>$newVersion/" $i/pom.xml
      fi
  done

  while [[ true ]]; do
    yesno -d no "NOTE: This script has made changes to your POM files.  Please ensure that it only made changes to the Matterhorn version number.  In rare cases some of the dependencies have the same version numbers, and the modification done above does *not* understand that it should not also change those versions.  Manual inspection of the changeset is required before continuing.  Have you finished checking all of the modifications?" has_checked
    if [[ "$has_checked" ]]; then 
        break
    fi
  done
  return 0
}

####################################################################################################
## ask
## Prompts a question
## Syntax: 
##    yesno [-a] [-d DEFAULT_OPTION] [-f FILTER] [-e ERROR_MSG] [-? HELP] [--] QUESTION RETURN_VARIABLE
##       -a allows the user to choose no option at all. This is superseded by -d
##       DEFAULT_OPTION is the default value for the question, which might be 'yes', 'no', or prefixes of these
##       FILTER is a pattern using the standard bash regexps that the answer has to comply with
##       ERROR_MSG is a message prompted to the user if the input answer doesn't comply with the specified format
##       HELP is a help message that is prompted to the user if they enter ?
##       -- forces to consider the following parameters as part of the options list and/or the return variable
##       QUESTION will be prompted to the user so that they answer 'yes' or 'no'. It HAS TO be quoted.
##       RETURN_VARIABLE is the variable where the selected option will be returned (non-empty for yes and empty for no)
##
## Return value:
##    0 - Success
##    1 - Wrong value for a flag
##    2 - Unknown flag
##    3 - Wrong number of parameters
##    
####################################################################################################
ask() {
    
    local blank_allowed
    local default
    local prompt
    local filter
    local reply
    local help
    local help_prompt="? for help"
    local err_bad_answer="Invalid answer"

    # Process command line arguments
  while [[ true ]]; do
    case "$1" in
      --) # Force stop parsing parameters
        break
      ;;
      -a) # Allow blank
        blank_allowed=true
      ;;
      -*) # Analyze the flags starting by - that require a value afterwards
        if [[ -z "$2" || $2 =~ ^-.$ ]]; then
          echo "Wrong value for argument $1: '$2'"
          return 1
        fi
        # Filters the - to decide what to do
        case "${1:1}" in 
          d)  # Default value
            default="$2"
          ;;
          f)  # Filter
            filter="$2"
          ;;
          h) # Help prompt
            help_prompt="$2"
          ;;
          \?) # Help
            help="$2"
          ;;
          e)  # Error message
            err_bad_answer="$2"
          ;;
          *)  # Unknown
            echo "Unknown option: $1"
            return 2
          ;;
            esac
            shift
          ;;
      *) 
      break
      ;;    
    esac
    shift
  done

    # Two parameters: one word for the question and another for the variable to set
    if [[ $# -ne 2 ]]; then
	echo "Wrong number of parameters"
	return 3
    fi
    
    prompt="$1"
   
    # Check the syntax of the default answer, if any
    if [[ "$default" ]]; then
	if [[ ! $default =~ $filter ]]; then
	    echo "Wrong value for argument -d: $default"
	    # Unset the default value, rather than exiting
	    unset default
	else 
	prompt="$prompt [$default]"
    fi
    fi

    # Check if a help message is present, and include that info in the prompt
    if [[ "$help" ]]; then
	prompt="$prompt ($help_prompt)"
    fi

    prompt="$prompt: "

    while [[ true ]]; do
	read -p "$prompt" temp
	# Assigns the default option to 'temp' if this is empty
	: ${temp:=$default}
        if [[ "$temp" == "?" && "$help" ]]; then
   	    # If help was asked and this exists, print it
	    echo "$help"
	elif [[ -z "$temp" && "$blank_allowed" ]]; then
	    # If no choice was selected and -a option was specified, assign an empty value and exit
	    eval ${!#}=
	    break
	elif [[ "$temp" && $temp =~ $filter ]]; then
	    # If the choice is within the valid range of answers then asign the answer to the appropriate variable and exit the loop
	    eval ${!#}=\"$temp\"
	    break
	else
	    # Otherwise, the option inputted is incorrect
	    echo "$err_bad_answer"
	fi
    done
}

####################################################################################################
## choose
## Prompts a list of options for the user to choose. Returns the 1-based index of the option
## Syntax: 
##    choose [-a] [-t TITLE] [-d DEFAULT_OPTION] [-p PROMPT] [-? HELP_MESSAGE] [-e ERROR_MSG] [-o OUTPUT_ARRAY] [--] OPTION_LIST RETURN_VARIABLE
##       -a allows the user to choose no option at all. This is superseded by -d
##       TITLE is a line preceding the options list, presenting somehow what the choice is about
##       DEFAULT_OPTION is the 1-based index of the default option if the user doesn't type any
##       PROMPT is the sentence prompted to the user when they are expected to input one of the options
##       HELP_MESSAGE is a text explaining or informing the user of the nature of the options or the choice itself
##       ERROR_MSG is a message prompted to the user if the input answer doesn't comply with the specified format
##       OUTPUT_ARRAY is a variable name where an array with the complete list of options will be stored, for convenience
##       -- forces to consider the following parameters as part of the options list and/or the return variable
##       OPTION_LIST is whitespace-separated list of options for the user to choose.
##                   Options containing whitespaces are allowed if surrounded by quotation marks.
##       RETURN_VARIABLE is the variable where the result will be returned.
##                       The result will be the **0-BASED** index the chosen option has in the options list.
##                       This is to ease the use of the return variable as an index in the options array
##
## Return value:
##    0 - Success
##    1 - Wrong argument value or no required value specified for an argument
##    2 - The specified default index is incorrect
##    3 - Flag unknown
##    4 - Insufficient number of parameters
##    
####################################################################################################
choose() {

    local default_option
    local blank_allowed
    local title
    local help
    local temp
    local output_array
    local err_bad_option="Invalid option"
    local prompt="Selection"
    local help_prompt="? for help"
    local i

    # Process command line arguments
    while [[ true ]]; do
	case "$1" in
	    --) # Force stop parsing parameters
		shift
		break
		;;
	    -a) # Allow a blank answer
		blank_allowed=true
		;;
	    -?) # Analyze the flags starting by - that have a value afterwards
		if [[ -z "$2" || $2 =~ ^-.$ ]]; then
		    echo "Wrong value specified for argument $1: '$2'"
		    return 1
		fi
		    # Filters the - to decide what to do
		case "${1:1}" in 
		    d)  # Default value
			if [[ ! $2 =~ ^0*[1-9][0-9]*$ ]]; then
			    echo "Wrong value for argument -d: $2 (should be an integer greater than 0)"
			else
			    default_option="$2"
			fi
			;;
		    t)  # Title
			title="$2"
			;;
		    p)  # Prompt
			prompt="$2"
			;;
		    e)  # Error message
			err_bad_option="$2"
			;;			
		    -h) # Help prompt
			help_prompt="$2"
			;;
		    \?)  # Help message
			help="$2"
			;;
		    o)  # Output array
			if [[ ! $2 =~ ^[a-zA-Z0-9_][a-zA-Z0-9_]*$ ]]; then
			    echo "Wrong value for argument -o: $2 (it should be a valid variable name)"
			    return 5
			fi
			output_array="$2"
			;;
		    *)  # Unknown
			echo "Unknown flag: $1"
			return 3
			;;
		esac
		shift
		;;
	    *)
		break
		;;    
	esac
	shift
    done
    
    # Check the parameters remaining after parsing the flags are sufficient
    if [[ $# -lt 2 ]]; then
	echo "Insufficient number of parameters"
	return 4
    fi

    # Array output
    if [[ "$output_array" ]]; then
	unset "$output_array"
	for (( i = 1; i < $#; i++ )); do
	    eval "$output_array[$((i-1))]=\"${!i}\""
	done
    fi

    # If there are only two parameters, ignore the parameters and assign the variable without prompting the user
    if [[ $# -eq 2 ]]; then
	eval ${!#}=\"$1\"
	return 0
    fi

    # Check the default option, if present, and include it in the prompt
    if [[ "$default_option" ]]; then 
	if [[ "$default_option" -ge $# ]]; then
	    echo "Wrong value for argument -d: $default_option (out of range)"
	    unset default_option
	else
	prompt="$prompt [${!default_option}]"
    fi
    fi

    # Check if there is any help message, and include an indication of this in the prompt
    if [[ "$help" ]]; then
	local prompt="$prompt ($help_prompt)"
    fi

    prompt="$prompt: "

    # If a title was specified, print it
    if [[ $title ]]; then
	echo "$title:"
    fi
    # Print the different options
    for (( i=1; i < $#; i++ )); do
	echo -e "\t$i) ${!i}"
    done
    # Ask the user for a choice
    while [[ true ]]; do
	read -p "$prompt" temp
	# Assigns the default option to 'temp' if this is empty
	: ${temp:=$default_option}
        if [[ "$temp" == "?" && "$help" ]]; then
   	    # If help was asked and this exists, print it
	    echo "$help"
	elif [[ -z "$temp" && "$blank_allowed" ]]; then
	    # If no choice was selected and -a option was specified, assign an empty value and exit
	    eval ${!#}=
	    break
	elif [[ $temp =~ ^0*[1-9][0-9]*$ && $temp -lt $# ]]; then
	    # If the choice is a number and within the valid range of answers then asign the option value to the variable and exit the loop
	    eval ${!#}=\"$(($temp-1))\"
	    break
	else
	    # Otherwise, the option inputted is incorrect
	    echo "$err_bad_option"
	fi
    done
    # temp holds the number of the choice
    return 0
}


####################################################################################################
## yesno
## Prompts a question to be answered with a yes/no question
## Syntax: 
##    yesno [-d DEFAULT_OPTION] [-h HELP_PROMPT] [-? HELP] QUESTION RETURN_VARIABLE
##       DEFAULT_OPTION is the default value for the question, which might be 'yes', 'no', or prefixes of these
##       HELP provides more information about the question if the user writes '?'
##       HELP_PROMPT is the indication to the user that they may get more info by pressing ?
##       QUESTION will be prompted to the user so that they answer 'yes' or 'no'. It HAS TO be quoted.
##       RETURN_VARIABLE is the variable where the selected option will be returned (non-empty for yes and empty for no)
##
## Return value:
##    0 - Success
##    1 - Wrong value for a flag
##    2 - Unknown flag
##    3 - Wrong number of parameters
##    
####################################################################################################
yesno() {

    local default
    local prompt
    local reply
    local help

    # The following should be chosen so that they don't collide, this is, one is not a preffix for the other; otherwise unexpected behaviour may happen
    local yesword='yes'
    local noword='no'
    local yesabbr='y'
    local noabbr='n'

    local help_prompt="? for help"    
    local err_badanswer="Please answer $yesword or $noword"

    # Process command line arguments
    while [[ true ]]; do
	case "$1" in
	    -d) # Default value
		if [[ "$(echo "${yesword}" | grep -i "$2" )" ]]; then
		    default="$yesword"
		elif [[ "$(echo "${noword}" | grep -i "$2" )" ]]; then
		    default="$noword"
		else
		    echo "Wrong value for argument $1: $2"
		    unset default
		fi
		shift
		;;
	    -'?') #Help message
		help="$2"
		shift
		;;
	    -e) # Error message
		err_bad_option="$2"
		shift
		;;			
	    -h) # Help prompt
		help_prompt="$2"
		shift
		;;
	    -?) # Unknown
		echo "Unknown option: $1"
		return 2
		;;
	    *) # No options left
		break
		;;    
	esac
	shift
    done

    # Two parameters: one word for the question and another for the variable to set
    if [[ $# -ne 2 ]]; then
	echo "Wrong number of parameters"
	return 3
    fi
    
    prompt="$1"

    if [[ "$default" == "$yesword" ]]; then
	prompt="$prompt [${yesabbr^^}/${noabbr,,}]"
    elif [[ "$default" == "$noword" ]]; then
	prompt="$prompt [${yesabbr,,}/${noabbr^^}]"
    else
	prompt="$prompt [${yesabbr,,}/${noabbr,,}]"
    fi
    
    if [[ $help ]]; then
	prompt="$prompt ($help_prompt): "
    else
	prompt="$prompt: "
    fi

    while [[ true ]]; do
	read -p "$prompt" reply
	: ${reply:=$default}
	if [[ "$reply" == '?' ]]; then
	    echo "$help"
	    continue
	elif [[ "$reply" && "$(echo "$yesword" | grep -i "$reply")" ]]; then
	    eval $2=true
	    break
	elif [[ "$reply" && "$(echo "$noword" | grep -i "$reply")" ]]; then
	    eval $2=
	    break
	fi
	echo "$err_badanswer"
    done
}
