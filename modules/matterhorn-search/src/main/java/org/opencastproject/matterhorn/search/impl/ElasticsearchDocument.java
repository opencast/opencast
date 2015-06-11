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

import static org.opencastproject.matterhorn.search.impl.IndexSchema.FUZZY_FIELDNAME_EXTENSION;

import org.opencastproject.matterhorn.search.Language;
import org.opencastproject.matterhorn.search.SearchMetadata;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Document that encapsulates business objects and offers support for adding those objects to a search index.
 */
public final class ElasticsearchDocument extends HashMap<String, Object> {

  /** Serial version uid */
  private static final long serialVersionUID = 2687550418831284487L;

  /** The document identifier */
  private String id = null;

  /** The document type */
  private String type = null;

  /**
   * Creates a new elastic search document based on the id, the document type and the metadata.
   * <p>
   * Note that the type needs to map to an Elasticsearch document type mapping.
   * 
   * @param id
   *          the resource identifier.
   * @param type
   *          the document type
   * @param resource
   *          the resource metadata
   */
  public ElasticsearchDocument(String id, String type, List<SearchMetadata<?>> resource) {
    this.id = id;
    this.type = type;
    for (SearchMetadata<?> entry : resource) {
      String metadataKey = entry.getName();
      put(metadataKey, entry.getValues());

      // TODO Not sure what to use for localizedFulltextFieldName
      if (entry.addToText())
        addToFulltext(entry, IndexSchema.TEXT, IndexSchema.TEXT);
    }
  }

  /**
   * Returns the document type.
   * 
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Adds the resource metadata to the designated fulltext fields.
   * 
   * @param item
   *          the metadata item
   * @param fulltextFieldName
   *          the fulltext field name
   * @param localizedFulltextFieldName
   *          the localized fulltext field name
   */
  @SuppressWarnings("unchecked")
  private void addToFulltext(SearchMetadata<?> item, String fulltextFieldName, String localizedFulltextFieldName) {

    // Get existing fulltext entries
    Collection<String> fulltext = (Collection<String>) get(fulltextFieldName);
    if (fulltext == null) {
      fulltext = new ArrayList<String>();
      put(fulltextFieldName, fulltext);
      put(fulltextFieldName + FUZZY_FIELDNAME_EXTENSION, fulltext);
    }

    // Language neutral elements
    for (Object value : item.getValues()) {
      if (value.getClass().isArray()) {
        Object[] fieldValues = (Object[]) value;
        for (Object v : fieldValues) {
          fulltext.add(v.toString());
        }
      } else {
        fulltext.add(value.toString());
      }
    }

    // Add localized metadata values
    for (Language language : item.getLocalizedValues().keySet()) {
      // Get existing fulltext entries
      String localizedFieldName = MessageFormat.format(localizedFulltextFieldName, language.getIdentifier());
      Collection<String> localizedFulltext = (Collection<String>) get(localizedFieldName);
      if (fulltext == null) {
        fulltext = new ArrayList<String>();
        put(localizedFieldName, fulltext);
      }
      Collection<?> values = item.getLocalizedValues().get(language);
      for (Object value : values) {
        if (value.getClass().isArray()) {
          Object[] fieldValues = (Object[]) value;
          for (Object v : fieldValues) {
            localizedFulltext.add(v.toString());
          }
        } else {
          localizedFulltext.add(value.toString());
        }
      }
    }

  }

  /**
   * Returns the identifier.
   * 
   * @return the identifier
   */
  public String getUID() {
    return id;
  }

}
