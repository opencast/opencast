#!/bin/bash

set -e
set -o pipefail

# Elasticsearch details
ES_HOST="http://localhost:9200"

# Elasticsearch authentication, keep both vars empty if no authentication is required
ES_USER=""
ES_PASS=""

if ! command -v jq > /dev/null 2>&1; then
    echo "Error: jq is not installed. Please install it and try again." >&2
    exit 1
fi

index_auth_args() {
  if [ ! -z "$ES_USER" ] && [ ! -z "$ES_PASS" ]; then
    echo "--user $ES_USER:$ES_PASS"
  fi
}

ES_HEALTH=$(curl --fail --silent --show-error $(index_auth_args) "$ES_HOST/_cluster/health" | jq -r -e '.status')

if [ "$ES_HEALTH" != "green" ] && [ "$ES_HEALTH" != "yellow" ]; then
  echo "Elasticsearch cluster is not healthy. Please check the cluster status and try again. ($ES_HEALTH)"
  exit 1
fi

count_docs() {
  _INDEX_NAME="$1"
  _INDEX_INFO=$(curl --fail --silent --show-error $(index_auth_args) "$ES_HOST/_cat/count/$_INDEX_NAME?format=json")
  echo "$_INDEX_INFO" | jq -r '.[0] | .count'
}

ensure_index_healthy() {
  _INDEX_NAME="$1"
  _INDEX_HEALTH="unknown"
  _WAIT_SEC=5

  while [ "$_INDEX_HEALTH" != "green" ]; do
    echo "Wait for index health becomes green for index $_INDEX_NAME."
    _INDEX_INFO=$(curl --silent $(index_auth_args) "$ES_HOST/_cluster/health/$_INDEX_NAME?format=json&wait_for_status=green&timeout=${_WAIT_SEC}s")
    _INDEX_TIMED_OUT=$(echo "$_INDEX_INFO" | jq -r '.timed_out')
    _INDEX_HEALTH=$(echo "$_INDEX_INFO" | jq -r '.status')
    if [ "$_INDEX_TIMED_OUT" == "true" ]; then
      echo "Index $_INDEX_NAME is not healthy ($_INDEX_HEALTH)."
    fi
  done
}

