/*
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

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XmlSafeParserTest {
  @Test
  public void newDocumentBuilderFactoryTest() throws ParserConfigurationException, SAXException, IOException {
    InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-safe.xml");

    DocumentBuilder db = XmlSafeParser.newDocumentBuilderFactory().newDocumentBuilder();
    Document d = db.parse(xmlInput);
  }

  @Test
  public void newDocumentBuilderFactoryUnsafeTest() throws ParserConfigurationException, SAXException, IOException {
    assertThrows(SAXParseException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe.xml");

      DocumentBuilder db = XmlSafeParser.newDocumentBuilderFactory().newDocumentBuilder();
      Document d = db.parse(xmlInput);
    });
  }

  @Test
  public void newDocumentBuilderFactoryUnsafe2Test() throws ParserConfigurationException, SAXException, IOException {
    assertThrows(SAXParseException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe2.xml");

      DocumentBuilder db = XmlSafeParser.newDocumentBuilderFactory().newDocumentBuilder();
      Document d = db.parse(xmlInput);
    });
  }

  @Test
  public void newSAXParserFactoryTest() throws ParserConfigurationException, SAXException, IOException {
    InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-safe.xml");

    SAXParser sp = XmlSafeParser.newSAXParserFactory().newSAXParser();
    sp.parse(xmlInput, new DefaultHandler());
  }

  @Test
  public void newSAXParserFactoryTest2() throws ParserConfigurationException, SAXException, IOException {
    InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-safe.xml");

    SAXParser sp = XmlSafeParser.newSAXParserFactory().newSAXParser();
    sp.getXMLReader().parse(new InputSource(xmlInput));
  }

  @Test
  public void newSAXParserFactoryUnsafeTest() throws ParserConfigurationException, SAXException, IOException {
    assertThrows(SAXParseException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe.xml");

      SAXParser sp = XmlSafeParser.newSAXParserFactory().newSAXParser();
      sp.parse(xmlInput, new DefaultHandler());
    });
  }

  @Test
  public void newSAXParserFactoryUnsafe2Test() throws ParserConfigurationException, SAXException, IOException {
    assertThrows(SAXParseException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe2.xml");

      SAXParser sp = XmlSafeParser.newSAXParserFactory().newSAXParser();
      sp.parse(xmlInput, new DefaultHandler());
    });
  }

  @Test
  public void newSAXParserFactoryUnsafeTest2() throws ParserConfigurationException, SAXException, IOException {
    assertThrows(SAXParseException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe.xml");

      SAXParser sp = XmlSafeParser.newSAXParserFactory().newSAXParser();
      sp.getXMLReader().parse(new InputSource(xmlInput));
    });
  }

  @Test
  public void newSAXParserFactoryUnsafe2Test2() throws ParserConfigurationException, SAXException, IOException {
    assertThrows(SAXParseException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe2.xml");

      SAXParser sp = XmlSafeParser.newSAXParserFactory().newSAXParser();
      sp.getXMLReader().parse(new InputSource(xmlInput));
    });
  }

  @Test
  public void newTransformerFactoryTest() throws TransformerException, TransformerConfigurationException {
    InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-safe.xml");

    Transformer tf = XmlSafeParser.newTransformerFactory().newTransformer();
    tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
  }

  @Test
  public void newTransformerFactoryUnsafeTest() {
    assertThrows(TransformerException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe.xml");

      Transformer tf = XmlSafeParser.newTransformerFactory().newTransformer();
      tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
    });
  }

  @Test
  public void newTransformerFactoryUnsafe2Test() {
    assertThrows(TransformerException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe2.xml");

      Transformer tf = XmlSafeParser.newTransformerFactory().newTransformer();
      tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
    });
  }

  @Test
  public void configureTransformerFactoryTest() throws TransformerException, TransformerConfigurationException {
    InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-safe.xml");

    // CHECKSTYLE:OFF
    Transformer tf = XmlSafeParser.configureTransformerFactory(TransformerFactory.newInstance()).newTransformer();
    // CHECKSTLYE:ON
    tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
  }

  @Test
  public void configureTransformerFactoryUnsafeTest() {
    assertThrows(TransformerException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe.xml");

      // CHECKSTYLE:OFF
      Transformer tf = XmlSafeParser.configureTransformerFactory(TransformerFactory.newInstance()).newTransformer();
      // CHECKSTLYE:ON
      tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
    });
  }

  @Test
  public void configureTransformerFactoryUnsafe2Test() {
    assertThrows(TransformerException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe2.xml");

      // CHECKSTYLE:OFF
      Transformer tf = XmlSafeParser.configureTransformerFactory(TransformerFactory.newInstance()).newTransformer();
      // CHECKSTLYE:ON
      tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
    });
  }

  @Test
  public void configureTransformerFactoryInbuiltTest() throws TransformerException, TransformerConfigurationException {
    InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-safe.xml");

    Transformer tf = XmlSafeParser.configureTransformerFactory(
      // CHECKSTYLE:OFF
      TransformerFactory.newInstance("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", null)
      // CHECKSTLYE:ON
    ).newTransformer();
    tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
  }

  @Test
  public void configureTransformerFactoryInbuiltUnsafeTest() {
    assertThrows(TransformerException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe.xml");

      Transformer tf = XmlSafeParser.configureTransformerFactory(
          // CHECKSTYLE:OFF
          TransformerFactory.newInstance("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", null)
      // CHECKSTLYE:ON
      ).newTransformer();
      tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
    });
  }

  @Test
  public void configureTransformerFactoryInbuiltUnsafe2Test() {
    assertThrows(TransformerException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe2.xml");

      Transformer tf = XmlSafeParser.configureTransformerFactory(
          // CHECKSTYLE:OFF
          TransformerFactory.newInstance("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", null)
      // CHECKSTLYE:ON
      ).newTransformer();
      tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
    });
  }

  @Test
  public void configureTransformerFactorySaxonTest() throws TransformerException, TransformerConfigurationException {
    InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-safe.xml");

    Transformer tf = XmlSafeParser.configureTransformerFactory(
      // CHECKSTYLE:OFF
      TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null)
      // CHECKSTLYE:ON
    ).newTransformer();
    tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
  }

  @Test
  public void configureTransformerFactorySaxonUnsafeTest() {
    assertThrows(TransformerException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe.xml");

      Transformer tf = XmlSafeParser.configureTransformerFactory(
          // CHECKSTYLE:OFF
          TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null)
      // CHECKSTLYE:ON
      ).newTransformer();
      tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
    });
  }

  @Test
  public void configureTransformerFactorySaxonUnsafe2Test() {
    assertThrows(TransformerException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe2.xml");

      Transformer tf = XmlSafeParser.configureTransformerFactory(
          // CHECKSTYLE:OFF
          TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null)
      // CHECKSTLYE:ON
      ).newTransformer();
      tf.transform(new StreamSource(xmlInput), new StreamResult(new StringWriter()));
    });
  }

  @Test
  public void parseTest() throws SAXException, IOException {
    InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-safe.xml");

    Document d = XmlSafeParser.parse(xmlInput);
  }

  @Test
  public void parseUnsafeTest() throws SAXException, IOException {
    assertThrows(SAXParseException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe.xml");

      Document d = XmlSafeParser.parse(xmlInput);
    });
  }

  @Test
  public void parseUnsafe2Test() throws SAXException, IOException {
    assertThrows(SAXParseException.class, () -> {
      InputStream xmlInput = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe2.xml");

      Document d = XmlSafeParser.parse(xmlInput);
    });
  }

  @Test
  public void parseChain() throws SAXException, IOException {
    InputStream xmlInputSafe1 = XmlSafeParserTest.class.getResourceAsStream("/dublincore-safe.xml");
    InputStream xmlInputSafe2 = XmlSafeParserTest.class.getResourceAsStream("/dublincore-safe.xml");
    InputStream xmlInputUnsafe1 = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe.xml");
    InputStream xmlInputUnsafe2 = XmlSafeParserTest.class.getResourceAsStream("/dublincore-unsafe2.xml");

    int catched = 0;
    Document d1;
    Document d2;
    Document d3;
    Document d4;

    try {
      d1 = XmlSafeParser.parse(xmlInputUnsafe1);
    }
    catch(SAXParseException e) {
      catched++;
    };

    d2 = XmlSafeParser.parse(xmlInputSafe1);

    try {
      d3 = XmlSafeParser.parse(xmlInputUnsafe2);
    }
    catch(SAXParseException e) {
      catched++;
    };

    Assertions.assertEquals(catched, 2);

    d4 = XmlSafeParser.parse(xmlInputSafe2);
  }
}
