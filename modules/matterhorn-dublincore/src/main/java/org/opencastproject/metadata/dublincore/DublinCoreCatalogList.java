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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Simple class that enables storage of {@link DublinCoreCatalog} list and serializing into xml or json string.
 * 
 */
public class DublinCoreCatalogList {

  /** Array storing Dublin cores */
  private List<DublinCoreCatalog> catalogList = new LinkedList<DublinCoreCatalog>();
  private long totalCatalogCount = 0;

  /**
   * Initialize with the given catalog list.
   * 
   * @param catalogs
   *          the catalogs to initialize this list with.
   * @param totalCount
   *          the total count of catalogs that match the creating query of this list
   *          must be &gt;= catalogs.size
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
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      DOMImplementation impl = builder.getDOMImplementation();

      Document doc = impl.createDocument(null, null, null);
      Element root = doc.createElement("dublincorelist");
      root.setAttribute("totalCount", String.valueOf(totalCatalogCount));
      doc.appendChild(root);
      for (DublinCoreCatalog series : catalogList) {
        Node node = doc.importNode(series.toXml().getDocumentElement(), true);
        root.appendChild(node);
      }

      Transformer tf = TransformerFactory.newInstance().newTransformer();
      DOMSource xmlSource = new DOMSource(doc);
      StringWriter out = new StringWriter();
      tf.transform(xmlSource, new StreamResult(out));
      return out.toString();
    } catch (Exception e) {
      throw new IOException(e);
    }
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
      jsonArray.add(((DublinCoreCatalogImpl) catalog).toJsonObject());
    }
    jsonObj.put("totalCount", String.valueOf(totalCatalogCount));
    jsonObj.put("catalogs", jsonArray);
    return jsonObj.toJSONString();
  }
}
