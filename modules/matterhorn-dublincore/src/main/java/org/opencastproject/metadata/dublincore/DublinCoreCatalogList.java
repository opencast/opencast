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
  private List<DublinCoreCatalog> catalogList;
  private int totalCatalogCount = 1;

  /** Default no-arg constructor initialized with an empty catalog list */
  public DublinCoreCatalogList() {
    catalogList = new LinkedList<DublinCoreCatalog>();
  }

  /**
   * Initialize with the given catalog list.
   * 
   * @param catalogs
   *          the catalogs to initialize this list with.
   */
  public DublinCoreCatalogList(List<DublinCoreCatalog> catalogs) {
    this();
    catalogList.addAll(catalogs);
    totalCatalogCount = catalogs.size();
  }

  /**
   * Returns list of DUblin Core currently stored
   * 
   * @return List of {@link DublinCoreCatalog}s
   */
  public List<DublinCoreCatalog> getCatalogList() {
    if (catalogList == null) {
      return new LinkedList<DublinCoreCatalog>();
    }
    return new LinkedList<DublinCoreCatalog>(catalogList);
  }

  /**
   * Sets Dublin core catalog list.
   * 
   * @param catalogList
   *          List of {@link DublinCoreCatalog}s
   */
  public void setCatalogList(List<DublinCoreCatalog> catalogList) {
    this.catalogList = catalogList;
    totalCatalogCount = catalogList.size();
  }

  /**
   * Adds one Dublin core to the list.
   * 
   * @param catalog
   *          {@link DublinCoreCatalog} to be added
   */
  public void addCatalog(DublinCoreCatalog catalog) {
    if (catalog != null) {
      catalogList.add(catalog);
      totalCatalogCount = totalCatalogCount + 1;
    }
  }
  
  /**
   * Get the total number of catalogs 
   * 
   * @return int totalCatalogCount
   */
  
  public int getCatalogCount() {
    return totalCatalogCount;
  }
  
  /**
   * Set the total number of catalogs
   * 
   * @param total
   */
  public void setCatalogCount(int total) {
    totalCatalogCount = total;
  }

  /**
   * Serializes list to XML.
   * 
   * @return serialized array as XML string
   * @throws IOException
   *           if serialization cannot be properly performed
   */
  public String getResultsAsXML() throws IOException {
    if (catalogList == null)
      catalogList = new LinkedList<DublinCoreCatalog>();

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
    if (catalogList == null) {
      catalogList = new LinkedList<DublinCoreCatalog>();
    }

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
