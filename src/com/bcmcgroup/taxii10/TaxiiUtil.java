/*
© 2015-2020 BCMC. Funding for this work was provided by the United States Government 
under contract GS06F1165Z/HSHQDC14F00094. The United States Government may use, 
disclose, reproduce, prepare derivative works, distribute copies to the public, 
and perform publicly and display publicly, in any manner and for any purpose, 
and to have or permit others to do so.

Please be advised that this project uses other open source software and uses of 
these software or their components must follow their respective license.
*/

package com.bcmcgroup.taxii10;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import org.apache.logging.log4j.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.bcmcgroup.flare.client.ClientUtil;
import com.bcmcgroup.flare.client.Subscriber10;

public class TaxiiUtil {
	
	private static final Logger logger = LogManager.getLogger(Subscriber10.class);
	private static final Properties config = ClientUtil.loadProperties();
	private static final String taxiiNS = config.getProperty("taxii10NS");
	
	/**
	 * Builds a connection to TAXII server
	 * @param address String containing the address to build the connection to
	 * @return HttpsURLConnection object connected to TAXII server
	 */
	public static HttpsURLConnection buildConnection(String address) {
		try {
			URL url = new URL(address);
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-Agent", config.getProperty("httpHeaderUserAgent"));
			conn.setRequestProperty("Content-Type", config.getProperty("httpHeaderContentType"));
			conn.setRequestProperty("Accept", config.getProperty("httpHeaderAccept"));
			conn.setRequestProperty("X-TAXII-Accept", "urn:taxii.mitre.org:message:xml:1.0");
			conn.setRequestProperty("X-TAXII-Content-Type", "urn:taxii.mitre.org:message:xml:1.0");
			conn.setRequestProperty("X-TAXII-Protocol", "urn:taxii.mitre.org:protocol:https:1.0");
			conn.setRequestProperty("X-TAXII-Services", "urn:taxii.mitre.org:services:1.0");
			conn.setDoOutput(true);
			return conn;
		} catch (MalformedURLException ioe) {
			logger.error("MalformedURLException when attempting to build an HTTPS connection.");
		} catch (IOException ioe) {
			logger.error("IOException when attempting to build an HTTPS connection.");
		}
		return null;
	}
	
	/**
	 * Generate a message ID
	 * @return a String containing a random integer between 0 and 2,000,000,000
	 */
	private static String generateMsgId() {
		SecureRandom r = new SecureRandom();
		return Integer.toString(r.nextInt(2000000000));
	}
	
	/**
	 * Constructs a TAXII Inbox_Message XML message and returns the Document object
	 * @param msgId the message_id
	 * @param extHdrs a HashMap containing all extended headers (can be null)
	 * @param message text for the Message tag
	 * @param sourceSubscription a HashMap containing all Source_Subscription attributes and tags (feed_name, subscription_id, Inclusive_Begin_Timestamp, Inclusive_End_Timestamp)
	 * @param contentBlocks an ArrayList of ContentBlock objects
	 * @return a Document object with the given parameters. The document will represent a TAXII Inbox Message.
	 *
	 * Usage Example:
	 *   Document im = inboxMessage("12345", null, "Here is my message", null, contentBlocks);
	 */
	public static Document inboxMessage(String msgId, HashMap<String,String> extHdrs, String message, HashMap<String,String> sourceSubscription, List<ContentBlock> contentBlocks) {
		DocumentBuilder db = ClientUtil.generateDocumentBuilder();
		if (db == null) {
			logger.error("Failed to generate a document builder for an inbox message.");
			return null;
		}
		Document taxiiDoc = db.newDocument();

		// Inbox_Message (root element)
		Element taxiiRoot = taxiiDoc.createElementNS(taxiiNS, "Inbox_Message");
		taxiiDoc.appendChild(taxiiRoot);
		if (msgId == null) {
		    taxiiRoot.setAttribute("message_id", generateMsgId());
		} else {
			taxiiRoot.setAttribute("message_id", msgId);
		}
		
		// Extended_Headers
		if (extHdrs != null && !extHdrs.keySet().isEmpty()) {
			Element eHs = taxiiDoc.createElementNS(taxiiNS, "Extended_Headers");
			Element eH = taxiiDoc.createElementNS(taxiiNS, "Extended_Header");
			for (String name : extHdrs.keySet()) {
				eH.setAttribute("name", name);
				eH.appendChild(taxiiDoc.createTextNode(extHdrs.get(name)));
				eHs.appendChild(eH);
			}
			taxiiRoot.appendChild(eHs);
		}
		
		// Message
		if (message != null) {
			Element msg = taxiiDoc.createElementNS(taxiiNS, "Message");
			msg.appendChild(taxiiDoc.createTextNode(message));
			taxiiRoot.appendChild(msg);
		}
		
		// Source_Subscription
		if (sourceSubscription != null && !sourceSubscription.keySet().isEmpty()) {
			Element ss = taxiiDoc.createElementNS(taxiiNS, "Source_Subscription");
			Set<String> ssKeys = sourceSubscription.keySet();
			if (ssKeys.contains("feed_name")) {
				ss.setAttribute("feed_name", sourceSubscription.get("feed_name"));
			}
			if (ssKeys.contains("subscription_id")) {
				ss.setAttribute("subscription_id", sourceSubscription.get("subscription_id"));
			}
			if (ssKeys.contains("Inclusive_Begin_Timestamp")) {
				Element ibt = taxiiDoc.createElementNS(taxiiNS, "Inclusive_Begin_Timestamp");
				ibt.appendChild(taxiiDoc.createTextNode(sourceSubscription.get("Inclusive_Begin_Timestamp")));
				ss.appendChild(ibt);
			}
			Element iet = taxiiDoc.createElementNS(taxiiNS, "Inclusive_End_Timestamp");
			iet.appendChild(taxiiDoc.createTextNode(sourceSubscription.get("Inclusive_End_Timestamp")));
			ss.appendChild(iet);
			taxiiRoot.appendChild(ss);
		}
				
		// Content_Block
		if (contentBlocks != null) {
			for (ContentBlock cBlock : contentBlocks) {
				cBlock.appendToDocument(taxiiDoc);
			}
		}
		return taxiiDoc;
	}
	
