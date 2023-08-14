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

package org.opencastproject.util;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.util.data.Option;

import org.w3c.dom.Node;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/** A thin layer around {@link XPath} to evaluate expressions in the context of a {@link Node}. */
public final class Xpath {
  private final XPath xpath;
  private final Node node;

  private Xpath(Node node) {
    this.xpath = XPathFactory.newInstance().newXPath();
    this.node = node;
  }

  /** Create a new evaluation context for <code>node</code> respecting the given namespace resolutions. */
  public static Xpath mk(Node node, NamespaceContext ns) {
    final Xpath xpath = new Xpath(node);
    xpath.xpath.setNamespaceContext(ns);
    return xpath;
  }

  /** Evaluate the xpath expression against the contained document. The expression must return a node. */
  // todo replace return type with Valid once it is implemented
  public Option<Node> node(String expr) {
    try {
      return option((Node) xpath.evaluate(expr, node, XPathConstants.NODE));
    } catch (XPathExpressionException e) {
      return none();
    }
  }

}
