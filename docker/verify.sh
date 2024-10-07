#!/usr/bin/env bash

################################################################################
# This data and information is proprietary to, and a valuable trade secret
# of, Basis Technology Corp.  It is given in confidence by Basis Technology
# and may only be used as permitted under the license agreement under which
# it has been distributed, and in no other way.
#
# Copyright (c) 2024 Basis Technology Corporation All rights reserved.
#
# The technical data and information provided herein are provided with
# `limited rights', and the computer software provided herein is provided
# with `restricted rights' as those terms are defined in DAR and ASPR
# 7-104.9(a).
#
################################################################################

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
