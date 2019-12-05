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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract container for the metadata
 */
public final class DublinCoreMetadataCollection implements MetadataCollection {
  /** The list containing all the metadata */
  private List<MetadataField> fieldsInOrder = new ArrayList<>();
  private final Map<String, MetadataField> inputFields = new HashMap<>();
  private final Map<String, MetadataField> outputFields = new HashMap<>();

  public DublinCoreMetadataCollection() {
    this(Collections.emptyList());
  }

  public DublinCoreMetadataCollection(final Iterable<MetadataField> fields) {
    for (final MetadataField field : fields) {
      addField(field);
    }
  }

  @Override
  public MetadataCollection getCopy() {
    final MetadataCollection copiedCollection = new DublinCoreMetadataCollection();
    for (final MetadataField field : getFields()) {
      final MetadataField copiedField = new MetadataField(field);
      copiedCollection.addField(copiedField);
    }
    return copiedCollection;
  }

  @Override
  public Map<String, MetadataField> getInputFields() {
    return inputFields;
  }

  @Override
  public Map<String, MetadataField> getOutputFields() {
    return outputFields;
  }

  @Override
  public void addField(final MetadataField metadata) {
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
  private void addFieldInOrder(final MetadataField metadata) {
    removeFieldIfExists(metadata);

    // Find all of the ordered or unordered elements.
    final ArrayList<MetadataField> orderedFields = new ArrayList<>();
    final ArrayList<MetadataField> unorderedFields = new ArrayList<>();
    for (final MetadataField field : fieldsInOrder) {
      if (field.getOrder() != null) {
        orderedFields.add(field);
      } else {
        unorderedFields.add(field);
      }
    }

    // Add the new field to either the ordered fields or the unordered fields.
    if (metadata.getOrder() != null) {
      orderedFields.add(metadata);
    } else {
      unorderedFields.add(metadata);
    }

    // Sort the ordered elements so that early entries don't push later entries to the right
    orderedFields.sort(Comparator.comparingInt(MetadataField::getOrder));

    // Add all the non-ordered elements to the collection
    fieldsInOrder = new ArrayList<>(unorderedFields);

    // Add all of the fields that have an index to their location starting at the lowest value.
    for (final MetadataField orderedField : orderedFields) {
      final int index = orderedField.getOrder() < fieldsInOrder.size() ? orderedField.getOrder()
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
  private void removeFieldIfExists(final MetadataField metadata) {
    int index = -1;
    for (final MetadataField field : fieldsInOrder) {
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
  public void removeField(final MetadataField metadata) {
    if (metadata == null)
      throw new IllegalArgumentException("The metadata must not be null.");
    this.fieldsInOrder.remove(metadata);
    this.inputFields.remove(metadata.getInputID());
    this.outputFields.remove(metadata.getOutputID());
  }

  @Override
  public List<MetadataField> getFields() {
    return this.fieldsInOrder;
  }

  @Override
  public void updateStringField(final MetadataField current, final String value) {
    if (current.getValue() != null && !(current.getValue() instanceof String)) {
      throw new IllegalArgumentException("Unable to update a field to a different type than String with this method!");
    }
    removeField(current);
    final MetadataField field = new MetadataField(current);
    field.setValue(value);
    addField(field);
  }

  @Override
  public boolean isUpdated() {
    for (final MetadataField field : fieldsInOrder) {
      if (field.isUpdated()) {
        return true;
      }
    }
    return false;
  }

}
