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
package org.opencastproject.scheduler.impl.persistence;

import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for storing events in persistence storage. Event ID is stored as primary key, DUBLIN_CORE field is used
 * to store serialized Dublin core and 'CA_METADATA' is used to store capture agent specific metadata.
 *
 */
@Entity(name = "EventEntity")
@NamedQueries({
        // Job queries
        @NamedQuery(name = "Event.findAll", query = "SELECT e FROM EventEntity e "),
        @NamedQuery(name = "Event.findByMpId", query = "SELECT e FROM EventEntity e WHERE e.mediaPackageId = :mpId"),
        @NamedQuery(name = "Event.countAll", query = "SELECT COUNT(e) FROM EventEntity e "),
        @NamedQuery(name = "Event.countBlacklisted", query = "SELECT COUNT(e) FROM EventEntity e WHERE e.blacklisted = TRUE"),
        @NamedQuery(name = "Event.countRespones", query = "SELECT COUNT(e) FROM EventEntity e WHERE e.reviewDate IS NOT NULL"),
        @NamedQuery(name = "Event.countConfirmed", query = "SELECT COUNT(e) FROM EventEntity e WHERE e.reviewStatus = org.opencastproject.scheduler.api.SchedulerService$ReviewStatus.CONFIRMED"),
        @NamedQuery(name = "Event.countConfirmedByDateRange", query = "SELECT COUNT(e) FROM EventEntity e WHERE e.reviewDate >= :start AND e.reviewDate < :end AND e.reviewStatus = org.opencastproject.scheduler.api.SchedulerService$ReviewStatus.CONFIRMED"),
        @NamedQuery(name = "Event.countUnconfirmed", query = "SELECT COUNT(e) FROM EventEntity e WHERE e.reviewStatus = org.opencastproject.scheduler.api.SchedulerService$ReviewStatus.UNCONFIRMED"),
        @NamedQuery(name = "Event.countOptedOut", query = "SELECT COUNT(e) FROM EventEntity e WHERE e.optOut = TRUE") })
@Table(name = "mh_scheduled_event", uniqueConstraints = { @UniqueConstraint(columnNames = { "mediapackage_id" }) })
public class EventEntity {

  /** Event ID, primary key */
  @Id
  @Column(name = "id")
  protected Long eventId;

  /** Mediapackage ID */
  @Column(name = "mediapackage_id")
  protected String mediaPackageId;

  /** Serialized Dublin core */
  @Lob
  @Column(name = "dublin_core", length = 65535)
  protected String dublinCoreXML;

  /** Serialized Capture agent metadata */
  @Lob
  @Column(name = "capture_agent_metadata", length = 65535)
  protected String captureAgentMetadata;

  /** Serialized access control */
  @Lob
  @Column(name = "access_control", length = 65535)
  protected String accessControl;

  /** Opt-out status */
  @Column(name = "opt_out")
  protected boolean optOut = false;

  /** Review status */
  @Column(name = "review_status")
  @Enumerated(EnumType.STRING)
  protected ReviewStatus reviewStatus = ReviewStatus.UNSENT;

  @Column(name = "review_date")
  @Temporal(TemporalType.TIMESTAMP)
  protected Date reviewDate;

  /** Opt-out status */
  @Column(name = "blacklisted")
  protected boolean blacklisted = false;

  /**
   * Default constructor without any import.
   */
  public EventEntity() {
  }

  /**
   * Returns event ID.
   *
   * @return event ID
   */
  public Long getEventId() {
    return eventId;
  }

  /**
   * Sets event ID.
   *
   * @param eventId
   */
  public void setEventId(Long eventId) {
    this.eventId = eventId;
  }

  /**
   * Returns the mediapackage ID.
   *
   * @return the mediapackage ID
   */
  public String getMediaPackageId() {
    return mediaPackageId;
  }

  /**
   * Sets the mediapackage ID.
   *
   * @param mediaPackageId
   *          the mediapackage ID
   */
  public void setMediaPackageId(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
  }

  /**
   * Returns serialized Dublin core.
   *
   * @return serialized Dublin core
   */
  public String getEventDublinCore() {
    return dublinCoreXML;
  }

  /**
   * Sets serialized Dublin core.
   *
   * @param dublinCoreXML
   *          serialized Dublin core
   */
  public void setEventDublinCore(String dublinCoreXML) {
    this.dublinCoreXML = dublinCoreXML;
  }

  /**
   * Returns serialized capture agent metadata
   *
   * @return serialized metadata
   */
  public String getCaptureAgentMetadata() {
    return captureAgentMetadata;
  }

  /**
   * Sets serialized capture agent metadata
   *
   * @param captureAgentMetadata
   *          serialized metadata
   */
  public void setCaptureAgentMetadata(String captureAgentMetadata) {
    this.captureAgentMetadata = captureAgentMetadata;
  }

  /**
   * Returns serialized access control
   *
   * @return serialized access control
   */
  public String getAccessControl() {
    return accessControl;
  }

  /**
   * Sets serialized access control.
   *
   * @param accessControl
   *          serialized access control
   */
  public void setAccessControl(String accessControl) {
    this.accessControl = accessControl;
  }

  /**
   * Returns the opted out status
   *
   * @return the opted out status
   */
  public boolean isOptOut() {
    return optOut;
  }

  /**
   * Sets the opted out status
   *
   * @param optOut
   *          the opted out status
   */
  public void setOptOut(boolean optOut) {
    this.optOut = optOut;
  }

  /**
   * Returns the review status
   *
   * @return the review status
   */
  public ReviewStatus getReviewStatus() {
    return reviewStatus;
  }

  /**
   * Sets the review status
   *
   * @param reviewStatus
   *          the review status
   */
  public void setReviewStatus(ReviewStatus reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  /**
   * Returns the review date
   *
   * @return the review date
   */
  public Date getReviewDate() {
    return reviewDate;
  }

  /**
   * Sets the review date
   *
   * @param reviewDate
   *          the review date
   */
  public void setReviewDate(Date reviewDate) {
    this.reviewDate = reviewDate;
  }

  /**
   * Returns the blacklist status
   *
   * @return the blacklist status
   */
  public boolean isBlacklisted() {
    return blacklisted;
  }

  /**
   * Sets the blacklist status
   *
   * @param blacklisted
   *          the blacklist status
   */
  public void setBlacklistStatus(boolean blacklisted) {
    this.blacklisted = blacklisted;
  }

}
