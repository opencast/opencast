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

import org.opencastproject.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;

import com.entwinemedia.fn.data.Opt;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A helper class for dealing with metadata.
 */
public final class MetadataUtils {
  private MetadataUtils() {
  }

  /**
   * Update a {@link DublinCoreCatalog} value with a value from a {@link AbstractMetadataCollection} if it is available
   * and updated.
   *
   * @param dc
   *          The {@link DublinCoreCatalog} to update.
   * @param metadata
   *          The {@link AbstractMetadataCollection} to pull the value from.
   * @param jsonID
   *          The id of the {@link MetadataField} to pull the value from.
   * @param dcEname
   *          The {@link EName} of the property to update in the {@link DublinCoreCatalog}
   */
  public static void updateDCString(DublinCoreCatalog dc, AbstractMetadataCollection metadata, String jsonID,
          EName dcEname) {
    Opt<String> updatedString = MetadataUtils.getUpdatedStringMetadata(metadata, jsonID);
    if (updatedString.isSome()) {
      dc.set(dcEname, updatedString.get());
    }
  }

  /**
   * Returns a Date value from a {@link AbstractMetadataCollection} if it has been updated and it is available.
   *
   * @param collection
   *          The {@link AbstractMetadataCollection} to pull the {@link Date} value from.
   * @param outputID
   *          The key that the front end uses for this property.
   * @return An {@link Opt} with a possible {@link Date} value if it is available.
   */
  public static Opt<Date> getUpdatedDateMetadata(AbstractMetadataCollection collection, String outputID) {
    Opt<Date> field = Opt.<Date> none();

    if (collection.getOutputFields().get(outputID) != null) {
      MetadataField<?> genericField = collection.getOutputFields().get(outputID);
      if (genericField.isUpdated()) {
        field = getDateMetadata(genericField);
      }
    }

    return field;
  }

  /**
   * Get a possible {@link Date} value from a {@link MetadataField}
   *
   * @param metadataField
   *          The {@link MetadataField} to retrieve the value from.
   * @return A {@link Date} if it is available.
   */
  public static Opt<Date> getDateMetadata(MetadataField<?> metadataField) {
    Opt<Date> value = Opt.<Date> none();
    if (metadataField.getValue().isSome() && metadataField.getValue().get() instanceof Date) {
      value = Opt.some((Date) metadataField.getValue().get());
    }
    return value;
  }

  /**
   * Copy a {@link MetadataField} into a new field.
   *
   * @param other
   *          The other {@link MetadataField} to copy the state from.
   * @return A new {@link MetadataField} with the same settings as the passed in {@link MetadataField}
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static MetadataField copyMetadataField(MetadataField other) {
    MetadataField newField = new MetadataField();
    newField.setCollection(other.getCollection());
    newField.setCollectionID(other.getCollectionID());
    newField.setInputId(other.getInputID());
    newField.setLabel(other.getLabel());
    newField.setListprovider(other.getListprovider());
    newField.setNamespace(other.getNamespace());
    newField.setOutputID(Opt.some(other.getOutputID()));
    newField.setPattern(other.getPattern());
    newField.setOrder(other.getOrder());
    newField.setReadOnly(other.isReadOnly());
    newField.setRequired(other.isRequired());
    newField.setJsonType(other.getJsonType());
    newField.setJsonToValue(other.getJsonToValue());
    newField.setValueToJSON(other.getValueToJSON());
    newField.setType(other.getType());
    if (other.getValue().isSome()) {
      newField.setValue(other.getValue().get());
    }
    return newField;
  }

  /**
   * Returns the {@link String} value of a {@link MetadataField} if updated and available.
   *
   * @param collection
   *          The {@link AbstractMetadataCollection} to pull the {@link MetadataField} from.
   * @param outputID
   *          The key used to id this {@link MetadataField} in the UI
   * @return The possible {@link String} value from a {@link MetadataField}.
   */
  public static Opt<String> getUpdatedStringMetadata(AbstractMetadataCollection collection, String outputID) {
    Opt<String> field = Opt.<String> none();

    if (collection.getOutputFields().get(outputID) != null) {
      MetadataField<?> genericField = collection.getOutputFields().get(outputID);
      if (genericField.isUpdated()) {
        field = getStringMetadata(genericField);
      }
    }
    return field;
  }

  /**
   * Returns the value of a {@link MetadataField} if available.
   *
   * @param metadataField
   *          The {@link MetadataField} to pull the value from.
   * @return An optional {@link String} value.
   */
  public static Opt<String> getStringMetadata(MetadataField<?> metadataField) {
    Opt<String> value = Opt.<String> none();
    if (metadataField != null && metadataField.getValue().isSome() && metadataField.getValue().get() instanceof String) {
      value = Opt.some((String) metadataField.getValue().get());
    }
    return value;
  }

  /**
   * Get the value of a {@link MetadataField} that is an {@link Iterable<String>}
   *
   * @param abstractMetadataCollection
   *          The {@link AbstractMetadataCollection} to search for the {@link MetadataField}
   * @param outputID
   *          The key in the UI that ids the relevant {@link MetadataField}
   * @return The {@link Iterable<String>} value if available.
   */
  public static Iterable<String> getIterableStringMetadataByOutputID(
          AbstractMetadataCollection abstractMetadataCollection, String outputID) {
    MetadataField<?> metadataField = abstractMetadataCollection.getOutputFields().get(outputID);
    if (metadataField != null) {
      return getIterableStringMetadata(metadataField);
    }
    return null;
  }

  /**
   * Get an {@link Iterable<String>} value from a {@link MetadataField} if it has been updated.
   *
   * @param metadata
   *          The {@link AbstractMetadataCollection} to search for the {@link MetadataField}
   * @param outputID
   *          The key in the UI that ids the relevant {@link MetadataField}
   * @return An optional {@link Iterable<String>} returned if updated.
   */
  public static Opt<List<String>> getUpdatedIterableStringMetadata(AbstractMetadataCollection metadata, String outputID) {
    Opt<List<String>> iterableString = Opt.<List<String>> none();
    MetadataField<?> field = metadata.getOutputFields().get(outputID);
    if (field == null) {
      return Opt.<List<String>> none();
    }
    if (field.isUpdated()) {
      iterableString = Opt.some(getListFromIterableStringMetadata(field));
    }
    return iterableString;
  }

  /**
   * Get a @ List<String>} from a {@link MetadataField<?>} if available.
   *
   * @param metadataField
   *          The {@link MetadataField} to get the value from.
   * @return A @ List<String>} (empty if not available)
   */
  public static List<String> getListFromIterableStringMetadata(MetadataField<?> metadataField) {
    List<String> strings = new ArrayList<String>();
    if (metadataField.getValue().isSome()) {
      if (metadataField.getValue().get() instanceof Iterable<?>) {
        Iterable<?> iterableCollection = (Iterable<?>) metadataField.getValue().get();
        for (Object value : iterableCollection) {
          if (value instanceof String) {
            strings.add(value.toString());
          }
        }
      } else if (metadataField.getValue().get() instanceof String) {
        strings.add(metadataField.getValue().get().toString());
      }

    }
    return strings;
  }

  /**
   * Get a @ Iterable<String>} from a {@link MetadataField<?>} if available.
   *
   * @param metadataField
   *          The {@link MetadataField} to get the value from.
   * @return A @ Iterable<String>} (empty if not available)
   */
  public static Iterable<String> getIterableStringMetadata(MetadataField<?> metadataField) {
    return getListFromIterableStringMetadata(metadataField);
  }
}
