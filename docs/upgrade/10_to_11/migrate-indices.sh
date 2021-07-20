#!/bin/bash

SERVER="http://localhost:9200"

OLD_INDEX="adminui"  # Do not use external API index as source as that one doesn't contain themes!
NEW_INDEX="apiindex"

# sub indices
VERSION="version"
EVENT="event"
SERIES="series"
THEME="theme"

set -e

# Clone admin ui index to new allinone index to avoid an index rebuild when upgrading to OC11

# show current indices to user
echo "Indices before:"
curl -X GET "$SERVER/_cat/indices?v" -w "\n"

# clone admin UI indices to new all-in-one indices
echo "Starting to clone ${OLD_INDEX} indices to ${NEW_INDEX} indices."
for SUB_INDEX in $VERSION $EVENT $SERIES $THEME; do
	SOURCE_INDEX="${OLD_INDEX}_${SUB_INDEX}"
	TARGET_INDEX="${NEW_INDEX}_${SUB_INDEX}"
	echo -e "Starting to clone ${SOURCE_INDEX} to ${TARGET_INDEX}.\n"

	# Make sure source index is open
	echo "Making sure source index is open:"
	curl  -X POST  "$SERVER/$SOURCE_INDEX/_open" -w "\n\n" -i -s

	# Put the source index in read-only mode
	echo "Putting source index in read-only mode:"
	curl  -X PUT  "$SERVER/$SOURCE_INDEX/_settings" \
	-H 'Content-Type: application/json' -d '{
		"settings": {
			"index.blocks.write": "true"
		}
	}' -w "\n\n" -i -s

	echo "Cloning source index to target index:"
	# Clone the source index to the target name and set the target to read-write mode
	RESPONSE=$(curl  -X POST  "$SERVER/$SOURCE_INDEX/_clone/$TARGET_INDEX"  \
	-H 'Content-Type: application/json'  -d '{
    		"settings": {
        		"index.blocks.write": null
    		}
	}' -i -s)
        echo -e "${RESPONSE}\n"

	# Check for errors
        if [[ $RESPONSE != *"200"* ]]; then
		echo "Error: Index could not be cloned. Aborting script."
		exit 1
	fi
	echo -e "Index cloned successfully.\n"
done

echo -e "All indices cloned.\n"

# show current indices to user
echo "Indices after:"
curl -X GET "$SERVER/_cat/indices?v" -w "\n"
