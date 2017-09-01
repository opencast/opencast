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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.http.HttpStatus.SC_CONFLICT;

import org.opencastproject.index.IndexProducer;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.group.GroupItem;
import org.opencastproject.message.broker.api.index.AbstractIndexProducer;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;
import org.opencastproject.message.broker.api.index.IndexRecreateObject.Service;
import org.opencastproject.security.api.DefaultOrganization;
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
import org.opencastproject.userdirectory.utils.UserDirectoryUtils;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.osgi.service.component.ComponentContext;
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
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Manages and locates users using JPA. Note that this also provides a REST endpoint to manage user roles. Since this is
 * not intended to be production code, the REST concerns have not be factored into a separate class. Feel free to
 * refactor.
 */
@Path("/")
@RestService(name = "groups", title = "Internal group manager", abstractText = "This service offers the ability to manage the groups for internal accounts.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
                "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class JpaGroupRoleProvider extends AbstractIndexProducer implements RoleProvider, GroupProvider {

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
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * Sets the user directory service
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * @param messageSender
   *          The messageSender to set
   */
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  /**
   * @param messageReceiver
   *          The messageReceiver to set
   */
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param organizationDirectoryService
   *          the organizationDirectoryService to set
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /**
   * Callback for activation of this component.
   *
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    logger.debug("Activate group role provider");
    this.cc = cc;

    // Set up persistence
    super.activate();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.RoleProvider#getRoles()
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
    List<Role> roles = new ArrayList<Role>();
    String orgId = securityService.getOrganization().getId();
    Group group = UserDirectoryPersistenceUtil.findGroupByRole(groupName, orgId, emf);
    if (group != null) {
      for (Role role : group.getRoles()) {
        JaxbRole grouprole = new JaxbRole(role.getName(), JaxbOrganization.fromOrganization(role.getOrganization()), role.getDescription(), Role.Type.DERIVED);
        roles.add(grouprole);
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

    logger.debug("updateGroupMembershipFromRoles({}, size={})", userName, roleList.size());

    // The list of groups for this user represented by the roleList is considered authoritative,
    // so remove the user from any groups which aren't represented in the roleList, and add the
    // user to all groups which are in the roleList.

    Set<String> membershipRoles = new HashSet<String>();

    // List of the user's groups
    List<JpaGroup> membership = UserDirectoryPersistenceUtil.findGroupsByUser(userName, orgId, emf);
    for (JpaGroup group : membership) {
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
         logger.warn("Unable to add or remove user {} from group {} - unauthorized", userName, group.getRole());
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
          logger.warn("Unable to add user {} to group {} - unauthorized", userName, group.getRole());
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
        JaxbRole grouprole = new JaxbRole(role.getName(), JaxbOrganization.fromOrganization(role.getOrganization()), role.getDescription(), Role.Type.DERIVED);
        roles.add(grouprole);
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

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("groups.json")
  @RestQuery(name = "allgroupsasjson", description = "Returns a list of groups", returnDescription = "Returns a JSON representation of the list of groups available the current user's organization", restParameters = {
          @RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The groups.") })
  public JaxbGroupList getGroupsAsJson(@QueryParam("limit") int limit, @QueryParam("offset") int offset)
          throws IOException {
    return getGroupsAsXml(limit, offset);
  }

  @GET
  @Produces(MediaType.APPLICATION_XML)
  @Path("groups.xml")
  @RestQuery(name = "allgroupsasxml", description = "Returns a list of groups", returnDescription = "Returns a XML representation of the list of groups available the current user's organization", restParameters = {
          @RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The groups.") })
  public JaxbGroupList getGroupsAsXml(@QueryParam("limit") int limit, @QueryParam("offset") int offset)
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

  @DELETE
  @Path("{id}")
  @RestQuery(name = "removegrouop", description = "Remove a group", returnDescription = "Return no content", pathParameters = { @RestParameter(name = "id", description = "The group identifier", isRequired = true, type = Type.STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Group deleted"),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to remove a group with the admin role."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Group not found."),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "An internal server error occured.") })
  public Response removeGroup(@PathParam("id") String groupId) {
    String orgId = securityService.getOrganization().getId();
    try {
      removeGroup(groupId, orgId);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      return Response.status(SC_NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      return Response.status(SC_FORBIDDEN).build();
    } catch (Exception e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("")
  @RestQuery(name = "createGroup", description = "Add a group", returnDescription = "Return the status codes", restParameters = {
          @RestParameter(name = "name", description = "The group name", isRequired = true, type = Type.STRING),
          @RestParameter(name = "description", description = "The group description", isRequired = false, type = Type.STRING),
          @RestParameter(name = "roles", description = "A comma seperated string of additional group roles", isRequired = false, type = Type.TEXT),
          @RestParameter(name = "users", description = "A comma seperated string of group members", isRequired = false, type = Type.TEXT) }, reponses = {
                  @RestResponse(responseCode = SC_CREATED, description = "Group created"),
                  @RestResponse(responseCode = SC_BAD_REQUEST, description = "Name too long"),
                  @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to create a group with the admin role."),
                  @RestResponse(responseCode = SC_CONFLICT, description = "An group with this name already exists.") })
  public Response createGroup(@FormParam("name") String name, @FormParam("description") String description,
          @FormParam("roles") String roles, @FormParam("users") String users) {
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
      return Response.status(SC_CONFLICT).build();

    try {
      addGroup(new JpaGroup(groupId, organization, name, description, roleSet, members));
    } catch (IllegalArgumentException e) {
      logger.warn(e.getMessage());
      return Response.status(Status.BAD_REQUEST).build();
    } catch (UnauthorizedException e) {
      return Response.status(SC_FORBIDDEN).build();
    }
    return Response.status(Status.CREATED).build();
  }

  @PUT
  @Path("{id}")
  @RestQuery(name = "updateGroup", description = "Update a group", returnDescription = "Return the status codes", pathParameters = { @RestParameter(name = "id", description = "The group identifier", isRequired = true, type = Type.STRING) }, restParameters = {
          @RestParameter(name = "name", description = "The group name", isRequired = true, type = Type.STRING),
          @RestParameter(name = "description", description = "The group description", isRequired = false, type = Type.STRING),
          @RestParameter(name = "roles", description = "A comma seperated string of additional group roles", isRequired = false, type = Type.TEXT),
          @RestParameter(name = "users", description = "A comma seperated string of group members", isRequired = true, type = Type.TEXT) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "Group updated"),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "Not enough permissions to update a group with the admin role."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Group not found"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Name too long") })
  public Response updateGroup(@PathParam("id") String groupId, @FormParam("name") String name,
          @FormParam("description") String description, @FormParam("roles") String roles,
          @FormParam("users") String users) throws NotFoundException {
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

    try {
      addGroup(group);
    } catch (IllegalArgumentException e) {
      logger.warn(e.getMessage());
      return Response.status(Status.BAD_REQUEST).build();
    } catch (UnauthorizedException ex) {
      return Response.status(SC_FORBIDDEN).build();
    }

    return Response.ok().build();
  }

  @Override
  public void repopulate(final String indexName) {
    final String destinationId = GroupItem.GROUP_QUEUE_PREFIX + WordUtils.capitalize(indexName);
    for (final Organization organization : organizationDirectoryService.getOrganizations()) {
      SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(cc, organization), new Effect0() {
        @Override
        protected void run() {
          final List<JpaGroup> groups = UserDirectoryPersistenceUtil.findGroups(organization.getId(), 0, 0, emf);
          int total = groups.size();
          final int responseInterval = (total < 100) ? 1 : (total / 100);
          int current = 1;
          logger.info(
                  "Re-populating index '{}' with groups of organization {}. There are {} group(s) to add to the index.",
                  new Object[] { indexName, securityService.getOrganization().getId(), total });
          for (JpaGroup group : groups) {
            messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                    GroupItem.update(JaxbGroup.fromGroup(group)));
            if (((current % responseInterval) == 0) || (current == total)) {
              messageSender.sendObjectMessage(IndexProducer.RESPONSE_QUEUE, MessageSender.DestinationType.Queue,
                    IndexRecreateObject.update(indexName, IndexRecreateObject.Service.Groups, total, current));
            }
            current++;
          }
        }
      });
    }
    Organization organization = new DefaultOrganization();
    SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(cc, organization), new Effect0() {
      @Override
      protected void run() {
        messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                IndexRecreateObject.end(indexName, IndexRecreateObject.Service.Groups));
      }
    });
  }

  @Override
  public MessageReceiver getMessageReceiver() {
    return messageReceiver;
  }

  @Override
  public Service getService() {
    return Service.Groups;
  }

  @Override
  public String getClassName() {
    return JpaGroupRoleProvider.class.getName();
  }

  @Override
  public MessageSender getMessageSender() {
    return messageSender;
  }

  @Override
  public SecurityService getSecurityService() {
    return securityService;
  }

  @Override
  public String getSystemUserName() {
    return SecurityUtil.getSystemUserName(cc);
  }

}
