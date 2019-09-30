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
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.metadata.dublincore.MetadataField.Type;
import org.opencastproject.metadata.dublincore.Precision;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author bbru
 *
 */
public final class DublinCoreMetadataUtil {

  private static final Logger logger = LoggerFactory.getLogger(DublinCoreMetadataUtil.class);

  private DublinCoreMetadataUtil() {
  }

  /**
   * Update a {@link DublinCoreCatalog} with the values contained within a {@link AbstractMetadataCollection}
   *
   * @param dc
   *          The {@link DublinCoreCatalog} to update the values within.
   * @param metadata
   *          The {@link AbstractMetadataCollection} data definitions and values to update the catalog with.
   */
  public static void updateDublincoreCatalog(DublinCoreCatalog dc, MetadataCollection metadata) {
    for (MetadataField<?> field : metadata.getOutputFields().values()) {
      if (field.isUpdated() && field.getValue().isSome()) {
        final String namespace = field.getNamespace().getOr(DublinCore.TERMS_NS_URI);
        final EName ename = new EName(namespace, field.getInputID());
        if (field.getType() == MetadataField.Type.START_DATE) {
          setStartDate(dc, field, ename);
        } else if (field.getType() == MetadataField.Type.DURATION) {
          // WARN: the duration change assumes the catalog's start date is already up to date.
          setDuration(dc, field, ename);
        } else if (field.getType() == Type.DATE) {
          // Skip over metadata field tagged with key "created".
          // DC created should only be modified by changing the start date, see MH-12250
          if (! DublinCore.PROPERTY_CREATED.equals(ename))
            setDate(dc, field, ename);
        } else if (field.getType() == MetadataField.Type.MIXED_TEXT || field.getType() == Type.ITERABLE_TEXT) {
          setIterableString(dc, field, ename);
        } else {
          if (field.isRequired() && StringUtils.isBlank(field.getValue().get().toString()))
            throw new IllegalArgumentException(
                    String.format(
                            "The event metadata field with id '%s' and the metadata type '%s' is required and can not be empty!.",
                            field.getInputID(), field.getType()));
          dc.set(ename, field.getValue().get().toString());
        }
      } else if (field.getValue().isNone() && field.isRequired()) {
        throw new IllegalArgumentException(String.format(
                "The event metadata field with id '%s' and the metadata type '%s' is required and can not be empty!.",
                field.getInputID(), field.getType()));
      }
    }
  }

  /**
   * Set the value of an iterable string (each element as an separate entry)
   *
   * @param dc    The dublin core catalog to add the iterable string values to (or remove it if empty)
   * @param field The {@link MetadataField} with the values to update.
   * @param ename The {@link EName} of the property in the {@link DublinCoreCatalog} to update.
   */
  private static void setIterableString(DublinCoreCatalog dc, MetadataField<?> field, final EName ename) {
    if (field.getValue().isSome()) {
      dc.remove(ename);
      if (field.getValue().get() instanceof String) {
        String valueString = (String) field.getValue().get();
        dc.set(ename, valueString);
      } else {
        @SuppressWarnings("unchecked")
        Iterable<String> valueIterable = (Iterable<String>) field.getValue().get();
        for (String valueString : valueIterable) {
          if (StringUtils.isNotBlank(valueString))
            dc.add(ename, valueString);
        }
      }
    }
  }

  private static Opt<DCMIPeriod> getPeriodFromCatalog(DublinCoreCatalog dc, EName ename) {
    List<DublinCoreValue> periodStrings = dc.get(ename);
    Opt<DCMIPeriod> p = Opt.<DCMIPeriod> none();
    for (DublinCoreValue periodString : periodStrings) {
      p = Opt.nul(EncodingSchemeUtils.decodePeriod(periodString.getValue()));
    }
    if (p.isNone()) {
      // fall back to created date with zero duration
      // opencast keep the event start date and created date in sync
      // if the start date isn't set, we can grab the created value
      DublinCoreValue createdDCValue = dc.getFirstVal(DublinCore.PROPERTY_CREATED);
      if (createdDCValue != null) {
        Date createdDate = EncodingSchemeUtils.decodeDate(createdDCValue.getValue());
        p = Opt.nul(new DCMIPeriod(createdDate, createdDate));
      }
    }
    return p;
  }

