package com.bcmcgroup.flare.xmldsig;

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

import org.apache.log4j.Logger;
import org.apache.jcp.xml.dsig.internal.dom.DOMSubTreeData;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bcmcgroup.flare.xmldsig.MyURIDereferencer;

/**
 * This class is used to implement a custom URIDereferencer for Xmldsig.sign()
 *
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 *
 */

class MyURIDereferencer implements URIDereferencer {
	private static final Logger logger = Logger.getLogger(MyURIDereferencer.class);
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