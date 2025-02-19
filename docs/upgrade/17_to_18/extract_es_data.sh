#!/bin/sh

set -e
set -o pipefail

# Elasticsearch parameters
ES_HOST="http://localhost:9200"
INDEX="opencast_series"
SIZE=1000
OUTPUT_FILE="update_statements.sql"
ERROR_LOG="error_log.txt"
PAGE=0
ES_USER="your_username"
ES_PASS="your_password"

# Clear output and error log files
> "$OUTPUT_FILE"
> "$ERROR_LOG"

if ! command -v jq &> /dev/null; then
    echo "Error: jq is not installed. Please install it and try again." >&2
    exit 1
fi

while :; do
    FROM=$((PAGE * SIZE))
    QUERY='{"_source": ["creator", "uid", "organization"], "from": '"$FROM"', "size": '"$SIZE"', "query": {"match_all": {}}}'

    RESPONSE=$(curl --fail --silent --show-error --user "$ES_USER:$ES_PASS" -X POST "$ES_HOST/$INDEX/_search" -H "Content-Type: application/json" -d "$QUERY")

    HITS_COUNT=$(echo "$RESPONSE" | jq '.hits.hits | length')

    # Break loop if no more records
    if [ "$HITS_COUNT" -eq 0 ]; then
        break
    fi

    echo "$RESPONSE" | jq -r '.hits.hits[] | "UPDATE oc_series SET creator_name = \"" + (._source.creator[0] // "") + "\" WHERE id = \"" + (._source.uid[0] // "") + "\" AND organization = \"" + (._source.organization[0] // "") + "\";"' >> "$OUTPUT_FILE" 2>> "$ERROR_LOG"

    PAGE=$((PAGE + 1))
done

echo "SQL update statements saved to $OUTPUT_FILE"
echo "Errors (if any) logged to $ERROR_LOG"
