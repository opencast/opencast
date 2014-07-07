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

/**
 * Implementation of a plain text content container.
 */
public class PlainTextContent extends ContentImpl {

  /** The plain text type */
  private static final String TYPE_PLAIN = "text/plain";

  /**
   * Creates new plain text content.
   *
   * @param value
   *          the content value
   */
  public PlainTextContent(String value) {
    this(value, Mode.Escaped);
  }

  /**
   * @param value
   *          the content value
   * @param mode
   *          the content mode
   */
  public PlainTextContent(String value, Mode mode) {
    super(value, TYPE_PLAIN, mode);
  }

}
