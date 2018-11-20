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

package org.opencastproject.message.broker.api.scheduler;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * {@link Serializable} class that represents all of the possible messages sent through a SchedulerService queue.
 */
public class SchedulerItem implements Serializable {
  private static final long serialVersionUID = 6061069989788904237L;

  private static final Gson gson = new Gson();

  public static final String SCHEDULER_QUEUE_PREFIX = "SCHEDULER.";

  public static final String SCHEDULER_QUEUE = SCHEDULER_QUEUE_PREFIX + "QUEUE";

  private final String event;
  private final String properties;
  private final String acl;
  private final String agentId;
  private final long end;
  private final String optOut;
  private final String presenters;
  private final String blacklisted;
  private final String reviewStatus;
  private final long reviewDate;
  private final String recordingState;
  private final long start;
  private final Long lastHeardFrom;
  private final Type type;

  public enum Type {
    UpdateCatalog, UpdateProperties, UpdateAcl, UpdateAgentId, UpdateOptOut, UpdateBlacklist, UpdateEnd, UpdatePresenters, UpdateReviewStatus, UpdateRecordingStatus, UpdateStart, DeleteRecordingStatus, Delete
  };

  /**
   * @param event
   *          The event details to update to.
   * @return Builds {@link SchedulerItem} for updating a scheduled event.
   */
  public static SchedulerItem updateCatalog(DublinCoreCatalog event) {
    return new SchedulerItem(event);
  }

  /**
   * @param properties
   *          The new properties to update to.
   * @return Builds {@link SchedulerItem} for updating the properties of an event.
   */
  public static SchedulerItem updateProperties(Map<String, String> properties) {
    return new SchedulerItem(properties);
  }

  /**
   * @return Builds {@link SchedulerItem} for deleting an event.
   */
  public static SchedulerItem delete() {
    return new SchedulerItem(Type.Delete);
  }

  /**
   * @param accessControlList
   *          the access control list
   * @return Builds {@link SchedulerItem} for updating the access control list of an event.
   */
  public static SchedulerItem updateAcl(AccessControlList accessControlList) {
    return new SchedulerItem(accessControlList);
  }

  /**
   * @param optOut
   *          the opt out status
   * @return Builds {@link SchedulerItem} for updating the opt out status of an event.
   */
  public static SchedulerItem updateOptOut(boolean optOut) {
    return new SchedulerItem(optOut, Type.UpdateOptOut);
  }

  /**
   * @param blacklisted
   *          the blacklist status
   * @return Builds {@link SchedulerItem} for updating the blacklist status of an event.
   */
  public static SchedulerItem updateBlacklist(boolean blacklisted) {
    return new SchedulerItem(blacklisted, Type.UpdateBlacklist);
  }

  /**
   * @param reviewStatus
   *          the review status
   * @param reviewDate
   *          the review date
   * @return Builds {@link SchedulerItem} for updating the review status of an event.
   */
  public static SchedulerItem updateReviewStatus(ReviewStatus reviewStatus, Date reviewDate) {
    return new SchedulerItem(reviewStatus, reviewDate);
  }

  /**
   * @param state
   *          The recording state
   * @param lastHeardFrom
   *          The recording last heard from date
   * @return Builds {@link SchedulerItem} for updating a recording.
   */
  public static SchedulerItem updateRecordingStatus(String state, Long lastHeardFrom) {
    return new SchedulerItem(state, lastHeardFrom);
  }

  /**
   * @param start
   *        The new start time for the event.
   * @return Builds {@link SchedulerItem} for updating the start of an event.
   */
  public static SchedulerItem updateStart(Date start) {
    return new SchedulerItem(start, null, Type.UpdateStart);
  }

  /**
   * @param end
   *        The new end time for the event.
   * @return Builds {@link SchedulerItem} for updating the end of an event.
   */
  public static SchedulerItem updateEnd(Date end) {
    return new SchedulerItem(null, end, Type.UpdateEnd);
  }

  /**
   * @param presenters
   *        The new set of presenters for the event.
   * @return Builds {@link SchedulerItem} for updating the presenters of an event.
   */
  public static SchedulerItem updatePresenters(Set<String> presenters) {
    return new SchedulerItem(presenters);
  }

  /**
   * @param agentId
   *        The new agent id for the event.
   * @return Builds {@link SchedulerItem} for updating the agent id of an event.
   */
  public static SchedulerItem updateAgent(String agentId) {
    return new SchedulerItem(agentId);
  }

  /**
   * @return Builds {@link SchedulerItem} for deleting a recording.
   */
  public static SchedulerItem deleteRecordingState() {
    return new SchedulerItem(Type.DeleteRecordingStatus);
  }

