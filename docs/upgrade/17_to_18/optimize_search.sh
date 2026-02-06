#!/bin/bash

set -ex
set -o pipefail

# Elasticsearch details
ES_HOST="http://localhost:9200"

# Elasticsearch authentication
ES_USER="your_username"
ES_PASS="your_password"

if ! command -v jq &> /dev/null; then
    echo "Error: jq is not installed. Please install it and try again." >&2
    exit 1
fi

ES_HEALTH=$(curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" "$ES_HOST/_cluster/health" | jq -r -e '.status')

if [ "$ES_HEALTH" != "green" ] && [ "$ES_HEALTH" != "yellow" ]; then
  echo "Elasticsearch cluster is not healthy. Please check the cluster status and try again. ($ES_HEALTH)"
  exit 1
fi

reindex() {
  INDEX_NAME="$1"
  INDEX_NAME_NEW="${INDEX_NAME}_new"
  MAPPING_FILE="$2"

  echo "Check if source index '$INDEX_NAME' exists..."
  if ! curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" --head "$ES_HOST/$INDEX_NAME"; then
    echo "Index '$INDEX_NAME' does not exist. Skipping reindexing."
    exit 1
  fi

  echo "Check if target index '$INDEX_NAME_NEW' exists and fail if it does."
  if curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" --head "$ES_HOST/$INDEX_NAME_NEW"; then
    echo "Index '$INDEX_NAME_NEW' already exists. Please remove it before running this script."
    exit 1
  fi

  echo "Create index '$INDEX_NAME_NEW' for reindex..."
  curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" -X PUT "$ES_HOST/$INDEX_NAME_NEW" -H "Content-Type: application/json" --data '{
    "settings": '"$(cat "indexSettings.json")"',
    "mappings": '"$(cat "$MAPPING_FILE")"'
  }'

  echo "Reindexing '$INDEX_NAME'..."
  task_id_response=$(curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" -X POST "$ES_HOST/_reindex?wait_for_completion=false" -H "Content-Type: application/json" --data '{
    "source": {
      "index": "'"$INDEX_NAME"'",
      "_source": {
        "excludes": [
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
    response=$(curl --silent --user "$ES_USER:$ES_PASS" -X GET "$ES_HOST/_tasks/$TASK_ID")
    completed=$(echo "$response" | jq -r '.completed')

    if [[ "$completed" == "true" ]]; then
      echo "Task $TASK_ID completed"
      break
    fi

    echo "Task $TASK_ID still running..."
    sleep 2
  done

  echo "Drop old index '$INDEX_NAME'."
  curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" -X DELETE "$ES_HOST/$INDEX_NAME"

  # We should use an alias to switch the index, but to be safe just reindex again
  echo "Create index '$INDEX_NAME' for reindex..."
  curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" -X PUT "$ES_HOST/$INDEX_NAME" -H "Content-Type: application/json" --data '{
    "settings": '"$(cat "indexSettings.json")"',
    "mappings": '"$(cat "$MAPPING_FILE")"'
  }'

  echo "Reindexing '$INDEX_NAME'..."
  task_id_response_2=$(curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" -X POST "$ES_HOST/_reindex?wait_for_completion=false" -H "Content-Type: application/json" --data '{
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
    response=$(curl --silent --user "$ES_USER:$ES_PASS" -X GET "$ES_HOST/_tasks/$TASK_ID_2")
    completed=$(echo "$response" | jq -r '.completed')

    if [[ "$completed" == "true" ]]; then
      echo "Task $TASK_ID_2 completed"
      break
    fi

    echo "Task $TASK_ID_2 still running..."
    sleep 2
  done

  echo "Drop intermediate index '$INDEX_NAME_NEW'."
  curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" -X DELETE "$ES_HOST/$INDEX_NAME_NEW"
}

reindex "opencast_event" "event_mapping.json"
reindex "opencast_series" "series_mapping.json"
reindex "opencast_theme" "theme_mapping.json"
reindex "opencast_search" "search_mapping.json"

update_text_field() {
  INDEX_NAME=$1
  FIELD_NAME=$2
  FIELDS=$3
  echo "Updating text field '$FIELD_NAME' in index '$INDEX_NAME'..."

  SCRIPT="
  ctx._source['$FIELD_NAME'] = [];
  if (ctx._source.containsKey('"$FIELD_NAME"_fuzzy')) {
    ctx._source['"$FIELD_NAME"_fuzzy'] = [];
  }
  for(c in params.fields) {
    if (ctx._source.containsKey(c) && ctx._source[c] != null) {
      ctx._source.text.addAll(ctx._source[c]);
    }
  }
  "

  echo "Updating documents text field..."
  curl --silent --fail --show-error --user "$ES_USER:$ES_PASS" -X POST "$ES_HOST/$INDEX_NAME/_update_by_query" -H "Content-Type: application/json" -d '{
    "script": {
      "source": "'"$(echo "$SCRIPT" | tr '\n' ' ')"'",
      "lang": "painless",
      "params": {
        "target": "'"$FIELD_NAME"'",
        "fields": '"$FIELDS"'
      }
    }
  }'
}

update_text_field "opencast_event" "text" '["title", "description", "location", "series_name", "subject", "creator", "publisher", "rights", "presenter", "contributor"]'
update_text_field "opencast_series" "text" '["title", "description", "subject", "creator", "organizers" ,"publishers", "rights_holder"]'
update_text_field "opencast_theme" "text" '["name", "description", "creator" ]'
update_text_field "opencast_search" "fulltext" '["abstract", "contributor", "creator", "publisher", "subject", "title"]'

echo ""
echo "Updated index settings, mappings and text fields for all indices."
