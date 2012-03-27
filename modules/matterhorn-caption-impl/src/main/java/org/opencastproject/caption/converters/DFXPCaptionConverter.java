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
package org.opencastproject.caption.converters;

import org.opencastproject.caption.api.Caption;
import org.opencastproject.caption.api.CaptionConverter;
import org.opencastproject.caption.api.CaptionConverterException;
import org.opencastproject.caption.api.IllegalTimeFormatException;
import org.opencastproject.caption.api.Time;
import org.opencastproject.caption.impl.CaptionImpl;
import org.opencastproject.caption.impl.TimeImpl;
import org.opencastproject.caption.util.TimeUtil;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * This is converter for DFXP, XML based caption format. DOM parser is used for both caption importing and exporting,
 * while SAX parser is used for determining which languages are present (DFXP can contain multiple languages).
 */
public class DFXPCaptionConverter implements CaptionConverter {

  /** logging utility */
  private static final Logger logger = LoggerFactory.getLogger(DFXPCaptionConverter.class);

  private static final String EXTENSION = "dfxp.xml";

  /**
   * {@inheritDoc} Parser used for parsing XML document is DOM parser. Language parameter will determine which language
   * is searched for and parsed. If there is no matching language, empty collection is returned. If language parameter
   * is <code>null</code> first language found is parsed.
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#importCaption(java.io.InputStream, java.lang.String)
   */
  @Override
  public List<Caption> importCaption(InputStream in, String language) throws CaptionConverterException {

    // create new collection
    List<Caption> collection = new ArrayList<Caption>();

    Document doc;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      doc = builder.parse(in);
      doc.getDocumentElement().normalize();
    } catch (ParserConfigurationException e) {
      throw new CaptionConverterException("Could not parse captions", e);
    } catch (SAXException e) {
      throw new CaptionConverterException("Could not parse captions", e);
    } catch (IOException e) {
      throw new CaptionConverterException("Could not parse captions", e);
    }

    // get all <div> elements since they contain information about language
    NodeList divElements = doc.getElementsByTagName("div");

    Element targetDiv = null;
    if (language != null) {
      // find first <div> element with matching language
      for (int i = 0; i < divElements.getLength(); i++) {
        Element n = (Element) divElements.item(i);
        if (n.getAttribute("xml:lang").equals(language)) {
          targetDiv = n;
          break;
        }
      }
    } else {
      if (divElements.getLength() > 1) {
        // more than one existing <div> element, no language specified
        logger.warn("More than one <div> element available. Parsing first one...");
      }
      if (divElements.getLength() != 0) {
        targetDiv = (Element) divElements.item(0);
      }
    }

    // check if we found node
    if (targetDiv == null) {
      logger.warn("No suitable <div> element found for language {}", language);
    } else {
      NodeList pElements = targetDiv.getElementsByTagName("p");

      // initialize start time
      Time time = null;
      try {
        time = new TimeImpl(0, 0, 0, 0);
      } catch (IllegalTimeFormatException e1) {
      }

      for (int i = 0; i < pElements.getLength(); i++) {
        try {
          Caption caption = parsePElement((Element) pElements.item(i));
          // check time
          if (caption.getStartTime().compareTo(time) < 0
                  || caption.getStopTime().compareTo(caption.getStartTime()) <= 0) {
            logger.warn("Caption with invalid time encountered. Skipping...");
            continue;
          }
          collection.add(caption);
        } catch (IllegalTimeFormatException e) {
          logger.warn("Caption with invalid time format encountered. Skipping...");
        }
      }
    }

