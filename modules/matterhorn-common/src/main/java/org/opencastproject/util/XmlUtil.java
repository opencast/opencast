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
package org.opencastproject.util;

import org.apache.commons.io.IOUtils;
import org.opencastproject.util.data.Either;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;

/** XML utility functions. */
public final class XmlUtil {
  private static final DocumentBuilderFactory nsDbf;
  private static final DocumentBuilderFactory dbf;

  static {
    nsDbf = DocumentBuilderFactory.newInstance();
    nsDbf.setNamespaceAware(true);
    //
    dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(false);
  }

  private XmlUtil() {
  }

  /** Namespace aware parsing of <code>src</code>. */
  public static Either<Exception, Document> parseNs(InputSource src) {
    try {
      DocumentBuilder docBuilder = nsDbf.newDocumentBuilder();
      return right(docBuilder.parse(src));
    } catch (Exception e) {
      return left(e);
    }
  }

  /** Namespace aware parsing of <code>xml</code>. */
  public static Either<Exception, Document> parseNs(String xml) {
    return parseNs(fromXmlString(xml));
  }

  /** Parsing of <code>src</code> without namespaces. */
  public static Either<Exception, Document> parse(InputSource src) {
    try {
      DocumentBuilder docBuilder = dbf.newDocumentBuilder();
      return right(docBuilder.parse(src));
    } catch (Exception e) {
      return left(e);
    }
  }

  public static InputSource fromXmlString(String xml) {
    return new InputSource(IOUtils.toInputStream(xml));
  }
}
