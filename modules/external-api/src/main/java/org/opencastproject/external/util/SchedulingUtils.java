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
package org.opencastproject.external.util;

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static java.time.ZoneOffset.UTC;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;

import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

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
    private Opt<Date> startDate = Opt.none();
    private Opt<Date> endDate = Opt.none();
    private Opt<Long> duration = Opt.none();
    private Opt<String> agentId = Opt.none();
    private Opt<String> inputs = Opt.none();
    private Opt<RRule> rrule = Opt.none();

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

    public Opt<Date> getStartDate() {
      return startDate;
    }

    public void setStartDate(Opt<Date> startDate) {
      this.startDate = startDate;
    }

    public Opt<Date> getEndDate() {
      if (endDate.isSome()) {
        return endDate;
      } else if (startDate.isSome() && duration.isSome()) {
        return Opt.some(Date.from(startDate.get().toInstant().plusMillis(duration.get())));
      } else {
        return Opt.none();
      }
    }

    public void setEndDate(Opt<Date> endDate) {
      this.endDate = endDate;
    }

    public Opt<Long> getDuration() {
      if (duration.isSome()) {
        return duration;
      } else if (startDate.isSome() && endDate.isSome()) {
        return Opt.some(endDate.get().getTime() - startDate.get().getTime());
      } else {
        return Opt.none();
      }
    }

    public void setDuration(Opt<Long> duration) {
      this.duration = duration;
    }

    public Opt<String> getAgentId() {
      return agentId;
    }

    public void setAgentId(Opt<String> agentId) {
      this.agentId = agentId;
    }

    public Opt<String> getInputs() {
      return inputs;
    }

    public void setInputs(Opt<String> inputs) {
      this.inputs = inputs;
    }

    public Opt<RRule> getRrule() {
      return rrule;
    }

    public void setRrule(Opt<RRule> rrule) {
      this.rrule = rrule;
    }

    /**
     * @return A JSON representation of this ScheudlingInfo object.
     */
    public JObject toJson() {
      final List<Field> fields = new ArrayList<>();
      final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
      if (startDate.isSome()) {
        fields.add(f(JSON_KEY_START_DATE, dateFormatter.format(startDate.get().toInstant().atZone(UTC))));
      }
      if (endDate.isSome()) {
        fields.add(f(JSON_KEY_END_DATE, dateFormatter.format(endDate.get().toInstant().atZone(UTC))));
      }
      if (agentId.isSome()) {
        fields.add(f(JSON_KEY_AGENT_ID, agentId.get()));
      }
      if (inputs.isSome()) {
        fields.add(f(JSON_KEY_INPUTS, arr(inputs.get().split(","))));
      }
      return obj(fields);
    }

    /**
     * @return A JSON source representation of this SchedulingInfo as needed by the IndexService to create an event.
     */
    @SuppressWarnings("unchecked")
    public JSONObject toSource() {
      final JSONObject source = new JSONObject();
      if (rrule.isSome()) {
        source.put("type", "SCHEDULE_MULTIPLE");
      } else {
        source.put("type", "SCHEDULE_SINGLE");
      }
      final JSONObject sourceMetadata = new JSONObject();
      final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
      if (startDate.isSome()) {
        sourceMetadata.put("start", dateFormatter.format(startDate.get().toInstant().atZone(UTC)));
      }
      if (endDate.isSome()) {
        sourceMetadata.put("end", dateFormatter.format(endDate.get().toInstant().atZone(UTC)));
      }
      if (agentId.isSome()) {
        sourceMetadata.put("device", agentId.get());
      }
      if (getDuration().isSome()) {
        sourceMetadata.put("duration", String.valueOf(getDuration().get()));
      }
      if (rrule.isSome()) {
        sourceMetadata.put("rrule", rrule.get().getValue());
      }
      sourceMetadata.put("inputs", inputs.getOr(""));

      source.put("metadata", sourceMetadata);
      return source;
    }

    /**
     * Creates a new SchedulingInfo of this instance which uses start date, end date, and agent id form the given
     * {@link TechnicalMetadata} if they are not present in this instance.
     *
     * @param metadata
     *          The {@link TechnicalMetadata} of which to use start date, end date, and agent id in case they are missing.
     *
     * @return The new SchedulingInfo with start date, end date, and agent id set.
     */
    public SchedulingInfo merge(TechnicalMetadata metadata) {
      SchedulingInfo result = new SchedulingInfo(this);
      if (result.startDate.isNone()) {
        result.startDate = Opt.some(metadata.getStartDate());
      }
      if (result.endDate.isNone()) {
        result.endDate = Opt.some(metadata.getEndDate());
      }
      if (result.agentId.isNone()) {
        result.agentId = Opt.some(metadata.getAgentId());
      }
      return result;
    }

    /**
     * Parse the given json and create a new SchedulingInfo.
     *
     * @param json
     *          The JSONObject to parse.
     *
     * @return The SchedulingInfo instance represented by the given JSON.
     */
    public static SchedulingInfo of(JSONObject json) {
      final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;
      final SchedulingInfo schedulingInfo = new SchedulingInfo();
      final String startDate = (String) json.get(JSON_KEY_START_DATE);
      final String endDate = (String) json.get(JSON_KEY_END_DATE);
      final String durationString = (String) json.get(JSON_KEY_DURATION);
      final String agentId = (String) json.get(JSON_KEY_AGENT_ID);
      final JSONArray inputs = (JSONArray) json.get(JSON_KEY_INPUTS);
      final String rrule = (String) json.get(JSON_KEY_RRULE);
      if (isNotBlank(startDate)) {
        schedulingInfo.startDate = Opt.some(Date.from(Instant.from(dateFormatter.parse(startDate))));
      }
      if (isNotBlank(endDate)) {
        schedulingInfo.endDate = Opt.some(Date.from(Instant.from(dateFormatter.parse(endDate))));
      }
      if (isNotBlank(agentId)) {
        schedulingInfo.agentId = Opt.some(agentId);
      }
      if (isNotBlank(durationString)) {
        try {
          schedulingInfo.duration = Opt.some(Long.parseLong(durationString));
        } catch (Exception e) {
          throw new IllegalArgumentException("Invalid format of field 'duration'");
        }
      }

      if (isBlank(endDate) && isBlank(durationString)) {
        throw new IllegalArgumentException("Either 'end' or 'duration' must be specified");
      }

      if (inputs != null) {
        schedulingInfo.inputs = Opt.some(String.join(",", inputs));
      }
      if (isNotBlank(rrule)) {
        try {
          RRule parsedRrule = new RRule(rrule);
          parsedRrule.validate();
          schedulingInfo.rrule = Opt.some(parsedRrule);
        } catch (Exception e) {
          throw new IllegalArgumentException("Invalid RRule: " + rrule);
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
        result.startDate = Opt.some(technicalMetadata.getStartDate());
        result.endDate = Opt.some(technicalMetadata.getEndDate());
        result.agentId = Opt.some(technicalMetadata.getAgentId());
        String inputs = technicalMetadata.getCaptureAgentConfiguration().get(CaptureParameters.CAPTURE_DEVICE_NAMES);
        if (isNotBlank(inputs)) {
          result.inputs = Opt.some(inputs);
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
   * @param externalIndex
   *          The ExternalIndex to use for getting the corresponding events for the conflicting MediaPackages.
   *
   * @return A List of conflicting events, represented as JSON objects.
   *
   * @throws SearchIndexException
   *          If an event cannot be found.
   */
  public static List<JValue> convertConflictingEvents(
      Optional<String> checkedEventId,
      List<MediaPackage> mediaPackages,
      IndexService indexService,
      AbstractSearchIndex externalIndex
  ) throws SearchIndexException {
    final List<JValue> result = new ArrayList<>();
    for (MediaPackage mediaPackage : mediaPackages) {
      final Opt<Event> eventOpt = indexService.getEvent(mediaPackage.getIdentifier().compact(), externalIndex);
      if (eventOpt.isSome()) {
        final Event event = eventOpt.get();
        if (checkedEventId.isPresent() && checkedEventId.equals(event.getIdentifier())) {
          continue;
        }
        result.add(obj(f("start", v(event.getTechnicalStartTime())), f("end", v(event.getTechnicalEndTime())),
            f("title", v(event.getTitle()))));
      } else {
        logger.warn("Index out of sync! Conflicting event catalog {} not found on event index!",
            mediaPackage.getIdentifier().compact());
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

    if (schedulingInfo.getRrule().isSome()) {
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
