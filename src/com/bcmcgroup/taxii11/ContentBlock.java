package com.bcmcgroup.taxii11;

/*
© 2015-2020 BCMC. Funding for this work was provided by the United States Government 
under contract GS06F1165Z/HSHQDC14F00094. The United States Government may use, 
disclose, reproduce, prepare derivative works, distribute copies to the public, 
and perform publicly and display publicly, in any manner and for any purpose, 
and to have or permit others to do so.

Please be advised that this project uses other open source software and uses of 
these software or their components must follow their respective license.
*/


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.bcmcgroup.flare.client.ClientUtil;

/**
 * @version		2.0.4
 */
public class ContentBlock {
	private static final Logger logger = Logger.getLogger(ContentBlock.class);

	private static final int DEFAULT_BUFFER_SIZE = 65536;  // TODO may want to mess with this number to see how it affects performance
	private final String content_binding;
	private String content;
	private String timestamp_label;
	private String padding;
    private static final Properties config = ClientUtil.loadProperties();
	private static final String taxiiNS = config.getProperty("taxii11NS");

	/**
	 * Constructor to be used for Content in a String
	 * @param cB String containing Content_Binding
	 * @param c String containing Content
	 * @param tsL String containing Timestamp_Label
	 * @param padd String containing Padding
	 * @param dsS ds:Signature Node object
	 */
	private ContentBlock(String cB, String c, String tsL, String padd, Node dsS) {
		content_binding = cB;
		content = c;
		timestamp_label = tsL;
		padding = padd;
    }

	/**
	 * Constructor to be used for Content in a File
	 * @param cB String containing Content_Binding
	 * @param c File containing Content
	 * @param tsL String containing Timestamp_Label
	 * @param padd String containing Padding
	 * @param dsS ds:Signature Node object
	 * @throws IOException if file not found
	 */
	public ContentBlock(String cB, File c, String tsL, String padd, Node dsS) throws IOException {
		content_binding = cB;
		if (c.exists()) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(c);
				content = readFileToString(fis);
			} catch (IOException e) {
				logger.error("IOException in ContentBlock11 constructor.");
				throw new IOException (e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException ioe) {
						logger.error("IOException in ContentBlock11 constructor when closing file input stream.");
					}
				}
			}
		} else {
			throw new IOException("File " + c + " does not exist");
		}
		timestamp_label = tsL;
		padding = padd;
    }

    /**
	 * Appends the calling ContentBlock11 object to the provided TAXII Document
	 * @param taxiiDoc TAXII Document object
	 * Usage Example:
	 * ContentBlock11 block = ...
	 * block.appendToDocument(taxiiDoc);
	 */
	public void appendToDocument(Document taxiiDoc) {
		try {
			if (this.content_binding == null) {
				return;
			} else if (this.content == null) {
				return;
			}
			Element cBlock = taxiiDoc.createElementNS(taxiiNS, "Content_Block");

			// Content_Binding
			Element cBinding = taxiiDoc.createElementNS(taxiiNS, "Content_Binding");
			cBinding.setAttribute("binding_id", this.content_binding);
			cBlock.appendChild(cBinding);

			// Content
			Element c = taxiiDoc.createElementNS(taxiiNS, "Content");
			DocumentBuilder db = ClientUtil.generateDocumentBuilder();
			Element element = db != null ? db.parse(new ByteArrayInputStream(this.content.getBytes())).getDocumentElement() : null;
			c.appendChild(taxiiDoc.importNode(element, true));
			cBlock.appendChild(c);

			// Timestamp_Label (optional)
			if (this.timestamp_label != null) {
				Element tsL = taxiiDoc.createElementNS(taxiiNS, "Timestamp_Label");
				tsL.appendChild(taxiiDoc.createTextNode(this.timestamp_label));
				cBlock.appendChild(tsL);
			}

			// Padding
			if (this.padding != null) {
				Element padd = taxiiDoc.createElementNS(taxiiNS, "Padding");
				padd.appendChild(taxiiDoc.createTextNode(this.padding));
				cBlock.appendChild(padd);
			}

			taxiiDoc.getDocumentElement().appendChild(cBlock);
		} catch (SAXException e) {
			logger.error("SAXException when attempting to append a content block to a document.");
		} catch (IOException e) {
			logger.error("IOException when attempting to append a content block to a document.");
		}
	}

	/**
	 * Converts a FileInputStream object into a String
	 *
	 * @param input FileInputStream object containing desired input
	 * @return String containing converted input, using UTF-8
	 */
	private static String readFileToString(FileInputStream input) {
		byte[] barray = null;
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			int n;
			while (-1 != (n = input.read(buffer))) {
				output.write(buffer, 0, n);
			}
			barray = output.toByteArray();
			output.flush();
			output.close();
			input.close();

		} catch (IOException io) {
			logger.error("IO Exception when attempting to read a file to a string (ContentBlock10).");
		}
		if (barray != null) {
			return new String(barray, StandardCharsets.UTF_8);
		} else {
			return null;
		}
	}
}
