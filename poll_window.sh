#!/bin/bash

taxii_version="1.1"
collection="Dagobah"
start="2016-01-01T12:00:00Z"

if [ ! -f begin.tmp ];
  then
    last_polled=`date`
    echo $last_polled > begin.tmp
    ./poll.sh $taxii_version $collection -b "$start" -e `date -d now -u +"%Y-%m-%dT%H:%M:%SZ"`
    exit
fi

begin=`cat begin.tmp`

begin_formatted=`date -d "$begin" -u +"%Y-%m-%dT%H:%M:%SZ"`
end_formatted=`date -d now -u +"%Y-%m-%dT%H:%M:%SZ"`

./poll.sh $taxii_version $collection -b "$begin_formatted" -e "$end_formatted"

date -d now > begin.tmp
