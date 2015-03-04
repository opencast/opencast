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
 * Matterhorn {@link EventSearchQuery} implementation of the Elasticsearch query builder.
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

    and(EventIndexSchema.ORGANIZATION, query.getOrganization(), true);

    // Recording identifier
    if (query.getIdentifier().length > 0) {
      and(EventIndexSchema.UID, query.getIdentifier(), true);
    }

    // Title
    if (query.getTitle() != null) {
      and(EventIndexSchema.TITLE, query.getTitle(), true);
    }

    // Description
    if (query.getDescription() != null) {
      and(EventIndexSchema.DESCRIPTION, query.getDescription(), true);
    }

    // Action
    if (query.getActions() != null && query.getActions().length > 0) {
      User user = query.getUser();
      if (!user.hasRole(GLOBAL_ADMIN_ROLE) && !user.hasRole(user.getOrganization().getAdminRole())) {
        for (Role role : user.getRoles()) {
          for (String action : query.getActions()) {
            and(EventIndexSchema.ACL_PERMISSION_PREFIX.concat(action), role.getName(), true);
          }
        }
      }
    }

    // Presenter
    if (query.getPresenters() != null) {
      for (String presenter : query.getPresenters())
        and(EventIndexSchema.PRESENTER, presenter, true);
    }

    // Contributors
    if (query.getContributors().length > 0) {
      and(EventIndexSchema.CONTRIBUTOR, query.getContributors(), true);
    }

    // Subject
    if (query.getSubject() != null) {
      and(EventIndexSchema.SUBJECT, query.getSubject(), true);
    }

    // Location
    if (query.getLocation() != null) {
      and(EventIndexSchema.LOCATION, query.getLocation(), true);
    }

    // Series Id
    if (query.getSeriesId() != null) {
      and(EventIndexSchema.SERIES_ID, query.getSeriesId(), true);
    }

    // Series Name
    if (query.getSeriesName() != null) {
      and(EventIndexSchema.SERIES_NAME, query.getSeriesName(), true);
    }

    // Language
    if (query.getLanguage() != null) {
      and(EventIndexSchema.LANGUAGE, query.getLanguage(), true);
    }

    // Source
    if (query.getSource() != null) {
      and(EventIndexSchema.SOURCE, query.getSource(), true);
    }

    // Created
    if (query.getCreated() != null) {
      and(EventIndexSchema.CREATED, query.getCreated(), true);
    }

    // Creator
    if (query.getCreator() != null) {
      and(EventIndexSchema.CREATOR, query.getCreator(), true);
    }

    // License
    if (query.getLicense() != null) {
      and(EventIndexSchema.LICENSE, query.getLicense(), true);
    }

    // Rights
    if (query.getRights() != null) {
      and(EventIndexSchema.RIGHTS, query.getRights(), true);
    }

    // Track mime types
    if (query.getTrackMimetypes().length > 0) {
      and(EventIndexSchema.TRACK_MIMETYPE, query.getTrackMimetypes(), true);
    }

    // Track stream resolutions
    if (query.getTrackStreamResolution().length > 0) {
      and(EventIndexSchema.TRACK_STREAM_RESOLUTION, query.getTrackStreamResolution(), true);
    }

    // Track flavors
    if (query.getTrackFlavor().length > 0) {
      and(EventIndexSchema.TRACK_FLAVOR, query.getTrackFlavor(), true);
    }

    // Metadata flavors
    if (query.getMetadataFlavor().length > 0) {
      and(EventIndexSchema.METADATA_FLAVOR, query.getMetadataFlavor(), true);
    }

    // Metadata mime types
    if (query.getMetadataMimetype().length > 0) {
      and(EventIndexSchema.METADATA_MIMETYPE, query.getMetadataMimetype(), true);
    }

    // Attachment flavors
    if (query.getAttachmentFlavor().length > 0) {
      and(EventIndexSchema.ATTACHMENT_FLAVOR, query.getAttachmentFlavor(), true);
    }

    // Publication flavors
    if (query.getPublicationFlavor().length > 0) {
      and(EventIndexSchema.PUBLICATION_FLAVOR, query.getPublicationFlavor(), true);
    }

    // Access policy
    if (query.getAccessPolicy() != null) {
      and(EventIndexSchema.ACCESS_POLICY, query.getAccessPolicy(), true);
    }

    // Managed ACL
    if (query.getManagedAcl() != null) {
      and(EventIndexSchema.MANAGED_ACL, query.getManagedAcl(), true);
    }

    // Workflow state
    if (query.getWorkflowState() != null) {
      and(EventIndexSchema.WORKFLOW_STATE, query.getWorkflowState(), true);
    }

    // Workflow id
    if (query.getWorkflowId() != null) {
      and(EventIndexSchema.WORKFLOW_ID, query.getWorkflowId(), true);
    }

    // Workflow definition id
    if (query.getWorkflowDefinition() != null) {
      and(EventIndexSchema.WORKFLOW_DEFINITION_ID, query.getWorkflowDefinition(), true);
    }

    // Workflow scheduled date
    if (query.getWorkflowScheduledDate() != null) {
      and(EventIndexSchema.WORKFLOW_SCHEDULED_DATETIME, query.getWorkflowScheduledDate(), true);
    }

    // Review status
    if (query.getReviewStatus() != null) {
      and(EventIndexSchema.REVIEW_STATUS, query.getReviewStatus(), true);
    }

    // Scheduling status
    if (query.getSchedulingStatus() != null) {
      and(EventIndexSchema.SCHEDULING_STATUS, query.getSchedulingStatus(), true);
    }

    // Recording start date period
    if (query.getStartFrom() != null && query.getStartTo() != null) {
      and(EventIndexSchema.START_DATE, query.getStartFrom(), query.getStartTo());
    }

    // Recording start date
    if (query.getStartDate() != null) {
      and(EventIndexSchema.START_DATE, query.getStartDate(), true);
    }

    // Recording duration
    if (query.getDuration() != null) {
      and(EventIndexSchema.DURATION, query.getDuration(), true);
    }

    // Opt out
    if (query.getOptedOut() != null) {
      and(EventIndexSchema.OPTED_OUT, query.getOptedOut(), true);
    }

    // Review date
    if (query.getReviewDate() != null) {
      and(EventIndexSchema.REVIEW_DATE, query.getReviewDate(), true);
    }

    // Blacklisted
    if (query.getBlacklisted() != null) {
      and(EventIndexSchema.BLACKLISTED, query.getBlacklisted(), true);
    }

    // Has comments
    if (query.getHasComments() != null) {
      and(EventIndexSchema.HAS_COMMENTS, query.getHasComments(), true);
    }

    // Has open comments
    if (query.getHasOpenComments() != null) {
      and(EventIndexSchema.HAS_OPEN_COMMENTS, query.getHasOpenComments(), true);
    }

    // Publications
    if (query.getPublications() != null) {
      for (String publication : query.getPublications())
        and(EventIndexSchema.PUBLICATION, publication, true);
    }

    // Archive version
    if (query.getArchiveVersion() != null) {
      and(EventIndexSchema.ARCHIVE_VERSION, query.getArchiveVersion(), true);
    }

    // Text
    if (query.getTerms() != null) {
      for (SearchTerms<String> terms : query.getTerms()) {
        StringBuffer queryText = new StringBuffer();
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
            groups = new ArrayList<ValueGroup>();
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
