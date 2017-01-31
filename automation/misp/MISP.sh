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
# Simple script to automate AIS data into MISP
# Author: Cory Kennedy (@corykennedy)
# -----------------------------------------------------------------------------
#      _____  .___  _________               _____  .___  ___________________
#     /  _  \ |   |/   _____/    .__       /     \ |   |/   _____/\______   \
#    /  /_\  \|   |\_____  \   __|  |___  /  \ /  \|   |\_____  \  |     ___/
#   /    |    \   |/        \ /__    __/ /    Y    \   |/        \ |    |
#   \____|__  /___/TAXII1.1 /    |__|    \____|__  /___/_______  / |____|
#           \/            \/                     \/            \/
# Usage: ./MISP.sh
# -----------------------------------------------------------------------------
#Transfer files from AIS host, then remove source files.
#Assumes you have ssh-copy-id yourself onto your flare host.
rsync -avz --remove-source-files -e ssh user@your.flare.host:/opt/Flare/TRANSFER /home/misp/feeds/
#Move into our working directory
cd /home/misp/feeds/TRANSFER/
#Find compressed AIS files and uncompress them
cat *.tgz | tar -zxvf - -i
#Import into MISP
python /home/misp/cti-toolkit/stixtransclient.py --file /home/misp/feeds/TRANSFER/ -r --misp --misp-url https://misp --misp-key [YOURMISPKEY] --misp-threat 2 --misp-distribution 0 --misp-info "AIS"
#Cleanup transfered files after completion
rm -rf /home/misp/feeds/TRANSFER/
# ------------------------------DISCLAIMER-------------------------------------#
# ANY DOWNLOAD AND USE OF THIS UNSUPPORTED SOFTWARE PROGRAM PRODUCT IS DONE AT #
# THE USERS OWN RISK AND THE USER WILL BE SOLELY RESPONSIBLE FOR ANY DAMAGE TO #
# – WITHOUT LIMITATION – ANY COMPUTER SYSTEM OR LOSS OF DATA THAT RESULTS FROM #
# SUCH ACTIVITIES. SHOULD IT PROVE DEFECTIVE,     USER ASSUMES THE COST OF ALL #
# NECESSARY SERVICING, REPAIR AND/OR CORRECTION.     IT IS THEREFORE UP TO THE #
# USER TO TAKE ADEQUATE PRECAUTION AGAINST POSSIBLE DAMAGES     RESULTING FROM #
# THIS UNSUPPORTED SOFTWARE.                                                   #
# ------------------------------DISCLAIMER-------------------------------------#
