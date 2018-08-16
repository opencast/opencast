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

package org.opencastproject.index.service.impl.index.event;

import org.opencastproject.index.service.impl.index.IndexObject;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.scheduler.api.RecordingState;
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.util.IoSupport;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

/**
 * Object wrapper for a recording event.
 */
@XmlType(name = "event", namespace = IndexObject.INDEX_XML_NAMESPACE, propOrder = { "identifier", "organization",
        "title", "description", "subject", "location", "presenters", "contributors", "seriesId", "seriesName",
        "language", "source", "created", "creator", "license", "rights", "accessPolicy", "managedAcl", "workflowState",
        "workflowId", "workflowDefinitionId", "recordingStartTime", "recordingEndTime", "duration", "trackMimetypes",
        "trackStreamResolutions", "trackFlavors", "metadataFlavors", "metadataMimetypes", "attachmentFlavors",
        "reviewStatus", "reviewDate", "optedOut", "blacklisted", "hasComments", "hasOpenComments", "hasPreview", "needsCutting",
        "publications", "workflowScheduledDate", "archiveVersion", "schedulingStatus", "recordingStatus", "eventStatus",
        "agentId", "agentConfigurations", "technicalStartTime", "technicalEndTime", "technicalPresenters" })
@XmlRootElement(name = "event", namespace = IndexObject.INDEX_XML_NAMESPACE)
@XmlAccessorType(XmlAccessType.NONE)
public class Event implements IndexObject {

  /**
   * The scheduling status of the event
   */
  public enum SchedulingStatus {
    BLACKLISTED, OPTED_OUT, READY_FOR_RECORDING
  };

  /** The document type */
  public static final String DOCUMENT_TYPE = "event";

  /** The name of the surrounding XML tag to wrap a result of multiple events */
  public static final String XML_SURROUNDING_TAG = "events";

  /** The mapping of recording and workflow states */
  private static final Map<String, String> workflowStatusMapping = new HashMap<>();
  private static final Map<String, String> recordingStatusMapping = new HashMap<>();

  static {
    recordingStatusMapping.put(RecordingState.CAPTURING, "EVENTS.EVENTS.STATUS.RECORDING");
    recordingStatusMapping.put(RecordingState.CAPTURE_FINISHED, "EVENTS.EVENTS.STATUS.RECORDING");
    recordingStatusMapping.put(RecordingState.MANIFEST, "EVENTS.EVENTS.STATUS.INGESTING");
    recordingStatusMapping.put(RecordingState.MANIFEST_FINISHED, "EVENTS.EVENTS.STATUS.INGESTING");
    recordingStatusMapping.put(RecordingState.COMPRESSING, "EVENTS.EVENTS.STATUS.INGESTING");
    recordingStatusMapping.put(RecordingState.UPLOADING, "EVENTS.EVENTS.STATUS.INGESTING");
    recordingStatusMapping.put(RecordingState.UPLOAD_FINISHED, "EVENTS.EVENTS.STATUS.INGESTING");
    recordingStatusMapping.put(RecordingState.CAPTURE_ERROR, "EVENTS.EVENTS.STATUS.RECORDING_FAILURE");
    recordingStatusMapping.put(RecordingState.MANIFEST_ERROR, "EVENTS.EVENTS.STATUS.RECORDING_FAILURE");
    recordingStatusMapping.put(RecordingState.COMPRESSING_ERROR, "EVENTS.EVENTS.STATUS.RECORDING_FAILURE");
    recordingStatusMapping.put(RecordingState.UPLOAD_ERROR, "EVENTS.EVENTS.STATUS.RECORDING_FAILURE");
    workflowStatusMapping.put(WorkflowState.INSTANTIATED.toString(), "EVENTS.EVENTS.STATUS.PENDING");
    workflowStatusMapping.put(WorkflowState.RUNNING.toString(), "EVENTS.EVENTS.STATUS.PROCESSING");
    workflowStatusMapping.put(WorkflowState.FAILING.toString(), "EVENTS.EVENTS.STATUS.PROCESSING");
    workflowStatusMapping.put(WorkflowState.PAUSED.toString(), "EVENTS.EVENTS.STATUS.PAUSED");
    workflowStatusMapping.put(WorkflowState.SUCCEEDED.toString(), "EVENTS.EVENTS.STATUS.PROCESSED");
    workflowStatusMapping.put(WorkflowState.FAILED.toString(), "EVENTS.EVENTS.STATUS.PROCESSING_FAILURE");
    workflowStatusMapping.put(WorkflowState.STOPPED.toString(), "EVENTS.EVENTS.STATUS.PROCESSING_CANCELED");
  }

