#!/bin/bash

FLARECLIENT_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
USAGE="Usage: ./outbox.sh <1.0|1.1> <Directory> ... Directory monitoring is automatically recursive."

# if not 2 or 3 args specified, show usage
if [ $# -lt 2 ]; then
    echo "$USAGE"
    exit 1
elif [ $# -gt 3 ]; then
    echo "$USAGE"
    exit 1
else
    VERSION=$1
    shift
    DIRECTORY=$1
    shift
fi

if [ ! -d "$DIRECTORY" ]; then
    echo "$DIRECTORY is not a directory!"
    exit 1
fi

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

if [ "$VERSION" = "1.0" ]; then
    CLASS=com.bcmcgroup.flare.client.PublisherOutbox10
elif [ "$VERSION" = "1.1" ]; then
    CLASS=com.bcmcgroup.flare.client.PublisherOutbox11
else
    echo "Invalid TAXII version"
    echo "$USAGE"
fi

exec "$JAVA" -classpath "$CLASSPATH" $CLASS "$DIRECTORY"

