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
 * Links are used to add references to external entities.
 * <p>
 * Note that this interface is heavily inspired and backed by the excellent rss/atom feed library <tt>Rome</tt>
 * (http://https://rome.dev.java.net).
 */
public interface Link {

  /**
   * Returns the link rel.
   *
   * @return the link rel, <b>null</b> if none
   */
  String getRel();

  /**
   * Sets the link rel.
   *
   * @param rel
   *          the link rel, <b>null</b> if none
   */
  void setRel(String rel);

  /**
   * Returns the link type.
   *
   * @return the link type, <b>null</b> if none
   */
  String getType();

  /**
   * Sets the link type.
   *
   * @param type
   *          the link type, <b>null</b> if none
   */
  void setType(String type);

  /**
   * Returns the link href.
   *
   * @return the link href, <b>null</b> if none
   */
  String getHref();

  /**
   * Sets the link href.
   *
   * @param href
   *          the link href, <b>null</b> if none
   */
  void setHref(String href);

  /**
   * Returns the link title.
   *
   * @return the link title, <b>null</b> if none
   */
  String getTitle();

  /**
   * Sets the link title.
   *
   * @param title
   *          the link title, <b>null</b> if none
   */
  void setTitle(String title);

  /**
   * Returns the hreflang.
   *
   * @return Returns the hreflang
   */
  String getHreflang();

  /**
   * Set the hreflang.
   *
   * @param hreflang
   *          The hreflang to set
   */
  void setHreflang(String hreflang);

  /**
   * Returns the length.
   *
   * @return Returns the length
   */
  long getLength();

  /**
   * Set the length.
   *
   * @param length
   *          The length to set
   */
  void setLength(long length);

  /**
   * Returns the link mediapackage element flavor.
   *
   * @return the link flavor, <b>null</b> if none
   */
  String getFlavor();

  /**
   * Sets the link mediapackage element flavor.
   *
   * @param flavor
   *          the link flavor to set, <b>null</b> if none
   */
  void setFlavor(String flavor);

}
