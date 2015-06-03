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

package org.opencastproject.index.service.impl.index.event;

import org.opencastproject.matterhorn.search.impl.AbstractSearchQuery;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.Permissions.Action;
import org.opencastproject.security.api.User;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This interface defines a fluent api for a query object used to lookup recording events in the search index.
 */
public class EventSearchQuery extends AbstractSearchQuery {

  private final List<String> identifiers = new ArrayList<String>();
  private String organization = null;
  private User user = null;
  private String title = null;
  private String description = null;
  private final Set<String> actions = new HashSet<String>();
  private final List<String> presenters = new ArrayList<String>();
  private final List<String> contributors = new ArrayList<String>();
  private String subject = null;
  private String location = null;
  private String seriesId = null;
  private String seriesName = null;
  private String language = null;
  private String source = null;
  private String created = null;
  private Date startFrom = null;
  private Date startTo = null;
  private String creator = null;
  private String license = null;
  private String rights = null;
  private final List<String> trackMimetypes = new ArrayList<String>();
  private final List<String> trackStreamResolutions = new ArrayList<String>();
  private final List<String> trackFlavors = new ArrayList<String>();
  private final List<String> metadataFlavors = new ArrayList<String>();
  private final List<String> metadataMimetypes = new ArrayList<String>();
  private final List<String> attachmentFlavors = new ArrayList<String>();
  private final List<String> publicationFlavors = new ArrayList<String>();
  private String accessPolicy = null;
  private String managedAcl = null;
  private String workflowState = null;
  private Long workflowId = null;
  private String workflowDefinition = null;
  private Long duration = null;
  private String startDate = null;
  private String eventStatus = null;
  private String reviewStatus = null;
  private String schedulingStatus = null;
  private Boolean optedOut = null;
  private String reviewDate = null;
  private Boolean blacklisted = null;
  private Boolean hasComments = null;
  private Boolean hasOpenComments = null;
  private final List<String> publications = new ArrayList<String>();
  private String workflowScheduledDate = null;
  private Long archiveVersion = null;

  @SuppressWarnings("unused")
  private EventSearchQuery() {
  }

  /**
   * Creates a query that will return event documents.
   */
  public EventSearchQuery(String organization, User user) {
    super(Event.DOCUMENT_TYPE);
    this.organization = organization;
    this.user = user;
    this.actions.add(Permissions.Action.READ.toString());
    if (!user.getOrganization().getId().equals(organization))
      throw new IllegalStateException("User's organization must match search organization");
  }

  /**
   * Selects recording events with the given identifier.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple recording events.
   *
   * @param id
   *          the recording identifier
   * @return the enhanced search query
   */
  public EventSearchQuery withIdentifier(String id) {
    if (StringUtils.isBlank(id))
      throw new IllegalArgumentException("Identifier cannot be null");
    this.identifiers.add(id);
    return this;
  }

  /**
   * Returns the list of recording identifiers or an empty array if no identifiers have been specified.
   *
   * @return the identifiers
   */
  public String[] getIdentifier() {
    return identifiers.toArray(new String[identifiers.size()]);
  }

  /**
   * Returns the organization of the recording
   *
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * Returns the user of this search query
   *
   * @return the user of this search query
   */
  public User getUser() {
    return user;
  }

  /**
   * Selects recordings with the given title.
   *
   * @param title
   *          the title
   * @return the enhanced search query
   */
  public EventSearchQuery withTitle(String title) {
    clearExpectations();
    this.title = title;
    return this;
  }

  /**
   * Returns the title of the recording.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Filter the recording events without any action checked.
   *
   * @return the enhanced search query
   */
  public EventSearchQuery withoutActions() {
    clearExpectations();
    this.actions.clear();
    return this;
  }

  /**
   * Filter the recording events with the given action.
   * <p>
   * Note that this method may be called multiple times to support filtering by multiple actions.
   *
   * @param action
   *          the action
   * @return the enhanced search query
   */
  public EventSearchQuery withAction(Action action) {
    if (action == null)
      throw new IllegalArgumentException("Action cannot be null");
    clearExpectations();
    this.actions.add(action.toString());
    return this;
  }

