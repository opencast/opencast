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

import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.functions.Misc.chuck;

import com.entwinemedia.fn.data.ImmutableIteratorBase;
import org.opencastproject.util.data.Either;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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

  /**
   * Writes an xml representation to a stream.
   *
   * @param doc
   *          the document
   * @param out
   *          the output stream
   * @throws IOException
   *           if there is an error transforming the dom to a stream
   */
  public static void toXml(Document doc, OutputStream out) throws IOException {
    try {
      DOMSource domSource = new DOMSource(doc);
      StreamResult result = new StreamResult(out);
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.VERSION, doc.getXmlVersion());
      transformer.transform(domSource, result);
    } catch (TransformerException e) {
      throw new IOException("unable to transform dom to a stream");
    }
  }

  /**
   * Writes an xml representation to an input stream and return it.
   *
   * @param document
   *          the document
   * @return the input stream containing the serialized xml representation
   * @throws IOException
   *           if there is an error transforming the dom to a stream
   */
  public static InputStream serializeDocument(Document document) throws IOException {
    ByteArrayOutputStream out = null;
    try {
      out = new ByteArrayOutputStream();
      XmlUtil.toXml(document, out);
      return new ByteArrayInputStream(out.toByteArray());
    } finally {
      IoSupport.closeQuietly(out);
    }
  }

  /**
   * Serializes the document to a XML string
   *
   * @param document
   *          the document
   * @return the serialized XML string
   * @throws IOException
   *           if there is an error transforming the dom to a stream
   */
  public static String toXmlString(Document document) throws IOException {
    InputStream inputStream = null;
    try {
      inputStream = serializeDocument(document);
      return IOUtils.toString(inputStream, "UTF-8");
    } finally {
      IoSupport.closeQuietly(inputStream);
    }
  }

  /** Create an {@link org.xml.sax.InputSource} from an XML string. */
  public static InputSource fromXmlString(String xml) {
    return new InputSource(IOUtils.toInputStream(xml));
  }

  /**
   * Creates an xml document root and returns it.
   *
   * @return the document
   */
  public static Document newDocument() {
    try {
      return nsDbf.newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      return chuck(e);
    }
  }

  /** Make a {@link org.w3c.dom.NodeList} iterable. */
  public static <A extends Node> Iterable<A> iterable(final NodeList nl) {
    return new Iterable<A>() {
      @Override
      public Iterator<A> iterator() {
        return new ImmutableIteratorBase<A>() {
          private int index = 0;

          @Override
          public boolean hasNext() {
            return index < nl.getLength();
          }

          @Override
          public A next() {
            if (hasNext()) {
              final Node next = nl.item(index);
              index = index + 1;
              return (A) next;
            } else {
              throw new NoSuchElementException();
            }
          }
        };
      }
    };
  }
}
