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

import org.opencastproject.feed.api.Category;

/**
 * TODO: Comment me
 */
public class CategoryImpl implements Category {

  /** The category name */
  private String name = null;

  /** The taxonomy uri */
  private String taxonomyUri = null;

  /**
   * Creates a new category.
   *
   * @param name
   *          the name
   * @param taxonomyUri
   *          the uri
   */
  public CategoryImpl(String name, String taxonomyUri) {
    this.name = name;
    this.taxonomyUri = taxonomyUri;
  }

  /**
   * @see org.opencastproject.feed.api.Category#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * @see org.opencastproject.feed.api.Category#getTaxonomyUri()
   */
  public String getTaxonomyUri() {
    return taxonomyUri;
  }

  /**
   * @see org.opencastproject.feed.api.Category#setName(java.lang.String)
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @see org.opencastproject.feed.api.Category#setTaxonomyUri(java.lang.String)
   */
  public void setTaxonomyUri(String taxonomyUri) {
    this.taxonomyUri = taxonomyUri;
  }

}