  /**
   * Returns the list of actions or an empty array if no actions have been specified.
   *
   * @return the actions
   */
  public String[] getActions() {
    return actions.toArray(new String[actions.size()]);
  }

  /**
   * Selects recording events with the given presenter.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple recording events.
   *
   * @param presenter
   *          the presenter
   * @return the enhanced search query
   */
  public EventSearchQuery withPresenter(String presenter) {
    if (StringUtils.isBlank(presenter))
      throw new IllegalArgumentException("Presenter cannot be null");
    clearExpectations();
    this.presenters.add(presenter);
    return this;
  }

  /**
   * Returns the list of recording presenters or an empty array if no presenter have been specified.
   *
   * @return the presenters
   */
  public String[] getPresenters() {
    return presenters.toArray(new String[presenters.size()]);
  }

  /**
   * Selects recording events with the given contributor.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple recording events.
   *
   * @param contributor
   *          the contributor
   * @return the enhanced search query
   */
  public EventSearchQuery withContributor(String contributor) {
    if (StringUtils.isBlank(contributor))
      throw new IllegalArgumentException("Contributor cannot be null");
    clearExpectations();
    this.contributors.add(contributor);
    return this;
  }

  /**
   * Returns the list of recording contributors or an empty array if no contributors have been specified.
   *
   * @return the contributors
   */
  public String[] getContributors() {
    return contributors.toArray(new String[contributors.size()]);
  }

  /**
   * Selects recording events with the given subject.
   *
   * @param subject
   *          the subject
   * @return the enhanced search query
   */
  public EventSearchQuery withSubject(String subject) {
    clearExpectations();
    this.subject = subject;
    return this;
  }

  /**
   * Returns the subject of the recording.
   *
   * @return the subject
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Selects recordings with the given description.
   *
   * @param description
   *          the description
   * @return the enhanced search query
   */
  public EventSearchQuery withDescription(String description) {
    clearExpectations();
    this.description = description;
    return this;
  }

  /**
   * Returns the description of the recording.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Selects recordings with the given location.
   *
   * @param location
   *          the location
   * @return the enhanced search query
   */
  public EventSearchQuery withLocation(String location) {
    clearExpectations();
    this.location = location;
    return this;
  }

  /**
   * Returns the location of the recording.
   *
   * @return the location
   */
  public String getLocation() {
    return location;
  }

  /**
   * Selects recordings with the given series identifier.
   *
   * @param seriesId
   *          the series identifier
   * @return the enhanced search query
   */
  public EventSearchQuery withSeriesId(String seriesId) {
    clearExpectations();
    this.seriesId = seriesId;
    return this;
  }

  /**
   * Returns the series identifier of the recording.
   *
   * @return the series identifier
   */
  public String getSeriesId() {
    return seriesId;
  }

  /**
   * Selects recordings with the given series name.
   *
   * @param seriesName
   *          the series name
   * @return the enhanced search query
   */
  public EventSearchQuery withSeriesName(String seriesName) {
    clearExpectations();
    this.seriesName = seriesName;
    return this;
  }

  /**
   * Returns the series name of the recording.
   *
   * @return the series name
   */
  public String getSeriesName() {
    return seriesName;
  }

  /**
   * Selects recordings with the given language.
   *
   * @param language
   *          the language
   * @return the enhanced search query
   */
  public EventSearchQuery withLanguage(String language) {
    clearExpectations();
    this.language = language;
    return this;
  }

  /**
   * Returns the language of the recording.
   *
   * @return the language
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Selects recordings with the given source type.
   *
   * @param source
   *          the source
   * @return the enhanced search query
   */
  public EventSearchQuery withSource(String source) {
    clearExpectations();
    this.source = source;
    return this;
  }

  /**
   * Returns the source of the recording.
   *
   * @return the source
   */
  public String getSource() {
    return source;
  }

  /**
   * Selects recordings with the given creation date.
   *
   * @param created
   *          the creation date
   * @return the enhanced search query
   */
  public EventSearchQuery withCreated(String created) {
    clearExpectations();
    this.created = created;
    return this;
  }

  /**
   * Returns the creation date of the recording.
   *
   * @return the creation date
   */
  public String getCreated() {
    return created;
  }

