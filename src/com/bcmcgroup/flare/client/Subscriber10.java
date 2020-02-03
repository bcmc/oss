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
import com.bcmcgroup.taxii10.TaxiiUtil;
import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

/**
 * Subscriber class for receiving TAXII 1.0 messages
 * @version 2.0.3
 */
public class Subscriber10 {
    private static final Logger logger = Logger.getLogger(Subscriber10.class);

    private static final Properties config = ClientUtil.loadProperties();
    private static final String pathToTrustStore;
    private static final String trustStorePassword;
    private static String basePath;
    private static final String taxii10serverUrlPoll;
    private static final String connectingToFlare;
    private static final String verifyDS;
    private static final String verifyAlias;

    static {

        // Ensure that all necessary properties are set
        String sslDebug;
        if ((sslDebug = config.getProperty("sslDebug")) == null || sslDebug.isEmpty())
            logger.warn("sslDebug is not set, or is not being read properly.");
        else if (sslDebug.equals("true"))
            System.setProperty("javax.net.debug", "ssl,handshake");

        String pathToSubscriberKeystore;
        if ((pathToSubscriberKeystore = config.getProperty("pathToSubscriberKeyStore")) == null || pathToSubscriberKeystore.isEmpty())
            logger.warn("pathToSubscriberKeystore is not set, or is not being read properly.");
        else
            System.setProperty("javax.net.ssl.keyStore", pathToSubscriberKeystore);

        String subscriberKeyStorePassword;
        if ((subscriberKeyStorePassword = ClientUtil.decrypt(config.getProperty("subscriberKeyStorePassword"))) == null || subscriberKeyStorePassword.isEmpty())
            logger.warn("subscriberKeyStorePassword is not set, is returning null, or is not being read properly.");
        else
            System.setProperty("javax.net.ssl.keyStorePassword", subscriberKeyStorePassword);

        if ((pathToTrustStore = config.getProperty("pathToTrustStore")) == null || pathToTrustStore.isEmpty())
            logger.warn("pathToTrustStore is not set, or is not being read properly.");
        else
            System.setProperty("javax.net.ssl.trustStore", pathToTrustStore);

        if ((trustStorePassword =  ClientUtil.decrypt(config.getProperty("trustStorePassword"))) == null || trustStorePassword.isEmpty())
            logger.warn("trustStorePassword is not set, or is not being read properly.");

        if ((basePath = config.getProperty("basePath")) == null || basePath.isEmpty())
            logger.warn("basePath is not set, or is not being read properly.");

        if (!(basePath.endsWith("/")))
            basePath = basePath + "/";

        if ((taxii10serverUrlPoll = config.getProperty("taxii10serverUrlPoll")) == null || taxii10serverUrlPoll.isEmpty())
            logger.warn("taxii10serverUrlPoll is not set, or is not being read properly.");

        if ((connectingToFlare = config.getProperty("connectingToFLARE")) == null || connectingToFlare.isEmpty())
            logger.warn("connectingToFLARE is not set, or is not being read properly.");

        if ((verifyDS = config.getProperty("verifyDS")) == null || verifyDS.isEmpty())
            logger.debug("verifyDS is not set, or is not being read properly.");

        if ((verifyAlias = config.getProperty("verifyAlias")) == null || verifyAlias.isEmpty())
            logger.debug("verifyAlias is not set, or is not being read properly.");
    }

