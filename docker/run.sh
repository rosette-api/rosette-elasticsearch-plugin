#!/usr/bin/env bash

docker run \
    -p 9200:9200 \
    -e ROSETTE_API_KEY=${ROSETTE_API_KEY} \
    -e ROSETTE_API_URL=${ROSETTE_API_URL:-https://api.rosette.com/rest/v1} \
    basistechnologycorporation/rosette-elasticsearch
