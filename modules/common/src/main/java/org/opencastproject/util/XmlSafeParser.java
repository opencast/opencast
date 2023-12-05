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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;


/** Preconfigured XML parsers, which are safeguarded against XXE and billion laugh attacks. */
public final class XmlSafeParser {

  private static final Logger logger = LoggerFactory.getLogger(XmlSafeParser.class);

  private XmlSafeParser() {
  }

  /**
   * Creates a preconfigured DocumentBuilderFactory, which is guarded against XXE and billion laugh attacks.
   * @return the preconfigured DocumentBuilderFactory
   */
  public static DocumentBuilderFactory newDocumentBuilderFactory() {
    // CHECKSTYLE:OFF
    DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    // CHECKSTYLE:ON
    // prevent XXE see
    // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
    // for more information
    try {
      f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      f.setFeature("http://xml.org/sax/features/external-general-entities", false);
      f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      f.setXIncludeAware(false);
      f.setExpandEntityReferences(false);
    }
    catch (Exception e) {
      // this shouldn't occur
      logger.error("Failed to configure safe DocumentBuilderFactory to prevent XXE.");
      throw new AssertionError("Failed to configure safe DocumentBuilderFactory to prevent XXE.", e);
    }

    return f;
  }

  /**
   * Creates a preconfigured SAXParserFactory, which is guarded against XXE and billion laugh attacks.
   * @return the preconfigured SAXParserFactory
   */
  public static SAXParserFactory newSAXParserFactory() {
    // CHECKSTYLE:OFF
    SAXParserFactory f = SAXParserFactory.newInstance();
    // CHECKSTYLE:ON

    try {
      f.setFeature("http://xml.org/sax/features/external-general-entities", false);
      f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    }
    catch (Exception e) {
      // this shouldn't occur
      logger.error("Failed to configure safe SAXParserFactory to prevent XXE.");
      throw new AssertionError("Failed to configure safe SAXParserFactory to prevent XXE.", e);
    }

    return f;
  }

  /**
   * Creates a preconfigured default TransformerFactory, which is guarded against XXE and billion laugh attacks.
   * @return the preconfigured TransformerFactory
   */
  // CHECKSTYLE:OFF
  public static TransformerFactory newTransformerFactory() {
    return configureTransformerFactory(TransformerFactory.newInstance("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", null));
  }
  // CHECKSTYLE:ON

  /**
   * Configures a TransformerFactory, to guard it against XXE and billion laugh attacks.
   * Supports the default Transformer and the Saxon Transformer.
   * The returned TransformerFactory is the same as the passed TranformerFactory.
   * @param f the TransformerFactory to configure
   * @return the configured Factory
   */
  public static TransformerFactory configureTransformerFactory(TransformerFactory f) {
    try {
      if (f.getClass().getName().equals("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl")) {
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        f.setAttribute("http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit", "1");
      }
      else if (f.getClass().getName().equals("net.sf.saxon.TransformerFactoryImpl")) {
        f.setAttribute("http://saxon.sf.net/feature/parserFeature?uri=http://apache.org/xml/features/disallow-doctype-decl", true);
      }
      else {
        throw new AssertionError("Unknown TransformerFactory " + f.getClass().getName());
      }
    }
    catch (Exception e) {
      // this shouldn't occur
      logger.error("Failed to configure safe TransformerFactory to prevent XXE.");
      throw new AssertionError("Failed to configure safe TransformerFactory to prevent XXE.", e);
    }

    return f;
  }

  /**
   * Parse a XML Document with a parser, which is guarded against XXE and billion laugh attacks.
   * The parsing is namespace aware.
   * Designed for checking documents for XXE and billion laugh attacks before further parsing
   * the returned document with the Unmarshaller, which can't be safely configured.
   * @param in the document to parse
   * @return the parsed document
   */
  public static Document parse(InputStream in) throws IOException, SAXException {
    return parse(new InputSource(in));
  }

  /**
   * The DocumentBuilder for the parse methods.
   * Creating a DocumentBuilder is quite expensive and DocumentBuilder is not thread-safe,
   * therefore we create a DocumentBuilder for each Thread.
   */
  private static ThreadLocal<DocumentBuilder> db = new ThreadLocal<DocumentBuilder>() {
          @Override
          protected DocumentBuilder initialValue() {
            DocumentBuilderFactory dbf = newDocumentBuilderFactory();
            DocumentBuilder d = null;
            try {
              dbf.setNamespaceAware(true);
              d = dbf.newDocumentBuilder();
            }
            catch (Exception e) {
              // this shouldn't occur
              logger.error("Failed to configure safe DocumentBuilder to prevent XXE.");
              throw new AssertionError("Failed to configure safe DocumentBuilder to prevent XXE.", e);
            }

            return d;
          }
  };

  /**
   * Parse a XML Document with a parser, which is guarded against XXE and billion laugh attacks.
   * The parsing is namespace aware.
   * Designed for checking documents for XXE and billion laugh attacks before further parsing
   * the returned document with the Unmarshaller, which can't be safely configured.
   * @param s the document to parse
   * @return the parsed document
   */
  public static Document parse(InputSource s) throws IOException, SAXException {
    // Use ThreadLocal DocumentBuilder to avoid building a new DocumentBuilder on each call.
    return db.get().parse(s);
  }

}