  /**
   * @param period
   *          The dublin core period for this event.
   * @return The milliseconds between the start and end time for this event.
   */
  static Long getDuration(Opt<DCMIPeriod> period) {
    Long duration = 0L;
    if (period.isSome() && period.get().hasStart() && period.get().hasEnd()) {
      return period.get().getEnd().getTime() - period.get().getStart().getTime();
    }
    return duration;
  }

  /**
   * Sets a date object with the correct formatting into the dublin core catalog.
   *
   * @param dc
   *          The dublin core catalog to insert the date.
   * @param field
   *          The field with the date value and pattern ready to format.
   * @param ename
   *          The unique id for the dublin core property.
   */
  private static void setDate(DublinCoreCatalog dc, MetadataField<?> field, EName ename) {
    if (field.getValue().get() instanceof Date && field.getPattern().isNone()) {
      throw new IllegalArgumentException("There needs to be a pattern property set for " + field.getInputID() + ":"
              + field.getOutputID() + ":" + field.getValue() + " metadata field to store and retrieve the result.");
    }
    if (field.getValue().get() instanceof Date) {
      SimpleDateFormat sdf = new SimpleDateFormat(field.getPattern().get());
      dc.set(ename, sdf.format((Date) field.getValue().get()));
    } else {
      dc.set(ename, field.getValue().get().toString());
    }
  }

