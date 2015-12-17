# FLAREclient Version 2.0.4 
## December 14th, 2015 - Release Changelog

##### Bug Fixes

- A bug in which XML was parsed without a namespace restriction was fixed to allow for wildcard namespaces. This was a problem when attempting to parse and save STIX content blocks from TAXII documents. XML with a namespace prefix in the Element tag would not be parsed, and thus would not be saved.

- An unintended extraneous layer of validation was occurring prior to saving documents in listening mode. The 'listener' HTTP handler validates both TAXII and STIX immediately upon reception.

###### Clarity

Various improvements were made for code clarity and readability. Scripts used to run the client were appended with '.sh' to make it more obvious that they are bash scripts. 

###### Efficiency 

Superfluous logic was purged, and an overall improvement to code efficiency was made. Dead or unused code was removed.

###### Security

Methods were changed from non-static to static wherever possible, in order to improve efficiency and security. Many fields and methods that were unnecessarily public were made private, and many classes were made package-local. References to passwords were removed from method names and scripts.


