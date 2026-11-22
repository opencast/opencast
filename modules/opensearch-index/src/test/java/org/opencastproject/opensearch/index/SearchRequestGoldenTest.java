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

package org.opencastproject.opensearch.index;

import static org.opencastproject.opensearch.index.QueryJson.assertMatchesGolden;

import org.opencastproject.opensearch.index.objects.event.EventQueryType;
import org.opencastproject.opensearch.index.objects.event.EventSearchQuery;
import org.opencastproject.opensearch.index.objects.series.SeriesSearchQuery;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Permissions.Action;
import org.opencastproject.security.api.User;
import org.opencastproject.util.requests.SortCriterion.Order;

import org.junit.Test;

import java.util.Date;

/**
 * Makes sure the requests sent to OpenSearch for event and series queries do not change unintentionally.
 */
public class SearchRequestGoldenTest {

  private static final JaxbOrganization ORG = new DefaultOrganization();

  private static final User ADMIN = new JaxbUser("admin", "test", ORG,
      new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, ORG));

  private static final User USER = new JaxbUser("user", "test", ORG,
      new JaxbRole("ROLE_USER_A", ORG), new JaxbRole("ROLE_USER_B", ORG));

  private static final Date FROM = new Date(1700000000000L);
  private static final Date TO = new Date(1710000000000L);

  private final OpenSearchIndex index = new OpenSearchIndex();

  @Test
  public void eventAdminDefault() throws Exception {
    assertMatchesGolden("event-admin-default", index.searchRequestBody(new EventSearchQuery(ORG.getId(), ADMIN)));
  }

  @Test
  public void eventUserPermissions() throws Exception {
    final EventSearchQuery query = new EventSearchQuery(ORG.getId(), USER)
        .withAction(Action.WRITE);
    assertMatchesGolden("event-user-permissions", index.searchRequestBody(query));
  }

  @Test
  public void eventById() throws Exception {
    final EventSearchQuery query = new EventSearchQuery(ORG.getId(), USER)
        .withoutActions()
        .withIdentifier("event-1");
    assertMatchesGolden("event-by-id", index.searchRequestBody(query));
  }

  @Test
  public void eventFilters() throws Exception {
    final EventSearchQuery query = new EventSearchQuery(ORG.getId(), ADMIN)
        .withIdentifier("event-1")
        .withIdentifier("event-2")
        .withTitle("Title")
        .withDescription("Description")
        .withPresenter("Presenter A")
        .withPresenter("Presenter B")
        .withContributor("Contributor")
        .withSubject("Subject")
        .withLocation("Location")
        .withSeriesId("series-1")
        .withSeriesName("Series")
        .withLanguage("en")
        .withSource("Source")
        .withCreated("2024-01-01T00:00:00Z")
        .withCreator("Creator")
        .withPublisher("Publisher")
        .withLicense("CC-BY")
        .withRights("Rights")
        .withManagedAcl("Managed ACL")
        .withWorkflowState("SUCCEEDED")
        .withWorkflowId(42L)
        .withEventStatus("EVENTS.EVENTS.STATUS.PROCESSED")
        .withComments(true)
        .withOpenComments(false)
        .withNeedsCutting(true)
        .withIsPublished(true)
        .withAgentId("agent")
        .withTechnicalPresenters("tech-presenter")
        .withExtendedMetadata("dublincore/extended", "field", "value")
        .withAccessControlEntry("ROLE_X", Action.WRITE)
        .withStartFrom(FROM)
        .withStartTo(TO)
        .withTechnicalStartFrom(FROM)
        .withTechnicalStartTo(TO)
        .withStartDate("2024-02-01T00:00:00Z");
    query.withOffset(10);
    query.withLimit(20);
    assertMatchesGolden("event-filters", index.searchRequestBody(query));
  }

  @Test
  public void eventTextAndSort() throws Exception {
    final EventSearchQuery query = new EventSearchQuery(ORG.getId(), USER)
        .sortByTitle(Order.Ascending)
        .sortByStartDate(Order.Descending)
        .sortByPresenter(Order.Descending)
        .sortByWorkflowState(Order.None);
    query.withText("lecture physics");
    assertMatchesGolden("event-text-sort", index.searchRequestBody(query));
  }

  @Test
  public void eventPublicationSort() throws Exception {
    final EventSearchQuery query = new EventSearchQuery(ORG.getId(), ADMIN)
        .sortByPublicationIgnoringInternal(Order.Ascending);
    assertMatchesGolden("event-publication-sort", index.searchRequestBody(query));
  }

  @Test
  public void eventLifecycleFilters() throws Exception {
    final EventSearchQuery query = new EventSearchQuery(ORG.getId(), ADMIN)
        .withTitle("Lecture*", EventQueryType.WILDCARD, true)
        .withPresenter("Guest*", EventQueryType.WILDCARD, false)
        .withSeriesId("series-1", EventQueryType.SEARCH, false)
        .withSeriesId("series-2", EventQueryType.SEARCH, true)
        .withLanguage("de", EventQueryType.SEARCH, false)
        .withCreated("2020-01-01T00:00:00Z", EventQueryType.GREATER_THAN, true)
        .withStartDate("2021-01-01T00:00:00Z", EventQueryType.GREATER_THAN)
        .withEndDate("2022-01-01T00:00:00Z", EventQueryType.LESS_THAN);
    assertMatchesGolden("event-lifecycle", index.searchRequestBody(query));
  }

  /** A start date period must not replace other conditions on the start date, and vice versa. */
  @Test
  public void eventLifecycleFiltersWithPeriod() throws Exception {
    final EventSearchQuery query = new EventSearchQuery(ORG.getId(), ADMIN)
        .withStartDate("2021-01-01T00:00:00Z", EventQueryType.GREATER_THAN)
        .withStartFrom(FROM)
        .withStartTo(TO)
        .withStartDate("2025-01-01T00:00:00Z", EventQueryType.LESS_THAN);
    assertMatchesGolden("event-lifecycle-period", index.searchRequestBody(query));
  }

  @Test
  public void seriesAdminDefault() throws Exception {
    assertMatchesGolden("series-admin-default", index.searchRequestBody(new SeriesSearchQuery(ORG.getId(), ADMIN)));
  }

  @Test
  public void seriesUserPermissions() throws Exception {
    final SeriesSearchQuery query = new SeriesSearchQuery(ORG.getId(), USER)
        .withAction(Action.WRITE);
    assertMatchesGolden("series-user-permissions", index.searchRequestBody(query));
  }

  @Test
  public void seriesFilters() throws Exception {
    final SeriesSearchQuery query = new SeriesSearchQuery(ORG.getId(), ADMIN)
        .withIdentifier("series-1")
        .withTitle("Title")
        .withDescription("Description")
        .withSubject("Subject A")
        .withSubject("Subject B")
        .withLanguage("en")
        .withCreator("Creator")
        .withLicense("CC-BY")
        .withCreatedFrom(FROM)
        .withCreatedTo(TO)
        .withOrganizer("Organizer")
        .withContributor("Contributor")
        .withPublisher("Publisher")
        .withManagedAcl("Managed ACL")
        .withRightsHolder("Rights holder")
        .withTheme(7L)
        .withExtendedMetadata("dublincore/extended", "field", "value")
        .withAccessControlEntry("ROLE_X", Action.WRITE);
    query.withOffset(5);
    query.withLimit(15);
    assertMatchesGolden("series-filters", index.searchRequestBody(query));
  }

  @Test
  public void seriesTextAndSort() throws Exception {
    final SeriesSearchQuery query = new SeriesSearchQuery(ORG.getId(), USER)
        .sortByTitle(Order.Descending)
        .sortByCreatedDateTime(Order.Ascending)
        .sortByOrganizers(Order.Ascending);
    query.withText(false, "lecture");
    assertMatchesGolden("series-text-sort", index.searchRequestBody(query));
  }
}
