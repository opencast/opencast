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

import org.opencastproject.matterhorn.search.SearchTerms;
import org.opencastproject.matterhorn.search.impl.AbstractSearchQuery;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.Permissions.Action;
import org.opencastproject.security.api.User;
import org.opencastproject.userdirectory.JpaRole;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This interface defines a fluent api for a query object used to lookup groups in the search index.
 */
public class GroupSearchQuery extends AbstractSearchQuery {

  protected List<String> identifiers = new ArrayList<String>();
  private User user = null;
  private String name = null;
  private String description = null;
  private Set<String> actions = new HashSet<String>();
  private String organization = null;
  private List<JpaRole> roles = new ArrayList<JpaRole>();
  private List<String> members = new ArrayList<String>();
  private boolean editOnly = false;

  @SuppressWarnings("unused")
  private GroupSearchQuery() {
  }

  /**
   * Creates a query that will return group documents.
   */
  public GroupSearchQuery(String organization, User user) {
    super(Group.DOCUMENT_TYPE);
    this.organization = organization;
    this.user = user;
    this.actions.add(Permissions.Action.READ.toString());
    if (!user.getOrganization().getId().equals(organization))
      throw new IllegalStateException("User's organization must match search organization");
  }

  /**
   * Selects group with the given identifier.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple groups.
   *
   * @param id
   *          the group identifier
   * @return the enhanced search query
   */
  public GroupSearchQuery withIdentifier(String id) {
    if (StringUtils.isBlank(id))
      throw new IllegalArgumentException("Identifier cannot be null");
    this.identifiers.add(id);
    return this;
  }

  /**
   * Returns the list of group identifiers or an empty array if no identifiers have been specified.
   *
   * @return the identifiers
   */
  public String[] getIdentifier() {
    return identifiers.toArray(new String[identifiers.size()]);
  }

  /**
   * Selects groups with the given name.
   *
   * @param name
   *          the name
   * @return the enhanced search query
   */
  public GroupSearchQuery withName(String name) {
    clearExpectations();
    this.name = name;
    return this;
  }

  /**
   * Returns the name of the group.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Filter the group without any action checked.
   *
   * @return the enhanced search query
   */
  public GroupSearchQuery withoutActions() {
    clearExpectations();
    this.actions.clear();
    return this;
  }

  /**
   * Filter the groups with the given action.
   * <p>
   * Note that this method may be called multiple times to support filtering by multiple actions.
   *
   * @param action
   *          the action
   * @return the enhanced search query
   */
  public GroupSearchQuery withAction(Action action) {
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
   * Selects group with the given description.
   *
   * @param description
   *          the description
   * @return the enhanced search query
   */
  public GroupSearchQuery withDescription(String description) {
    clearExpectations();
    this.description = description;
    return this;
  }

  /**
   * Returns the description of the group.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the organization of the group.
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
   * Selects groups with the given role.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple roles.
   *
   * @param role
   *          the role
   * @return the enhanced search query
   */
  public GroupSearchQuery withRole(JpaRole role) {
    if (role == null)
      throw new IllegalArgumentException("Role cannot be null");
    clearExpectations();
    this.roles.add(role);
    return this;
  }

  /**
   * Returns the list of group's roles or an empty array if no roles have been specified.
   *
   * @return the roles
   */
  public String[] getRoles() {
    return roles.toArray(new String[roles.size()]);
  }

  /**
   * Selects groups with the given member.
   * <p>
   * Note that this method may be called multiple times to support selection of multiple members.
   *
   * @param member
   *          the member
   * @return the enhanced search query
   */
  public GroupSearchQuery withMember(String member) {
    if (StringUtils.isBlank(member)) {
      throw new IllegalArgumentException("Contributor can't be null");
    }
    clearExpectations();
    this.members.add(member);
    return this;
  }

  /**
   * Returns the list of group members or an empty array if no member have been specified.
   *
   * @return the members
   */
  public String[] getMembers() {
    return members.toArray(new String[members.size()]);
  }

  /**
   * @param edit
   *          True to only get groups with edit permissions
   * @return enhanced search query
   */
  public GroupSearchQuery withEdit(Boolean edit) {
    this.editOnly = edit;
    return this;
  }

  /**
   * @return True to only get groups that this user can edit.
   */
  public boolean isEditOnly() {
    return editOnly;
  }

  /**
   * Defines the sort order for the groups by members.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public GroupSearchQuery sortByMembers(Order order) {
    withSortOrder(GroupIndexSchema.MEMBERS, order);
    return this;
  }

  /**
   * Returns the sort order for the groups' members.
   *
   * @return the sort order
   */
  public Order getGroupMembersSortOrder() {
    return getSortOrder(GroupIndexSchema.MEMBERS);
  }

  /**
   * Defines the sort order for the groups by roles.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public GroupSearchQuery sortByRoles(Order order) {
    withSortOrder(GroupIndexSchema.ROLES, order);
    return this;
  }

  /**
   * Returns the sort order for the groups roles.
   *
   * @return the sort order
   */
  public Order getGroupRolesSortOrder() {
    return getSortOrder(GroupIndexSchema.ROLES);
  }

  /**
   * Defines the sort order for the group by name.
   *
   * @param order
   *          the order
   * @return the enhanced search query
   */
  public GroupSearchQuery sortByName(Order order) {
    withSortOrder(GroupIndexSchema.NAME, order);
    return this;
  }

  /**
   * Returns the sort order for the group name.
   *
   * @return the sort order
   */
  public Order getGroupNameSortOrder() {
    return getSortOrder(GroupIndexSchema.NAME);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(GroupSearchQuery.class.getSimpleName() + " ");
    if (identifiers.size() > 0) {
      sb.append("ids:'" + identifiers.toString() + "' ");
    }
    if (StringUtils.trimToNull(name) != null) {
      sb.append("name:'" + name + "' ");
    }
    if (StringUtils.trimToNull(description) != null) {
      sb.append("description:'" + description + "' ");
    }
    if (StringUtils.trimToNull(organization) != null) {
      sb.append("organization:'" + organization + "' ");
    }
    if (roles.size() > 0) {
      sb.append("roles:'" + roles.toString() + "' ");
    }

    if (members.size() > 0) {
      sb.append("members:'" + members.toString() + "' ");
    }

    sb.append("Edit:'" + editOnly + "' ");

    if (getTerms().size() > 0) {
      sb.append("Text:");
      for (SearchTerms<String> searchTerm : getTerms()) {
        sb.append("'" + searchTerm.getTerms() + "' ");
      }
    }

    return sb.toString();

  }
}
