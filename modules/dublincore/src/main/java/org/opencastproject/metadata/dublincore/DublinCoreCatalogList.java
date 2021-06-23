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

import org.opencastproject.util.IoSupport;
import org.opencastproject.util.XmlSafeParser;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Simple class that enables storage of {@link DublinCoreCatalog} list and serializing into xml or json string.
 *
 */
public class DublinCoreCatalogList {
  /** Array storing Dublin cores */
  private List<DublinCoreCatalog> catalogList = new LinkedList<DublinCoreCatalog>();
  private long totalCatalogCount = 0;
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreCatalogList.class);

  /**
   * Initialize with the given catalog list.
   *
   * @param catalogs
   *          the catalogs to initialize this list with.
   * @param totalCount
   *          the total count of catalogs that match the creating query of this list must be &gt;= catalogs.size
   */
  public DublinCoreCatalogList(List<DublinCoreCatalog> catalogs, long totalCount) {
    if (totalCount < catalogs.size())
      throw new IllegalArgumentException("total count is less than the number of catalogs passed");
    catalogList.addAll(catalogs);
    totalCatalogCount = totalCount;
  }

  /**
   * Returns list of Dublin Core currently stored
   *
   * @return List of {@link DublinCoreCatalog}s
   */
  public List<DublinCoreCatalog> getCatalogList() {
    return new LinkedList<DublinCoreCatalog>(catalogList);
  }

  /**
   * Get the total number of catalogs matching the creating query. Is &gt;= {@link #size()}.
   *
   * @return int totalCatalogCount
   */

  public long getTotalCount() {
    return totalCatalogCount;
  }

  /**
   * Return the number of contained catalogs.
   */
  public long size() {
    return catalogList.size();
  }

  /**
   * Serializes list to XML.
   *
   * @return serialized array as XML string
   * @throws IOException
   *           if serialization cannot be properly performed
   */
  public String getResultsAsXML() throws IOException {
    try {
      DocumentBuilder builder = XmlSafeParser.newDocumentBuilderFactory().newDocumentBuilder();
      DOMImplementation impl = builder.getDOMImplementation();

      Document doc = impl.createDocument(null, null, null);
      Element root = doc.createElement("dublincorelist");
      root.setAttribute("totalCount", String.valueOf(totalCatalogCount));
      doc.appendChild(root);
      for (DublinCoreCatalog series : catalogList) {
        Node node = doc.importNode(series.toXml().getDocumentElement(), true);
        root.appendChild(node);
      }

      Transformer tf = XmlSafeParser.newTransformerFactory().newTransformer();
      DOMSource xmlSource = new DOMSource(doc);
      StringWriter out = new StringWriter();
      tf.transform(xmlSource, new StreamResult(out));
      return out.toString();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Parses an XML or JSON string to an dublin core catalog list.
   *
   * @param dcString
   *          the XML or JSON string
   * @throws IOException
   *           if there is a problem parsing the XML or JSON
   */
  public static DublinCoreCatalogList parse(String dcString) throws IOException {
    List<DublinCoreCatalog> catalogs = new ArrayList<DublinCoreCatalog>();
    if (dcString.startsWith("{")) {
      JSONObject json;
      try {
        json = (JSONObject) new JSONParser().parse(dcString);
        long totalCount = Long.parseLong((String) json.get("totalCount"));
        JSONArray catalogsArray = (JSONArray) json.get("catalogs");
        for (Object catalog : catalogsArray) {
          catalogs.add(DublinCoreJsonFormat.read((JSONObject) catalog));
        }
        return new DublinCoreCatalogList(catalogs, totalCount);
      } catch (Exception e) {
        throw new IllegalStateException("Unable to load dublin core catalog list, json parsing failed.", e);
      }
    } else {
      // XML
      InputStream is = null;
      try {
        DocumentBuilderFactory docBuilderFactory = XmlSafeParser.newDocumentBuilderFactory();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        is = IOUtils.toInputStream(dcString, "UTF-8");
        Document document = docBuilder.parse(is);
        XPath xPath = XPathFactory.newInstance().newXPath();

        Number totalCount = (Number) xPath.evaluate("/*[local-name() = 'dublincorelist']/@totalCount", document,
                XPathConstants.NUMBER);

        NodeList nodes = (NodeList) xPath
                .evaluate("//*[local-name() = 'dublincore']", document, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
          InputStream nodeIs = null;
          try {
            nodeIs = nodeToString(nodes.item(i));
            catalogs.add(DublinCoreXmlFormat.read(nodeIs));
          } finally {
            IoSupport.closeQuietly(nodeIs);
          }
        }
        return new DublinCoreCatalogList(catalogs, totalCount.longValue());
      } catch (Exception e) {
        throw new IOException(e);
      } finally {
        IoSupport.closeQuietly(is);
      }
    }
  }

  /**
   * Serialize a node to an input stream
   *
   * @param node
   *          the node to serialize
   * @return the serialized input stream
   */
  private static InputStream nodeToString(Node node) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      Transformer t = XmlSafeParser.newTransformerFactory().newTransformer();
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      t.setOutputProperty(OutputKeys.INDENT, "yes");
      t.transform(new DOMSource(node), new StreamResult(outputStream));
      return new ByteArrayInputStream(outputStream.toByteArray());
    } catch (TransformerException te) {
      logger.warn("nodeToString Transformer Exception", te);
    }
    return null;
  }

  /**
   * Serializes list to JSON array string.
   *
   * @return serialized array as json array string
   */
  @SuppressWarnings("unchecked")
  public String getResultsAsJson() {
    JSONObject jsonObj = new JSONObject();
    JSONArray jsonArray = new JSONArray();
    for (DublinCoreCatalog catalog : catalogList) {
      jsonArray.add(DublinCoreJsonFormat.writeJsonObject((DublinCoreCatalog) catalog));
    }
    jsonObj.put("totalCount", String.valueOf(totalCatalogCount));
    jsonObj.put("catalogs", jsonArray);
    return jsonObj.toJSONString();
  }
}
