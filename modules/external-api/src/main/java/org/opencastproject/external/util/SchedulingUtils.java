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
package org.opencastproject.external.util;

import static java.time.ZoneOffset.UTC;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class SchedulingUtils {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SchedulingUtils.class);

  private static final String JSON_KEY_AGENT_ID = "agent_id";
  private static final String JSON_KEY_START_DATE = "start";
  private static final String JSON_KEY_END_DATE = "end";
  private static final String JSON_KEY_DURATION = "duration";
  private static final String JSON_KEY_INPUTS = "inputs";
  private static final String JSON_KEY_RRULE = "rrule";


  private SchedulingUtils() {
  }

  public static class SchedulingInfo {
    private Optional<Date> startDate = Optional.empty();
    private Optional<Date> endDate = Optional.empty();
    private Optional<Long> duration = Optional.empty();
    private Optional<String> agentId = Optional.empty();
    private Optional<String> inputs = Optional.empty();
    private Optional<RRule> rrule = Optional.empty();

    public SchedulingInfo() {
    }

    /**
     * Copy the given SchedulingInfo object.
     *
     * @param other
     *          The SchedulingInfo object to copy.
     */
    public SchedulingInfo(SchedulingInfo other) {
      this.startDate = other.startDate;
      this.endDate = other.endDate;
      this.duration = other.duration;
      this.agentId = other.agentId;
      this.inputs = other.inputs;
      this.rrule = other.rrule;
    }

    public Optional<Date> getStartDate() {
      return startDate;
    }

    public void setStartDate(Optional<Date> startDate) {
      this.startDate = startDate;
    }

    public Optional<Date> getEndDate() {
      if (endDate.isPresent()) {
        return endDate;
      } else if (startDate.isPresent() && duration.isPresent()) {
        return Optional.of(Date.from(startDate.get().toInstant().plusMillis(duration.get())));
      } else {
        return Optional.empty();
      }
    }

    public void setEndDate(Optional<Date> endDate) {
      this.endDate = endDate;
    }

    public Optional<Long> getDuration() {
      if (duration.isPresent()) {
        return duration;
      } else if (startDate.isPresent() && endDate.isPresent()) {
        return Optional.of(endDate.get().getTime() - startDate.get().getTime());
      } else {
        return Optional.empty();
      }
    }

    public void setDuration(Optional<Long> duration) {
      this.duration = duration;
    }

    public Optional<String> getAgentId() {
      return agentId;
    }

    public void setAgentId(Optional<String> agentId) {
      this.agentId = agentId;
    }

    public Optional<String> getInputs() {
      return inputs;
    }

    public void setInputs(Optional<String> inputs) {
      this.inputs = inputs;
    }

    public Optional<RRule> getRrule() {
      return rrule;
    }

    public void setRrule(Optional<RRule> rrule) {
      this.rrule = rrule;
    }

    /**
     * @return A JSON representation of this SchedulingInfo object.
     */
    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
      if (startDate.isPresent()) {
        json.addProperty(JSON_KEY_START_DATE, dateFormatter.format(startDate.get().toInstant().atZone(ZoneOffset.UTC)));
      }
      if (endDate.isPresent()) {
        json.addProperty(JSON_KEY_END_DATE, dateFormatter.format(endDate.get().toInstant().atZone(ZoneOffset.UTC)));
      }
      if (agentId.isPresent()) {
        json.addProperty(JSON_KEY_AGENT_ID, agentId.get());
      }
      if (inputs.isPresent()) {
        JsonArray inputsArray = new JsonArray();
        for (String input : inputs.get().split(",")) {
          inputsArray.add(new JsonPrimitive(input.trim()));
        }
        json.add(JSON_KEY_INPUTS, inputsArray);
      }
      return json;
    }

    /**
     * @return A JSON source representation of this SchedulingInfo as needed by the IndexService to create an event.
     */
    public JsonObject toSource() {
      final JsonObject source = new JsonObject();
      if (rrule.isPresent()) {
        source.addProperty("type", "SCHEDULE_MULTIPLE");
      } else {
        source.addProperty("type", "SCHEDULE_SINGLE");
      }
      final JsonObject sourceMetadata = new JsonObject();
      final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
      if (startDate.isPresent()) {
        sourceMetadata.addProperty("start", dateFormatter.format(startDate.get().toInstant().atZone(UTC)));
      }
      if (endDate.isPresent()) {
        sourceMetadata.addProperty("end", dateFormatter.format(endDate.get().toInstant().atZone(UTC)));
      }
      if (agentId.isPresent()) {
        sourceMetadata.addProperty("device", agentId.get());
      }
      if (getDuration().isPresent()) {
        sourceMetadata.addProperty("duration", String.valueOf(getDuration().get()));
      }
      if (rrule.isPresent()) {
        sourceMetadata.addProperty("rrule", rrule.get().getValue());
      }
      sourceMetadata.addProperty("inputs", inputs.orElse(""));

      source.add("metadata", sourceMetadata);
      return source;
    }

    /**
     * Creates a new SchedulingInfo of this instance which uses start date, end date, and agent id form the given
     * {@link TechnicalMetadata} if they are not present in this instance.
     *
     * @param metadata
     *        The {@link TechnicalMetadata} of which to use start date, end date, and agent id in case they are missing.
     *
     * @return The new SchedulingInfo with start date, end date, and agent id set.
     */
    public SchedulingInfo merge(TechnicalMetadata metadata) {
      SchedulingInfo result = new SchedulingInfo(this);
      if (result.startDate.isEmpty()) {
        result.startDate = Optional.of(metadata.getStartDate());
      }
      if (result.endDate.isEmpty()) {
        result.endDate = Optional.of(metadata.getEndDate());
      }
      if (result.agentId.isEmpty()) {
        result.agentId = Optional.of(metadata.getAgentId());
      }
      return result;
    }

    /**
     * Parse the given json and create a new SchedulingInfo.
     *
     * @param json
     *          The JSON object to parse.
     *
     * @return The SchedulingInfo instance represented by the given JSON.
     */
    /** Render an element as plain text rather than as JSON. */
    private static String asPlainText(JsonElement value) {
      return value.isJsonPrimitive() ? value.getAsString() : value.toString();
    }

    /** Read a member as plain text, null when absent or JSON null. */
    private static String getString(JsonObject json, String key) {
      final JsonElement value = json.get(key);
      if (value == null || value.isJsonNull()) {
        return null;
      }
      return value.isJsonPrimitive() ? value.getAsString() : value.toString();
    }

    public static SchedulingInfo of(JsonObject json) {
      final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
      final SchedulingInfo schedulingInfo = new SchedulingInfo();
      final String startDate = getString(json, JSON_KEY_START_DATE);
      final String endDate = getString(json, JSON_KEY_END_DATE);
      final String agentId = getString(json, JSON_KEY_AGENT_ID);
      final JsonArray inputs = json.getAsJsonArray(JSON_KEY_INPUTS);
      final String rrule = getString(json, JSON_KEY_RRULE);

      // Special handling because the original implementation required String but now we require long
      final String durationString = getString(json, JSON_KEY_DURATION);

      if (isNotBlank(startDate)) {
        schedulingInfo.startDate = Optional.of(Date.from(Instant.from(dateFormatter.parse(startDate))));
      }
      if (isNotBlank(endDate)) {
        schedulingInfo.endDate = Optional.of(Date.from(Instant.from(dateFormatter.parse(endDate))));
      }
      if (isNotBlank(agentId)) {
        schedulingInfo.agentId = Optional.of(agentId);
      }
      if (isNotBlank(durationString)) {
        try {
          schedulingInfo.duration = Optional.of(Long.parseLong(durationString));
        } catch (Exception e) {
          throw new IllegalArgumentException("Invalid format of field 'duration'");
        }
      }

      if (isBlank(endDate) && isBlank(durationString)) {
        throw new IllegalArgumentException("Either 'end' or 'duration' must be specified");
      }

      if (inputs != null) {
        schedulingInfo.inputs = Optional.of(StreamSupport.stream(inputs.spliterator(), false)
            .map(SchedulingInfo::asPlainText)
            .collect(Collectors.joining(",")));
      }
      if (isNotBlank(rrule)) {
        try {
          RRule parsedRrule = new RRule(rrule);
          parsedRrule.validate();
          schedulingInfo.rrule = Optional.of(parsedRrule);
        } catch (Exception e) {
          throw new IllegalArgumentException("Invalid RRule: " + rrule);
        }
        if (isBlank(durationString) || isBlank(startDate) || isBlank(endDate)) {
          throw new IllegalArgumentException("'start', 'end' and 'duration' must be specified when 'rrule' is "
              + "specified");
        }
      }
      return schedulingInfo;
    }

    /**
     * Get the SchedulingInfo for the given event id.
     *
     * @param eventId
     *          The id of the event to get the SchedulingInfo for.
     * @param schedulerService
     *          The {@link SchedulerService} to query for the event id.
     *
     * @return The SchedulingInfo for the given event id.
     *
     * @throws UnauthorizedException
     *          If the {@link SchedulerService} cannot be queried due to missing authorization.
     * @throws SchedulerException
     *          In case internal errors occur within the {@link SchedulerService}.
     */
    public static SchedulingInfo of(String eventId, SchedulerService schedulerService)
            throws UnauthorizedException, SchedulerException {
      final SchedulingInfo result = new SchedulingInfo();
      try {
        final TechnicalMetadata technicalMetadata = schedulerService.getTechnicalMetadata(eventId);
        result.startDate = Optional.of(technicalMetadata.getStartDate());
        result.endDate = Optional.of(technicalMetadata.getEndDate());
        result.agentId = Optional.of(technicalMetadata.getAgentId());
        String inputs = technicalMetadata.getCaptureAgentConfiguration().get(CaptureParameters.CAPTURE_DEVICE_NAMES);
        if (isNotBlank(inputs)) {
          result.inputs = Optional.of(inputs);
        }
        return result;
      } catch (NotFoundException e) {
        return result;
      }
    }
  }

  /**
   * Convert the given list of {@link MediaPackage} elements to a JSON used to tell which events are causing conflicts.
   *
   * @param checkedEventId
   *          The id of the event which was checked for conflicts. May be empty if an rrule was checked.
   * @param mediaPackages
   *          The conflicting {@link MediaPackage}s.
   * @param indexService
   *          The {@link IndexService} for getting the corresponding events for the conflicting {@link MediaPackage}s.
   * @param elasticsearchIndex
   *          The index to use for getting the corresponding events for the conflicting MediaPackages.
   *
   * @return A List of conflicting events, represented as JSON objects.
   *
   * @throws SearchIndexException
   *          If an event cannot be found.
   */
  public static List<JsonObject> convertConflictingEvents(
      Optional<String> checkedEventId,
      List<MediaPackage> mediaPackages,
      IndexService indexService,
      ElasticsearchIndex elasticsearchIndex
  ) throws SearchIndexException {
    List<JsonObject> result = new ArrayList<>();
    for (MediaPackage mediaPackage : mediaPackages) {
      Optional<Event> eventOpt = indexService.getEvent(mediaPackage.getIdentifier().toString(), elasticsearchIndex);
      if (eventOpt.isPresent()) {
        final Event event = eventOpt.get();
        if (checkedEventId.isPresent() && checkedEventId.get().equals(event.getIdentifier())) {
          continue;
        }

        JsonObject eventJson = new JsonObject();
        if (event.getTechnicalStartTime() != null) {
          eventJson.addProperty("start", event.getTechnicalStartTime().toString());
        }
        if (event.getTechnicalEndTime() != null) {
          eventJson.addProperty("end", event.getTechnicalEndTime().toString());
        }
        eventJson.addProperty("title", event.getTitle());

        result.add(eventJson);
      } else {
        logger.warn("Index out of sync! Conflicting event catalog {} not found on event index!",
            mediaPackage.getIdentifier().toString());
      }
    }
    return result;
  }

  /**
   * Get the conflicting events for the given SchedulingInfo.
   *
   * @param schedulingInfo
   *          The SchedulingInfo to check for conflicts.
   * @param agentStateService
   *          The {@link CaptureAgentStateService} to use for retrieving capture agents.
   * @param schedulerService
   *          The {@link SchedulerService} to use for conflict checking.
   * @return
   *          A list of {@link MediaPackage} elements which cause conflicts with the given SchedulingInfo.
   *
   * @throws NotFoundException
   *          If the capture agent cannot be found.
   * @throws UnauthorizedException
   *          If the {@link SchedulerService} cannot be queried due to missing authorization.
   * @throws SchedulerException
   *          In case internal errors occur within the {@link SchedulerService}.
   */
  public static List<MediaPackage> getConflictingEvents(
      SchedulingInfo schedulingInfo,
      CaptureAgentStateService agentStateService,
      SchedulerService schedulerService
  ) throws NotFoundException, UnauthorizedException, SchedulerException {

    if (schedulingInfo.getRrule().isPresent()) {
      final Agent agent = agentStateService.getAgent(schedulingInfo.getAgentId().get());
      String timezone = agent.getConfiguration().getProperty("capture.device.timezone");
      if (StringUtils.isBlank(timezone)) {
        timezone = TimeZone.getDefault().getID();
        logger.warn("No 'capture.device.timezone' set on agent {}. The default server timezone {} will be used.",
            schedulingInfo.getAgentId().get(), timezone);
      }
      return schedulerService.findConflictingEvents(
          schedulingInfo.getAgentId().get(),
          schedulingInfo.getRrule().get(),
          schedulingInfo.getStartDate().get(),
          schedulingInfo.getEndDate().get(),
          schedulingInfo.getDuration().get(),
          TimeZone.getTimeZone(timezone)
      );
    }

    return schedulerService.findConflictingEvents(
        schedulingInfo.getAgentId().get(),
        schedulingInfo.getStartDate().get(),
        schedulingInfo.getEndDate().get()
    );
  }

}