  /** The identifier */
  @XmlElement(name = "identifier")
  private String identifier = null;

  /** The organization identifier */
  @XmlElement(name = "organization")
  private String organization = null;

  /** The title */
  @XmlElement(name = "title")
  private String title = null;

  /** The description */
  @XmlElement(name = "description")
  private String description = null;

  /** The subject */
  @XmlElement(name = "subject")
  private String subject = null;

  /** The location */
  @XmlElement(name = "location")
  private String location = null;

  @XmlElementWrapper(name = "presenters")
  @XmlElement(name = "presenter")
  private List<String> presenters = null;

  @XmlElementWrapper(name = "contributors")
  @XmlElement(name = "contributor")
  private List<String> contributors = null;

  /** The series identifier */
  @XmlElement(name = "series_id")
  private String seriesId = null;

  /** The series name */
  @XmlElement(name = "series_name")
  private String seriesName = null;

  /** The language for the event */
  @XmlElement(name = "language")
  private String language = null;

  /** The source for the event */
  @XmlElement(name = "source")
  private String source = null;

  /** The creation date of the event */
  @XmlElement(name = "created")
  private String created = null;

  /** The creator of the event */
  @XmlElement(name = "creator")
  private String creator = null;

  /** The license of the event */
  @XmlElement(name = "license")
  private String license = null;

  /** The rights of the event */
  @XmlElement(name = "rights")
  private String rights = null;

  /** The access policy of the event */
  @XmlElement(name = "access_policy")
  private String accessPolicy = null;

  /** The name of the managed ACL used by the series (if set) */
  @XmlElement(name = "managed_acl")
  private String managedAcl = null;

  /** The current state of the workflow related to this event */
  @XmlElement(name = "workflow_state")
  private String workflowState = null;

  /** The workflow id related to this event */
  @XmlElement(name = "workflow_id")
  private Long workflowId = null;

  /** The workflow definition id related to this event */
  @XmlElement(name = "workflow_definition_id")
  private String workflowDefinitionId = null;

  /** The recording start date from this event */
  @XmlElement(name = "recording_start_time")
  private String recordingStartTime = null;

  /** The recording end date from this event */
  @XmlElement(name = "recording_end_time")
  private String recordingEndTime = null;

  /** The recording duration from this event */
  @XmlElement(name = "duration")
  private Long duration = null;

  /** The mimetypes list of the tracks from this event */
  @XmlElementWrapper(name = "track_mimetypes")
  @XmlElement(name = "track_mimetype")
  private List<String> trackMimetypes = null;

  /** The resolutions list of the tracks from this event */
  @XmlElementWrapper(name = "track_stream_resolutions")
  @XmlElement(name = "track_stream_resolution")
  private List<String> trackStreamResolutions = null;

  /** The flavors list of the tracks from this event */
  @XmlElementWrapper(name = "track_flavors")
  @XmlElement(name = "track_flavor")
  private List<String> trackFlavors = null;

  /** The flavors list of the metadata from this event */
  @XmlElementWrapper(name = "metadata_flavors")
  @XmlElement(name = "metadata_flavor")
  private List<String> metadataFlavors = null;

  /** The mimetypes list of the metadata from this event */
  @XmlElementWrapper(name = "metadata_mimetypes")
  @XmlElement(name = "metadata_mimetype")
  private List<String> metadataMimetypes = null;

  /** The flavors list of the attachment from this event */
  @XmlElementWrapper(name = "attachment_flavors")
  @XmlElement(name = "attachment_flavor")
  private List<String> attachmentFlavors = null;

  /** The status of the event */
  @XmlElement(name = "event_status")
  private String eventStatus = null;

