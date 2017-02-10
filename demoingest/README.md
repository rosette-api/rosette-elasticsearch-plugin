# Elasticsearch RosetteAPI Demo Ingester

- Start Elasticsearch (preferably 5.0.2)
- To ingest documents run:  
`mvn exec:java -Dexec.mainClass="com.rosette.elasticsearch.demoingest.DemoIngestCLI" -Dexec.args="-t http://localhost:9300 -d dir/with/docs -k <YOUR_API_KEY>"`
- To see the results run:  
`curl -XPOST localhost:9200/demo/_search?pretty -d '{ "query" : { "match_all" : {} } }'`