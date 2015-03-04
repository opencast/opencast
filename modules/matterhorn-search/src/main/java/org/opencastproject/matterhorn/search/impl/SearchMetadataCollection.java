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

package org.opencastproject.matterhorn.search.impl;

import org.opencastproject.matterhorn.search.Language;
import org.opencastproject.matterhorn.search.SearchMetadata;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wrapper that facilitates in posting business objects to the search index.
 * <p>
 * This implementation provides utility methods that will ease handling of objects such as dates or users and help
 * prevent posting of <code>null</code> values.
 */
public class SearchMetadataCollection implements Collection<SearchMetadata<?>> {

  /** The metadata */
  protected Map<String, SearchMetadata<?>> metadata = new HashMap<String, SearchMetadata<?>>();

  /** Returns the document identifier */
  protected String identifier = null;

  /** Returns the document type */
  protected String documentType = null;

  /**
   * Creates a new resource metadata collection for the given document type.
   * <p>
   * Make sure to set the identifier after the fact using {@link #setIdentifier(String)}.
   * 
   * @param documentType
   *          the document type
   */
  public SearchMetadataCollection(String documentType) {
    this(null, documentType);
  }

  /**
   * Creates a new resource metadata collection for the given document type.
   * 
   * @param identifier
   *          the document identifier
   * @param documentType
   *          the document type
   */
  public SearchMetadataCollection(String identifier, String documentType) {
    this.identifier = identifier;
    this.documentType = documentType;
  }

  /**
   * Sets the document identifier.
   * 
   * @param identifier
   *          the identifier
   */
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  /**
   * Returns the document identifier.
   * 
   * @return the identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Returns the document type that determines where the document is posted to the index.
   * 
   * @return the document type
   */
  public String getDocumentType() {
    return documentType;
  }

  /**
   * Adds the field and its value to the search index.
   * 
   * @param fieldName
   *          the field name
   * @param fieldValue
   *          the value
   * @param addToText
   *          <code>true</code> to add the contents to the fulltext field as well
   */
  @SuppressWarnings("unchecked")
  public void addField(String fieldName, Object fieldValue, boolean addToText) {
    if (fieldName == null)
      throw new IllegalArgumentException("Field name cannot be null");
    if (fieldValue == null)
      return;

    SearchMetadata<Object> m = (SearchMetadata<Object>) metadata.get(fieldName);
    if (m == null) {
      m = new SearchMetadataImpl<Object>(fieldName);
      metadata.put(fieldName, m);
    }

    m.setAddToText(addToText);

    if (fieldValue.getClass().isArray()) {
      Object[] fieldValues = (Object[]) fieldValue;
      for (Object v : fieldValues) {
        m.addValue(v);
      }
    } else {
      m.addValue(fieldValue);
    }
  }

  /**
   * Returns the localized field name, which is the original field name extended by an underscore and the language
   * identifier.
   * 
   * @param fieldName
   *          the field name
   * @param language
   *          the language
   * @return the localized field name
   */
  protected String getLocalizedFieldName(String fieldName, Language language) {
    return MessageFormat.format(fieldName, language.getIdentifier());
  }

  /**
   * Returns the metadata as a list of {@link SearchMetadata} items.
   * 
   * @return the metadata items
   */
  public List<SearchMetadata<?>> getMetadata() {
    List<SearchMetadata<?>> result = new ArrayList<SearchMetadata<?>>();
    result.addAll(metadata.values());
    return result;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#add(java.lang.Object)
   */
  public boolean add(SearchMetadata<?> e) {
    return metadata.put(e.getName(), e) != null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#addAll(java.util.Collection)
   */
  public boolean addAll(Collection<? extends SearchMetadata<?>> c) {
    for (SearchMetadata<?> m : c) {
      metadata.put(m.getName(), m);
    }
    return true;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#clear()
   */
  public void clear() {
    metadata.clear();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#contains(java.lang.Object)
   */
  public boolean contains(Object o) {
    return metadata.values().contains(o);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#containsAll(java.util.Collection)
   */
  public boolean containsAll(Collection<?> c) {
    for (Object o : c) {
      if (!metadata.values().contains(o))
        return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#isEmpty()
   */
  public boolean isEmpty() {
    return metadata.isEmpty();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#iterator()
   */
  public Iterator<SearchMetadata<?>> iterator() {
    List<SearchMetadata<?>> result = new ArrayList<SearchMetadata<?>>();
    result.addAll(metadata.values());
    return result.iterator();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#remove(java.lang.Object)
   */
  public boolean remove(Object o) {
    return metadata.remove(o) != null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#removeAll(java.util.Collection)
   */
  public boolean removeAll(Collection<?> c) {
    boolean removed = false;
    for (Object o : c)
      removed |= metadata.remove(o) != null;
    return removed;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#retainAll(java.util.Collection)
   */
  public boolean retainAll(Collection<?> c) {
    boolean removed = false;
    for (SearchMetadata<?> m : metadata.values()) {
      if (!c.contains(m)) {
        metadata.remove(m);
        removed = true;
      }
    }
    return removed;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#size()
   */
  public int size() {
    return metadata.size();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Collection#toArray()
   */
  public Object[] toArray() {
    return metadata.values().toArray();
  }

  /**
   * Returns the metadata keys and the metadata items as a map for convenient access of search metadata by key.
   * 
   * @return the map
   */
  public Map<String, SearchMetadata<?>> toMap() {
    return metadata;
  }

  /**
   * @see java.util.Collection#toArray(java.lang.Object[])
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] arg0) {
    return (T[]) metadata.values().toArray(new SearchMetadataImpl[metadata.size()]);
  }

}
