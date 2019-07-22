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

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.matterhorn.search.SearchTerms;
import org.opencastproject.matterhorn.search.SearchTerms.Quantifier;
import org.opencastproject.matterhorn.search.impl.AbstractElasticsearchQueryBuilder;
import org.opencastproject.matterhorn.search.impl.IndexSchema;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Opencast {@link EventSearchQuery} implementation of the Elasticsearch query builder.
 */
public class EventQueryBuilder extends AbstractElasticsearchQueryBuilder<EventSearchQuery> {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(EventQueryBuilder.class);

  /**
   * Creates a new elastic search query based on the events query.
   *
   * @param query
   *          the events query
   */
  public EventQueryBuilder(EventSearchQuery query) {
    super(query);
  }

  /**
   * @see org.opencastproject.matterhorn.search.impl.AbstractElasticsearchQueryBuilder#buildQuery(org.opencastproject.matterhorn.search.SearchQuery)
   */
  @Override
  public void buildQuery(EventSearchQuery query) {
    // Organization
    if (query.getOrganization() == null)
      throw new IllegalStateException("No organization set on the event search query!");

    and(EventIndexSchema.ORGANIZATION, query.getOrganization());

    // Recording identifier
    if (query.getIdentifier().length > 0) {
      and(EventIndexSchema.UID, query.getIdentifier());
    }

    // Title
    if (query.getTitle() != null) {
      and(EventIndexSchema.TITLE, query.getTitle());
    }

    // Description
    if (query.getDescription() != null) {
      and(EventIndexSchema.DESCRIPTION, query.getDescription());
    }

    // Action
    if (query.getActions() != null && query.getActions().length > 0) {
      User user = query.getUser();
      if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(user.getOrganization().getAdminRole())) {
        for (Role role : user.getRoles()) {
          for (String action : query.getActions()) {
            and(EventIndexSchema.ACL_PERMISSION_PREFIX.concat(action), role.getName());
          }
        }
      }
    }

    // Presenter
    if (query.getPresenters() != null) {
      for (String presenter : query.getPresenters())
        and(EventIndexSchema.PRESENTER, presenter);
    }

    // Contributors
    if (query.getContributors().length > 0) {
      and(EventIndexSchema.CONTRIBUTOR, query.getContributors());
    }

    // Subject
    if (query.getSubject() != null) {
      and(EventIndexSchema.SUBJECT, query.getSubject());
    }

    // Location
    if (query.getLocation() != null) {
      and(EventIndexSchema.LOCATION, query.getLocation());
    }

    // Series Id
    if (query.getSeriesId() != null) {
      and(EventIndexSchema.SERIES_ID, query.getSeriesId());
    }

    // Series Name
    if (query.getSeriesName() != null) {
      and(EventIndexSchema.SERIES_NAME, query.getSeriesName());
    }

    // Language
    if (query.getLanguage() != null) {
      and(EventIndexSchema.LANGUAGE, query.getLanguage());
    }

    // Source
    if (query.getSource() != null) {
      and(EventIndexSchema.SOURCE, query.getSource());
    }

    // Created
    if (query.getCreated() != null) {
      and(EventIndexSchema.CREATED, query.getCreated());
    }

    // Creator
    if (query.getCreator() != null) {
      and(EventIndexSchema.CREATOR, query.getCreator());
    }

    // Publisher
    if (query.getPublisher() != null) {
      and(EventIndexSchema.PUBLISHER, query.getPublisher());
    }

    // License
    if (query.getLicense() != null) {
      and(EventIndexSchema.LICENSE, query.getLicense());
    }

    // Rights
    if (query.getRights() != null) {
      and(EventIndexSchema.RIGHTS, query.getRights());
    }

    // Access policy
    if (query.getAccessPolicy() != null) {
      and(EventIndexSchema.ACCESS_POLICY, query.getAccessPolicy());
    }

    // Managed ACL
    if (query.getManagedAcl() != null) {
      and(EventIndexSchema.MANAGED_ACL, query.getManagedAcl());
    }

    // Workflow state
    if (query.getWorkflowState() != null) {
      and(EventIndexSchema.WORKFLOW_STATE, query.getWorkflowState());
    }

    // Workflow id
    if (query.getWorkflowId() != null) {
      and(EventIndexSchema.WORKFLOW_ID, query.getWorkflowId());
    }

    // Workflow definition id
    if (query.getWorkflowDefinition() != null) {
      and(EventIndexSchema.WORKFLOW_DEFINITION_ID, query.getWorkflowDefinition());
    }

    // Event status
    if (query.getEventStatus() != null) {
      and(EventIndexSchema.EVENT_STATUS, query.getEventStatus());
    }

    // Recording start date period
    if (query.getStartFrom() != null && query.getStartTo() != null) {
      and(EventIndexSchema.START_DATE, query.getStartFrom(), query.getStartTo());
    }

    // Recording start date
    if (query.getStartDate() != null) {
      and(EventIndexSchema.START_DATE, query.getStartDate());
    }

    // Recording duration
    if (query.getDuration() != null) {
      and(EventIndexSchema.DURATION, query.getDuration());
    }

    // Has comments
    if (query.getHasComments() != null) {
      and(EventIndexSchema.HAS_COMMENTS, query.getHasComments());
    }

    // Has open comments
    if (query.getHasOpenComments() != null) {
      and(EventIndexSchema.HAS_OPEN_COMMENTS, query.getHasOpenComments());
    }

    // Publications
    if (query.getPublications() != null) {
      for (String publication : query.getPublications())
        and(EventIndexSchema.PUBLICATION, publication);
    }

    // Archive version
    if (query.getArchiveVersion() != null) {
      and(EventIndexSchema.ARCHIVE_VERSION, query.getArchiveVersion());
    }

    // Technical agent identifier
    if (query.getAgentId() != null) {
      and(EventIndexSchema.AGENT_ID, query.getAgentId());
    }

    // Technical start date period
    if (query.getTechnicalStartFrom() != null && query.getTechnicalStartTo() != null) {
      and(EventIndexSchema.TECHNICAL_START, query.getTechnicalStartFrom(), query.getTechnicalStartTo());
    }

    // Technical start date
    if (query.getTechnicalStartTime() != null) {
      and(EventIndexSchema.TECHNICAL_START, query.getTechnicalStartTime());
    }

    // Technical end date
    if (query.getTechnicalEndTime() != null) {
      and(EventIndexSchema.TECHNICAL_END, query.getTechnicalEndTime());
    }

    // Technical presenters
    if (query.getTechnicalPresenters().length > 0) {
      and(EventIndexSchema.TECHNICAL_PRESENTERS, query.getTechnicalPresenters());
    }

    // Text
    if (query.getTerms() != null) {
      for (SearchTerms<String> terms : query.getTerms()) {
        StringBuilder queryText = new StringBuilder();
        for (String term : terms.getTerms()) {
          if (queryText.length() > 0)
            queryText.append(" ");
          queryText.append(term);
        }
        if (query.isFuzzySearch())
          fuzzyText = queryText.toString();
        else
          this.text = queryText.toString();
        if (Quantifier.All.equals(terms.getQuantifier())) {
          if (groups == null)
            groups = new ArrayList<>();
          if (query.isFuzzySearch()) {
            logger.warn("All quantifier not supported in conjunction with wildcard text");
          }
          groups.add(new ValueGroup(IndexSchema.TEXT, (Object[]) terms.getTerms().toArray(new String[terms.size()])));
        }
      }
    }

    // Filter query
    if (query.getFilter() != null) {
      this.filter = query.getFilter();
    }

  }

}
