#!/usr/bin/env bash

################################################################################
# This data and information is proprietary to, and a valuable trade secret
# of, Basis Technology Corp.  It is given in confidence by Basis Technology
# and may only be used as permitted under the license agreement under which
# it has been distributed, and in no other way.
#
# Copyright (c) 2024 Basis Technology Corporation All rights reserved.
#
# The technical data and information provided herein are provided with
# `limited rights', and the computer software provided herein is provided
# with `restricted rights' as those terms are defined in DAR and ASPR
# 7-104.9(a).
#
################################################################################

#Expects two arguments:
#- ELASTIC_VERSION: The version of elastic for which you are building the plugin
#- GITHUB_ACCESS_TOKEN: Personal OAuth access token with full repo permissions.
#
# ./tools/release.sh ${ELASTIC_VERSION} ${GITHUB_ACCESS_TOKEN}

set -e

ELASTIC_VERSION=$1
ACCESS_TOKEN=$2

echo "**"
echo "* Set versions so they will be incremented correctly."
echo "**"
mvn versions:update-property -Dproperty=elasticsearch.version -DnewVersion=[${ELASTIC_VERSION}] -DallowDowngrade  -DgenerateBackupPoms=false
mvn versions:set -DnewVersion=${ELASTIC_VERSION}.0-SNAPSHOT -DgenerateBackupPoms=false
git commit -a -m "Auto-update Elasticsearch to ${ELASTIC_VERSION}"

echo "**"
echo "* First running mvn release:prepare release:perform"
echo "* (You may be asked for your ssh password)"
echo "**"
mvn -Prelease release:prepare release:perform --batch-mode

echo "**"
echo "* Now adding the release to the github repo"
echo "**"

version=$(sed -n 's/^version=\(.*\)/\1/p' plugin/target/classes/plugin-descriptor.properties)

if [ "${version}" == ${ELASTIC_VERSION} ]; then
  echo "Error: version ${version} does not match Elastic version ${ELASTIC_VERSION}"
  exit 1
fi

response=$(curl -sS "https://github.com/rosette-api/rosette-elasticsearch-plugin/releases/tag/${version}")

if [ "${response}" = "Not Found" ]; then
  echo "FAILURE: Failed to find release tag for ${version}. Did mvn release run successfully?"
  exit 1
fi

echo "* You will now be prompted for your github.com password..."

notes="Release compatible with Elasticsearch ${ELASTIC_VERSION}"

#response=$(curl -XPOST -u $git_username https://api.github.com/repos/rosette-api/rosette-elasticsearch-plugin/releases -d '{ "tag_name": "'"$version"'", "name" : "'"rosette-elasticsearch-plugin-$version"'", "body" : "'"$notes"'" }')

response=$(curl -sS \
                -H "Content-Type: application/json" \
                -H "Authorization: token ${ACCESS_TOKEN}" \
                -d '{ "tag_name": "'"${version}"'", "name" : "'"rosette-elasticsearch-plugin-${version}"'", "body" : "'"${notes}"'" }' \
                https://api.github.com/repos/rosette-api/rosette-elasticsearch-plugin/releases)
echo "${response}"
uploadurl=$(echo "${response}" | sed -n 's/.*"upload_url": "\(.*\){?name,label}",/\1/p')

if [ -z "${uploadurl}" ]; then
  echo "* Failed to create new release in github. Verify correct github credentials and that the release doesn't already exist. Aborting."
  exit 1
fi

filename="plugin/target/releases/rosette-elasticsearch-plugin-${version}.zip"
fullurl="${uploadurl}?name=$(basename ${filename})"
echo "* Adding plugin zip package to release assets..."
response=$(curl -sS \
                -H "Content-Type: application/zip" \
                -H "Authorization: token ${ACCESS_TOKEN}" \
                "${uploadurl}?name=$(basename ${filename})" --data-binary @"${filename}")

echo "**"
echo "* Release success!"
echo "* MAKE SURE to head to https://oss.sonatype.org/#stagingRepositories to release the staged artifacts."
echo "* Verify the tag and release look correct at https://github.com/rosette-api/rosette-elasticsearch-plugin."
