#!/bin/bash

FLARECLIENT_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
USAGE="Usage: ./hash.sh <propertyName> <value>"

# if not 2 args, print usage
if [ $# -ne 2 ]; then
    echo "$USAGE"
    exit 1
else
    PROPERTY=$1
    shift
    PASSWORD=$1
fi

# some Java parameters
if [ "$JAVA_HOME" != "" ]; then
    JAVA_HOME=$JAVA_HOME
fi

if [ "$JAVA_HOME" = "" ]; then
    echo "Searching for JAVA ..."
    OUTPUT="$(find / 2>/dev/null -name 'java')"
    echo OUTPUT: $OUTPUT
    for f in $OUTPUT
    do
	#echo "Processing $f"
       if [[ $f == *bin/java ]]
          then
          echo Java installed in $f
          LEN=${#f}
          #echo $LEN
          DIFF=`expr $LEN - 9`
          #echo $DIFF
          JAVA_HOME=${f:0:DIFF}
          echo using JAVA_HOME: $JAVA_HOME
          export JAVA_HOME
          break
       fi
    done
fi

JAVA=$JAVA_HOME/bin/java
CLASSPATH="$FLARECLIENT_HOME:$FLARECLIENT_HOME/lib/*"
CLASS=com.bcmcgroup.flare.client.Hash

exec "$JAVA" -cp "$CLASSPATH" $CLASS $PROPERTY $PASSWORD
