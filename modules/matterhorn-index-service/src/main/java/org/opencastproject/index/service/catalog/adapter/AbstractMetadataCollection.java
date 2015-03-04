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
package org.opencastproject.index.service.catalog.adapter;

import static com.entwinemedia.fn.data.json.Jsons.a;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.exception.MetadataParsingException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.JValue;
import org.apache.commons.lang.StringUtils;
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
public abstract class AbstractMetadataCollection {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractMetadataCollection.class);

  /* Keys for the different properties of the metadata JSON Object */
  protected static final String KEY_METADATA_ID = "id";
  protected static final String KEY_METADATA_VALUE = "value";

  /** The list containing all the metadata */
  private List<MetadataField<?>> fieldsInOrder = new ArrayList<MetadataField<?>>();
  private Map<String, MetadataField<?>> inputFields = new HashMap<String, MetadataField<?>>();
  private Map<String, MetadataField<?>> outputFields = new HashMap<String, MetadataField<?>>();

  /**
   * Format the metadata as JSON array
   *
   * @return a JSON array representation of the metadata
   */
  public JValue toJSON() {
    List<JValue> metadata = new ArrayList<JValue>();
    for (MetadataField<?> metadataField : getFields()) {
      metadata.add(metadataField.toJSON());
    }
    return a(metadata);
  }

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
  public AbstractMetadataCollection fromJSON(String json) throws MetadataParsingException {
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

  public Map<String, MetadataField<?>> getInputFields() {
    return inputFields;
  }

  public Map<String, MetadataField<?>> getOutputFields() {
    return outputFields;
  }

  /**
   * Add the given {@link MetadataField} field to the metadata list
   *
   * @param metadata
   *          The {@link MetadataField} field to add
   * @throws IllegalArgumentException
   *           if the {@link MetadataField} is null
   *
   */
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
    ArrayList<MetadataField<?>> orderedFields = new ArrayList<MetadataField<?>>();
    ArrayList<MetadataField<?>> unorderedFields = new ArrayList<MetadataField<?>>();
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
    fieldsInOrder = new ArrayList<MetadataField<?>>(unorderedFields);

    // Add all of the fields that have an index to their location starting at the lowest value.
    for (MetadataField<?> orderedField : orderedFields) {
      Integer index = orderedField.getOrder().get() < fieldsInOrder.size() ? orderedField.getOrder().get() : fieldsInOrder
              .size();
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

  public void removeField(MetadataField<?> metadata) {
    if (metadata == null)
      throw new IllegalArgumentException("The metadata must not be null.");
    this.fieldsInOrder.remove(metadata);
    this.inputFields.remove(metadata.getInputID());
    this.outputFields.remove(metadata.getOutputID());
  }

  public List<MetadataField<?>> getFields() {
    return this.fieldsInOrder;
  }

  public void updateStringField(MetadataField<?> current, String value) {
    if (current.getValue().isSome() && !(current.getValue().get() instanceof String)) {
      throw new IllegalArgumentException("Unable to update a field to a different type than String with this method!");
    }
    removeField(current);
    MetadataField<String> field = MetadataField.createTextMetadataField(current.getInputID(),
            Opt.some(current.getOutputID()), current.getLabel(), current.isReadOnly(), current.isRequired(),
            current.getCollection(), current.getCollectionID(), current.getOrder(), current.getNamespace());
    field.setValue(value);
    addField(field);
  }

  /**
   * Get the values collection with the given name from the {@link ListProvidersService}.
   *
   * @param name
   *          The target collection
   * @param listProviderService
   *          The list provider service
   * @return A value collection with the given name wrapped in an {@link Opt}, or {@link Opt#none()} if no collection
   *         has been found with this name.
   * @throws IllegalArgumentException
   *           if the name or the listProviderService is null or the name blank.
   */
  protected Opt<Map<String, Object>> getCollection(String name, ListProvidersService listProviderService) {
    if (StringUtils.isBlank(name))
      throw new IllegalArgumentException("The listName must not be null or empty!");
    if (listProviderService == null)
      throw new IllegalArgumentException("The list provider must not be null!");

    Opt<Map<String, Object>> list;
    try {
      list = Opt.some(listProviderService.getList(name, new ResourceListQueryImpl(), null));
    } catch (ListProviderException e) {
      logger.warn("Not able to find a value list with the name {}", name);
      list = Opt.none();
    }
    return list;
  }

  public boolean isUpdated() {
    for (MetadataField<?> field : fieldsInOrder) {
      if (field.isUpdated()) {
        return true;
      }
    }
    return false;
  }
}
