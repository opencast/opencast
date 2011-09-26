/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.oaipmh.harvester;

import org.opencastproject.util.data.Option;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static org.apache.commons.lang.StringUtils.trimToNull;

/**
 * Base class for all OAI-PMH responses.
 */
public abstract class OaiPmhResponse {
  protected final Document doc;
  protected final XPath xpath;

  public OaiPmhResponse(Document doc) {
    this.doc = doc;
    XPath xPath = XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext(OaiPmhNamespaceContext.getContext());
    this.xpath = xPath;
  }

  /**
   * Check if response is an error response.
   */
  public boolean isError() {
    return xpathExists("/oai20:OAI-PMH/oai20:error");
  }

  /**
   * Get the error code if this is an error response.
   */
  public Option<String> getErrorCode() {
    return Option.wrap(trimToNull(xpathString("/oai20:OAI-PMH/oai20:error/@code")));
  }

  /**
   * Check if this is a "noRecordsMatch" error response.
   */
  public boolean isErrorNoRecordsMatch() {
    return xpathExists("/oai20:OAI-PMH/oai20:error[@code='noRecordsMatch']");
  }

  /**
   * Evaluate the xpath expression against the contained document.
   * The expression must return a string (text).
   */
  protected String xpathString(String expr) {
    try {
      return ((String) xpath.evaluate(expr, doc, XPathConstants.STRING)).trim();
    } catch (XPathExpressionException e) {
      throw new RuntimeException("malformed xpath expression " + expr, e);
    }
  }

  /**
   * Evaluate the xpath expression against the contained document.
   * The expression must return a node.
   */
  protected Node xpathNode(String expr) {
    try {
      return (Node) xpath.evaluate(expr, doc, XPathConstants.NODE);
    } catch (XPathExpressionException e) {
      throw new RuntimeException("malformed xpath expression " + expr, e);
    }
  }

  /**
   * Evaluate the xpath expression against the contained document.
   * The expression must return a node list.
   */
  protected NodeList xpathNodeList(String expr) {
    try {
      return (NodeList) xpath.evaluate(expr, doc, XPathConstants.NODESET);
    } catch (XPathExpressionException e) {
      throw new RuntimeException("malformed xpath expression " + expr, e);
    }
  }

  protected boolean xpathExists(String expr) {
    return xpathNodeList(expr).getLength() > 0;
  }
}
