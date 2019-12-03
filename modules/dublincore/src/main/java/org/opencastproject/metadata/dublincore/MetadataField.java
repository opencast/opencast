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

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * This is a generic and very abstract view of a certain field/property in a metadata catalog. The main purpose of this
 * class is to have a generic access to the variety of information stored in metadata catalogs.
 *
 * @param <A>
 *          Defines the type of the metadata value
 */
public class MetadataField<A> {

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
  private Opt<String> collectionID;
  /** The format to use for temporal date properties. */
  private Opt<String> pattern = Opt.none();
  /** The delimiter used to display and parse list values. */
  private Opt<String> delimiter = Opt.none();
  /** The id of the field used to identify it in the dublin core. */
  private String inputID;
  /** The i18n id for the label to show the property. */
  private String label;
  /** The provider to populate the property with. */
  private Opt<String> listprovider = Opt.none();
  /** The optional namespace of the field used if a field can be found in more than one namespace */
  private Opt<String> namespace;
  /**
   * In the order of properties where this property should be oriented in the UI i.e. 0 means the property should come
   * first, 1 means it should come second etc.
   */
  private Opt<Integer> order;
  /** The optional id of the field used to output for the ui, if not present will assume the same as the inputID. */
  private final Opt<String> outputID;
  /** Whether the property should not be edited. */
  private boolean readOnly;
  /** Whether the property is required to update the metadata. */
  private boolean required;
  /** The type of the metadata for example text, date etc. */
  private Type type;

  private Opt<A> value;
  private Opt<Boolean> translatable;
  private boolean updated = false;
  private Opt<Map<String, String>> collection;

  // this can only be true if the metadata field is representing multiple events with different values
  private Opt<Boolean> hasDifferentValues = Opt.none();

