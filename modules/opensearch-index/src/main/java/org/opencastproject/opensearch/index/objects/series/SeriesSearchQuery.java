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


package org.opencastproject.opensearch.index.objects.series;

import static org.opencastproject.opensearch.impl.IndexSchema.SORT_FIELD_NAME_EXTENSION;

import org.opencastproject.opensearch.index.objects.AbstractIndexQuery;
import org.opencastproject.security.api.User;
import org.opencastproject.util.requests.SortCriterion.Order;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Query for series in the search index.
 */
public class SeriesSearchQuery extends AbstractIndexQuery<SeriesSearchQuery> {

  /** Text fields which are sorted by their dedicated sort field */
  private static final Set<String> SORT_FIELDS = Set.of(
      SeriesIndexSchema.TITLE, SeriesIndexSchema.DESCRIPTION, SeriesIndexSchema.SUBJECT, SeriesIndexSchema.CREATOR,
      SeriesIndexSchema.ORGANIZERS, SeriesIndexSchema.CONTRIBUTORS, SeriesIndexSchema.PUBLISHERS,
      SeriesIndexSchema.RIGHTS_HOLDER);

  private Date createdFrom = null;
  private Date createdTo = null;

  /**
   * Creates a query for series of the given organization, visible to the given user.
   *
   * @param organization
   *          the organization
   * @param user
   *          the user
   */
  public SeriesSearchQuery(String organization, User user) {
    super(organization, user);
  }

  @Override
  protected String organizationField() {
    return SeriesIndexSchema.ORGANIZATION;
  }

  @Override
  protected String aclPermissionPrefix() {
    return SeriesIndexSchema.ACL_PERMISSION_PREFIX;
  }

  @Override
  protected String extendedMetadataPrefix() {
    return SeriesIndexSchema.EXTENDED_METADATA_PREFIX;
  }

  @Override
  protected String sortField(String field) {
    return SORT_FIELDS.contains(field) ? field + SORT_FIELD_NAME_EXTENSION : field;
  }

  @Override
  protected List<String> additionalTextFields() {
    return List.of(SeriesIndexSchema.UID);
  }

  /** Only return the series with the given identifier. Can be called multiple times to find several series. */
  public SeriesSearchQuery withIdentifier(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Identifier cannot be null");
    }
    return and(SeriesIndexSchema.UID, id);
  }

  public SeriesSearchQuery withTitle(String title) {
    return set(SeriesIndexSchema.TITLE, title);
  }

  public SeriesSearchQuery withDescription(String description) {
    return set(SeriesIndexSchema.DESCRIPTION, description);
  }

  /** Only return series with this subject. Multiple subjects are combined with OR. */
  public SeriesSearchQuery withSubject(String subject) {
    if (StringUtils.isBlank(subject)) {
      throw new IllegalArgumentException("Subject cannot be null");
    }
    return and(SeriesIndexSchema.SUBJECT, subject);
  }

  public SeriesSearchQuery withLanguage(String language) {
    return set(SeriesIndexSchema.LANGUAGE, language);
  }

  public SeriesSearchQuery withCreator(String creator) {
    return set(SeriesIndexSchema.CREATOR, creator);
  }

  public SeriesSearchQuery withLicense(String license) {
    return set(SeriesIndexSchema.LICENSE, license);
  }

  public SeriesSearchQuery withTheme(long theme) {
    return set(SeriesIndexSchema.THEME, theme);
  }

  public SeriesSearchQuery withManagedAcl(String managedAcl) {
    return set(SeriesIndexSchema.MANAGED_ACL, managedAcl);
  }

  /** Only return series with this organizer. Multiple organizers are combined with OR. */
  public SeriesSearchQuery withOrganizer(String organizer) {
    if (StringUtils.isBlank(organizer)) {
      throw new IllegalArgumentException("Organizer cannot be null");
    }
    return and(SeriesIndexSchema.ORGANIZERS, organizer);
  }

  /** Only return series with this contributor. Multiple contributors are combined with OR. */
  public SeriesSearchQuery withContributor(String contributor) {
    if (StringUtils.isBlank(contributor)) {
      throw new IllegalArgumentException("Contributor can't be null");
    }
    return and(SeriesIndexSchema.CONTRIBUTORS, contributor);
  }

  /** Only return series with this publisher. Multiple publishers are combined with OR. */
  public SeriesSearchQuery withPublisher(String publisher) {
    if (StringUtils.isBlank(publisher)) {
      throw new IllegalArgumentException("Publisher can't be null");
    }
    return and(SeriesIndexSchema.PUBLISHERS, publisher);
  }

  public SeriesSearchQuery withRightsHolder(String rightsHolder) {
    return set(SeriesIndexSchema.RIGHTS_HOLDER, rightsHolder);
  }

  /** Only return series created at or after this date. Only effective together with {@link #withCreatedTo(Date)}. */
  public SeriesSearchQuery withCreatedFrom(Date createdFrom) {
    this.createdFrom = createdFrom;
    return createdFrom != null && createdTo != null
        ? andBetween(SeriesIndexSchema.CREATED_DATE_TIME, createdFrom, createdTo)
        : this;
  }

  /** Only return series created at or before this date. Only effective together with {@link #withCreatedFrom(Date)}. */
  public SeriesSearchQuery withCreatedTo(Date createdTo) {
    this.createdTo = createdTo;
    return createdFrom != null && createdTo != null
        ? andBetween(SeriesIndexSchema.CREATED_DATE_TIME, createdFrom, createdTo)
        : this;
  }

  public SeriesSearchQuery sortBySubject(Order order) {
    return withSortOrder(SeriesIndexSchema.SUBJECT, order);
  }

  public SeriesSearchQuery sortByCreator(Order order) {
    return withSortOrder(SeriesIndexSchema.CREATOR, order);
  }

  public SeriesSearchQuery sortByPublishers(Order order) {
    return withSortOrder(SeriesIndexSchema.PUBLISHERS, order);
  }

  public SeriesSearchQuery sortByDescription(Order order) {
    return withSortOrder(SeriesIndexSchema.DESCRIPTION, order);
  }

  public SeriesSearchQuery sortByLanguage(Order order) {
    return withSortOrder(SeriesIndexSchema.LANGUAGE, order);
  }

  public SeriesSearchQuery sortByRightsHolder(Order order) {
    return withSortOrder(SeriesIndexSchema.RIGHTS_HOLDER, order);
  }

  public SeriesSearchQuery sortByLicense(Order order) {
    return withSortOrder(SeriesIndexSchema.LICENSE, order);
  }

  public SeriesSearchQuery sortByContributors(Order order) {
    return withSortOrder(SeriesIndexSchema.CONTRIBUTORS, order);
  }

  public SeriesSearchQuery sortByManagedAcl(Order order) {
    return withSortOrder(SeriesIndexSchema.MANAGED_ACL, order);
  }

  public SeriesSearchQuery sortByCreatedDateTime(Order order) {
    return withSortOrder(SeriesIndexSchema.CREATED_DATE_TIME, order);
  }

  public SeriesSearchQuery sortByOrganizers(Order order) {
    return withSortOrder(SeriesIndexSchema.ORGANIZERS, order);
  }

  public SeriesSearchQuery sortByTitle(Order order) {
    return withSortOrder(SeriesIndexSchema.TITLE, order);
  }
}
