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

import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.impl.ResourceListQueryImpl;

import com.google.common.collect.Iterables;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DublinCoreMetadataCollection {
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreMetadataCollection.class);

  /** The list containing all the metadata */
  private List<MetadataField> fieldsInOrder = new ArrayList<>();
  private final Map<String, MetadataField> outputFields = new HashMap<>();

  public DublinCoreMetadataCollection() {
    this(Collections.emptyList());
  }

  public DublinCoreMetadataCollection(final Iterable<MetadataField> fields) {
    for (final MetadataField field : fields) {
      addField(field);
    }
  }

  public DublinCoreMetadataCollection(final DublinCoreMetadataCollection c) {
    this(c.fieldsInOrder);
  }

  public DublinCoreMetadataCollection readOnlyCopy() {
    return new DublinCoreMetadataCollection(this.fieldsInOrder.stream().map(MetadataField::readOnlyCopy)
            .collect(Collectors.toList()));
  }

  public Map<String, MetadataField> getOutputFields() {
    return outputFields;
  }

  public void addField(final MetadataField metadata) {
    if (metadata == null)
      throw new IllegalArgumentException("The metadata must not be null.");
    addFieldInOrder(metadata);
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

  public void addEmptyField(final MetadataField metadataField, final ListProvidersService listProvidersService) {
    addField(metadataField, Collections.emptyList(), listProvidersService);
  }

  public void addField(final MetadataField metadataField, final String value, final ListProvidersService listProvidersService) {
    addField(metadataField, Collections.singletonList(value), listProvidersService);
  }

  /**
   * Set value to a metadata field of unknown type
   */
  private static void setValueFromDCCatalog(
          final List<String> filteredValues,
          final MetadataField metadataField) {
    if (filteredValues.isEmpty()) {
      throw new IllegalArgumentException("Values cannot be empty");
    }

    if (filteredValues.size() > 1
            && metadataField.getType() != MetadataField.Type.MIXED_TEXT
            && metadataField.getType() != MetadataField.Type.ITERABLE_TEXT) {
      logger.warn("Cannot put multiple values into a single-value field, only the last value is used. {}",
              Arrays.toString(filteredValues.toArray()));
    }

    switch (metadataField.getType()) {
      case BOOLEAN:
        metadataField.setValue(Boolean.parseBoolean(Iterables.getLast(filteredValues)), false);
        break;
      case DATE:
        if (metadataField.getPattern() == null) {
          metadataField.setPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        }
        metadataField.setValue(EncodingSchemeUtils.decodeDate(Iterables.getLast(filteredValues)), false);
        break;
      case DURATION:
        final String value = Iterables.getLast(filteredValues);
        final DCMIPeriod period = EncodingSchemeUtils.decodePeriod(value);
        if (period == null)
          throw new IllegalArgumentException("period couldn't be parsed: " + value);
        final long longValue = period.getEnd().getTime() - period.getStart().getTime();
        metadataField.setValue(Long.toString(longValue), false);
        break;
      case ITERABLE_TEXT:
      case MIXED_TEXT:
        metadataField.setValue(filteredValues, false);
        break;
      case LONG:
        metadataField.setValue(Long.parseLong(Iterables.getLast(filteredValues)), false);
        break;
      case START_DATE:
        if (metadataField.getPattern() == null) {
          metadataField.setPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        }
        metadataField.setValue(Iterables.getLast(filteredValues), false);
        break;
      case TEXT:
      case ORDERED_TEXT:
      case TEXT_LONG:
        metadataField.setValue(Iterables.getLast(filteredValues), false);
        break;
      default:
        throw new IllegalArgumentException("Unknown metadata type! " + metadataField.getType());
    }
  }


  public void addField(final MetadataField metadataField, final List<String> values, final ListProvidersService listProvidersService) {
    final List<String> filteredValues = values.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());

    if (!filteredValues.isEmpty()) {
      setValueFromDCCatalog(filteredValues, metadataField);
    }

    metadataField.setIsTranslatable(getCollectionIsTranslatable(metadataField, listProvidersService));
    metadataField.setCollection(getCollection(metadataField, listProvidersService));

    addField(metadataField);
  }

  private static Boolean getCollectionIsTranslatable(
          final MetadataField metadataField,
          final ListProvidersService listProvidersService) {
    if (listProvidersService != null && metadataField.getListprovider() != null) {
      try {
        return listProvidersService.isTranslatable(metadataField.getListprovider());
      } catch (final ListProviderException ex) {
        // failed to get is-translatable property on list-provider-service
        // as this field is optional, it is fine to pass here
      }
    }
    return null;
  }

  private static Map<String, String> getCollection(
          final MetadataField metadataField,
          final ListProvidersService listProvidersService) {
    try {
      if (listProvidersService != null && metadataField.getListprovider() != null) {
        return listProvidersService.getList(metadataField.getListprovider(),
                new ResourceListQueryImpl(), true);
      }
      return null;
    } catch (final ListProviderException e) {
      logger.warn("Unable to set collection on metadata because", e);
      return null;
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

  public void removeField(final MetadataField metadata) {
    if (metadata == null)
      throw new IllegalArgumentException("The metadata must not be null.");
    this.fieldsInOrder.remove(metadata);
    this.outputFields.remove(metadata.getOutputID());
  }

  public List<MetadataField> getFields() {
    return this.fieldsInOrder;
  }

  public void updateStringField(final MetadataField current, final String value) {
    if (current.getValue() != null && !(current.getValue() instanceof String)) {
      throw new IllegalArgumentException("Unable to update a field to a different type than String with this method!");
    }
    removeField(current);
    final MetadataField field = new MetadataField(current);
    field.setValue(value);
    addField(field);
  }

  public boolean isUpdated() {
    for (final MetadataField field : fieldsInOrder) {
      if (field.isUpdated()) {
        return true;
      }
    }
    return false;
  }

}
