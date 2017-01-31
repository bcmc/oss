 ------------------------------DISCLAIMER-------------------------------------
 ANY DOWNLOAD AND USE OF THIS UNSUPPORTED SOFTWARE PROGRAM PRODUCT IS DONE AT
 THE USERS OWN RISK AND THE USER WILL BE SOLELY RESPONSIBLE FOR ANY DAMAGE TO
 – WITHOUT LIMITATION – ANY COMPUTER SYSTEM OR LOSS OF DATA THAT RESULTS FROM
 SUCH ACTIVITIES. SHOULD IT PROVE DEFECTIVE,     USER ASSUMES THE COST OF ALL
 NECESSARY SERVICING, REPAIR AND/OR CORRECTION.     IT IS THEREFORE UP TO THE
 USER TO TAKE ADEQUATE PRECAUTION AGAINST POSSIBLE DAMAGES     RESULTING FROM
 THIS UNSUPPORTED SOFTWARE.                                                   
 ------------------------------DISCLAIMER-------------------------------------
 Description: Simple set of scripts to automate AIS data pulls into MISP to   
 enable quick correlation, taxonomy tagging and review of data.               
 Automation can also be used as standalone scripts                            
 Scripts are simply a working POC and written to only support TAXII 1.1       
 Author: Cory Kennedy (@corykennedy)                                          
                             _____  .___  _________                           
                            /  _  \ |   |/   _____/                           
                           /  /_\  \|   |\_____  \                            
                          /    |    \   |/        \                           
                          \____|__  /___/___v1.0_  /                          
                                  \/            \/                            
                                      AIS Automation                          
==============================================================================
                        -=[Instructions Summary]=-                            

  * Install scripts & crontabs onto your MISP and Flare servers               
     * Need help with crontabs?  Try: https://crontab.guru/                   
     * The below will execute the scripts at 2:30am daily and log all output  

==============================================================================
                            -=[Dependancies]=-                                
                          1. Active AIS Participation                         
                             * https://www.us-cert.gov/ais                    
                          2. Working Flare instance                           
                          3. Working MISP instance                            
                          4. Working CTI-Toolkit on MISP server               
                             * https://github.com/certau/cti-toolkit.git      
==============================================================================

------------------------------------------------------------------------------
                               =[BEGIN FLARE]=                                
------------------------------------------------------------------------------
[FLARE Script Installation]

1. git clone -b Automation --single-branch https://github.com/NoDataFound/oss.git oss-automation
2. Move scripts from oss-automation/scripts/flare to your flare server.
            * Example: mv oss-automation/scripts/flare/* /opt/Flare/scripts/
3. Install crontabs

[FLARE Crontab Installation]

1. From a terminal type: crontab -e
2. Copy and paste the below into your crontab
30 2 * * * /opt/Flare/scripts/CISCP.sh &>/opt/Flare/scripts/logs/CISCP_`date +\%y-\%m-\%d`.out
32 2 * * * /opt/Flare/scripts/AIS.sh &>/opt/Flare/scripts/logs/AIS_`date +\%y-\%m-\%d`.out
3. Save crontab
Note: Flare server is complete. Proceed to MISP


------------------------------------------------------------------------------
                               =[BEGIN MISP]=                                
------------------------------------------------------------------------------

[MISP Script Installation]

1. git clone -b Automation --single-branch https://github.com/NoDataFound/oss.git oss-automation
2. Move scripts from oss-automation/scripts/misp to your MISP server
            * Example: mv oss-automation/scripts/misp/* /home/misp/scripts/
3. Install crontabs

[MISP Crontab Installation]

1. From a terminal type: crontab -e
2. Copy and paste the below into your crontab
30 2 * * * /home/misp/scripts/MISP.sh
3. Save crontab
Note: MISP server is complete.

 ------------------------------DISCLAIMER-------------------------------------
 ANY DOWNLOAD AND USE OF THIS UNSUPPORTED SOFTWARE PROGRAM PRODUCT IS DONE AT
 THE USERS OWN RISK AND THE USER WILL BE SOLELY RESPONSIBLE FOR ANY DAMAGE TO
 – WITHOUT LIMITATION – ANY COMPUTER SYSTEM OR LOSS OF DATA THAT RESULTS FROM
 SUCH ACTIVITIES. SHOULD IT PROVE DEFECTIVE,     USER ASSUMES THE COST OF ALL
 NECESSARY SERVICING, REPAIR AND/OR CORRECTION.     IT IS THEREFORE UP TO THE
 USER TO TAKE ADEQUATE PRECAUTION AGAINST POSSIBLE DAMAGES     RESULTING FROM
 THIS UNSUPPORTED SOFTWARE.                                                   
 ------------------------------DISCLAIMER-------------------------------------
