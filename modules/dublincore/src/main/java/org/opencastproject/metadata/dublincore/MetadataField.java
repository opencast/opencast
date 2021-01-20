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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.TimeZone;

/**
 * This is a generic and very abstract view of a certain field/property in a metadata catalog. The main purpose of this
 * class is to have a generic access to the variety of information stored in metadata catalogs.
 */
public class MetadataField {

  private static final Logger logger = LoggerFactory.getLogger(MetadataField.class);

  /** Keys for the different values in the configuration file */
  public static final String CONFIG_COLLECTION_ID_KEY = "collectionID";
  private static final String CONFIG_PATTERN_KEY = "pattern";
  private static final String CONFIG_DELIMITER_KEY = "delimiter";
  public static final String CONFIG_INPUT_ID_KEY = "inputID";
  public static final String CONFIG_LABEL_KEY = "label";
  public static final String CONFIG_LIST_PROVIDER_KEY = "listprovider";
  private static final String CONFIG_NAMESPACE_KEY = "namespace";
  private static final String CONFIG_ORDER_KEY = "order";
  private static final String CONFIG_OUTPUT_ID_KEY = "outputID";
  public static final String CONFIG_PROPERTY_PREFIX = "property";
  public static final String CONFIG_READ_ONLY_KEY = "readOnly";
  public static final String CONFIG_REQUIRED_KEY = "required";
  public static final String CONFIG_TYPE_KEY = "type";

  /**
   * Possible types for the metadata field. The types are used in the frontend and backend to know how the metadata
   * fields should be formatted (if needed).
   */
  public enum Type {
    BOOLEAN, DATE, DURATION, ITERABLE_TEXT, MIXED_TEXT, ORDERED_TEXT, LONG, START_DATE, START_TIME, TEXT, TEXT_LONG
  }

  /** The id of a collection to validate values against. */
  private String collectionID;
  /** The format to use for temporal date properties. */
  private String pattern;
  /** The delimiter used to display and parse list values. */
  private String delimiter;
  /** The id of the field used to identify it in the dublin core. */
  private final String inputID;
  /** The i18n id for the label to show the property. */
  private final String label;
  /** The provider to populate the property with. */
  private final String listprovider;
  /** The optional namespace of the field used if a field can be found in more than one namespace */
  private final String namespace;
  /**
   * In the order of properties where this property should be oriented in the UI i.e. 0 means the property should come
   * first, 1 means it should come second etc.
   */
  private final Integer order;
  /** The optional id of the field used to output for the ui, if not present will assume the same as the inputID. */
  private final String outputID;
  /** Whether the property should not be edited. */
  private boolean readOnly;
  /** Whether the property is required to update the metadata. */
  private final boolean required;
  /** The type of the metadata for example text, date etc. */
  private Type type;

  private Object value;
  private Boolean translatable;
  private boolean updated = false;
  private Map<String, String> collection;

  // this can only be true if the metadata field is representing multiple events with different values
  private Boolean hasDifferentValues = null;

  /**
   * Copy constructor
   *
   * @param other
   *          Other metadata field
   */
  public MetadataField(final MetadataField other) {
    this.inputID = other.inputID;
    this.outputID = other.outputID;
    this.label = other.label;
    this.readOnly = other.readOnly;
    this.required = other.required;
    this.value = other.value;
    this.translatable = other.translatable;
    this.hasDifferentValues = other.hasDifferentValues;
    this.type = other.type;
    this.collection = other.collection;
    this.collectionID = other.collectionID;
    this.order = other.order;
    this.namespace = other.namespace;
    this.updated = other.updated;
    this.pattern = other.pattern;
    this.delimiter = other.delimiter;
    this.listprovider = other.listprovider;
  }

