package com.bcmcgroup.flare.client;

/*
© 2015-2020 BCMC. Funding for this work was provided by the United States Government under contract GS06F1165Z/HSHQDC14F00094. The United States Government may use, disclose, reproduce, prepare derivative works, distribute copies to the public, and perform publicly and display publicly, in any manner and for any purpose, and to have or permit others to do so.

Please be advised that this project uses other open source software and uses of these software or their components must follow their respective license.
*/

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mitre.stix.validator.SchemaError;
import org.mitre.stix.validator.StixValidator;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.security.*;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Utility class for a variety of client functions.
 * @version 2.0.3
 */

public class ClientUtil {

    private static final Logger logger = Logger.getLogger(ClientUtil.class);
    private static final char[] seeds = "enfldsgbnlsngdlksdsgm".toCharArray();
    private static final byte[] ivBytes = "0102030405060708".getBytes();
    private static final String salt = "a9v5n38s";
    private static final int iterations = 65536;
    private static final int keySize = 256;
    private static final ArrayList<String> stixVersions = new ArrayList<>(Arrays.asList("1.0", "1.0.1", "1.1", "1.1.1"));

    /**
     * Set the value of a property for a specific property name.
     *
     * @param property the property name with its value to be set in the config.properties
     * @param value    the value to be set in the config.properties
     *
     */

