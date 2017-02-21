#!/usr/bin/env bash

curl -XPUT 'localhost:9200/_ingest/pipeline/rosapi?pretty' -d'
{
  "processors": [
    {
      "ros_language" : {
        "field" : "text",
        "target_field" : "language"
      }
    }
  ]
}
'

curl -XPUT 'localhost:9200/test_idx?pretty' -d'
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
'

curl -XPUT 'localhost:9200/test_idx/rosette/1?pretty&refresh=true&pipeline=rosapi' -d'
{
  "text": "I know that I know nothing"
}
'

curl -XPUT 'localhost:9200/test_idx/rosette/2?pretty&refresh=true&pipeline=rosapi' -d'
{
  "text": "ξέρω ότι δεν ξέρω τίποτα"
}
'

curl -XPUT 'localhost:9200/test_idx/rosette/3?pretty&refresh=true&pipeline=rosapi' -d'
{
  "text": "No one desires evil"
}
'

sleep 3

#curl -XPOST 'localhost:9200/test_idx/_search?pretty' -d'
#{
#  "query": {
#    "match" : {
#      "content.iso639" : "eng"
#    }
#  }
#}
#'

#curl -XPOST 'localhost:9200/test_idx/_search?pretty' -d'
#{
#  "query": {
#    "bool" : {
#      "must" : {
#        "term" : { "content" : "know" }
#      },
#      "must" : {
#        "match": {"content.iso639" : "ell"}
#      }
#    }
#  }
#}
#'

curl -XPOST 'localhost:9200/test_idx/_search?pretty' -d'
{
  "query": {
    "constant_score" : {
      "filter" : {
        "exists" : {"field" : "language"}
      }
    }
  }
}
'
