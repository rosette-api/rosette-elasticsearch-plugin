#!/usr/bin/env bash

curl -XPUT 'localhost:9200/test_idx?pretty' -d'
{
  "mappings": {
    "rosette": {
      "properties": {
        "content": {
          "type": "text",
          "fields": {
            "iso639": {
              "type": "iso639",
              "store": true,
              "index" : "not_analyzed"
            }
          }
        }
      }
    }
  }
}
'

curl -XPUT 'localhost:9200/test_idx/rosette/1?pretty&refresh=true' -d'
{
  "content": "I know that I know nothing"
}
'

curl -XPUT 'localhost:9200/test_idx/rosette/2?pretty&refresh=true' -d'
{
  "content": "ξέρω ότι δεν ξέρω τίποτα"
}
'

curl -XPUT 'localhost:9200/test_idx/rosette/2?pretty&refresh=true' -d'
{
  "content": "No one desires evil"
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
  "_source": true,
  "stored_fields": "*",
  "query": {
    "constant_score" : {
      "filter" : {
        "exists" : {"field" : "content.iso639"}
      }
    }
  }
}
'