    public static void setProperty(String property, String value) {
        Properties properties = new Properties();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        File file = new File("config.properties");
        try {
            inputStream = new FileInputStream(file);
            properties.load(inputStream);
            properties.setProperty(property, value);
            outputStream = new FileOutputStream(file);
            properties.store(outputStream, "");
        } catch (IOException e) {
            logger.error("IOException when attempting to set a configuration property. ");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("IOException when attempting to set a configuration property. ");
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.error("IOException when attempting to set a configuration property. ");
                }
            }
        }
    }

    /**
     * Convert a Document into a String
     *
     * @param document           the Document to be converted to String
     * @param omitXmlDeclaration set to true if you'd like to omit the XML declaration, false otherwise
     * @return the String converted from a Document
     *
     */
    public static String convertDocumentToString(Document document, boolean omitXmlDeclaration) {
        try {
            StringWriter stringWriter = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            if (omitXmlDeclaration) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            } else {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            }
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (TransformerException e) {
            logger.error("Transformer Exception when attempting to convert a document to a string. ");
        }
        return null;
    }

    /**
     * Convert an XML String to a Document
     *
     * @param xmlString the xml string to be converted to Document
     * @return the document converted from the xml string
     *
     */
    public static Document convertStringToDocument(String xmlString) {
        try {
            DocumentBuilder db = com.bcmcgroup.flare.client.ClientUtil.generateDocumentBuilder();
            if (db != null) {
                return db.parse(new InputSource(new StringReader(xmlString)));
            } else {
                logger.debug("Document Builder is null when attempting to call convertStringToDocument.");
            }
        } catch (SAXException e) {
            logger.error("SAX Exception when attempting to convert a string to a document. ");
        } catch (IOException e) {
            logger.error("IOException when attempting to convert a string to a document. ");
        }
        return null;
    }

    /**
     * Decrypt a string value
     *
     * @param encryptedText the text String to be decrypted
     * @return the decrypted String
     *
     */
    public static String decrypt(String encryptedText) {
        try {
            byte[] saltBytes = salt.getBytes("UTF-8");
            byte[] encryptedTextBytes = Base64.decodeBase64(encryptedText);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec spec = new PBEKeySpec(seeds, saltBytes, iterations, keySize);
            SecretKey secretKey = factory.generateSecret(spec);
            SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

            // Decrypt the message, given derived key and initialization vector.
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));
            byte[] decryptedTextBytes = null;
            try {
                decryptedTextBytes = cipher.doFinal(encryptedTextBytes);
            } catch (IllegalBlockSizeException e) {
                logger.error("IllegalBlockSizeException when attempting to decrypt a string.");
            } catch (BadPaddingException e) {
                logger.error("BadPaddingException when attempting to decrypt a string.");
            }
            if (decryptedTextBytes != null) {
                return new String(decryptedTextBytes);
            }

        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException when attempting to decrypt a string. ");
        } catch (InvalidKeySpecException e) {
            logger.error("InvalidKeySpecException when attempting to decrypt a string. ");
        } catch (NoSuchPaddingException e) {
            logger.error("NoSuchPaddingException when attempting to decrypt a string. ");
        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException when attempting to decrypt a string. ");
        } catch (InvalidKeyException e) {
            logger.error("InvalidKeyException when attempting to decrypt a string. ");
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("InvalidAlgorithmParameterException when attempting to decrypt a string. ");
        }
        return null;
    }

    /**
     * Encrypt plain text using AES
     *
     * @param plainText the String text to be encrypted in AES
     * @return the encrypted text String
     *
     */
    public static String encrypt(String plainText) {
        try {
            byte[] saltBytes = salt.getBytes("UTF-8");
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec spec = new PBEKeySpec(seeds, saltBytes, iterations, keySize);
            SecretKey secretKey = factory.generateSecret(spec);
            SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(ivBytes));
            byte[] encryptedTextBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return new Base64().encodeAsString(encryptedTextBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException when attempting to encrypt a string. ");
        } catch (InvalidKeySpecException e) {
            logger.error("InvalidKeySpecException when attempting to encrypt a string. ");
        } catch (NoSuchPaddingException e) {
            logger.error("NoSuchPaddingException when attempting to encrypt a string. ");
        } catch (IllegalBlockSizeException e) {
            logger.error("IllegalBlockSizeException when attempting to encrypt a string. ");
        } catch (BadPaddingException e) {
            logger.error("BadPaddingException when attempting to encrypt a string. ");
        } catch (UnsupportedEncodingException e) {
            logger.error("UnsupportedEncodingException when attempting to encrypt a string. ");
        } catch (InvalidKeyException e) {
            logger.error("InvalidKeyException when attempting to encrypt a string. ");
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("InvalidAlgorithmParameterException when attempting to encrypt a string. ");
        }
        return null;
    }

    /**
     * Fetch private key from KeyStore
     *
     * @param keyStorePath a String containing the path to the KeyStore
     * @param keyStorePW   a String containing the KeyStore password
     * @param keyName      a String containing the alias of targeted certificate
     * @param keyPW        a String containing the key password
     * @return the PrivateKeyEntry object containing the targeted private key
     *
     */
    public static PrivateKeyEntry getKeyEntry(String keyStorePath, String keyStorePW, String keyName, String keyPW) {
        KeyStore ks;
        PrivateKeyEntry keyEntry = null;
        FileInputStream is = null;
        try {
            ks = KeyStore.getInstance("JKS");
            is = new FileInputStream(keyStorePath);
            ks.load(is, keyStorePW.toCharArray());
            keyEntry = (PrivateKeyEntry) ks.getEntry(keyName, new KeyStore.PasswordProtection(keyPW.toCharArray()));
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException when attempting to get a key entry in a keystore. " + e);
        } catch (IOException e) {
            logger.error("IOException when attempting to get a key entry in a keystore. " + e);
        } catch (KeyStoreException e) {
            logger.error("KeyStoreException when attempting to get a key entry in a keystore. " + e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException when attempting to get a key entry in a keystore. " + e);
        } catch (CertificateException e) {
            logger.error("CertificateException when attempting to get a key entry in a keystore. " + e);
        } catch (UnrecoverableEntryException e) {
            logger.error("UnrecoverableEntryException when attempting to get a key entry in a keystore. " + e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    logger.error("IOException when attempting to close an input stream. " + ioe);
                }
            }
        }
        return keyEntry;
    }

    /**
     * Constructs a DocumentBuilder object for XML documents
     *
     * @return DocumentBuilder object with the proper initializations
     */
    public static DocumentBuilder generateDocumentBuilder() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setIgnoringComments(true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.error("ParserConfigurationException when attempting to generate a document builder.");
        }
        return null;
    }

    /**
     * Fetch a public key (certificate) from PrivateKeyEntry object
     *
     * @param keyEntry a PrivateKeyEntry object containing key of interest
     * @return the PublicKey object containing the targeted public key
     *
     */
    public static PublicKey getPublicKey(PrivateKeyEntry keyEntry) {
        if (keyEntry != null) {
            X509Certificate cert = (X509Certificate) keyEntry.getCertificate();
            return cert.getPublicKey();
        }
        return null;
    }

    /**
     * Fetch a public key (certificate) from KeyStore
     *
     * @param keyStorePath a String containing the path to the KeyStore
     * @param keyStorePW   a String containing the KeyStore password
     * @param alias        a String containing the alias of targeted certificate
     * @return the PublicKey object containing the targeted public key
     *
     */
    public static PublicKey getPublicKeyByAlias(String keyStorePath, String keyStorePW, String alias) {
        KeyStore ks;
        FileInputStream is = null;
        try {
            ks = KeyStore.getInstance("JKS");
            is = new FileInputStream(keyStorePath);
            ks.load(is, keyStorePW.toCharArray());
            Certificate certificate = ks.getCertificate(alias);
            if (certificate != null) {
                return certificate.getPublicKey();
            }
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException when attempting to extract a public key by an alias in a keystore. " + e);
        } catch (IOException e) {
            logger.error("IOException when attempting to extract a public key by an alias in a keystore. " + e);
        } catch (KeyStoreException e) {
            logger.error("KeyStoreException when attempting to extract a public key by an alias in a keystore. " + e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("NoSuchAlgorithmException when attempting to extract a public key by an alias in a keystore. " + e);
        } catch (CertificateException e) {
            logger.error("CertificateException when attempting to extract a public key by an alias in a keystore. " + e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    logger.error("IOException when attempting to close an input stream. " + ioe);
                }
            }
        }
        return null;
    }

    /**
     * Obtain the STIX version from the Content_Binding urn
     *
     * @param node Content_Binding node
     * @return a String containing the STIX version
     * @throws DOMException DOM operations only raise exceptions in "exceptional" circumstances, i.e., when an operation
     * is impossible to perform (either for logical reasons, because data is lost, or because the implementation has become unstable).
     * <a href="https://docs.oracle.com/javase/7/docs/api/org/w3c/dom/DOMException.html">Reference to Oracle Documentation.</a>
     *
     */
    private static String getStixVersion(Node node) throws DOMException {
        try {
            String urnStr = "";
            if (node.hasAttributes()) {
                Attr attr = (Attr) node.getAttributes().getNamedItem("binding_id");
                if (attr != null) {
                    urnStr = attr.getValue();
                }
            }
            if (urnStr.isEmpty()) {
                urnStr = node.getTextContent();
            }
            if (urnStr != null && !urnStr.isEmpty()) {
                int lastIndex = urnStr.lastIndexOf(":");
                String version = "";
                if (lastIndex >= 0) {
                    version = urnStr.substring(lastIndex + 1);
                }
                return version;
            }
        } catch (DOMException e) {
            logger.debug("DOMException when attempting to parse binding id from a STIX node. ");
            throw e;
        }
        return "";
    }

    /**
     * Load config.properties into memory space
     *
     * @return Properties object containing config properties
     *
     */
    public static Properties loadProperties() {
        InputStream inputStream = null;
        Properties config = new Properties();
        try {
            inputStream = new FileInputStream("config.properties");
            config.load(inputStream);
        } catch (IOException e) {
            logger.error("IOException when attempting to load config properties. ");
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                logger.debug("IOException when attempting to close an input stream. ");
            }
        }
        return config;
    }

    /**
     * Helper function that removes whitespace nodes from Element objects to allow for easier parsing
     *
     * @param e the Element object
     *
     */
    public static void removeWhitespaceNodes(Element e) {
        NodeList children = e.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child instanceof Text && ((Text) child).getData().trim().length() == 0) {
                e.removeChild(child);
            } else if (child instanceof Element) {
                removeWhitespaceNodes((Element) child);
            }
        }
    }

    /**
     * Sends an HTTPS POST request and returns the response
     *
     * @param conn    the HTTPS connection object
     * @param payload the payload for the POST request
     * @return the response from the remote server
     *
     */
    public static int sendPost(HttpsURLConnection conn, String payload) {
        OutputStream outputStream = null;
        DataOutputStream wr = null;
        InputStream is = null;
        int response = 0;
        logger.debug("Attempting HTTPS POST...");
        try {
            outputStream = conn.getOutputStream();
            wr = new DataOutputStream(outputStream);
            wr.write(payload.getBytes("UTF-8"));
            wr.flush();
            is = conn.getInputStream();
            response = conn.getResponseCode();
        } catch (IOException e) {
            logger.debug("IOException when attempting to send a post message. ");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.debug("IOException when attempting to close an input stream. ");
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.debug("IOException when attempting to close an output stream. ");
                }
            }
            if (wr != null) {
                try {
                    wr.close();
                } catch (IOException e) {
                    logger.debug("IOException when attempting to close a data output stream. ");
                }
            }
        }
        logger.debug("HTTPS POST Response: " + response);
        return response;
    }

    /**
     * Sends an HTTPS POST request and returns the response in String format
     *
     * @param conn    the HTTPS connection object
     * @param payload the payload for the POST request
     * @return the response from the remote server in String format
     *
     */
    public static String sendPostGetStringResponse(HttpsURLConnection conn, String payload) {
        OutputStream outputStream = null;
        DataOutputStream wr = null;
        InputStream is = null;
        String response = "";
        logger.debug("Attempting HTTPS POST");
        try {
            outputStream = conn.getOutputStream();
            wr = new DataOutputStream(outputStream);
            wr.write(payload.getBytes("UTF-8"));
            wr.flush();
            is = conn.getInputStream();
            response = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            logger.debug("IOException when attempting to send a post message. ");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.debug("IOException when attempting to close an input stream. ");
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.debug("IOException when attempting to close an output stream. ");
                }
            }
            if (wr != null) {
                try {
                    wr.close();
                } catch (IOException e) {
                    logger.debug("IOException when attempting to close a data output stream. ");
                }
            }
        }
        return response;
    }

    /**
     * Validate each Content block in a TAXII document (against the STIX schema files)
     *
     * @param taxiiDoc The TAXII document containing the STIX to be validated
     * @return true if all STIX validates, false otherwise
     *
     */
    public static boolean validateStix(Document taxiiDoc) {
        try {
            NodeList bindings = taxiiDoc.getElementsByTagNameNS("*", "Content_Binding");
            NodeList contents = taxiiDoc.getElementsByTagNameNS("*", "Content");
            int numBindings = bindings.getLength();
            if (numBindings != contents.getLength()) {
                logger.warn("STIX Validation mismatch for number of Content_Bindings and Contents. Message failed");
                return false;
            }
            for (int i = 0; i < numBindings; i++) {
                Node binding = bindings.item(i);
                String stixVersion = getStixVersion(binding);
                logger.debug("STIX Version: " + stixVersion);
                Node stix = contents.item(i).getFirstChild();
                Source source = new DOMSource(stix);
                if (!stixVersion.isEmpty() && stixVersions.contains(stixVersion)) {
                    StixValidator sv = new StixValidator(stixVersion);
                    List<SchemaError> errors = sv.validate(source);
                    if (errors.size() > 0) {
                        logger.warn("STIX Validation Errors: " + errors.size());
                        for (SchemaError error : errors) {
                            logger.debug("SchemaError Category: " + error.getCategory());
                            logger.debug("SchemaError Message: " + error.getMessage());
                        }
                        logger.warn("Message failed due to STIX " + stixVersion + " content validation errors!  Please check content and try again.");
                        return false;
                    }
                } else {
                    throw new SAXException("Error: No STIX version number is specified by the Content_Binding urn.");
                }
            }
        } catch (SAXException e) {
            logger.debug("SAX Exception when attempting to validate STIX content. ");
            return false;
        } catch (IOException e) {
            logger.debug("IOException when attempting to validate STIX content. ");
            return false;
        }
        logger.debug("All STIX has been validated successfully.");
        return true;
    }

    public static String getContentBinding(File file) {
        String stix = "";
        try {
            stix = FileUtils.readFileToString(file);
        } catch (IOException e) {
            logger.error("Couldn't get content bindings from file. Ensure the file exists.");
        }

        Document stixDoc = convertStringToDocument(stix);

        Element documentElement = null;
        if (stixDoc != null) {
            documentElement = stixDoc.getDocumentElement();
        } else {
            logger.error("Couldn't convert string to a document when getting content bindings...");
        }

        String version = "";
        if (documentElement != null) {
            version = documentElement.getAttribute("version");
        } else {
            logger.error("Couldn't get document element when getting content bindings...");
        }

        switch (version) {
            case "1.1.1":
                return "urn:stix.mitre.org:xml:1.1.1";
            case "1.1":
                return "urn:stix.mitre.org:xml:1.1";
            case "1.0.1":
                return "urn:stix.mitre.org:xml:1.0.1";
            default:
                return "urn:stix.mitre.org:xml:1.0";
        }
    }
}
	    		
