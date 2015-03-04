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
package org.opencastproject.metadata.dublincore;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.XMLCatalogImpl.CatalogEntry;
import org.opencastproject.util.XmlNamespaceContext;

import org.apache.commons.io.IOUtils;
import org.eclipse.persistence.internal.oxm.record.DOMInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;

/**
 * Parse a DublinCore catalog from XML.
 */
@ParametersAreNonnullByDefault
public final class DublinCoreXmlFormat extends DefaultHandler {
  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreXmlFormat.class);

  /** The element content */
  private StringBuilder content = new StringBuilder();

  /** The node attributes */
  private Attributes attributes = null;

  private DublinCoreCatalog dc = DublinCores.mkSimple();

  private DublinCoreXmlFormat() {
  }

  /**
   * Read an XML encoded catalog from a stream.
   *
   * @param xml
   *         the input stream containing the DublinCore catalog
   * @return the catalog representation
   * @throws javax.xml.parsers.ParserConfigurationException
   *         if setting up the parser failed
   * @throws org.xml.sax.SAXException
   *         if an error occurred while parsing the document
   * @throws java.io.IOException
   *         if the stream cannot be accessed in a proper way
   */
  @Nonnull
  public static DublinCoreCatalog read(InputStream xml)
          throws IOException, SAXException, ParserConfigurationException {
    return new DublinCoreXmlFormat().readImpl(new InputSource(xml));
  }

  /**
   * Read an XML encoded catalog from a string.
   *
   * @param xml
   *         the string containing the DublinCore catalog
   * @return the catalog representation
   * @throws javax.xml.parsers.ParserConfigurationException
   *         if setting up the parser failed
   * @throws org.xml.sax.SAXException
   *         if an error occurred while parsing the document
   * @throws java.io.IOException
   *         if the stream cannot be accessed in a proper way
   */
  @Nonnull
  public static DublinCoreCatalog read(String xml)
          throws IOException, SAXException, ParserConfigurationException {
    return new DublinCoreXmlFormat().readImpl(new InputSource(IOUtils.toInputStream(xml, "UTF-8")));
  }

  @Nonnull
  public static DublinCoreCatalog read(Node xml)
          throws IOException, SAXException, ParserConfigurationException {
    return new DublinCoreXmlFormat().readImpl(new DOMInputSource(xml));
  }

  public static Document writeDocument(DublinCoreCatalog dc)
          throws ParserConfigurationException, TransformerException, IOException {
    // Create the DOM document
    final Document doc;
    {
      final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      docBuilderFactory.setNamespaceAware(true);
      doc = docBuilderFactory.newDocumentBuilder().newDocument();
    }
    if (dc.getRootTag() != null) {
      final Element rootElement = doc.createElementNS(dc.getRootTag().getNamespaceURI(), dc.toQName(dc.getRootTag()));
      doc.appendChild(rootElement);
      for (EName property : dc.getProperties()) {
        for (CatalogEntry element : dc.getValues(property)) {
          rootElement.appendChild(element.toXml(doc));
        }
      }
      return doc;
    } else {
      throw new RuntimeException("DublinCore catalog does not have a root tag.");
    }
  }

  // SAX

  private DublinCoreCatalog readImpl(InputSource in)
          throws ParserConfigurationException, SAXException, IOException {
    final SAXParserFactory factory = SAXParserFactory.newInstance();
    // no DTD
    factory.setValidating(false);
    // namespaces!
    factory.setNamespaceAware(true);
    // read document                                   ‘
    factory.newSAXParser().parse(in, this);
    return dc;
  }

  /**
   * Returns the element content.
   */
  private String getAndResetContent() {
    String str = content.toString().trim();
    content = new StringBuilder();
    return str;
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    super.characters(ch, start, length);
    content.append(ch, start, length);
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    dc.addBindings(XmlNamespaceContext.mk(prefix, uri));
  }

  /**
   * Read <code>type</code> attribute from track or catalog element.
   *
   * @see org.xml.sax.helpers.DefaultHandler#startElement(String, String, String, org.xml.sax.Attributes)
   */
  @Override
  public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
    if (dc.getRootTag() == null) {
      dc.setRootTag(new EName(uri, localName));
    }
    this.attributes = attributes;
  }

  @Override
  public void endElement(String uri, String localName, String name) throws SAXException {
    if (dc.getRootTag() != null) {
      dc.addElement(EName.mk(uri, localName), getAndResetContent(), attributes);
    }
  }

  @Override
  public void error(SAXParseException e) throws SAXException {
    logger.warn("Error parsing DublinCore catalog: " + e.getMessage());
    super.error(e);
  }

  @Override
  public void fatalError(SAXParseException e) throws SAXException {
    logger.warn("Fatal error parsing DublinCore catalog: " + e.getMessage());
    super.fatalError(e);
  }

  @Override
  public void warning(SAXParseException e) throws SAXException {
    logger.warn("Warning parsing DublinCore catalog: " + e.getMessage());
    super.warning(e);
  }
}