  /**
   * The start date to start looking for events.
   *
   * @param startFrom
   *          The start date to start looking for events
   * @return the enhanced search query
   */
  public EventSearchQuery withStartFrom(Date startFrom) {
    this.startFrom = startFrom;
    return this;
  }

  /**
   * @return The Date after which all events returned should have been started
   */
  public Date getStartFrom() {
    return startFrom;
  }

  /**
   * The start date to stop looking for events.
   *
   * @param startTo
   *          The start date to stop looking for events
   * @return the enhanced search query
   */
  public EventSearchQuery withStartTo(Date startTo) {
    this.startTo = startTo;
    return this;
  }

  /**
   * @return The Date before which all events returned should have been started
   */
  public Date getStartTo() {
    return startTo;
  }

  /**
   * Selects recordings with the given creator.
   *
   * @param creator
   *          the creator
   * @return the enhanced search query
   */
  public EventSearchQuery withCreator(String creator) {
    clearExpectations();
    this.creator = creator;
    return this;
  }

  /**
   * Returns the creator of the recording.
   *
   * @return the creator
   */
  public String getCreator() {
    return creator;
  }

  /**
   * Selects recordings with the given license.
   *
   * @param license
   *          the license
   * @return the enhanced search query
   */
  public EventSearchQuery withLicense(String license) {
    clearExpectations();
    this.license = license;
    return this;
  }

  /**
   * Returns the license of the recording.
   *
   * @return the license
   */
  public String getLicense() {
    return license;
  }

  /**
   * Selects recordings with the given rights.
   *
   * @param rights
   *          the rights
   * @return the enhanced search query
   */
  public EventSearchQuery withRights(String rights) {
    clearExpectations();
    this.rights = rights;
    return this;
  }

  /**
   * Returns the rights of the recording.
   *
   * @return the rights
   */
  public String getRights() {
    return rights;
  }

  /**
   * Selects recording events with the given track type.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple recording events.
   *
   * @param trackMimetypes
   *          the track type
   * @return the enhanced search query
   */
  public EventSearchQuery withTrackMimetype(String trackMimetypes) {
    if (StringUtils.isBlank(trackMimetypes))
      throw new IllegalArgumentException("Track mimetype cannot be null");
    clearExpectations();
    this.trackMimetypes.add(trackMimetypes);
    return this;
  }

  /**
   * Returns the list of track types or an empty array if no track type have been specified.
   *
   * @return the track types
   */
  public String[] getTrackMimetypes() {
    return trackMimetypes.toArray(new String[trackMimetypes.size()]);
  }

  /**
   * Selects recording events with the given track stream resolution.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple recording events.
   *
   * @param trackStreamResolution
   *          the track stream resolution
   * @return the enhanced search query
   */
  public EventSearchQuery withTrackStreamResolution(String trackStreamResolution) {
    if (StringUtils.isBlank(trackStreamResolution))
      throw new IllegalArgumentException("Track stream resolution cannot be null");
    clearExpectations();
    this.trackStreamResolutions.add(trackStreamResolution);
    return this;
  }

  /**
   * Returns the list of track stream resolution or an empty array if no track type have been specified.
   *
   * @return the track stream resolution
   */
  public String[] getTrackStreamResolution() {
    return trackStreamResolutions.toArray(new String[trackStreamResolutions.size()]);
  }

  /**
   * Selects recording events with the given track flavor.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple recording events.
   *
   * @param trackFlavor
   *          the track flavor
   * @return the enhanced search query
   */
  public EventSearchQuery withTrackFlavor(String trackFlavor) {
    if (StringUtils.isBlank(trackFlavor))
      throw new IllegalArgumentException("Track flavor cannot be null");
    clearExpectations();
    this.trackFlavors.add(trackFlavor);
    return this;
  }

  /**
   * Returns the list of track flavors or an empty array if no track type have been specified.
   *
   * @return the track flavor
   */
  public String[] getTrackFlavor() {
    return trackFlavors.toArray(new String[trackFlavors.size()]);
  }

