# Rosette API Plugin for Elasticsearch for Document Enrichment

## Overview
Basis Technology has written a plugin for Elasticsearch as a means of calling the Rosette API endpoints at indexing time, to annotate unstructured textual fields in a document with text analytic results in separate “metadata” fields. This document enrichment from Rosette allows refinement of search results through these Rosette functions:
- **Language identification** - tag the language of each document 
- **Sentiment analysis** - tag the sentiment of each document or sentiment surrounding each entity (person, location, organization, etc.) 
- **Categorization** - tag each document with its primary topic (sports, home/garden, politics, etc.)
- **Entity extraction and linking** - find the key entities in each document for faceted searching and link entities to Wikidata entries
- **Name translation** - Translate names into English from 11 languages such as Arabic, Chinese, Korean, Japanese, and Russian.

Note: There are two other Rosette plugins for Elasticsearch which offer these NLP functions through the Rosette SDK.
- *Identity Resolution Plugin:* Fuzzy name matching (across 11 types of variations and across languages and scripts)
- *Multilingual Search Enablement Plugin:* Text pre-processing (lemmatization, tokenization, noun decompounding, etc.) to enable search in 40+ languages, while enhancing precision and recall
See [Rosette’s Elasticsearch Plugins](https://www.rosette.com/elastic/) or contact info@rosette.com for more information.

## Quick Start Guide
### Overview
Rosette functionality is called through an ingest node of Elasticsearch that pre-processes documents before indexing takes place. You define a pipeline that specifies the series of processors that transforms or enriches the document. See the [Ingest APIs of Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/master/ingest-apis.html) for more about how to create, add, or delete pipelines.
### Version Compatibility
The plugin uses semantic versioning. The first three numbers indicate the version of Elasticsearch that the plugin is compatible with, and the last number indicates the version of the plugin within that Elasticsearch version.
For example, 5.3.1.1 is the second patch version of the plugin for Elasticsearch 5.3.1.

### Installation
1. Install Elasticsearch 
(Make sure the Elasticsearch version is compatible with the Document Enrichment Plugin or the plugin will not install.)

2. Install the Rosette API plugin (where x.x.x.x stands for the version number) by navigating to the elasticsearch-x.x.x root directory and running the following. 
```sh
bin/elasticsearch-plugin install file:///path/to/rosette-elasticsearch-plugin-x.x.x.x.zip
```
Use the absolute file path to refer to the plugin zip. You may be prompted to grant permissions necessary for the plugin to function. The Document Enrichment plugin is now in plugins/rosapi.

3. Input your Rosette API key. If you don’t already have one, [sign up for a free or paid Rosette API plan](https://developer.rosette.com/signup), or for those who need greater speed or security, contact our sales team (sales@basistech.com) to learn about our on-premise version of Rosette API. You can set the key in one of two ways:
* as an environment variable `export ROSETTE_API_KEY=<your key here>`
* as an Elasticsearch setting `ingest.rosette.api_key: <your key here>` in the `config/elasticsearch.yml` file

There is also an option to specify an alternative URL to use for on-premise installations of Rosette API. Once again this is either via an environment variable `export ROSETTE_API_URL=<alternative rosapi url>` or via a config setting `ingest.rosette.api_url: <alternative rosapi url>`

### Configuration
Each Rosette function is implemented as an ingest processor, which is configured as part of an ingest pipeline. Ingest pipelines are specified when indexing a document.

For example, here's a simple pipeline that runs language identification:
* First create the pipeline:
```sh
curl -XPUT localhost:9200/_ingest/pipeline/lang_id -d '
{
"processors": [
    {
    "ros_language" :
        { "field" : "text", "target_field" : "language" }
    }
            ]
}
```
* Then index a document with that pipeline:
```sh
curl -XPOST localhost:9200/indexName/mappingName?pipeline=lang_id -d '
{ "text" : "This is a document containing English text" }
```
See the [Elasticsearch Ingest configuration](https://www.elastic.co/guide/en/elasticsearch/reference/master/ingest.html) for more details.

## How It Works: Rosette Processors
Below are details of how to call each Rosette processor through the plugin. Note that entity extraction, entity linking, and entity-level sentiment analysis can be completed with one call, but categorization, name translation, and sentiment analysis are each separate calls. (See more about [Rosette API pricing plans](https://www.rosette.com/pricing/), which range from free to high-call volume plans.)

For full details of acceptable parameter values see the [online Rosette API documentation](https://developer.rosette.com/features-and-functions) after you have [signed up to receive an API key](https://developer.rosette.com/).

### Language Identification

**Function:**
Given a text field, Rosette detects the language it is most likely to be, and indexes the [identified language](https://developer.rosette.com/features-and-functions#language-support26) in the record.

**Parameters:**

|Name 	| Required 	| Default 		| Description |
|--------|-----------|----------------|--------------|
|field 	| yes 	|  			| Field containing input text|
|target_field 	| no 	| ros_language 	| Field to hold output|

**Examples:**

Configuration:
```sh
{
  "ros_language" : {
    "field" : "text",
    "target_field" : "language"
  }
}
```
Output:
```sh
{
  "text" : "This is English",
  "language" : "eng"
}
```
### Entity Extraction, Linking, and Entity-Level Sentiment

**Function:**
Extracts entities (identifies 18 [entity types](https://developer.rosette.com/features-and-functions#-entity-types) in [20 languages](https://developer.rosette.com/features-and-functions#language-support24) from a body of text and stores them along with their QID (wikidata ID number) and entity type. 

Optionally, Rosette can translate the entity mentions to English ([9 supported languages](https://developer.rosette.com/features-and-functions#language-support44)) and determine the sentiment (pos, neg, or neu) surrounding an entity.

**Parameters:**

Name	| Required	| Default	| Description
--------|-----------|------------|--------------------
field	| yes	|		| Field containing input text
target_field	| no	| ros_entities	| Field to hold output object
include_translation	| no	| false	| Boolean indicating whether entity mentions should be translated
translation_language	| no	| eng	| Target language to translate entity mentions into
include_sentiment	| no	| false	| Boolean indicating whether to include entity-level sentiment
include_offsets	| no	| false	| Boolean indicating whether to include entity offsets

**Examples:**

**Configuration:**
```sh
{
  "ros_entities" : {
    "field" : "text",
    "target_field" : "entities",
    "include_translate" : true,
    "include_sentiment" : true,
    "include_offsets" : true
  }
}
```
**Output:**
```sh
{
  "text" : "Bill Murray will appear in new Ghostbusters film.",
  "entities" : [
    {
      "mention" : "Bill Murray",
      "type" : "PERSON",
      "entityId" : "Q29250",
      "translation" : "Bill Murray",
      "sentiment" : "neu",
      "count" : 1,
      "offsets" : [...]
    },
    {
      "mention" : "Ghostbusters",
      "type" : "PRODUCT",
      "entityId" : "Q108745",
      "translation" : "Ghostbusters",
      "sentiment" : "neu",
      "count" : 1,
      "offsets" : [...]
    }
  ]
}
```
### Sentiment Analysis

**Function:**
Rosette detects the overall sentiment of a body of text as negative (neg), neutral (neu) or positive (pos). (https://developer.rosette.com/features-and-functions#language-support)

**Parameters:**

Name	| Required	| Default	| Description
--------|-----------|------------|---------------------------------------------
field	| yes	|		| Field containing input text
target_field	| no	| ros_sentiment	| Field to hold output object

**Examples:**

**Configuration:**
```sh
{
  "ros_sentiment" : {
    "field" : "text",
    "target_field" : "sentiment"
  }
}
```
**Output:**
```sh
{
  "text" : "Original Ghostbuster Dan Aykroyd, who also co-wrote the 1984 Ghostbusters film, couldn’t be more pleased with the new all-female Ghostbusters cast, telling The Hollywood Reporter",
  "sentiment" : "pos"
}
```
### Categorization

**Function:**
Rosette classifies a text field as a member of a general category. Default categories are the tier 1 categories of the IAB Quality Assurance Guidelines (QAG) Taxonomy. (https://www.iab.com/guidelines/iab-quality-assurance-guidelines-qag-taxonomy/)

**Parameters:**

Name	| Required	| Default	| Description
---------|-----------|----------|-------------------------------------------
field	| yes	|		| Field containing input text
target_field	| no	| ros_category	| Field to hold output ([output values](https://developer.rosette.com/features-and-functions#categorization))

**Examples:**

**Configuration:**
```sh
{
  "ros_category" : {
    "field" : "text",
    "target_field" : "category"
  }
}
```
**Output:**
```sh
{
  "text" : "This is an article about the arts.",
  "category" : "ARTS_AND_ENTERTAINMENT"
}
```
### Name Translation

**Function:**
Accepts a field that it assumes is a name (of a person, location, or organization) and translates the name to the target language. 

A name such as Ichiro Suzuki is of “language origin” Japanese, while the “script” is English, whereas 鈴木一郎 is of “language origin” Japanese and “script” Japanese.

**Parameters:**

Name	| Required	| Default			| Description
--------|-----------|-------------------|--------------------------------
field	| yes	|				| Field containing input text
target_field	| no	| ros_translation		| Field to hold output object
target_language	| no	| eng		| Language to translate to ([language codes](https://developer.rosette.com/features-and-functions#language-support44))
target_script		| no	|Zyyy (Unknown)	|Script to translate to ([script codes](https://developer.rosette.com/features-and-functions#language-support44))
entity_type	| no	| PERSON	| Entity type of the name being translated: PERSON (default), LOCATION, or ORGANIZATION
source_language | no	| xxx (Unknown)	| Language of use of the name being translated—that is, which language is the name written in. ([language codes](https://developer.rosette.com/features-and-functions#language-support44))
Source_script	| no	| Zyyy (Unknown)	| Script of the name being translated ([script codes](https://developer.rosette.com/features-and-functions#language-support44))
source_language_of_origin	| no	| xxx (Unknown)	| Language of origin of the name being translated  ([language codes](https://developer.rosette.com/features-and-functions#language-support44))

**Examples:**

**Configuration:**
```sh
{
  "ros_language" : {
    "field" : "name",
    "target_field" : "translation",
    "target_language" : "eng",
    "entity_type" : "PERSON"
  }
}
```
**Output:**
```sh
{
  "name" : "マット・デイモン",
  "translation" : "Matt Damon"
}
```
### Sample Ingest Pipeline
```sh
{
  "description" : "Illustrative ingest pipeline that runs Rosette Api on documents indexed in the text field. Errors in sentiment (usually due to limited language support) send a document to a separate index. Errors in categorization (also usually language support) are ignored completely. Overall errors won't stop the ingest process but an error field will be populated with the cause of the error.",
  "processors" : [
    {
      "ros_language" : {
        "field" : "text",
        "target_field" : "language"
      }
    },
    {
      "ros_entities" : {
        "field" : "text",
        "target_field" : "entities",
        "include_translate" : true,
        "include_sentiment" : true
      }
    },
    {
      "ros_sentiment" : {
        "field" : "text",
        "target_field" : "sentiment",
        "on_failure" : [
          {
            "set" : {
              "field" : "_index",
              "value" : "no_sent_index"
            }
          }
        ]
      },
    },
    {
      "ros_category" : {
        "field" : "text",
        "target_field" : "category",
        "ignore_failure" : true
      }
    }
  ],
  "on_failure" : [
    {
      "set" : {
        "field" : "error",
        "value" : "{{ _ingest.on_failure_message }}"
      }
    }
  ]
}
```






