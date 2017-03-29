Rosette Plugin for ElasticSearch
================================

A Document Enrichment plugin that brings the Rosette API to Elasticsearch.

Please see the [Full Documentation](docs/Rosette-API-Plugin-for-Elasticsearch-Doc-Enrichment.md) for details on how to use the plugin.

To build, make sure to have the ROSETTE_API_KEY environment variable set to your Rosette API key. (ie. `export ROSETTE_API_KEY=<YOUR_API_KEY>`)

Then run `mvn clean install`

The plugin zip can then be found in `plugin/target/releases/` ready to be installed into the appropriate version of Elasticsearch.

You can also [Test with Docker](docker/README.md)
