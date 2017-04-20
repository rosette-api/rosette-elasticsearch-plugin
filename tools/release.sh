#!/usr/bin/env bash

#Expects two arguments:
#- git_username: Your user id on github.com
#- version: The version number being released
#
# ./release.sh git_username=bwsawyer version=5.3.0.0

set -e

ARGS=$@

for arg in $ARGS; do
  eval "$arg"
done

echo "**"
echo "* First running mvn release:prepare release:perform"
echo "* (You may be asked for your ssh password)"
echo "**"
mvn release:prepare release:perform --batch-mode

echo "**"
echo "* Now adding the release to the github repo"
echo "**"

response=$(curl "https://github.com/bwsawyer/rosette-elasticsearch-plugin/releases/tag/$version")

if [ "$response" = "Not Found" ]; then
  echo "FAILURE: Failed to find release tag for the given version. Did mvn release run successfully?"
  exit 1
fi

echo "* You will now be prompted for your github.com password..."
response=$(curl -XPOST -u $git_username https://api.github.com/repos/bwsawyer/rosette-elasticsearch-plugin/releases -d '{ "tag_name": "'"$version"'", "name" : "'"rosette-elasticsearch-plugin-$version"'" }')

uploadurl=$(echo "$response" | sed -n 's/.*"upload_url": "\(.*\){?name,label}",/\1/p')
filename="plugin/target/releases/rosette-elasticsearch-plugin-$version.zip"
fullurl="$uploadurl?name=$(basename $filename)"

response=$(curl -XPOST -H "Content-Type: application/zip" -u  $git_username "$uploadurl?name=$(basename $filename)" --data-binary @"$filename")

echo "**"
echo "* Release success!"
echo "* MAKE SURE to head to https://oss.sonatype.org/#stagingRepositories to release the staged artifacts."
echo "* Verify the tag and release look correct at https://github.com/rosette-api/rosette-elasticsearch-plugin."