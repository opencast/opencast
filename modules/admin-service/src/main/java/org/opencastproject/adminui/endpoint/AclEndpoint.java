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

package org.opencastproject.adminui.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
import static org.opencastproject.userdirectory.UserIdRoleProvider.getUserRolePrefix;
import static org.opencastproject.userdirectory.UserIdRoleProvider.isSanitize;
import static org.opencastproject.util.RestUtil.R.conflict;
import static org.opencastproject.util.RestUtil.R.noContent;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.adminui.util.TextFilter;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.endpoint.JsonConv;
import org.opencastproject.authorization.xacml.manager.impl.ManagedAclImpl;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.list.common.query.AclsListQuery;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.requests.SortCriterion;
import org.opencastproject.util.requests.SortCriterion.Order;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

@Path("/admin-ng/acl")
@RestService(
    name = "acl",
    title = "Acl service",
    abstractText = "Provides operations for acl",
    notes = { "This service offers the default acl CRUD Operations for the admin UI.",
              "<strong>Important:</strong> "
                + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
                + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
                + "DO NOT use this for integration of third-party applications.<em>"})
@Component(
    immediate = true,
    service = AclEndpoint.class,
    property = {
        "service.description=Admin UI - ACL Endpoint",
        "opencast.service.type=org.opencastproject.adminui.AclEndpoint",
        "opencast.service.path=/admin-ng/acl",
    }
)
@JaxrsResource
public class AclEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AclEndpoint.class);

  /** The acl service factory */
  private AclServiceFactory aclServiceFactory;

  /** The security service */
  private SecurityService securityService;

  // The role directory service
  private RoleDirectoryService roleDirectoryService;

  /** The global user directory service */
  protected UserDirectoryService userDirectoryService;

  /**
   * @param aclServiceFactory
   *          the aclServiceFactory to set
   */
  @Reference
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  /** OSGi callback for role directory service. */
  @Reference
  public void setRoleDirectoryService(RoleDirectoryService roleDirectoryService) {
    this.roleDirectoryService = roleDirectoryService;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback. */
  @Activate
  protected void activate(ComponentContext cc) {
    logger.info("Activate the Admin ui - Acl facade endpoint");
  }

  private AclService aclService() {
    return aclServiceFactory.serviceFor(securityService.getOrganization());
  }

  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  @GET
  @Path("acls.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "allaclasjson",
      description = "Returns a list of acls",
      returnDescription = "Returns a JSON representation of the list of acls available the current user's organization",
      restParameters = {
          @RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They "
              + "should be formated like that: 'filter1:value1,filter2:value2'", type = STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order. May include any of the "
              + "following: NAME. Add '_DESC' to reverse the sort order (e.g. NAME_DESC).", type = STRING),
          @RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.",
              isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset",
              type = RestParameter.Type.STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The list of ACL's has successfully been returned")
      })
  public Response getAclsAsJson(@QueryParam("filter") String filter, @QueryParam("sort") String sort,
          @QueryParam("offset") int offset, @QueryParam("limit") int limit) throws IOException {
    if (limit < 1) {
      limit = 100;
    }
    Optional<String> optSort = Optional.ofNullable(trimToNull(sort));
    Optional<String> filterName = Optional.empty();
    Optional<String> filterText = Optional.empty();

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      String value = filters.get(name);
      if (AclsListQuery.FILTER_NAME_NAME.equals(name)) {
        filterName = Optional.of(value);
      } else if ((AclsListQuery.FILTER_TEXT_NAME.equals(name)) && (StringUtils.isNotBlank(value))) {
        filterText = Optional.of(value);
      }
    }

    // Filter acls by filter criteria
    List<ManagedAcl> filteredAcls = new ArrayList<>();
    for (ManagedAcl acl : aclService().getAcls()) {
      // Filter list
      if ((filterName.isPresent() && !filterName.get().equals(acl.getName()))
              || (filterText.isPresent() && !TextFilter.match(filterText.get(), acl.getName()))) {
        continue;
      }
      filteredAcls.add(acl);
    }
    int total = filteredAcls.size();

    // Sort by name, description or role
    if (optSort.isPresent()) {
      final ArrayList<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
      Collections.sort(filteredAcls, new Comparator<ManagedAcl>() {
        @Override
        public int compare(ManagedAcl acl1, ManagedAcl acl2) {
          for (SortCriterion criterion : sortCriteria) {
            Order order = criterion.getOrder();
            switch (criterion.getFieldName()) {
              case "name":
                if (order.equals(Order.Descending)) {
                  return ObjectUtils.compare(acl2.getName(), acl1.getName());
                }
                return ObjectUtils.compare(acl1.getName(), acl2.getName());
              default:
                logger.info("Unkown sort type: {}", criterion.getFieldName());
                return 0;
            }
          }
          return 0;
        }
      });
    }

    int start = Math.min(offset, filteredAcls.size());
    int end = Math.min(start + limit, filteredAcls.size());

    // Apply Limit and offset
    List<ManagedAcl> subList = filteredAcls.subList(start, end);

    // Convert each ManagedAcl to JsonObject using a helper method
    List<JsonObject> aclJSON = new ArrayList<>();
    for (ManagedAcl acl : subList) {
      aclJSON.add(full(acl));
    }

    return okJsonList(aclJSON, offset, limit, total);
  }

  @GET
  @Path("roles.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "getRoles",
      description = "Returns a list of roles",
      returnDescription = "Returns a JSON representation of the roles with the given parameters under the "
          + "current user's organization.",
      restParameters = {
          @RestParameter(name = "query", isRequired = false, description = "The query.", type = STRING),
          @RestParameter(name = "target", isRequired = false, description = "The target of the roles.",
              type = STRING),
          @RestParameter(name = "limit", defaultValue = "100",
              description = "The maximum number of items to return per page.", isRequired = false,
              type = RestParameter.Type.STRING),
          @RestParameter(name = "offset", defaultValue = "0", description = "The page number.", isRequired = false,
              type = RestParameter.Type.STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The list of roles.")
      })
  public Response getRoles(@QueryParam("query") String query, @QueryParam("target") String target,
      @QueryParam("offset") int offset, @QueryParam("limit") int limit) {

    String roleQuery = "%";
    if (StringUtils.isNotBlank(query)) {
      roleQuery = query.trim() + "%";
    }

    Role.Target roleTarget = Role.Target.ALL;

    if (StringUtils.isNotBlank(target)) {
      try {
        roleTarget = Role.Target.valueOf(target.trim());
      } catch (Exception e) {
        logger.warn("Invalid target filter value {}", target);
      }
    }

    List<Role> roles = roleDirectoryService.findRoles(roleQuery, roleTarget, offset, limit);
    Set<Role> uniqueRoles = new LinkedHashSet<>(roles);

    JSONArray jsonRoles = new JSONArray();
    for (Role role: uniqueRoles) {
      JSONObject jsonRole = new JSONObject();
      jsonRole.put("name", role.getName());
      jsonRole.put("type", role.getType().toString());
      jsonRole.put("description", role.getDescription());
      jsonRole.put("organization", role.getOrganizationId());
      jsonRole.put("isSanitize", isSanitize());
      if (!isSanitize()) {
        User user = userDirectoryService.loadUser(role.getName().replaceFirst(getUserRolePrefix(), ""));
        if (user != null) {
          jsonRole.put("user", generateJsonUser(user));
        }
      }
      jsonRoles.add(jsonRole);
    }

    return Response.ok(jsonRoles.toJSONString()).build();
  }

  @DELETE
  @Path("{id}")
  @RestQuery(
      name = "deleteacl",
      description = "Delete an ACL",
      returnDescription = "Delete an ACL",
      pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "The ACL identifier", type = INTEGER)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been deleted"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"),
          @RestResponse(responseCode = SC_CONFLICT, description = "The ACL could not be deleted, there are still "
              + "references on it")
      })
  public Response deleteAcl(@PathParam("id") long aclId) throws NotFoundException {
    try {
      if (!aclService().deleteAcl(aclId)) {
        return conflict();
      }
    } catch (AclServiceException e) {
      logger.warn("Error deleting manged acl with id '{}'", aclId, e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
    return noContent();
  }

  @POST
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "createacl",
      description = "Create an ACL",
      returnDescription = "Create an ACL",
      restParameters = {
          @RestParameter(name = "name", isRequired = true, description = "The ACL name", type = STRING),
          @RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been added"),
          @RestResponse(responseCode = SC_CONFLICT, description = "An ACL with the same name already exists"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL")
      })
  public Response createAcl(@FormParam("name") String name, @FormParam("acl") String accessControlList) {
    final AccessControlList acl = parseAcl(accessControlList);
    Optional<ManagedAcl> managedAcl = aclService().createAcl(acl, name);
    if (managedAcl.isEmpty()) {
      logger.info("An ACL with the same name '{}' already exists", name);
      throw new WebApplicationException(Response.Status.CONFLICT);
    }
    return RestUtils.okJson(full(managedAcl.get()));
  }

  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "updateacl",
      description = "Update an ACL",
      returnDescription = "Update an ACL",
      pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "The ACL identifier", type = INTEGER)
      },
      restParameters = {
          @RestParameter(name = "name", isRequired = true, description = "The ACL name", type = STRING),
          @RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been updated"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL")
      })
  public Response updateAcl(@PathParam("id") long aclId, @FormParam("name") String name,
      @FormParam("acl") String accessControlList) throws NotFoundException {
    final Organization org = securityService.getOrganization();
    final AccessControlList acl = parseAcl(accessControlList);
    final ManagedAclImpl managedAcl = new ManagedAclImpl(aclId, name, org.getId(), acl);
    if (!aclService().updateAcl(managedAcl)) {
      logger.info("No ACL with id '{}' could be found under organization '{}'", aclId, org.getId());
      throw new NotFoundException();
    }
    return RestUtils.okJson(full(managedAcl));
  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "getacl",
      description = "Return the ACL by the given id",
      returnDescription = "Return the ACL by the given id",
      pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "The ACL identifier", type = INTEGER)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found")
      })
  public Response getAcl(@PathParam("id") long aclId) throws NotFoundException {
    Optional<ManagedAcl> managedAcl = aclService().getAcl(aclId);
    if (managedAcl.isPresent()) {
      return RestUtils.okJson(full(managedAcl.get()));
    }
    logger.info("No ACL with id '{}' could by found", aclId);
    throw new NotFoundException();
  }

  @GET
  @Path("acl/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "getaclbyname",
      description = "Return the ACL by the given name",
      returnDescription = "Return the ACL by the given name",
      pathParameters = {
          @RestParameter(name = "name", isRequired = true, description = "The ACL name", type = STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found")
      })
  public Response getAcl(@PathParam("name") String aclName) throws NotFoundException {
    Optional<ManagedAcl> managedAcl = aclService().getAcl(aclName);
    if (managedAcl.isPresent()) {
      return RestUtils.okJson(full(managedAcl.get()));
    }
    logger.info("No ACL with name '{}' could by found", aclName);
    throw new NotFoundException();
  }

  private static AccessControlList parseAcl(String acl) {
    try {
      return AccessControlParser.parseAcl(acl);
    } catch (Exception e) {
      logger.warn("Unable to parse ACL", e);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }

  public JsonObject full(AccessControlEntry ace) {
    JsonObject json = new JsonObject();
    json.addProperty(JsonConv.KEY_ROLE, ace.getRole());
    json.addProperty(JsonConv.KEY_ACTION, ace.getAction());
    json.addProperty(JsonConv.KEY_ALLOW, ace.isAllow());
    return json;
  }

  private JsonObject fullAccessControlEntry(AccessControlEntry ace) {
    return full(ace);
  }

  public JsonObject full(AccessControlList acl) {
    JsonObject json = new JsonObject();
    JsonArray aceArray = new JsonArray();

    List<AccessControlEntry> entries = acl.getEntries();
    if (entries != null) {
      for (AccessControlEntry entry : entries) {
        aceArray.add(fullAccessControlEntry(entry));
      }
    }

    json.add(JsonConv.KEY_ACE, aceArray);
    return json;
  }

  public JsonObject full(ManagedAcl acl) {
    JsonObject json = new JsonObject();
    json.addProperty(JsonConv.KEY_ID, acl.getId());
    json.addProperty(JsonConv.KEY_NAME, acl.getName());
    json.addProperty(JsonConv.KEY_ORGANIZATION_ID, acl.getOrganizationId());
    json.add("acl", full(acl.getAcl()));
    return json;
  }

  public Map<String, Object> generateJsonUser(User user) {
    // Prepare the roles
    Map<String, Object> userData = new HashMap<>();
    userData.put("username", user.getUsername());
    userData.put("name", user.getName());
    userData.put("email", user.getEmail());
    return userData;
  }

}
