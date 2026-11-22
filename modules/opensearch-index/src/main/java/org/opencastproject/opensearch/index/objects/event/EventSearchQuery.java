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


package org.opencastproject.opensearch.index.objects.event;

import static org.opencastproject.opensearch.impl.IndexSchema.SORT_FIELD_NAME_EXTENSION;

import org.opencastproject.opensearch.index.objects.AbstractIndexQuery;
import org.opencastproject.security.api.User;
import org.opencastproject.util.requests.SortCriterion.Order;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Query for events in the search index.
 */
public class EventSearchQuery extends AbstractIndexQuery<EventSearchQuery> {

  /** Text fields which are sorted by their dedicated sort field */
  private static final Set<String> SORT_FIELDS = Set.of(
      EventIndexSchema.TITLE, EventIndexSchema.CONTRIBUTOR, EventIndexSchema.PRESENTER, EventIndexSchema.SUBJECT,
      EventIndexSchema.DESCRIPTION, EventIndexSchema.LOCATION, EventIndexSchema.SERIES_NAME,
      EventIndexSchema.CREATOR, EventIndexSchema.PUBLISHER);

  private Date startFrom = null;
  private Date startTo = null;
  private Date technicalStartFrom = null;
  private Date technicalStartTo = null;

  /**
   * Creates a query for events of the given organization, visible to the given user.
   *
   * @param organization
   *          the organization
   * @param user
   *          the user
   */
  public EventSearchQuery(String organization, User user) {
    super(organization, user);
  }

  @Override
  protected String organizationField() {
    return EventIndexSchema.ORGANIZATION;
  }

  @Override
  protected String aclPermissionPrefix() {
    return EventIndexSchema.ACL_PERMISSION_PREFIX;
  }

  @Override
  protected String extendedMetadataPrefix() {
    return EventIndexSchema.EXTENDED_METADATA_PREFIX;
  }

  @Override
  protected String sortField(String field) {
    return SORT_FIELDS.contains(field) ? field + SORT_FIELD_NAME_EXTENSION : field;
  }

  @Override
  protected List<String> additionalTextFields() {
    return List.of(EventIndexSchema.UID, EventIndexSchema.SERIES_ID);
  }

  /**
   * Adds a condition on an arbitrary index field.
   *
   * @param field
   *          the index field, e.g. {@link EventIndexSchema#TITLE}
   * @param value
   *          the value to compare with
   * @param type
   *          how to compare the value
   * @param must
   *          whether the condition has to be fulfilled or must not be fulfilled; only applies to
   *          {@link EventQueryType#SEARCH} and {@link EventQueryType#WILDCARD}
   * @return this query
   */
  public EventSearchQuery withField(String field, String value, EventQueryType type, boolean must) {
    return switch (type) {
      case SEARCH -> must ? and(field, value) : andNot(field, value);
      case WILDCARD -> andWildcard(field, value, must);
      case GREATER_THAN -> andAfter(field, value);
      case LESS_THAN -> andBefore(field, value);
    };
  }