  /** The event review status */
  @XmlElement(name = "review_status")
  private String reviewStatus = ReviewStatus.UNSENT.toString();

  /** The event opted out status, whether the event is opted out or not opted out */
  @XmlElement(name = "opted_out")
  private Boolean optedOut = null;

  /** The event blacklist status, whether the event is blacklisted or not */
  @XmlElement(name = "blacklisted")
  private Boolean blacklisted = null;

  /** The event review date */
  @XmlElement(name = "review_date")
  private String reviewDate = null;

  /** Whether the event has comments */
  @XmlElement(name = "has_comments")
  private Boolean hasComments = false;

  /** Whether the event has open comments */
  @XmlElement(name = "has_open_comments")
  private Boolean hasOpenComments = false;

  /** Whether the event has preview files */
  @XmlElement(name = "has_preview")
  private Boolean hasPreview = false;

  /** Whether the event has open needs cutting comment */
  @XmlElement(name = "needs_cutting")
  private Boolean needsCutting = false;

  /** The list of publications from this event */
  @XmlElementWrapper(name = "publications")
  @XmlElement(name = "publication")
  private List<Publication> publications = new ArrayList<>();

  /** The workflow scheduled date of the event */
  @XmlElement(name = "workflow_scheduled_date")
  private String workflowScheduledDate = null;

  /** The scheduling status of the event */
  @XmlElement(name = "scheduling_status")
  private String schedulingStatus = null;

  /** The recording status of the event */
  @XmlElement(name = "recording_status")
  private String recordingStatus = null;

  /** The archive version of the event */
  @XmlElement(name = "archive_version")
  private Long archiveVersion = null;

  /** The id of the capture agent */
  @XmlElement(name = "agent_id")
  private String agentId = null;

  /** The configuration of the capture agent */
  @XmlElementWrapper(name = "agent_configuration")
  private Map<String, String> agentConfigurations = new HashMap<String, String>();

  /** The technical end time of the recording */
  @XmlElement(name = "technical_end_time")
  private String technicalEndTime = null;

  /** The technical start time of the recording */
  @XmlElement(name = "technical_start_time")
  private String technicalStartTime = null;

  @XmlElementWrapper(name = "technical_presenters")
  @XmlElement(name = "technical_presenter")
  private List<String> technicalPresenters = null;

  /** Context for serializing and deserializing */
  private static JAXBContext context = null;

  /**
   * Required default no arg constructor for JAXB.
   */
  public Event() {

  }

  /**
   * The recording identifier.
   *
   * @param identifier
   *          the object identifier
   * @param organization
   *          the organization
   */
  public Event(String identifier, String organization) {
    this.identifier = identifier;
    this.organization = organization;
    updateEventStatus();
  }

  /**
   * Returns the recording event identifier.
   *
   * @return the identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Returns the organization of the recording.
   *
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * Sets the recording title.
   *
   * @param title
   *          the title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Returns the recording title.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the recording description.
   *
   * @param description
   *          the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the recording description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the recording subject
   *
   * @param subject
   *          the subject to set
   */
  public void setSubject(String subject) {
    this.subject = subject;
  }

  /**
   * Returns the recording subject.
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Sets the recording location.
   *
   * @param location
   *          the location
   */
  public void setLocation(String location) {
    this.location = location;
  }

  /**
   * Returns the recording location.
   *
   * @return the location
   */
  public String getLocation() {
    return location;
  }

  /**
   * Sets the series identifier
   *
   * @param seriesId
   *          the series identifier
   */
  public void setSeriesId(String seriesId) {
    this.seriesId = seriesId;
  }

  /**
   * Returns the series identifier
   *
   * @return the series identifier
   */
  public String getSeriesId() {
    return seriesId;
  }

  /**
   * Sets the series name
   *
   * @param seriesName
   *          the series name
   */
  public void setSeriesName(String seriesName) {
    this.seriesName = seriesName;
  }

  /**
   * Returns the series name
   *
   * @return the series name
   */
  public String getSeriesName() {
    return seriesName;
  }

  /**
   * Sets the language
   *
   * @param language
   *          the language
   */
  public void setLanguage(String language) {
    this.language = language;
  }

