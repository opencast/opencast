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

import org.opencastproject.feed.api.Link;

/**
 * TODO: Comment me
 */
public class LinkImpl implements Link {

  private String href = null;

  private String rel = null;

  private String type = null;

  private String hreflang = null;

  private String title = null;

  private String flavor = null;

  private long length = -1;

  /**
   * Creates a link with the given <code>href</code> attribute.
   *
   * @param href
   *          the link
   */
  public LinkImpl(String href) {
    this.href = href;
  }

  /**
   * @see org.opencastproject.feed.api.Link#getHref()
   */
  public String getHref() {
    return href;
  }

  /**
   * @see org.opencastproject.feed.api.Link#getHreflang()
   */
  public String getHreflang() {
    return hreflang;
  }

  /**
   * @see org.opencastproject.feed.api.Link#getLength()
   */
  public long getLength() {
    return length;
  }

  /**
   * @see org.opencastproject.feed.api.Link#getRel()
   */
  public String getRel() {
    return rel;
  }

  /**
   * @see org.opencastproject.feed.api.Link#getTitle()
   */
  public String getTitle() {
    return title;
  }

  /**
   * @see org.opencastproject.feed.api.Link#getType()
   */
  public String getType() {
    return type;
  }

  /**
   * @see org.opencastproject.feed.api.Link#setHref(java.lang.String)
   */
  public void setHref(String href) {
    this.href = href;
  }

  /**
   * @see org.opencastproject.feed.api.Link#setHreflang(java.lang.String)
   */
  public void setHreflang(String hreflang) {
    this.hreflang = hreflang;
  }

  /**
   * @see org.opencastproject.feed.api.Link#setLength(long)
   */
  public void setLength(long length) {
    this.length = length;
  }

  /**
   * @see org.opencastproject.feed.api.Link#setRel(java.lang.String)
   */
  public void setRel(String rel) {
    this.rel = rel;
  }

  /**
   * @see org.opencastproject.feed.api.Link#setTitle(java.lang.String)
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @see org.opencastproject.feed.api.Link#setType(java.lang.String)
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @see org.opencastproject.feed.api.Link#getFlavor()
   */
  public String getFlavor() {
    return flavor;
  }

  /**
   * @see org.opencastproject.feed.api.Link#setFlavor(java.lang.String)
   */
  public void setFlavor(String flavor) {
    this.flavor = flavor;
  }


}