  /**
   * Constructor to build an update event {@link SchedulerItem}.
   *
   * @param event
   *          The event details to update.
   */
  public SchedulerItem(DublinCoreCatalog event) {
    try {
      this.event = event.toXmlString();
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    this.properties = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = -1;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = -1;
    this.recordingState = null;
    this.start = -1;
    this.lastHeardFrom = null;
    this.type = Type.UpdateCatalog;
  }

  /**
   * Constructor to build an update properties for an event {@link SchedulerItem}.
   *
   * @param properties
   *          The properties to update.
   */
  public SchedulerItem(Map<String, String> properties) {
    this.event = null;
    this.properties = serializeProperties(properties);
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = -1;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = -1;
    this.recordingState = null;
    this.start = -1;
    this.lastHeardFrom = null;
    this.type = Type.UpdateProperties;
  }

  /**
   * Constructor to build a delete event {@link SchedulerItem}.
   *
   */
  public SchedulerItem(Type type) {
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = -1;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = -1;
    this.recordingState = null;
    this.start = -1;
    this.lastHeardFrom = null;
    this.type = type;
  }

  /**
   * Constructor to build an update access control list event {@link SchedulerItem}.
   *
   * @param accessControlList
   *          The access control list
   */
  public SchedulerItem(AccessControlList accessControlList) {
    this.event = null;
    this.properties = null;
    try {
      this.acl = AccessControlParser.toJson(accessControlList);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    this.agentId = null;
    this.blacklisted = null;
    this.end = -1;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = -1;
    this.recordingState = null;
    this.start = -1;
    this.lastHeardFrom = null;
    this.type = Type.UpdateAcl;
  }

  /**
   * Constructor to build an update blacklist status event {@link SchedulerItem} or an opt out status event.
   *
   * @param x
   *          The blacklist status or the opt out status
   * @param type
   *          The type of boolean to set
   */
  public SchedulerItem(boolean x, Type type) {
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.agentId = null;
    if (type == Type.UpdateBlacklist) {
      this.blacklisted = gson.toJson(x);
    } else {
      this.blacklisted = null;
    }
    this.end = -1;
    if (type == Type.UpdateOptOut) {
      this.optOut = gson.toJson(x);
    } else {
      this.optOut = null;
    }
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = -1;
    this.recordingState = null;
    this.start = -1;
    this.lastHeardFrom = null;
    this.type = type;
  }

  /**
   * Constructor to build an update review status event {@link SchedulerItem}.
   *
   * @param reviewStatus
   *          The review status
   * @param reviewDate
   *          The review date
   */
  public SchedulerItem(ReviewStatus reviewStatus, Date reviewDate) {
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = -1;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = reviewStatus.toString();
    this.reviewDate = reviewDate == null ? -1 : reviewDate.getTime();
    this.recordingState = null;
    this.start = -1;
    this.lastHeardFrom = null;
    this.type = Type.UpdateReviewStatus;
  }

  /**
   * Constructor to build an update recording status event {@link SchedulerItem}.
   *
   * @param state
   *          the recording status
   * @param lastHeardFrom
   *          the last heard from time
   */
  public SchedulerItem(String state, Long lastHeardFrom) {
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = -1;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = -1;
    this.recordingState = state;
    this.start = -1;
    this.lastHeardFrom = lastHeardFrom;
    this.type = Type.UpdateRecordingStatus;
  }

  public SchedulerItem(Date start, Date end, Type type) {
    this.event = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = end == null ? -1 : end.getTime();
    this.lastHeardFrom = null;
    this.optOut = null;
    this.presenters = null;
    this.properties = null;
    this.recordingState = null;
    this.reviewDate = -1;
    this.reviewStatus = null;
    this.start = start == null ? -1 : start.getTime();
    this.type = type;
  }

  public SchedulerItem(String agentId) {
    this.event = null;
    this.acl = null;
    this.agentId = agentId;
    this.blacklisted = null;
    this.end = -1;
    this.lastHeardFrom = null;
    this.optOut = null;
    this.presenters = null;
    this.properties = null;
    this.recordingState = null;
    this.reviewDate = -1;
    this.reviewStatus = null;
    this.start = -1;
    this.type = Type.UpdateAgentId;
  }

  public SchedulerItem(Set<String> presenters) {
    this.event = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = -1;
    this.lastHeardFrom = null;
    this.optOut = null;
    this.presenters = gson.toJson(presenters);
    this.properties = null;
    this.recordingState = null;
    this.reviewDate = -1;
    this.reviewStatus = null;
    this.start = -1;
    this.type = Type.UpdatePresenters;
  }
  public DublinCoreCatalog getEvent() {
    if (StringUtils.isBlank(event))
      return null;

    return DublinCoreXmlFormat.readOpt(event).orNull();
  }

  public Map<String, String> getProperties() {
    try {
      return parseProperties(properties);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
  }

  public AccessControlList getAcl() {
    try {
      return acl == null ? null : AccessControlParser.parseAcl(acl);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String getAgentId() {
    return agentId;
  }

  public Boolean getBlacklisted() {
    return gson.fromJson(blacklisted, Boolean.class);
  }

  public Date getEnd() {
    return end < 0 ? null : new Date(end);
  }

  public Long getLastHeardFrom() {
    return lastHeardFrom;
  }

  public Boolean getOptOut() {
    return gson.fromJson(optOut, Boolean.class);
  }

  @SuppressWarnings("unchecked")
  public Set<String> getPresenters() {
    return gson.fromJson(presenters, Set.class);
  }

  public ReviewStatus getReviewStatus() {
    return ReviewStatus.valueOf(reviewStatus);
  }

  public Date getReviewDate() {
    return reviewDate < 0 ? null : new Date(reviewDate);
  }

  public String getRecordingState() {
    return recordingState;
  }

  public Date getStart() {
    return start < 0 ? null : new Date(start);
  }

  public Type getType() {
    return type;
  }

  /**
   * Serializes Properties to String.
   *
   * @param caProperties
   *          properties to be serialized
   * @return serialized properties
   */
  private String serializeProperties(Map<String, String> caProperties) {
    StringBuilder wfPropertiesString = new StringBuilder();
    for (Map.Entry<String, String> entry : caProperties.entrySet())
      wfPropertiesString.append(entry.getKey() + "=" + entry.getValue() + "\n");
    return wfPropertiesString.toString();
  }

  /**
   * Parses Properties represented as String.
   *
   * @param serializedProperties
   *          properties to be parsed.
   * @return parsed properties
   * @throws IOException
   *           if parsing fails
   */
  private Map<String, String> parseProperties(String serializedProperties) throws IOException {
    Properties caProperties = new Properties();
    caProperties.load(new StringReader(serializedProperties));
    return new HashMap<String, String>((Map) caProperties);
  }

}
