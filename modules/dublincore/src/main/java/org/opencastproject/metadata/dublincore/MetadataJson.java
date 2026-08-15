/*
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

import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;

import org.opencastproject.mediapackage.MediaPackageElementFlavor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

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
  private static final String JSON_KEY_LISTPROVIDER = "listprovider";
  private static final String KEY_METADATA_TITLE = "title";
  private static final String KEY_METADATA_FLAVOR = "flavor";
  private static final String KEY_METADATA_FIELDS = "fields";
  private static final String KEY_METADATA_LOCKED = "locked";


  /* Keys for the different properties of the metadata JSON Object */
  private static final String KEY_METADATA_ID = "id";
  private static final String KEY_METADATA_VALUE = "value";

  private static final String PATTERN_DURATION = "HH:mm:ss";

  /**
   * Turn a map into a JSON object
   *
   * @param map the source map
   * @return a new JSON object generated with the map values
   */
  private static JsonObject mapToJson(final Map<String, String> map) {
    Objects.requireNonNull(map);
    JsonObject json = new JsonObject();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      json.addProperty(entry.getKey(), safeString(entry.getValue()));
    }
    return json;
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

  private static <T> JsonElement valueToJson(final T rawValue, final MetadataField.Type type, final String pattern) {
    switch (type) {
      case BOOLEAN:
        return rawValue == null ? new JsonPrimitive("") : new JsonPrimitive(rawValue.toString());

      case DATE: {
        if (rawValue == null) {
          return new JsonPrimitive("");
        }
        SimpleDateFormat dateFormat = getSimpleDateFormatter(pattern);
        return new JsonPrimitive(dateFormat.format((Date) rawValue));
      }

      case DURATION: {
        if (rawValue == null) {
          return new JsonPrimitive("");
        }
        long returnValue = 0L;
        String value = (String) rawValue;
        DCMIPeriod period = EncodingSchemeUtils.decodePeriod(value);

        if (period != null && period.hasStart() && period.hasEnd()) {
          returnValue = period.getEnd().getTime() - period.getStart().getTime();
        } else {
          try {
            returnValue = Long.parseLong(value);
          } catch (NumberFormatException e) {
            logger.debug("Unable to parse duration '{}' as either period or millisecond duration.", value);
          }
        }
        return new JsonPrimitive(DurationFormatUtils.formatDuration(returnValue, PATTERN_DURATION));
      }

      case ITERABLE_TEXT:
      case MIXED_TEXT: {
        JsonArray jsonArray = new JsonArray();

        if (rawValue == null) {
          return jsonArray;
        }

        if (rawValue instanceof String) {
          for (String entry : ((String) rawValue).split(",")) {
            if (StringUtils.isNotBlank(entry)) {
              jsonArray.add(safeString(entry));
            }
          }
        } else {
          for (Object val : (Iterable<?>) rawValue) {
            if (val != null) {
              jsonArray.add(safeString(val));
            }
          }
        }
        return jsonArray;
      }

      case ORDERED_TEXT:
      case TEXT_LONG:
      case TEXT:
        return rawValue == null ? new JsonPrimitive("") : new JsonPrimitive(rawValue.toString());

      case LONG:
        return rawValue == null ? new JsonPrimitive("") : new JsonPrimitive(rawValue.toString());

      case START_DATE:
      case START_TIME: {
        if (rawValue == null) {
          return new JsonPrimitive("");
        }
        String value = (String) rawValue;

        if (StringUtils.isBlank(value)) {
          return new JsonPrimitive("");
        }

        // Try to parse the metadata as DCIM metadata.
        final DCMIPeriod p = EncodingSchemeUtils.decodePeriod(value);
        final SimpleDateFormat dateFormat = getSimpleDateFormatter(pattern);
        if (p != null) {
          return new JsonPrimitive(dateFormat.format(p.getStart()));
        }

        // Not DCIM metadata so it might already be formatted (given from the front and is being returned there
        try {
          dateFormat.parse(value);
          return new JsonPrimitive(value);
        } catch (Exception e) {
          logger.error(
              "Unable to parse temporal metadata '{}' as either DCIM data or a formatted date using pattern {} "
                  + "because:",
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

  private static Object valueFromJson(final JsonElement value, final MetadataField field) {
    switch (field.getType()) {
      case BOOLEAN: {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
          return value.getAsBoolean();
        }
        final String stringValue = value.getAsString();
        if (StringUtils.isBlank(stringValue)) {
          return null;
        }
        return Boolean.parseBoolean(stringValue);
      }
      case DATE: {
        final SimpleDateFormat dateFormat = getSimpleDateFormatter(field.getPattern());
        try {
          final String date = value.getAsString();

          if (StringUtils.isBlank(date)) {
            return null;
          }

          return dateFormat.parse(date);
        } catch (final java.text.ParseException e) {
          logger.error("Not able to parse date {}: {}", value, e.getMessage());
          return null;
        }
      }
      case DURATION: {
        if (!isJsonString(value)) {
          logger.warn("The given value for duration can not be parsed.");
          return "";
        }

        final String duration = value.getAsString();
        final String[] durationParts = duration.split(":");
        if (durationParts.length < 3) {
          return null;
        }
        final long hours = Long.parseLong(durationParts[0]);
        final long minutes = Long.parseLong(durationParts[1]);
        final long seconds = Long.parseLong(durationParts[2]);

        final long returnValue = ((hours * 60 + minutes) * 60 + seconds) * 1000;

        return Long.toString(returnValue);
      }
      case ITERABLE_TEXT: {
        if (value == null || value.isJsonNull()) {
          return null;
        }
        return toStringList(value.getAsJsonArray());
      }
      case MIXED_TEXT: {
        final JsonArray array;
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
          try {
            array = JsonParser.parseString(value.getAsString()).getAsJsonArray();
          } catch (final JsonParseException | IllegalStateException e) {
            throw new IllegalArgumentException("Unable to parse Mixed Iterable value into a JSON array:", e);
          }
        } else if (value == null || value.isJsonNull()) {
          return new ArrayList<>();
        } else {
          array = value.getAsJsonArray();
        }

        return toStringList(array);
      }
      case TEXT:
      case TEXT_LONG:
      case ORDERED_TEXT: {
        if (value == null || value.isJsonNull()) {
          return "";
        }
        if (!isJsonString(value)) {
          logger.warn("Value cannot be parsed as String. Expecting type 'String', but received '{}'.", value);
          return null;
        }
        return value.getAsString();
      }
      case LONG: {
        if (!isJsonString(value)) {
          logger.warn("The given value for Long can not be parsed.");
          return 0L;
        }
        return Long.parseLong(value.getAsString());
      }
      case START_DATE:
      case START_TIME: {
        final String date = value == null || value.isJsonNull() ? null : value.getAsString();

        if (StringUtils.isBlank(date)) {
          return "";
        }

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

  /** Read a string member, returning null when it is absent or JSON null. */
  private static String getStringOrNull(final JsonObject json, final String key) {
    final JsonElement value = json.get(key);
    return value == null || value.isJsonNull() ? null : value.getAsString();
  }

  /** True if the element is a JSON string, matching the previous instanceof String checks. */
  private static boolean isJsonString(final JsonElement value) {
    return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
  }

  private static List<String> toStringList(final JsonArray array) {
    final List<String> out = new ArrayList<>(array.size());
    for (final JsonElement element : array) {
      out.add(element.isJsonNull() ? null : element.getAsString());
    }
    return out;
  }

  public static JsonObject fieldToJson(final MetadataField f, final boolean withOrderedText,
      final boolean withListprovider) {
    Objects.requireNonNull(f);

    JsonObject json = new JsonObject();

    json.addProperty(JSON_KEY_ID, safeString(f.getOutputID()));
    json.addProperty(JSON_KEY_LABEL, safeString(f.getLabel()));
    json.add(JSON_KEY_VALUE, valueToJson(f.getValue(), f.getType(), f.getPattern()));
    json.addProperty(JSON_KEY_TYPE, safeString(jsonType(f, withOrderedText).toString().toLowerCase()));
    json.addProperty(JSON_KEY_READONLY, f.isReadOnly());
    json.addProperty(JSON_KEY_REQUIRED, f.isRequired());

    if (f.getCollection() != null) {
      json.add(JSON_KEY_COLLECTION, mapToJson(f.getCollection()));
    } else if (f.getCollectionID() != null) {
      json.addProperty(JSON_KEY_COLLECTION, f.getCollectionID());
    }

    if (f.isTranslatable() != null) {
      json.addProperty(JSON_KEY_TRANSLATABLE, f.isTranslatable());
    }
    if (f.getDelimiter() != null) {
      json.addProperty(JSON_KEY_DELIMITER, f.getDelimiter());
    }
    if (f.hasDifferentValues() != null) {
      json.addProperty(JSON_KEY_DIFFERENT_VALUES, f.hasDifferentValues());
    }

    if (f.getListprovider() != null && withListprovider) {
      json.addProperty(JSON_KEY_LISTPROVIDER, f.getListprovider());
    }

    return json;
  }

  public static String safeString(Object input) {
    return input != null ? input.toString() : "";
  }

  public static MetadataField copyWithDifferentJsonValue(final MetadataField t, final String v) {
    final MetadataField copy = new MetadataField(t);
    copy.setValue(valueFromJson(new JsonPrimitive(v), copy));
    return copy;
  }

  public static JsonArray collectionToJson(final DublinCoreMetadataCollection collection,
      final boolean withOrderedText, final boolean withListprovider) {
    JsonArray jsonArray = new JsonArray();
    for (MetadataField field : collection.getFields()) {
      JsonObject fieldJson = fieldToJson(field, withOrderedText, withListprovider);
      jsonArray.add(fieldJson);
    }
    return jsonArray;
  }

  public static JsonArray extractSingleCollectionfromListJson(JsonArray json) {
    if (json == null || json.size() != 1) {
      throw new IllegalArgumentException("Input has to be a JSON array with one entry");
    }

    return json.get(0).getAsJsonObject().getAsJsonArray(KEY_METADATA_FIELDS);
  }

  public static void fillCollectionFromJson(final DublinCoreMetadataCollection collection, final JsonElement json) {
    if (json == null || !json.isJsonArray()) {
      throw new IllegalArgumentException("couldn't fill metadata collection, didn't get an array");
    }

    for (final JsonElement element : json.getAsJsonArray()) {
      final JsonObject item = element.getAsJsonObject();
      final String fieldId = getStringOrNull(item, KEY_METADATA_ID);

      if (fieldId == null) {
        continue;
      }
      final JsonElement value = item.get(KEY_METADATA_VALUE);
      if (value == null || value.isJsonNull()) {
        continue;
      }

      final MetadataField target = collection.getOutputFields().get(fieldId);
      if (target == null) {
        continue;
      }

      final Object o = valueFromJson(value, target);
      target.setValue(o);
    }
  }

  public static void fillListFromJson(final MetadataList metadataList, final JsonArray json) {
    for (final JsonElement element : json) {
      final JsonObject item = element.getAsJsonObject();
      final MediaPackageElementFlavor flavor = MediaPackageElementFlavor
              .parseFlavor(getStringOrNull(item, KEY_METADATA_FLAVOR));
      final String title = getStringOrNull(item, KEY_METADATA_TITLE);
      if (title == null) {
        continue;
      }

      final JsonElement value = item.get(KEY_METADATA_FIELDS);
      if (value == null || value.isJsonNull()) {
        continue;
      }

      final DublinCoreMetadataCollection collection = metadataList.getMetadataByFlavor(flavor.toString());
      if (collection == null) {
        continue;
      }
      MetadataJson.fillCollectionFromJson(collection, value);
    }
  }

  public static JsonArray listToJson(final MetadataList metadataList, final boolean withOrderedText,
      final boolean withListprovider) {
    JsonArray catalogs = new JsonArray();

    for (Map.Entry<String, MetadataList.TitledMetadataCollection> metadata
        : metadataList.getMetadataList().entrySet()) {
      JsonObject catalogJson = new JsonObject();

      DublinCoreMetadataCollection metadataCollection = metadata.getValue().getCollection();

      if (!MetadataList.Locked.NONE.equals(metadataList.getLocked())) {
        catalogJson.addProperty(KEY_METADATA_LOCKED, metadataList.getLocked().getValue());
        metadataCollection = metadataCollection.readOnlyCopy();
      }

      catalogJson.addProperty(KEY_METADATA_FLAVOR, metadata.getKey());
      catalogJson.addProperty(KEY_METADATA_TITLE, metadata.getValue().getTitle());
      catalogJson.add(KEY_METADATA_FIELDS, collectionToJson(metadataCollection, withOrderedText, withListprovider));

      catalogs.add(catalogJson);
    }

    return catalogs;
  }
}
