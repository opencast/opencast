#!/bin/sh

set -eu

# https://github.com/settings/tokens
USER=<username>
TOKEN=<personal-github-access-token>

echo 'List of Committers'
echo '------------------'
echo
echo '<ul>'

LINK='<a href=" + .html_url + ">'
IMG='<img style=\"width: 40px; margin: 0\" src=" + .avatar_url + " /> "'
curl -s -u "$USER:$TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/orgs/opencast/teams/committers/members \
    | jq -r '.[] | "<li>'"${LINK}${IMG}"' + .login + "</a></li>"' \
    | sort -f \
    | grep -v oc-bot \
    | sed 's_margin: 0"_margin: 0"\n_g' \
    | sed 's_ />_\n />_g'

echo '</ul>'
