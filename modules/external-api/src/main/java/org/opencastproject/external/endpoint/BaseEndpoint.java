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
package org.opencastproject.external.endpoint;

import static org.opencastproject.index.service.util.JSONUtils.collectionToJsonArray;
import static org.opencastproject.index.service.util.JSONUtils.safeString;
import static org.opencastproject.userdirectory.UserIdRoleProvider.getUserIdRole;
import static org.opencastproject.util.RestUtil.getEndpointUrl;

import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiVersion;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The external service endpoint acts as a location for external apis to query the current server of the external
 * supported API.
 */
@Path("/api")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_0_0, ApiMediaType.VERSION_1_1_0, ApiMediaType.VERSION_1_2_0,
            ApiMediaType.VERSION_1_3_0, ApiMediaType.VERSION_1_4_0, ApiMediaType.VERSION_1_5_0,
            ApiMediaType.VERSION_1_6_0, ApiMediaType.VERSION_1_7_0, ApiMediaType.VERSION_1_8_0,
            ApiMediaType.VERSION_1_9_0, ApiMediaType.VERSION_1_10_0, ApiMediaType.VERSION_1_11_0,
            ApiMediaType.VERSION_1_12_0})
@RestService(name = "externalapiservice", title = "External API Service", notes = {},
             abstractText = "Provides a location for external apis to query the current server of the API.")
