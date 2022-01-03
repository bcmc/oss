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

import com.bcmcgroup.flare.xmldsig.Xmldsig;
import com.bcmcgroup.taxii10.ContentBlock;
import com.bcmcgroup.taxii10.TaxiiUtil;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.logging.log4j.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Outbox class for publishing TAXII 1.0 messages
 * @version 2.0.3
 */
class PublisherOutbox10 {

    private static final Logger logger = LogManager.getLogger(PublisherOutbox10.class);

    private static final Properties config = ClientUtil.loadProperties();
    private static final String pathToPublisherKeyStore;
    private static final String publisherKeyStorePassword;
    private static final String publisherKeyName;
    private static final String publisherKeyPassword;
    private static final String connectingToFLARE;
    private static final String taxii10serverUrlInbox;


    static {

        // Ensure that all necessary properties are set
        String sslDebug;
        if ((sslDebug = config.getProperty("sslDebug")) == null || sslDebug.isEmpty())
            logger.warn("sslDebug is not set, or is not being read properly.");
        else if (sslDebug.equals("true"))
            System.setProperty("javax.net.debug", "ssl,handshake");

        if ((pathToPublisherKeyStore = config.getProperty("pathToPublisherKeyStore")) == null || pathToPublisherKeyStore.isEmpty())
            logger.warn("pathToPublisherKeyStore is not set, or is not being read properly.");
        else
            System.setProperty("javax.net.ssl.keyStore", pathToPublisherKeyStore);

        if ((publisherKeyStorePassword = ClientUtil.decrypt(config.getProperty("publisherKeyStorePassword"))) == null || publisherKeyStorePassword.isEmpty())
            logger.warn("publisherKeyStorePassword is not set, is returning null, or is not being read properly.");
        else
            System.setProperty("javax.net.ssl.keyStorePassword", publisherKeyStorePassword);

        String pathToTrustStore;
        if ((pathToTrustStore = config.getProperty("pathToTrustStore")) == null || pathToTrustStore.isEmpty())
            logger.warn("pathToTrustStore is not set, or is not being read properly.");
        else
            System.setProperty("javax.net.ssl.trustStore", pathToTrustStore);

        if ((publisherKeyName = config.getProperty("publisherKeyName")) == null || publisherKeyName.isEmpty())
            logger.warn("publisherKeyName is not set, or is not being read properly.");

        if ((publisherKeyPassword = ClientUtil.decrypt(config.getProperty("publisherKeyPassword"))) == null || publisherKeyPassword.isEmpty())
            logger.warn("publisherKeyPassword is not set, or is not being read properly.");

        if ((connectingToFLARE = config.getProperty("connectingToFLARE")) == null || connectingToFLARE.isEmpty())
            logger.warn("connectingToFLARE is not set, or is not being read properly.");

        if ((taxii10serverUrlInbox = config.getProperty("taxii10serverUrlInbox")) == null || taxii10serverUrlInbox.isEmpty())
            logger.warn("taxii10serverUrlInbox is not set, or is not being read properly.");

    }

