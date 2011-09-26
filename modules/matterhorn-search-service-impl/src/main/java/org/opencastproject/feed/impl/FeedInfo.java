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
    this.type = type;
    this.version = version;
    this.query = query;

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