reindex() {
  INDEX_NAME="$1"
  INDEX_NAME_NEW="${INDEX_NAME}_new"
  MAPPING_FILE="$2"

  echo "Check if source index '$INDEX_NAME' exists..."
  if ! curl --fail --silent --show-error $(index_auth_args) --head "$ES_HOST/$INDEX_NAME"; then
    echo "Index '$INDEX_NAME' does not exist. Skipping reindexing."
    exit 1
  fi

  echo "Check if target index '$INDEX_NAME_NEW' exists and fail if it does."
  if curl --fail --silent --show-error $(index_auth_args) --head "$ES_HOST/$INDEX_NAME_NEW"; then
    echo "Index '$INDEX_NAME_NEW' already exists. Please remove it before running this script."
    exit 1
  fi

  echo "Create index '$INDEX_NAME_NEW' for reindex..."
  curl --fail --silent --show-error $(index_auth_args) -X PUT "$ES_HOST/$INDEX_NAME_NEW" -H "Content-Type: application/json" --data '{
    "settings": '"$(cat "indexSettings.json")"',
    "mappings": '"$(cat "$MAPPING_FILE")"'
  }'

  echo "Reindexing '$INDEX_NAME'..."
  task_id_response=$(curl --fail --silent --show-error $(index_auth_args) -X POST "$ES_HOST/_reindex?wait_for_completion=false&refresh=true" -H "Content-Type: application/json" --data '{
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
    response=$(curl --silent $(index_auth_args) -X GET "$ES_HOST/_tasks/$TASK_ID")
    completed=$(echo "$response" | jq -r '.completed')

    if [ "${completed}" = "true" ]; then
      echo "Task $TASK_ID completed"
      # check failures
      reindex_failures_count=$(echo "$response" | jq -r '.response.failures | length')
      if [ "$reindex_failures_count" -gt 0 ]; then
        echo "Reindex of $INDEX_NAME has failures:"
        echo "$response" | jq -r '.response.failures'
        echo
        echo "Please fix errors and try again."
        exit 1
      fi

      # Double check health and documents count, just to be sure we can continue
      # Check index health of the created index
      ensure_index_healthy "$INDEX_NAME_NEW"
      # Check documents count of source and target index
      index_docs_count=$(count_docs "$INDEX_NAME")
      index_new_docs_count=$(count_docs "$INDEX_NAME_NEW")
      if [ $index_docs_count != $index_new_docs_count ]; then
        # In some cases it is ok, but to be sure we should stop here
        echo "Indexed documents count not matches the migrated index $INDEX_NAME_NEW."
        echo "$INDEX_NAME = $index_docs_count docs"
        echo "$INDEX_NAME_NEW = $index_new_docs_count docs"
        echo "Please fix the root cause and run this script again or trigger an index-rebuild within Opencast."
        exit 1
      fi
      break
    fi

    echo "Task $TASK_ID still running..."
    sleep 2
  done

  echo "Drop old index '$INDEX_NAME'."
  curl --fail --silent --show-error $(index_auth_args) -X DELETE "$ES_HOST/$INDEX_NAME"

  # We should use an alias to switch the index, but to be safe just clone intermediate index.
  # There is no rename index option but we can use index aliases, clone or reindex instead.
  # To be safe, we will use clone operation here.
  # https://docs.opensearch.org/1.3/api-reference/index-apis/clone/

  # The clone operation will copy (by hard-linking data preferably) the index including all index settings.
  echo "Clone index '$INDEX_NAME_NEW' to target '$INDEX_NAME'"
  # Source index must be in read-only state, let's update index settings
  index_settings_update_result=$(curl --fail --silent --show-error $(index_auth_args) -X PUT "$ES_HOST/$INDEX_NAME_NEW/_settings" -H "Content-Type: application/json" --data '{"index.blocks.write": true}')
  index_settings_update_acknowledged=$(echo "$index_settings_update_result" | jq '.acknowledged')
  if [ "$index_settings_update_acknowledged" != "true" ]; then
    echo "Unable to set read-only flag for index $INDEX_NAME_NEW. Abort."
    exit 1
  fi
  index_clone_result=$(curl --fail --silent --show-error $(index_auth_args) -X PUT "$ES_HOST/$INDEX_NAME_NEW/_clone/$INDEX_NAME")
  index_clone_acknowledged=$(echo "$index_clone_result" | jq '.acknowledged')
  if [ "$index_clone_acknowledged" != "true" ]; then
    echo "Unable to clone $INDEX_NAME_NEW to $INDEX_NAME."
    exit 1
  fi
  index_settings_update_result=$(curl --fail --silent --show-error $(index_auth_args) -X PUT "$ES_HOST/$INDEX_NAME/_settings" -H "Content-Type: application/json" --data '{"index.blocks.write": false}')
  index_settings_update_acknowledged=$(echo "$index_settings_update_result" | jq '.acknowledged')
  if [ "$index_settings_update_acknowledged" != "true" ]; then
    echo "Unable to unset read-only flag for index $INDEX_NAME. Abort."
    exit 1
  fi

  echo "Drop intermediate index '$INDEX_NAME_NEW'."
  curl --fail --silent --show-error $(index_auth_args) -X DELETE "$ES_HOST/$INDEX_NAME_NEW"
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
  ctx._source['${FIELD_NAME}'] = [];
  if (ctx._source.containsKey('${FIELD_NAME}_fuzzy')) {
    ctx._source['${FIELD_NAME}_fuzzy'] = [];
  }
  for(c in params.fields) {
    if (ctx._source.containsKey(c) && ctx._source[c] != null) {
      ctx._source.text.addAll(ctx._source[c]);
    }
  }
  "

  echo "Updating documents text field..."
  curl --silent --fail --show-error $(index_auth_args) -X POST "$ES_HOST/$INDEX_NAME/_update_by_query" -H "Content-Type: application/json" -d '{
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
