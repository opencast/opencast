#!/bin/env bash

#############################################################
# Opensearch URL
ES_HOST="http://localhost:9200"

# Opensearch authentication or empty values if not configured
ES_USER=""
ES_PASS=""
#############################################################

set -e
set -o pipefail

if ! command -v jq &> /dev/null; then
    echo "Error: jq is not installed. Please install it and try again." >&2
    exit 1
fi

curlUserArgs() {
  if [ -z "$ES_USER" ] || [ -z "$ES_PASS" ]; then
    return
  fi
  echo "--user $ES_USER:$ES_PASS"
}

ES_HEALTH=$(curl --fail --silent $(curlUserArgs) "$ES_HOST/_cluster/health" | jq -r -e '.status')

if [ "$ES_HEALTH" != "green" ] && [ "$ES_HEALTH" != "yellow" ]; then
  echo "Opensearch cluster is not healthy. Please check the cluster status and try again. ($ES_HEALTH)"
  exit 1
fi

createIndex() {
  CREATE_INDEX_NAME="$1"
  MAPPING_FILE="$2"

  echo "Check if target index '$CREATE_INDEX_NAME' exists and fail if it does."
  if curl --fail --silent $(curlUserArgs) --head "$ES_HOST/$CREATE_INDEX_NAME"; then
    echo "Index '$CREATE_INDEX_NAME' already exists. Please remove it before running this script."
    exit 1
  fi

  echo "Create index '$CREATE_INDEX_NAME'"
  curl --fail --silent $(curlUserArgs) -X PUT "$ES_HOST/$CREATE_INDEX_NAME" -H "Content-Type: application/json" --data '{
    "settings": '"$(cat "indexSettings.json")"',
    "mappings": '"$(cat "$MAPPING_FILE")"'
  }'
}

reindex() {
  INDEX_NAME="$1"
  INDEX_NAME_NEW="${INDEX_NAME}_new"
  MAPPING_FILE="$2"

  echo "Check if source index '$INDEX_NAME' exists..."
  if ! curl --fail --silent $(curlUserArgs) --head "$ES_HOST/$INDEX_NAME"; then
    echo "Index '$INDEX_NAME' does not exist. Skipping reindexing."
    exit 1
  fi

  createIndex "$INDEX_NAME_NEW" "$MAPPING_FILE"

  echo "Reindexing '$INDEX_NAME'..."
  task_id_response=$(curl --fail --silent $(curlUserArgs) -X POST "$ES_HOST/_reindex?wait_for_completion=false" -H "Content-Type: application/json" --data '{
    "source": {
      "index": "'"$INDEX_NAME"'",
      "_source": {
        "excludes": [
          "text",
          "text_fuzzy"
        ]
      }
    },
    "dest": {
      "index": "'"$INDEX_NAME_NEW"'"
    }
  }')
  TASK_ID=$(echo "$task_id_response" | jq -r '.task')
  echo "Wait for reindexing to complete..."
  while true; do
    response=$(curl --silent $(curlUserArgs) -X GET "$ES_HOST/_tasks/$TASK_ID")
    completed=$(echo "$response" | jq -r '.completed')

    if [[ "$completed" == "true" ]]; then
      echo "Task $TASK_ID completed"
      break
    fi

    echo "Task $TASK_ID still running..."
    sleep 2
  done

#  echo "Update index version for '$INDEX_NAME'"
#  curl --fail --silent $(curlUserArgs) -X PUT "$ES_HOST/opencast_version/$INDEX_NAME" \
#    -H "Content-Type: application/json" --data '{
#      "version": 1
#    }'

  echo "Drop old index '$INDEX_NAME'."
  curl --fail --silent $(curlUserArgs) -X DELETE "$ES_HOST/$INDEX_NAME"

  # We should use an alias to switch the index, but to be safe just reindex again
  echo "Create index '$INDEX_NAME' for reindex..."
  createIndex "$INDEX_NAME" "$MAPPING_FILE"

  echo "Reindexing '$INDEX_NAME'..."
  task_id_response_2=$(curl --fail --silent $(curlUserArgs) -X POST "$ES_HOST/_reindex?wait_for_completion=false" -H "Content-Type: application/json" --data '{
    "source": {
      "index": "'"$INDEX_NAME_NEW"'"
    },
    "dest": {
      "index": "'"$INDEX_NAME"'"
    }
  }')
  TASK_ID_2=$(echo "$task_id_response_2" | jq -r '.task')
  echo "Wait for reindexing to complete..."
  while true; do
    response=$(curl --silent $(curlUserArgs) -X GET "$ES_HOST/_tasks/$TASK_ID_2")
    completed=$(echo "$response" | jq -r '.completed')

    if [[ "$completed" == "true" ]]; then
      echo "Task $TASK_ID_2 completed"
      break
    fi

    echo "Task $TASK_ID_2 still running..."
    sleep 2
  done

  echo "Drop intermediate index '$INDEX_NAME_NEW'."
  curl --fail --silent $(curlUserArgs) -X DELETE "$ES_HOST/$INDEX_NAME_NEW"
}

echo "Drop previously unused index 'opencast_version'."
curl --silent $(curlUserArgs) -X DELETE "$ES_HOST/opencast_version"

createIndex "opencast_version" "version-mapping.json"

reindex "opencast_event" "event-mapping.json"
reindex "opencast_series" "series-mapping.json"
reindex "opencast_search" "search-mapping.json"

echo ""
echo "Updated index settings and mappings for all indices."