  /**
   * Metadata field constructor
   *
   * @param inputID
   *          The identifier of the new metadata field
   * @param label
   *          the label of the field. The string displayed next to the field value on the frontend. This is usually be a
   *          translation key
   * @param readOnly
   *          Define if the new metadata field can be or not edited
   * @param required
   *          Define if the new metadata field is or not required
   * @param value
   *          The metadata field value
   * @param type
   *          The metadata field type @ EventMetadata.Type}
   * @param collection
   *          If the field has a limited list of possible value, the option should contain this one. Otherwise it should
   *          be none. This is also possible to use the collectionId parameter for that.
   * @param collectionID
   *          The id of the limit list of possible value that should be get through the resource endpoint.
   * @param listprovider An optional list provider ID
   * @param pattern Pattern for time/date fields
   * @param delimiter Delimiter
   * @throws IllegalArgumentException
   *           if the id, label, type parameters is/are null
   */
  public MetadataField(
          final String inputID,
          final String outputID,
          final String label,
          final boolean readOnly,
          final boolean required,
          final Object value,
          final Boolean translatable,
          final Type type,
          final Map<String, String> collection,
          final String collectionID,
          final Integer order,
          final String namespace,
          final String listprovider,
          final String pattern,
          final String delimiter) throws IllegalArgumentException {
    if (StringUtils.isBlank(inputID))
      throw new IllegalArgumentException("The metadata input id must not be null.");
    if (StringUtils.isBlank(label))
      throw new IllegalArgumentException("The metadata label must not be null.");
    if (type == null)
      throw new IllegalArgumentException("The metadata type must not be null.");
    this.inputID = inputID;
    this.outputID = outputID;
    this.label = label;
    this.readOnly = readOnly;
    this.required = required;
    this.value = value;
    this.translatable = translatable;
    this.type = type;
    this.collection = collection;
    this.collectionID = collectionID;
    this.order = order;
    this.namespace = namespace;
    this.listprovider = listprovider;
    this.pattern = pattern;
    this.delimiter = delimiter;
  }

  /**
   * Set the option of a limited list of possible values.
   *
   * @param collection
   *          The option of a limited list of possible values
   */
  public void setCollection(final Map<String, String> collection) {
    this.collection = collection;
  }

  public Map<String, String> getCollection() {
    return collection;
  }

  public Object getValue() {
    return value;
  }

  public Boolean isTranslatable() {
    return translatable;
  }

  public boolean isUpdated() {
    return updated;
  }

  public void setValue(final Object value) {
    setValue(value, true);
  }

  public void setValue(final Object value, final boolean setUpdated) {
    this.value = value;

    if (setUpdated) {
      this.updated = true;
    }
  }

  public void setIsTranslatable(final Boolean translatable) {
    this.translatable = translatable;
  }

  public static SimpleDateFormat getSimpleDateFormatter(final String pattern) {
    final SimpleDateFormat dateFormat;
    if (StringUtils.isNotBlank(pattern)) {
      dateFormat = new SimpleDateFormat(pattern);
    } else {
      dateFormat = new SimpleDateFormat();
    }
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormat;
  }

