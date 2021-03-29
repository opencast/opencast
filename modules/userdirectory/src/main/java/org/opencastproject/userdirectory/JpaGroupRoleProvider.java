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

package org.opencastproject.userdirectory;

import org.opencastproject.index.rebuild.AbstractIndexProducer;
import org.opencastproject.index.rebuild.IndexProducer;
import org.opencastproject.index.rebuild.IndexRebuildService;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.group.GroupItem;
import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.GroupProvider;
import org.opencastproject.security.api.JaxbGroup;
import org.opencastproject.security.api.JaxbGroupList;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Organization;
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
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.userdirectory.api.AAIRoleProvider;
import org.opencastproject.userdirectory.api.GroupRoleProvider;
import org.opencastproject.userdirectory.utils.UserDirectoryUtils;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
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
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

/**
 * Manages and locates users using JPA.
 */
@Component(
  property = {
    "service.description=Provides a group role directory"
  },
  immediate = true,
  service = { RoleProvider.class, JpaGroupRoleProvider.class, IndexProducer.class }
)
public class JpaGroupRoleProvider extends AbstractIndexProducer
        implements AAIRoleProvider, GroupProvider, GroupRoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JpaGroupRoleProvider.class);

  /** The JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.common";

  /** The message broker service */
  protected MessageSender messageSender;

  /** The message broker receiver */
  protected MessageReceiver messageReceiver;

  /** The security service */
  protected SecurityService securityService = null;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The component context */
  private ComponentContext cc;

  /** OSGi DI */
  @Reference(name = "entityManagerFactory", target = "(osgi.unit.name=org.opencastproject.common)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * Sets the user directory service
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  @Reference(name = "userDirectoryService")
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * @param messageSender
   *          The messageSender to set
   */
  @Reference(name = "message-broker-sender")
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  /**
   * @param messageReceiver
   *          The messageReceiver to set
   */
  @Reference(name = "message-broker-receiver")
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  @Reference(name = "security-service")
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param organizationDirectoryService
   *          the organizationDirectoryService to set
   */
  @Reference(name = "organization-directory-service")
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
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.userdirectory.api.AAIRoleProvider#getRoles()
   */
  @Override
  public Iterator<Role> getRoles() {
    String orgId = securityService.getOrganization().getId();
    return getGroupsRoles(UserDirectoryPersistenceUtil.findGroups(orgId, 0, 0, emf)).iterator();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getRolesForUser(String)
   */
  @Override
  public List<Role> getRolesForUser(String userName) {
    String orgId = securityService.getOrganization().getId();
    return getGroupsRoles(UserDirectoryPersistenceUtil.findGroupsByUser(userName, orgId, emf));
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
    Group group = UserDirectoryPersistenceUtil.findGroupByRole(groupName, orgId, emf);
    if (group != null) {
      for (Role role : group.getRoles()) {
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
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    String orgId = securityService.getOrganization().getId();

    //  Here we want to return only the ROLE_GROUP_ names, not the roles associated with a group
    List<JpaGroup> groups = UserDirectoryPersistenceUtil.findGroups(orgId, 0, 0, emf);

    List<Role> roles = new ArrayList<Role>();
    for (JpaGroup group : groups) {
      if (like(group.getRole(), query))
        roles.add(new JaxbRole(group.getRole(), JaxbOrganization.fromOrganization(group.getOrganization()), "", Role.Type.GROUP));
    }

    Set<Role> result = new HashSet<Role>();
    int i = 0;
    for (Role entry : roles) {
      if (limit != 0 && result.size() >= limit)
        break;
      if (i >= offset)
        result.add(entry);
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

    // The list of groups for this user represented by the roleList is considered authoritative,
    // so remove the user from any groups which aren't represented in the roleList, and add the
    // user to all groups which are in the roleList.

    Set<String> membershipRoles = new HashSet<String>();

    // List of the user's groups
    List<JpaGroup> membership = UserDirectoryPersistenceUtil.findGroupsByUser(userName, orgId, emf);
    for (JpaGroup group : membership) {
      if (StringUtils.isNotBlank(prefix) && !group.getRole().startsWith(prefix)) {
        //ignore groups of other providers
        continue;
      }
      try {
        if (roleList.contains(group.getRole())) {
          // record this membership
          membershipRoles.add(group.getRole());
        } else {
          // remove user from this group
          logger.debug("Removing user {} from group {}", userName, group.getRole());
          group.getMembers().remove(userName);
          addGroup(group);
        }
      } catch (UnauthorizedException e) {
         logger.warn("Unauthorized to add or remove user {} from group {}", userName, group.getRole(), e);
      }
    }

    // Now add the user to any groups that they are not already a member of
    for (String rolename : roleList) {
      if (!membershipRoles.contains(rolename)) {
        JpaGroup group = UserDirectoryPersistenceUtil.findGroupByRole(rolename, orgId, emf);
        try {
          if (group != null) {
            logger.debug("Adding user {} to group {}", userName, rolename);
            group.getMembers().add(userName);
            addGroup(group);
          } else {
            logger.warn("Cannot add user {} to group {} - no group found with that role", userName, rolename);
          }
        } catch (UnauthorizedException e) {
          logger.warn("Unauthorized to add user {} to group {}", userName, group.getRole(), e);
        }
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
  public Group loadGroup(String groupId, String orgId) {
    return UserDirectoryPersistenceUtil.findGroup(groupId, orgId, emf);
  }

  /**
   * Adds or updates a group to the persistence.
   *
   * @param group
   *          the group to add
   */
  @Override
  public void addGroup(final JpaGroup group) throws UnauthorizedException {
    if (group != null && !UserDirectoryUtils.isCurrentUserAuthorizedHandleRoles(securityService, group.getRoles()))
      throw new UnauthorizedException("The user is not allowed to add or update a group with the admin role");

    Group existingGroup = loadGroup(group.getGroupId(), group.getOrganization().getId());
    if (existingGroup != null && !UserDirectoryUtils.isCurrentUserAuthorizedHandleRoles(securityService, existingGroup.getRoles()))
      throw new UnauthorizedException("The user is not allowed to update a group with the admin role");

    Set<JpaRole> roles = UserDirectoryPersistenceUtil.saveRoles(group.getRoles(), emf);
    JpaOrganization organization = UserDirectoryPersistenceUtil.saveOrganization(group.getOrganization(), emf);

    JpaGroup jpaGroup = new JpaGroup(group.getGroupId(), organization, group.getName(), group.getDescription(), roles,
            group.getMembers());

    // Then save the jpaGroup
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaGroup foundGroup = UserDirectoryPersistenceUtil.findGroup(jpaGroup.getGroupId(), jpaGroup.getOrganization()
              .getId(), emf);
      if (foundGroup == null) {
        em.persist(jpaGroup);
      } else {
        foundGroup.setName(jpaGroup.getName());
        foundGroup.setDescription(jpaGroup.getDescription());
        foundGroup.setMembers(jpaGroup.getMembers());
        foundGroup.setRoles(roles);
        em.merge(foundGroup);
      }
      tx.commit();
      messageSender.sendObjectMessage(GroupItem.GROUP_QUEUE, MessageSender.DestinationType.Queue,
              GroupItem.update(JaxbGroup.fromGroup(jpaGroup)));
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  private void removeGroup(String groupId, String orgId) throws NotFoundException, UnauthorizedException, Exception {
    Group group = loadGroup(groupId, orgId);
    if (group != null && !UserDirectoryUtils.isCurrentUserAuthorizedHandleRoles(securityService, group.getRoles()))
      throw new UnauthorizedException("The user is not allowed to delete a group with the admin role");

    UserDirectoryPersistenceUtil.removeGroup(groupId, orgId, emf);
    messageSender.sendObjectMessage(GroupItem.GROUP_QUEUE, MessageSender.DestinationType.Queue,
            GroupItem.delete(groupId));
  }

  /**
   * Returns all roles from a given group list
   *
   * @param groups
   *          the group list
   * @return the role list
   */
  private List<Role> getGroupsRoles(List<JpaGroup> groups) {
    List<Role> roles = new ArrayList<Role>();
    for (Group group : groups) {
      roles.add(new JaxbRole(group.getRole(), JaxbOrganization.fromOrganization(group.getOrganization()), "", Role.Type.GROUP));
      for (Role role : group.getRoles()) {
        roles.add(new JaxbRole(role.getName(), role.getOrganizationId(), role.getDescription(), Role.Type.DERIVED));
      }

    }
    return roles;
  }

  public Iterator<Group> getGroups() {
    String orgId = securityService.getOrganization().getId();
    return new ArrayList<Group>(UserDirectoryPersistenceUtil.findGroups(orgId, 0, 0, emf)).iterator();
  }

  private boolean like(final String str, final String expr) {
    if (str == null)
      return false;
    String regex = expr.replace("_", ".").replace("%", ".*?");
    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    return p.matcher(str).matches();
  }

  /**
   * Retrieves a group list based on input constraints.
   *
   * @param limit
   *          the int amount to limit the results
   * @param offset
   *          the offset to start this result set at
   * @return the JaxbGroupList of results
   * @throws IOException
   *           if unexpected IO exception occurs
   */
  public JaxbGroupList getGroupsAsJson(int limit, int offset)
          throws IOException {
    return getGroupsAsXml(limit, offset);
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
  public JaxbGroupList getGroupsAsXml(int limit, int offset)
          throws IOException {
    if (limit < 1)
      limit = 100;
    String orgId = securityService.getOrganization().getId();
    JaxbGroupList groupList = new JaxbGroupList();
    List<JpaGroup> groups = UserDirectoryPersistenceUtil.findGroups(orgId, limit, offset, emf);
    for (JpaGroup group : groups) {
      groupList.add(group);
    }
    return groupList;
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

    HashSet<JpaRole> roleSet = new HashSet<JpaRole>();
    if (roles != null) {
      for (String role : StringUtils.split(roles, ",")) {
        roleSet.add(new JpaRole(StringUtils.trim(role), organization));
      }
    }

    HashSet<String> members = new HashSet<String>();
    if (users != null) {
      for (String member : StringUtils.split(users, ",")) {
        members.add(StringUtils.trim(member));
      }
    }

    final String groupId = name.toLowerCase().replaceAll("\\W", "_");

    JpaGroup existingGroup = UserDirectoryPersistenceUtil.findGroup(groupId, organization.getId(), emf);
    if (existingGroup != null)
      throw new ConflictException("group already exists");

    addGroup(new JpaGroup(groupId, organization, name, description, roleSet, members));

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

    JpaGroup group = UserDirectoryPersistenceUtil.findGroup(groupId, organization.getId(), emf);
    if (group == null)
      throw new NotFoundException();

    if (StringUtils.isNotBlank(name))
      group.setName(StringUtils.trim(name));

    if (StringUtils.isNotBlank(description))
      group.setDescription(StringUtils.trim(description));

    if (StringUtils.isNotBlank(roles)) {
      HashSet<JpaRole> roleSet = new HashSet<JpaRole>();
      for (String role : StringUtils.split(roles, ",")) {
        roleSet.add(new JpaRole(StringUtils.trim(role), organization));
      }
      group.setRoles(roleSet);
    } else {
      group.setRoles(new HashSet<JpaRole>());
    }

    if (users != null) {

      HashSet<String> members = new HashSet<String>();
      HashSet<String> invalidateUsers = new HashSet<String>();

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

  @Override
  public void repopulate(final String indexName) {
    final String destinationId = GroupItem.GROUP_QUEUE_PREFIX + WordUtils.capitalize(indexName);
    for (final Organization organization : organizationDirectoryService.getOrganizations()) {
      SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(cc, organization), () -> {
        final List<JpaGroup> groups = UserDirectoryPersistenceUtil.findGroups(organization.getId(), 0, 0, emf);
        int total = groups.size();
        int current = 1;
        logIndexRebuildBegin(logger, indexName, total, "groups", organization);
        for (JpaGroup group : groups) {
          messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                  GroupItem.update(JaxbGroup.fromGroup(group)));
          logIndexRebuildProgress(logger, indexName, total, current);
          current++;
        }
      });
    }
  }

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.Groups;
  }
}
