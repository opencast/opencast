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

import java.util.List;
import java.util.Map;

/**
 * Resource metadata models a piece of metadata that describes one aspect of a
 * resource, e. g. the title.
 */
public interface SearchMetadata<T> {

  /**
   * Returns the name of this metadata item.
   * 
   * @return the name
   */
  String getName();

  /**
   * Adds <code>value</code> to the list of language neutral values.
   * 
   * @param language
   *          the language
   * @param v
   *          the value
   */
  void addLocalizedValue(Language language, T v);

  /**
   * Returns the values for this metadata item, mapped to their respective
   * languages.
   * 
   * @return the localized values
   */
  Map<Language, List<T>> getLocalizedValues();

  /**
   * Returns <code>true</code> if this metadata item has been localized.
   * 
   * @return <code>true</code> if the metadata item is localized
   */
  boolean isLocalized();

  /**
   * Adds <code>value</code> to the list of language neutral values.
   * 
   * @param v
   *          the value
   */
  void addValue(T v);

  /**
   * Returns a list of all all non-localized values. In order to retrieve
   * localized values for this metadata field, use {@link #getLocalizedValues()}
   * .
   * 
   * @return the list of language neutral values
   */
  List<T> getValues();

  /**
   * Returns the first value of the available values or <code>null</code> if no
   * value is available.
   * 
   * @return the first value
   * @see #getValues()
   */
  T getValue();

  /**
   * Removes all values currently in the metadata container.
   */
  void clear();

  /**
   * Adds the metadata values to the user facing fulltext index.
   * 
   * @param addToFulltext
   *          <code>true</code> to add the values to the fulltext index
   */
  void setAddToText(boolean addToFulltext);

  /**
   * Returns <code>true</code> if the values should be added to the user facing
   * fulltext search index.
   * 
   * @return <code>true</code> if the metadata values should be added to the
   *         fulltext index
   */
  boolean addToText();

}
