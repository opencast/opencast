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

package org.opencastproject.index.service.catalog.adapter;

import static com.entwinemedia.fn.data.json.Jsons.arr;

import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.metadata.dublincore.MetadataParsingException;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Abstract container for the metadata
 */
public abstract class AbstractMetadataCollection implements MetadataCollection {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractMetadataCollection.class);

  /* Keys for the different properties of the metadata JSON Object */
  protected static final String KEY_METADATA_ID = "id";
  protected static final String KEY_METADATA_VALUE = "value";

  /** The list containing all the metadata */
  private List<MetadataField<?>> fieldsInOrder = new ArrayList<>();
  private Map<String, MetadataField<?>> inputFields = new HashMap<>();
  private Map<String, MetadataField<?>> outputFields = new HashMap<>();

  @Override
  public JValue toJSON() {
    List<JValue> metadata = new ArrayList<>();
    for (MetadataField<?> metadataField : getFields()) {
      metadata.add(metadataField.toJSON());
    }
    return arr(metadata);
  }

  @Override
  public MetadataCollection fromJSON(String json) throws MetadataParsingException {
    if (StringUtils.isBlank(json))
      throw new IllegalArgumentException("The JSON string must not be empty or null!");

    JSONParser parser = new JSONParser();
    JSONArray metadataJSON;
    try {
      metadataJSON = (JSONArray) parser.parse(json);
    } catch (ParseException e) {
      throw new MetadataParsingException("Not able to parse the given string as JSON event metadata.", e.getCause());
    }

    ListIterator<JSONObject> listIterator = metadataJSON.listIterator();

    while (listIterator.hasNext()) {
      JSONObject item = listIterator.next();
      String fieldId = (String) item.get(KEY_METADATA_ID);
      MetadataField<?> target = null;

      if (fieldId == null)
        continue;
      Object value = item.get(KEY_METADATA_VALUE);
      if (value == null)
        continue;

      target = outputFields.get(fieldId);
      if (target == null)
        continue;

      target.fromJSON(value);
    }
    return this;
  }

  @Override
  public Map<String, MetadataField<?>> getInputFields() {
    return inputFields;
  }

  @Override
  public Map<String, MetadataField<?>> getOutputFields() {
    return outputFields;
  }

  @Override
  public void addField(MetadataField<?> metadata) {
    if (metadata == null)
      throw new IllegalArgumentException("The metadata must not be null.");
    addFieldInOrder(metadata);
    this.inputFields.put(metadata.getInputID(), metadata);
    this.outputFields.put(metadata.getOutputID(), metadata);
  }

  /**
   * Adds a field in ui order to the collection. If no order is specified it will be added to the end.
   *
   * @param metadata
   *          The metadata to add to the collection.
   */
  private void addFieldInOrder(MetadataField<?> metadata) {
    removeFieldIfExists(metadata);

    // Find all of the ordered or unordered elements.
    ArrayList<MetadataField<?>> orderedFields = new ArrayList<>();
    ArrayList<MetadataField<?>> unorderedFields = new ArrayList<>();
    for (MetadataField<?> field : fieldsInOrder) {
      if (field.getOrder().isSome()) {
        orderedFields.add(field);
      } else {
        unorderedFields.add(field);
      }
    }

    // Add the new field to either the ordered fields or the unordered fields.
    if (metadata.getOrder().isSome()) {
      orderedFields.add(metadata);
    } else {
      unorderedFields.add(metadata);
    }

    // Sort the ordered elements so that early entries don't push later entries to the right
    Collections.sort(orderedFields, new Comparator<MetadataField<?>>() {
      @Override
      public int compare(MetadataField<?> o1, MetadataField<?> o2) {
        return o1.getOrder().get() - o2.getOrder().get();
      }
    });

    // Add all the non-ordered elements to the collection
    fieldsInOrder = new ArrayList<>(unorderedFields);

    // Add all of the fields that have an index to their location starting at the lowest value.
    for (MetadataField<?> orderedField : orderedFields) {
      Integer index = orderedField.getOrder().get() < fieldsInOrder.size() ? orderedField.getOrder().get()
              : fieldsInOrder.size();
      fieldsInOrder.add(index, orderedField);
    }
  }

  /**
   * Removes a {@link MetadataField} if it already exists in the ordered collection.
   *
   * @param metadata
   *          The field to remove.
   */
  private void removeFieldIfExists(MetadataField<?> metadata) {
    int index = -1;
    for (MetadataField<?> field : fieldsInOrder) {
      if (field.getInputID().equalsIgnoreCase(metadata.getInputID())
              && field.getOutputID().equalsIgnoreCase(metadata.getOutputID())) {
        index = fieldsInOrder.indexOf(field);
      }
    }

    if (index >= 0) {
      fieldsInOrder.remove(index);
    }
  }

  @Override
  public void removeField(MetadataField<?> metadata) {
    if (metadata == null)
      throw new IllegalArgumentException("The metadata must not be null.");
    this.fieldsInOrder.remove(metadata);
    this.inputFields.remove(metadata.getInputID());
    this.outputFields.remove(metadata.getOutputID());
  }

  @Override
  public List<MetadataField<?>> getFields() {
    return this.fieldsInOrder;
  }

  @Override
  public void updateStringField(MetadataField<?> current, String value) {
    if (current.getValue().isSome() && !(current.getValue().get() instanceof String)) {
      throw new IllegalArgumentException("Unable to update a field to a different type than String with this method!");
    }
    removeField(current);
    MetadataField<String> field = MetadataField.createTextMetadataField(current.getInputID(),
            Opt.some(current.getOutputID()), current.getLabel(), current.isReadOnly(), current.isRequired(),
            current.isTranslatable(), current.getCollection(), current.getCollectionID(), current.getOrder(),
            current.getNamespace());
    field.setValue(value);
    addField(field);
  }

  @Override
  public boolean isUpdated() {
    for (MetadataField<?> field : fieldsInOrder) {
      if (field.isUpdated()) {
        return true;
      }
    }
    return false;
  }

}