  /**
   * Copy constructor
   *
   * @param other
   *          Other metadata field
   */
  public MetadataField(final MetadataField<A> other) {
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
   * @throws IllegalArgumentException
   *           if the id, label, type, valueToJSON parameters is/are null
   */
  private MetadataField(
          final String inputID,
          final Opt<String> outputID,
          final String label,
          final boolean readOnly,
          final boolean required,
          final A value,
          final Opt<Boolean> translatable,
          final Type type,
          final Opt<Map<String, String>> collection,
          final Opt<String> collectionID,
          final Opt<Integer> order,
          final Opt<String> namespace) throws IllegalArgumentException {
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
    if (value == null)
      this.value = Opt.none();
    else
      this.value = Opt.some(value);
    this.translatable = translatable;
    this.type = type;
    this.collection = collection;
    this.collectionID = collectionID;
    this.order = order;
    this.namespace = namespace;
  }

  /**
   * Set the option of a limited list of possible values.
   *
   * @param collection
   *          The option of a limited list of possible values
   */
  public void setCollection(final Opt<Map<String, String>> collection) {
    if (collection == null)
      this.collection = Opt.none();
    else {
      this.collection = collection;
    }
  }

  public Opt<Map<String, String>> getCollection() {
    return collection;
  }

  public Opt<A> getValue() {
    return value;
  }

  public Opt<Boolean> isTranslatable() {
    return translatable;
  }

  public boolean isUpdated() {
    return updated;
  }

  public void setValue(final A value) {
    setValue(value, true);
  }

  public void setValue(final A value, final boolean setUpdated) {
    if (value == null) {
      this.value = Opt.none();
    } else {
      this.value = Opt.some(value);
    }

    if (setUpdated) {
      this.updated = true;
    }
  }

  public void setIsTranslatable(final Opt<Boolean> translatable) {
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

  /**
   * Create a metadata field based on a {@link Boolean}.
   *
   * @param inputID
   *          The identifier of the new metadata field
   * @param label
   *          The label of the new metadata field
   * @param readOnly
   *          Define if the new metadata is or not a readonly field
   * @param required
   *          Define if the new metadata field is or not required
   * @param order
   *          The ui order for the new field, 0 at the top and progressively down from there.
   * @return The new metadata field
   *
   */
  private static MetadataField<Boolean> createBooleanMetadata(final String inputID, final Opt<String> outputID, final String label,
          final boolean readOnly, final boolean required, final Opt<Integer> order, final Opt<String> namespace) {

    return new MetadataField<>(inputID, outputID, label, readOnly, required, null, Opt.none(), Type.BOOLEAN,
            Opt.none(), Opt.none(), order, namespace);
  }

  /**
   * Create a metadata field based on a {@link Date}.
   *
   * @param inputID
   *          The identifier of the new metadata field
   * @param label
   *          The label of the new metadata field
   * @param readOnly
   *          Define if the new metadata is or not a readonly field
   * @param required
   *          Define if the new metadata field is or not required
   * @param pattern
   *          The date pattern for {@link SimpleDateFormat}.
   * @param order
   *          The ui order for the new field, 0 at the top and progressively down from there.
   * @return The new metadata field
   *
   */
  public static MetadataField<Date> createDateMetadata(final String inputID, final Opt<String> outputID, final String label,
          final boolean readOnly, final boolean required, final String pattern, final Opt<Integer> order, final Opt<String> namespace) {

    final MetadataField<Date> dateField = new MetadataField<>(inputID, outputID, label, readOnly, required, null, Opt.none(),
            Type.DATE, Opt.none(), Opt.none(), order, namespace);
    if (StringUtils.isNotBlank(pattern)) {
      dateField.setPattern(Opt.some(pattern));
    }
    return dateField;
  }

  public static MetadataField<String> createDurationMetadataField(final String inputID, final Opt<String> outputID, final String label,
          final boolean readOnly, final boolean required, final Opt<Integer> order, final Opt<String> namespace) {
    return createDurationMetadataField(inputID, outputID, label, readOnly, required, Opt.none(),
            Opt.none(), Opt.none(), order, namespace);
  }

  private static MetadataField<String> createDurationMetadataField(final String inputID, final Opt<String> outputID, final String label,
          final boolean readOnly, final boolean required, final Opt<Boolean> isTranslatable, final Opt<Map<String, String>> collection,
          final Opt<String> collectionId, final Opt<Integer> order, final Opt<String> namespace) {

    return new MetadataField<>(inputID, outputID, label, readOnly, required, "", isTranslatable, Type.DURATION,
            collection, collectionId, order, namespace);
  }

  /**
   * Create a metadata field of type mixed iterable String
   *
   * @param inputID
   *          The identifier of the new metadata field
   * @param label
   *          The label of the new metadata field
   * @param readOnly
   *          Define if the new metadata field can be or not edited
   * @param required
   *          Define if the new metadata field is or not required
   * @param isTranslatable
   *          If the field value is not human readable and should be translated before
   * @param collection
   *          If the field has a limited list of possible value, the option should contain this one. Otherwise it should
   *          be none.
   * @param order
   *          The ui order for the new field, 0 at the top and progressively down from there.
   * @return the new metadata field
   */
  private static MetadataField<Iterable<String>> createMixedIterableStringMetadataField(final String inputID,
          final Opt<String> outputID, final String label, final boolean readOnly, final boolean required, final Opt<Boolean> isTranslatable,
          final Opt<Map<String, String>> collection, final Opt<String> collectionId, final Opt<String> delimiter, final Opt<Integer> order,
          final Opt<String> namespace) {

    final MetadataField<Iterable<String>> mixedField = new MetadataField<>(inputID, outputID, label, readOnly, required,
             new ArrayList<>(), isTranslatable, Type.MIXED_TEXT, collection, collectionId, order, namespace);
     mixedField.setDelimiter(delimiter);
     return mixedField;
  }

  /**
   * Create a metadata field of type iterable String
   *
   * @param inputID
   *          The identifier of the new metadata field
   * @param label
   *          The label of the new metadata field
   * @param readOnly
   *          Define if the new metadata field can be or not edited
   * @param required
   *          Define if the new metadata field is or not required
   * @param isTranslatable
   *          If the field value is not human readable and should be translated before
   * @param collection
   *          If the field has a limited list of possible value, the option should contain this one. Otherwise it should
   *          be none.
   * @param order
   *          The ui order for the new field, 0 at the top and progressively down from there.
   * @return the new metadata field
   */
  public static MetadataField<Iterable<String>> createIterableStringMetadataField(final String inputID, final Opt<String> outputID,
          final String label, final boolean readOnly, final boolean required, final Opt<Boolean> isTranslatable,
          final Opt<Map<String, String>> collection, final Opt<String> collectionId, final Opt<String> delimiter, final Opt<Integer> order,
          final Opt<String> namespace) {

    final MetadataField<Iterable<String>> iterableField = new MetadataField<>(inputID, outputID, label, readOnly, required,
            new ArrayList<>(), isTranslatable, Type.ITERABLE_TEXT, collection, collectionId,
            order, namespace);
    iterableField.setDelimiter(delimiter);
    return iterableField;
  }

  private static MetadataField<Long> createLongMetadataField(final String inputID, final Opt<String> outputID, final String label,
          final boolean readOnly, final boolean required, final Opt<Boolean> isTranslatable, final Opt<Map<String, String>> collection,
          final Opt<String> collectionId, final Opt<Integer> order, final Opt<String> namespace) {

    return new MetadataField<>(inputID, outputID, label, readOnly, required, 0L, isTranslatable, Type.TEXT,
            collection, collectionId, order, namespace);
  }

  private static MetadataField<String> createTemporalMetadata(final String inputID, final Opt<String> outputID, final String label,
          final boolean readOnly, final boolean required, final String pattern, final Type type,
          final Opt<Integer> order, final Opt<String> namespace) {
    if (StringUtils.isBlank(pattern)) {
      throw new IllegalArgumentException(
              "For temporal metadata field " + inputID + " of type " + type + " there needs to be a pattern.");
    }

    final MetadataField<String> temporalStart = new MetadataField<>(inputID, outputID, label, readOnly, required, null,
            Opt.none(), type, Opt.none(), Opt.none(), order, namespace);
    temporalStart.setPattern(Opt.some(pattern));

    return temporalStart;
  }

  public static MetadataField<String> createTemporalStartDateMetadata(final String inputID, final Opt<String> outputID,
          final String label, final boolean readOnly, final boolean required, final String pattern, final Opt<Integer> order,
          final Opt<String> namespace) {
    return createTemporalMetadata(inputID, outputID, label, readOnly, required, pattern, Type.START_DATE,
            order, namespace);
  }

  public static MetadataField<String> createTemporalStartTimeMetadata(final String inputID, final Opt<String> outputID,
          final String label, final boolean readOnly, final boolean required, final String pattern, final Opt<Integer> order,
          final Opt<String> namespace) {
    return createTemporalMetadata(inputID, outputID, label, readOnly, required, pattern, Type.START_TIME,
            order, namespace);
  }

  /**
   * Create a metadata field of type String with a single line in the front end.
   *
   * @param inputID
   *          The identifier of the new metadata field
   * @param label
   *          The label of the new metadata field
   * @param readOnly
   *          Define if the new metadata field can be or not edited
   * @param required
   *          Define if the new metadata field is or not required
   * @param isTranslatable
   *          If the field value is not human readable and should be translated before
   * @param collection
   *          If the field has a limited list of possible value, the option should contain this one. Otherwise it should
   *          be none.
   * @param order
   *          The ui order for the new field, 0 at the top and progressively down from there.
   * @return the new metadata field
   */
  public static MetadataField<String> createTextMetadataField(final String inputID, final Opt<String> outputID, final String label,
          final boolean readOnly, final boolean required, final Opt<Boolean> isTranslatable, final Opt<Map<String, String>> collection,
          final Opt<String> collectionId, final Opt<Integer> order, final Opt<String> namespace) {
    return createTextAnyMetadataField(inputID, outputID, label, readOnly, required, isTranslatable, collection,
            collectionId, order, Type.TEXT, namespace);
  }

  /**
   * Create a metadata field of type String with a single line in the front end which can be ordered and filtered.
   *
   * @param inputID
   *          The identifier of the new metadata field
   * @param label
   *          The label of the new metadata field
   * @param readOnly
   *          Define if the new metadata field can be or not edited
   * @param required
   *          Define if the new metadata field is or not required
   * @param isTranslatable
   *          If the field value is not human readable and should be translated before
   * @param collection
   *          If the field has a limited list of possible value, the option should contain this one. Otherwise it should
   *          be none.
   * @param order
   *          The ui order for the new field, 0 at the top and progressively down from there.
   * @return the new metadata field
   */
  private static MetadataField<String> createOrderedTextMetadataField(final String inputID, final Opt<String> outputID,
          final String label, final boolean readOnly, final boolean required, final Opt<Boolean> isTranslatable,
          final Opt<Map<String, String>> collection, final Opt<String> collectionId, final Opt<Integer> order, final Opt<String> namespace) {
    return createTextAnyMetadataField(inputID, outputID, label, readOnly, required, isTranslatable, collection,
            collectionId, order, Type.ORDERED_TEXT, namespace);
  }


  /**
   * Create a metadata field of type String with many lines in the front end.
   *
   * @param inputID
   *          The identifier of the new metadata field
   * @param label
   *          The label of the new metadata field
   * @param readOnly
   *          Define if the new metadata field can be or not edited
   * @param required
   *          Define if the new metadata field is or not required
   * @param isTranslatable
   *          If the field value is not human readable and should be translated before
   * @param collection
   *          If the field has a limited list of possible value, the option should contain this one. Otherwise it should
   *          be none.
   * @param order
   *          The ui order for the new field, 0 at the top and progressively down from there.
   * @return the new metadata field
   */
  public static MetadataField<String> createTextLongMetadataField(final String inputID, final Opt<String> outputID, final String label,
          final boolean readOnly, final boolean required, final Opt<Boolean> isTranslatable, final Opt<Map<String, String>> collection,
          final Opt<String> collectionId, final Opt<Integer> order, final Opt<String> namespace) {
    return createTextAnyMetadataField(inputID, outputID, label, readOnly, required, isTranslatable, collection,
            collectionId, order, Type.TEXT_LONG, namespace);
  }

  /**
   * Create a metadata field of type String specifying the type for the front end.
   *
   * @param inputID
   *          The identifier of the new metadata field
   * @param label
   *          The label of the new metadata field
   * @param readOnly
   *          Define if the new metadata field can be or not edited
   * @param required
   *          Define if the new metadata field is or not required
   * @param isTranslatable
   *          If the field value is not human readable and should be translated before
   * @param collection
   *          If the field has a limited list of possible value, the option should contain this one. Otherwise it should
   *          be none.
   * @param order
   *          The ui order for the new field, 0 at the top and progressively down from there.
   * @param type
   *          The metadata field type as defined in {@link MetadataField.Type}
   * @return the new metadata field
   */
  private static MetadataField<String> createTextAnyMetadataField(final String inputID, final Opt<String> outputID, final String label,
          final boolean readOnly, final boolean required, final Opt<Boolean> isTranslatable, final Opt<Map<String, String>> collection,
          final Opt<String> collectionId, final Opt<Integer> order, final Type type, final Opt<String> namespace) {

    return new MetadataField<>(inputID, outputID, label, readOnly, required, "", isTranslatable, type,
            collection, collectionId, order, namespace);
  }

  public static MetadataField createMetadataField(final Map<String,String> configuration) {

    final String inputID = configuration.get(CONFIG_INPUT_ID_KEY);
    final String label = configuration.get(CONFIG_LABEL_KEY);

    final Opt<String> collectionID = Opt.nul(configuration.get(CONFIG_COLLECTION_ID_KEY));
    final Opt<String> delimiter = Opt.nul(configuration.get(CONFIG_DELIMITER_KEY));
    final Opt<String> outputID = Opt.nul(configuration.get(CONFIG_OUTPUT_ID_KEY));
    final Opt<String> listprovider = Opt.nul(configuration.get(CONFIG_LIST_PROVIDER_KEY));
    final Opt<String> namespace = Opt.nul(configuration.get(CONFIG_NAMESPACE_KEY));

    final Type type = configuration.containsKey(CONFIG_TYPE_KEY)
            ? Type.valueOf(configuration.get(CONFIG_TYPE_KEY).toUpperCase()) : null;
    final boolean required = configuration.containsKey(CONFIG_REQUIRED_KEY) && Boolean
            .parseBoolean(configuration.get(CONFIG_REQUIRED_KEY).toUpperCase());
    final boolean readOnly = configuration.containsKey(CONFIG_READ_ONLY_KEY) && Boolean
            .parseBoolean(configuration.get(CONFIG_READ_ONLY_KEY).toUpperCase());

    final String pattern = configuration.getOrDefault(CONFIG_PATTERN_KEY, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    Opt<Integer> order = Opt.none();
    if (configuration.containsKey(CONFIG_ORDER_KEY)) {
      try {
        order = Opt.some(Integer.parseInt(configuration.get(CONFIG_ORDER_KEY)));
      } catch (final NumberFormatException e) {
        logger.warn("Unable to parse order value {} of metadata field {}", configuration.get(CONFIG_ORDER_KEY),
                inputID, e);
      }
    }

    final MetadataField metadataField = createMetadataField(inputID, outputID, label, readOnly, required, Opt.none(), type,
            Opt.none(), collectionID, order, namespace, delimiter, pattern);
    metadataField.setListprovider(listprovider);
    return metadataField;
  }

  public static MetadataField createMetadataField(final String inputID, final Opt<String> outputID, final String label, final boolean readOnly,
          final boolean required, final Opt<Boolean> translatable, final Type type, final Opt<Map<String, String>> collection,
          final Opt<String> collectionID, final Opt<Integer> order, final Opt<String> namespace, final Opt<String> delimiter, final String pattern) {

    switch (type) {
      case BOOLEAN:
        return createBooleanMetadata(inputID, outputID, label, readOnly, required, order, namespace);
      case DATE:
        return createDateMetadata(inputID, outputID, label, readOnly, required, pattern, order, namespace);
      case DURATION:
        return createDurationMetadataField(inputID, outputID, label, readOnly, required, translatable,
                collection, collectionID, order, namespace);
      case ITERABLE_TEXT:
        return createIterableStringMetadataField(inputID, outputID, label, readOnly, required, translatable,
                collection, collectionID, delimiter, order, namespace);
      case MIXED_TEXT:
        return createMixedIterableStringMetadataField(inputID, outputID, label, readOnly, required,
                translatable, collection, collectionID, delimiter, order, namespace);
      case LONG:
        return createLongMetadataField(inputID, outputID, label, readOnly, required, translatable,
                collection, collectionID, order, namespace);
      case TEXT:
        return createTextMetadataField(inputID, outputID, label, readOnly, required, translatable, collection,
                collectionID, order, namespace);
      case TEXT_LONG:
        return createTextLongMetadataField(inputID, outputID, label, readOnly, required, translatable, collection,
                collectionID, order, namespace);
      case START_DATE:
        return createTemporalStartDateMetadata(inputID, outputID, label, readOnly, required, pattern, order, namespace);
      case START_TIME:
        return createTemporalStartTimeMetadata(inputID, outputID, label, readOnly, required, pattern, order, namespace);
      case ORDERED_TEXT:
        return createOrderedTextMetadataField(inputID, outputID, label, readOnly, required, translatable, collection,
                collectionID, order, namespace);
      default:
        throw new IllegalArgumentException("Unknown metadata type! " + type);
    }
  }

  public Opt<String> getCollectionID() {
    return collectionID;
  }

  public void setCollectionID(final Opt<String> collectionID) {
    this.collectionID = collectionID;
  }

  public String getInputID() {
    return inputID;
  }

  public void setInputId(final String inputID) {
    this.inputID = inputID;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(final String label) {
    this.label = label;
  }

  public Opt<String> getListprovider() {
    return listprovider;
  }

  public void setListprovider(final Opt<String> listprovider) {
    this.listprovider = listprovider;
  }

  public Opt<String> getNamespace() {
    return namespace;
  }

  public void setNamespace(final Opt<String> namespace) {
    this.namespace = namespace;
  }

  public Opt<Integer> getOrder() {
    return order;
  }

  public void setOrder(final Opt<Integer> order) {
    this.order = order;
  }

  /**
   * @return The outputID if available, inputID if it is missing.
   */
  public String getOutputID() {
    if (outputID.isSome()) {
      return outputID.get();
    } else {
      return inputID;
    }
  }

  public Opt<String> getPattern() {
    return pattern;
  }

  public void setPattern(final Opt<String> pattern) {
    this.pattern = pattern;
  }

  public Opt<String> getDelimiter() {
    return delimiter;
  }

  public void setDelimiter(final Opt<String> delimiter) {
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

  public void setRequired(final boolean required) {
    this.required = required;
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
    value = Opt.none();
    hasDifferentValues = Opt.some(true);
  }

  public Opt<Boolean> hasDifferentValues() {
    return hasDifferentValues;
  }
}
