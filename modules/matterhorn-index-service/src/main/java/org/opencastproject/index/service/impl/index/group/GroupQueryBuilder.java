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
package org.opencastproject.index.service.impl.index.group;

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
 * Matterhorn {@link GroupSearchQuery} implementation of the Elasticsearch query builder.
 */
public class GroupQueryBuilder extends AbstractElasticsearchQueryBuilder<GroupSearchQuery> {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(GroupQueryBuilder.class);

  /**
   * Creates a new elastic search query based on the group query.
   *
   * @param query
   *          the group query
   */
  public GroupQueryBuilder(GroupSearchQuery query) {
    super(query);
  }

  /**
   * @see org.opencastproject.matterhorn.search.impl.AbstractElasticsearchQueryBuilder#buildQuery(org.opencastproject.matterhorn.search.SearchQuery)
   */
  @Override
  public void buildQuery(GroupSearchQuery query) {
    // Organization
    if (query.getOrganization() == null)
      throw new IllegalStateException("No organization set on the group search query!");

    and(GroupIndexSchema.ORGANIZATION, query.getOrganization(), true);

    // group identifier
    if (query.getIdentifier().length > 0) {
      and(GroupIndexSchema.UID, query.getIdentifier(), true);
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

    if (query.getName() != null) {
      and(GroupIndexSchema.NAME, query.getName(), true);
    }

    if (query.getDescription() != null) {
      and(GroupIndexSchema.DESCRIPTION, query.getDescription(), true);
    }

    if (query.getRoles().length > 0) {
      and(GroupIndexSchema.ROLES, query.getRoles(), true);
    }

    if (query.getMembers().length > 0) {
      and(GroupIndexSchema.MEMBERS, query.getMembers(), true);
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
