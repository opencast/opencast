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


package org.opencastproject.feed.impl;

import org.opencastproject.feed.api.Enclosure;

/**
 * TODO: Comment me
 */
public class EnclosureImpl implements Enclosure {

  /** Url to the enclosure */
  private String url = null;

  /** Enclosure type */
  private String type = null;

  /** Enclosure flavor */
  private String flavor = null;

  /** Enclosure length */
  private long length = 0;

  /**
   * Creates a new enclosure.
   *
   * @param url
   *          the enclosure's url
   * @param type
   *          the type
   * @param flavor
   *          the mediapackage element flavor
   * @param length
   *          the lenght
   */
  public EnclosureImpl(String url, String type, String flavor, long length) {
    this.url = url;
    this.type = type;
    this.flavor = flavor;
    this.length = length;
  }

  /**
   * @see org.opencastproject.feed.api.Enclosure#getLength()
   */
  public long getLength() {
    return length;
  }

  /**
   * @see org.opencastproject.feed.api.Enclosure#getType()
   */
  public String getType() {
    return type;
  }

  /**
   * @see org.opencastproject.feed.api.Enclosure#getUrl()
   */
  public String getUrl() {
    return url;
  }

  /**
   * @see org.opencastproject.feed.api.Enclosure#setLength(long)
   */
  public void setLength(long length) {
    this.length = length;
  }

  /**
   * @see org.opencastproject.feed.api.Enclosure#setType(java.lang.String)
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @see org.opencastproject.feed.api.Enclosure#setUrl(java.lang.String)
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * @see org.opencastproject.feed.api.Enclosure#getFlavor()
   */
  public String getFlavor() {
    return flavor;
  }

  /**
   * @see org.opencastproject.feed.api.Enclosure#setFlavor(java.lang.String)
   */
  public void setFlavor(String flavor) {
    this.flavor = flavor;
  }

}
