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

package org.opencastproject.matterhorn.search;

/**
 * The search result is what is delivered by the search function.
 */
public interface SearchResultItem<T extends Object> extends Comparable<SearchResultItem<T>> {

  /**
   * Returns the relevance of this hit with respect to the search terms. Greater values mean increased relevance, a 1.0
   * signifies a direct hit while 0.0 means a very unlikely hit.
   * 
   * @return the relevance
   */
  double getRelevance();

  /**
   * Returns the source of this search result. This will usually be the site or a site's module.
   * 
   * @return the source
   */
  T getSource();

}