  /**
   * Selects recording events with the given track flavor.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple recording events.
   *
   * @param metadataFlavor
   *          the metadata flavor
   * @return the enhanced search query
   */
  public EventSearchQuery withMetadataFlavor(String metadataFlavor) {
    if (StringUtils.isBlank(metadataFlavor))
      throw new IllegalArgumentException("Metadata flavor cannot be null");
    clearExpectations();
    this.metadataFlavors.add(metadataFlavor);
    return this;
  }

  /**
   * Returns the list of track flavors or an empty array if no track type have been specified.
   *
   * @return the metadata flavor
   */
  public String[] getMetadataFlavor() {
    return metadataFlavors.toArray(new String[metadataFlavors.size()]);
  }

  /**
   * Selects recording events with the given metadata mimetype.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple recording events.
   *
   * @param metadataMimetype
   *          the metadata mimetype
   * @return the enhanced search query
   */
  public EventSearchQuery withMetadataMimetype(String metadataMimetype) {
    if (StringUtils.isBlank(metadataMimetype))
      throw new IllegalArgumentException("Metadata mimetype cannot be null");
    clearExpectations();
    this.metadataMimetypes.add(metadataMimetype);
    return this;
  }

  /**
   * Returns the list of metadata mimetypes or an empty array if no track type have been specified.
   *
   * @return the metadata mimetypes
   */
  public String[] getMetadataMimetype() {
    return metadataMimetypes.toArray(new String[metadataMimetypes.size()]);
  }

  /**
   * Selects recording events with the given attachment flavor.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple recording events.
   *
   * @param attachmentFlavor
   *          the attachment flavor
   * @return the enhanced search query
   */
  public EventSearchQuery withAttachmentFlavor(String attachmentFlavor) {
    if (StringUtils.isBlank(attachmentFlavor))
      throw new IllegalArgumentException("Attachment flavor cannot be null");
    clearExpectations();
    this.attachmentFlavors.add(attachmentFlavor);
    return this;
  }

  /**
   * Returns the list of attachment flavors or an empty array if no track type have been specified.
   *
   * @return the attachement flavors
   */
  public String[] getAttachmentFlavor() {
    return attachmentFlavors.toArray(new String[attachmentFlavors.size()]);
  }

  /**
   * Selects recording events with the given publication flavor.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple recording events.
   *
   * @param publicationFlavor
   *          the publication flavor
   * @return the enhanced search query
   */
  public EventSearchQuery withPublicationFlavor(String publicationFlavor) {
    if (StringUtils.isBlank(publicationFlavor))
      throw new IllegalArgumentException("Publication flavor cannot be null");
    clearExpectations();
    this.publicationFlavors.add(publicationFlavor);
    return this;
  }

  /**
   * Returns the list of publication flavors or an empty array if no track type have been specified.
   *
   * @return the attachement flavors
   */
  public String[] getPublicationFlavor() {
    return publicationFlavors.toArray(new String[publicationFlavors.size()]);
  }

  /**
   * Selects recordings with the given access policy.
   *
   * @param accessPolicy
   *          the access policy
   * @return the enhanced search query
   */
  public EventSearchQuery withAccessPolicy(String accessPolicy) {
    clearExpectations();
    this.accessPolicy = accessPolicy;
    return this;
  }

  /**
   * Returns the access policy of the recording.
   *
   * @return the access policy
   */
  public String getAccessPolicy() {
    return accessPolicy;
  }

  /**
   * Selects recordings with the given managed ACL name.
   *
   * @param managedAcl
   *          the name of the managed ACL
   * @return the enhanced search query
   */
  public EventSearchQuery withManagedAcl(String managedAcl) {
    clearExpectations();
    this.managedAcl = managedAcl;
    return this;
  }

  /**
   * Returns the name of the managed ACL set to the recording.
   *
   * @return the name of the managed ACL
   */
  public String getManagedAcl() {
    return managedAcl;
  }

  /**
   * Selects recordings with the given workflow state.
   *
   * @param workflowState
   *          the workflow state
   * @return the enhanced search query
   */
  public EventSearchQuery withWorkflowState(String workflowState) {
    clearExpectations();
    this.workflowState = workflowState;
    return this;
  }