  /**
   * Sets the start date in a dublin core catalog to the right value and keeps the start time and duration the same.
   *
   * @param dc
   *          The dublin core catalog to adjust
   * @param field
   *          The metadata field that contains the start date.
   * @param ename
   *          The EName in the catalog to identify the property that has the dublin core period.
   */
  static void setStartDate(DublinCoreCatalog dc, MetadataField<?> field, EName ename) {
    if (field.getValue().isNone()
            || (field.getValue().get() instanceof String && StringUtils.isBlank(field.getValue().get().toString()))) {
      logger.debug("No value was set for metadata field with dublin core id '{}' and json id '{}'", field.getInputID(),
              field.getOutputID());
      return;
    }
    try {
      // Get the current date
      SimpleDateFormat dateFormat = MetadataField.getSimpleDateFormatter(field.getPattern().get());
      Date startDate = dateFormat.parse((String) field.getValue().get());
      // Get the current period
      Opt<DCMIPeriod> period = getPeriodFromCatalog(dc, ename);
      // Get the current duration
      Long duration = getDuration(period);
      // Get the current end date based on new date and duration.
      DateTime endDate = new DateTime(startDate.getTime() + duration);
      dc.set(ename, EncodingSchemeUtils.encodePeriod(new DCMIPeriod(startDate, endDate.toDate()),
              Precision.Second));
      // ensure that DC created is start date, see MH-12250
      setDate(dc, field, DublinCore.PROPERTY_CREATED);
    } catch (ParseException e) {
      logger.error("Not able to parse date {} to update the dublin core because: {}", field.getValue(),
              ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * Gets the current hour, minute and second from a dublin core period if available.
   *
   * @param period
   *          The current period from dublin core.
   * @return A new DateTime with the current hour, minute and second.
   */
  private static DateTime getCurrentStartDateTime(Opt<DCMIPeriod> period) {
    DateTime currentStartTime = new DateTime();
    currentStartTime = currentStartTime.withZone(DateTimeZone.UTC);
    currentStartTime = currentStartTime.withYear(2001);
    currentStartTime = currentStartTime.withMonthOfYear(1);
    currentStartTime = currentStartTime.withDayOfMonth(1);
    currentStartTime = currentStartTime.withHourOfDay(0);
    currentStartTime = currentStartTime.withMinuteOfHour(0);
    currentStartTime = currentStartTime.withSecondOfMinute(0);

    if (period.isSome() && period.get().hasStart()) {
      DateTime fromDC = new DateTime(period.get().getStart().getTime());
      fromDC = fromDC.withZone(DateTimeZone.UTC);
      currentStartTime = currentStartTime.withZone(DateTimeZone.UTC);
      currentStartTime = currentStartTime.withYear(fromDC.getYear());
      currentStartTime = currentStartTime.withMonthOfYear(fromDC.getMonthOfYear());
      currentStartTime = currentStartTime.withDayOfMonth(fromDC.getDayOfMonth());
      currentStartTime = currentStartTime.withHourOfDay(fromDC.getHourOfDay());
      currentStartTime = currentStartTime.withMinuteOfHour(fromDC.getMinuteOfHour());
      currentStartTime = currentStartTime.withSecondOfMinute(fromDC.getSecondOfMinute());
    }
    return currentStartTime;
  }

  /**
   * Sets the duration in a dublin core catalog to the right value and keeps the start date and start time the same.
   *
   * @param dc
   *          The dublin core catalog to adjust
   * @param field
   *          The metadata field that contains the duration.
   * @param ename
   *          The EName in the catalog to identify the property that has the dublin core period.
   */
  static void setDuration(DublinCoreCatalog dc, MetadataField<?> field, EName ename) {
    if (field.getValue().isNone()) {
      logger.error("No value was set for metadata field with dublin core id '{}' and json id '{}'", field.getInputID(),
              field.getOutputID());
      return;
    }

    // Get the current period
    Opt<DCMIPeriod> period = getPeriodFromCatalog(dc, ename);
    // Get the current duration
    Long duration = 0L;
    try {
      duration = Long.parseLong(field.getValue().get().toString());
    } catch (NumberFormatException e) {
      logger.debug("Unable to parse the duration's value '{}' as a long value. Trying it as a period next.",
              field.getValue().get());
    }
    if (duration < 1L) {
      duration = getDuration(period);
    }
    // Get the current start date (WARN: this assumes any start time updates have already been performed)
    DateTime startDateTime = getCurrentStartDateTime(period);
    // Get the current end date based on new date and duration.
    DateTime endDate = new DateTime(startDateTime.toDate().getTime() + duration);
    dc.set(ename, EncodingSchemeUtils.encodePeriod(new DCMIPeriod(startDateTime.toDate(), endDate.toDate()),
            Precision.Second));
  }

  @SuppressWarnings("unchecked")
  public static Map<String, MetadataField<?>> getDublinCoreProperties(Dictionary configProperties) {

    Map<String,Map<String, String>> allProperties = new HashMap();

    for (Object configObject : Collections.list(configProperties.keys())) {
      String property = configObject.toString();

      Opt<String> propertyNameOpt = getDublinCorePropertyName(property);
      Opt<String> propertyKeyOpt = getDublinCorePropertyKey(property);

      if (propertyNameOpt.isSome() && propertyKeyOpt.isSome()) {

        String propertyName = propertyNameOpt.get();
        String propertyKey = propertyKeyOpt.get();

        Map<String,String> metadataFieldProperties = allProperties.computeIfAbsent(propertyName,
                key -> new HashMap<>());
        metadataFieldProperties.put(propertyKey, configProperties.get(property).toString());
      }
    }

    Map<String, MetadataField<?>> metadataFieldsMap = new TreeMap<String, MetadataField<?>>();
    for (Map<String, String> metadataFieldPropertiesMap : allProperties.values()) {
      MetadataField metadataField = MetadataField.createMetadataField(metadataFieldPropertiesMap);
      metadataFieldsMap.put(metadataField.getOutputID(), metadataField);
    }

    return metadataFieldsMap;
  }

  static boolean isDublinCoreProperty(String propertyKey) {
    return !StringUtils.isBlank(propertyKey) && propertyKey.split("\\.").length == 3
            && propertyKey.split("\\.")[0].equalsIgnoreCase(MetadataField.CONFIG_PROPERTY_PREFIX);
  }

  static Opt<String> getDublinCorePropertyName(String propertyKey) {
    if (isDublinCoreProperty(propertyKey)) {
      return Opt.some(propertyKey.split("\\.")[1]);
    }
    return Opt.none();
  }

  static Opt<String> getDublinCorePropertyKey(String propertyKey) {
    if (isDublinCoreProperty(propertyKey)) {
      return Opt.some(propertyKey.split("\\.")[2]);
    }
    return Opt.none();
  }

}