  public static MetadataField createMetadataField(final Map<String,String> configuration) {
    final String inputID = configuration.get(CONFIG_INPUT_ID_KEY);
    final String label = configuration.get(CONFIG_LABEL_KEY);

    final String collectionID = configuration.get(CONFIG_COLLECTION_ID_KEY);
    final String delimiter = configuration.get(CONFIG_DELIMITER_KEY);
    final String outputID = configuration.get(CONFIG_OUTPUT_ID_KEY);
    final String listprovider = configuration.get(CONFIG_LIST_PROVIDER_KEY);
    final String namespace = configuration.get(CONFIG_NAMESPACE_KEY);

    final Type type = configuration.containsKey(CONFIG_TYPE_KEY)
            ? Type.valueOf(configuration.get(CONFIG_TYPE_KEY).toUpperCase()) : null;
    final boolean required = configuration.containsKey(CONFIG_REQUIRED_KEY) && Boolean
            .parseBoolean(configuration.get(CONFIG_REQUIRED_KEY).toUpperCase());
    final boolean readOnly = configuration.containsKey(CONFIG_READ_ONLY_KEY) && Boolean
            .parseBoolean(configuration.get(CONFIG_READ_ONLY_KEY).toUpperCase());

    final String pattern = configuration.getOrDefault(CONFIG_PATTERN_KEY, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    Integer order = null;
    if (configuration.containsKey(CONFIG_ORDER_KEY)) {
      try {
        order = Integer.parseInt(configuration.get(CONFIG_ORDER_KEY));
      } catch (final NumberFormatException e) {
        logger.warn("Unable to parse order value {} of metadata field {}", configuration.get(CONFIG_ORDER_KEY),
                inputID, e);
      }
    }

    if (type == null)
      throw new IllegalArgumentException("type is null");

    switch (type) {
      case BOOLEAN:
        return new MetadataField(
                inputID,
                outputID,
                label,
                readOnly,
                required,
                null,
                null,
                type,
                null,
                null,
                order,
                namespace,
                listprovider,
                null,
                null);
      case DATE:
        return new MetadataField(
                inputID,
                outputID,
                label,
                readOnly,
                required,
                null,
                null,
                type,
                null,
                null,
                order,
                namespace,
                listprovider,
                StringUtils.isNotBlank(pattern) ? pattern : null,
                null);
      case DURATION:
      case TEXT:
      case ORDERED_TEXT:
      case TEXT_LONG:
        return new MetadataField(
                inputID,
                outputID,
                label,
                readOnly,
                required,
                "",
                null,
                type,
                null,
                collectionID,
                order,
                namespace,
                listprovider,
                null,
                null);
      case ITERABLE_TEXT:
      case MIXED_TEXT:
        return new MetadataField(
                inputID,
                outputID,
                label,
                readOnly,
                required,
                new ArrayList<>(),
                null,
                type,
                null,
                collectionID,
                order,
                namespace,
                listprovider,
                null,
                delimiter);
      case LONG:
        return new MetadataField(
                inputID,
                outputID,
                label,
                readOnly,
                required,
                0L,
                null,
                Type.LONG,
                null,
                collectionID,
                order,
                namespace,
                listprovider,
                null, null);
      case START_DATE:
      case START_TIME:
        if (StringUtils.isBlank(pattern)) {
          throw new IllegalArgumentException(
                  "For temporal metadata field " + inputID + " of type " + type + " there needs to be a pattern.");
        }

        return new MetadataField(
                inputID,
                outputID,
                label,
                readOnly,
                required,
                null,
                null,
                type,
                null,
                null,
                order,
                namespace,
                listprovider,
                pattern,
                null);
      default:
        throw new IllegalArgumentException("Unknown metadata type! " + type);
    }
  }

  public String getCollectionID() {
    return collectionID;
  }

  public void setCollectionID(final String collectionID) {
    this.collectionID = collectionID;
  }

  public String getInputID() {
    return inputID;
  }

  public String getLabel() {
    return label;
  }

  public String getListprovider() {
    return listprovider;
  }

  public String getNamespace() {
    return namespace;
  }

  public Integer getOrder() {
    return order;
  }

  /**
   * @return The outputID if available, inputID if it is missing.
   */
  public String getOutputID() {
    if (outputID != null) {
      return outputID;
    }
    return inputID;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(final String pattern) {
    this.pattern = pattern;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public void setDelimiter(final String delimiter) {
    this.delimiter = delimiter;
  }

  public void setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public boolean isRequired() {
    return required;
  }

  public void setUpdated(final boolean updated) {
    this.updated = updated;
  }

  public Type getType() {
    return type;
  }

  public void setType(final Type type) {
    this.type = type;
  }

  public void setDifferentValues() {
    value = null;
    hasDifferentValues = true;
  }

  public Boolean hasDifferentValues() {
    return hasDifferentValues;
  }

  public MetadataField readOnlyCopy() {
    final MetadataField metadataField = new MetadataField(this);
    metadataField.setReadOnly(true);
    return metadataField;
  }
}
