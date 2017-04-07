Rosette Plugin for ElasticSearch
================================

A Document Enrichment plugin that brings the [Rosette API](https://developer.rosette.com/) to Elasticsearch. 

This Ingest plugin allows Elasticsearch users to perform Language Identification, Sentiment Analysis, Entity Extraction, 
Categorization, and Name Translation on documents as they're indexed.

## How to Install
There are two common ways to install the plugin into Elasticsearch. (Make sure the version of the plugin matches the version of Elasticsearch you are using!)

- Download the desired version of the plugin from the Releases tab on github
  - Install using: `bin/elasticsearch-plugin install file:///<path_to_plugin>`
- Install from a deployed maven artifact: 
`bin/elasticsearch-plugin install com.rosette.elasticsearch:rosette-elasticsearch-plugin:<plugin_version>`

Please see the [Full Documentation](docs/Rosette-API-Plugin-for-Elasticsearch-Doc-Enrichment.md) for details on how to use the plugin.

#### Note on Versioning:
The plugin uses semantic versioning. The first three numbers describe which version of Elasticsearch this version of the plugin is compatible with, and the last number indicates the version of theplugin within that Elasticsearch version.

For instance, `5.3.0.1` is the second patch version of the plugin for Elasticsearch 5.3.0.

## How to Build
Building the plugin requires a Rosette API key. If you don’t already have a Rosette API developer account, head over to [developer.rosette.com](http://developer.rosette.com/signup) to get your free Rosette API key.

Place the key in the ROSETTE_API_KEY environment variable (ie. `export ROSETTE_API_KEY=<YOUR_API_KEY>`)

Then run `mvn clean install`

The plugin zip can then be found in `plugin/target/releases/` ready to be installed into the appropriate version of Elasticsearch.

You can also [Test with Docker](docker/README.md)
