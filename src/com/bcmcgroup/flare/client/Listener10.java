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

import com.bcmcgroup.taxii10.StatusMessage;
import com.sun.net.httpserver.*;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Properties;

/**
 * Listener class for receiving incoming TAXII 1.1 Messages automatically
 * @version 2.0.3
 */

class Listener10 {

    private static final Logger logger = Logger.getLogger(Listener10.class);

    public class MyHandler implements HttpHandler {
        public final DocumentBuilder db;
        private Properties config = new Properties();

        public MyHandler(Properties configParam) {
            config = configParam;
            db = ClientUtil.generateDocumentBuilder();
        }

        /**
         * Handles incoming HTTP requests
         *
         * @param exchange incoming HttpExchange object
         * @throws IOException if the connection is null
         *
         */
        public void handle(HttpExchange exchange) throws IOException {

            logger.debug("Received HTTPS POST...");

            // set response headers
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add("Accept", config.getProperty("httpHeaderAccept"));
            responseHeaders.add("X-TAXII-Content-Type", "urn:taxii.mitre.org:message:xml:1.0");
            responseHeaders.add("X-TAXII-Protocol", "urn:taxii.mitre.org:protocol:https:1.0");
            responseHeaders.add("X-TAXII-Services", "urn:taxii.mitre.org:services:1.0");
            StatusMessage sM;

            // reject if not POST method
            String requestMethod = exchange.getRequestMethod();
            OutputStream responseBody = exchange.getResponseBody();
            if (!requestMethod.equals("POST")) {
                responseHeaders.add("Allow", "POST");
                exchange.sendResponseHeaders(405, 0);
                String response = "Request method = " + requestMethod + ", only allow POST method!";
                responseBody.write(response.getBytes());
                responseBody.close();
                return;
            }

            // reject if the POST does not contain XML
            Headers requestHeaders = exchange.getRequestHeaders();
            String contentType = requestHeaders.getFirst("Content-Type");
            if (!contentType.startsWith("application/xml")) {
                exchange.sendResponseHeaders(406, 0);
                responseBody.write("POST request must be XML! Please set the Content-Type header to 'application/xml'".getBytes());
                responseBody.close();
                return;
            }

            // reject if request body isn't valid TAXII
            Document taxiiDoc;
            try {
                InputStream istream = exchange.getRequestBody();
                taxiiDoc = db.parse(istream);
                com.bcmcgroup.flare.client.TaxiiValidator tv = new TaxiiValidator();
                if (!tv.validate(taxiiDoc)) {
                    sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "TAXII XML failed to validate!  Please alter the message body to conform with the TAXII 1.0 schema.");
                    sM.sendResponse(responseBody, exchange);
                    return;
                } else if (!ClientUtil.validateStix(taxiiDoc)) {
                    sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "STIX XML content failed to validate!  Please alter the message contents to conform with the appropriate STIX schemas.");
                    sM.sendResponse(responseBody, exchange);
                    return;
                }
            } catch (SAXException e) {
                logger.error("SAX Exception when attempting to generate a TAXII document from an HTTPS request.");
                sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "SAX Exception when attempting to generate a TAXII document from an HTTPS request. Ensure that the request body is valid XML.");
                sM.sendResponse(responseBody, exchange);
                return;
            } catch (IllegalArgumentException e) {
                logger.error("Illegal Argument Exception when attempting to generate a TAXII document from an HTTPS request.");
                sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "IllegalArgumentException: " + "Illegal Argument Exception when attempting to generate a TAXII document from an HTTPS request. Ensure that the request is properly formed.");
                sM.sendResponse(responseBody, exchange);
                return;
            }

            // fetch message_id for in_response_to in response
            String inResponseTo = taxiiDoc.getDocumentElement().getAttribute("message_id");

            // otherwise, parse TAXII document and save content
            responseHeaders.add("Content-Type", config.getProperty("httpHeaderContentType"));
            Subscriber10 sub = new Subscriber10();
            sub.save(taxiiDoc, null);
            sM = new StatusMessage(null, inResponseTo, "SUCCESS", null, null);
            sM.sendResponse(responseBody, exchange);
        }
    }

    private static void usage() {
        logger.error("Usage: java Listener10 [-p port]");
        System.exit(-1);
    }

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
            if (config.getProperty("sslDebug").equals("true")) {
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

            server.createContext(config.getProperty("listenerEndpoint"), new com.bcmcgroup.flare.client.Listener10().new MyHandler(config));
            server.setExecutor(null);
            server.start();
            logger.info("TAXII 1.0 listener started, now waiting for incoming requests. Press CTRL-C to kill.");
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
