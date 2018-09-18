#!/bin/sh

set -eux

remote=""
if [ "$#" -ge 1 ];
then
  remote="-h 127.0.0.1 -P $1"
  echo "# Port and host overriden to $remote"
fi

cd "$(dirname "$0")"

versions="$(
  set -eu
  for dir in *_to_*; do
    version=$(echo "$dir" | sed 's/[^0-9.].*$/.0/;s/\([0-9]*\.[0-9]*\)\..*$/\1/')
    if [ "$(echo "$version >= 2.2" | bc -l)" = 1 ]; then
      if [ -n "$version" ]; then
        echo "$version $dir"
      fi
    fi
  done | sort -h)"

tmp22sql="$(mktemp)"
curl -s -L -o "$tmp22sql" https://raw.githubusercontent.com/opencast/opencast/2.2.0/docs/scripts/ddl/mysql5.sql

echo "# Creating database and applying Opencast 2.2 ddl script"

echo "create database octest;" | mysql -u root $remote
echo "mysql -u root $remote octest < $tmp22sql"
mysql -u root $remote octest < "$tmp22sql"
rm "$tmp22sql"

echo "# Running upgrade scripts"

echo "$versions" | while read -r line; do
  dir="$(echo "$line" | cut -d' ' -f2)"
  echo "mysql -u root $remote octest < $dir/mysql5.sql"
  mysql -u root $remote octest < "$dir/mysql5.sql"
done
