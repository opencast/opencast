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


package org.opencastproject.matterhorn.search.impl;

import org.opencastproject.matterhorn.search.Language;
import org.opencastproject.matterhorn.search.SearchMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation for the {@link SearchMetadata}.
 */
public class SearchMetadataImpl<T> implements SearchMetadata<T> {

  /** The name of this metadata item */
  protected String name = null;

  /** Values */
  protected List<T> values = new ArrayList<T>();

  /** Localized values */
  protected Map<Language, List<T>> localizedValues = new HashMap<Language, List<T>>();

  /** True to add the values to the fulltext index */
  protected boolean addToText = false;

  /**
   * Creates a new metadata object with the given name and values.
   * 
   * @param name
   *          the metadata name
   * @param values
   *          the language neutral values
   * @param localizedValues
   *          the localized values
   * @param addToText
   *          <code>true</code> to add these items to the user facing fulltext index
   */
  public SearchMetadataImpl(String name, List<T> values, Map<Language, List<T>> localizedValues, boolean addToText) {
    this.name = name;
    this.values = values;
    this.localizedValues = localizedValues;
    this.addToText = addToText;
  }

  /**
   * Creates a new metadata item with the given name.
   * 
   * @param name
   *          the name
   */
  public SearchMetadataImpl(String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchMetadata#getName()
   */
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchMetadata#isLocalized()
   */
  public boolean isLocalized() {
    return localizedValues != null && localizedValues.size() > 0;
  }

  /**
   * Adds <code>value</code> to the list of language neutral values.
   * 
   * @param language
   *          the language
   * @param v
   *          the value
   */
  public void addLocalizedValue(Language language, T v) {
    if (localizedValues == null)
      localizedValues = new HashMap<Language, List<T>>();
    List<T> values = localizedValues.get(language);
    if (values == null)
      values = new ArrayList<T>();
    if (!values.contains(v))
      values.add(v);
    localizedValues.put(language, values);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchMetadata#getLocalizedValues()
   */
  public Map<Language, List<T>> getLocalizedValues() {
    if (localizedValues == null)
      return Collections.emptyMap();
    return localizedValues;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchMetadata#addValue(java.lang.Object)
   */
  public void addValue(T v) {
    if (values == null)
      values = new ArrayList<T>();
    if (!values.contains(v))
      values.add(v);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchMetadata#getValues()
   */
  public List<T> getValues() {
    if (values == null)
      return Collections.emptyList();
    return values;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchMetadata#getValue()
   */
  @Override
  public T getValue() {
    if (values == null || values.size() == 0)
      return null;
    return values.get(0);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchMetadata#setAddToText(boolean)
   */
  public void setAddToText(boolean addToText) {
    this.addToText = addToText;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchMetadata#addToFulltext()
   */
  public boolean addToText() {
    return addToText;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.matterhorn.search.SearchMetadata#clear()
   */
  public void clear() {
    if (values != null)
      values.clear();
    if (localizedValues != null)
      localizedValues.clear();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SearchMetadata<?>))
      return false;
    return name.equals(((SearchMetadata<?>) obj).getName());
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return name;
  }

}
