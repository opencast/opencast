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

package org.opencastproject.authorization.xacml.manager.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.authorization.xacml.manager.impl.Util.getManagedAcl;
import static org.opencastproject.util.RestUtil.R.conflict;
import static org.opencastproject.util.RestUtil.R.noContent;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.R.serverError;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.impl.ManagedAclImpl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Functions;
import org.opencastproject.util.data.functions.Options;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public abstract class AbstractAclServiceRestEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(AbstractAclServiceRestEndpoint.class);

  protected abstract AclServiceFactory getAclServiceFactory();

  protected abstract String getEndpointBaseUrl();

  protected abstract SecurityService getSecurityService();

  protected abstract AuthorizationService getAuthorizationService();

  protected abstract AssetManager getAssetManager();

  protected abstract SeriesService getSeriesService();

  @GET
  @Path("/acl/{aclId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "getacl",
      description = "Return the ACL by the given id",
      returnDescription = "Return the ACL by the given id",
      pathParameters = {
          @RestParameter(name = "aclId", isRequired = true, description = "The ACL identifier", type = INTEGER)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during returning the ACL")
      }
  )
  public String getAcl(@PathParam("aclId") long aclId) throws NotFoundException {
    final Option<ManagedAcl> managedAcl = aclService().getAcl(aclId);
    if (managedAcl.isNone()) {
      logger.info("No ACL with id '{}' could be found", aclId);
      throw new NotFoundException();
    }
    return JsonConv.full(managedAcl.get()).toJson();
  }

  @POST
  @Path("/acl/extend")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "extendacl",
      description = "Return the given ACL with a new role and action in JSON format",
      returnDescription = "Return the ACL with the new role and action in JSON format",
      restParameters = {
          @RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING),
          @RestParameter(name = "action", isRequired = true, description = "The action for the ACL", type = STRING),
          @RestParameter(name = "role", isRequired = true, description = "The role for the ACL", type = STRING),
          @RestParameter(
              name = "allow",
              isRequired = true,
              description = "The allow status for the ACL",
              type = BOOLEAN
          )
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been returned"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The ACL, action or role was invalid or empty"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during returning the ACL")
      }
  )
  public String extendAcl(
      @FormParam("acl") String accessControlList,
      @FormParam("action") String action,
      @FormParam("role") String role,
      @FormParam("allow") boolean allow
  ) {
    if (StringUtils.isBlank(accessControlList) || StringUtils.isBlank(action) || StringUtils.isBlank(role)) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    AccessControlList acl = AccessControlUtil.extendAcl(parseAcl.apply(accessControlList), role, action, allow);
    return JsonConv.full(acl).toJson();
  }

  @POST
  @Path("/acl/reduce")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "reduceacl",
      description = "Return the given ACL without a role and action in JSON format",
      returnDescription = "Return the ACL without the role and action in JSON format", restParameters = {
          @RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING),
          @RestParameter(name = "action", isRequired = true, description = "The action for the ACL", type = STRING),
          @RestParameter(name = "role", isRequired = true, description = "The role for the ACL", type = STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been returned"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The ACL, role or action was invalid or empty"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during returning the ACL")
      }
  )
  public String reduceAcl(
      @FormParam("acl") String accessControlList,
      @FormParam("action") String action,
      @FormParam("role") String role
  ) {
    if (StringUtils.isBlank(accessControlList) || StringUtils.isBlank(action) || StringUtils.isBlank(role)) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    AccessControlList acl = AccessControlUtil.reduceAcl(parseAcl.apply(accessControlList), role, action);
    return JsonConv.full(acl).toJson();
  }

  @GET
  @Path("/acl/acls.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "getacls",
      description = "Lists the ACL's as JSON",
      returnDescription = "The list of ACL's as JSON",
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The list of ACL's has successfully been returned"),
          @RestResponse(
              responseCode = SC_INTERNAL_SERVER_ERROR,
              description = "Error during returning the list of ACL's"
          )
      }
  )
  public String getAcls() {
    return Jsons.arr(mlist(aclService().getAcls()).map(Functions.co(JsonConv.fullManagedAcl)))
            .toJson();
  }

  @POST
  @Path("/acl")
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
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during adding the ACL")
      }
  )
  public String createAcl(
      @FormParam("name") String name,
      @FormParam("acl") String accessControlList
  ) {
    final AccessControlList acl = parseAcl.apply(accessControlList);
    final Option<ManagedAcl> managedAcl = aclService().createAcl(acl, name);
    if (managedAcl.isNone()) {
      logger.info("An ACL with the same name '{}' already exists", name);
      throw new WebApplicationException(Response.Status.CONFLICT);
    }
    return JsonConv.full(managedAcl.get()).toJson();
  }

  @PUT
  @Path("/acl/{aclId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(
      name = "updateacl",
      description = "Update an ACL",
      returnDescription = "Update an ACL",
      pathParameters = {
          @RestParameter(name = "aclId", isRequired = true, description = "The ACL identifier", type = INTEGER)
      },
      restParameters = {
          @RestParameter(name = "name", isRequired = true, description = "The ACL name", type = STRING),
          @RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been updated"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during updating the ACL")
      }
  )
  public String updateAcl(
      @PathParam("aclId") long aclId,
      @FormParam("name") String name,
      @FormParam("acl") String accessControlList
  ) throws NotFoundException {
    final Organization org = getSecurityService().getOrganization();
    final AccessControlList acl = parseAcl.apply(accessControlList);
    final ManagedAclImpl managedAcl = new ManagedAclImpl(aclId, name, org.getId(), acl);
    if (!aclService().updateAcl(managedAcl)) {
      logger.info("No ACL with id '{}' could be found under organization '{}'", aclId, org.getId());
      throw new NotFoundException();
    }
    return JsonConv.full(managedAcl).toJson();
  }

  @DELETE
  @Path("/acl/{aclId}")
  @RestQuery(
      name = "deleteacl",
      description = "Delete an ACL",
      returnDescription = "Delete an ACL",
      pathParameters = {
          @RestParameter(name = "aclId", isRequired = true, description = "The ACL identifier", type = INTEGER)
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been deleted"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"),
          @RestResponse(
              responseCode = SC_CONFLICT,
              description = "The ACL could not be deleted, there are still references on it"
          ),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during deleting the ACL")
      }
  )
  public Response deleteAcl(@PathParam("aclId") long aclId) throws NotFoundException {
    try {
      if (!aclService().deleteAcl(aclId)) {
        return conflict();
      }
    } catch (AclServiceException e) {
      logger.warn("Error deleting manged acl with id '{}': {}", aclId, e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
    return noContent();
  }

  @POST
  @Path("/apply/episode/{episodeId}")
  @RestQuery(
      name = "applyAclToEpisode",
      description = "Immediate application of an ACL to an episode",
      returnDescription = "Status code",
      pathParameters = {
          @RestParameter(name = "episodeId", isRequired = true, description = "The episode ID", type = STRING)
      },
      restParameters = {
          @RestParameter(
              name = "aclId",
              isRequired = false,
              description = "The ID of the ACL to apply. If missing the episode ACL will be "
                  + "deleted to fall back to the series ACL",
              type = INTEGER
          )
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has been successfully applied"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL or the episode has not been found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Internal error")
      }
  )
  public Response applyAclToEpisode(@PathParam("episodeId") String episodeId, @FormParam("aclId") Long aclId) {
    final AclService aclService = aclService();
    final Option<Option<ManagedAcl>> macl = option(aclId).map(getManagedAcl(aclService));
    if (macl.isSome() && macl.get().isNone()) {
      return notFound();
    }
    try {
      if (aclService.applyAclToEpisode(episodeId, Options.join(macl))) {
        return ok();
      } else {
        return notFound();
      }
    } catch (AclServiceException e) {
      logger.error("Error applying acl to episode {}", episodeId);
      return serverError();
    }
  }

  @POST
  @Path("/apply/series/{seriesId}")
  @RestQuery(
      name = "applyAclToSeries",
      description = "Immediate application of an ACL to a series",
      returnDescription = "Status code",
      pathParameters = {
          @RestParameter(name = "seriesId", isRequired = true, description = "The series ID", type = STRING)
      },
      restParameters = {
          @RestParameter(
              name = "aclId",
              isRequired = true,
              description = "The ID of the ACL to apply",
              type = INTEGER
          ),
          @RestParameter(
              name = "override",
              isRequired = false,
              defaultValue = "false",
              description = "If true the series ACL will take precedence over any existing episode ACL",
              type = STRING
          )
      },
      responses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has been successfully applied"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL or the series has not been found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Internal error")
      }
  )
  public Response applyAclToSeries(
      @PathParam("seriesId") String seriesId,
      @FormParam("aclId") long aclId,
      @DefaultValue("false") @FormParam("override") boolean override
  ) {
    final AclService aclService = aclService();
    for (ManagedAcl macl : aclService.getAcl(aclId)) {
      try {
        if (aclService.applyAclToSeries(seriesId, macl, override)) {
          return ok();
        } else {
          return notFound();
        }
      } catch (AclServiceException e) {
        logger.error("Error applying acl to series {}", seriesId);
        return serverError();
      }
    }
    // acl not found
    return notFound();
  }

  private static final Function<String, AccessControlList> parseAcl = new Function<String, AccessControlList>() {
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

  private AclService aclService() {
    return getAclServiceFactory().serviceFor(getSecurityService().getOrganization());
  }
}
