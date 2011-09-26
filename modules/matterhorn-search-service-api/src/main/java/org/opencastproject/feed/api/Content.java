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
 * Models a content entry, used for descriptions and title elements in feeds and feed entries.
 * <p>
 * Note that this interface is heavily inspired and backed by the excellent rss/atom feed library <tt>Rome</tt>
 * (http://https://rome.dev.java.net).
 */
public interface Content {

  /** The content mode */
  public enum Mode {
    Escaped, Xml
  };

  /**
   * Returns the content type.
   * <p>
   * When used for the description of an entry, if <b>null</b> 'text/plain' must be assumed.
   * 
   * @return the content type, <b>null</b> if none.
   */
  String getType();

  /**
   * Sets the content type.
   * <p>
   * When used for the description of an entry, if <b>null</b> 'text/plain' must be assumed.
   * 
   * @param type
   *          the content type to set, <b>null</b> if none.
   */
  void setType(String type);

  /**
   * Gets the content mode (needed for Atom 0.3 support).
   * 
   * @return type the content, <b>null</b> if none.
   */
  Mode getMode();

  /**
   * Sets the content mode (needed for Atom 0.3 support).
   * 
   * @param mode
   *          the content mode to set, <b>null</b> if none.
   */
  void setMode(Mode mode);

  /**
   * Returns the content value.
   * 
   * @return the content value, <b>null</b> if none.
   */
  String getValue();

  /**
   * Sets the content value.
   * 
   * @param value
   *          the content value to set, <b>null</b> if none.
   */
  void setValue(String value);

}
