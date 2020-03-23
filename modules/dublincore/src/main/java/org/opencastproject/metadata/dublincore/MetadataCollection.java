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
package org.opencastproject.metadata.dublincore;

import com.entwinemedia.fn.data.json.JValue;

import java.util.List;
import java.util.Map;

public interface MetadataCollection {

  /**
   * Format the metadata as JSON array
   *
   * @return a JSON array representation of the metadata
   */
  JValue toJSON();

  /**
   * Parse the given JSON string to extract the metadata. The JSON structure must look like this:
   *
   * <pre>
   * [
   *  {
   *     "id"        : "field id",
   *     "value"     : "field value",
   *
   *     // The following properties should not be present as they are useless,
   *     // but they do not hurt for the parsing.
   *
   *     "label"     : "EVENTS.SERIES.DETAILS.METADATA.LABEL",
   *     "type"      : "",
   *     // The collection can be a json object like below...
   *     "collection": { "id1": "value1", "id2": "value2" },
   *     // Or a the id of the collection available through the resource endpoint
   *     "collection": "USERS",
   *     "readOnly": false
   *   },
   *
   *   // Additionally fields
   *   ...
   * ]
   * </pre>
   *
   * @param json
   *          A JSON array of metadata as String
   * @throws MetadataParsingException
   *           if the JSON structure is not correct
   * @throws IllegalArgumentException
   *           if the JSON string is null or empty
   */
  MetadataCollection fromJSON(String json) throws MetadataParsingException;

  /**
   * Copy all fields of a metadata collection into a new collection.
   *
   * @return a copy of the current MetadataCollection
   */
  MetadataCollection getCopy();

  Map<String, MetadataField<?>> getInputFields();

  Map<String, MetadataField<?>> getOutputFields();

  /**
   * Add the given {@link MetadataField} field to the metadata list
   *
   * @param metadata
   *          The {@link MetadataField} field to add
   * @throws IllegalArgumentException
   *           if the {@link MetadataField} is null
   *
   */
  void addField(MetadataField<?> metadata);

  void removeField(MetadataField<?> metadata);

  List<MetadataField<?>> getFields();

  void updateStringField(MetadataField<?> current, String value);

  boolean isUpdated();

}
