#!/bin/bash

SERVER="http://localhost:9200"
USER=""
PASSWORD=""

NUMBER_OF_REPLICAS=0  # change this if you use a cluster
AUTO_EXPAND_REPLICAS="false"

TIMEOUT="60s"

OLD_INDEX="adminui"  # Do not use external API index as source as that one doesn't contain themes!
NEW_INDEX="opencast"

# sub indices
VERSION="version"
EVENT="event"
SERIES="series"
THEME="theme"

set -eu

# Clone admin ui index to new allinone index to avoid an index rebuild when upgrading to OC11

# show current indices to user
echo -e "Indices before:\n"
curl -u "$USER:$PASSWORD" -X GET "$SERVER/_cat/indices?v" -w "\n"

# clone admin UI indices to new all-in-one indices
echo "Starting to clone ${OLD_INDEX} indices to ${NEW_INDEX} indices."
for SUB_INDEX in $VERSION $EVENT $SERIES $THEME; do
	SOURCE_INDEX="${OLD_INDEX}_${SUB_INDEX}"
	TARGET_INDEX="${NEW_INDEX}_${SUB_INDEX}"
	echo -e "Starting to clone ${SOURCE_INDEX} to ${TARGET_INDEX}.\n"

	# Make sure source index is open
	echo -e "Making sure source index is open:\n"
	curl -u "$USER:$PASSWORD" -X POST  "$SERVER/$SOURCE_INDEX/_open" -w "\n\n" -i -s

	# Put the source index in read-only mode
	echo -e "Putting source index in read-only mode:\n"
	curl -u "$USER:$PASSWORD" -X PUT  "$SERVER/$SOURCE_INDEX/_settings" \
	-H 'Content-Type: application/json' -d '{
		"settings": {
			"index.blocks.write": "true"
		}
	}' -w "\n\n" -i -s

	# Clone the source index to the target name and set the target to read-write mode
	echo -e "Cloning source index to target index:\n"
	curl -u "$USER:$PASSWORD" -X POST  "$SERVER/$SOURCE_INDEX/_clone/$TARGET_INDEX"  \
	-H 'Content-Type: application/json'  -d "{
		\"settings\": {
			\"index.blocks.write\": null,
			\"index.number_of_replicas\": $NUMBER_OF_REPLICAS,
			\"index.auto_expand_replicas\":$AUTO_EXPAND_REPLICAS
		}
	}" -w "\n\n" -i -s

	# Check for errors
	echo -e "Check if index is healthy.\n"
	RESPONSE=$(curl -u "$USER:$PASSWORD" -X GET "$SERVER/_cluster/health/$TARGET_INDEX?wait_for_status=green&timeout=$TIMEOUT" -i -s)
	echo -e "${RESPONSE}\n"

	if [[ $RESPONSE == *"408"* ]]; then
		echo "Timeout reached."
		exit 1
	elif [[ $RESPONSE != *"200"* ]]; then
                echo "Error: Index is not healthy or cannot be found."
                exit 1
	else
		echo -e "Index looks good.\n"
        fi
done

echo -e "All indices cloned.\n"

# show current indices to user
echo -e "Indices after:\n"
curl -u "$USER:$PASSWORD" -X GET "$SERVER/_cat/indices?v" -w "\n"
