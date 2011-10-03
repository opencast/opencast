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

package org.opencastproject.feed.api;

/**
 * An enclose is an optional part of a feed entry.
 * <p>
 * Note that this interface is heavily inspired and backed by the excellent rss/atom feed library <tt>Rome</tt>
 * (http://https://rome.dev.java.net).
 */
public interface Enclosure {

  /**
   * Returns the enclosure URL.
   * 
   * @return the enclosure URL, <b>null</b> if none
   */
  String getUrl();

  /**
   * Sets the enclosure URL.
   * 
   * @param url
   *          the enclosure URL to set, <b>null</b> if none
   */
  void setUrl(String url);

  /**
   * Returns the enclosure length.
   * 
   * @return the enclosure length, <b>null</b> if none
   */
  long getLength();

  /**
   * Sets the enclosure length.
   * 
   * @param length
   *          the enclosure length to set, <b>null</b> if none
   */
  void setLength(long length);

  /**
   * Returns the enclosure type.
   * 
   * @return the enclosure type, <b>null</b> if none
   */
  String getType();

  /**
   * Sets the enclosure type.
   * 
   * @param type
   *          the enclosure type to set, <b>null</b> if none
   */
  void setType(String type);

}
