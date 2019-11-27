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

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;
import com.google.common.collect.Iterables;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  private static final String PATTERN_DURATION = "HH:mm:ss";

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

  /* Keys for the different properties of the metadata JSON Object */
  private static final String JSON_KEY_ID = "id";
  private static final String JSON_KEY_LABEL = "label";
  private static final String JSON_KEY_READONLY = "readOnly";
  private static final String JSON_KEY_REQUIRED = "required";
  private static final String JSON_KEY_TYPE = "type";
  private static final String JSON_KEY_VALUE = "value";
  private static final String JSON_KEY_COLLECTION = "collection";
  private static final String JSON_KEY_TRANSLATABLE = "translatable";
  private static final String JSON_KEY_DELIMITER = "delimiter";
  private static final String JSON_KEY_DIFFERENT_VALUES = "differentValues";

  /**
   * Possible types for the metadata field. The types are used in the frontend and backend to know how the metadata
   * fields should be formatted (if needed).
   */
  public enum Type {
    BOOLEAN, DATE, DURATION, ITERABLE_TEXT, MIXED_TEXT, ORDERED_TEXT, LONG, START_DATE, START_TIME, TEXT, TEXT_LONG
  }

  public enum JsonType {
    BOOLEAN, DATE, NUMBER, TEXT, MIXED_TEXT, ORDERED_TEXT, TEXT_LONG, TIME
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
  /** The type of the metadata for the json to use example text, date, time, number etc. */
  private JsonType jsonType;

  private Opt<A> value;
  private Opt<Boolean> translatable;
  private boolean updated = false;
  private Opt<Map<String, String>> collection;
  private final Fn<Opt<A>, JValue> valueToJSON;
  private final Fn<Object, A> jsonToValue;

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
    this.jsonType = other.jsonType;
    this.collection = other.collection;
    this.collectionID = other.collectionID;
    this.valueToJSON = other.valueToJSON;
    this.jsonToValue = other.jsonToValue;
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
   * @param valueToJSON
   *          Function to format the metadata field value to a JSON value.
   * @param jsonToValue
   *          Function to parse the JSON value of the metadata field.
   * @throws IllegalArgumentException
   *           if the id, label, type, valueToJSON or/and jsonToValue parameters is/are null
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
          final JsonType jsonType,
          final Opt<Map<String, String>> collection,
          final Opt<String> collectionID,
          final Fn<Opt<A>, JValue> valueToJSON,
          final Fn<Object, A> jsonToValue,
          final Opt<Integer> order,
          final Opt<String> namespace) throws IllegalArgumentException {
    if (valueToJSON == null)
      throw new IllegalArgumentException("The function 'valueToJSON' must not be null.");
    if (jsonToValue == null)
      throw new IllegalArgumentException("The function 'jsonToValue' must not be null.");
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
    this.jsonType = jsonType;
    this.collection = collection;
    this.collectionID = collectionID;
    this.valueToJSON = valueToJSON;
    this.jsonToValue = jsonToValue;
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

  public JObject toJSON() {
    final Map<String, Field> values = new HashMap<>();
    values.put(JSON_KEY_ID, f(JSON_KEY_ID, v(getOutputID(), Jsons.BLANK)));
    values.put(JSON_KEY_LABEL, f(JSON_KEY_LABEL, v(label, Jsons.BLANK)));
    values.put(JSON_KEY_VALUE, f(JSON_KEY_VALUE, valueToJSON.apply(value)));
    values.put(JSON_KEY_TYPE, f(JSON_KEY_TYPE, v(jsonType.toString().toLowerCase(), Jsons.BLANK)));
    values.put(JSON_KEY_READONLY, f(JSON_KEY_READONLY, v(readOnly)));
    values.put(JSON_KEY_REQUIRED, f(JSON_KEY_REQUIRED, v(required)));

    if (hasDifferentValues.isSome())
      values.put(JSON_KEY_DIFFERENT_VALUES, f(JSON_KEY_DIFFERENT_VALUES, v(hasDifferentValues.get())));

    if (collection.isSome())
      values.put(JSON_KEY_COLLECTION, f(JSON_KEY_COLLECTION, mapToJSON(collection.get())));
    else if (collectionID.isSome())
      values.put(JSON_KEY_COLLECTION, f(JSON_KEY_COLLECTION, v(collectionID.get())));
    if (translatable.isSome())
      values.put(JSON_KEY_TRANSLATABLE, f(JSON_KEY_TRANSLATABLE, v(translatable.get())));
    if (delimiter.isSome())
      values.put(JSON_KEY_DELIMITER, f(JSON_KEY_DELIMITER, v(delimiter.get())));
    return obj(values);
  }

  public void fromJSON(final Object json) {
    this.setValue(jsonToValue.apply(json));
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

    final Fn<Opt<Boolean>, JValue> booleanToJson = new Fn<Opt<Boolean>, JValue>() {
      @Override
      public JValue apply(final Opt<Boolean> value) {
        if (value.isNone())
          return Jsons.BLANK;
        else {
          return v(value.get(), Jsons.BLANK);
        }
      }
    };

    final Fn<Object, Boolean> jsonToBoolean = new Fn<Object, Boolean>() {
      @Override
      public Boolean apply(final Object value) {
        if (value instanceof Boolean) {
          return (Boolean) value;
        }
        final String stringValue = value.toString();
        if (StringUtils.isBlank(stringValue)) {
          return null;
        }
        return Boolean.parseBoolean(stringValue);
      }
    };

    return new MetadataField<>(inputID, outputID, label, readOnly, required, null, Opt.none(), Type.BOOLEAN, JsonType.BOOLEAN,
            Opt.none(), Opt.none(), booleanToJson, jsonToBoolean, order, namespace);
  }

  /**
   * Creates a copy of a {@link MetadataField} and sets the value based upon a string.
   *
   * @param oldField
   *          The field whose other values such as ids, label etc. will be copied.
   * @param value
   *          The value that will be interpreted as being from a JSON value.
   * @return A new {@link MetadataField} with the value set
   */
  public static MetadataField<?> copyMetadataFieldWithValue(final MetadataField<?> oldField, final String value) {
    final MetadataField<?> newField = new MetadataField(oldField);
    newField.fromJSON(value);
    return newField;
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
    final SimpleDateFormat dateFormat = getSimpleDateFormatter(pattern);

    final Fn<Opt<Date>, JValue> dateToJSON = new Fn<Opt<Date>, JValue>() {
      @Override
      public JValue apply(final Opt<Date> date) {
        if (date.isNone())
          return Jsons.BLANK;
        else {
          return v(dateFormat.format(date.get()), Jsons.BLANK);
        }
      }
    };

    final Fn<Object, Date> jsonToDate = new Fn<Object, Date>() {
      @Override
      public Date apply(final Object value) {
        try {
          final String date = (String) value;

          if (StringUtils.isBlank(date))
            return null;

          return dateFormat.parse(date);
        } catch (final java.text.ParseException e) {
          logger.error("Not able to parse date {}: {}", value, e.getMessage());
          return null;
        }
      }
    };

    final MetadataField<Date> dateField = new MetadataField<>(inputID, outputID, label, readOnly, required, null, Opt.none(),
            Type.DATE, JsonType.DATE, Opt.none(), Opt.none(), dateToJSON, jsonToDate,
            order, namespace);
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

    final Fn<Opt<String>, JValue> periodToJSON = new Fn<Opt<String>, JValue>() {
      @Override
      public JValue apply(final Opt<String> value) {
        if (value == null || value.isEmpty()) {
          return v("");
        }
        long returnValue = 0L;
        final DCMIPeriod period = EncodingSchemeUtils.decodePeriod(value.get());
        if (period != null && period.hasStart() && period.hasEnd()) {
          returnValue = period.getEnd().getTime() - period.getStart().getTime();
        } else {
          try {
            returnValue = Long.parseLong(value.get());
          } catch (final NumberFormatException e) {
            logger.debug("Unable to parse duration '{}' as either period or millisecond duration.", value.get());
          }
        }
        return v(DurationFormatUtils.formatDuration(returnValue, PATTERN_DURATION));
      }
    };

    final Fn<Object, String> jsonToPeriod = new Fn<Object, String>() {
      @Override
      public String apply(final Object value) {
        if (!(value instanceof String)) {
          logger.warn("The given value for duration can not be parsed.");
          return "";
        }

        final String duration = (String) value;
        final String[] durationParts = duration.split(":");
        if (durationParts.length < 3)
          return null;
        final long hours = Long.parseLong(durationParts[0]);
        final long minutes = Long.parseLong(durationParts[1]);
        final long seconds = Long.parseLong(durationParts[2]);

        final long returnValue = ((hours * 60 + minutes) * 60 + seconds) * 1000;

        return Long.toString(returnValue);
      }
    };
    return new MetadataField<>(inputID, outputID, label, readOnly, required, "", isTranslatable, Type.DURATION,
            JsonType.TEXT, collection, collectionId, periodToJSON, jsonToPeriod, order, namespace);
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

    final Fn<Opt<Iterable<String>>, JValue> iterableToJSON = new Fn<Opt<Iterable<String>>, JValue>() {
      @Override
      public JValue apply(final Opt<Iterable<String>> value) {
        if (value.isNone())
          return arr();

        final Object val = value.get();
        final List<JValue> list = new ArrayList<>();

        // The current value is just an iterable string.
        for (final Object v : value.get()) {
          list.add(v(v, Jsons.BLANK));
        }
        return arr(list);
      }
    };

    final Fn<Object, Iterable<String>> jsonToIterable = new Fn<Object, Iterable<String>>() {
      @Override
      public Iterable<String> apply(final Object arrayIn) {
        final JSONParser parser = new JSONParser();
        final JSONArray array;
        if (arrayIn instanceof String) {
          try {
            array = (JSONArray) parser.parse((String) arrayIn);
          } catch (final ParseException e) {
            throw new IllegalArgumentException("Unable to parse Mixed Iterable value into a JSONArray:", e);
          }
        } else {
          array = (JSONArray) arrayIn;
        }

        if (array == null)
          return new ArrayList<>();
        final String[] arrayOut = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
          arrayOut[i] = (String) array.get(i);
        }
        return Arrays.asList(arrayOut);
      }

    };

     final MetadataField<Iterable<String>> mixedField = new MetadataField<>(inputID, outputID, label, readOnly, required,
             new ArrayList<>(), isTranslatable, Type.MIXED_TEXT, JsonType.MIXED_TEXT, collection, collectionId,
             iterableToJSON, jsonToIterable, order, namespace);
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

    final Fn<Opt<Iterable<String>>, JValue> iterableToJSON = new Fn<Opt<Iterable<String>>, JValue>() {
      @Override
      public JValue apply(final Opt<Iterable<String>> value) {
        if (value.isNone())
          return arr();

        final Object val = value.get();
        final List<JValue> list = new ArrayList<>();

        // The current value is just an iterable string.
        for (final Object v : value.get()) {
          list.add(v(v, Jsons.BLANK));
        }
        return arr(list);
      }
    };

    final Fn<Object, Iterable<String>> jsonToIterable = new Fn<Object, Iterable<String>>() {
      @Override
      public Iterable<String> apply(final Object arrayIn) {
        final JSONArray array = (JSONArray) arrayIn;
        if (array == null)
          return null;
        final String[] arrayOut = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
          arrayOut[i] = (String) array.get(i);
        }
        return Arrays.asList(arrayOut);
      }

    };

    final MetadataField<Iterable<String>> iterableField = new MetadataField<>(inputID, outputID, label, readOnly, required,
            new ArrayList<>(), isTranslatable, Type.ITERABLE_TEXT, JsonType.TEXT, collection, collectionId,
            iterableToJSON, jsonToIterable, order, namespace);
    iterableField.setDelimiter(delimiter);
    return iterableField;
  }

  private static MetadataField<Long> createLongMetadataField(final String inputID, final Opt<String> outputID, final String label,
          final boolean readOnly, final boolean required, final Opt<Boolean> isTranslatable, final Opt<Map<String, String>> collection,
          final Opt<String> collectionId, final Opt<Integer> order, final Opt<String> namespace) {

    final Fn<Opt<Long>, JValue> longToJSON = new Fn<Opt<Long>, JValue>() {
      @Override
      public JValue apply(final Opt<Long> value) {
        if (value.isNone())
          return Jsons.BLANK;
        else
          return v(value.get().toString());
      }
    };

    final Fn<Object, Long> jsonToLong = new Fn<Object, Long>() {
      @Override
      public Long apply(final Object value) {
        if (!(value instanceof String)) {
          logger.warn("The given value for Long can not be parsed.");
          return 0L;
        }
        final String longString = (String) value;
        return Long.parseLong(longString);
      }
    };

    return new MetadataField<>(inputID, outputID, label, readOnly, required, 0L, isTranslatable, Type.TEXT, JsonType.NUMBER,
            collection, collectionId, longToJSON, jsonToLong, order, namespace);
  }

  private static MetadataField<String> createTemporalMetadata(final String inputID, final Opt<String> outputID, final String label,
          final boolean readOnly, final boolean required, final String pattern, final Type type, final JsonType jsonType,
          final Opt<Integer> order, final Opt<String> namespace) {
    if (StringUtils.isBlank(pattern)) {
      throw new IllegalArgumentException(
              "For temporal metadata field " + inputID + " of type " + type + " there needs to be a pattern.");
    }

    final SimpleDateFormat dateFormat = getSimpleDateFormatter(pattern);

    final Fn<Object, String> jsonToDateString = new Fn<Object, String>() {
      @Override
      public String apply(final Object value) {
        final String date = (String) value;

        if (StringUtils.isBlank(date))
          return "";

        try {
          dateFormat.parse(date);
        } catch (final java.text.ParseException e) {
          logger.error("Not able to parse date string {}: {}", value, getMessage(e));
          return null;
        }

        return date;
      }
    };

    final Fn<Opt<String>, JValue> dateToJSON = new Fn<Opt<String>, JValue>() {
      @Override
      public JValue apply(final Opt<String> periodEncodedString) {
        if (periodEncodedString.isNone() || StringUtils.isBlank(periodEncodedString.get())) {
          return Jsons.BLANK;
        }

        // Try to parse the metadata as DCIM metadata.
        final DCMIPeriod p = EncodingSchemeUtils.decodePeriod(periodEncodedString.get());
        if (p != null) {
          return v(dateFormat.format(p.getStart()), Jsons.BLANK);
        }

        // Not DCIM metadata so it might already be formatted (given from the front and is being returned there
        try {
          dateFormat.parse(periodEncodedString.get());
          return v(periodEncodedString.get(), Jsons.BLANK);
        } catch (final Exception e) {
          logger.error(
                  "Unable to parse temporal metadata '{}' as either DCIM data or a formatted date using pattern {} because:",
                  periodEncodedString.get(), pattern, e);
          throw new IllegalArgumentException(e);
        }
      }
    };

    final MetadataField<String> temporalStart = new MetadataField<>(inputID, outputID, label, readOnly, required, null,
            Opt.none(), type, jsonType, Opt.none(), Opt.none(), dateToJSON,
            jsonToDateString, order, namespace);
    temporalStart.setPattern(Opt.some(pattern));

    return temporalStart;
  }

  public static MetadataField<String> createTemporalStartDateMetadata(final String inputID, final Opt<String> outputID,
          final String label, final boolean readOnly, final boolean required, final String pattern, final Opt<Integer> order,
          final Opt<String> namespace) {
    return createTemporalMetadata(inputID, outputID, label, readOnly, required, pattern, Type.START_DATE,
            JsonType.DATE, order, namespace);
  }

  public static MetadataField<String> createTemporalStartTimeMetadata(final String inputID, final Opt<String> outputID,
          final String label, final boolean readOnly, final boolean required, final String pattern, final Opt<Integer> order,
          final Opt<String> namespace) {
    return createTemporalMetadata(inputID, outputID, label, readOnly, required, pattern, Type.START_TIME,
            JsonType.TIME, order, namespace);
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
            collectionId, order, Type.TEXT, JsonType.TEXT, namespace);
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
            collectionId, order, Type.ORDERED_TEXT, JsonType.ORDERED_TEXT, namespace);
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
            collectionId, order, Type.TEXT_LONG, JsonType.TEXT_LONG, namespace);
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
          final Opt<String> collectionId, final Opt<Integer> order, final Type type, final JsonType jsonType, final Opt<String> namespace) {

    final Fn<Opt<String>, JValue> stringToJSON = new Fn<Opt<String>, JValue>() {
      @Override
      public JValue apply(final Opt<String> value) {
        return v(value.getOr(""));
      }
    };

    final Fn<Object, String> jsonToString = new Fn<Object, String>() {
      @Override
      public String apply(final Object jsonValue) {
        if (jsonValue == null)
          return "";
        if (!(jsonValue instanceof String)) {
          logger.warn("Value cannot be parsed as String. Expecting type 'String', but received type '{}'.", jsonValue.getClass().getName());
          return null;
        }
        return (String) jsonValue;
      }
    };

    return new MetadataField<>(inputID, outputID, label, readOnly, required, "", isTranslatable, type, jsonType,
            collection, collectionId, stringToJSON, jsonToString, order, namespace);
  }

  /**
   * Turn a map into a {@link JObject} object
   *
   * @param map
   *          the source map
   * @return a new {@link JObject} generated with the map values
   */
  private static JObject mapToJSON(final Map<String, String> map) {
    if (map == null) {
      throw new IllegalArgumentException("Map must not be null!");
    }

    final List<Field> fields = new ArrayList<>();
    for (final Entry<String, String> item : map.entrySet()) {
      fields.add(f(item.getKey(), v(item.getValue(), Jsons.BLANK)));
    }
    return obj(fields);
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

  /**
   * Set value to a metadata field of unknown type
   */
  public static MetadataField setValueFromDCCatalog(final List<String> filteredValues, final MetadataField metadataField) {

    if (filteredValues.isEmpty()) {
      throw new IllegalArgumentException("Values cannot be empty");
    }

    if (filteredValues.size() > 1
            && metadataField.getType() != MetadataField.Type.MIXED_TEXT
            && metadataField.getType() != MetadataField.Type.ITERABLE_TEXT) {
      logger.warn("Cannot put multiple values into a single-value field, only the last value is used. {}",
              Arrays.toString(filteredValues.toArray()));
    }

    switch (metadataField.type) {
      case BOOLEAN:
        ((MetadataField<Boolean>)metadataField).setValue(Boolean.parseBoolean(Iterables.getLast(filteredValues)), false);
        break;
      case DATE:
        if (metadataField.getPattern().isNone()) {
          metadataField.setPattern(Opt.some("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        }
        ((MetadataField<Date>)metadataField).setValue(EncodingSchemeUtils.decodeDate(Iterables.getLast(filteredValues)), false);
        break;
      case DURATION:
        final String value = Iterables.getLast(filteredValues);
        final DCMIPeriod period = EncodingSchemeUtils.decodePeriod(value);
        final long longValue = period.getEnd().getTime() - period.getStart().getTime();
        ((MetadataField<String>)metadataField).setValue(Long.toString(longValue), false);
        break;
      case ITERABLE_TEXT:
      case MIXED_TEXT:
        ((MetadataField<Iterable<String>>)metadataField).setValue(filteredValues, false);
        break;
      case LONG:
        ((MetadataField<Long>)metadataField).setValue(Long.parseLong(Iterables.getLast(filteredValues)), false);
        break;
      case START_DATE:
        if (metadataField.getPattern().isNone()) {
          metadataField.setPattern(Opt.some("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        }
        ((MetadataField<String>)metadataField).setValue(Iterables.getLast(filteredValues), false);
        break;
      case TEXT:
      case ORDERED_TEXT:
      case TEXT_LONG:
        ((MetadataField<String>)metadataField).setValue(Iterables.getLast(filteredValues), false);
        break;
      default:
        throw new IllegalArgumentException("Unknown metadata type! " + metadataField.getType());
    }
    return metadataField;
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

  public JsonType getJsonType() {
    return jsonType;
  }

  public void setJsonType(final JsonType jsonType) {
    this.jsonType = jsonType;
  }

  public void setDifferentValues() {
    value = Opt.none();
    hasDifferentValues = Opt.some(true);
  }

  public Opt<Boolean> hasDifferentValues() {
    return hasDifferentValues;
  }
}
