#!/bin/sh
# creates a sed "editor" that substitutes $2 for every occurence of $1
# $1 is assumed to be a known string, with no funky characters
# $2 is an unknown file path, which might need to be escaped so that 
#    sed/regexp characters are handled properly
# If there are more arguments, $3 and $4 forms the next pair and so on...

# In the second "substitution" part of a sed s-command, the following
# characters are "funky" \ / & (but we get away with ^ $ . [ *). 
# Admittedly, neither \ nor & is very useful in paths.

RESULT="sed"
while [ $# -ne 0 ]; do

RHS=$(echo $2 | sed -e 's/\\/[\\]/g' -e 's/[/]/\\\//g' -e 's/&/\\\&/g')
RESULT="$RESULT -e 's/$1/$RHS/g'"
shift 2
done

echo $RESULT