  /**
   * Returns the language
   *
   * @return the language
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Sets the source of this event
   *
   * @param source
   *          the source
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * Returns the source of this event
   *
   * @return the source
   */
  public String getSource() {
    return source;
  }

  /**
   * Sets the creation date
   *
   * @param created
   *          the creation date
   */
  public void setCreated(String created) {
    this.created = created;
  }

  /**
   * Returns the creation date
   *
   * @return the creation date
   */
  public String getCreated() {
    return created;
  }

  /**
   * Sets the creator
   *
   * @param creator
   *          the language
   */
  public void setCreator(String creator) {
    this.creator = creator;
  }

  /**
   * Returns the creator
   *
   * @return the language
   */
  public String getCreator() {
    return creator;
  }

  /**
   * Sets the license
   *
   * @param license
   *          the language
   */
  public void setLicense(String license) {
    this.license = license;
  }

  /**
   * Returns the license
   *
   * @return the license
   */
  public String getLicense() {
    return license;
  }

  /**
   * Sets the rights
   *
   * @param rights
   *          the rights
   */
  public void setRights(String rights) {
    this.rights = rights;
  }

  /**
   * Returns the rights
   *
   * @return the rights
   */
  public String getRights() {
    return rights;
  }

  /**
   * Sets the access policy
   *
   * @param accessPolicy
   *          the access policy
   */
  public void setAccessPolicy(String accessPolicy) {
    this.accessPolicy = accessPolicy;
  }

  /**
   * Returns the access policy
   *
   * @return the access policy
   */
  public String getAccessPolicy() {
    return accessPolicy;
  }

  /**
   * Sets the name of the managed ACL used by the event.
   *
   * @param managedAcl
   *          the managed ACL name
   */
  public void setManagedAcl(String managedAcl) {
    this.managedAcl = managedAcl;
  }

  /**
   * Returns the name of the managed ACL, if the event does not have a custom ACL.
   *
   * @return the managed ACL name
   */
  public String getManagedAcl() {
    return managedAcl;
  }

  /**
   * Sets the current workflow state related to this event
   *
   * @param workflowState
   *          the current workflow state
   */
  public void setWorkflowState(WorkflowState workflowState) {
    this.workflowState = workflowState == null ? null : workflowState.toString();
    updateEventStatus();
  }

  /**
   * Returns the current workflow state related to this event
   *
   * @return the workflow state
   */
  public String getWorkflowState() {
    return workflowState;
  }

  /**
   * Sets the current workflow id related to this event
   *
   * @param workflowId
   *          the current workflow id
   */
  public void setWorkflowId(Long workflowId) {
    this.workflowId = workflowId;
  }

  /**
   * Returns the current workflow id related to this event
   *
   * @return the workflow id
   */
  public Long getWorkflowId() {
    return workflowId;
  }

  /**
   * Sets the current workflow definition id related to this event
   *
   * @param workflowDefinitionId
   *          the current workflow definition id
   */
  public void setWorkflowDefinitionId(String workflowDefinitionId) {
    this.workflowDefinitionId = workflowDefinitionId;
  }

  /**
   * Returns the current workflow definition id related to this event
   *
   * @return the workflow definition id
   */
  public String getWorkflowDefinitionId() {
    return workflowDefinitionId;
  }

  /**
   * Sets the start date
   *
   * @param recordingStartTime
   *          the start date
   */
  public void setRecordingStartDate(String recordingStartTime) {
    this.recordingStartTime = recordingStartTime;
  }

  /**
   * Returns the start date
   *
   * @return the start date
   */
  public String getRecordingStartDate() {
    return recordingStartTime;
  }

  /**
   * Sets the recording end date
   *
   * @param recordingEndTime
   *          the end date
   */
  public void setRecordingEndDate(String recordingEndTime) {
    this.recordingEndTime = recordingEndTime;
  }

  /**
   * Returns the recording end date
   *
   * @return the end date
   */
  public String getRecordingEndDate() {
    return recordingEndTime;
  }

