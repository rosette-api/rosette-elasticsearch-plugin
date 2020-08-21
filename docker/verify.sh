#!/usr/bin/env bash

# The way we check to see if test.sh succeeded is to read in the output
# in this script.  We then check the last line of the output file and
# based on the last line, decide if the test was successful.  This
# mechanism fails sporadically, and I suspect it is caused by this
# script being executed before the file is finished writing out.
# Perhaps a brief snooze will make it more reliable.
sleep 5

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
