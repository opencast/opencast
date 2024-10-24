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

package org.opencastproject.elasticsearch.index.objects.event;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.elasticsearch.api.SearchTerms;
import org.opencastproject.elasticsearch.api.SearchTerms.Quantifier;
import org.opencastproject.elasticsearch.impl.AbstractElasticsearchQueryBuilder;
import org.opencastproject.elasticsearch.impl.IndexSchema;
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

  @Override
  public void buildQuery(EventSearchQuery query) {
    // Organization
    if (query.getOrganization() == null) {
      throw new IllegalStateException("No organization set on the event search query!");
    }

    and(EventIndexSchema.ORGANIZATION, query.getOrganization());

    // Recording identifier
    if (query.getIdentifier().length > 0) {
      and(EventIndexSchema.UID, query.getIdentifier());
    }

    // Title
    if (query.getTitleValue() != null) {
      addToQuery(EventIndexSchema.TITLE,
          query.getTitle().getValue(), query.getTitle().getType(), query.getTitle().isMust());
    }

    // Description
    if (query.getDescriptionValue() != null) {
      addToQuery(EventIndexSchema.DESCRIPTION,
          query.getDescription().getValue(), query.getDescription().getType(), query.getDescription().isMust());
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
    if (query.getPresenterValues() != null) {
      for (EventSearchQueryField<String> presenter : query.getPresenters()) {
        addToQuery(EventIndexSchema.PRESENTER,
            presenter.getValue(), presenter.getType(), presenter.isMust());
      }
    }

    // Contributors
    if (query.getContributorValues() != null) {
      for (EventSearchQueryField<String> contributor : query.getContributors()) {
        addToQuery(EventIndexSchema.CONTRIBUTOR,
            contributor.getValue(), contributor.getType(), contributor.isMust());
      }
    }

    // Subject
    if (query.getSubjectValue() != null) {
      addToQuery(EventIndexSchema.SUBJECT,
          query.getSubject().getValue(), query.getSubject().getType(), query.getSubject().isMust());
    }

    // Location
    if (query.getLocationValue() != null) {
      addToQuery(EventIndexSchema.LOCATION,
          query.getLocation().getValue(), query.getLocation().getType(), query.getLocation().isMust());
    }

    // Series Id
    if (query.getSeriesIdValue() != null) {
      addToQuery(EventIndexSchema.SERIES_ID,
          query.getSeriesId().getValue(), query.getSeriesId().getType(), query.getSeriesId().isMust());
    }

    // Series Name
    if (query.getSeriesNameValue() != null) {
      addToQuery(EventIndexSchema.SERIES_NAME,
          query.getSeriesName().getValue(), query.getSeriesName().getType(), query.getSeriesName().isMust());
    }

    // Language
    if (query.getLanguageValue() != null) {
      addToQuery(EventIndexSchema.LANGUAGE,
          query.getLanguage().getValue(), query.getLanguage().getType(), query.getLanguage().isMust());
    }

    // Source
    if (query.getSourceValue() != null) {
      addToQuery(EventIndexSchema.SOURCE,
          query.getSource().getValue(), query.getSource().getType(), query.getSource().isMust());
    }

    // Created
    if (query.getCreatedValue() != null) {
      addToQuery(EventIndexSchema.CREATED,
          query.getCreated().getValue(), query.getCreated().getType(), query.getCreated().isMust());
    }

    // Creator
    if (query.getCreatorValue() != null) {
      addToQuery(EventIndexSchema.CREATOR,
          query.getCreator().getValue(), query.getCreator().getType(), query.getCreator().isMust());
    }

    // Publisher
    if (query.getPublisherValue() != null) {
      addToQuery(EventIndexSchema.PUBLISHER,
          query.getPublisher().getValue(), query.getPublisher().getType(), query.getPublisher().isMust());
    }

    // License
    if (query.getLicenseValue() != null) {
      addToQuery(EventIndexSchema.LICENSE,
          query.getLicense().getValue(), query.getLicense().getType(), query.getLicense().isMust());
    }

    // Rights
    if (query.getRightsValue() != null) {
      addToQuery(EventIndexSchema.RIGHTS,
          query.getRights().getValue(), query.getRights().getType(), query.getRights().isMust());
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
    if (query.getStartDateValues() != null) {
      for (EventSearchQueryField<String> date : query.getStartDate()) {
        addToQuery(EventIndexSchema.START_DATE,
            date.getValue(), date.getType(), date.isMust());
      }
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
    if (query.getComments() != null) {
      for (String comment : query.getComments()) {
        and(EventIndexSchema.COMMENTS, comment);
      }
    }

    // Publications
    if (query.getPublications() != null) {
      for (String publication : query.getPublications()) {
        and(EventIndexSchema.PUBLICATION, publication);
      }
    }

    // Is published
    if (query.getIsPublished() != null) {
      and(EventIndexSchema.IS_PUBLISHED, query.getIsPublished());
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
          if (queryText.length() > 0) {
            queryText.append(" ");
          }
          queryText.append(term);
        }
        if (query.isFuzzySearch()) {
          fuzzyText = queryText.toString();
        } else {
          this.text = queryText.toString();
        }
        if (Quantifier.All.equals(terms.getQuantifier())) {
          if (groups == null) {
            groups = new ArrayList<>();
          }
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

  private void addToQuery(String schema, Object value, EventQueryType type, boolean must) {
    switch (type) {
      case SEARCH -> addToQueryAndNot(must, schema, value);
      case WILDCARD -> addToQueryWildcardAndNot(must, schema, (String) value);
      case GREATER_THAN -> andDateGreaterThan(schema, (String) value);
      case LESS_THAN -> andDateLessThan(schema, (String) value);
      default -> throw new IllegalArgumentException("Unsupported event query type: " + type);
    }
  }

  private void addToQueryAndNot(boolean must, String schema, Object value) {
    if (must) {
      and(schema, value);
    } else {
      andNot(schema, value);
    }
  }

  private void addToQueryWildcardAndNot(boolean must, String schema, String value) {
    if (must) {
      andWildcard(schema, value);
    } else {
      andWildcardNot(schema, value);
    }
  }

}
