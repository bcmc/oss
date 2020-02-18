#!/bin/bash
# ------------------------------DISCLAIMER-------------------------------------#
# ANY DOWNLOAD AND USE OF THIS UNSUPPORTED SOFTWARE PROGRAM PRODUCT IS DONE AT #
# THE USERS OWN RISK AND THE USER WILL BE SOLELY RESPONSIBLE FOR ANY DAMAGE TO #
# – WITHOUT LIMITATION – ANY COMPUTER SYSTEM OR LOSS OF DATA THAT RESULTS FROM #
# SUCH ACTIVITIES. SHOULD IT PROVE DEFECTIVE,     USER ASSUMES THE COST OF ALL #
# NECESSARY SERVICING, REPAIR AND/OR CORRECTION.     IT IS THEREFORE UP TO THE #
# USER TO TAKE ADEQUATE PRECAUTION AGAINST POSSIBLE DAMAGES     RESULTING FROM #
# THIS UNSUPPORTED SOFTWARE.                                                   #
# ------------------------------DISCLAIMER-------------------------------------#
# Simple shell script to pull the AIS feed from AIS Data
# There are much better ways to do this. This is only a quick working POC
# Author: Cory Kennedy (@corykennedy)
#                          _____  .___  _________
#                         /  _  \ |   |/   _____/
#                        /  /_\  \|   |\_____  \
#                       /    |    \   |/        \
#                       \____|__  /___/___v1.0_  /
#                               \/             \/
#                                   AIS Automation
# Usage: ./AIS.sh
# -----------------------------------------------------------------------------
#Move into our working directory
cd /opt/Flare/
#Poll AIS server for AIS feed using TAXII 1.1 and format date parameters
./poll.sh 1.1 AIS -b $(date -d '1 days ago' --utc "+%FT%T.%N" | sed -r 's/[[:digit:]]{6}$/Z/') -e $(date -d --utc "+%FT%T.%N" | sed -r 's/[[:digit:]]{6}$/Z/')
#Move into our working feed directory
cd /opt/Flare/subscribeFeeds/AIS/
#Prepare all files for transport
tar -zcvf /opt/Flare/TRANSFER/AIS_`date +%y-%m-%d`.tgz .
#Cleanup files after completion
rm -rf /opt/Flare/subscribeFeeds/AIS/*
# ------------------------------DISCLAIMER-------------------------------------#
# ANY DOWNLOAD AND USE OF THIS UNSUPPORTED SOFTWARE PROGRAM PRODUCT IS DONE AT #
# THE USERS OWN RISK AND THE USER WILL BE SOLELY RESPONSIBLE FOR ANY DAMAGE TO #
# – WITHOUT LIMITATION – ANY COMPUTER SYSTEM OR LOSS OF DATA THAT RESULTS FROM #
# SUCH ACTIVITIES. SHOULD IT PROVE DEFECTIVE,     USER ASSUMES THE COST OF ALL #
# NECESSARY SERVICING, REPAIR AND/OR CORRECTION.     IT IS THEREFORE UP TO THE #
# USER TO TAKE ADEQUATE PRECAUTION AGAINST POSSIBLE DAMAGES     RESULTING FROM #
# THIS UNSUPPORTED SOFTWARE.                                                   #
# ------------------------------DISCLAIMER-------------------------------------#
