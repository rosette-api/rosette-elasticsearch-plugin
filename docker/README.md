Testing the plugin
==================

`mvn clean install` from the top level directory first, then:

```
./build.sh
ROSETTE_API_KEY=<blah> ./run.sh
<wait for it to fully start>
./test.sh
<tweak test.sh to add index and query tests as you wish>
```
