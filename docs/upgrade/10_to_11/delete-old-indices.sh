#!/bin/bash

SERVER="http://localhost:9200"
USER=""
PASSWORD=""

# indices
ADMIN_UI_INDEX="adminui"
EXTERNAL_API_INDEX="externalapi"

# sub indices
VERSION="version"
EVENT="event"
SERIES="series"
THEME="theme"
GROUP="group"

set -eu

delete_indices() {
	local INDEX="$1"
	shift  # shift arguments to the left so we can get the rest as an array
	local SUB_INDICES=("$@")
	for SUB_INDEX in ${SUB_INDICES[@]}; do
		DELETE_INDEX="${INDEX}_${SUB_INDEX}"
		echo -e "Starting to delete ${DELETE_INDEX}.\n"

		# Make sure index is open
		echo -e "Making sure index is open:\n"
		curl -u "$USER:$PASSWORD" -X POST  "$SERVER/$DELETE_INDEX/_open" -w "\n\n" -i -s

		# Delete index
		echo -e "Deleting index:\n"
		RESPONSE=$(curl -u "$USER:$PASSWORD" -X DELETE  "$SERVER/$DELETE_INDEX" -i -s)
		echo -e "${RESPONSE}\n"

	        # Check for errors
		if [[ $RESPONSE != *"200"* ]]; then
			echo -e "Index could not be deleted.\n"
		else
			echo -e "Index successfully deleted.\n"
		fi
	done
}

# Delete old indices that will no longer be used in Opencast >= 11
# Make sure you use the migration script first if you don't want to do an index rebuild!
echo -e "Indices before:\n"
curl -u "$USER:$PASSWORD" -X GET "$SERVER/_cat/indices?v" -w "\n"

echo "Deleting all external api and admin ui indices."
echo -e "Deleting the now unused group index might fail if you have already removed it or if you started with OC >= 10.\n"

SUB_INDICES=("${VERSION}" "${EVENT}" "${SERIES}" "${THEME}" "${GROUP}")
delete_indices $ADMIN_UI_INDEX "${SUB_INDICES[@]}"

SUB_INDICES=($VERSION $EVENT $SERIES $GROUP)
delete_indices $EXTERNAL_API_INDEX "${SUB_INDICES[@]}"

echo -e "Cleanup done.\n"

echo -e "Indices after:\n"
curl -u "$USER:$PASSWORD" -X GET "$SERVER/_cat/indices?v" -w "\n"
