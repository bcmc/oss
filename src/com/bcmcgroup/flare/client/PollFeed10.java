package com.bcmcgroup.flare.client;

import org.apache.logging.log4j.*;

import java.io.IOException;
import java.util.Properties;

/*
© 2015-2020 BCMC. Funding for this work was provided by the United States Government 
under contract GS06F1165Z/HSHQDC14F00094. The United States Government may use, 
disclose, reproduce, prepare derivative works, distribute copies to the public, 
and perform publicly and display publicly, in any manner and for any purpose, 
and to have or permit others to do so.

Please be advised that this project uses other open source software and uses of 
these software or their components must follow their respective license.
*/

/**
 * Poll class for receiving incoming TAXII 1.1 Messages manually
 * @version 2.0.3
 */
class PollFeed10 {

    private static final Logger logger = LogManager.getLogger(PollFeed10.class);

    public static void main(String[] args) throws IOException {
        String feedName = null, bTime = null, eTime = null;
        if (args.length == 1) {
            feedName = args[0];
        } else if (args.length == 3 && (args[1].equals("-b") || args[1].equals("-e"))) {
            feedName = args[0];
            if (args[1].equals("-b")) {
                bTime = args[2];
            } else {
                eTime = args[2];
            }
        } else if (args.length == 5 && ((args[1].equals("-b") && args[3].equals("-e")) || (args[1].equals("-e") || args[3].equals("-b")))) {
            feedName = args[0];
            if (args[1].equals("-b")) {
                bTime = args[2];
                eTime = args[4];
            } else {
                bTime = args[4];
                eTime = args[2];
            }
        }

        Properties config = ClientUtil.loadProperties();
        String subId = config.getProperty(feedName + "_subId");
        if (config.getProperty("sslDebug").equals("true")) {
            System.setProperty("javax.net.debug", "ssl,handshake");
        }

        try {
            Subscriber10.poll(null, feedName, subId, null, bTime, eTime, null, null);
        } catch (Exception e) {
            logger.error("General exception when attempting to poll. Ensure that all configuration has been completed correctly.");
        }
    }
}
