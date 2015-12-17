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

import com.bcmcgroup.flare.xmldsig.Xmldsig;
import com.bcmcgroup.taxii11.StatusMessage;
import com.bcmcgroup.taxii11.TaxiiUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.Headers;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 */
class Listener11 {

    private static final Logger logger = Logger.getLogger(Listener11.class);

    public class MyHandler implements HttpHandler {
        public final DocumentBuilder db;
        private Properties config = new Properties();

        public MyHandler(Properties configParam) {
            config = configParam;
            db = ClientUtil.generateDocumentBuilder();
        }

        /**
         * Handles incoming HTTP requests
         * @param t incoming HttpExchange object
         * @throws IOException if the connection is null
         *
         */
        public void handle(HttpExchange t) throws IOException {

            logger.debug("Received HTTPS POST...");
            String advanced = config.getProperty("advanced");

            // set response headers
            Headers responseHeaders = t.getResponseHeaders();
            responseHeaders.add("Accept", config.getProperty("httpHeaderAccept"));
            responseHeaders.add("X-TAXII-Content-Type", "urn:taxii.mitre.org:message:xml:1.1");
            responseHeaders.add("X-TAXII-Protocol", "urn:taxii.mitre.org:protocol:https:1.0");
            responseHeaders.add("X-TAXII-Services", "urn:taxii.mitre.org:services:1.1");
            StatusMessage sM;

            // reject if not POST method
            String requestMethod = t.getRequestMethod();
            OutputStream responseBody = t.getResponseBody();
            if (!requestMethod.equals("POST")) {
                responseHeaders.add("Allow", "POST");
                t.sendResponseHeaders(405, 0);
                String response = "Request method = " + requestMethod + ", only allow POST method!";
                responseBody.write(response.getBytes());
                responseBody.close();
                return;
            }

            // reject if the POST does not contain XML
            Headers requestHeaders = t.getRequestHeaders();
            String contentType = requestHeaders.getFirst("Content-Type");
            if (!contentType.startsWith("application/xml")) {
                t.sendResponseHeaders(406, 0);
                responseBody.write("POST request must be XML! Please set the Content-Type header to 'application/xml'".getBytes());
                responseBody.close();
                return;
            }

            // reject if request body isn't valid TAXII
            Document taxiiDoc;
            try {
                InputStream istream = t.getRequestBody();
                taxiiDoc = db.parse(istream);
                TaxiiValidator tv = new TaxiiValidator();
                if (!tv.validate(taxiiDoc)) {
                    sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "TAXII XML failed to validate!  Please alter the message body to conform with the TAXII 1.1 schema.");
                    sM.sendResponse(responseBody, t);
                    return;
                } else if (!ClientUtil.validateStix(taxiiDoc)) {
                    sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "STIX XML content failed to validate!  Please alter the message contents to conform with the appropriate STIX schemas.");
                    sM.sendResponse(responseBody, t);
                    return;
                }
            } catch (SAXException e) {
                logger.error("SAX Exception when attempting to generate a TAXII document from an HTTPS request.");
                sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "SAX Exception when attempting to generate a TAXII document from an HTTPS request. Ensure that the request body is valid XML.");
                sM.sendResponse(responseBody, t);
                return;
            } catch (IllegalArgumentException e) {
                logger.error("Illegal Argument Exception when attempting to generate a TAXII document from an HTTPS request.");
                sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "Illegal Argument Exception when attempting to generate a TAXII document from an HTTPS request. Ensure that the request is properly formed.");
                sM.sendResponse(responseBody, t);
                return;
            }

            // fetch message_id for in_response_to in response
            String inResponseTo = taxiiDoc.getDocumentElement().getAttribute("message_id");

            // reject if digital signature doesn't verify
            String verifyDS = config.getProperty("verifyDS");
            if ("true".equalsIgnoreCase(verifyDS)) {
                String ts = config.getProperty("pathToTrustStore");
                String tsPw = ClientUtil.decrypt(config.getProperty("trustStorePassword"));
                String vA = config.getProperty("verifyAlias");
                if (!Xmldsig.verifySignature(taxiiDoc, ts, tsPw, vA)) {
                    logger.error("TAXII server digital signature verification failed, discarding message.");
                    sM = new StatusMessage(null, inResponseTo, "BAD_MESSAGE", null, "TAXII server digital signature verification failed, discarding message.");
                    sM.sendResponse(responseBody, t);
                    return;
                }
            } else {
                logger.debug("verifyDS is configured as false or not configured, so no validation of digital signature ... ");
            }

            // otherwise, parse TAXII document and save content
            responseHeaders.add("Content-Type", config.getProperty("httpHeaderContentType"));
            Subscriber11 sub = new Subscriber11();

            if ("true".equalsIgnoreCase(advanced)) {
                // check to see if this is a Storefront Query Request
                NodeList nl = taxiiDoc.getElementsByTagNameNS(config.getProperty("taxii11NS"), "Query");
                Element query = null;
                if (nl.getLength() != 0) {
                    query = (Element) nl.item(0);
                }
                logger.debug("handle query: " + query);
                if (query != null) {
                    String resultId = UUID.randomUUID().toString();
                    sub.saveQuery(taxiiDoc, resultId);
                    HashMap<String, String> statusDetail = new HashMap<>();
                    statusDetail.put("ESTIMATED_WAIT", config.getProperty("queryEstimatedWait"));
                    statusDetail.put("RESULT_ID", resultId);
                    statusDetail.put("WILL_PUSH", "false");
                    sM = new StatusMessage(null, inResponseTo, "PENDING", statusDetail, null);
                    sM.sendResponse(responseBody, t);
                    return;
                }

                // check to see if this is a Storefront Discovery_Request
                if (taxiiDoc.getDocumentElement().getLocalName().toLowerCase().contains("discovery")) {
                    try {
                        Document responseDoc = TaxiiUtil.discoveryResponse(null, inResponseTo, null, TaxiiUtil.getServiceList());
                        TaxiiValidator tv = new TaxiiValidator();
                        if (!tv.validate(responseDoc)) {
                            sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "TAXII XML failed to validate! Please alter the message body to conform with the TAXII 1.1 schema.");
                            sM.sendResponse(responseBody, t);
                            return;
                        }
                        String responseString = ClientUtil.convertDocumentToString(responseDoc, true);
                        t.sendResponseHeaders(200, 0);
                        responseBody.write(responseString != null ? responseString.getBytes() : new byte[0]);
                        responseBody.close();
                        return;
                    } catch (IOException e) {
                        logger.error("IOException in discovery service when attempting to create a response and validate it.");
                    } catch (Exception e) {
                        logger.error("General exception in discovery service when attempting to create a response and validate it.");
                    }
                }

                // else check to see if this is a Storefront Poll_Fulfillment Request
                nl = taxiiDoc.getElementsByTagNameNS(config.getProperty("taxii11NS"), "Poll_Fulfillment");
                Element pollFulfillment = null;
                if (nl.getLength() != 0) {
                    pollFulfillment = (Element) nl.item(0);
                }
                if (pollFulfillment != null) {
                    String resultId = pollFulfillment.getAttribute("result_id");
                    logger.debug("handle pollFulfillment resultId: " + resultId);
                    File dir = new File(config.getProperty("queryResponsePath"));
                    logger.debug("handle pollFulfillment dir: " + dir);
                    String[] fileList = dir.list();
                    if (Arrays.asList(fileList).contains("response_" + resultId + ".xml")) {
                        try (InputStream inputStream = new FileInputStream(config.getProperty("queryResponsePath") + "response_" + resultId + ".xml")) {
                            InputSource inputSource = new InputSource(new InputStreamReader(inputStream));
                            SAXSource source = new SAXSource(inputSource);
                            StreamResult result = new StreamResult(responseBody);
                            TransformerFactory tf = TransformerFactory.newInstance();
                            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                            Transformer trans = tf.newTransformer();
                            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                            t.sendResponseHeaders(200, 0);
                            trans.transform(source, result);
                            responseBody.close();
                            inputStream.close();
                        } catch (TransformerConfigurationException e) {
                            logger.error("TransformerConfigurationException in poll fulfillment when attempting to transform an input source.");
                        } catch (TransformerException e) {
                            logger.error("TransformerException in poll fulfillment when attempting to transform an input source.");
                        } catch (IOException e) {
                            logger.error("IOException in poll fulfillment when attempting to transform an input source.");
                        }
                    } else { // file from FLARErelay not found, send error!
                        sM = new StatusMessage(null, inResponseTo, "FAILURE", null, "No response from DHS CIR!");
                        sM.sendResponse(responseBody, t);
                        return;
                    }
                    return;
                }
            }

            // Otherwise, save the TAXII document
            sub.save(taxiiDoc, null);
            sM = new StatusMessage(null, inResponseTo, "SUCCESS", null, null);
            sM.sendResponse(responseBody, t);
        }
    }

    private static void usage() {
        logger.error("Usage: java Listener11 [-p port]");
        System.exit(-1);
    }

    /**
	 * Runs the HTTPS Listener
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		int PORT = 8000;
		if (args.length != 0) {
			if ((args.length != 2) || (!args[0].equals("-p"))) {
				usage();
			}
			try {
				PORT = Integer.parseInt(args[1]);
				if (PORT < 0 || PORT > 65535) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException nfe) {
				logger.error("Port must be an integer 0-65535");
				System.exit(-1);
			}
		}

        InputStream trustStoreStream = null;
        InputStream subStoreStream = null;
        try {
            Properties config = ClientUtil.loadProperties();
            if (config.getProperty("sslDebug").equalsIgnoreCase("true")) {
                System.setProperty("javax.net.debug", "ssl,handshake");
            }

            // set KeyStore & TrustStore
            String passphrase = config.getProperty("subscriberKeyStorePassword");
            KeyStore ks = KeyStore.getInstance("JKS");
            subStoreStream = new FileInputStream(config.getProperty("pathToSubscriberKeyStore"));
            String decryptedPw = ClientUtil.decrypt(passphrase.trim());
            if (decryptedPw == null) {
                logger.error("Couldn't access keystore. Ensure that key-values in config.properties are set and correct.");
                return;
            }
            ks.load(subStoreStream, decryptedPw.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, decryptedPw.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ts = KeyStore.getInstance("JKS");
            trustStoreStream = new FileInputStream(config.getProperty("pathToTrustStore"));
            passphrase = config.getProperty("trustStorePassword");
            decryptedPw = ClientUtil.decrypt(passphrase.trim());
            ts.load(trustStoreStream, decryptedPw != null ? decryptedPw.toCharArray() : new char[0]);
            tmf.init(ts);

            // create SSL context
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // create and configure server
            final SSLEngine sslEngine = sslContext.createSSLEngine();
            HttpsServer server = HttpsServer.create(new InetSocketAddress(PORT), 0);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    SSLParameters sslparams = getSSLContext().getDefaultSSLParameters();
                    sslparams.setNeedClientAuth(true);
                    String[] cipherSuites = sslEngine.getEnabledCipherSuites();
                    sslparams.setCipherSuites(cipherSuites);
                    sslparams.setProtocols(sslEngine.getEnabledProtocols());
                    params.setSSLParameters(sslparams);
                }
            });

            server.createContext(config.getProperty("listenerEndpoint"), new Listener11().new MyHandler(config));
            server.setExecutor(null);
            server.start();
            logger.info("TAXII 1.1 listener started, now waiting for incoming requests. Press CTRL-C to kill.");
        } catch (javax.net.ssl.SSLHandshakeException e) {
            logger.error("SSLHandshakeException when try to run listener. Enable sslDebug for more information.");
        } catch (IOException e) {
            logger.error("IOException when attempting to run listener.");
        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException when attempting to run listener.");
        } catch (KeyStoreException e) {
            logger.error("KeyStoreException when attempting to run listener.");
        } catch (CertificateException e) {
            logger.error("CertificateException when attempting to run listener.");
        } catch (UnrecoverableKeyException e) {
            logger.error("UnrecoverableKeyException when attempting to run listener.");
        } catch (KeyManagementException e) {
            logger.error("KeyManagementException when attempting to run listener.");
        } catch (Exception e) {
            logger.error("General exception when attempting to run listener. Ensure that all configuration has been completed correctly.");
        } finally {
            try {
                if (trustStoreStream != null) {
                    trustStoreStream.close();
                }
            } catch (IOException e) {
                logger.error("IOException closing trustStoreStream.");
            }
            try {
                if (subStoreStream != null) {
                    subStoreStream.close();
                }
            } catch (IOException e) {
                logger.error("IOException closing subStoreStream.");
            }
        }
    }
}