  /**
   * Returns the workflow state of the recording.
   *
   * @return the workflow state
   */
  public String getWorkflowState() {
    return workflowState;
  }

  /**
   * Selects recordings with the given workflow id.
   *
   * @param workflowId
   *          the workflow id
   * @return the enhanced search query
   */
  public EventSearchQuery withWorkflowId(long workflowId) {
    clearExpectations();
    this.workflowId = workflowId;
    return this;
  }

  /**
   * Returns the workflow id of the recording.
   *
   * @return the workflow id
   */
  public Long getWorkflowId() {
    return workflowId;
  }

  /**
   * Selects recordings with the given workflow definition.
   *
   * @param workflowDefinition
   *          the workflow definition
   * @return the enhanced search query
   */
  public EventSearchQuery withWorkflowDefinition(String workflowDefinition) {
    clearExpectations();
    this.workflowDefinition = workflowDefinition;
    return this;
  }

  /**
   * Returns the workflow definition of the recording.
   *
   * @return the workflow definition
   */
  public String getWorkflowDefinition() {
    return workflowDefinition;
  }

  /**
   * Selects recordings with the given start date.
   *
   * @param startDate
   *          the start date
   * @return the enhanced search query
   */
  public EventSearchQuery withStartDate(String startDate) {
    clearExpectations();
    this.startDate = startDate;
    return this;
  }

  /**
   * Returns the start date of the recording.
   *
   * @return the start date
   */
  public String getStartDate() {
    return startDate;
  }

  /**
   * Selects recordings with the given duration.
   *
   * @param duration
   *          the duration
   * @return the enhanced search query
   */
  public EventSearchQuery withDuration(long duration) {
    clearExpectations();
    this.duration = duration;
    return this;
  }

  /**
   * Returns the duration of the recording.
   *
   * @return the duration
   */
  public Long getDuration() {
    return duration;
  }

  /**
   * Selects recordings with the given review status.
   *
   * @param reviewStatus
   *          the review status
   * @return the enhanced search query
   */
  public EventSearchQuery withReviewStatus(String reviewStatus) {
    clearExpectations();
    this.reviewStatus = reviewStatus;
    return this;
  }

  /**
   * Returns the review status of the recording.
   *
   * @return the review status
   */
  public String getReviewStatus() {
    return reviewStatus;
  }

  /**
   * Selects recordings with the given scheduling status.
   *
   * @param schedulingStatus
   *          the scheduling status
   * @return the enhanced search query
   */
  public EventSearchQuery withSchedulingStatus(String schedulingStatus) {
    clearExpectations();
    this.schedulingStatus = schedulingStatus;
    return this;
  }

  /**
   * Returns the scheduling status of the recording.
   *
   * @return the scheduling status
   */
  public String getSchedulingStatus() {
    return schedulingStatus;
  }

  /**
   * Selects recordings with the given event status.
   *
   * @param eventStatus
   *          the event status
   * @return the enhanced search query
   */
  public EventSearchQuery withEventStatus(String eventStatus) {
    clearExpectations();
    this.eventStatus = eventStatus;
    return this;
  }

  /**
   * Returns the event status of the recording.
   *
   * @return the event status
   */
  public String getEventStatus() {
    return eventStatus;
  }

  /**
   * Selects recordings with the given review date.
   *
   * @param reviewDate
   *          the review date
   * @return the enhanced search query
   */
  public EventSearchQuery withReviewDate(String reviewDate) {
    clearExpectations();
    this.reviewDate = reviewDate;
    return this;
  }

  /**
   * Returns the review date of the recording.
   *
   * @return the review date
   */
  public String getReviewDate() {
    return reviewDate;
  }

  /**
   * Selects recordings with the given recording status (opted out).
   *
   * @param optedOut
   *          the recording status
   * @return the enhanced search query
   */
  public EventSearchQuery withOptedOut(boolean optedOut) {
    clearExpectations();
    this.optedOut = optedOut;
    return this;
  }

  /**
   * Returns the recording status (opted out) of the recording.
   *
   * @return the recording out
   */
  public Boolean getOptedOut() {
    return optedOut;
  }

