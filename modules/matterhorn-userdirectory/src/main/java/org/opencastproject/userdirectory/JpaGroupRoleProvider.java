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
package org.opencastproject.userdirectory;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.opencastproject.kernel.security.persistence.JpaOrganization;
import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleProvider;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.spi.PersistenceProvider;
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
public class JpaGroupRoleProvider implements RoleProvider {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(JpaGroupRoleProvider.class);

  /** The JPA provider */
  protected PersistenceProvider persistenceProvider = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The factory used to generate the entity manager */
  protected EntityManagerFactory emf = null;

  protected Map<String, Object> persistenceProperties;

  /**
   * @param persistenceProvider
   *          the persistenceProvider to set
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  /**
   * @param persistenceProperties
   *          the persistenceProperties to set
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for activation of this component.
   * 
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    logger.debug("Activate group role provider");

    // Set up persistence
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.userdirectory", persistenceProperties);
  }

  /**
   * Callback for deactivation of this component.
   */
  public void deactivate() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
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
   * @see org.opencastproject.security.api.RoleProvider#getOrganization()
   */
  @Override
  public String getOrganization() {
    return UserProvider.ALL_ORGANIZATIONS;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.security.api.RoleProvider#findRoles(String, int, int)
   */
  @Override
  public Iterator<Role> findRoles(String query, int offset, int limit) {
    if (query == null)
      throw new IllegalArgumentException("Query must be set");
    String orgId = securityService.getOrganization().getId();

    List<Role> groupRoles = getGroupsRoles(UserDirectoryPersistenceUtil.findGroups(orgId, 0, 0, emf));
    List<Role> roles = new ArrayList<Role>();
    for (Role role : groupRoles) {
      if (like(role.getName(), query) || like(role.getDescription(), query))
        roles.add(role);
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
  public void addGroup(final JpaGroup group) {
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
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
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
      roles.add(new JaxbRole(group.getRole(), JaxbOrganization.fromOrganization(group.getOrganization()), ""));
      roles.addAll(group.getRoles());
    }
    return roles;
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
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Group not found."),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "An internal server error occured.") })
  public Response removeGroup(@PathParam("id") String groupId) throws NotFoundException {
    String orgId = securityService.getOrganization().getId();
    try {
      UserDirectoryPersistenceUtil.removeGroup(groupId, orgId, emf);
      return Response.noContent().build();
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
          @RestParameter(name = "users", description = "A comma seperated string of group members", isRequired = true, type = Type.TEXT) }, reponses = {
          @RestResponse(responseCode = SC_CREATED, description = "Group created"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Name too long") })
  public Response createGroup(@FormParam("name") String name, @FormParam("description") String description,
          @FormParam("roles") String roles, @FormParam("users") String users) {
    JpaOrganization organization = (JpaOrganization) securityService.getOrganization();

    HashSet<JpaRole> roleSet = new HashSet<JpaRole>();
    for (String role : StringUtils.split(roles, ",")) {
      roleSet.add(new JpaRole(StringUtils.trim(role), organization));
    }
    HashSet<String> members = new HashSet<String>();
    for (String member : StringUtils.split(users, ",")) {
      members.add(StringUtils.trim(member));
    }

    try {
      addGroup(new JpaGroup(name.toLowerCase().replaceAll("\\W", "_"), organization, name, description, roleSet,
              members));
    } catch (IllegalArgumentException e) {
      logger.warn(e.getMessage());
      return Response.status(Status.BAD_REQUEST).build();
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

    if (StringUtils.isNotBlank(users)) {
      HashSet<String> members = new HashSet<String>();
      for (String member : StringUtils.split(users, ",")) {
        members.add(StringUtils.trim(member));
      }
      group.setMembers(members);
    }

    try {
      addGroup(group);
    } catch (IllegalArgumentException e) {
      logger.warn(e.getMessage());
      return Response.status(Status.BAD_REQUEST).build();
    }

    return Response.ok().build();
  }

}
