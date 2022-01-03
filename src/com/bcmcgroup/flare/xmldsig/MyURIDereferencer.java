package com.bcmcgroup.flare.xmldsig;

/*
© 2015-2020 BCMC. Funding for this work was provided by the United States Government 
under contract GS06F1165Z/HSHQDC14F00094. The United States Government may use, 
disclose, reproduce, prepare derivative works, distribute copies to the public, 
and perform publicly and display publicly, in any manner and for any purpose, 
and to have or permit others to do so.

Please be advised that this project uses other open source software and uses of 
these software or their components must follow their respective license.
*/


import java.util.ArrayList;
import java.util.List;

import javax.xml.crypto.Data;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.*;
import org.apache.jcp.xml.dsig.internal.dom.DOMSubTreeData;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bcmcgroup.flare.xmldsig.MyURIDereferencer;

/**
 * This class is used to implement a custom URIDereferencer for Xmldsig.sign()
 * @version		2.0.4
 */

class MyURIDereferencer implements URIDereferencer {
	private static final Logger logger = LogManager.getLogger(MyURIDereferencer.class);
	private Node node = null;
    private final List<Node> l = new ArrayList<>();

    public MyURIDereferencer(Node node) {
    	this.node = node;
    }

    public Data dereference(URIReference uriReference, XMLCryptoContext context) throws URIReferenceException {
          XPath xpath = XPathFactory.newInstance().newXPath();
          NodeList nodes = null;
          try {
        	  String x = uriReference.getURI().replace("#xpointer(", "");
        	  x = x.substring(0, x.length()-1);
        	  nodes = (NodeList) xpath.evaluate(x, this.node, XPathConstants.NODESET);
          } catch (XPathExpressionException e) {
              logger.debug("XPathExpression Exception when defrencing a URI.");
          }

          if (nodes != null) {
	          for (int i = 0; i < nodes.getLength(); i++) {
	        	  l.add(nodes.item(i));
	          }
          }

          return new DOMSubTreeData(l.get(0), true);
     }
}
