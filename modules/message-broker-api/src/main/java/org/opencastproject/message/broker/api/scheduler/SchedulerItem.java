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

import org.opencastproject.message.broker.api.MessageItem;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreXmlFormat;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;

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
public class SchedulerItem implements MessageItem, Serializable {
  private static final long serialVersionUID = 6061069989788904237L;

  public static final String SCHEDULER_QUEUE_PREFIX = "SCHEDULER.";

  public static final String SCHEDULER_QUEUE = SCHEDULER_QUEUE_PREFIX + "QUEUE";

  private final String mediaPackageId;
  private final String event;
  private final String properties;
  private final String acl;
  private final String agentId;
  private final Date end;
  private final Boolean optOut;
  private final Set<String> presenters;
  private final Boolean blacklisted;
  private final String reviewStatus;
  private final Date reviewDate;
  private final String recordingState;
  private final Date start;
  private final Long lastHeardFrom;
  private final Type type;

  public enum Type {
    UpdateCatalog, UpdateProperties, UpdateAcl, UpdateAgentId, UpdateOptOut, UpdateBlacklist, UpdateEnd, UpdatePresenters, UpdateReviewStatus, UpdateRecordingStatus, UpdateStart, DeleteRecordingStatus, Delete
  };

  /**
   * @param mediaPackageId
   *          The unique id for the event to update.
   * @param event
   *          The event details to update to.
   * @return Builds {@link SchedulerItem} for updating a scheduled event.
   */
  public static SchedulerItem updateCatalog(String mediaPackageId, DublinCoreCatalog event) {
    return new SchedulerItem(mediaPackageId, event);
  }

  /**
   * @param mediaPackageId
   *          The unique id for the event to update.
   * @param properties
   *          The new properties to update to.
   * @return Builds {@link SchedulerItem} for updating the properties of an event.
   */
  public static SchedulerItem updateProperties(String mediaPackageId, Map<String, String> properties) {
    return new SchedulerItem(mediaPackageId, properties);
  }

  /**
   * @param mediaPackageId
   *          The unique id of the event to delete.
   * @return Builds {@link SchedulerItem} for deleting an event.
   */
  public static SchedulerItem delete(final String mediaPackageId) {
    return new SchedulerItem(mediaPackageId, Type.Delete);
  }

  /**
   * @param mediaPackageId
   *          the mediapackage id
   * @param accessControlList
   *          the access control list
   * @return Builds {@link SchedulerItem} for updating the access control list of an event.
   */
  public static Serializable updateAcl(String mediaPackageId, AccessControlList accessControlList) {
    return new SchedulerItem(mediaPackageId, accessControlList);
  }

  /**
   * @param mediaPackageId
   *          the mediapackage id
   * @param optOut
   *          the opt out status
   * @return Builds {@link SchedulerItem} for updating the opt out status of an event.
   */
  public static SchedulerItem updateOptOut(String mediaPackageId, boolean optOut) {
    return new SchedulerItem(mediaPackageId, optOut);
  }

  /**
   * @param mediapackageId
   *          the mediapackage id
   * @param blacklisted
   *          the blacklist status
   * @return Builds {@link SchedulerItem} for updating the blacklist status of an event.
   */
  public static SchedulerItem updateBlacklist(String mediapackageId, boolean blacklisted) {
    return new SchedulerItem(blacklisted, mediapackageId);
  }

  /**
   * @param mediaPackageId
   *          the mediapackage id
   * @param reviewStatus
   *          the review status
   * @param reviewDate
   *          the review date
   * @return Builds {@link SchedulerItem} for updating the review status of an event.
   */
  public static SchedulerItem updateReviewStatus(String mediaPackageId, ReviewStatus reviewStatus, Date reviewDate) {
    return new SchedulerItem(mediaPackageId, reviewStatus, reviewDate);
  }

