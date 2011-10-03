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

import org.opencastproject.feed.api.Content;

/**
 * TODO: Comment me
 */
public class ContentImpl implements Content {

  /** The content type */
  protected String type = null;

  /** Content value */
  protected String value = null;

  /** Content mode */
  protected Mode mode = null;

  /**
   * Creates a new content element with only the value set.
   * 
   * @param value
   *          the content value
   */
  public ContentImpl(String value) {
    this(value, null, Mode.Escaped);
  }

  /**
   * Creates a new content element
   * 
   * @param value
   *          the content value
   * @param type
   *          the content type
   * @param mode
   *          the content mode
   */
  public ContentImpl(String value, String type, Mode mode) {
    this.value = value;
    this.type = type;
    this.mode = mode;
  }

  /**
   * @see org.opencastproject.feed.api.Content#getMode()
   */
  public Mode getMode() {
    return mode;
  }

  /**
   * @see org.opencastproject.feed.api.Content#getType()
   */
  public String getType() {
    return type;
  }

  /**
   * @see org.opencastproject.feed.api.Content#getValue()
   */
  public String getValue() {
    return value;
  }

  /**
   * @see org.opencastproject.feed.api.Content#setMode(Content.Mode)
   */
  public void setMode(Mode mode) {
    this.mode = mode;
  }

  /**
   * @see org.opencastproject.feed.api.Content#setType(java.lang.String)
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @see org.opencastproject.feed.api.Content#setValue(java.lang.String)
   */
  public void setValue(String value) {
    this.value = value;
  }

}
