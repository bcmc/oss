#!/bin/bash

FLARECLIENT_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
USAGE="Usage: ./listener.sh <1.0|1.1> [-p PORT]"

# if not 1 or 3 args, print usage
if [ $# -eq 0 ]; then
    echo "$USAGE"
    exit 1
elif [ $# -eq 1 ]; then
    PORT=8000
    VERSION=$1
elif [ $# -eq 2 -o $# -ge 4 ]; then
    echo "$USAGE"
    exit 1
elif [ $# -eq 3 ]; then
    VERSION=$1
    shift
    if [ $VERSION != "1.0" -a $VERSION != "1.1" ]; then
        echo "Invalid TAXII version!"
        echo "$USAGE"
        exit 1
    fi
    OPTION=$1
    shift
    if [ $OPTION != "-p" ]; then
        echo "$USAGE"
        exit 1
    fi
    PORT=$1
    if [[ $PORT = *[!0-9]* ]]; then
        echo "Port must be an integer 0-65535"
        exit 1
    elif [ $PORT -lt 0 -o $PORT -gt 65535 ]; then
        echo "Port must be an integer 0-65535"
        exit 1
    fi
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

if [ "$VERSION" = "1.0" ]; then
    CLASS=com.bcmcgroup.flare.client.Listener10
elif [ "$VERSION" = "1.1" ]; then
    CLASS=com.bcmcgroup.flare.client.Listener11
else
    echo "Invalid TAXII version"
    echo "$USAGE"
fi
exec "$JAVA" -cp "$CLASSPATH" $CLASS -p $PORT
