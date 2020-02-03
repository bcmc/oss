package com.bcmcgroup.flare.client;

/*
© 2015-2020 BCMC. Funding for this work was provided by the United States Government 
under contract GS06F1165Z/HSHQDC14F00094. The United States Government may use, 
disclose, reproduce, prepare derivative works, distribute copies to the public, 
and perform publicly and display publicly, in any manner and for any purpose, 
and to have or permit others to do so.

Please be advised that this project uses other open source software and uses of 
these software or their components must follow their respective license.
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
