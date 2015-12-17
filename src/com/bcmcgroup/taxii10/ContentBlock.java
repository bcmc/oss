package com.bcmcgroup.taxii10;

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
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 */
public class ContentBlock {
	private static final Logger logger = Logger.getLogger(ContentBlock.class);

	private static final int DEFAULT_BUFFER_SIZE = 65536;  // TODO may want to mess with this number to see how it affects performance
	private final String content_binding;
	private String content;
	private String timestamp_label;
	private String padding;
	private Node ds_signature;
	private static final Properties config = ClientUtil.loadProperties();
	private static final String taxiiNS = config.getProperty("taxii10NS");

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
		ds_signature = dsS;
	}

	/**
	 * Constructor to be used for Content in a File
	 * @param contentBinding String containing Content_Binding
	 * @param file File containing Content
	 * @param timestampLabel String containing Timestamp_Label
	 * @param padding String containing Padding
	 * @param digitalSignature ds:Signature Node object
	 */
	public ContentBlock(String contentBinding, File file, String timestampLabel, String padding, Node digitalSignature) throws IOException {
		content_binding = contentBinding;
		if (file.exists()) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
			    content = readFileToString(fis);
			} catch (IOException e) {
				logger.error("IOException in ContentBlock10 constructor.");
				throw new IOException (e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException ioe) {
						logger.error("IOException in ContentBlock10 constructor when closing file input stream.");
					}
				}
			}
		} else {
			throw new IOException("File " + file + " does not exist");
		}
		timestamp_label = timestampLabel;
		this.padding = padding;
		ds_signature = digitalSignature;
	}

	/**
	 * Appends the calling ContentBlock10 object to the provided TAXII Document
	 *
	 * @param taxiiDoc TAXII Document object
	 *
	 * Usage Example:
	 *   ContentBlock10 block = ...
	 *   block.appendToDocument(taxiiDoc);
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
			cBinding.appendChild(taxiiDoc.createTextNode(this.content_binding));
			cBlock.appendChild(cBinding);

			// Content
			Element c = taxiiDoc.createElementNS(taxiiNS, "Content");
			DocumentBuilder db = ClientUtil.generateDocumentBuilder();
			Element element = null;
			if (db == null) {
				logger.error("Document Builder failed to generate.");
			} else {
				element = db.parse(new ByteArrayInputStream(this.content.getBytes())).getDocumentElement();
			}
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
	 * @throws IOException
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