    /**
     * Returns a String containing a TAXII XML response - the results from the poll, also saves the poll results locally
     *
     * @param messageId    the message_id
     * @param feedName       the feed_name
     * @param subscriptionId    the subscription_id
     * @param extendedHeaders  a HashMap containing all extended headers (can be null)
     * @param ebt      the Exclusive_Begin_Timestamp parameter
     * @param iet      the Inclusive_End_Timestamp parameter
     * @param cBind    a set of content bindings that the subscriber will accept
     * @param fileName the base file name to be used for saving each content block from the poll response.
     */
    public static void poll(String messageId, String feedName, String subscriptionId, HashMap<String, String> extendedHeaders, String ebt, String iet, Set<String> cBind, String fileName) {

        // Debug Prints
        logger.debug("Poll Feed Name: " + feedName);
        logger.debug("Poll Subscription ID: " + subscriptionId);
        logger.debug("Poll Exclusive Begin Time: " + ebt);
        logger.debug("Poll Inclusive End Time: " + iet);

        // Build TAXII Doc
        Document taxiiDoc = TaxiiUtil.pollRequest(messageId, feedName, subscriptionId, extendedHeaders, ebt, iet, cBind);

        // Build request string
        String requestString = ClientUtil.convertDocumentToString(taxiiDoc, true);
        String address = null;
        if ("true".equalsIgnoreCase(connectingToFlare)) {
            try {
                address = taxii10serverUrlPoll + "/" + URLEncoder.encode(feedName, "UTF-8").replace("+", "%20");
            } catch (UnsupportedEncodingException e) {
                logger.debug("Unsupported encoding exception when trying to encode the URL for TAXII 1.0 poll URL");
            }
        } else {
            address = taxii10serverUrlPoll;
        }

        // Build HTTPS connection
        HttpsURLConnection conn = TaxiiUtil.buildConnection(address);
        if (conn == null) {
            logger.error("HTTPS Connection is null. Poll will be unsuccessful. Ensure that taxii11serverUrlPoll and other necessary configuration fields are correct.");
        }

        // Build response string
        String responseString = ClientUtil.sendPostGetStringResponse(conn, requestString);
        if (responseString == null) {
            logger.error("Error obtaining a response from HTTPS POST.");
            return;
        }

        // Convert response string to document.
        Document responseDoc = ClientUtil.convertStringToDocument(responseString);
        if (responseDoc == null) {
            logger.debug("Error converting response to document.");
            return;
        }

        // Digital signature validation
        Boolean verifyStatus = false;
        if ("true".equalsIgnoreCase(verifyDS)) {
            verifyStatus = Xmldsig.verifySignature(responseDoc, pathToTrustStore, trustStorePassword, verifyAlias);
            logger.debug("Digital Signature Validation: " + verifyStatus);
            if (!verifyStatus) {
                return;
            }
        } else {
            logger.debug("Digital Signature Validation skipped...");
        }


        // TAXII Validation on the response.
        TaxiiValidator taxiiValidator;
        try {
            taxiiValidator = new TaxiiValidator();
            if (!taxiiValidator.validate(responseDoc)) {
                logger.error("TAXII Validation Failure: Response is not valid TAXII.");
                return;
            }
        } catch (SAXException e) {
            logger.debug("SAX Exception during TAXII Validation.");
            return;
        } catch (IOException e) {
            logger.debug("IO Exception during TAXII Validation:");
            return;
        }

        // Fetch STIX and save
        String savePath = basePath + "subscribeFeeds/" + feedName + "/";
        Element responseTaxiiRoot = responseDoc.getDocumentElement();
        String responseIBT = getTimestampFromElement("Inclusive_Begin_Timestamp", responseTaxiiRoot);
        String responseIET = getTimestampFromElement("Inclusive_End_Timestamp", responseTaxiiRoot);
        String responseFN = responseTaxiiRoot.getAttribute("feed_name");
        String responseSubId = responseTaxiiRoot.getAttribute("subscription_id");
        logger.debug("Poll Response Saved: " + savePath);
        logger.debug("Poll Response Feed Name: " + responseFN);
        logger.debug("Poll Response Subscription ID: " + responseSubId);
        logger.debug("Poll Response Inclusive Begin: " + responseIBT);
        logger.debug("Poll Response Inclusive End: " + responseIET);
        if (responseTaxiiRoot.getElementsByTagNameNS("*", "Content_Block").getLength() == 0) {
            logger.info("Poll Response on feed " + responseFN + " contained no content. Collection may be empty.");
        }
        fetchStixAndSave("poll", responseTaxiiRoot, savePath, fileName);
    }


