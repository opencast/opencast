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
package org.opencastproject.scheduler.impl.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Entity object for storing extended scheduled event information in persistence storage.
 */
@IdClass(EventIdPK.class)
@Entity(name = "ExtendedEvent")
@NamedQueries({
        @NamedQuery(name = "ExtendedEvent.findAll", query = "SELECT e FROM ExtendedEvent e WHERE e.organization = :org"),
        @NamedQuery(name = "ExtendedEvent.countAll", query = "SELECT COUNT(e) FROM ExtendedEvent e"),
        @NamedQuery(name = "ExtendedEvent.findEvents", query = "SELECT e.mediaPackageId FROM ExtendedEvent e WHERE e.organization = :org AND e.captureAgentId = :ca AND e.startDate < :end AND e.endDate > :start ORDER BY e.startDate ASC"),
        @NamedQuery(name = "ExtendedEvent.searchEventsCA", query = "SELECT e FROM ExtendedEvent e WHERE e.organization = :org AND e.captureAgentId = :ca AND e.startDate >= :startFrom AND e.startDate < :startTo AND e.endDate >= :endFrom AND e.endDate < :endTo ORDER BY e.startDate ASC"),
        @NamedQuery(name = "ExtendedEvent.searchEvents", query = "SELECT e FROM ExtendedEvent e WHERE e.organization = :org AND e.startDate >= :startFrom AND e.startDate < :startTo AND e.endDate >= :endFrom AND e.endDate < :endTo ORDER BY e.startDate ASC"),
        @NamedQuery(name = "ExtendedEvent.knownRecordings", query = "SELECT e FROM ExtendedEvent e WHERE e.organization = :org AND e.recordingState IS NOT NULL AND e.recordingLastHeard IS NOT NULL")
})
@Table(name = "oc_scheduled_extended_event", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "mediapackage_id", "organization" }) })
public class ExtendedEventDto {

  /** Event ID, primary key */
  @Id
  @Column(name = "mediapackage_id", length = 128)
  private String mediaPackageId;

  /** Organization id, primary key */
  @Id
  @Column(name = "organization", length = 128)
  private String organization;

  /** Capture agent id */
  @Column(name = "capture_agent_id", length = 128)
  private String captureAgentId;

  /** recording start date */
  @Column(name = "start_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date startDate;

  /** recording end date */
  @Column(name = "end_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date endDate;

  /** source */
  @Column(name = "source")
  private String source;

  /** recording state */
  @Column(name = "recording_state")
  private String recordingState;

  /** recording last heard */
  @Column(name = "recording_last_heard")
  private Long recordingLastHeard;

  /** presenters */
  @Column(name = "presenters")
  private String presenters;

  /** last modified date */
  @Column(name = "last_modified_date")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastModifiedDate;

  /** capture agent properties */
  @Column(name = "capture_agent_properties")
  private String captureAgentProperties;

  /** workflow properties */
  @Column(name = "workflow_properties")
  private String workflowProperties;

  @Column(name = "checksum", length = 64)
  private String checksum;

  /**
   * Default constructor without any import.
   */
  public ExtendedEventDto() {
  }

  public String getMediaPackageId() {
    return mediaPackageId;
  }

  public void setMediaPackageId(String mediaPackageId) {
    this.mediaPackageId = mediaPackageId;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getCaptureAgentId() {
    return captureAgentId;
  }

  public void setCaptureAgentId(String captureAgentId) {
    this.captureAgentId = captureAgentId;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getRecordingState() {
    return recordingState;
  }

  public void setRecordingState(String recordingState) {
    this.recordingState = recordingState;
  }

  public Long getRecordingLastHeard() {
    return recordingLastHeard;
  }

  public void setRecordingLastHeard(Long recordingLastHeard) {
    this.recordingLastHeard = recordingLastHeard;
  }

  public String getPresenters() {
    return presenters;
  }

  public void setPresenters(String presenters) {
    this.presenters = presenters;
  }

  public Date getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(Date lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public String getCaptureAgentProperties() {
    return captureAgentProperties;
  }

  public void setCaptureAgentProperties(String captureAgentProperties) {
    this.captureAgentProperties = captureAgentProperties;
  }

  public String getWorkflowProperties() {
    return workflowProperties;
  }

  public void setWorkflowProperties(String workflowProperties) {
    this.workflowProperties = workflowProperties;
  }
}