	/**
	 * Constructs a TAXII Poll_Request XML message and returns the Document object
	 * @param msgId the message_id
	 * @param fN the feed name
	 * @param subId the subscription_id
	 * @param extHdrs a HashMap containing all extended headers (can be null)
	 * @param bTime the Exclusive_Begin_Timestamp value
	 * @param eTime the Inclusive_End_Timestamp value
	 * @param cBind set of acceptable Content Bindings for this request
	 * @return a Document object with the given parameters. The document will represent a TAXII poll request.
	 * 
	 * Usage Example:
	 *   Document pr = pollRequest("12345", "myFeed", "12345678-90ab-cdef-1234-567890abcdef", null, "2014-05-24T22:23:00.000000Z", null, null);
	 */
	public static Document pollRequest(String msgId, String fN, String subId, HashMap<String,String> extHdrs, String bTime, String eTime, Set<String> cBind) {
		DocumentBuilder db = ClientUtil.generateDocumentBuilder();
        if (db == null) {
            logger.error("Failed to generate a document builder for poll request.");
            return null;
        }
		Document taxiiDoc = db.newDocument();
		
		// Poll_Request (root element)
		Element taxiiRoot = taxiiDoc.createElementNS(taxiiNS, "Poll_Request");
		taxiiDoc.appendChild(taxiiRoot);
		if (msgId == null) {
		    taxiiRoot.setAttribute("message_id", generateMsgId());
		} else {
			taxiiRoot.setAttribute("message_id", msgId);
		}
		if (fN != null) {
			taxiiRoot.setAttribute("feed_name", fN);
		} else {
			logger.error("Poll Request feed name was null.");
			return null;
		}
		if (subId != null) {
			taxiiRoot.setAttribute("subscription_id", subId);
		}
		
		// Extended_Headers
		if (extHdrs != null && !extHdrs.keySet().isEmpty()) {
			Element eHs = taxiiDoc.createElementNS(taxiiNS, "Extended_Headers");
			Element eH = taxiiDoc.createElementNS(taxiiNS, "Extended_Header");
			for (String name : extHdrs.keySet()) {
				eH.setAttribute("name", name);
				eH.appendChild(taxiiDoc.createTextNode(extHdrs.get(name)));
				eHs.appendChild(eH);
			}
			taxiiRoot.appendChild(eHs);
		}
				
		// Exclusive_Begin_Timestamp
		if (bTime != null) {
			Element bT = taxiiDoc.createElementNS(taxiiNS, "Exclusive_Begin_Timestamp");
			bT.appendChild(taxiiDoc.createTextNode(bTime));
			taxiiRoot.appendChild(bT);
		}
				
		// Inclusive_End_Timestamp
		if (eTime != null) {
			Element eT = taxiiDoc.createElementNS(taxiiNS, "Inclusive_End_Timestamp");
			eT.appendChild(taxiiDoc.createTextNode(eTime));
			taxiiRoot.appendChild(eT);
		}
				
		// Content_Binding
		if (cBind != null) {
			Element cB;
			for (String aCBind : cBind) {
				cB = taxiiDoc.createElementNS(taxiiNS, "Content_Binding");
				cB.appendChild(taxiiDoc.createTextNode(aCBind));
				taxiiRoot.appendChild(cB);
			}
		}
		return taxiiDoc;
	}
}