  /**
   * Selects recordings with the given recording status (blacklisted).
   *
   * @param blacklisted
   *          the recording status (blacklisted)
   * @return the enhanced search query
   */
  public EventSearchQuery withBlacklisted(boolean blacklisted) {
    clearExpectations();
    this.blacklisted = blacklisted;
    return this;
  }

  /**
   * Returns the recording status (blacklisted) of the recording.
   *
   * @return the recording backlisted status
   */
  public Boolean getBlacklisted() {
    return blacklisted;
  }

  /**
   * Selects recordings with the given has comments status.
   *
   * @param hasComments
   *          the has comments status
   * @return the enhanced search query
   */
  public EventSearchQuery withComments(boolean hasComments) {
    clearExpectations();
    this.hasComments = hasComments;
    return this;
  }

  /**
   * Selects recordings with the given has open comments status.
   *
   * @param hasOpenComments
   *          the has open comments status
   * @return the enhanced search query
   */
  public EventSearchQuery withOpenComments(boolean hasOpenComments) {
    clearExpectations();
    this.hasOpenComments = hasOpenComments;
    return this;
  }

  /**
   * Returns the has comments status of the recording.
   *
   * @return the recording has comments status
   */
  public Boolean getHasComments() {
    return hasComments;
  }

  /**
   * Returns the has open comments status of the recording.
   *
   * @return the recording has open comments status
   */
  public Boolean getHasOpenComments() {
    return hasOpenComments;
  }

  /**
   * Selects recording events with the given publication.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple recording events.
   *
   * @param publication
   *          the publication
   * @return the enhanced search query
   */
  public EventSearchQuery withPublications(String publication) {
    if (StringUtils.isBlank(publication))
      throw new IllegalArgumentException("Publication cannot be null");
    clearExpectations();
    this.publications.add(publication);
    return this;
  }

  /**
   * Returns the list of event publications or an empty array if no publications have been specified.
   *
   * @return the publications
   */
  public String[] getPublications() {
    return publications.toArray(new String[publications.size()]);
  }

  /**
   * Selects events with the given workflow scheduled date.
   *
   * @param workflowScheduledDate
   *          the workflow scheduled date
   * @return the enhanced search query
   */
  public EventSearchQuery withWorkflowScheduledDate(String workflowScheduledDate) {
    clearExpectations();
    this.workflowScheduledDate = workflowScheduledDate;
    return this;
  }

  /**
   * Returns the workflow scheduled date of the event.
   *
   * @return the workflow scheduled date
   */
  public String getWorkflowScheduledDate() {
    return workflowScheduledDate;
  }

  /**
   * Selects events with the given archive version.
   *
   * @param archiveVersion
   *          the archive version
   * @return the enhanced search query
   */
  public EventSearchQuery withArchiveVersion(long archiveVersion) {
    clearExpectations();
    this.archiveVersion = archiveVersion;
    return this;
  }

  /**
   * Returns the archive version of the event.
   *
   * @return the archive version
   */
  public Long getArchiveVersion() {
    return archiveVersion;
  }

  /**
   * Defines the sort order for the recording start date.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public EventSearchQuery sortByStartDate(Order order) {
    withSortOrder(EventIndexSchema.START_DATE, order);
    return this;
  }

  /**
   * Returns the sort order for the recording start date.
   *
   * @return the sort order
   */
  public Order getStartDateSortOrder() {
    return getSortOrder(EventIndexSchema.START_DATE);
  }

  /**
   * Defines the sort order for the recording end date.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public EventSearchQuery sortByEndDate(Order order) {
    withSortOrder(EventIndexSchema.END_DATE, order);
    return this;
  }

  /**
   * Returns the sort order for the recording end date.
   *
   * @return the sort order
   */
  public Order getEndDateSortOrder() {
    return getSortOrder(EventIndexSchema.END_DATE);
  }

  /**
   * Defines the sort order for the recording date.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public EventSearchQuery sortByDate(Order order) {
    withSortOrder(EventIndexSchema.END_DATE, order);
    return this;
  }

  /**
   * Returns the sort order for the recording date.
   *
   * @return the sort order
   */
  public Order getDateSortOrder() {
    return getSortOrder(EventIndexSchema.END_DATE);
  }

