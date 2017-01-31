####DISCLAIMER
>>ANY DOWNLOAD AND USE OF THIS UNSUPPORTED SOFTWARE PROGRAM PRODUCT IS DONE AT
>>THE USERS OWN RISK AND THE USER WILL BE SOLELY RESPONSIBLE FOR ANY DAMAGE TO
>>– WITHOUT LIMITATION – ANY COMPUTER SYSTEM OR LOSS OF DATA THAT RESULTS FROM
>>SUCH ACTIVITIES. SHOULD IT PROVE DEFECTIVE,     USER ASSUMES THE COST OF ALL
>>NECESSARY SERVICING, REPAIR AND/OR CORRECTION.     IT IS THEREFORE UP TO THE
>>USER TO TAKE ADEQUATE PRECAUTION AGAINST POSSIBLE DAMAGES     RESULTING FROM
>>THIS UNSUPPORTED SOFTWARE.                                                 

 
        Description: Simple set of scripts to automate AIS data pulls into MISP to enable:
        
        + Quick correlation, taxonomy tagging, visualization and review of data.       
        + Automation can also be used as standalone scripts                          
        + *Scripts are simply a working POC and written to only support TAXII 1.1*    

                                     _____  .___  _________                           
                                    /  _  \ |   |/   _____/                           
                                   /  /_\  \|   |\_____  \                            
                                  /    |    \   |/        \                           
                                  \____|__  /___/___v1.0_  /                          
                                          \/            \/                            
                                                    AIS Automation
                                                    Author: Cory Kennedy (@corykennedy)
                                                    
##[Dependancies]
```bash
- Active AIS Participation | https://www.us-cert.gov/ais                    
- Working Flare instance                           
- Working MISP instance                            
- Working CTI-Toolkit on MISP server | https://github.com/certau/cti-toolkit.git
```
                             
##[Installation Summary]
```bash
- Install scripts & crontabs onto your MISP and Flare servers               
- Need help with crontabs?  Try: https://crontab.guru/                   
- The below will execute the scripts at 2:30am daily and log all output
```
                               
##[FLARE Script Installation]
```bash
1. git clone -b Automation --single-branch https://github.com/NoDataFound/oss.git oss-automation
2. Move scripts from oss-automation/scripts/flare to your flare server.
            * Example: mv oss-automation/scripts/flare/* /opt/Flare/scripts/
3. Install crontabs
```
###[FLARE Crontab Installation]
```bash
1. From a terminal type: crontab -e
2. Copy and paste the below into your crontab
30 2 * * * /opt/Flare/scripts/CISCP.sh &>/opt/Flare/scripts/logs/CISCP_`date +\%y-\%m-\%d`.out
32 2 * * * /opt/Flare/scripts/AIS.sh &>/opt/Flare/scripts/logs/AIS_`date +\%y-\%m-\%d`.out
3. Save crontab
```
Flare server is complete. Proceed to MISP

##[MISP Script Installation]

```bash
1. git clone -b Automation --single-branch https://github.com/NoDataFound/oss.git oss-automation
2. Move scripts from oss-automation/scripts/misp to your MISP server
            * Example: mv oss-automation/scripts/misp/* /home/misp/scripts/
3. Install crontabs
```

###[MISP Crontab Installation]
```bash
1. From a terminal type: crontab -e
2. Copy and paste the below into your crontab
30 2 * * * /home/misp/scripts/MISP.sh
3. Save crontab
```
Note: MISP server is complete!

