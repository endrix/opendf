#!/bin/sh
if [ $# -eq 0 ]; then
    echo Please indicate the target: -clean, Cal2C/cal2c.d.byte, Cal2C/cal2c.byte or Cal2C/cal2c.native
else
    SYSTEM=`uname -s`
    if [ $SYSTEM == "Linux" ]; then
        ocamlbuild $*
    else
        ocamlbuild -classic-display -no-log $*
    fi
fi
