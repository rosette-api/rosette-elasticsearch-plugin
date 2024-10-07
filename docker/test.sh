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

code=0
command_counter=0
set -x

# uses '|| code=$?' to make sure that even if there is an error,
# the maven build continues and stops the docker image
# the exit codes will be appended to the output file for later verification

curl -fsSL -H 'Content-Type: application/json' -XPUT "$1:$2/_ingest/pipeline/my_pipeline" -d'
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
' || ((code++))
echo
echo "After command ${command_counter} our error code count is ${code}."
command_counter=$((command_counter + 1))

curl -fsSL -H 'Content-Type: application/json' -XPOST "$1:$2/indexname/mappingName?pipeline=my_pipeline&pretty" -d'
{
  "text" : "This is a document containing English text"
}
' || ((code++))
echo "After command ${command_counter} our error code count is ${code}."
command_counter=$((command_counter + 1))

curl -fsSL -H "Content-Type: application/json" -XPUT "$1:$2/_ingest/pipeline/rosapi?pretty" -d'
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
' || ((code++))
echo "After command ${command_counter} our error code count is ${code}."
command_counter=$((command_counter + 1))

#Pipeline without categories since it only supports English
curl -fsSL -H "Content-Type: application/json" -XPUT "$1:$2/_ingest/pipeline/rosapi_jpn?pretty" -d'
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
' || ((code++))
echo "After command ${command_counter} our error code count is ${code}."
command_counter=$((command_counter + 1))

curl -fsSL -H "Content-Type: application/json" -XPUT "$1:$2/test_idx?include_type_name=true&pretty" -d'
{
  "mappings": {
    "rosette": {
      "properties": {
        "text" : { "type" : "text" },
        "name" : { "type" : "text" },
        "language" : { "type" : "keyword" },
        "category" : { "type" : "keyword" },
        "sentiment" : { "type" : "keyword" },
        "entities" : { "type" : "nested" },
        "translation" : { "type" : "text" }
      }
    }
  }
}
' || ((code++))
echo "After command ${command_counter} our error code count is ${code}."
command_counter=$((command_counter + 1))

curl -fsSL -H "Content-Type: application/json" -XPUT "$1:$2/test_idx/rosette/1?pretty&refresh=true&pipeline=rosapi" -d'
{
  "text": "Original Ghostbuster Dan Aykroyd, who also co-wrote the 1984 Ghostbusters film, couldn’t be more pleased with the new all-female Ghostbusters cast, telling The Hollywood Reporter, “The Aykroyd family is delighted by this inheritance of the Ghostbusters torch by these most magnificent women in comedy.”"
}
' || ((code++))
echo "After command ${command_counter} our error code count is ${code}."
command_counter=$((command_counter + 1))

curl -fsSL -H "Content-Type: application/json" -XPUT "$1:$2/test_idx/rosette/2?pretty&refresh=true&pipeline=rosapi_jpn" -d'
{
  "text": "バングラデシュ政府、ロヒンギャ難民の島への移動を計画 \nバングラデシュ政府、ロヒンギャ難民の島への移動を計画\n\nテンガール・チャール島は約10年前に、メグナ川の堆積土で形成され、高潮の際には数十センチの水に囲まれてしまう。道路や堤防などは築かれておらず、島を記載する地図はあまりない。\n\n約30キロ西には60万人が住むハティｱ島があり、現在の難民キャンプからの移動には9時間かかる。\n\nある地元政府関係者はAFP通信に対し、テンガール・チャール島について、「島に行けるのは冬のみで、海賊たちの隠れ家になっている」と語った。島を洪水から守るため植樹が行われているが、完了するまでには少なくとも10年がかかるという。同関係者は、「モンスーンの季節には完全に水浸しになってしまう」と話し、「あそこに住まわせるというのは、ひどいアイデアだ」と指摘した。\n\nImage caption 移住が計画されているテンガール・チャール島はハティア（Hatiya）島の近くにある\n\nミャンマーでは、ロヒンギャの人々は国境を接するバングラデシュからの不法移民として扱われており、国籍の取得ができずにいる。\n\n"
}
' || ((code++))
echo "After command ${command_counter} our error code count is ${code}."
command_counter=$((command_counter + 1))

curl -fsSL -H "Content-Type: application/json" -XPUT "$1:$2/test_idx/rosette/3?pretty&refresh=true&pipeline=rosapi" -d'
{
  "text" : "Vladimir Vladimirovich Nabokov was a Russian-American novelist and entomologist. His first nine novels were in Russian, and he achieved international prominence after he began writing English prose.",
  "name" : "Vladimir Nabokov"
}
' || ((code++))
echo "After command ${command_counter} our error code count is ${code}."
command_counter=$((command_counter + 1))

sleep 3

curl -fsSL -H "Content-Type: application/json" -XPOST "$1:$2/test_idx/_search?pretty" -d'
{
  "query": {
    "constant_score" : {
      "filter" : {
        "exists" : {"field" : "language"}
      }
    }
  }
}
' || ((code++))
echo "After command ${command_counter} our error code count is ${code}."

set +x
# The way we check to see if test.sh succeeded is to read in the output
# in verify.sh.  We then check the last line of the output file and
# based on the last line, decide if the test was successful.  This
# mechanism fails sporadically, and I suspect it is caused by a buffering
# issue.  Perhaps a brief snooze will make it more reliable.
sleep 2
echo "exit: $code"
