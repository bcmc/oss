package com.bcmcgroup.taxii10;

/*
© 2015-2020 BCMC. Funding for this work was provided by the United States Government 
under contract GS06F1165Z/HSHQDC14F00094. The United States Government may use, 
disclose, reproduce, prepare derivative works, distribute copies to the public, 
and perform publicly and display publicly, in any manner and for any purpose, 
and to have or permit others to do so.

Please be advised that this project uses other open source software and uses of 
these software or their components must follow their respective license.
*/

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.bcmcgroup.flare.client.ClientUtil;
import com.sun.net.httpserver.HttpExchange;

/**
 * @version		2.0.4
 */
public class StatusMessage {
	private static final Logger logger = Logger.getLogger(StatusMessage.class);
	private final String msgId;
	private final String inResponseTo;
	private final String statusType;
	private final String statusDetail;
	private final String message;
	private static final Properties config = ClientUtil.loadProperties();
	private static final String taxiiNS = config.getProperty("taxii10NS");

	/**
	 * Constructor for a StatusMessage
	 * @param mI String containing the attribute "message_id"
	 * @param iRT String containing the attribute "in_response_to"
	 * @param sT String containing the attribute "status_type"
	 * @param sD String containing the Element "Status_Detail"
	 * @param m String containing the Element "Message"
	 */
	public StatusMessage(String mI, String iRT, String sT, String sD, String m) {
		if (mI != null) {
			msgId = mI;
		} else {
			msgId = generateMsgId();
			// msgId = UUID.randomUUID().toString(); // we will use this once TAXII 1.1 releases
		}
		inResponseTo = iRT;
		statusType = sT;
		statusDetail = sD;
		message = m;
	}

	/**
	 * Constructs and returns the Document containing the response
	 *
	 * @return A Document object containing the TAXII Status_Message
	 */
	private Document getDocument() {
		DocumentBuilder db = ClientUtil.generateDocumentBuilder();
		if (db == null) {
			logger.error("Failed to generate a document builder for TAXII 1.0 status message.");
			return null;
		}
		Document responseDoc = db.newDocument();
		Element responseRoot = responseDoc.createElementNS(taxiiNS, "Status_Message");
		responseRoot.setAttribute("message_id", msgId);
		responseRoot.setAttribute("in_response_to", inResponseTo);
		responseRoot.setAttribute("status_type", statusType);
		if (statusDetail != null && statusType != null && ( "RETRY".equalsIgnoreCase(statusType) || statusType.startsWith("UNSUPPORTED"))) {
			Element sDetail = responseDoc.createElementNS(taxiiNS, "Status_Detail");
			sDetail.appendChild(responseDoc.createTextNode(statusDetail));
			responseRoot.appendChild(sDetail);
		}
		if (message != null) {
			Element msg = responseDoc.createElementNS(taxiiNS, "Message");
			msg.appendChild(responseDoc.createTextNode(message));
			responseRoot.appendChild(msg);
		}
		responseDoc.appendChild(responseRoot);
		return responseDoc;
	}

	/**
	 * Sends the Status_Message HTTPS response
	 *
	 * @param responseBody the OutputStream object containing the response body
	 * @param exchange the HttpExchange object required to send the response
	 */
    public void sendResponse(OutputStream responseBody, HttpExchange exchange) {
    	try {
			DOMSource source = new DOMSource(this.getDocument().getDocumentElement());
			StreamResult result = new StreamResult(responseBody);
			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Transformer t = tf.newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			exchange.sendResponseHeaders(200, 0);
			t.transform(source, result);
			responseBody.close();
	    } catch (TransformerConfigurationException e) {
			logger.error("TransformerConfigurationException when attempting to send a status message response.");
		} catch (TransformerException e) {
			logger.error("TransformerException when attempting to send a status message response.");
		} catch (IOException e) {
			logger.error("IOException  when attempting to send a status message response.");
		}
    }

	/**
	 * Generate a message ID (note this won't guarantee any uniqueness).
	 * We expect to convert to UUID when TAXII 1.1 is released.
	 *
	 * @return a string containing a random integer between 0 and 2,000,000,000
	 */
	private String generateMsgId() {
		SecureRandom r = new SecureRandom();
		return Integer.toString(r.nextInt(2000000000));
	}
}
