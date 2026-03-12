#!/bin/sh

set -ex
set -o pipefail

# Elasticsearch details
ES_HOST="http://localhost:9200"
INDEX_NAME="opencast_event"
FIELD_NAME="is_published"

# Elasticsearch authentication
ES_USER="your_username"
ES_PASS="your_password"

if ! command -v jq > /dev/null 2>&1; then
    echo "Error: jq is not installed. Please install it and try again." >&2
    exit 1
fi

ES_HEALTH=$(curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" "$ES_HOST/_cluster/health" | jq -r -e '.status')

if [ "$ES_HEALTH" != "green" ] && [ "$ES_HEALTH" != "yellow" ]; then
  echo "Elasticsearch cluster is not healthy. Please check the cluster status and try again. ($ES_HEALTH)"
  exit 1
fi

# Check if the field already exists
EXISTING_MAPPING=$(curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" -X GET "$ES_HOST/$INDEX_NAME/_mapping" | jq -r --arg FIELD_NAME "$FIELD_NAME" 'to_entries | .[0].value.mappings.properties[$FIELD_NAME]')
if [ "$EXISTING_MAPPING" != "null" ]; then
  echo "Field '$FIELD_NAME' already exists in index '$INDEX_NAME'. No changes needed."
else
  # Update index mapping to add the new field
  echo "Updating index mapping to add '$FIELD_NAME' field..."
  curl --fail --silent --show-error -u "$ES_USER:$ES_PASS" -X PUT "$ES_HOST/$INDEX_NAME/_mapping" -H "Content-Type: application/json" -d '{
    "properties": {
      "'"$FIELD_NAME"'": {
        "type": "boolean"
      }
    }
  }'
fi

SCRIPT="
boolean hasExternalPublication = false;
if (ctx._source.containsKey('publication')) {
  for (publication in ctx._source.publication) {
    if (publication.channel != 'internal') {
      hasExternalPublication = true;
      break;
    }
  }
}
ctx._source.is_published = hasExternalPublication;
"

# Update documents: Set 'is_published' based on publication channel
echo "Updating documents based on publication channel..."
curl --silent --fail --show-error --user "$ES_USER:$ES_PASS" -X POST "$ES_HOST/$INDEX_NAME/_update_by_query" -H "Content-Type: application/json" -d '{
  "script": {
    "source": "'"$(echo "$SCRIPT" | tr '\n' ' ')"'",
    "lang": "painless"
  }
}'

echo ""
echo "Field '$FIELD_NAME' updated based on publication channels!"
