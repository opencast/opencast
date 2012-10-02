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
package org.opencastproject.episode.api;

import org.apache.commons.io.IOUtils;
import org.opencastproject.util.data.functions.Misc;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.opencastproject.util.data.Monadics.mlist;

/**
 * The search result represents a set of result items that has been compiled as a result for a search operation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "search-results", namespace = "http://search.opencastproject.org", propOrder = { "query", "resultSet" })
@XmlRootElement(name = "search-results", namespace = "http://search.opencastproject.org")
public class JaxbSearchResult implements SearchResult {

  /** Context for serializing and deserializing */
  private static final JAXBContext context;

  static {
    try {
      context = JAXBContext.newInstance("org.opencastproject.episode.api", JaxbSearchResult.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads the search result from the input stream.
   * 
   * @param xml
   *          the input stream
   * @return the deserialized search result
   */
  public static JaxbSearchResult valueOf(InputStream xml) {
    try {
      Unmarshaller unmarshaller = context.createUnmarshaller();
      Source source = new StreamSource(xml);
      return unmarshaller.unmarshal(source, JaxbSearchResult.class).getValue();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    } finally {
      IOUtils.closeQuietly(xml);
    }
  }
  
  /** A list of search items. */
  @XmlElement(name = "result")
  private List<JaxbSearchResultItem> resultSet = null;

  /** The query that yielded the result set */
  @XmlElement(name = "query")
  private String query = null;

  /** The pagination offset. */
  @XmlAttribute
  private long offset = 0;

  /** The pagination limit. Default is 10. */
  @XmlAttribute
  private long limit = 10;

  /** The number of hits total, regardless of the limit */
  @XmlAttribute
  private long total = 0;

  /** The search time in milliseconds */
  @XmlAttribute
  private long searchTime = 0;

  /**
   * A no-arg constructor needed by JAXB
   */
  public JaxbSearchResult() {
    this.resultSet = new ArrayList<JaxbSearchResultItem>();
  }

  /**
   * Creates a new and empty search result.
   * 
   * @param query
   *          the query
   */
  public JaxbSearchResult(String query) {
    this();
    if (query == null)
      throw new IllegalArgumentException("Query cannot be null");
    this.query = query;
  }

  @Override
  public List<SearchResultItem> getItems() {
    return mlist(resultSet).map(Misc.<JaxbSearchResultItem, SearchResultItem>cast()).value();
  }

  /**
   * Adds an item to the result set.
   * 
   * @param item
   *          the item to add
   */
  public void addItem(JaxbSearchResultItem item) {
    if (item == null)
      throw new IllegalArgumentException("Parameter item cannot be null");
    resultSet.add(item);
  }

  public void setItems(List<JaxbSearchResultItem> items) {
    resultSet = items;
  }

  @Override
  public String getQuery() {
    return query;
  }

  @Override
  public long size() {
    return resultSet != null ? resultSet.size() : 0;
  }

  @Override
  public long getOffset() {
    return offset;
  }

  /**
   * Set the offset.
   * 
   * @param offset
   *          The offset.
   */
  public void setOffset(long offset) {
    this.offset = offset;
  }

  @Override
  public long getLimit() {
    return limit;
  }

  /**
   * Set the limit.
   * 
   * @param limit
   *          The limit.
   */
  public void setLimit(long limit) {
    this.limit = limit;
  }

  @Override
  public long getSearchTime() {
    return searchTime;
  }

  /**
   * Set the search time.
   * 
   * @param searchTime
   *          The time in ms.
   */
  public void setSearchTime(long searchTime) {
    this.searchTime = searchTime;
  }

  /**
   * Sets the total hit count.
   * 
   * @param total
   *          the total hit count
   */
  public void setTotalSize(long total) {
    this.total = total;
  }

  @Override
  public long getTotalSize() {
    return total;
  }

  @Override
  public long getPage() {
    if (limit != 0)
      return offset / limit;
    return 0;
  }
}