  /**
   * Sets the recording duration
   *
   * @param duration
   *          the recording duration
   */
  public void setDuration(long duration) {
    this.duration = duration;
  }

  /**
   * Returns the recording duration
   *
   * @return the recording duration
   */
  public Long getDuration() {
    return duration;
  }

  /**
   * Sets the tracks mimetypes from this event
   *
   * @param trackMimetypes
   *          the tracks mimetypes
   */
  public void setTrackMimetypes(List<String> trackMimetypes) {
    this.trackMimetypes = trackMimetypes;
  }

  /**
   * Returns the tracks mimetypes from this event
   *
   * @return the tracks mimetypes
   */
  public List<String> getTrackMimetypes() {
    return trackMimetypes;
  }

  /**
   * Sets the tracks stream resolutions from this event
   *
   * @param trackStreamResolution
   *          the tracks stream resolutions
   */
  public void setTrackStreamResolutions(List<String> trackStreamResolution) {
    this.trackStreamResolutions = trackStreamResolution;
  }

  /**
   * Returns the tracks stream resolutions from this event
   *
   * @return the tracks stream resolutions
   */
  public List<String> getTrackStreamResolution() {
    return trackStreamResolutions;
  }

  /**
   * Sets the tracks flavors from this event
   *
   * @param trackFlavors
   *          the tracks flavors
   */
  public void setTrackFlavors(List<String> trackFlavors) {
    this.trackFlavors = trackFlavors;
  }

  /**
   * Returns the tracks flavors from this event
   *
   * @return the tracks flavors
   */
  public List<String> getTrackFlavors() {
    return trackFlavors;
  }

  /**
   * Sets the metadata flavors from this event
   *
   * @param metadataFlavors
   *          the tracks flavors
   */
  public void setMetadataFlavors(List<String> metadataFlavors) {
    this.metadataFlavors = metadataFlavors;
  }

  /**
   * Returns the metadata flavors from this event
   *
   * @return the metadata flavors
   */
  public List<String> getMetadataFlavors() {
    return metadataFlavors;
  }

  /**
   * Sets the metadata mimetypes from this event
   *
   * @param metadataMimetypes
   *          the metadata mimetypes
   */
  public void setMetadataMimetypes(List<String> metadataMimetypes) {
    this.metadataMimetypes = metadataMimetypes;
  }

  /**
   * Returns the metadata mimetypes from this event
   *
   * @return the metadata mimetypes
   */
  public List<String> getMetadataMimetypes() {
    return metadataMimetypes;
  }

  /**
   * Sets the attachments flavors from this event
   *
   * @param attachmentFlavors
   *          the attachments flavors
   */
  public void setAttachmentFlavors(List<String> attachmentFlavors) {
    this.attachmentFlavors = attachmentFlavors;
  }

  /**
   * Returns the attachments flavors from this event
   *
   * @return the attachments flavors
   */
  public List<String> getAttachmentFlavors() {
    return attachmentFlavors;
  }

  /**
   * Sets the review status from this event
   *
   * @param reviewStatus
   *          the review status
   */
  public void setReviewStatus(ReviewStatus reviewStatus) {
    this.reviewStatus = reviewStatus == null ? null : reviewStatus.toString();
  }

  /**
   * Returns the review status from this event
   *
   * @return the review status
   */
  public String getReviewStatus() {
    return reviewStatus;
  }

  /**
   * Sets the review date
   *
   * @param reviewDate
   *          the review date
   */
  public void setReviewDate(String reviewDate) {
    this.reviewDate = reviewDate;
  }

  /**
   * Returns the review date
   *
   * @return the review date
   */
  public String getReviewDate() {
    return reviewDate;
  }

  /**
   * Sets the opt out status for this event
   *
   * @param optedOut
   *          the opt out status, whether the event is opted-out or not opted-out
   */
  public void setOptedOut(Boolean optedOut) {
    this.optedOut = optedOut;

    updateSchedulingStatus();
    updateEventStatus();
  }

  /**
   * Returns the opt out status from this event
   *
   * @return the opt out status from this event, whether the event is opted-out or not opted-out
   */
  public Boolean getOptedOut() {
    return optedOut;
  }

