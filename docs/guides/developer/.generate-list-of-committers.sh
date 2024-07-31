#!/bin/sh

set -eu

# https://github.com/settings/tokens
#GITHUB_USER=<username>
#GITHUB_PA_TOKEN=<personal-github-access-token>

OUT=docs/participate/list-of-committers.md.include

echo 'List of Committers' > "${OUT}"
echo '------------------' >> "${OUT}"
echo >> "${OUT}"
echo 'The current list of committers in aplhabetical order:' >> "${OUT}"
echo >> "${OUT}"
echo '<ul>' >> "${OUT}"

LINK='<a href=" + .html_url + ">'
IMG='<img style=\"width: 40px; margin: 0\" src=" + .avatar_url + " /> "'
curl -s -u "$GITHUB_USER:$GITHUB_PA_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/orgs/opencast/teams/committers/members \
    | jq -r '.[] | "<li>'"${LINK}${IMG}"' + .login + "</a></li>"' \
    | sort -f \
    | grep -v oc-bot \
    | sed 's_margin: 0"_margin: 0"\n_g' \
    | sed 's_ />_\n />_g' \
    >> "${OUT}"

echo '</ul>' >> "${OUT}"
