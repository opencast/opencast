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

package org.opencastproject.userdirectory;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.GroupProvider;
import org.opencastproject.security.api.JaxbGroupList;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.security.impl.jpa.JpaGroup;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaRole;
import org.opencastproject.userdirectory.api.AAIRoleProvider;
import org.opencastproject.userdirectory.api.GroupRoleProvider;
import org.opencastproject.userdirectory.utils.UserDirectoryUtils;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.requests.SortCriterion;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.EntityManagerFactory;

/**
 * Manages and locates users using JPA.
 */
@Component(
    property = {
        "service.description=Provides a group role directory"
    },
    immediate = true,
    service = { RoleProvider.class, JpaGroupRoleProvider.class }
)
public class JpaGroupRoleProvider implements AAIRoleProvider, GroupProvider, GroupRoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JpaGroupRoleProvider.class);

  /** The JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.common";

  /** The security service */
  protected SecurityService securityService = null;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The component context */
  private ComponentContext cc;

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.common)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /**
   * Sets the user directory service
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param organizationDirectoryService
   *          the organizationDirectoryService to set
   */
  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /**
   * Callback for activation of this component.
   *
   * @param cc
   *          the component context
   */
  @Activate
  public void activate(ComponentContext cc) {
    logger.debug("Activate group role provider");
    this.cc = cc;
    db = dbSessionFactory.createSession(emf);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.userdirectory.api.AAIRoleProvider#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    String orgId = securityService.getOrganization().getId();
    List<JpaGroup> roles = db.exec(UserDirectoryPersistenceUtil.findGroupsQuery(orgId, 0, 0));
    return getGroupsRoles(roles).iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(String userName) {
    String orgId = securityService.getOrganization().getId();
    List<JpaGroup> roles = db.exec(UserDirectoryPersistenceUtil.findGroupsByUserQuery(userName, orgId));
    return getGroupsRoles(roles);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForGroup(String groupName) {
    List<Role> roles = new ArrayList<>();
    String orgId = securityService.getOrganization().getId();
    Optional<JpaGroup> group = db.exec(UserDirectoryPersistenceUtil.findGroupByRoleQuery(groupName, orgId));
    if (group.isPresent()) {
      for (Role role : group.get().getRoles()) {
        roles.add(new JaxbRole(role.getName(), role.getOrganizationId(), role.getDescription(), Role.Type.DERIVED));
      }
    } else {
      logger.warn("Group {} not found", groupName);
    }
    return roles;
  }


  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return UserProvider.ALL_ORGANIZATIONS;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, Role.Target, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, Role.Target target, int offset, int limit) {
    if (query == null) {
      throw new IllegalArgumentException("Query must be set");
    }
    String orgId = securityService.getOrganization().getId();

    //  Here we want to return only the ROLE_GROUP_ names, not the roles associated with a group
    List<JpaGroup> groups = db.exec(UserDirectoryPersistenceUtil.findGroupsQuery(orgId, 0, 0));

    List<Role> roles = new ArrayList<>();
    for (JpaGroup group : groups) {
      if (like(group.getRole(), query)) {
        roles.add(new JaxbRole(
            group.getRole(),
            JaxbOrganization.fromOrganization(group.getOrganization()),
            "",
            Role.Type.GROUP
        ));
      }
    }

    Set<Role> result = new HashSet<>();
    int i = 0;
    for (Role entry : roles) {
      if (limit != 0 && result.size() >= limit) {
        break;
      }
      if (i >= offset) {
        result.add(entry);
      }
      i++;
    }
    return result.iterator();
  }

  /**
   * Updates a user's group membership
   *
   * @param userName
   *          the username
   * @param orgId
   *          the user's organization
   * @param roleList
   *          the list of group role names
   */
  public void updateGroupMembershipFromRoles(String userName, String orgId, List<String> roleList) {
    updateGroupMembershipFromRoles(userName, orgId, roleList, "");
  }

  /**
   * Updates a user's group membership
   *
   * @param userName
   *          the username
   * @param orgId
              the user's organization
   * @param roleList
   *          the list of group role names
   * @param prefix
   *          handle only roles with given prefix
   */
  public void updateGroupMembershipFromRoles(String userName, String orgId, List<String> roleList, String prefix) {
    logger.debug("updateGroupMembershipFromRoles({}, size={})", userName, roleList.size());

    // Add the user to all groups which are in the roleList, but allow the user to be part of groups
    // without having the group role

    Set<String> membershipRoles = new HashSet<>();

    // List of the user's groups
    List<JpaGroup> membership = db.exec(UserDirectoryPersistenceUtil.findGroupsByUserQuery(userName, orgId));
    for (JpaGroup group : membership) {
      if (StringUtils.isNotBlank(prefix) && !group.getRole().startsWith(prefix)) {
        //ignore groups of other providers
        continue;
      }
      if (roleList.contains(group.getRole())) {
        // record this membership
        membershipRoles.add(group.getRole());
      }
    }

    // Now add the user to any groups that they are not already a member of
    for (String rolename : roleList) {
      if (!membershipRoles.contains(rolename)) {
        Optional<JpaGroup> group = db.exec(UserDirectoryPersistenceUtil.findGroupByRoleQuery(rolename, orgId));
        if (group.isPresent()) {
          try {
            logger.debug("Adding user {} to group {}", userName, rolename);
            group.get().getMembers().add(userName);
            addGroup(group.get());
          } catch (UnauthorizedException e) {
            logger.warn("Unauthorized to add user {} to group {}", userName, group.get().getRole(), e);
          }
        } else {
          logger.warn("Cannot add user {} to group {} - no group found with that role", userName, rolename);
        }
      }
    }
  }

  /**
   * Removes a user from all groups
   *
   * @param userName
   *          the username
   * @param orgId
   *          the user's organization
   *
   */
  public void removeMemberFromAllGroups(String userName, String orgId) {
    // List of the user's groups
    List<JpaGroup> membership = db.exec(UserDirectoryPersistenceUtil.findGroupsByUserQuery(userName, orgId));
    for (JpaGroup group : membership) {
      try {
        logger.debug("Removing user {} from group {}", userName, group.getRole());
        group.getMembers().remove(userName);
        addGroup(group);
      } catch (UnauthorizedException e) {
        logger.warn("Unauthorized to add or remove user {} from group {}", userName, group.getRole(), e);
      }
    }
  }

  /**
   * Loads a group from persistence
   *
   * @param groupId
   *          the group id
   * @param orgId
   *          the organization id
   * @return the loaded group or <code>null</code> if not found
   */
  public JpaGroup loadGroup(String groupId, String orgId) {
    return db.exec(UserDirectoryPersistenceUtil.findGroupQuery(groupId, orgId))
        .orElse(null);
  }

  /**
   * Get group.
   *
   * @param groupId
   *
   * @return the group
   */
  public JpaGroup getGroup(String groupId) {
    String orgId = securityService.getOrganization().getId();
    return loadGroup(groupId, orgId);
  }

  /**
   * Adds or updates a group to the persistence.
   *
   * @param group
   *          the group to add
   */
  @Override
  public void addGroup(final JpaGroup group) throws UnauthorizedException {
    if (group != null && !UserDirectoryUtils.isCurrentUserAuthorizedHandleRoles(securityService, group.getRoles())) {
      throw new UnauthorizedException("The user is not allowed to add or update a group with the admin role");
    }

    Group existingGroup = loadGroup(group.getGroupId(), group.getOrganization().getId());
    if (existingGroup != null
        && !UserDirectoryUtils.isCurrentUserAuthorizedHandleRoles(securityService, existingGroup.getRoles())) {
      throw new UnauthorizedException("The user is not allowed to update a group with the admin role");
    }

    db.execTx(em -> {
      Set<JpaRole> roles = UserDirectoryPersistenceUtil.saveRolesQuery(group.getRoles()).apply(em);
      JpaOrganization organization = UserDirectoryPersistenceUtil.saveOrganizationQuery(group.getOrganization())
          .apply(em);

      JpaGroup jpaGroup = new JpaGroup(group.getGroupId(), organization, group.getName(), group.getDescription(), roles,
          group.getMembers());

      // Then save the jpaGroup
      Optional<JpaGroup> foundGroup = UserDirectoryPersistenceUtil.findGroupQuery(jpaGroup.getGroupId(),
          jpaGroup.getOrganization().getId()).apply(em);
      if (foundGroup.isEmpty()) {
        em.persist(jpaGroup);
      } else {
        foundGroup.get().setName(jpaGroup.getName());
        foundGroup.get().setDescription(jpaGroup.getDescription());
        foundGroup.get().setMembers(jpaGroup.getMembers());
        foundGroup.get().setRoles(roles);
        em.merge(foundGroup.get());
      }
    });
  }

  private void removeGroup(String groupId, String orgId) throws NotFoundException, UnauthorizedException {
    Group group = loadGroup(groupId, orgId);
    if (group != null && !UserDirectoryUtils.isCurrentUserAuthorizedHandleRoles(securityService, group.getRoles())) {
      throw new UnauthorizedException("The user is not allowed to delete a group with the admin role");
    }

    db.execTxChecked(UserDirectoryPersistenceUtil.removeGroupQuery(groupId, orgId));
  }

  /**
   * Returns all roles from a given group list
   *
   * @param groups
   *          the group list
   * @return the role list
   */
  private List<Role> getGroupsRoles(List<JpaGroup> groups) {
    List<Role> roles = new ArrayList<>();
    for (Group group : groups) {
      roles.add(new JaxbRole(
          group.getRole(),
          JaxbOrganization.fromOrganization(group.getOrganization()),
          "",
          Role.Type.GROUP
      ));
      for (Role role : group.getRoles()) {
        roles.add(new JaxbRole(role.getName(), role.getOrganizationId(), role.getDescription(), Role.Type.DERIVED));
      }
    }
    return roles;
  }

  public Iterator<Group> getGroups() {
    String orgId = securityService.getOrganization().getId();
    return new ArrayList<Group>(db.exec(UserDirectoryPersistenceUtil.findGroupsQuery(orgId, 0, 0)))
        .iterator();
  }

  private boolean like(final String str, final String expr) {
    if (str == null) {
      return false;
    }
    String regex = expr.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(str).matches();
  }

  /**
   * Returns a XML representation of the list of groups available the current user's organization.
   *
   * @param limit
   *          the int amount to limit the results
   * @param offset
   *          the offset to start this result set at
   * @return the JaxbGroupList of results
   * @throws IOException
   *           if unexpected IO exception occurs
   */
  public JaxbGroupList getGroups(int limit, int offset) throws IOException {
    if (limit < 1) {
      limit = 100;
    }
    String orgId = securityService.getOrganization().getId();
    JaxbGroupList groupList = new JaxbGroupList();
    List<JpaGroup> groups = db.exec(UserDirectoryPersistenceUtil.findGroupsQuery(orgId, limit, offset));
    for (JpaGroup group : groups) {
      groupList.add(group);
    }
    return groupList;
  }

  /**
   * Get groups by the defined filter and sorting criteria.
   *
   * @param limit
   *          how many groups to get (optional)
   * @param offset
   *          where to start the list for pagination (optional)
   * @param nameFilter
   *          filter by group name (optional)
   * @param textFilter
   *          fulltext filter (optional)
   * @param sortCriteria
   *          the sorting criteria
   *
   * @return a list of groups
   */
  public List<JpaGroup> getGroups(Optional<Integer> limit, Optional<Integer> offset, Optional<String> nameFilter,
          Optional<String> textFilter, Set<SortCriterion> sortCriteria) {
    String orgId = securityService.getOrganization().getId();
    return db.exec(UserDirectoryPersistenceUtil.findGroupsQuery(orgId, limit, offset, nameFilter, textFilter,
        sortCriteria));
  }

  /**
   * Count groups that fit the filter criteria in total.
   *
   * @param nameFilter
   *          filter by group name (optional)
   * @param textFilter
   *          fulltext filter (optional)
   *
   * @return a list of groups
   */
  public long countTotalGroups(Optional<String> nameFilter, Optional<String> textFilter) {
    String orgId = securityService.getOrganization().getId();
    return db.exec(UserDirectoryPersistenceUtil.countTotalGroupsQuery(orgId, nameFilter, textFilter));
  }

  /**
   * Remove a group by id
   *
   * @param groupId
   *          the id of the group to remove
   * @throws Exception
   *           unexpected error occurred
   * @throws UnauthorizedException
   *           user is not authorized to remove this group
   * @throws NotFoundException
   *           the group was not found
   */
  public void removeGroup(String groupId) throws NotFoundException, UnauthorizedException, Exception {
    String orgId = securityService.getOrganization().getId();
    removeGroup(groupId, orgId);
  }

  /**
   * Create a new group
   *
   * @param name
   *          the name of the group
   * @param description
   *          a description of the group
   * @param roles
   *          the roles of the group
   * @param users
   *          the users in the group
   * @throws IllegalArgumentException
   *           if missing or bad parameters
   * @throws UnauthorizedException
   *           if user does not have rights to create group
   * @throws ConflictException
   *           if group already exists
   */
  public void createGroup(String name, String description, String roles, String users)
          throws IllegalArgumentException, UnauthorizedException, ConflictException {
    JpaOrganization organization = (JpaOrganization) securityService.getOrganization();

    HashSet<JpaRole> roleSet = new HashSet<>();
    if (roles != null) {
      for (String role : StringUtils.split(roles, ",")) {
        roleSet.add(new JpaRole(StringUtils.trim(role), organization));
      }
    }

    HashSet<String> members = new HashSet<>();
    if (users != null) {
      for (String member : StringUtils.split(users, ",")) {
        members.add(StringUtils.trim(member));
      }
    }

    final String groupId = name.toLowerCase().replaceAll("\\W", "_");

    Optional<JpaGroup> existingGroup = db.exec(UserDirectoryPersistenceUtil.findGroupQuery(groupId,
        organization.getId()));
    if (existingGroup.isPresent()) {
      throw new ConflictException("group already exists");
    }

    addGroup(new JpaGroup(groupId, organization, name, description, roleSet, members));
  }

  /**
   * Remove member from group.
   *
   * @param groupId
   * @param member
   *
   * @return true if we updated the group, false otherwise
   *
   * @throws NotFoundException
   * @throws UnauthorizedException
   */
  public boolean removeMemberFromGroup(String groupId, String member) throws NotFoundException, UnauthorizedException {
    JpaGroup group = getGroup(groupId);
    if (group == null) {
      throw new NotFoundException();
    }
    Set<String> members = group.getMembers();
    if (!members.contains(member)) {
      return false; // nothing to do here
    }
    group.removeMember(member);
    userDirectoryService.invalidate(member);

    addGroup(group);
    return true;
  }

  /**
   * Add member to group.
   *
   * @param groupId
   * @param member
   *
   * @return true if we updated the group, false otherwise
   *
   * @throws NotFoundException
   * @throws UnauthorizedException
   */
  public boolean addMemberToGroup(String groupId, String member) throws NotFoundException, UnauthorizedException {
    JpaGroup group = getGroup(groupId);
    if (group == null) {
      throw new NotFoundException();
    }
    Set<String> members = group.getMembers();
    if (members.contains(member)) {
      return false; // nothing to do here
    }
    group.addMember(member);
    userDirectoryService.invalidate(member);

    addGroup(group);
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.userdirectory.api.GroupRoleProvider#updateGroup(String, String, String, String, String)
   */
  @Override
  public void updateGroup(String groupId, String name, String description, String roles, String users)
          throws NotFoundException, UnauthorizedException {
    JpaOrganization organization = (JpaOrganization) securityService.getOrganization();

    Optional<JpaGroup> groupOpt = db.exec(UserDirectoryPersistenceUtil.findGroupQuery(groupId, organization.getId()));
    if (groupOpt.isEmpty()) {
      throw new NotFoundException();
    }
    JpaGroup group = groupOpt.get();

    if (StringUtils.isNotBlank(name)) {
      group.setName(StringUtils.trim(name));
    }

    if (StringUtils.isNotBlank(description)) {
      group.setDescription(StringUtils.trim(description));
    }

    if (StringUtils.isNotBlank(roles)) {
      HashSet<JpaRole> roleSet = new HashSet<>();
      for (String role : StringUtils.split(roles, ",")) {
        roleSet.add(new JpaRole(StringUtils.trim(role), organization));
      }
      group.setRoles(roleSet);
    } else {
      group.setRoles(new HashSet<>());
    }

    if (users != null) {
      HashSet<String> members = new HashSet<>();
      HashSet<String> invalidateUsers = new HashSet<>();

      Set<String> groupMembers = group.getMembers();

      for (String member : StringUtils.split(users, ",")) {
        String newMember = StringUtils.trim(member);
        members.add(newMember);
        if (!groupMembers.contains(newMember)) {
          invalidateUsers.add(newMember);
        }
      }

      for (String member : groupMembers) {
        if (!members.contains(member)) {
          invalidateUsers.add(member);
        }
      }

      group.setMembers(members);

      // Invalidate cache entries for users who have been added or removed
      for (String member : invalidateUsers) {
        userDirectoryService.invalidate(member);
      }
    }
    addGroup(group);
  }
}
