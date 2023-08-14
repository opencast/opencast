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

import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

public final class MetadataJson {
  private static final Logger logger = LoggerFactory.getLogger(MetadataJson.class);

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
  private static final String KEY_METADATA_TITLE = "title";
  private static final String KEY_METADATA_FLAVOR = "flavor";
  private static final String KEY_METADATA_FIELDS = "fields";
  private static final String KEY_METADATA_LOCKED = "locked";

  /* Keys for the different properties of the metadata JSON Object */
  private static final String KEY_METADATA_ID = "id";
  private static final String KEY_METADATA_VALUE = "value";

  private static final String PATTERN_DURATION = "HH:mm:ss";

  /**
   * Turn a map into a {@link JObject} object
   *
   * @param map the source map
   * @return a new {@link JObject} generated with the map values
   */
  private static JObject mapToJson(final Map<String, String> map) {
    Objects.requireNonNull(map);
    final List<Field> fields = new ArrayList<>();
    for (final Map.Entry<String, String> item : map.entrySet()) {
      fields.add(f(item.getKey(), v(item.getValue(), Jsons.BLANK)));
    }
    return obj(fields);
  }

  public enum JsonType {
    BOOLEAN, DATE, NUMBER, TEXT, MIXED_TEXT, ORDERED_TEXT, TEXT_LONG, TIME
  }

  private MetadataJson() {
  }

  private static SimpleDateFormat getSimpleDateFormatter(final String pattern) {
    final SimpleDateFormat dateFormat;
    if (StringUtils.isNotBlank(pattern)) {
      dateFormat = new SimpleDateFormat(pattern);
    } else {
      dateFormat = new SimpleDateFormat();
    }
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormat;
  }

  private static <T> JValue valueToJson(final T rawValue, final MetadataField.Type type, final String pattern) {
    switch (type) {
      case BOOLEAN:
        if (rawValue == null)
          return Jsons.BLANK;
        return v(rawValue, Jsons.BLANK);
      case DATE: {
        if (rawValue == null)
          return Jsons.BLANK;
        final SimpleDateFormat dateFormat = getSimpleDateFormatter(pattern);
        return v(dateFormat.format((Date) rawValue), Jsons.BLANK);
      }
      case DURATION: {
        if (rawValue == null)
          return Jsons.BLANK;
        long returnValue = 0L;
        final String value = (String) rawValue;
        final DCMIPeriod period = EncodingSchemeUtils.decodePeriod(value);
        if (period != null && period.hasStart() && period.hasEnd()) {
          returnValue = period.getEnd().getTime() - period.getStart().getTime();
        } else {
          try {
            returnValue = Long.parseLong(value);
          } catch (final NumberFormatException e) {
            logger.debug("Unable to parse duration '{}' as either period or millisecond duration.", value);
          }
        }
        return v(DurationFormatUtils.formatDuration(returnValue, PATTERN_DURATION));
      }
      case ITERABLE_TEXT:
      case MIXED_TEXT: {
        if (rawValue == null)
          return arr();

        final List<JValue> list = new ArrayList<>();
        if (rawValue instanceof String) {
          // The value is a string so we need to split it.
          final String stringVal = (String) rawValue;
          for (final String entry : stringVal.split(",")) {
            if (StringUtils.isNotBlank(entry))
              list.add(v(entry, Jsons.BLANK));
          }
        } else {
          // The current value is just an iterable string.
          for (final Object v : (Iterable<String>)rawValue) {
            list.add(v(v, Jsons.BLANK));
          }
        }

        return arr(list);
      }
      case ORDERED_TEXT:
      case TEXT_LONG:
      case TEXT:
        return v(rawValue == null ? "" : (String)rawValue);
      case LONG:
        if (rawValue == null)
          return Jsons.BLANK;
        return v(rawValue.toString());
      case START_DATE: {
        if (rawValue == null)
          return Jsons.BLANK;

        final String value = (String) rawValue;

        if (StringUtils.isBlank(value))
          return Jsons.BLANK;

        // Try to parse the metadata as DCIM metadata.
        final DCMIPeriod p = EncodingSchemeUtils.decodePeriod(value);
        final SimpleDateFormat dateFormat = getSimpleDateFormatter(pattern);
        if (p != null)
          return v(dateFormat.format(p.getStart()), Jsons.BLANK);

        // Not DCIM metadata so it might already be formatted (given from the front and is being returned there
        try {
          dateFormat.parse(value);
          return v(value, Jsons.BLANK);
        } catch (final Exception e) {
          logger.error(
                  "Unable to parse temporal metadata '{}' as either DCIM data or a formatted date using pattern {} because:",
                  value,
                  pattern,
                  e);
          throw new IllegalArgumentException(e);
        }
      }
      case START_TIME: {
        if (rawValue == null)
          return Jsons.BLANK;

        final String value = (String) rawValue;

        if (StringUtils.isBlank(value))
          return Jsons.BLANK;

        // Try to parse the metadata as DCIM metadata.
        final DCMIPeriod p = EncodingSchemeUtils.decodePeriod(value);
        if (p != null) {
          final SimpleDateFormat dateFormat = getSimpleDateFormatter(pattern);
          return v(dateFormat.format(p.getStart()), Jsons.BLANK);
        }

        // Not DCIM metadata so it might already be formatted (given from the front and is being returned there
        try {
          final SimpleDateFormat dateFormat = getSimpleDateFormatter(pattern);
          dateFormat.parse(value);
          return v(value, Jsons.BLANK);
        } catch (final Exception e) {
          logger.error(
                  "Unable to parse temporal metadata '{}' as either DCIM data or a formatted date using pattern {} because:",
                  value,
                  pattern,
                  e);
          throw new IllegalArgumentException(e);
        }
      }
      default:
        throw new IllegalArgumentException("invalid metadata field of type '" + type + "'");
    }
  }

