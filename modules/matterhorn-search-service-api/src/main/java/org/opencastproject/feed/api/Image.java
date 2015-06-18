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


package org.opencastproject.feed.api;

/**
 * Images are used to add visual content to feeds and feed entries.
 * <p>
 * Note that this interface is heavily inspired and backed by the excellent rss/atom feed library <tt>Rome</tt>
 * (http://https://rome.dev.java.net).
 */
public interface Image {

  /**
   * Returns the image title.
   *
   * @return the image title, <b>null</b> if none
   */
  String getTitle();

  /**
   * Sets the image title.
   *
   * @param title
   *          the image title to set, <b>null</b> if none
   */
  void setTitle(String title);

  /**
   * Returns the image URL.
   *
   * @return the image URL, <b>null</b> if none
   */
  String getUrl();

  /**
   * Sets the image URL.
   *
   * @param url
   *          the image URL to set, <b>null</b> if none
   */
  void setUrl(String url);

  /**
   * Returns the image link.
   *
   * @return the image link, <b>null</b> if none
   */
  String getLink();

  /**
   * Sets the image link.
   *
   * @param link
   *          the image link to set, <b>null</b> if none
   */
  void setLink(String link);

  /**
   * Returns the image description.
   *
   * @return the image description, <b>null</b> if none
   */
  String getDescription();

  /**
   * Sets the image description.
   *
   * @param description
   *          the image description to set, <b>null</b> if none
   */
  void setDescription(String description);

}
