#!/bin/bash

#
# Creates a self-signed FAREclient keystore for testing
#

USAGE="Usage: $0 <keystore password>"
if [ "$#" -ne 1 ]; then
    echo $USAGE
    exit 1
fi

# create a certificate authority for self-signing your FLAREclient certificate
openssl genrsa -out CA.key 2048
openssl req -new -x509 -days 3650 -key CA.key -out CA.crt

# create FLAREclient certificate key pair
openssl genrsa -out FLAREclient.key 2048

# create certificate signing request for FLAREclient certificate
openssl req -new -key FLAREclient.key -out FLAREclient.csr

# self-sign the FLAREClient certificate
openssl x509 -req -days 3650 -CA CA.crt -CAkey CA.key -set_serial 01 -in FLAREclient.csr -out FLAREclient.crt

# convert FLAREclient certificate into a PKCS#12 file 
openssl pkcs12 -export -in FLAREclient.crt -inkey FLAREclient.key -out FLAREclient.p12 -name client -CAfile CA.crt -caname rootCA

# convert PCKS#12 file into Java Keystore so FLAREclient can use it
keytool -importkeystore -deststorepass $1 -destkeypass $1 -destkeystore FLAREclient.jks -srckeystore FLAREclient.p12 -srcstoretype PKCS12 -srcstorepass $1 -alias client
 
