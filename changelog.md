# FLAREclient Version 2.0.5 
## January 3rd, 2022 - Release Changelog

###### Security

The logging framework was upgraded from log4j to log4j2 (v2.17.1) to address a critical vulnerability.
As part of the log4j update a new log4j2.properties file is used for setting log configuration properties. 


###### Dependency Management
This project was updated to be a maven project. 
The pom.xml file manages most of the dependencies and includes them in a jar with dependencies when the project is built with:
```mvn package```

Dependency jars which cannot be found in the maven central repository are included in the lib folder.