  /** Only return the event with the given identifier. Can be called multiple times to find several events. */
  public EventSearchQuery withIdentifier(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Identifier cannot be null");
    }
    return and(EventIndexSchema.UID, id);
  }

  public EventSearchQuery withTitle(String title) {
    return set(EventIndexSchema.TITLE, title);
  }

  public EventSearchQuery withDescription(String description) {
    return set(EventIndexSchema.DESCRIPTION, description);
  }

  /** Only return events with this presenter. Multiple presenters are combined with OR. */
  public EventSearchQuery withPresenter(String presenter) {
    if (StringUtils.isBlank(presenter)) {
      throw new IllegalArgumentException("Presenter cannot be null");
    }
    return and(EventIndexSchema.PRESENTER, presenter);
  }

  /** Only return events with this contributor. Multiple contributors are combined with OR. */
  public EventSearchQuery withContributor(String contributor) {
    if (StringUtils.isBlank(contributor)) {
      throw new IllegalArgumentException("Contributor cannot be null");
    }
    return and(EventIndexSchema.CONTRIBUTOR, contributor);
  }

  public EventSearchQuery withSubject(String subject) {
    return set(EventIndexSchema.SUBJECT, subject);
  }

  public EventSearchQuery withLocation(String location) {
    return set(EventIndexSchema.LOCATION, location);
  }

  /** Only return events of this series. Multiple series are combined with OR. */
  public EventSearchQuery withSeriesId(String seriesId) {
    if (StringUtils.isBlank(seriesId)) {
      throw new IllegalArgumentException("Series id cannot be null");
    }
    return and(EventIndexSchema.SERIES_ID, seriesId);
  }

  public EventSearchQuery withSeriesName(String seriesName) {
    return set(EventIndexSchema.SERIES_NAME, seriesName);
  }

  public EventSearchQuery withLanguage(String language) {
    return set(EventIndexSchema.LANGUAGE, language);
  }

  public EventSearchQuery withSource(String source) {
    return set(EventIndexSchema.SOURCE, source);
  }

  public EventSearchQuery withCreated(String created) {
    return set(EventIndexSchema.CREATED, created);
  }

  public EventSearchQuery withCreator(String creator) {
    return set(EventIndexSchema.CREATOR, creator);
  }

  public EventSearchQuery withPublisher(String publisher) {
    return set(EventIndexSchema.PUBLISHER, publisher);
  }

  public EventSearchQuery withLicense(String license) {
    return set(EventIndexSchema.LICENSE, license);
  }

  public EventSearchQuery withRights(String rights) {
    return set(EventIndexSchema.RIGHTS, rights);
  }

  public EventSearchQuery withManagedAcl(String managedAcl) {
    return set(EventIndexSchema.MANAGED_ACL, managedAcl);
  }

  public EventSearchQuery withWorkflowState(String workflowState) {
    return set(EventIndexSchema.WORKFLOW_STATE, workflowState);
  }

  public EventSearchQuery withWorkflowId(long workflowId) {
    return set(EventIndexSchema.WORKFLOW_ID, workflowId);
  }

  public EventSearchQuery withEventStatus(String eventStatus) {
    return set(EventIndexSchema.EVENT_STATUS, eventStatus);
  }

  public EventSearchQuery withComments(boolean hasComments) {
    return set(EventIndexSchema.HAS_COMMENTS, hasComments);
  }

  public EventSearchQuery withOpenComments(boolean hasOpenComments) {
    return set(EventIndexSchema.HAS_OPEN_COMMENTS, hasOpenComments);
  }

  public EventSearchQuery withNeedsCutting(boolean needsCutting) {
    return set(EventIndexSchema.NEEDS_CUTTING, needsCutting);
  }

  public EventSearchQuery withIsPublished(boolean isPublished) {
    return set(EventIndexSchema.IS_PUBLISHED, isPublished);
  }

  public EventSearchQuery withAgentId(String agentId) {
    return set(EventIndexSchema.AGENT_ID, agentId);
  }

  /** Only return events with this technical presenter. Multiple presenters are combined with OR. */
  public EventSearchQuery withTechnicalPresenters(String presenter) {
    if (StringUtils.isBlank(presenter)) {
      throw new IllegalArgumentException("Presenter cannot be null");
    }
    return and(EventIndexSchema.TECHNICAL_PRESENTERS, presenter);
  }

  /** Only return events starting at or after this date. Only effective together with {@link #withStartTo(Date)}. */
  public EventSearchQuery withStartFrom(Date startFrom) {
    this.startFrom = startFrom;
    return startFrom != null && startTo != null ? andBetween(EventIndexSchema.START_DATE, startFrom, startTo) : this;
  }

  /** Only return events starting at or before this date. Only effective together with {@link #withStartFrom(Date)}. */
  public EventSearchQuery withStartTo(Date startTo) {
    this.startTo = startTo;
    return startFrom != null && startTo != null ? andBetween(EventIndexSchema.START_DATE, startFrom, startTo) : this;
  }

  /**
   * Only return events with a technical start at or after this date. Only effective together with
   * {@link #withTechnicalStartTo(Date)}.
   */
  public EventSearchQuery withTechnicalStartFrom(Date startFrom) {
    this.technicalStartFrom = startFrom;
    return technicalStartFrom != null && technicalStartTo != null
        ? andBetween(EventIndexSchema.TECHNICAL_START, technicalStartFrom, technicalStartTo)
        : this;
  }

  /**
   * Only return events with a technical start at or before this date. Only effective together with
   * {@link #withTechnicalStartFrom(Date)}.
   */
  public EventSearchQuery withTechnicalStartTo(Date startTo) {
    this.technicalStartTo = startTo;
    return technicalStartFrom != null && technicalStartTo != null
        ? andBetween(EventIndexSchema.TECHNICAL_START, technicalStartFrom, technicalStartTo)
        : this;
  }

  public EventSearchQuery sortByStartDate(Order order) {
    return withSortOrder(EventIndexSchema.START_DATE, order);
  }

  public EventSearchQuery sortByTechnicalStartDate(Order order) {
    return withSortOrder(EventIndexSchema.TECHNICAL_START, order);
  }

  public EventSearchQuery sortByEndDate(Order order) {
    return withSortOrder(EventIndexSchema.END_DATE, order);
  }

  public EventSearchQuery sortByTechnicalEndDate(Order order) {
    return withSortOrder(EventIndexSchema.TECHNICAL_END, order);
  }

  public EventSearchQuery sortByTitle(Order order) {
    return withSortOrder(EventIndexSchema.TITLE, order);
  }

  public EventSearchQuery sortByUID(Order order) {
    return withSortOrder(EventIndexSchema.UID, order);
  }

  public EventSearchQuery sortByPresenter(Order order) {
    return withSortOrder(EventIndexSchema.PRESENTER, order);
  }

  public EventSearchQuery sortByLocation(Order order) {
    return withSortOrder(EventIndexSchema.LOCATION, order);
  }

  public EventSearchQuery sortBySeriesName(Order order) {
    return withSortOrder(EventIndexSchema.SERIES_NAME, order);
  }

  public EventSearchQuery sortByManagedAcl(Order order) {
    return withSortOrder(EventIndexSchema.MANAGED_ACL, order);
  }

  public EventSearchQuery sortByWorkflowState(Order order) {
    return withSortOrder(EventIndexSchema.WORKFLOW_STATE, order);
  }

  public EventSearchQuery sortByEventStatus(Order order) {
    return withSortOrder(EventIndexSchema.EVENT_STATUS, order);
  }

  /** Sort by the number of publications */
  public EventSearchQuery sortByPublicationIgnoringInternal(Order order) {
    // TODO: actually ignore the internal publication
    return withSortOrder(EventIndexSchema.PUBLICATION, order);
  }
}
