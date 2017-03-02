#!/usr/bin/env bash

# uses '|| true' to make sure that even if there is an error,
# the maven build continues and stops the docker image
curl -XPUT "localhost:$1/_ingest/pipeline/rosapi?pretty" -d'
{
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
        "translation_language" : "eng"
      }
    },
    {
      "ros_entities" : {
        "field" : "text",
        "target_field" : "entities",
        "include_sentiment" : false,
        "include_offsets" : false,
        "include_translation" : false,
        "translation_language" : "eng"
      }
    },
    {
      "ros_name_translation" : {
        "field" : "name",
        "target_field" : "translation",
        "target_language" : "rus"
      }
    }
  ]
}
' || true

#Pipeline without categories since it only supports English
curl -XPUT "localhost:$1/_ingest/pipeline/rosapi_jpn?pretty" -d'
{
  "processors": [
    {
      "ros_language" : {
        "field" : "text",
        "target_field" : "language"
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
        "translation_language" : "eng"
      }
    },
    {
      "ros_entities" : {
        "field" : "text",
        "target_field" : "entities",
        "include_sentiment" : false,
        "include_offsets" : false,
        "include_translation" : false,
        "translation_language" : "eng"
      }
    },
    {
      "ros_name_translation" : {
        "field" : "name",
        "target_field" : "translation",
        "target_language" : "rus"
      }
    }
  ]
}
' || true

curl -XPUT "localhost:$1/test_idx?pretty" -d'
{
  "mappings": {
    "rosette": {
      "properties": {
        "text" : { "type" : "text" },
        "language" : { "type" : "keyword" }
      }
    }
  }
}
' || true

curl -XPUT "localhost:$1/test_idx/rosette/1?pretty&refresh=true&pipeline=rosapi" -d'
{
  "text": "I know that I know nothing"
}
' || true

curl -XPUT "localhost:$1/test_idx/rosette/2?pretty&refresh=true&pipeline=rosapi_jpn" -d'
{
  "text": "ξέρω ότι δεν ξέρω τίποτα"
}
' || true

curl -XPUT "localhost:$1/test_idx/rosette/3?pretty&refresh=true&pipeline=rosapi" -d'
{
  "text": "No one desires evil"
}
' || true

sleep 3

curl -XPOST "localhost:$1/test_idx/_search?pretty" -d'
{
  "query": {
    "constant_score" : {
      "filter" : {
        "exists" : {"field" : "language"}
      }
    }
  }
}
' || true
