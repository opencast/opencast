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

package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.Feed;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class used to transport requested feed information.
 */
public class FeedInfo {

  /** The feed type */
  private Feed.Type type = null;

  /** Feed version */
  private float version = 0.0f;

  /** Content query */
  private String[] query = null;

  /** Number of entries in feed */
  private int size = -1;

  private String romeVersion = null;

  /**
   * Creates a new feed info.
   * 
   * @param type
   *          the feed type
   * @param version
   *          the feed version
   * @param query
   *          the content query
   */
  public FeedInfo(Feed.Type type, float version, String[] query) {
    this(type, version, query, -1);
  }

  /**
   * Creates a new feed info.
   * 
   * @param type
   *          the feed type
   * @param version
   *          the feed version
   * @param query
   *          the content query
   * @param size
   *          the number of entries
   */
  public FeedInfo(Feed.Type type, float version, String[] query, int size) {
    this.type = type;
    this.version = version;
    this.query = query;
    this.size = size;

    // Use english locale to ensure the use of '.' as decimal separator.
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
    nf.setMinimumFractionDigits(1);
    nf.setMaximumFractionDigits(1);
    romeVersion = type.toString().toLowerCase() + "_" + nf.format(version);
  }

  /**
   * Returns the feed type.
   * 
   * @return the type
   */
  public Feed.Type getType() {
    return type;
  }

  /**
   * Returns the feed version.
   * 
   * @return the version
   */
  public float getVersion() {
    return version;
  }

  /**
   * Sets the maximum number of entries that should be returned by this feed.
   * 
   * @param size
   *          the maximum number of feed items
   */
  public void setSize(int size) {
    this.size = size;
  }

  /**
   * Returns the maximum number of feed entries or <code>-1</code> if no maximum size was specified.
   * 
   * @return the maximum number of entries in this feed
   */
  public int getSize() {
    return size;
  }

  /**
   * Returns the content query.
   * 
   * @return the query
   */
  public String[] getQuery() {
    return query;
  }

  public String toROMEVersion() {
    return romeVersion;
  }

}
