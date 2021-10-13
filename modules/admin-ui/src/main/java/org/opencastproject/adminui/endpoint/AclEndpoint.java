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

package org.opencastproject.adminui.endpoint;

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.index.service.util.RestUtils.okJsonList;
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
import org.opencastproject.index.service.resources.list.query.AclsListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.RoleDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.requests.SortCriterion;
import org.opencastproject.util.requests.SortCriterion.Order;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.StreamOp;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

@Path("/")
@RestService(name = "acl", title = "Acl service",
  abstractText = "Provides operations for acl",
  notes = { "This service offers the default acl CRUD Operations for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public class AclEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AclEndpoint.class);

  /** The acl service factory */
  private AclServiceFactory aclServiceFactory;

  /** The security service */
  private SecurityService securityService;

  // The role directory service
  private RoleDirectoryService roleDirectoryService;

  /**
   * @param aclServiceFactory
   *          the aclServiceFactory to set
   */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  /** OSGi callback for role directory service. */
  public void setRoleDirectoryService(RoleDirectoryService roleDirectoryService) {
    this.roleDirectoryService = roleDirectoryService;
  }

  /**
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback. */
  protected void activate(ComponentContext cc) {
    logger.info("Activate the Admin ui - Acl facade endpoint");
  }

  private AclService aclService() {
    return aclServiceFactory.serviceFor(securityService.getOrganization());
  }

  @GET
  @Path("acls.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "allaclasjson", description = "Returns a list of acls", returnDescription = "Returns a JSON representation of the list of acls available the current user's organization", restParameters = {
          @RestParameter(name = "filter", isRequired = false, description = "The filter used for the query. They should be formated like that: 'filter1:value1,filter2:value2'", type = STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order. May include any of the following: NAME. Add '_DESC' to reverse the sort order (e.g. NAME_DESC).", type = STRING),
          @RestParameter(defaultValue = "100", description = "The maximum number of items to return per page.", isRequired = false, name = "limit", type = RestParameter.Type.STRING),
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, responses = { @RestResponse(responseCode = SC_OK, description = "The list of ACL's has successfully been returned") })
  public Response getAclsAsJson(@QueryParam("filter") String filter, @QueryParam("sort") String sort,
          @QueryParam("offset") int offset, @QueryParam("limit") int limit) throws IOException {
    if (limit < 1)
      limit = 100;
    Opt<String> optSort = Opt.nul(trimToNull(sort));
    Option<String> filterName = Option.none();
    Option<String> filterText = Option.none();

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      String value = filters.get(name);
      if (AclsListQuery.FILTER_NAME_NAME.equals(name)) {
        filterName = Option.some(value);
      } else if ((AclsListQuery.FILTER_TEXT_NAME.equals(name)) && (StringUtils.isNotBlank(value))) {
        filterText = Option.some(value);
      }
    }

    // Filter acls by filter criteria
    List<ManagedAcl> filteredAcls = new ArrayList<>();
    for (ManagedAcl acl : aclService().getAcls()) {
      // Filter list
      if ((filterName.isSome() && !filterName.get().equals(acl.getName()))
              || (filterText.isSome() && !TextFilter.match(filterText.get(), acl.getName()))) {
        continue;
      }
      filteredAcls.add(acl);
    }
    int total = filteredAcls.size();

    // Sort by name, description or role
    if (optSort.isSome()) {
      final Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
      Collections.sort(filteredAcls, new Comparator<ManagedAcl>() {
        @Override
        public int compare(ManagedAcl acl1, ManagedAcl acl2) {
          for (SortCriterion criterion : sortCriteria) {
            Order order = criterion.getOrder();
            switch (criterion.getFieldName()) {
              case "name":
                if (order.equals(Order.Descending))
                  return ObjectUtils.compare(acl2.getName(), acl1.getName());
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

    // Apply Limit and offset
    List<JValue> aclJSON = Stream.$(filteredAcls).drop(offset)
            .apply(limit > 0 ? StreamOp.<ManagedAcl> id().take(limit) : StreamOp.<ManagedAcl> id()).map(fullManagedAcl)
            .toList();
    return okJsonList(aclJSON, offset, limit, total);
  }

  @GET
  @Path("roles.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getRoles", description = "Returns a list of roles",
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
                              type = RestParameter.Type.STRING) },
             responses = { @RestResponse(responseCode = SC_OK, description = "The list of roles.") })
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

    JSONArray jsonRoles = new JSONArray();
    for (Role role: roles) {
      JSONObject jsonRole = new JSONObject();
      jsonRole.put("name", role.getName());
      jsonRole.put("type", role.getType().toString());
      jsonRole.put("description", role.getDescription());
      jsonRole.put("organization", role.getOrganizationId());
      jsonRoles.add(jsonRole);
    }

    return Response.ok(jsonRoles.toJSONString()).build();
  }

  @DELETE
  @Path("{id}")
  @RestQuery(name = "deleteacl", description = "Delete an ACL", returnDescription = "Delete an ACL", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The ACL identifier", type = INTEGER) }, responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been deleted"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"),
          @RestResponse(responseCode = SC_CONFLICT, description = "The ACL could not be deleted, there are still references on it") })
  public Response deleteAcl(@PathParam("id") long aclId) throws NotFoundException {
    try {
      if (!aclService().deleteAcl(aclId))
        return conflict();
    } catch (AclServiceException e) {
      logger.warn("Error deleting manged acl with id '{}': {}", aclId, e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
    return noContent();
  }

  @POST
  @Path("")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "createacl", description = "Create an ACL", returnDescription = "Create an ACL", restParameters = {
          @RestParameter(name = "name", isRequired = true, description = "The ACL name", type = STRING),
          @RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING) }, responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been added"),
          @RestResponse(responseCode = SC_CONFLICT, description = "An ACL with the same name already exists"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL") })
  public Response createAcl(@FormParam("name") String name, @FormParam("acl") String accessControlList) {
    final AccessControlList acl = parseAcl.apply(accessControlList);
    final Opt<ManagedAcl> managedAcl = aclService().createAcl(acl, name).toOpt();
    if (managedAcl.isNone()) {
      logger.info("An ACL with the same name '{}' already exists", name);
      throw new WebApplicationException(Response.Status.CONFLICT);
    }
    return RestUtils.okJson(full(managedAcl.get()));
  }

  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "updateacl", description = "Update an ACL", returnDescription = "Update an ACL", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The ACL identifier", type = INTEGER) }, restParameters = {
          @RestParameter(name = "name", isRequired = true, description = "The ACL name", type = STRING),
          @RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING) }, responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been updated"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL") })
  public Response updateAcl(@PathParam("id") long aclId, @FormParam("name") String name,
          @FormParam("acl") String accessControlList) throws NotFoundException {
    final Organization org = securityService.getOrganization();
    final AccessControlList acl = parseAcl.apply(accessControlList);
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
  @RestQuery(name = "getacl", description = "Return the ACL by the given id", returnDescription = "Return the ACL by the given id", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The ACL identifier", type = INTEGER) }, responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found") })
  public Response getAcl(@PathParam("id") long aclId) throws NotFoundException {
    for (ManagedAcl managedAcl : aclService().getAcl(aclId)) {
      return RestUtils.okJson(full(managedAcl));
    }
    logger.info("No ACL with id '{}' could by found", aclId);
    throw new NotFoundException();
  }

  private static final Fn<String, AccessControlList> parseAcl = new Fn<String, AccessControlList>() {
    @Override
    public AccessControlList apply(String acl) {
      try {
        return AccessControlParser.parseAcl(acl);
      } catch (Exception e) {
        logger.warn("Unable to parse ACL");
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
      }
    }
  };

  public JObject full(AccessControlEntry ace) {
    return obj(f(JsonConv.KEY_ROLE, v(ace.getRole())), f(JsonConv.KEY_ACTION, v(ace.getAction())),
            f(JsonConv.KEY_ALLOW, v(ace.isAllow())));
  }

  private final Fn<AccessControlEntry, JValue> fullAccessControlEntry = new Fn<AccessControlEntry, JValue>() {
    @Override
    public JValue apply(AccessControlEntry ace) {
      return full(ace);
    }
  };

  public JObject full(AccessControlList acl) {
    return obj(f(JsonConv.KEY_ACE, arr(Stream.$(acl.getEntries()).map(fullAccessControlEntry))));
  }

  public JObject full(ManagedAcl acl) {
    List<Field> fields = new ArrayList<>();
    fields.add(f(JsonConv.KEY_ID, v(acl.getId())));
    fields.add(f(JsonConv.KEY_NAME, v(acl.getName())));
    fields.add(f(JsonConv.KEY_ORGANIZATION_ID, v(acl.getOrganizationId())));
    fields.add(f(JsonConv.KEY_ACL, full(acl.getAcl())));
    return obj(fields);
  }

  private final Fn<ManagedAcl, JValue> fullManagedAcl = new Fn<ManagedAcl, JValue>() {
    @Override
    public JValue apply(ManagedAcl acl) {
      return full(acl);
    }
  };

}
