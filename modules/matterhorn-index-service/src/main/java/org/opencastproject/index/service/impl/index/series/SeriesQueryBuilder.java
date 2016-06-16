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

package org.opencastproject.index.service.impl.index.series;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.index.service.impl.index.event.EventIndexSchema;
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
 * Matterhorn {@link SeriesSearchQuery} implementation of the Elasticsearch query builder.
 */
public class SeriesQueryBuilder extends AbstractElasticsearchQueryBuilder<SeriesSearchQuery> {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SeriesQueryBuilder.class);

  /**
   * Creates a new elastic search query based on the series query.
   *
   * @param query
   *          the series query
   */
  public SeriesQueryBuilder(SeriesSearchQuery query) {
    super(query);
  }

  /**
   * @see org.opencastproject.matterhorn.search.impl.AbstractElasticsearchQueryBuilder#buildQuery(org.opencastproject.matterhorn.search.SearchQuery)
   */
  @Override
  public void buildQuery(SeriesSearchQuery query) {
    // Organization
    if (query.getOrganization() == null)
      throw new IllegalStateException("No organization set on the series search query!");

    and(SeriesIndexSchema.ORGANIZATION, query.getOrganization(), true);

    // Series identifier
    if (query.getIdentifier().length > 0) {
      and(SeriesIndexSchema.UID, query.getIdentifier(), true);
    }

    // Title
    if (query.getTitle() != null) {
      and(SeriesIndexSchema.TITLE, query.getTitle(), true);
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

    if (query.getDescription() != null) {
      and(SeriesIndexSchema.DESCRIPTION, query.getDescription(), true);
    }

    if (query.getSubjects().length > 0) {
      and(SeriesIndexSchema.SUBJECT, query.getSubjects(), true);
    }

    if (query.getLanguage() != null) {
      and(SeriesIndexSchema.LANGUAGE, query.getLanguage(), true);
    }

    if (query.getCreator() != null) {
      and(SeriesIndexSchema.CREATOR, query.getCreator(), true);
    }

    if (query.getLicense() != null) {
      and(SeriesIndexSchema.LICENSE, query.getLicense(), true);
    }

    if (query.getAccessPolicy() != null) {
      and(SeriesIndexSchema.ACCESS_POLICY, query.getAccessPolicy(), true);
    }

    if (query.getCreatedFrom() != null && query.getCreatedTo() != null) {
      and(SeriesIndexSchema.CREATED_DATE_TIME, query.getCreatedFrom(), query.getCreatedTo());
    }

    if (query.getOrganizers().length > 0) {
      and(SeriesIndexSchema.ORGANIZERS, query.getOrganizers(), true);
    }

    if (query.getContributors().length > 0) {
      and(SeriesIndexSchema.CONTRIBUTORS, query.getContributors(), true);
    }

    if (query.getPublishers().length > 0) {
      and(SeriesIndexSchema.PUBLISHERS, query.getPublishers(), true);
    }

    if (query.getManagedAcl() != null) {
      and(SeriesIndexSchema.MANAGED_ACL, query.getManagedAcl(), true);
    }

    if (query.getRightsHolder() != null) {
      and(SeriesIndexSchema.RIGHTS_HOLDER, query.getRightsHolder(), true);
    }

    if (query.getOptedOut() != null) {
      and(SeriesIndexSchema.OPT_OUT, query.getOptedOut(), true);
    }

    if (query.getTheme() != null) {
      and(SeriesIndexSchema.THEME, query.getTheme(), true);
    }

    if (query.getCreatedFrom() != null && query.getCreatedTo() != null) {
      and(SeriesIndexSchema.CREATED_DATE_TIME, query.getCreatedFrom(), query.getCreatedTo());
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
