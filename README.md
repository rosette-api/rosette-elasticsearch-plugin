<a href="https://www.rosette.com"><img src="https://s3.amazonaws.com/styleguide.basistech.com/logos/rosette-logo.png" width="181" height="47" /></a>

---

[![Build Status](https://travis-ci.org/rosette-api/rosette-elasticsearch-plugin.svg?branch=master)](https://travis-ci.org/rosette-api/rosette-elasticsearch-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/com.rosette.elasticsearch/rosette-elasticsearch-plugin?color=blue)](https://mvnrepository.com/artifact/com.rosette.elasticsearch/rosette-elasticsearch-plugin)

# Rosette Plugin for Elasticsearch

A Document Enrichment plugin that brings the Rosette API to Elasticsearch.

This ingest plugin allows Elasticsearch users to perform Language Identification, Sentiment Analysis, Entity Extraction,
Categorization, and Name Translation on documents as they're indexed.

## Rosette API
The Rosette Text Analytics Platform uses natural language processing, statistical modeling, and machine learning to
analyze unstructured and semi-structured text across 364 language-encoding-script combinations, revealing valuable
information and actionable data. Rosette provides endpoints for extracting entities and relationships, translating and
comparing the similarity of names, categorizing and adding linguistic tags to text and more.

## Rosette API Access
- Rosette Cloud [Sign Up](https://developer.rosette.com/signup)
- Rosette Enterprise [Evaluation](https://www.rosette.com/product-eval/)

## Quick Start

## How to Install
There are two common ways to install the plugin into Elasticsearch. (Make sure the version of the plugin matches the version of Elasticsearch you are using!)

- Download the desired version of the plugin from the Releases tab on github
  - Install using: `bin/elasticsearch-plugin install file:///<path_to_plugin>`
- Install from a deployed maven artifact:
`bin/elasticsearch-plugin install com.rosette.elasticsearch:rosette-elasticsearch-plugin:<plugin_version>`


#### Note on Versioning:
The plugin uses semantic versioning. The first three numbers describe which version of Elasticsearch this version of the plugin is compatible with, and the last number indicates the version of the plugin within that Elasticsearch version.

For instance, `5.3.0.1` is the second patch version of the plugin for Elasticsearch 5.3.0.

## How to Build
Building the plugin requires a Rosette API key. If you don’t already have a Rosette API developer account, head over to [developer.rosette.com](https://developer.rosette.com/signup) to get your free Rosette API key.

Place the key in the ROSETTE_API_KEY environment variable (ie. `export ROSETTE_API_KEY=<YOUR_API_KEY>`)

Then run `mvn clean install`

The plugin zip can then be found in `plugin/target/releases/` ready to be installed into the appropriate version of Elasticsearch.

You can also [Test with Docker](docker/README.md)

#### Documentation & Support
- [Full Plugin Documentation](docs/Rosette-API-Plugin-for-Elasticsearch-Doc-Enrichment.md)
- [Rosette Platform API](https://developer.rosette.com/features-and-functions)
- [Rosette Platform Release Notes](https://support.rosette.com/hc/en-us/articles/360018354971-Release-Notes)
- [Support](https://support.rosette.com)
- [Plugin License: Apache 2.0](https://github.com/rosette-api/python/blob/develop/LICENSE.txt)