    /**
     * Helper function to loop through the content blocks (fetch the STIX content) and save each xml document locally
     *
     * @param filePrefix typically "push" or "poll" to declare how this document was obtained
     * @param root       the root xml element
     * @param savePath   the directory path to save the xml file
     * @param fileName   the file name to save the xml file as [defaults to current time stamp]
     */
    private static void fetchStixAndSave(String filePrefix, Element root, String savePath, String fileName) {
        try {
            Date date = new Date();
            NodeList contentBlocks = root.getElementsByTagNameNS("*", "Content_Block");
            Element contentBlock, content;
            Document contentDoc = null;
            DocumentBuilder db = ClientUtil.generateDocumentBuilder();
            for (int i = 0; i < contentBlocks.getLength(); i++) {
                // capture STIX document
                contentBlock = (Element) contentBlocks.item(i);
                content = (Element) contentBlock.getElementsByTagNameNS("*", "Content").item(0);
                ClientUtil.removeWhitespaceNodes(content);
                content = (Element) content.getFirstChild();
                if (db != null) {
                    contentDoc = db.newDocument();
                    Node contentImport = contentDoc.importNode(content, true);
                    contentDoc.appendChild(contentImport);
                } else {
                    logger.debug("Document Builder was null when attempting to create the content document.");
                }

                // save the STIX document
                TransformerFactory tf = TransformerFactory.newInstance();
                tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                Transformer t = tf.newTransformer();
                t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                if (fileName == null) {
                    fileName = String.valueOf(date.getTime());
                }
                t.transform(new DOMSource(contentDoc), new StreamResult(new File(savePath + filePrefix + "_" + fileName + "_" + i + ".xml")));
                logger.info("Received content. Saved to " + (savePath + filePrefix + "_" + fileName + "_" + i + ".xml"));
            }
        } catch (TransformerConfigurationException e) {
            logger.error("TransformerConfigurationException when attempting to parse and save STIX content.");
        } catch (TransformerFactoryConfigurationError e) {
            logger.error("TransformerFactoryConfigurationError when attempting to parse and save STIX content.");
        } catch (TransformerException e) {
            logger.error("TransformerException when attempting to parse and save STIX content.");
        }
    }

    /**
     * Helper function to return a String containing the particular requested time stamp value from the supplied document
     *
     * @param tsType   String containing the precise tag name for this time stamp
     * @param element  the root xml element
     * @return String containing the requested time stamp
     */
    private static String getTimestampFromElement(String tsType, Element element) {
        Node node;
        String ts = "";
        NodeList nlist = element.getElementsByTagNameNS("*", tsType);
        if (nlist != null && nlist.getLength() > 0) {
            node = nlist.item(0);
            if (node != null) {
                ts = node.getNodeValue();
                if (ts == null) {
                    node = node.getFirstChild();
                    ts = node.getNodeValue();
                }
            }
        }
        return ts;
    }

    /**
     * Strips off the TAXII portion from the supplied TAXII Inbox_Message Document
     * and then saves the content as an XML File to the appropriate feed destination.
     *
     * @param taxiiDoc a TAXII 1.0 compliant Document object
     * @param fileName file name to save as [default is timestamp]
     *
     */
    public void save(Document taxiiDoc, String fileName) {
        Properties config = ClientUtil.loadProperties();

        // reject if digital signature doesn't verify
        if ("true".equalsIgnoreCase(verifyDS)) {
            String ts = config.getProperty("pathToTrustStore");
            String tsPw = ClientUtil.decrypt(config.getProperty("trustStorePassword"));
            String vA = config.getProperty("verifyAlias");
            if (!Xmldsig.verifySignature(taxiiDoc, ts, tsPw, vA)) {
                logger.error("Digital signature validation failed.");
                return;
            }
        } else {
            logger.debug("Digital Signature validation is configured as false.");
        }

        Element taxiiRoot = taxiiDoc.getDocumentElement();
        String ibt;
        String iet;
        Element srcSub = (Element) taxiiRoot.getElementsByTagNameNS("*", "Source_Subscription").item(0);
        String savePath = config.getProperty("basePath") + "subscribeFeeds/";
        String subId;
        String feedName;
        if (srcSub != null) {
            feedName = srcSub.getAttribute("feed_name");
            if (feedName.equals("")) {
                logger.error("No feed name was found in the response. You may be mismatching TAXII Protocol Versions.");
                return;
            }
            savePath = savePath + feedName + "/";
            subId = srcSub.getAttribute("subscription_id");
            try {
                ibt = getTimestampFromElement("Inclusive_Begin_Timestamp", srcSub);
                iet = getTimestampFromElement("Inclusive_End_Timestamp", srcSub);
                logger.debug("Push Save Feed Name: " + feedName);
                logger.debug("Push Save Path: " + savePath + fileName);
                logger.debug("Push Save Inclusive Begin: " + ibt);
                logger.debug("Push Save Inclusive End: " + iet);
                fetchStixAndSave("push", taxiiRoot, savePath, fileName);
            } catch (DOMException e) {
                logger.error("DOMException when attempting to save a document.");
            }
        }
    }

}
