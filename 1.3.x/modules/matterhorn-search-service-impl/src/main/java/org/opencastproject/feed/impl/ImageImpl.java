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

import org.opencastproject.feed.api.Image;

/**
 * TODO: Comment me
 */
public class ImageImpl implements Image {

  /** Image title */
  private String title = null;

  /** Image url */
  private String url = null;

  /** Link behind the image */
  private String link = null;

  /** Image description */
  private String description = null;

  /**
   * Creates a new image.
   * 
   * @param url
   *          the image url
   */
  public ImageImpl(String url) {
    this(url, null, null, null);
  }

  /**
   * Creates a new image.
   * 
   * @param url
   *          the image url
   * @param title
   *          the title
   */
  public ImageImpl(String url, String title) {
    this(url, title, null, null);
  }

  /**
   * Creates a new image.
   * 
   * @param url
   *          the image url
   * @param title
   *          the title
   * @param description
   *          the description
   */
  public ImageImpl(String url, String title, String description) {
    this(url, title, description, null);
  }

  /**
   * Creates a new image.
   * 
   * @param url
   *          the image url
   * @param title
   *          the title
   * @param description
   *          the description
   * @param link
   *          link behind the image
   */
  public ImageImpl(String url, String title, String description, String link) {
    this.url = url;
    this.title = title;
    this.description = description;
    this.link = link;
  }

  /**
   * @see org.opencastproject.feed.api.Image#getDescription()
   */
  public String getDescription() {
    return description;
  }

  /**
   * @see org.opencastproject.feed.api.Image#getLink()
   */
  public String getLink() {
    return link;
  }

  /**
   * @see org.opencastproject.feed.api.Image#getTitle()
   */
  public String getTitle() {
    return title;
  }

  /**
   * @see org.opencastproject.feed.api.Image#getUrl()
   */
  public String getUrl() {
    return url;
  }

  /**
   * @see org.opencastproject.feed.api.Image#setDescription(java.lang.String)
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @see org.opencastproject.feed.api.Image#setLink(java.lang.String)
   */
  public void setLink(String link) {
    this.link = link;
  }

  /**
   * @see org.opencastproject.feed.api.Image#setTitle(java.lang.String)
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @see org.opencastproject.feed.api.Image#setUrl(java.lang.String)
   */
  public void setUrl(String url) {
    this.url = url;
  }

}
