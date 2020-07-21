Testing the plugin
==================

`mvn clean install` from the top level directory first, then:

```
mvn docker:build
ROSETTE_API_KEY=<blah> mvn docker:run

<wait for it to fully start>
<look in the logs for the local port docker chose to map to (ie. 'Waiting on url http://localhost:32769')
<pass that port as an argument to the test script>

./test.sh <docker_port>
```
==================

There is a known issue where the docker container logs an error message for unknown reasons:
```
[ERROR] DOCKER> Cannot process chunk response: java.io.IOException: Bad file descriptor
```
This can be ignored.
 