    // return collection
    return collection;
  }

  /**
   * Parse &lt;p&gt; element which contains one caption.
   * 
   * @param p
   *          &lt;p&gt; element to be parsed
   * @return new {@link Caption} object
   * @throws IllegalTimeFormatException
   *           if time format does not match with expected format for DFXP
   */
  private Caption parsePElement(Element p) throws IllegalTimeFormatException {
    Time begin = TimeUtil.importDFXP(p.getAttribute("begin").trim());
    Time end = TimeUtil.importDFXP(p.getAttribute("end").trim());
    // FIXME add logic for duration if end is absent

    // get text inside p
    String[] textArray = getTextCore(p).split("\n");

    return new CaptionImpl(begin, end, textArray);
  }

  /**
   * Returns caption text stripped of all tags.
   * 
   * @param p
   *          &lt;p&gt; element to be parsed
   * @return Caption text with \n as new line character
   */
  private String getTextCore(Node p) {
    StringBuffer captionText = new StringBuffer();
    // get children
    NodeList list = p.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      if (list.item(i).getNodeType() == Node.TEXT_NODE) {
        captionText.append(list.item(i).getTextContent());
      } else if ("br".equals(list.item(i).getNodeName())) {
        captionText.append("\n");
      } else {
        captionText.append(getTextCore(list.item(i)));
      }
    }
    return captionText.toString().trim();
  }

  /**
   * {@inheritDoc} DOM parser is used to parse template from which whole document is then constructed.
   */
  @Override
  public void exportCaption(OutputStream outputStream, List<Caption> captions, String language) throws IOException {
    // get document builder factory and parse template
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    Document doc = null;
    InputStream is = null;
    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      // load dfxp template from file
      is = DFXPCaptionConverter.class.getResourceAsStream("/templates/template.dfxp.xml");
      doc = builder.parse(is);
    } catch (ParserConfigurationException e) {
      // should not happen
      throw new RuntimeException(e);
    } catch (SAXException e) {
      // should not happen unless template is invalid
      throw new RuntimeException(e);
    } catch (IOException e) {
      // should not happen
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(is);
    }

    // retrieve body element
    Node bodyNode = doc.getElementsByTagName("body").item(0);

    // create new div element with specified language
    Element divNode = doc.createElement("div");
    divNode.setAttribute("xml:lang", language != null ? language : "und");
    bodyNode.appendChild(divNode);

    // update document
    for (Caption caption : captions) {
      Element newNode = doc.createElement("p");
      newNode.setAttribute("begin", TimeUtil.exportToDFXP(caption.getStartTime()));
      newNode.setAttribute("end", TimeUtil.exportToDFXP(caption.getStopTime()));
      String[] captionText = caption.getCaption();
      // text part
      newNode.appendChild(doc.createTextNode(captionText[0]));
      for (int i = 1; i < captionText.length; i++) {
        newNode.appendChild(doc.createElement("br"));
        newNode.appendChild(doc.createTextNode(captionText[i]));
      }
      divNode.appendChild(newNode);
    }

    // initialize stream writer
    OutputStreamWriter osw = new OutputStreamWriter(outputStream, "UTF-8");
    StreamResult result = new StreamResult(osw);
    DOMSource source = new DOMSource(doc);
    TransformerFactory tfactory = TransformerFactory.newInstance();
    Transformer transformer;
    try {
      transformer = tfactory.newTransformer();
      transformer.transform(source, result);
      osw.flush();
    } catch (TransformerConfigurationException e) {
      // should not happen
      throw new RuntimeException(e);
    } catch (TransformerException e) {
      // should not happen
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(osw);
    }
  }

  /**
   * {@inheritDoc} Uses SAX parser to quickly read the document and retrieve available languages.
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#getLanguageList(java.io.InputStream)
   */
  @Override
  public String[] getLanguageList(InputStream input) throws CaptionConverterException {

    // create lang list
    final List<String> langList = new LinkedList<String>();

    // get SAX parser
    SAXParserFactory factory = SAXParserFactory.newInstance();
    try {
      SAXParser parser = factory.newSAXParser();
      // create handler
      DefaultHandler handler = new DefaultHandler() {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
          if ("div".equals(qName)) {
            // we found div tag - let's make a lookup for language
            String lang = attributes.getValue("xml:lang");
            if (lang == null) {
              // should never happen
              logger.warn("Missing xml:lang attribute for div element.");
            } else if (langList.contains(lang)) {
              logger.warn("Multiple div elements with same language.");
            } else {
              langList.add(lang);
            }
          }
        }
      };

      // parse stream
      parser.parse(input, handler);
    } catch (ParserConfigurationException e) {
      // should not happen
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new CaptionConverterException("Could not parse captions", e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return langList.toArray(new String[0]);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionConverter#getExtension()
   */
  @Override
  public String getExtension() {
    return EXTENSION;
  }

}