  /**
   * @param mediaPackageId
   *          The event id
   * @param state
   *          The recording state
   * @param lastHeardFrom
   *          The recording last heard from date
   * @return Builds {@link SchedulerItem} for updating a recording.
   */
  public static SchedulerItem updateRecordingStatus(String mediaPackageId, String state, Long lastHeardFrom) {
    return new SchedulerItem(mediaPackageId, state, lastHeardFrom);
  }

  /**
   * @param mpId
   *        The mediapackage id
   * @param start
   *        The new start time for the event.
   * @return Builds {@link SchedulerItem} for updating the start of an event.
   */
  public static SchedulerItem updateStart(String mpId, Date start) {
    return new SchedulerItem(mpId, start, null, Type.UpdateStart);
  }

  /**
   * @param mpId
   *        The mediapackage id
   * @param end
   *        The new end time for the event.
   * @return Builds {@link SchedulerItem} for updating the end of an event.
   */
  public static SchedulerItem updateEnd(String mpId, Date end) {
    return new SchedulerItem(mpId, null, end, Type.UpdateEnd);
  }

  /**
   * @param mpId
   *        The mediapackage id
   * @param presenters
   *        The new set of presenters for the event.
   * @return Builds {@link SchedulerItem} for updating the presenters of an event.
   */
  public static SchedulerItem updatePresenters(String mpId, Set<String> presenters) {
    return new SchedulerItem(mpId, presenters);
  }

  /**
   * @param mpId
   *        The mediapackage id
   * @param agentId
   *        The new agent id for the event.
   * @return Builds {@link SchedulerItem} for updating the agent id of an event.
   */
  public static SchedulerItem updateAgent(String mpId, String agentId) {
    return new SchedulerItem(mpId, agentId);
  }

  /**
   * @param mediaPackageId
   *          The unique id of the recording to delete.
   * @return Builds {@link SchedulerItem} for deleting a recording.
   */
  public static SchedulerItem deleteRecordingState(String mediaPackageId) {
    return new SchedulerItem(mediaPackageId, Type.DeleteRecordingStatus);
  }

  /**
   * Constructor to build an update event {@link SchedulerItem}.
   *
   * @param event
   *          The event details to update.
   */
  public SchedulerItem(String mediaPackageId, DublinCoreCatalog event) {
    this.mediaPackageId = mediaPackageId;
    try {
      this.event = event.toXmlString();
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    this.properties = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = null;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.recordingState = null;
    this.start = null;
    this.lastHeardFrom = null;
    this.type = Type.UpdateCatalog;
  }

  /**
   * Constructor to build an update properties for an event {@link SchedulerItem}.
   *
   * @param mediaPackageId
   *          The id of the event to update.
   * @param properties
   *          The properties to update.
   */
  public SchedulerItem(String mediaPackageId, Map<String, String> properties) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = serializeProperties(properties);
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = null;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.recordingState = null;
    this.start = null;
    this.lastHeardFrom = null;
    this.type = Type.UpdateProperties;
  }

