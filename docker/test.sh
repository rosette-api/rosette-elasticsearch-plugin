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

ES_HOST=$1

TMPDIR=$(mktemp -d)
function cleanup() {
    echo "Cleaning up $TMPDIR"
    test -d $TMPDIR && rm -rf $TMPDIR
}
trap "cleanup" EXIT

if [ -z "$ES_HOST" ]; then
  echo "Error: missing ES_HOST"
  exit 1
fi

EXISTS=$(curl  --head -L -s -o /dev/null -w "%{http_code}" "${ES_HOST}")
if [[ $EXISTS -ne 200 ]]; then
  echo "Error: not available at ${ES_HOST} got HTTP ${EXISTS}"
  exit 1
fi
echo "Creating pipeline on ${ES_HOST}"
curl -s -XPUT "${ES_HOST}/_ingest/pipeline/rosette_pipeline" -H 'Content-Type: application/json' -d'{
    "description" :"All Rosette Processors",
    "processors": [
    {
        "ros_language" : {
            "field" : "text",
            "target_field" : "language"
        }
    },
    {
        "ros_categories" : {
            "field" : "text",
            "target_field" : "category"
        }
    },
    {
        "ros_name_translation" : {
            "field" : "text",
            "target_language" : "kor",
            "target_field" : "kor_name"
        }
    },
    {
        "ros_sentiment" : {
            "field" : "text",
            "target_field" : "sentiment"
        }
    },
    {
        "ros_entities" : {
            "field" : "text",
            "target_field" : "entities_sentiment",
            "include_sentiment" : true,
            "include_offsets" : true,
            "include_translation" : true,
            "translation_language" : "kor"
        }
    },
       {
        "ros_entities" : {
            "field" : "text",
            "target_field" : "entities_english",
            "include_sentiment" : false,
            "include_offsets" : false,
            "include_translation" : true,
            "translation_language" : "eng"
        }
    }
  ]
}' | jq -r .acknowledged | grep -q true || echo "FAILED"

EXISTS=$(curl -s -o /dev/null -w "%{http_code}" "${ES_HOST}/_ingest/pipeline/rosette_pipeline")
if [[ $EXISTS -ne 200 ]]; then
  echo "Error: pipeline not available ${EXISTS}"
  exit 1
else
  echo "Pipeline created"
fi

echo "Annotating document with pipeline"
curl -s -XPUT "${ES_HOST}/test_idx/_doc/2?pipeline=rosette_pipeline&pretty" -H 'Content-Type: application/json' -d'{"text":"New York"}'

EXISTS=$(curl -s -o /dev/null -w "%{http_code}" "${ES_HOST}/test_idx/_doc/2?pretty")
if [[ $EXISTS -ne 200 ]]; then
  echo "Error: document not available ${EXISTS}"
  exit 1
else
  echo "document created"
fi

echo "Fetching document"
DOC="${TMPDIR}/document.json"
curl -s -o "${DOC}" "${ES_HOST}/test_idx/_doc/2?pretty"
FAILED=0
echo "Testing sentiment"
if [[ "neu" == $(jq -r ._source.sentiment "${DOC}") ]]; then
  echo "pass";
else
  FAILED=1
  echo "fail";
fi
echo "Testing entities_sentiment sentiment"
if [[ "neu" == $(jq -r ._source.entities_sentiment[0].sentiment "${DOC}") ]]; then
  echo "pass";
else
  FAILED=1
  echo "fail";
fi
echo "Testing entities_sentiment QID Q60"
if [[ "Q60" == $(jq -r ._source.entities_sentiment[0].entityId "${DOC}") ]]; then
  echo "pass";
else
  FAILED=1
  echo "fail";
fi
echo "Testing entities_sentiment entity Korean name"
if [[ "노옥 케이티" == $(jq -r ._source.entities_sentiment[0].translation "${DOC}") ]]; then
  echo "pass";
else
  FAILED=1
  echo "fail";
fi
echo "Testing document name translation"
if [[ "노옥" == $(jq -r ._source.kor_name "${DOC}") ]]; then
  echo "pass";
else
  FAILED=1
  echo "fail";
fi
echo "Testing document language"
if [[ "eng" == $(jq -r ._source.language "${DOC}") ]]; then
  echo "pass";
else
  FAILED=1
  echo "fail";
fi
echo "Testing document category"
if [[ "ARTS_AND_ENTERTAINMENT" == $(jq -r ._source.category "${DOC}") ]]; then
  echo "pass";
else
  FAILED=1
  echo "fail";
fi
echo "Testing document entities_english translation"
if [[ "Niyu Yurk Siti" == $(jq -r ._source.entities_english[0].translation "${DOC}") ]]; then
  echo "pass";
else
  FAILED=1
  echo "fail";
fi
echo "Testing document entities_english QID Q60"
if [[ "Q60" == $(jq -r ._source.entities_english[0].entityId "${DOC}") ]]; then
  echo "pass";
else
  FAILED=1
  echo "fail";
fi
if [[ $FAILED -eq 0 ]]; then
  echo "All tests passed"
else
  echo "Some tests failed"
fi
exit $FAILED
