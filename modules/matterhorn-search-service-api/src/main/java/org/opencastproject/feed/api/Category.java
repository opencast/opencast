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
 * Categories are used in conjunction with the dublin core module.
 * <p>
 * Note that this interface is heavily inspired and backed by the excellent rss/atom feed library <tt>Rome</tt>
 * (http://https://rome.dev.java.net).
 */
public interface Category {

  /**
   * Returns the category name.
   * 
   * @return the category name, <b>null</b> if none.
   */
  String getName();

  /**
   * Sets the category name.
   * 
   * @param name
   *          the category name to set, <b>null</b> if none.
   */
  void setName(String name);

  /**
   * Returns the category taxonomy URI.
   * 
   * @return the category taxonomy URI, <b>null</b> if none.
   */
  String getTaxonomyUri();

  /**
   * Sets the category taxonomy URI.
   * 
   * @param taxonomyUri
   *          the category taxonomy URI to set, <b>null</b> if none.
   */
  void setTaxonomyUri(String taxonomyUri);

}