  /**
   * Sets the blacklist status for this event
   *
   * @param blacklisted
   *          the blacklist status, whether the event is blacklisted or not
   */
  public void setBlacklisted(Boolean blacklisted) {
    this.blacklisted = blacklisted;

    updateSchedulingStatus();
    updateEventStatus();
  }

  /**
   * Returns the blacklist status from this event
   *
   * @return the blacklist status from this event, whether the event is blacklist or not
   */
  public Boolean getBlacklisted() {
    return blacklisted;
  }

  /**
   * Sets the list of presenters.
   *
   * @param presenters
   *          the presenters for this event
   */
  public void setPresenters(List<String> presenters) {
    this.presenters = presenters;
  }

  /**
   * Returns the recording presenters.
   *
   * @return the presenters
   */
  public List<String> getPresenters() {
    return presenters;
  }

  /**
   * Sets the list of contributors.
   *
   * @param contributors
   *          the contributors for this event
   */
  public void setContributors(List<String> contributors) {
    this.contributors = contributors;
  }

  /**
   * Returns the recording contributors.
   *
   * @return the contributors
   */
  public List<String> getContributors() {
    return contributors;
  }

  /**
   * Sets the has comments status from this event
   *
   * @param hasComments
   *          the has comments status from this event
   */
  public void setHasComments(boolean hasComments) {
    this.hasComments = hasComments;
  }

  /**
   * Returns the has comments status from this event
   *
   * @return the has comments status from this event
   */
  public boolean hasComments() {
    return hasComments;
  }

  /**
   * Sets the has open comments status from this event
   *
   * @param hasOpenComments
   *          the has open comments status from this event
   */
  public void setHasOpenComments(boolean hasOpenComments) {
    this.hasOpenComments = hasOpenComments;
  }

  /**
   * Returns the has comments status from this event
   *
   * @return the has comments status from this event
   */
  public boolean hasOpenComments() {
    return hasOpenComments;
  }

  /**
   * Sets the has preview status from this event
   *
   * @param hasPreview
   *          the has preview status from this event
   */
  public void setHasPreview(boolean hasPreview) {
    this.hasPreview = hasPreview;
  }

  /**
   * Returns the has preview status from this event
   *
   * @return the has preview status from this event
   */
  public boolean hasPreview() {
    return hasPreview;
  }

  /**
   * Sets the subtype of the publication element that indicates a preview element.
   *
   * @param previewSubtype
   *          the subtype
   */
  public void updatePreview(String previewSubtype) {
    hasPreview = EventIndexUtils.subflavorMatches(publications, previewSubtype);
  }

  /**
   * Sets the has open needs cutting comment for this event
   *
   * @param needsCutting
   *          this event has the open comments status that it needs cutting
   */
  public void setNeedsCutting(boolean needsCutting) {
    this.needsCutting = needsCutting;
  }

  /**
   * Returns the has comment needs cutting for this event
   *
   * @return the event has the comments status that it needs cutting
   */
  public boolean needsCutting() {
    return needsCutting;
  }

  /**
   * Sets the list of publications.
   *
   * @param publications
   *          the publications for this event
   */
  public void setPublications(List<Publication> publications) {
    this.publications = publications;
  }

  /**
   * Returns the event publications.
   *
   * @return the publications
   */
  public List<Publication> getPublications() {
    return publications;
  }

  /**
   * Sets the workflow scheduled date
   *
   * @param workflowScheduledDate
   *          the workflow scheduled date
   */
  public void setWorkflowScheduledDate(String workflowScheduledDate) {
    this.workflowScheduledDate = workflowScheduledDate;
  }

  /**
   * Returns the workflow scheduled date
   *
   * @return the workflow scheduled date
   */
  public String getWorkflowScheduledDate() {
    return workflowScheduledDate;
  }

  /**
   * Sets the archive version
   *
   * @param archiveVersion
   *          the archive version
   */
  public void setArchiveVersion(Long archiveVersion) {
    this.archiveVersion = archiveVersion;
  }

  /**
   * Returns the archive version
   *
   * @return the archive version
   */
  public Long getArchiveVersion() {
    return archiveVersion;
  }

