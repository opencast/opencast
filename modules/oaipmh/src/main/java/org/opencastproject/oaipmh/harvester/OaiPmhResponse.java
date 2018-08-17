/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.oaipmh.harvester;

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.functions.Booleans.eq;

import org.opencastproject.util.data.Option;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Base class for all OAI-PMH responses.
 */
public abstract class OaiPmhResponse {
  protected final Document doc;
  protected final XPath xpath;

  public OaiPmhResponse(Document doc) {
    this.doc = doc;
    this.xpath = createXPath();
  }

  /**
   * Create an XPath object suitable for processing OAI-PMH response documents.
   */
  public static XPath createXPath() {
    XPath xPath = XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext(OaiPmhNamespaceContext.getContext());
    return xPath;
  }

  /**
   * Check if response is an error response.
   */
  public boolean isError() {
    return xpathExists("/oai20:OAI-PMH/oai20:error");
  }

  protected boolean isError(String code) {
    return getErrorCode().map(eq(code)).getOrElse(false);
  }

  /**
   * Get the error code if this is an error response.
   */
  public Option<String> getErrorCode() {
    return option(trimToNull(xpathString("/oai20:OAI-PMH/oai20:error/@code")));
  }

  /** Return the request tag. */
  public String getRequest() {
    return xpathString("/oai20:OAI-PMH/oai20:request");
  }

  /**
   * Evaluate the xpath expression against the contained document. The expression must return a string (text).
   */
  protected String xpathString(String expr) {
    try {
      return ((String) xpath.evaluate(expr, doc, XPathConstants.STRING)).trim();
    } catch (XPathExpressionException e) {
      throw new RuntimeException("malformed xpath expression " + expr, e);
    }
  }

  /**
   * Evaluate the xpath expression against the contained document. The expression must return a node.
   */
  protected Node xpathNode(String expr) {
    try {
      return (Node) xpath.evaluate(expr, doc, XPathConstants.NODE);
    } catch (XPathExpressionException e) {
      throw new RuntimeException("malformed xpath expression " + expr, e);
    }
  }

  public static Node xpathNode(XPath xpath, Node context, String expr) {
    if (expr.startsWith("/"))
      throw new IllegalArgumentException("An xpath expression that evaluates relative to a given context node "
              + "must not be absolute, i.e. start with a '/'. In this case the expression is evaluated against the"
              + "whole document which might not be wanted.");
    try {
      return (Node) xpath.evaluate(expr, context, XPathConstants.NODE);
    } catch (XPathExpressionException e) {
      throw new RuntimeException("malformed xpath expression " + expr, e);
    }
  }

  /**
   * Evaluate the xpath expression against the contained document. The expression must return a node list.
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
