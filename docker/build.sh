#!/usr/bin/env bash -e

DIR="$(cd "$( dirname "${BASH_SOURCE[0]}")" && pwd )"
TOP_DIR="$(cd "$( dirname "${BASH_SOURCE[0]}")/.." && pwd )"

count=$(find "$TOP_DIR/target/releases" -name "*.zip" 2>/dev/null | wc -l)

if [ $count -eq 0 ] ; then
    echo "ERROR: no plugin files in $TOP_DIR/target/releases"
    exit 1
fi

mkdir -p $DIR/plugins 2>/dev/null
find $DIR/plugins -name "*.zip" -exec rm -f {} \;
cp -p $TOP_DIR/target/releases/*.zip $DIR/plugins

elasticsearch_version=$(<$TOP_DIR/target/test-classes/elasticsearch.version)
sed 's/{elasticsearch.version}/'"${elasticsearch_version}"'/' $DIR/Dockerfile.tmpl > $DIR/Dockerfile

(cd "$DIR"; docker build -t basistechnologycorporation/rosette-elasticsearch .)