  /**
   * Constructor to build a delete event {@link SchedulerItem}.
   *
   * @param mediaPackageId
   *          The id of the event to delete.
   */
  public SchedulerItem(String mediaPackageId, Type type) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = null;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.recordingState = null;
    this.start = null;
    this.lastHeardFrom = null;
    this.type = type;
  }

  /**
   * Constructor to build an update access control list event {@link SchedulerItem}.
   *
   * @param mediaPackageId
   *          The mediapackage id
   * @param accessControlList
   *          The access control list
   */
  public SchedulerItem(String mediaPackageId, AccessControlList accessControlList) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = null;
    try {
      this.acl = AccessControlParser.toJson(accessControlList);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
    this.agentId = null;
    this.blacklisted = null;
    this.end = null;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.recordingState = null;
    this.start = null;
    this.lastHeardFrom = null;
    this.type = Type.UpdateAcl;
  }

  /**
   * Constructor to build an update opt out status event {@link SchedulerItem}.
   *
   * @param mediaPackageId
   *          The mediapackage id
   * @param optOut
   *          The opt out status
   */
  public SchedulerItem(String mediaPackageId, boolean optOut) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = null;
    this.optOut = optOut;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.recordingState = null;
    this.start = null;
    this.lastHeardFrom = null;
    this.type = Type.UpdateOptOut;
  }

  /**
   * Constructor to build an update blacklist status event {@link SchedulerItem}.
   *
   * @param blacklisted
   *          The blacklist status
   * @param mediaPackageId
   *          The mediapackage id
   */
  public SchedulerItem(boolean blacklisted, String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = blacklisted;
    this.end = null;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.recordingState = null;
    this.start = null;
    this.lastHeardFrom = null;
    this.type = Type.UpdateBlacklist;
  }

  /**
   * Constructor to build an update review status event {@link SchedulerItem}.
   *
   * @param mediaPackageId
   *          The mediapackage id
   * @param reviewStatus
   *          The review status
   * @param reviewDate
   *          The review date
   */
  public SchedulerItem(String mediaPackageId, ReviewStatus reviewStatus, Date reviewDate) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = null;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = reviewStatus.toString();
    this.reviewDate = reviewDate;
    this.recordingState = null;
    this.start = null;
    this.lastHeardFrom = null;
    this.type = Type.UpdateReviewStatus;
  }

  /**
   * Constructor to build an update recording status event {@link SchedulerItem}.
   *
   * @param mediaPackageId
   *          The mediapackage id
   * @param state
   *          the recording status
   * @param lastHeardFrom
   *          the last heard from time
   */
  public SchedulerItem(String mediaPackageId, String state, Long lastHeardFrom) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.properties = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = null;
    this.optOut = null;
    this.presenters = null;
    this.reviewStatus = null;
    this.reviewDate = null;
    this.recordingState = state;
    this.start = null;
    this.lastHeardFrom = lastHeardFrom;
    this.type = Type.UpdateRecordingStatus;
  }

  public SchedulerItem(String mediaPackageId, Date start, Date end, Type type) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = end;
    this.lastHeardFrom = null;
    this.optOut = null;
    this.presenters = null;
    this.properties = null;
    this.recordingState = null;
    this.reviewDate = null;
    this.reviewStatus = null;
    this.start = start;
    this.type = type;
  }

  public SchedulerItem(String mediaPackageId, String agentId) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.acl = null;
    this.agentId = agentId;
    this.blacklisted = null;
    this.end = null;
    this.lastHeardFrom = null;
    this.optOut = null;
    this.presenters = null;
    this.properties = null;
    this.recordingState = null;
    this.reviewDate = null;
    this.reviewStatus = null;
    this.start = null;
    this.type = Type.UpdateAgentId;
  }

  public SchedulerItem(String mediaPackageId, Set<String> presenters) {
    this.mediaPackageId = mediaPackageId;
    this.event = null;
    this.acl = null;
    this.agentId = null;
    this.blacklisted = null;
    this.end = null;
    this.lastHeardFrom = null;
    this.optOut = null;
    this.presenters = presenters;
    this.properties = null;
    this.recordingState = null;
    this.reviewDate = null;
    this.reviewStatus = null;
    this.start = null;
    this.type = Type.UpdatePresenters;
  }

  @Override
  public String getId() {
    return mediaPackageId;
  }

  public String getMediaPackageId() {
    return mediaPackageId;
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
    return blacklisted;
  }

  public Date getEnd() {
    return end;
  }

  public Long getLastHeardFrom() {
    return lastHeardFrom;
  }

  public Boolean getOptOut() {
    return optOut;
  }

  public Set<String> getPresenters() {
    return presenters;
  }

  public ReviewStatus getReviewStatus() {
    return ReviewStatus.valueOf(reviewStatus);
  }

  public Date getReviewDate() {
    return reviewDate;
  }

  public String getRecordingState() {
    return recordingState;
  }

  public Date getStart() {
    return start;
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