  private static JsonType jsonType(final MetadataField f, final boolean withOrderedText) {
    switch (f.getType()) {
      case BOOLEAN:
        return JsonType.BOOLEAN;
      case DATE:
      case START_DATE:
        return JsonType.DATE;
      case DURATION:
      case ITERABLE_TEXT:
      case TEXT:
        return JsonType.TEXT;
      case MIXED_TEXT:
        return JsonType.MIXED_TEXT;
      case ORDERED_TEXT:
        return withOrderedText ? JsonType.ORDERED_TEXT : JsonType.TEXT;
      case LONG:
        return JsonType.NUMBER;
      case START_TIME:
        return JsonType.TIME;
      case TEXT_LONG:
        return JsonType.TEXT_LONG;
      default:
        throw new IllegalArgumentException("invalid field type '" + f.getType() + "'");
    }
  }

  private static Object valueFromJson(final Object value, final MetadataField field) {
    switch (field.getType()) {
      case BOOLEAN: {
        if (value instanceof Boolean)
          return value;
        final String stringValue = value.toString();
        if (StringUtils.isBlank(stringValue))
          return null;
        return Boolean.parseBoolean(stringValue);
      }
      case DATE: {
        final SimpleDateFormat dateFormat = getSimpleDateFormatter(field.getPattern());
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
      case DURATION: {
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
      case ITERABLE_TEXT: {
        final JSONArray array = (JSONArray) value;
        if (array == null)
          return null;
        final String[] arrayOut = new String[array.size()];
        for (int i = 0; i < array.size(); i++)
          arrayOut[i] = (String) array.get(i);
        return Arrays.asList(arrayOut);
      }
      case MIXED_TEXT: {
        final JSONParser parser = new JSONParser();
        final JSONArray array;
        if (value instanceof String) {
          try {
            array = (JSONArray) parser.parse((String) value);
          } catch (final ParseException e) {
            throw new IllegalArgumentException("Unable to parse Mixed Iterable value into a JSONArray:", e);
          }
        } else {
          array = (JSONArray) value;
        }

        if (array == null)
          return new ArrayList<>();
        final String[] arrayOut = new String[array.size()];
        for (int i = 0; i < array.size(); i++)
          arrayOut[i] = (String) array.get(i);
        return Arrays.asList(arrayOut);
      }
      case TEXT:
      case TEXT_LONG:
      case ORDERED_TEXT: {
        if (value == null)
          return "";
        if (!(value instanceof String)) {
          logger.warn("Value cannot be parsed as String. Expecting type 'String', but received type '{}'.", value.getClass().getName());
          return null;
        }
        return value;
      }
      case LONG: {
        if (!(value instanceof String)) {
          logger.warn("The given value for Long can not be parsed.");
          return 0L;
        }
        final String longString = (String) value;
        return Long.parseLong(longString);
      }
      case START_DATE:
      case START_TIME:
      {
        final String date = (String) value;

        if (StringUtils.isBlank(date))
          return "";

        try {
          final SimpleDateFormat dateFormat = getSimpleDateFormatter(field.getPattern());
          dateFormat.parse(date);
        } catch (final java.text.ParseException e) {
          logger.error("Not able to parse date string {}: {}", value, getMessage(e));
          return null;
        }

        return date;
      }
      default:
        throw new IllegalArgumentException("invalid field type '" + field.getType() + "'");
    }
  }

  public static JObject fieldToJson(final MetadataField f, final boolean withOrderedText) {
    Objects.requireNonNull(f);
    final Map<String, Field> values = new HashMap<>();
    values.put(JSON_KEY_ID, f(JSON_KEY_ID, v(f.getOutputID(), Jsons.BLANK)));
    values.put(JSON_KEY_LABEL, f(JSON_KEY_LABEL, v(f.getLabel(), Jsons.BLANK)));
    values.put(JSON_KEY_VALUE, f(JSON_KEY_VALUE, valueToJson(f.getValue(), f.getType(), f.getPattern())));
    values.put(JSON_KEY_TYPE, f(JSON_KEY_TYPE, v(jsonType(f, withOrderedText).toString().toLowerCase(), Jsons.BLANK)));
    values.put(JSON_KEY_READONLY, f(JSON_KEY_READONLY, v(f.isReadOnly())));
    values.put(JSON_KEY_REQUIRED, f(JSON_KEY_REQUIRED, v(f.isRequired())));

    if (f.getCollection() != null)
      values.put(JSON_KEY_COLLECTION, f(JSON_KEY_COLLECTION, mapToJson(f.getCollection())));
    else if (f.getCollectionID() != null)
      values.put(JSON_KEY_COLLECTION, f(JSON_KEY_COLLECTION, v(f.getCollectionID())));
    if (f.isTranslatable() != null)
      values.put(JSON_KEY_TRANSLATABLE, f(JSON_KEY_TRANSLATABLE, v(f.isTranslatable())));
    if (f.getDelimiter() != null)
      values.put(JSON_KEY_DELIMITER, f(JSON_KEY_DELIMITER, v(f.getDelimiter())));
    if (f.hasDifferentValues() != null)
      values.put(JSON_KEY_DIFFERENT_VALUES, f(JSON_KEY_DIFFERENT_VALUES, v(f.hasDifferentValues())));
    return obj(values);
  }

  public static MetadataField copyWithDifferentJsonValue(final MetadataField t, final String v) {
    final MetadataField copy = new MetadataField(t);
    copy.setValue(valueFromJson(v, copy));
    return copy;
  }

  public static JValue collectionToJson(final DublinCoreMetadataCollection collection, final boolean withOrderedText) {
    return arr(collection.getFields().stream().map(field -> fieldToJson(field, withOrderedText))
            .collect(Collectors.toList()));
  }

  public static JSONArray extractSingleCollectionfromListJson(JSONArray json) {
    if (json == null || json.size() != 1) {
      throw new IllegalArgumentException("Input has to be a JSONArray with one entry");
    }

    return (JSONArray) ((JSONObject) json.get(0)).get(KEY_METADATA_FIELDS);
  }

  public static void fillCollectionFromJson(final DublinCoreMetadataCollection collection, final Object json) {
    if (!(json instanceof  JSONArray))
      throw new IllegalArgumentException("couldn't fill metadata collection, didn't get an array");

    final JSONArray metadataJson = (JSONArray) json;
    for (final JSONObject item : (Iterable<JSONObject>) metadataJson) {
      final String fieldId = (String) item.get(KEY_METADATA_ID);

      if (fieldId == null)
        continue;
      final Object value = item.get(KEY_METADATA_VALUE);
      if (value == null)
        continue;

      final MetadataField target = collection.getOutputFields().get(fieldId);
      if (target == null)
        continue;

      final Object o = valueFromJson(value, target);
      target.setValue(o);
    }
  }

  public static void fillListFromJson(final MetadataList metadataList, final JSONArray json) {
    for (final JSONObject item : (Iterable<JSONObject>) json) {
      final MediaPackageElementFlavor flavor = MediaPackageElementFlavor
              .parseFlavor((String) item.get(KEY_METADATA_FLAVOR));
      final String title = (String) item.get(KEY_METADATA_TITLE);
      if (title == null)
        continue;

      final JSONArray value = (JSONArray) item.get(KEY_METADATA_FIELDS);
      if (value == null)
        continue;

      final DublinCoreMetadataCollection collection = metadataList.getMetadataByFlavor(flavor.toString());
      if (collection == null)
        continue;
      MetadataJson.fillCollectionFromJson(collection, value);
    }
  }

  public static JValue listToJson(final MetadataList metadataList, final boolean withOrderedText) {
    final List<JValue> catalogs = new ArrayList<>();
    for (final Map.Entry<String, MetadataList.TitledMetadataCollection> metadata : metadataList.getMetadataList().entrySet()) {
      final List<Field> fields = new ArrayList<>();

      DublinCoreMetadataCollection metadataCollection = metadata.getValue().getCollection();

      if (!MetadataList.Locked.NONE.equals(metadataList.getLocked())) {
        fields.add(f(KEY_METADATA_LOCKED, v(metadataList.getLocked().getValue())));
        metadataCollection = metadataCollection.readOnlyCopy();
      }

      fields.add(f(KEY_METADATA_FLAVOR, v(metadata.getKey())));
      fields.add(f(KEY_METADATA_TITLE, v(metadata.getValue().getTitle())));
      fields.add(f(KEY_METADATA_FIELDS, MetadataJson.collectionToJson(metadataCollection, withOrderedText)));

      catalogs.add(obj(fields));
    }
    return arr(catalogs);
  }
}
