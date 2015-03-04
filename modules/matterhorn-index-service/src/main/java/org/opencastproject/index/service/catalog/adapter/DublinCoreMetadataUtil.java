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

import org.opencastproject.index.service.catalog.adapter.MetadataField.TYPE;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;

import com.entwinemedia.fn.data.Opt;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
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
  public static void updateDublincoreCatalog(DublinCoreCatalog dc, AbstractMetadataCollection metadata) {
    for (MetadataField<?> field : metadata.getOutputFields().values()) {
      if (field.isUpdated() && field.getValue().isSome()) {
        final String namespace = field.getNamespace().or(DublinCore.TERMS_NS_URI);
        final EName ename = new EName(namespace, field.getInputID());
        if (field.getType() == TYPE.START_DATE) {
          setTemporalStartDate(dc, field, ename);
        } else if (field.getType() == TYPE.START_TIME) {
          setTemporalStartTime(dc, field, ename);
        } else if (field.getType() == TYPE.DURATION) {
          setDuration(dc, field, ename);
        } else if (field.getType() == TYPE.DATE) {
          setDate(dc, field, ename);
        } else if (field.getType() == TYPE.ITERABLE_TEXT) {
          setIterableString(dc, field, ename);
        } else {
          dc.set(ename, field.getValue().get().toString());
        }
      }
    }
  }

  /**
   * Set the value of an iterable string, comma separated
   *
   * @param dc
   *          The dublin core catalog to add the iterable string value to (or remove it if empty)
   * @param field
   *          The {@link MetadataField} with the value to update.
   * @param ename
   *          The {@link EName} of the property in the {@link DublinCoreCatalog} to update.
   */
  private static void setIterableString(DublinCoreCatalog dc, MetadataField<?> field, final EName ename) {
    if (field.getValue().isSome()) {
      @SuppressWarnings("unchecked")
      Iterable<String> valueIterable = (Iterable<String>) field.getValue().get();
      String valueString = StringUtils.join(valueIterable.iterator(), ",");
      if (StringUtils.isBlank(StringUtils.trimToEmpty(valueString))) {
        // The value of the iterative string is empty so we will remove it.
        dc.remove(ename);
      } else {
        dc.set(ename, valueString);
      }
    }
  }

  private static Opt<DCMIPeriod> getPeriodFromCatalog(DublinCoreCatalog dc, EName ename) {
    List<DublinCoreValue> periodStrings = dc.get(ename);
    Opt<DCMIPeriod> p = Opt.<DCMIPeriod> none();
    for (DublinCoreValue periodString : periodStrings) {
      p = Opt.some(EncodingSchemeUtils.decodePeriod(periodString.getValue()));
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
   * Gets the current hour, minute and second from a dublin core period if available.
   *
   * @param period
   *          The current period from dublin core.
   * @return A new DateTime with the current hour, minute and second.
   */
  static DateTime getCurrentStartTime(Opt<DCMIPeriod> period) {
    DateTime currentStartTime = new DateTime();
    currentStartTime = currentStartTime.withZone(DateTimeZone.UTC);
    currentStartTime = currentStartTime.withHourOfDay(0);
    currentStartTime = currentStartTime.withMinuteOfHour(0);
    currentStartTime = currentStartTime.withSecondOfMinute(0);

    if (period.isSome() && period.get().hasStart()) {
      DateTime fromDC = new DateTime(period.get().getStart().getTime());
      fromDC = fromDC.withZone(DateTimeZone.UTC);
      currentStartTime = currentStartTime.withZone(DateTimeZone.UTC);
      currentStartTime = currentStartTime.withHourOfDay(fromDC.getHourOfDay());
      currentStartTime = currentStartTime.withMinuteOfHour(fromDC.getMinuteOfHour());
      currentStartTime = currentStartTime.withSecondOfMinute(fromDC.getSecondOfMinute());
    }
    return currentStartTime;
  }

  /**
   * Gets the current hour, minute and second from a dublin core period if available.
   *
   * @param period
   *          The current period from dublin core.
   * @return A new DateTime with the current hour, minute and second.
   */
  static DateTime getCurrentStartDate(Opt<DCMIPeriod> period) {
    DateTime currentStartDate = new DateTime();
    currentStartDate = currentStartDate.withZone(DateTimeZone.UTC);
    currentStartDate = currentStartDate.withYear(2001);
    currentStartDate = currentStartDate.withMonthOfYear(1);
    currentStartDate = currentStartDate.withDayOfMonth(1);

    if (period.isSome() && period.get().hasStart()) {
      DateTime fromDC = new DateTime(period.get().getStart().getTime());
      fromDC = fromDC.withZone(DateTimeZone.UTC);
      currentStartDate = currentStartDate.withZone(DateTimeZone.UTC);
      currentStartDate = currentStartDate.withYear(fromDC.getYear());
      currentStartDate = currentStartDate.withMonthOfYear(fromDC.getMonthOfYear());
      currentStartDate = currentStartDate.withDayOfMonth(fromDC.getDayOfMonth());
    }
    return currentStartDate;
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
  static void setTemporalStartDate(DublinCoreCatalog dc, MetadataField<?> field, EName ename) {
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
      // Get the current start time hours, minutes and seconds
      DateTime currentStartTime = getCurrentStartTime(period);
      // Setup the new start time
      DateTime startDateTime = new DateTime(startDate.getTime());
      startDateTime = startDateTime.withZone(DateTimeZone.UTC);
      startDateTime = startDateTime.withHourOfDay(currentStartTime.getHourOfDay());
      startDateTime = startDateTime.withMinuteOfHour(currentStartTime.getMinuteOfHour());
      startDateTime = startDateTime.withSecondOfMinute(currentStartTime.getSecondOfMinute());
      // Get the current end date based on new date and duration.
      DateTime endDate = new DateTime(startDateTime.toDate().getTime() + duration);
      dc.set(ename, EncodingSchemeUtils.encodePeriod(new DCMIPeriod(startDateTime.toDate(), endDate.toDate()),
              Precision.Second));
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
   * Sets the start time in a dublin core catalog to the right value and keeps the start date and duration the same.
   *
   * @param dc
   *          The dublin core catalog to adjust
   * @param field
   *          The metadata field that contains the start time.
   * @param ename
   *          The EName in the catalog to identify the property that has the dublin core period.
   */
  static void setTemporalStartTime(DublinCoreCatalog dc, MetadataField<?> field, EName ename) {
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
      // Get the current start date
      DateTime currentStartDate = getCurrentStartDate(period);
      // Setup the new start time
      DateTime startDateTime = new DateTime(startDate.getTime());
      startDateTime = startDateTime.withZone(DateTimeZone.UTC);
      startDateTime = startDateTime.withYear(currentStartDate.getYear());
      startDateTime = startDateTime.withMonthOfYear(currentStartDate.getMonthOfYear());
      startDateTime = startDateTime.withDayOfMonth(currentStartDate.getDayOfMonth());

      // Get the current end date based on new date and duration.
      DateTime endDate = new DateTime(startDateTime.toDate().getTime() + duration);
      dc.set(ename, EncodingSchemeUtils.encodePeriod(new DCMIPeriod(startDateTime.toDate(), endDate.toDate()),
              Precision.Second));
    } catch (ParseException e) {
      logger.error("Not able to parse date {} to update the dublin core because: {}", field.getValue(),
              ExceptionUtils.getStackTrace(e));
    }
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
      logger.debug("Unable to parse the duration's value '{}' as a long value. Trying it as a period next.", field
              .getValue().get());
    }
    if (duration < 1L) {
      duration = getDuration(period);
    }
    // Get the current start date
    DateTime startDateTime = getCurrentStartDateTime(period);
    // Get the current end date based on new date and duration.
    DateTime endDate = new DateTime(startDateTime.toDate().getTime() + duration);
    dc.set(ename, EncodingSchemeUtils.encodePeriod(new DCMIPeriod(startDateTime.toDate(), endDate.toDate()),
            Precision.Second));
  }

  @SuppressWarnings("unchecked")
  public static Map<String, MetadataField<?>> getDublinCoreProperties(Dictionary configProperties) {
    Map<String, MetadataField<?>> dublinCorePropertyMapByConfigurationName = new HashMap<String, MetadataField<?>>();
    for (Object configObject : Collections.list(configProperties.keys())) {
      String property = configObject.toString();
      if (getDublinCorePropertyName(property).isSome()) {
        MetadataField<?> dublinCoreProperty = dublinCorePropertyMapByConfigurationName.get(getDublinCorePropertyName(
                property).get());
        if (dublinCoreProperty == null) {
          dublinCoreProperty = new MetadataField();
        }
        dublinCoreProperty.setValue(getDublinCorePropertyKey(property).get(), configProperties.get(property).toString());
        dublinCorePropertyMapByConfigurationName.put(getDublinCorePropertyName(property).get(), dublinCoreProperty);
      }
    }
    Map<String, MetadataField<?>> dublinCorePropertyMap = new TreeMap<String, MetadataField<?>>();
    for (MetadataField dublinCoreProperty : dublinCorePropertyMapByConfigurationName.values()) {
      dublinCorePropertyMap.put(dublinCoreProperty.getOutputID(), dublinCoreProperty);
    }
    return dublinCorePropertyMap;
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