  /**
   * Sets the scheduling status
   *
   * @param status
   *          the scheduling status
   */
  public void setSchedulingStatus(String status) {
    this.schedulingStatus = status;
  }

  /**
   * Update the scheduling status based on the optedOut and blacklisted properties
   */
  public void updateSchedulingStatus() {
    if (blacklisted == null && optedOut == null) {
      schedulingStatus = null;
    } else if (blacklisted != null && blacklisted) {
      schedulingStatus = SchedulingStatus.BLACKLISTED.toString();
    } else if (optedOut != null && optedOut) {
      schedulingStatus = SchedulingStatus.OPTED_OUT.toString();
    } else {
      schedulingStatus = SchedulingStatus.READY_FOR_RECORDING.toString();
    }
  }

  private void updateEventStatus() {
    if (getWorkflowId() != null) {
      eventStatus = workflowStatusMapping.get(getWorkflowState());
      return;
    }

    if (getRecordingStatus() != null) {
      eventStatus = recordingStatusMapping.get(getRecordingStatus());
      return;
    }

    if (BooleanUtils.isTrue(getBlacklisted())) {
      eventStatus = "EVENTS.EVENTS.STATUS.BLACKLISTED";
      return;
    }

    if (BooleanUtils.isTrue(getOptedOut())) {
      eventStatus = "EVENTS.EVENTS.STATUS.OPTEDOUT";
      return;
    }

    if (StringUtils.isNotBlank(getSchedulingStatus())) {
      eventStatus = "EVENTS.EVENTS.STATUS.SCHEDULED";
      return;
    }

    eventStatus = "EVENTS.EVENTS.STATUS.PROCESSED";
  }

  /**
   * Check whether or not the start time of the recording of this event has passed, yet.
   *
   * This method treats non-scheduled (i.e.) uploaded events as always having
   * a recording start time in the pase, because for the video to be able
   * to be uploaded, it obviously must have been recorded, already.
   *
   * @return <code>true</code> if the start ime of the recording of this event
   *         is in the past, and <code>false</code> otherwise
   */
  public boolean hasRecordingStarted() {
    return
            // handle uploaded events
            getSchedulingStatus() == null
            // handle scheduled events
            || getRecordingStatus() != null;
  }

  /**
   * Returns the scheduling status
   *
   * @return the scheduling status
   */
  public String getSchedulingStatus() {
    return schedulingStatus;
  }

  /**
   * Sets the recording status
   *
   * @param recordingStatus
   *          the recording status
   */
  public void setRecordingStatus(String recordingStatus) {
    this.recordingStatus = recordingStatus;
    updateEventStatus();
  }

  /**
   * Returns the recording status
   *
   * @return the recording status
   */
  public String getRecordingStatus() {
    return recordingStatus;
  }

  /**
   * Returns the event status
   *
   * @return the event status
   */
  public String getEventStatus() {
    updateEventStatus();
    return eventStatus;
  }

  /**
   * Returns the agent id
   *
   * @return the agent id
   */
  public String getAgentId() {
    return agentId;
  }

  /**
   * Sets the agent id
   *
   * @param agentId
   *          the agent id
   */
  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  /**
   * Returns the agent configuration
   *
   * @return the agent configuration
   */
  public Map<String, String> getAgentConfiguration() {
    return agentConfigurations;
  }

  /**
   * Sets the agent configuration
   *
   * @param agentConfigurations
   *          the agent configuration
   */
  public void setAgentConfiguration(Map<String, String> agentConfigurations) {
    this.agentConfigurations = agentConfigurations;
  }

  /**
   * Returns the technical end time
   *
   * @return the technical end time
   */
  public String getTechnicalEndTime() {
    return technicalEndTime;
  }

  /**
   * Sets the technical end time
   *
   * @param technicalEndTime
   *          the technical end time
   */
  public void setTechnicalEndTime(String technicalEndTime) {
    this.technicalEndTime = technicalEndTime;
  }

  /**
   * Returns the technical start time
   *
   * @return the technical start time
   */
  public String getTechnicalStartTime() {
    return technicalStartTime;
  }

