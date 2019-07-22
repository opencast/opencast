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

package org.opencastproject.adminui.util;

import static org.opencastproject.adminui.endpoint.AbstractEventEndpoint.SCHEDULING_AGENT_ID_KEY;
import static org.opencastproject.adminui.endpoint.AbstractEventEndpoint.SCHEDULING_END_KEY;
import static org.opencastproject.adminui.endpoint.AbstractEventEndpoint.SCHEDULING_START_KEY;

import org.opencastproject.adminui.index.AdminUISearchIndex;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.mediapackage.MediaPackageElements;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


/**
 * This class holds utility functions which are related to the bulk update feature for events.
 */
public final class BulkUpdateUtil {

  private static final JSONParser parser = new JSONParser();

  private BulkUpdateUtil() {
  }

  /**
   * Wraps the IndexService.getEvent() method to convert SearchIndexExceptions into RuntimeExceptions. Useful when
   * using Java's functional programming features.
   *
   * @param indexSvc The IndexService instance.
   * @param index The index to get the event from.
   * @param id The id of the event to get.
   * @return An optional holding the event or nothing, if not found.
   */
  public static Optional<Event> getEvent(
    final IndexService indexSvc,
    final AdminUISearchIndex index,
    final String id) {
    try {
      final Event event = indexSvc.getEvent(id, index).orNull();
      return Optional.ofNullable(event);
    } catch (SearchIndexException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Takes the given scheduling information and completes the event start and end dates as well as the duration for the
   * given event. If the weekday shall be changed, the start and end dates are adjusted accordingly.
   *
   * @param event The event to complete the scheduling information for.
   * @param scheduling The (yet incomplete) scheduling information to complete.
   * @return The completed scheduling information, adjusted for the given event.
   */
  @SuppressWarnings("unchecked")
  public static JSONObject addSchedulingDates(final Event event, final JSONObject scheduling) {
    final JSONObject result = deepCopy(scheduling);
    ZonedDateTime startDate = ZonedDateTime.parse(event.getRecordingStartDate());
    ZonedDateTime endDate = ZonedDateTime.parse(event.getRecordingEndDate());
    final InternalDuration oldDuration = InternalDuration.of(startDate.toInstant(), endDate.toInstant());
    final ZoneId timezone = ZoneId.of((String) result.get("timezone"));

    // The client only sends start time hours and/or minutes. We have to apply this to each event to get a full date.
    if (result.containsKey(SCHEDULING_START_KEY)) {
      startDate = adjustedSchedulingDate(result, SCHEDULING_START_KEY, startDate, timezone);
    }
    // The client only sends end time hours and/or minutes. We have to apply this to each event to get a full date.
    if (result.containsKey(SCHEDULING_END_KEY)) {
      endDate = adjustedSchedulingDate(result, SCHEDULING_END_KEY, endDate, timezone);
    }
    if (endDate.isBefore(startDate)) {
      endDate = endDate.plusDays(1);
    }

    // If duration is set, we have to adjust the end or start date.
    if (result.containsKey("duration")) {
      final JSONObject time = (JSONObject) result.get("duration");
      final InternalDuration newDuration = new InternalDuration(oldDuration);
      if (time.containsKey("hour")) {
        newDuration.hours = (Long) time.get("hour");
      }
      if (time.containsKey("minute")) {
        newDuration.minutes = (Long) time.get("minute");
      }
      if (time.containsKey("second")) {
        newDuration.seconds = (Long) time.get("second");
      }
      if (result.containsKey(SCHEDULING_END_KEY)) {
        startDate = endDate.minusHours(newDuration.hours)
          .minusMinutes(newDuration.minutes)
          .minusSeconds(newDuration.seconds);
      } else {
        endDate = startDate.plusHours(newDuration.hours)
          .plusMinutes(newDuration.minutes)
          .plusSeconds(newDuration.seconds);
      }
    }

    // Setting the weekday means that the event should be moved to the new weekday within the same week
    if (result.containsKey("weekday")) {
      final String weekdayAbbrev = ((String) result.get("weekday"));
      if (weekdayAbbrev != null) {
        final DayOfWeek newWeekDay = Arrays.stream(DayOfWeek.values())
          .filter(d -> d.name().startsWith(weekdayAbbrev.toUpperCase()))
          .findAny()
          .orElseThrow(() -> new IllegalArgumentException("Cannot parse weekday: " + weekdayAbbrev));
        final int daysDiff = newWeekDay.getValue() - startDate.getDayOfWeek().getValue();
        startDate = startDate.plusDays(daysDiff);
        endDate = endDate.plusDays(daysDiff);
      }
    }

    result.put(SCHEDULING_START_KEY, startDate.format(DateTimeFormatter.ISO_INSTANT));
    result.put(SCHEDULING_END_KEY, endDate.format(DateTimeFormatter.ISO_INSTANT));
    return result;
  }

  /**
   * Creates a json object containing meta data based on the given scheduling information.
   *
   * @param scheduling The scheduling information to extract meta data from.
   * @return The meta data, consisting of location, startDate, and duration.
   */
  @SuppressWarnings("unchecked")
  public static JSONObject toNonTechnicalMetadataJson(final JSONObject scheduling) {
    final List<JSONObject> fields = new ArrayList<>();
    if (scheduling.containsKey(SCHEDULING_AGENT_ID_KEY)) {
      final JSONObject locationJson = new JSONObject();
      locationJson.put("id", "location");
      locationJson.put("value", scheduling.get(SCHEDULING_AGENT_ID_KEY));
      fields.add(locationJson);
    }
    if (scheduling.containsKey(SCHEDULING_START_KEY) && scheduling.containsKey(SCHEDULING_END_KEY)) {
      final JSONObject startDateJson = new JSONObject();
      startDateJson.put("id", "startDate");
      final String startDate = Instant.parse((String) scheduling.get(SCHEDULING_START_KEY))
        .atOffset(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ".000Z";
      startDateJson.put("value", startDate);
      fields.add(startDateJson);

      final JSONObject durationJson = new JSONObject();
      durationJson.put("id", "duration");
      final Instant start = Instant.parse((String) scheduling.get(SCHEDULING_START_KEY));
      final Instant end = Instant.parse((String) scheduling.get(SCHEDULING_END_KEY));
      final InternalDuration duration = InternalDuration.of(start, end);
      durationJson.put("value", duration.toString());
      fields.add(durationJson);
    }

    final JSONObject result = new JSONObject();
    result.put("flavor", MediaPackageElements.EPISODE.toString());
    result.put("title", CommonEventCatalogUIAdapter.EPISODE_TITLE);
    result.put("fields", fields);
    return result;
  }

  /**
   * Merges all fields of the given meta data json objects into one object.
   *
   * @param first The first meta data json object.
   * @param second The second meta data json object.
   * @return A new json meta data object, containing the field of both input objects.
   */
  @SuppressWarnings("unchecked")
  public static JSONObject mergeMetadataFields(final JSONObject first, final JSONObject second) {
    if (first == null) {
      return second;
    }
    if (second == null) {
      return first;
    }
    final JSONObject result = deepCopy(first);
    final Collection fields = (Collection) result.get("fields");
    fields.addAll((Collection) second.get("fields"));
    return result;
  }

  private static JSONObject deepCopy(final JSONObject o) {
    try {
      return (JSONObject) parser.parse(o.toJSONString());
    } catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static class InternalDuration {
    private long hours;
    private long minutes;
    private long seconds;

    InternalDuration() {
    }

    InternalDuration(final InternalDuration other) {
      this.hours = other.hours;
      this.minutes = other.minutes;
      this.seconds = other.seconds;
    }

    public static InternalDuration of(final Instant start, final Instant end) {
      final InternalDuration result = new InternalDuration();
      final Duration duration = Duration.between(start, end);
      result.hours = duration.toHours();
      result.minutes = duration.minusHours(result.hours).toMinutes();
      result.seconds = duration.minusHours(result.hours).minusMinutes(result.minutes).getSeconds();
      return result;
    }

    @Override
    public String toString() {
      return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
  }

  private static ZonedDateTime adjustedSchedulingDate(
    final JSONObject scheduling,
    final String dateKey,
    final ZonedDateTime date,
    final ZoneId timezone) {
    final JSONObject time = (JSONObject) scheduling.get(dateKey);
    ZonedDateTime result = date.withZoneSameInstant(timezone);
    if (time.containsKey("hour")) {
      final int hour = Math.toIntExact((Long) time.get("hour"));
      result = result.withHour(hour);
    }
    if (time.containsKey("minute")) {
      final int minute = Math.toIntExact((Long) time.get("minute"));
      result = result.withMinute(minute);
    }
    return result.withZoneSameInstant(ZoneOffset.UTC);
  }

  /**
   * Model class for one group of update instructions
   */
  public static class BulkUpdateInstructionGroup {
    private final List<String> eventIds;
    private final JSONObject metadata;
    private final JSONObject scheduling;

    /**
     * Create a new group from parsed JSON data
     *
     * @param eventIds Event IDs in this group
     * @param metadata Metadata for this group
     * @param scheduling Scheduling for this group
     */
    public BulkUpdateInstructionGroup(final List<String> eventIds, final JSONObject metadata, final JSONObject scheduling) {
      this.eventIds = eventIds;
      this.metadata = metadata;
      this.scheduling = scheduling;
    }

    /**
     * Get the list of IDs of events to apply the bulk update to.
     *
     * @return The list of IDs of the events to apply the bulk update to.
     */
    public List<String> getEventIds() {
      return eventIds;
    }

    /**
     * Get the meta data update to apply.
     *
     * @return The meta data update to apply.
     */
    public JSONObject getMetadata() {
      return metadata;
    }

    /**
     *  Get the scheduling information update to apply.
     *
     * @return The scheduling information update to apply.
     */
    public JSONObject getScheduling() {
      return scheduling;
    }
  }

  /**
   * Model class for the bulk update instructions which are sent by the UI.
   */
  public static class BulkUpdateInstructions {
    private static final String KEY_EVENTS = "events";
    private static final String KEY_METADATA = "metadata";
    private static final String KEY_SCHEDULING = "scheduling";

    private final List<BulkUpdateInstructionGroup> groups;

    /**
     * Create a new instance by parsing the given json String.
     *
     * @param json The json serialized version of the bulk update instructions sent by the UI.
     *
     * @throws IllegalArgumentException If the json string cannot be parsed.
     */
    @SuppressWarnings("unchecked")
    public BulkUpdateInstructions(final String json) throws IllegalArgumentException {
      try {
        final JSONArray root = (JSONArray) parser.parse(json);
        groups = new ArrayList<>(root.size());
        for (final Object jsonGroup : root) {
          final JSONObject jsonObject = (JSONObject) jsonGroup;
          final JSONArray eventIds = (JSONArray) jsonObject.get(KEY_EVENTS);
          final JSONObject metadata = (JSONObject) jsonObject.get(KEY_METADATA);
          final JSONObject scheduling = (JSONObject) jsonObject.get(KEY_SCHEDULING);
          groups.add(new BulkUpdateInstructionGroup(eventIds, metadata, scheduling));
        }
      } catch (final ParseException e) {
        throw new IllegalArgumentException(e);
      }
    }

    public List<BulkUpdateInstructionGroup> getGroups() {
      return groups;
    }
  }

}