  /**
   * Defines the sort order for the recording date.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public EventSearchQuery sortByTitle(Order order) {
    withSortOrder(EventIndexSchema.TITLE, order);
    return this;
  }

  /**
   * Returns the sort order for the recording start date.
   *
   * @return the sort order
   */
  public Order getTitleSortOrder() {
    return getSortOrder(EventIndexSchema.TITLE);
  }

  /**
   * Defines the sort order for the recording date.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public EventSearchQuery sortByPresenter(Order order) {
    withSortOrder(EventIndexSchema.PRESENTER, order);
    return this;
  }

  /**
   * Returns the sort order for the recording start date.
   *
   * @return the sort order
   */
  public Order getPresentersSortOrder() {
    return getSortOrder(EventIndexSchema.PRESENTER);
  }

  /**
   * Defines the sort order for the location.
   *
   * @param order
   *          the sort order
   * @return the updated query
   */
  public EventSearchQuery sortByLocation(Order order) {
    withSortOrder(EventIndexSchema.LOCATION, order);
    return this;
  }

  /**
   * Returns the sort order for the location.
   *
   * @return the sort order
   */
  public Order getLocationSortOrder() {
    return getSortOrder(EventIndexSchema.LOCATION);
  }

  /**
   * Defines the sort order for the series name.
   *
   * @param order
   *          the sort order
   * @return the updated query
   */
  public EventSearchQuery sortBySeriesName(Order order) {
    withSortOrder(EventIndexSchema.SERIES_NAME, order);
    return this;
  }

  /**
   * Returns the sort order for the series name.
   *
   * @return the sort order
   */
  public Order getSeriesNameSortOrder() {
    return getSortOrder(EventIndexSchema.SERIES_NAME);
  }

  /**
   * Defines the sort order for the managed ACL.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public EventSearchQuery sortByManagedAcl(Order order) {
    withSortOrder(EventIndexSchema.MANAGED_ACL, order);
    return this;
  }

  /**
   * Returns the sort order for the series managed ACL.
   *
   * @return the sort order
   */
  public Order getManagedAclSortOrder() {
    return getSortOrder(EventIndexSchema.MANAGED_ACL);
  }

  /**
   * Defines the sort order for the review status.
   *
   * @param order
   *          the sort order
   * @return the updated query
   */
  public EventSearchQuery sortByReviewStatus(Order order) {
    withSortOrder(EventIndexSchema.REVIEW_STATUS, order);
    return this;
  }

  /**
   * Returns the sort order for the review status.
   *
   * @return the sort order
   */
  public Order getReviewStatusSortOrder() {
    return getSortOrder(EventIndexSchema.REVIEW_STATUS);
  }

  /**
   * Defines the sort order for the workflow state.
   *
   * @param order
   *          the sort order
   * @return the updated query
   */
  public EventSearchQuery sortByWorkflowState(Order order) {
    withSortOrder(EventIndexSchema.WORKFLOW_STATE, order);
    return this;
  }

  /**
   * Returns the sort order for the workflow state.
   *
   * @return the sort order
   */
  public Order getWorkflowStateSortOrder() {
    return getSortOrder(EventIndexSchema.WORKFLOW_STATE);
  }

  /**
   * Defines the sort order for the scheduling status.
   *
   * @param order
   *          the sort order
   * @return the updated query
   */
  public EventSearchQuery sortBySchedulingStatus(Order order) {
    withSortOrder(EventIndexSchema.SCHEDULING_STATUS, order);
    return this;
  }

  /**
   * Returns the sort order for the scheduling status.
   *
   * @return the sort order
   */
  public Order getSchedulingStatusSortOrder() {
    return getSortOrder(EventIndexSchema.SCHEDULING_STATUS);
  }

  /**
   * Defines the sort order for the event status.
   *
   * @param order
   *          the sort order
   * @return the updated query
   */
  public EventSearchQuery sortByEventStatus(Order order) {
    withSortOrder(EventIndexSchema.EVENT_STATUS, order);
    return this;
  }

  /**
   * Returns the sort order for the event status.
   *
   * @return the sort order
   */
  public Order getEventStatusSortOrder() {
    return getSortOrder(EventIndexSchema.EVENT_STATUS);
  }

}
