#!/usr/bin/env bash

#Expects two arguments:
#- git_username: Your user id on github.com
#- rel_notes: (Optional) File containing release notes for this release
#
# ./tools/release.sh ${ELASTIC_VERSION} ${GITHUB_ACCESS_TOKEN}

set -e

ELASTIC_VERSION=$1
ACCESS_TOKEN=$2

# Set versions so they will be incremented correctly
mvn versions:update-property -Dproperty=elasticsearch.version -DnewVersion=[${ELASTIC_VERSION}] -DallowDowngrade  -DgenerateBackupPoms=false
mvn versions:set -DnewVersion=${ELASTIC_VERSION}.0-SNAPSHOT -DgenerateBackupPoms=false
git commit -a -m "Auto-update ElasticSearch to $ELASTIC_VERSION"

echo "**"
echo "* First running mvn release:prepare release:perform"
echo "* (You may be asked for your ssh password)"
echo "**"
mvn -Prelease release:prepare release:perform --batch-mode

echo "**"
echo "* Now adding the release to the github repo"
echo "**"

version=$(sed -n 's/^version=\(.*\)/\1/p' plugin/target/classes/plugin-descriptor.properties)

if [ "$version" == ${ELASTIC_VERSION} ]; then
  echo "Error: version $version does not match Elastic version ${ELASTIC_VERSION}"
  exit 1
fi

response=$(curl "https://github.com/rosette-api/rosette-elasticsearch-plugin/releases/tag/$version")

if [ "$response" = "Not Found" ]; then
  echo "FAILURE: Failed to find release tag for the given version. Did mvn release run successfully?"
  exit 1
fi

echo "* You will now be prompted for your github.com password..."

notes="Release compatible with Elasticsearch ${ELASTIC_VERSION}"

#response=$(curl -XPOST -u $git_username https://api.github.com/repos/rosette-api/rosette-elasticsearch-plugin/releases -d '{ "tag_name": "'"$version"'", "name" : "'"rosette-elasticsearch-plugin-$version"'", "body" : "'"$notes"'" }')

response=$(curl -sS -H "Content-Type: application/json" \
	    -d '{ "tag_name": "'"$version"'", "name" : "'"rosette-elasticsearch-plugin-$version"'", "body" : "'"$notes"'" }'
		    https://api.github.com/repos/rosette-api/rosette-elasticsearch-plugin/releases?access_token=${ACCESS_TOKEN})
echo "$response"
uploadurl=$(echo "$response" | sed -n 's/.*"upload_url": "\(.*\){?name,label}",/\1/p')

if [ -z "$uploadurl" ]; then
  echo "* Failed to create new release in github. Verify correct github credentials and that the release doesn't already exist. Aborting."
  exit 1
fi

filename="plugin/target/releases/rosette-elasticsearch-plugin-$version.zip"
fullurl="$uploadurl?name=$(basename $filename)"
echo "* Adding plugin zip package to release assets..."
response=$(curl -XPOST -H "Content-Type: application/zip"  "$uploadurl?name=$(basename $filename)&access_token=${ACCESS_TOKEN}" --data-binary @"$filename")

echo "**"
echo "* Release success!"
echo "* MAKE SURE to head to https://oss.sonatype.org/#stagingRepositories to release the staged artifacts."
echo "* Verify the tag and release look correct at https://github.com/rosette-api/rosette-elasticsearch-plugin."
