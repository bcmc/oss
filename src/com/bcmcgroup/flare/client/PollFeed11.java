package com.bcmcgroup.flare.client;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

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

/**
 * Poll class for receiving incoming TAXII 1.0 Messages manually
 * @version 2.0.3
 */
class PollFeed11 {

    private static final Logger logger = Logger.getLogger(PollFeed11.class);

    public static void main(String[] args) throws IOException {
        String collectionName = null, bTime = null, eTime = null;
        if (args.length == 1) {
            collectionName = args[0];
        } else if (args.length == 3 && (args[1].equals("-b") || args[1].equals("-e"))) {
            collectionName = args[0];
            if (args[1].equals("-b")) {
                bTime = args[2];
            } else {
                eTime = args[2];
            }
        } else if (args.length == 5 && ((args[1].equals("-b") && args[3].equals("-e")) || (args[1].equals("-e") || args[3].equals("-b")))) {
            collectionName = args[0];
            if (args[1].equals("-b")) {
                bTime = args[2];
                eTime = args[4];
            } else {
                bTime = args[4];
                eTime = args[2];
            }
        }

        Properties config = ClientUtil.loadProperties();
        String subId = config.getProperty(collectionName + "_subId");
        if (config.getProperty("sslDebug").equals("true")) {
            System.setProperty("javax.net.debug", "ssl,handshake");
        }

        try {
            Subscriber11.poll(null, collectionName, subId, null, bTime, eTime, null, null);
        } catch (Exception e) {
            logger.error("General exception when attempting to poll. Ensure that all configuration has been completed correctly.");
        }
    }
}