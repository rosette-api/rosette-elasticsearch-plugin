#!/usr/bin/env bash

if [[ "$(tail -1 "$1" 2>/dev/null)" !=  "exit: 0" ]] ; then
    echo "There are test failures. Exiting..."
    cat $1
    exit 1
fi

if grep -E -i 'exception|"failed":[1-9]' "$1" ; then
    echo "Test query failed! See $1 for details. Exiting..."
    cat $1
    exit 1
fi
