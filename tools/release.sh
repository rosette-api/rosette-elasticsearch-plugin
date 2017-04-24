#!/usr/bin/env bash

#Expects two arguments:
#- git_username: Your user id on github.com
#- rel_notes: (Optional) File containing release notes for this release
#
# ./tools/release.sh git_username=bwsawyer rel_notes=./docs/rel-notes.md

set -e

ARGS=$@

for arg in $ARGS; do
  eval "$arg"
done

if [ -z "$git_username" ]; then
  echo "Please specify a github username (Eg. ./tools/release.sh git_username=bwsawyer)"
  exit 1
fi

echo "**"
echo "* First running mvn release:prepare release:perform"
echo "* (You may be asked for your ssh password)"
echo "**"
mvn -Prelease release:prepare release:perform --batch-mode

echo "**"
echo "* Now adding the release to the github repo"
echo "**"

version=$(sed -n 's/^version=\(.*\)/\1/p' plugin/target/classes/plugin-descriptor.properties)

response=$(curl "https://github.com/rosette-api/rosette-elasticsearch-plugin/releases/tag/$version")

if [ "$response" = "Not Found" ]; then
  echo "FAILURE: Failed to find release tag for the given version. Did mvn release run successfully?"
  exit 1
fi

echo "* You will now be prompted for your github.com password..."

notes=""
#there is some additional json escaping we could do
if [ $rel_notes ]; then
  notes=$(cat $rel_notes | awk '{printf "%s\\n", $0}')
fi

response=$(curl -XPOST -u $git_username https://api.github.com/repos/rosette-api/rosette-elasticsearch-plugin/releases -d '{ "tag_name": "'"$version"'", "name" : "'"rosette-elasticsearch-plugin-$version"'", "body" : "'"$notes"'" }')
echo "$response"
uploadurl=$(echo "$response" | sed -n 's/.*"upload_url": "\(.*\){?name,label}",/\1/p')

if [ -z "$uploadurl" ]; then
  echo "* Failed to create new release in github. Verify correct github credentials and that the release doesn't already exist. Aborting."
  exit 1
fi

filename="plugin/target/releases/rosette-elasticsearch-plugin-$version.zip"
fullurl="$uploadurl?name=$(basename $filename)"
echo "* Adding plugin zip package to release assets..."
response=$(curl -XPOST -H "Content-Type: application/zip" -u  $git_username "$uploadurl?name=$(basename $filename)" --data-binary @"$filename")

echo "**"
echo "* Release success!"
echo "* MAKE SURE to head to https://oss.sonatype.org/#stagingRepositories to release the staged artifacts."
echo "* Verify the tag and release look correct at https://github.com/rosette-api/rosette-elasticsearch-plugin."