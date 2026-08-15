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

package org.opencastproject.adminui.util;

import static org.opencastproject.adminui.endpoint.AbstractEventEndpoint.SCHEDULING_AGENT_ID_KEY;
import static org.opencastproject.adminui.endpoint.AbstractEventEndpoint.SCHEDULING_END_KEY;
import static org.opencastproject.adminui.endpoint.AbstractEventEndpoint.SCHEDULING_START_KEY;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.util.GsonUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


/**
 * This class holds utility functions which are related to the bulk update feature for events.
 */
public final class BulkUpdateUtil {


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
      final ElasticsearchIndex index,
      final String id) {
    try {
      return indexSvc.getEvent(id, index);
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
  public static JsonObject addSchedulingDates(final Event event, final JsonObject scheduling) {
    final JsonObject result = scheduling.deepCopy();
    ZonedDateTime startDate = ZonedDateTime.parse(event.getRecordingStartDate());
    ZonedDateTime endDate = ZonedDateTime.parse(event.getRecordingEndDate());
    final InternalDuration oldDuration = InternalDuration.of(startDate.toInstant(), endDate.toInstant());
    final ZoneId timezone = ZoneId.of(GsonUtil.getStringOrNull(result, "timezone"));

    // The client only sends start time hours and/or minutes. We have to apply this to each event to get a full date.
    if (result.has(SCHEDULING_START_KEY)) {
      startDate = adjustedSchedulingDate(result, SCHEDULING_START_KEY, startDate, timezone);
    }
    // The client only sends end time hours and/or minutes. We have to apply this to each event to get a full date.
    if (result.has(SCHEDULING_END_KEY)) {
      endDate = adjustedSchedulingDate(result, SCHEDULING_END_KEY, endDate, timezone);
    }
    if (endDate.isBefore(startDate)) {
      endDate = endDate.plusDays(1);
    }

    // If duration is set, we have to adjust the end or start date.
    if (result.has("duration")) {
      final JsonObject time = result.getAsJsonObject("duration");
      final InternalDuration newDuration = new InternalDuration(oldDuration);
      if (time.has("hour")) {
        newDuration.hours = time.get("hour").getAsLong();
      }
      if (time.has("minute")) {
        newDuration.minutes = time.get("minute").getAsLong();
      }
      if (time.has("second")) {
        newDuration.seconds = time.get("second").getAsLong();
      }
      if (result.has(SCHEDULING_END_KEY)) {
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
    if (result.has("weekday")) {
      final String weekdayAbbrev = GsonUtil.getStringOrNull(result, "weekday");
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

    result.addProperty(SCHEDULING_START_KEY, startDate.format(DateTimeFormatter.ISO_INSTANT));
    result.addProperty(SCHEDULING_END_KEY, endDate.format(DateTimeFormatter.ISO_INSTANT));
    return result;
  }

  /**
   * Creates a json object containing meta data based on the given scheduling information.
   *
   * @param scheduling The scheduling information to extract meta data from.
   * @return The meta data, consisting of location, startDate, and duration.
   */
  public static JsonObject toNonTechnicalMetadataJson(final JsonObject scheduling) {
    final JsonArray fields = new JsonArray();
    if (scheduling.has(SCHEDULING_AGENT_ID_KEY)) {
      final JsonObject locationJson = new JsonObject();
      locationJson.addProperty("id", "location");
      locationJson.add("value", scheduling.get(SCHEDULING_AGENT_ID_KEY));
      fields.add(locationJson);
    }
    if (scheduling.has(SCHEDULING_START_KEY) && scheduling.has(SCHEDULING_END_KEY)) {
      final JsonObject startDateJson = new JsonObject();
      startDateJson.addProperty("id", "startDate");
      final String startDate = Instant.parse(GsonUtil.getStringOrNull(scheduling, SCHEDULING_START_KEY))
          .atOffset(ZoneOffset.UTC)
          .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ".000Z";
      startDateJson.addProperty("value", startDate);
      fields.add(startDateJson);

      final JsonObject durationJson = new JsonObject();
      durationJson.addProperty("id", "duration");
      final Instant start = Instant.parse(GsonUtil.getStringOrNull(scheduling, SCHEDULING_START_KEY));
      final Instant end = Instant.parse(GsonUtil.getStringOrNull(scheduling, SCHEDULING_END_KEY));
      final InternalDuration duration = InternalDuration.of(start, end);
      durationJson.addProperty("value", duration.toString());
      fields.add(durationJson);
    }

    final JsonObject result = new JsonObject();
    result.addProperty("flavor", MediaPackageElements.EPISODE.toString());
    result.addProperty("title", CommonEventCatalogUIAdapter.EPISODE_TITLE);
    result.add("fields", fields);
    return result;
  }

  /**
   * Merges all fields of the given meta data json objects into one object.
   *
   * @param first The first meta data json object.
   * @param second The second meta data json object.
   * @return A new json meta data object, containing the field of both input objects.
   */
  public static JsonObject mergeMetadataFields(final JsonObject first, final JsonObject second) {
    if (first == null) {
      return second;
    }
    if (second == null) {
      return first;
    }
    final JsonObject result = first.deepCopy();
    result.getAsJsonArray("fields").addAll(second.getAsJsonArray("fields"));
    return result;
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
      final JsonObject scheduling,
      final String dateKey,
      final ZonedDateTime date,
      final ZoneId timezone) {
    final JsonObject time = scheduling.getAsJsonObject(dateKey);
    ZonedDateTime result = date.withZoneSameInstant(timezone);
    if (time.has("hour")) {
      final int hour = Math.toIntExact(time.get("hour").getAsLong());
      result = result.withHour(hour);
    }
    if (time.has("minute")) {
      final int minute = Math.toIntExact(time.get("minute").getAsLong());
      result = result.withMinute(minute);
    }
    return result.withZoneSameInstant(ZoneOffset.UTC);
  }

  /**
   * Model class for one group of update instructions
   */
  public static class BulkUpdateInstructionGroup {
    private final List<String> eventIds;
    private final JsonObject metadata;
    private final JsonObject scheduling;

    /**
     * Create a new group from parsed JSON data
     *
     * @param eventIds Event IDs in this group
     * @param metadata Metadata for this group
     * @param scheduling Scheduling for this group
     */
    public BulkUpdateInstructionGroup(final List<String> eventIds, final JsonObject metadata,
        final JsonObject scheduling) {
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
    public JsonObject getMetadata() {
      return metadata;
    }

    /**
     *  Get the scheduling information update to apply.
     *
     * @return The scheduling information update to apply.
     */
    public JsonObject getScheduling() {
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
    public BulkUpdateInstructions(final String json) throws IllegalArgumentException {
      try {
        final JsonArray root = GsonUtil.gson().fromJson(json, JsonArray.class);
        groups = new ArrayList<>(root.size());
        for (final JsonElement jsonGroup : root) {
          final JsonObject jsonObject = jsonGroup.getAsJsonObject();
          final List<String> eventIds = new ArrayList<>();
          jsonObject.getAsJsonArray(KEY_EVENTS).forEach(id -> eventIds.add(id.getAsString()));
          final JsonObject metadata = jsonObject.getAsJsonObject(KEY_METADATA);
          final JsonObject scheduling = jsonObject.getAsJsonObject(KEY_SCHEDULING);
          groups.add(new BulkUpdateInstructionGroup(eventIds, metadata, scheduling));
        }
      } catch (final JsonParseException | IllegalStateException | NullPointerException e) {
        throw new IllegalArgumentException(e);
      }
    }

    public List<BulkUpdateInstructionGroup> getGroups() {
      return groups;
    }
  }

}
