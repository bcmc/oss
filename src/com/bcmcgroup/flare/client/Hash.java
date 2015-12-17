package com.bcmcgroup.flare.client;

/*
Copyright 2014 BCMC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import org.apache.log4j.Logger;

/**
 * Utility class for encrypting strings to be used in particular key-value pairs in config properties
 * @version 2.0.3
 */

class Hash {

    private static final Logger logger = Logger.getLogger(Hash.class);

    /**
     * the main method to run for encryption of plaintext, the result value will be
     * saved into config.property file for the value field of the passed in property name
     *
     * @param args property name, string value to be encrypted
     */
    public static void main(String[] args) {
        String input = null;
        try {
            if (args.length == 2) {
                input = args[1];
            } else {
                logger.error("Usage: java Hash <property name in config.properties file> <String to be encrypted> ... Beware of '!', '@', and other bash interpreted characters. Escape them with \\");
                System.exit(-1);
            }
            String propertyName = args[0];
            String output = com.bcmcgroup.flare.client.ClientUtil.encrypt(input);
            ClientUtil.setProperty(propertyName, output);
        } catch (Exception e) {
            logger.error("A general exception occurred when attempting to set a property in the configuration file.");
        }
    }
}