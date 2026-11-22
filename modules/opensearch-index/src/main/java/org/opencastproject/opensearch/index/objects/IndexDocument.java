/*
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


package org.opencastproject.opensearch.index.objects;

import org.opencastproject.opensearch.impl.IndexSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A document to be written to the search index. Every field holds a list of distinct values.
 */
public class IndexDocument extends LinkedHashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  /**
   * Adds a value to a field. Elements of arrays are added individually, {@code null} values are ignored.
   *
   * @param name
   *          the field name
   * @param value
   *          the value to add
   */
  @SuppressWarnings("unchecked")
  public void addField(String name, Object value) {
    if (name == null) {
      throw new IllegalArgumentException("Field name cannot be null");
    }
    if (name.contains(".")) {
      throw new IllegalArgumentException("Field name may not contain '.'");
    }
    if (value == null) {
      return;
    }
    final List<Object> values = (List<Object>) computeIfAbsent(name, n -> new ArrayList<>());
    final Object[] newValues = value.getClass().isArray() ? (Object[]) value : new Object[] { value };
    for (Object v : newValues) {
      if (!values.contains(v)) {
        values.add(v);
      }
    }
  }

  /**
   * Returns the serialized object stored in a document read from the index.
   *
   * @param source
   *          the source of a document as returned by OpenSearch
   * @return the serialized object
   */
  public static String getObject(Map<String, Object> source) {
    final Object value = source.get(IndexSchema.OBJECT);
    if (value instanceof List<?> list) {
      return (String) list.get(0);
    }
    return (String) value;
  }
}
