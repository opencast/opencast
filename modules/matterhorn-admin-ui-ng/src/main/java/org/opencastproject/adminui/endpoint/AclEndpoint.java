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
package org.opencastproject.adminui.endpoint;

import static com.entwinemedia.fn.data.json.Jsons.a;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.j;
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

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.endpoint.JsonConv;
import org.opencastproject.authorization.xacml.manager.impl.ManagedAclImpl;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchQuery.Order;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.StreamOp;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.JField;
import com.entwinemedia.fn.data.json.JObjectWrite;
import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang3.ObjectUtils;
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
@RestService(name = "acl", title = "Acl service", notes = "This service offers the default acl CRUD Operations for the admin UI.", abstractText = "Provides operations for acl")
public class AclEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(AclEndpoint.class);

  /** The acl service factory */
  private AclServiceFactory aclServiceFactory;

  /** The security service */
  private SecurityService securityService;

  /**
   * @param aclServiceFactory
   *          the aclServiceFactory to set
   */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
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
          @RestParameter(defaultValue = "0", description = "The page number.", isRequired = false, name = "offset", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The list of ACL's has successfully been returned") })
  public Response getAclsAsJson(@QueryParam("filter") String filter, @QueryParam("sort") String sort,
          @QueryParam("offset") int offset, @QueryParam("limit") int limit) throws IOException {
    if (limit < 1)
      limit = 100;
    Opt<String> optSort = Opt.nul(trimToNull(sort));

    Map<String, String> filters = RestUtils.parseFilter(filter);
    for (String name : filters.keySet()) {
      // TODO
      logger.debug("Acl filter name: {}", name);
      logger.debug("Acl filter value: {}", filters.get(name));
    }

    // Filter acls by filter criteria
    List<ManagedAcl> filteredAcls = new ArrayList<ManagedAcl>();
    for (ManagedAcl acl : aclService().getAcls()) {
      // Filter acl
      // if ((filterName.isSome() && !filterName.get().equals(agent.getName()))
      // || (filterStatus.isSome() && !filterStatus.get().equals(agent.getState()))
      // || (filterLastUpdated.isSome() && filterLastUpdated.get() != agent.getLastHeardFrom()))
      // continue;
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

  @DELETE
  @Path("{id}")
  @RestQuery(name = "deleteacl", description = "Delete an ACL", returnDescription = "Delete an ACL", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The ACL identifier", type = INTEGER) }, reponses = {
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
          @RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been added"),
          @RestResponse(responseCode = SC_CONFLICT, description = "An ACL with the same name already exists"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL") })
  public Response createAcl(@FormParam("name") String name, @FormParam("acl") String accessControlList) {
    final AccessControlList acl = parseAcl.ap(accessControlList);
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
          @RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been updated"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL") })
  public Response updateAcl(@PathParam("id") long aclId, @FormParam("name") String name,
          @FormParam("acl") String accessControlList) throws NotFoundException {
    final Organization org = securityService.getOrganization();
    final AccessControlList acl = parseAcl.ap(accessControlList);
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
  @RestQuery(name = "getacl", description = "Return the ACL by the given id", returnDescription = "Return the ACL by the given id", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The ACL identifier", type = INTEGER) }, reponses = {
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
    public AccessControlList ap(String acl) {
      try {
        return AccessControlParser.parseAcl(acl);
      } catch (Exception e) {
        logger.warn("Unable to parse ACL");
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
      }
    }
  };

  public static JObjectWrite full(AccessControlEntry ace) {
    return j(f(JsonConv.KEY_ROLE, v(ace.getRole())), f(JsonConv.KEY_ACTION, v(ace.getAction())),
            f(JsonConv.KEY_ALLOW, v(ace.isAllow())));
  }

  public static final Fn<AccessControlEntry, JValue> fullAccessControlEntry = new Fn<AccessControlEntry, JValue>() {
    @Override
    public JValue ap(AccessControlEntry ace) {
      return full(ace);
    }
  };

  public static JObjectWrite full(AccessControlList acl) {
    return j(f(JsonConv.KEY_ACE, a(Stream.$(acl.getEntries()).map(fullAccessControlEntry))));
  }

  public static JObjectWrite full(ManagedAcl acl) {
    List<JField> fields = new ArrayList<JField>();
    fields.add(f(JsonConv.KEY_ID, v(acl.getId())));
    fields.add(f(JsonConv.KEY_NAME, v(acl.getName())));
    fields.add(f(JsonConv.KEY_ORGANIZATION_ID, v(acl.getOrganizationId())));
    fields.add(f(JsonConv.KEY_ACL, full(acl.getAcl())));
    return j(fields);
  }

  public static final Fn<ManagedAcl, JValue> fullManagedAcl = new Fn<ManagedAcl, JValue>() {
    @Override
    public JValue ap(ManagedAcl acl) {
      return full(acl);
    }
  };

}
