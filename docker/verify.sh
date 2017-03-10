#!/usr/bin/env bash

if grep -E -i 'exception|"failed":[1-9]' $1; then
    echo "Test query failed! See $1 for details. Exiting...";
    exit 1;
fi