@Tag(
    name = "External API",
    description = "The external service endpoint acts as a location for external apis to query the current server of "
        + "the external supported API."
)
@Component(
    immediate = true,
    service = BaseEndpoint.class,
    property = {
        "service.description=External API - Base Endpoint",
        "opencast.service.type=org.opencastproject.external",
        "opencast.service.path=/api"
    }
)
@JaxrsResource
public class BaseEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(BaseEndpoint.class);

  /** Base URL of this endpoint */
  protected String endpointBaseUrl;

  /* OSGi service references */
  private SecurityService securityService;

  /** OSGi DI */
  @Reference
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi activation method */
  @Activate
  void activate(ComponentContext cc) {
    logger.info("Activating External API - Base Endpoint");

    final Tuple<String, String> endpointUrl = getEndpointUrl(cc, OpencastConstants.EXTERNAL_API_URL_ORG_PROPERTY,
            RestConstants.SERVICE_PATH_PROPERTY);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
    logger.debug("Configured service endpoint is {}", endpointBaseUrl);
  }

  @GET
  @Path("")
  @RestQuery(
      name = "getendpointinfo",
      description = "Returns key characteristics of the API such as the server name and the default version.",
      returnDescription = "",
      responses = {
          @RestResponse(description = "The api information is returned.", responseCode = HttpServletResponse.SC_OK)
      })
  @Operation(
      summary = "Get API information",
      description = "Returns key characteristics of the API such as the server name and the default version."
  )
  @ApiResponse(responseCode = "200", description = "The api information is returned.")
  public Response getEndpointInfo() {
    Organization organization = securityService.getOrganization();
    String orgExternalAPIUrl = organization.getProperties().get(OpencastConstants.EXTERNAL_API_URL_ORG_PROPERTY);

    String url;
    if (StringUtils.isNotBlank(orgExternalAPIUrl)) {
      url = orgExternalAPIUrl;
    } else {
      url = endpointBaseUrl;
    }

    JsonObject json = new JsonObject();
    json.addProperty("url", url);
    json.addProperty("version", ApiVersion.CURRENT_VERSION.toString());

    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, json.toString());
  }

  @GET
  @Path("info/me")
  @RestQuery(
      name = "getuserinfo",
      description = "Returns information on the logged in user.",
      returnDescription = "",
      responses = {
          @RestResponse(description = "The user information is returned.", responseCode = HttpServletResponse.SC_OK)
      })
  @Operation(summary = "Get user information", description = "Returns information on the logged in user.")
  @ApiResponse(responseCode = "200", description = "The user information is returned.")
  public Response getUserInfo() {
    final User user = securityService.getUser();

    JsonObject json = new JsonObject();
    json.addProperty("email", safeString(user.getEmail()));
    json.addProperty("name", user.getName());
    json.addProperty("provider", user.getProvider());
    json.addProperty("userrole", getUserIdRole(user.getUsername()));
    json.addProperty("username", user.getUsername());

    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, json.toString());
  }

  @GET
  @Path("info/me/roles")
  @RestQuery(
      name = "getuserroles",
      description = "Returns current user's roles.",
      returnDescription = "",
      responses = {
          @RestResponse(description = "The set of roles is returned.", responseCode = HttpServletResponse.SC_OK)
      })
  @Operation(summary = "Get user roles", description = "Returns current user's roles.")
  @ApiResponse(responseCode = "200", description = "The set of roles is returned.")
  public Response getUserRoles() {
    final User user = securityService.getUser();

    JsonArray roles = new JsonArray();
    for (final Role role : user.getRoles()) {
      roles.add(role.getName());
    }

    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, roles.toString());
  }

  @GET
  @Path("info/organization")
  @RestQuery(
      name = "getorganizationinfo",
      description = "Returns the current organization.",
      returnDescription = "",
      responses = {
          @RestResponse(description = "The organization details are returned.",
              responseCode = HttpServletResponse.SC_OK)
      })
  @Operation(summary = "Get organization information", description = "Returns the current organization.")
  @ApiResponse(responseCode = "200", description = "The organization details are returned.")
  public Response getOrganizationInfo() {
    final Organization org = securityService.getOrganization();

    JsonObject json = new JsonObject();
    json.addProperty("adminRole", org.getAdminRole());
    json.addProperty("anonymousRole", org.getAnonymousRole());
    json.addProperty("id", org.getId());
    json.addProperty("name", org.getName());

    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, json.toString());
  }

  @GET
  @Path("info/organization/properties")
  @RestQuery(
      name = "getorganizationproperties",
      description = "Returns the current organization's properties.",
      returnDescription = "",
      responses = {
          @RestResponse(description = "The organization properties are returned.",
              responseCode = HttpServletResponse.SC_OK)
      })
  @Operation(summary = "Get organization properties", description = "Returns the current organization's properties.")
  @ApiResponse(responseCode = "200", description = "The organization properties are returned.")
  public Response getOrganizationProperties() {
    final Organization org = securityService.getOrganization();

    JsonObject json = new JsonObject();
    for (Entry<String, String> prop : org.getProperties().entrySet()) {
      json.addProperty(prop.getKey(), safeString(prop.getValue()));
    }

    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, json.toString());
  }

  @GET
  @Path("info/organization/properties/engageuiurl")
  @RestQuery(
      name = "getorganizationpropertiesengageuiurl",
      description = "Returns the engage ui url property.",
      returnDescription = "",
      responses = {
          @RestResponse(description = "The engage ui url is returned.", responseCode = HttpServletResponse.SC_OK)
      })
  @Operation(summary = "Get organization properties engage ui url", description = "Returns the engage ui url property.")
  @ApiResponse(responseCode = "200", description = "The engage ui url is returned.")
  public Response getOrganizationPropertiesEngageUiUrl() {
    final Organization org = securityService.getOrganization();
    String keyToFind = "org.opencastproject.engage.ui.url";

    String value = null;
    for (Entry<String, String> prop : org.getProperties().entrySet()) {
      if (prop.getKey().equals(keyToFind)) {
        value = prop.getValue() != null ? prop.getValue() : "";
        break;
      }
    }

    if (value == null) {
      value = safeString(UrlSupport.DEFAULT_BASE_URL);
    }

    JsonObject json = new JsonObject();
    json.addProperty(keyToFind, value);

    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, json.toString());
  }

  @GET
  @Path("version")
  @RestQuery(
      name = "getversion",
      description = "Returns a list of available version as well as the default version.",
      returnDescription = "",
      responses = {
          @RestResponse(description = "The default version is returned.", responseCode = HttpServletResponse.SC_OK)
      })
  @Operation(
      summary = "Get available versions",
      description = "Returns a list of available version as well as the default version."
  )
  @ApiResponse(responseCode = "200", description = "The default version is returned.")
  public Response getVersion() throws Exception {
    List<String> versions = new ArrayList<>();
    versions.add(ApiVersion.VERSION_1_0_0.toString());
    versions.add(ApiVersion.VERSION_1_1_0.toString());
    versions.add(ApiVersion.VERSION_1_2_0.toString());
    versions.add(ApiVersion.VERSION_1_3_0.toString());
    versions.add(ApiVersion.VERSION_1_4_0.toString());
    versions.add(ApiVersion.VERSION_1_5_0.toString());
    versions.add(ApiVersion.VERSION_1_6_0.toString());
    versions.add(ApiVersion.VERSION_1_7_0.toString());
    versions.add(ApiVersion.VERSION_1_8_0.toString());
    versions.add(ApiVersion.VERSION_1_9_0.toString());
    versions.add(ApiVersion.VERSION_1_10_0.toString());
    versions.add(ApiVersion.VERSION_1_11_0.toString());
    versions.add(ApiVersion.VERSION_1_12_0.toString());
    JsonObject json = new JsonObject();
    json.add("versions", collectionToJsonArray(versions));
    json.addProperty("default", ApiVersion.CURRENT_VERSION.toString());
    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, json.toString());
  }

  @GET
  @Path("version/default")
  @RestQuery(
      name = "getversiondefault",
      description = "Returns the default version.",
      returnDescription = "",
      responses = {
          @RestResponse(description = "The default version is returned.", responseCode = HttpServletResponse.SC_OK)
      })
  @Operation(summary = "Get default version", description = "Returns the default version.")
  @ApiResponse(responseCode = "200", description = "The default version is returned.")
  @Schema(name = "Version")
  public Response getVersionDefault() throws Exception {
    JsonObject json = new JsonObject();
    json.addProperty("default", ApiVersion.CURRENT_VERSION.toString());
    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, json.toString());
  }

}
