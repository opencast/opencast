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

package org.opencastproject.metadata.dublincore;

import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.XMLCatalogImpl;
import org.opencastproject.mediapackage.XMLCatalogImpl.CatalogEntry;
import org.opencastproject.util.XmlNamespaceContext;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;

/**
 * XML serialization of Dublin Core catalogs.
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

  // Option to instante catalog with intentionally emptied values
  // used to target removal of DublinCore catalog values during update.
  private DublinCoreXmlFormat(boolean includeEmpty) {
    dc.includeEmpty(includeEmpty);
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
   * Read an XML encoded catalog from a file.
   *
   * @param xml
   *         the file containing the DublinCore catalog
   * @return the catalog representation
   * @throws javax.xml.parsers.ParserConfigurationException
   *         if setting up the parser failed
   * @throws org.xml.sax.SAXException
   *         if an error occurred while parsing the document
   * @throws java.io.IOException
   *         if the stream cannot be accessed in a proper way
   */
  @Nonnull
  public static DublinCoreCatalog read(File xml)
          throws IOException, SAXException, ParserConfigurationException {
    try (FileInputStream in = new FileInputStream(xml)) {
      return new DublinCoreXmlFormat().readImpl(new InputSource(in));
    }
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
    return new DublinCoreXmlFormat().readImpl(
        new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
  }

  public static Opt<DublinCoreCatalog> readOpt(String xml) {
    try {
      return Opt.some(read(xml));
    } catch (Exception e) {
      return Opt.none();
    }
  }

  /** {@link #read(String)} as a function, returning none on error. */
  public static final Fn<String, Opt<DublinCoreCatalog>> readOptFromString = new Fn<String, Opt<DublinCoreCatalog>>() {
    @Override public Opt<DublinCoreCatalog> apply(String xml) {
      return readOpt(xml);
    }
  };

  @Nonnull
  public static DublinCoreCatalog read(Node xml)
      throws TransformerException {
    return new DublinCoreXmlFormat().readImpl(xml);
  }

  @Nonnull
  public static DublinCoreCatalog read(InputSource xml)
      throws IOException, SAXException, ParserConfigurationException {
    return new DublinCoreXmlFormat().readImpl(xml);
  }

  // Optional read to optionally instantiate catalog with intentionally empty elements
  // used to remove existing DublinCore catalog values during a merge update.
  @Nonnull
  public static DublinCoreCatalog read(String xml, boolean includeEmptiedElements)
          throws IOException, SAXException, ParserConfigurationException {
    return new DublinCoreXmlFormat(includeEmptiedElements).readImpl(
        new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
  }

  @Nonnull
  public static DublinCoreCatalog read(Node xml, boolean includeEmptiedElements)
      throws TransformerException {
    return new DublinCoreXmlFormat(includeEmptiedElements).readImpl(xml);
  }

  @Nonnull
  public static DublinCoreCatalog read(InputSource xml, boolean includeEmptiedElements)
      throws IOException, SAXException, ParserConfigurationException {
    return new DublinCoreXmlFormat(includeEmptiedElements).readImpl(xml);
  }

  @Nonnull
  public static DublinCoreCatalog read(File xml, boolean includeEmptiedElements)
          throws IOException, SAXException, ParserConfigurationException {
    try (FileInputStream in = new FileInputStream(xml)) {
      return new DublinCoreXmlFormat(includeEmptiedElements).readImpl(new InputSource(in));
    }
  }

  /**
   * Merge values that are  new, changed, or emptied from the "from" catalog into the existing "into" catalog.
   *
   * @param fromCatalog contains targeted new values (new elements, changed element values, emptied element values).
   * @param intoCatalog is the existing catalog that merges in the targed changes.
   * @return the merged catalog
   */
  public static DublinCoreCatalog merge(DublinCoreCatalog fromCatalog, DublinCoreCatalog intoCatalog) {

    // If one catalog is null, return the other.
    if (fromCatalog == null) {
      return intoCatalog;
    }
    if (intoCatalog == null) {
      return fromCatalog;
    }

    DublinCoreCatalog mergedCatalog = (DublinCoreCatalog) intoCatalog.clone();
    List<CatalogEntry> mergeEntries = fromCatalog.getEntriesSorted();

    for (CatalogEntry mergeEntry: mergeEntries) {
      // ignore root entry
      if ((mergeEntry.getEName()).equals(intoCatalog.getRootTag()))
        continue;

      // if language is provided, only overwrite existing of same language
      String lang = mergeEntry.getAttribute(XMLCatalogImpl.XML_LANG_ATTR);
      if (StringUtils.isNotEmpty(lang)) {
        // Passing a null value will remove the exiting value
        mergedCatalog.set(mergeEntry.getEName(), StringUtils.trimToNull(mergeEntry.getValue()), lang);
      } else {
        mergedCatalog.set(mergeEntry.getEName(), StringUtils.trimToNull(mergeEntry.getValue()));
      }
    }
    return mergedCatalog;
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
      for (CatalogEntry element : dc.getEntriesSorted()) {
        rootElement.appendChild(element.toXml(doc));
      }
      return doc;
    } else {
      throw new RuntimeException("DublinCore catalog does not have a root tag.");
    }
  }

  public static String writeString(DublinCoreCatalog dc) {
    try {
      return dc.toXmlString();
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Error serializing the episode dublincore catalog %s.", dc), e);
    }
  }

  // SAX

  private DublinCoreCatalog readImpl(Node node) throws TransformerException {
    final Result outputTarget = new SAXResult(this);
    final Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    t.transform(new DOMSource(node), outputTarget);
    return dc;
  }

  private DublinCoreCatalog readImpl(InputSource in)
          throws ParserConfigurationException, SAXException, IOException {
    final SAXParserFactory factory = SAXParserFactory.newInstance();
    // no DTD
    factory.setValidating(false);
    // namespaces!
    factory.setNamespaceAware(true);
    // read document
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
