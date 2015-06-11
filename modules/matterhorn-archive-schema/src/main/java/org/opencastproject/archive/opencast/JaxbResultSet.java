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

package org.opencastproject.archive.opencast;

import org.opencastproject.util.data.Function;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.data.Monadics.mlist;

/** The search result represents a set of result items that has been compiled as a result for a search operation. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "search-results", namespace = "http://archive.opencastproject.org", propOrder = {"query", "resultSet"})
@XmlRootElement(name = "search-results", namespace = "http://archive.opencastproject.org")
public class JaxbResultSet {
  /** Context for serializing and deserializing */
  private static final JAXBContext context;

  static {
    try {
      context = JAXBContext.newInstance("org.opencastproject.archive.opencast", JaxbResultSet.class.getClassLoader());
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Reads the search result from the input stream.
   *
   * @param in
   *         the input stream
   * @return the deserialized result set
   */
  public static JaxbResultSet valueOf(InputStream in) {
    return withResource(in, new Function.X<InputStream, JaxbResultSet>() {
      @Override public JaxbResultSet xapply(InputStream in) throws Exception {
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        return unmarshaller.unmarshal(new StreamSource(in), JaxbResultSet.class).getValue();
      }
    });
  }

  /** A list of search items. */
  @XmlElement(name = "result")
  protected List<JaxbResultItem> resultSet = new ArrayList<JaxbResultItem>();

  /** The query that yielded the result set */
  @XmlElement(name = "query")
  protected String query = null;

  /** The pagination offset. */
  @XmlAttribute
  protected long offset = 0;

  /** The pagination limit. Default is 10. */
  @XmlAttribute
  protected long limit = 10;

  /** The number of hits total, regardless of the limit */
  @XmlAttribute
  protected long total = 0;

  /** The search time in milliseconds */
  @XmlAttribute
  protected long searchTime = 0;

  /** A no-arg constructor needed by JAXB. */
  public JaxbResultSet() {
  }

  public static JaxbResultSet create(final OpencastResultSet source) {
    // completeness ensured by unit test
    final JaxbResultSet target = new JaxbResultSet();
    target.resultSet = mlist(source.getItems()).map(JaxbResultItem.create).value();
    target.query = source.getQuery();
    target.total = source.getTotalSize();
    target.limit = source.getLimit();
    target.offset = source.getOffset();
    target.searchTime = source.getSearchTime();
    return target;
  }
}