    /**
     * Establishes a directory watch service that will publish STIX XML files that are placed into it. This will not account for overwrites.
     *
     * @param dir Directory to watch for STIX content to be published.
     *
     */
    private PublisherOutbox10(final File dir) {

        logger.info("Outbox is awaiting files...");

        // The monitor will perform polling on the folder every 2 seconds
        final long pollingInterval = 2000;

        // Create an observer, monitor, and listener to watch for directory changes
        FileAlterationObserver observer = new FileAlterationObserver(dir);
        FileAlterationMonitor monitor = new FileAlterationMonitor(pollingInterval);
        FileAlterationListener listener = new FileAlterationListenerAdaptor() {

            private String feedName;

            // Is triggered when a file is created in the monitored folder. Works recursively, and ignores publishFeeds.
            @Override
            public void onFileCreate(File file) {

                List<ContentBlock> contentBlocks = new ArrayList<>();

                // Do nothing if dropping files into the publishFeeds directory
                if (file.getParentFile().getName().equals("publishFeeds")) {
                    return;
                }

                try {
                    this.feedName = file.getParentFile().getName();

                    String contentBinding = ClientUtil.getContentBinding(file);

                    // Form a content block and ensure it passes STIX validation
                    ContentBlock contentBlock = new ContentBlock(contentBinding, file, null, null, null);
                    contentBlocks.add(contentBlock);

                    int responseCode = publish(this.feedName, null, null, contentBlocks);
                    if (responseCode != 0) {
                        logger.info(file.getName() + " " + responseCode);
                    } else {
                        logger.warn("Error publishing " + file.getName() + " to collection " + this.feedName + ".");
                    }

                } catch (IOException e) {
                    logger.error("IOException when trying to publish " + file.getName());
                } catch (SAXException s) {
                    logger.error("SAXException when trying to publish " + file.getName());
                }

            }
        };

        // Start the directory watch service
        observer.addListener(listener);
        monitor.addObserver(observer);
        try {
            monitor.start();
            logger.info("Directory monitoring service has started and is awaiting files...");
            logger.debug("Monitor started...");
        } catch (Exception e) {
            logger.error("General exception thrown when trying to start the directory monitor...");
        }

    }

    /**
     * Publishes a STIX XML file via the TAXII protocol.
     *
     * @param feedName      the feed name to be published to.
     * @param extHdrs       extended headers that are added to the TAXII message. TAXII allows the specification of Extended Headers in all TAXII Messages. All Extended Headers are
     *                      defined by third parties outside the TAXII specifications. Extended Headers in TAXII are represented as
     *                      name-value pairs.
     * @param message       text for the message tag within the TAXII message.
     * @param contentBlocks the STIX content blocks to be appended to the TAXII document.
     * @return Returns true or false. True for a successful publish, and false for an unsuccessful publish.
     * @throws IOException  if it can't find the file to be published.
     * @throws SAXException if it can't find the schema file to validate the TAXII document.
     */
    private static int publish(String feedName, HashMap<String, String> extHdrs, String message, List<ContentBlock> contentBlocks) throws IOException, SAXException {

        Document taxiiDoc = TaxiiUtil.inboxMessage(null, extHdrs, message, null, contentBlocks);

        if (!ClientUtil.validateStix(taxiiDoc)) {
            logger.error("Inbox Message to be published did not validate.");
            return 0;
        }

        boolean signResult = Xmldsig.signInboxMessage(taxiiDoc, pathToPublisherKeyStore, publisherKeyStorePassword, publisherKeyName, publisherKeyPassword);
        if (!signResult) {
            logger.error("An attempt to digitally sign the document failed.");
            return 0;
        }

        TaxiiValidator validator = new TaxiiValidator();
        if (!validator.validate(taxiiDoc)) {
            return 0;
        }

        String payload = ClientUtil.convertDocumentToString(taxiiDoc, false);

        String address;
        if (connectingToFLARE.equals("true")) {
            address = taxii10serverUrlInbox + "/" + URLEncoder.encode(feedName, "UTF-8").replace("+", "%20");
        } else {
            address = taxii10serverUrlInbox;
        }

        logger.debug("Attempting HTTPS connection...");
        HttpsURLConnection conn = TaxiiUtil.buildConnection(address);
        return ClientUtil.sendPost(conn, payload);
    }

    private static void usage() {
        logger.error("Usage: PublisherOutbox takes 1 argument, the directory to be watched. Directory monitoring is automatically recursive.");
        System.exit(-1);
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            usage();
        }

        if (args[0].equals("-h") || args[0].equals("--help") || args[0].equals("?") || args[0].equals("help")) {
            usage();
        }

        File file = new File(args[0]);

        if (file.isDirectory() && file.exists()) {
            new PublisherOutbox10(file);
        }

    }
}
