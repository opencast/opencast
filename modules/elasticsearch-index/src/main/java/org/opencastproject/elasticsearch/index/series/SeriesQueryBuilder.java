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

package org.opencastproject.elasticsearch.index.series;

import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.opencastproject.elasticsearch.api.SearchTerms;
import org.opencastproject.elasticsearch.api.SearchTerms.Quantifier;
import org.opencastproject.elasticsearch.impl.AbstractElasticsearchQueryBuilder;
import org.opencastproject.elasticsearch.impl.IndexSchema;
import org.opencastproject.elasticsearch.index.event.EventIndexSchema;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Opencast {@link SeriesSearchQuery} implementation of the Elasticsearch query builder.
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

  @Override
  public void buildQuery(SeriesSearchQuery query) {
    // Organization
    if (query.getOrganization() == null) {
      throw new IllegalStateException("No organization set on the series search query!");
    }

    and(SeriesIndexSchema.ORGANIZATION, query.getOrganization());

    // Series identifier
    if (query.getIdentifier().length > 0) {
      and(SeriesIndexSchema.UID, query.getIdentifier());
    }

    // Title
    if (query.getTitle() != null) {
      and(SeriesIndexSchema.TITLE, query.getTitle());
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

    if (query.getDescription() != null) {
      and(SeriesIndexSchema.DESCRIPTION, query.getDescription());
    }

    if (query.getSubjects().length > 0) {
      and(SeriesIndexSchema.SUBJECT, query.getSubjects());
    }

    if (query.getLanguage() != null) {
      and(SeriesIndexSchema.LANGUAGE, query.getLanguage());
    }

    if (query.getCreator() != null) {
      and(SeriesIndexSchema.CREATOR, query.getCreator());
    }

    if (query.getLicense() != null) {
      and(SeriesIndexSchema.LICENSE, query.getLicense());
    }

    if (query.getAccessPolicy() != null) {
      and(SeriesIndexSchema.ACCESS_POLICY, query.getAccessPolicy());
    }

    if (query.getCreatedFrom() != null && query.getCreatedTo() != null) {
      and(SeriesIndexSchema.CREATED_DATE_TIME, query.getCreatedFrom(), query.getCreatedTo());
    }

    if (query.getOrganizers().length > 0) {
      and(SeriesIndexSchema.ORGANIZERS, query.getOrganizers());
    }

    if (query.getContributors().length > 0) {
      and(SeriesIndexSchema.CONTRIBUTORS, query.getContributors());
    }

    if (query.getPublishers().length > 0) {
      and(SeriesIndexSchema.PUBLISHERS, query.getPublishers());
    }

    if (query.getManagedAcl() != null) {
      and(SeriesIndexSchema.MANAGED_ACL, query.getManagedAcl());
    }

    if (query.getRightsHolder() != null) {
      and(SeriesIndexSchema.RIGHTS_HOLDER, query.getRightsHolder());
    }

    if (query.getTheme() != null) {
      and(SeriesIndexSchema.THEME, query.getTheme());
    }

    if (query.getCreatedFrom() != null && query.getCreatedTo() != null) {
      and(SeriesIndexSchema.CREATED_DATE_TIME, query.getCreatedFrom(), query.getCreatedTo());
    }

    // Text
    if (query.getTerms() != null) {
      for (SearchTerms<String> terms : query.getTerms()) {
        StringBuffer queryText = new StringBuffer();
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
            groups = new ArrayList<ValueGroup>();
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

}
