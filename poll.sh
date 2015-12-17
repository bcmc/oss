#!/bin/bash

FLARECLIENT_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
USAGE="Usage: ./poll.sh <1.0|1.1> <Feed Name> [-b Exclusive_Begin_Timestamp] [-e Inclusive_End_Timestamp]"

# if not 2, 4, or 6 args, print usage
if [ $# -eq 2 ]; then
    VERSION=$1
    shift
    FEEDNAME=$1
elif [ $# -eq 4 ]; then
    VERSION=$1
    shift
    FEEDNAME=$1
    shift
    OPTION=$1
    if [ $OPTION == "-b" ]; then
        shift
        BEGINTIME=$1
    elif [ $OPTION == "-e" ]; then
        shift
        ENDTIME=$1
    else
        echo "$USAGE"
        exit 1
    fi
elif [ $# -eq 6 ]; then
    VERSION=$1
    shift
    FEEDNAME=$1
    shift
    OPTION=$1
    if [ $OPTION == "-b" ]; then
        shift
        BEGINTIME=$1
    elif [ $OPTION == "-e" ]; then
        shift
        ENDTIME=$1
    else
        echo "$USAGE"
        exit 1
    fi
    shift
    OPTION=$1
    if [ $OPTION == "-b" ]; then
        shift
        BEGINTIME=$1
    elif [ $OPTION == "-e" ]; then
        shift
        ENDTIME=$1
    else
        echo "$USAGE"
        exit 1
    fi
else
    echo "$USAGE"
    exit 1
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
    CLASS=com.bcmcgroup.flare.client.PollFeed10
elif [ "$VERSION" = "1.1" ]; then
    CLASS=com.bcmcgroup.flare.client.PollFeed11
else
    echo "Invalid TAXII version"
    echo "$USAGE"
fi

if [[ -n "$BEGINTIME" ]]; then
    if [[ -n "$ENDTIME" ]]; then
        exec "$JAVA" -cp "$CLASSPATH" $CLASS "$FEEDNAME" -b $BEGINTIME -e $ENDTIME
    else
        exec "$JAVA" -cp "$CLASSPATH" $CLASS "$FEEDNAME" -b $BEGINTIME
    fi
else
    if [[ -n "$ENDTIME" ]]; then
        exec "$JAVA" -cp "$CLASSPATH" $CLASS "$FEEDNAME" -e $ENDTIME
    else
        exec "$JAVA" -cp "$CLASSPATH" $CLASS "$FEEDNAME"
    fi
fi
