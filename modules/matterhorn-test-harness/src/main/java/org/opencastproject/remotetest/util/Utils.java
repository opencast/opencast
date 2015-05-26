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

package org.opencastproject.remotetest.util;

import org.opencastproject.remotetest.Main;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Test utilities
 *
 */
public class Utils {

  public static Document parseXml(InputStream in) throws Exception {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /**
   * Converts the node to a string representation.
   *
   * @param node
   *          the node
   * @return the string representation
   * @throws Exception
   */
  public static String nodeToString(Node node) throws Exception {
    DOMSource domSource = new DOMSource(node);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    StreamResult result = new StreamResult(out);
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.transform(domSource, result);
    InputStream in = new ByteArrayInputStream(out.toByteArray());
    return IOUtils.toString(in, "UTF-8");
  }

  /**
   * Converts a node list to a list of their string values. Nodes that do not have a string value are returned as the
   * empty string.
   */
  public static List<String> nodeListToStringList(NodeList nodes) {
    List<String> strings = new ArrayList<String>(nodes.getLength());
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node.getNodeValue() != null) {
        strings.add(node.getNodeValue().trim());
      } else {
        Node fchild = node.getFirstChild();
        if (fchild != null && fchild.getNodeValue() != null) {
          strings.add(fchild.getNodeValue().trim());
        } else {
          strings.add("");
        }
      }

    }
    return strings;
  }

  public static Object xpath(String document, String path, QName returnType) throws Exception {
    return xpath(parseXml(IOUtils.toInputStream(document, "UTF-8")), path, returnType);
  }

  public static Object xpath(InputStream is, String path, QName returnType) throws XPathExpressionException {
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();
      return xPath.compile(path).evaluate(is, returnType);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  public static Object xpath(Document document, String path, QName returnType) throws XPathExpressionException,
          TransformerException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    xPath.setNamespaceContext(new UniversalNamespaceResolver(document));
    return xPath.compile(path).evaluate(document, returnType);
  }

  public static Boolean xpathExists(Document document, String path) throws Exception {
    return (Boolean) xpath(document, path, XPathConstants.BOOLEAN);
  }

  public static JSONObject parseJson(String doc) throws Exception {
    return (JSONObject) JSONValue.parse(doc);
  }

  public static String schedulerEvent(Integer duration, String title, String id) throws Exception {
    Long start = System.currentTimeMillis() + 60000;
    Long end = start + duration;
    InputStream is = null;
    try {
      is = Utils.class.getResourceAsStream("/scheduler-event.xml");
      String event = IOUtils.toString(is, "UTF-8");
      return event.replace("@@id@@", id).replace("@@title@@", title).replace("@@start@@", start.toString())
              .replace("@@end@@", end.toString()).replace("@@duration@@", duration.toString());
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  public static File getUrlAsFile(String url) throws IOException {
    HttpGet get = new HttpGet(url);
    HttpResponse response = null;
    FileOutputStream out = null;
    TrustedHttpClient client = null;
    try {
      client = Main.getClient();
      response = client.execute(get);
      File f = File.createTempFile("testfile", ".tmp");
      out = new FileOutputStream(f);
      IOUtils.copy(response.getEntity().getContent(), out);
      return f;
    } finally {
      Main.returnClient(client);
    }
  }

  public static Document getUrlAsDocument(String url) throws Exception {
    TrustedHttpClient client = Main.getClient();
    HttpGet get = new HttpGet(url);
    HttpResponse response = null;
    try {
      response = client.execute(get);
      return parseXml(response.getEntity().getContent());
    } finally {
      Main.returnClient(client);
    }
  }

  public static final String md5(File f) throws IOException {
    byte[] bytes = new byte[1024];
    InputStream is = null;
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      is = new DigestInputStream(new FileInputStream(f), md);
      while ((is.read(bytes)) >= 0) {
      }
      return hex(md.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("No MD5 algorithm available");
    } finally {
      is.close();
    }
  }

  /**
   * Converts the checksum to a hex string.
   *
   * @param data
   *          the digest
   * @return the digest hex representation
   */
  private static String hex(byte[] data) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < data.length; i++) {
      int halfbyte = (data[i] >>> 4) & 0x0F;
      int two_halfs = 0;
      do {
        if ((0 <= halfbyte) && (halfbyte <= 9))
          buf.append((char) ('0' + halfbyte));
        else
          buf.append((char) ('a' + (halfbyte - 10)));
        halfbyte = data[i] & 0x0F;
      } while (two_halfs++ < 1);
    }
    return buf.toString();
  }

}