  /**
   * Sets the technical start time
   *
   * @param technicalStartTime
   *          the technical start time
   */
  public void setTechnicalStartTime(String technicalStartTime) {
    this.technicalStartTime = technicalStartTime;
  }

  /**
   * Returns the technical presenters
   *
   * @return the technical presenters
   */
  public List<String> getTechnicalPresenters() {
    return technicalPresenters;
  }

  /**
   * Sets the technical presenters
   *
   * @param technicalPresenters
   *          the technical presenters
   */
  public void setTechnicalPresenters(List<String> technicalPresenters) {
    this.technicalPresenters = technicalPresenters;
  }

  /**
   * Reads the recording event from the input stream.
   *
   * @param xml
   *          the input stream
   * @return the deserialized recording event
   * @throws IOException
   */
  public static Event valueOf(InputStream xml) throws IOException {
    try {
      if (context == null) {
        createJAXBContext();
      }
      Unmarshaller unmarshaller = context.createUnmarshaller();
      return unmarshaller.unmarshal(new StreamSource(xml), Event.class).getValue();
    } catch (JAXBException e) {
      throw new IOException(e.getLinkedException() != null ? e.getLinkedException() : e);
    } finally {
      IoSupport.closeQuietly(xml);
    }
  }

  /**
   * Reads the recording event from the input stream.
   *
   * @param json
   *          the input stream
   * @return the deserialized recording event
   * @throws JSONException
   * @throws XMLStreamException
   * @throws JAXBException
   */
  public static Event valueOfJson(InputStream json)
          throws IOException, JSONException, XMLStreamException, JAXBException {
    // TODO Get this to work, it is currently returning null properties for all properties.
    if (context == null) {
      createJAXBContext();
    }

    BufferedReader streamReader = new BufferedReader(new InputStreamReader(json, "UTF-8"));
    StringBuilder jsonStringBuilder = new StringBuilder();
    String inputStr;
    while ((inputStr = streamReader.readLine()) != null)
      jsonStringBuilder.append(inputStr);

    JSONObject obj = new JSONObject(jsonStringBuilder.toString());
    Configuration config = new Configuration();
    config.setSupressAtAttributes(true);
    Map<String, String> xmlToJsonNamespaces = new HashMap<String, String>(1);
    xmlToJsonNamespaces.put(IndexObject.INDEX_XML_NAMESPACE, "");
    config.setXmlToJsonNamespaces(xmlToJsonNamespaces);
    MappedNamespaceConvention con = new MappedNamespaceConvention(config);
    XMLStreamReader xmlStreamReader = new MappedXMLStreamReader(obj, con);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    Event event = (Event) unmarshaller.unmarshal(xmlStreamReader);
    return event;
  }

  /**
   * Initialize the JAXBContext.
   */
  private static void createJAXBContext() throws JAXBException {
    context = JAXBContext.newInstance(Event.class);
  }

  /**
   * Serializes the recording event.
   *
   * @return the serialized recording event
   */
  @Override
  public String toJSON() {
    try {
      if (context == null) {
        createJAXBContext();
      }
      Marshaller marshaller = Event.context.createMarshaller();

      Configuration config = new Configuration();
      config.setSupressAtAttributes(true);
      MappedNamespaceConvention con = new MappedNamespaceConvention(config);
      StringWriter writer = new StringWriter();
      XMLStreamWriter xmlStreamWriter = new MappedXMLStreamWriter(con, writer) {
        @Override
        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }

        @Override
        public void writeStartElement(String uri, String local) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }

        @Override
        public void setPrefix(String pfx, String uri) throws XMLStreamException {
        }

        @Override
        public void setDefaultNamespace(String uri) throws XMLStreamException {
        }
      };

      marshaller.marshal(this, xmlStreamWriter);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

  /**
   * Serializes the recording event to an XML format.
   *
   * @return A String with this event's content as XML.
   */
  public String toXML() {
    try {
      if (context == null) {
        createJAXBContext();
      }
      StringWriter writer = new StringWriter();
      Marshaller marshaller = Event.context.createMarshaller();
      marshaller.marshal(this, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e.getLinkedException() != null ? e.getLinkedException() : e);
    }
  }

}
