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
package org.opencastproject.external.endpoint;

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.opencastproject.userdirectory.UserIdRoleProvider.getUserIdRole;
import static org.opencastproject.util.RestUtil.getEndpointUrl;

import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiVersion;
import org.opencastproject.external.index.ExternalIndex;
import org.opencastproject.index.rebuild.IndexRebuildService;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JString;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;
import com.entwinemedia.fn.data.json.SimpleSerializer;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The external service endpoint acts as a location for external apis to query the current server of the external
 * supported API.
 */
@Path("/")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_0_0, ApiMediaType.VERSION_1_1_0, ApiMediaType.VERSION_1_2_0,
            ApiMediaType.VERSION_1_3_0, ApiMediaType.VERSION_1_4_0, ApiMediaType.VERSION_1_5_0 })
@RestService(name = "externalapiservice", title = "External API Service", notes = {},
             abstractText = "Provides a location for external apis to query the current server of the API.")
public class BaseEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(BaseEndpoint.class);

  /** The executor service */
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  /** The json serializer */
  private static final SimpleSerializer serializer = new SimpleSerializer();

  /** Base URL of this endpoint */
  protected String endpointBaseUrl;

  /* OSGi service references */
  private SecurityService securityService;
  private ExternalIndex externalIndex;

  private IndexRebuildService indexRebuildService = null;

  /** OSGi DI */
  void setExternalIndex(ExternalIndex externalIndex) {
    this.externalIndex = externalIndex;
  }

  /** OSGi DI */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void setIndexRebuildService(IndexRebuildService indexRebuildService) {
    this.indexRebuildService = indexRebuildService;
  }

  /** OSGi activation method */
  void activate(ComponentContext cc) {
    logger.info("Activating External API - Base Endpoint");

    final Tuple<String, String> endpointUrl = getEndpointUrl(cc, OpencastConstants.EXTERNAL_API_URL_ORG_PROPERTY,
            RestConstants.SERVICE_PATH_PROPERTY);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
    logger.debug("Configured service endpoint is {}", endpointBaseUrl);
  }

  @GET
  @Path("")
  @RestQuery(name = "getendpointinfo", description = "Returns key characteristics of the API such as the server name and the default version.", returnDescription = "", responses = {
          @RestResponse(description = "The api information is returned.", responseCode = HttpServletResponse.SC_OK) })
  public Response getEndpointInfo() {
    Organization organization = securityService.getOrganization();
    String orgExternalAPIUrl = organization.getProperties().get(OpencastConstants.EXTERNAL_API_URL_ORG_PROPERTY);

    JString url;
    if (StringUtils.isNotBlank(orgExternalAPIUrl)) {
      url = v(orgExternalAPIUrl);
    } else {
      url = v(endpointBaseUrl);
    }

    JValue json = obj(f("url", url), f("version", v(ApiVersion.CURRENT_VERSION.toString())));
    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, serializer.toJson(json));
  }

  @GET
  @Path("info/me")
  @RestQuery(name = "getuserinfo", description = "Returns information on the logged in user.", returnDescription = "", responses = {
          @RestResponse(description = "The user information is returned.", responseCode = HttpServletResponse.SC_OK) })
  public Response getUserInfo() {
    final User user = securityService.getUser();

    JValue json = obj(f("email", v(user.getEmail(), Jsons.BLANK)), f("name", v(user.getName())),
            f("provider", v(user.getProvider())), f("userrole", v(getUserIdRole(user.getUsername()))),
            f("username", v(user.getUsername())));
    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, serializer.toJson(json));
  }

  @GET
  @Path("info/me/roles")
  @RestQuery(name = "getuserroles", description = "Returns current user's roles.", returnDescription = "", responses = {
          @RestResponse(description = "The set of roles is returned.", responseCode = HttpServletResponse.SC_OK) })
  public Response getUserRoles() {
    final User user = securityService.getUser();

    List<JValue> roles = new ArrayList<>();
    for (final Role role : user.getRoles()) {
      roles.add(v(role.getName()));
    }

    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, serializer.toJson(arr(roles)));
  }

  @GET
  @Path("info/organization")
  @RestQuery(name = "getorganizationinfo", description = "Returns the current organization.", returnDescription = "", responses = {
          @RestResponse(description = "The organization details are returned.", responseCode = HttpServletResponse.SC_OK) })
  public Response getOrganizationInfo() {
    final Organization org = securityService.getOrganization();

    JValue json = obj(f("adminRole", v(org.getAdminRole())), f("anonymousRole", v(org.getAnonymousRole())),
            f("id", v(org.getId())), f("name", v(org.getName())));

    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, serializer.toJson(json));
  }

  @GET
  @Path("info/organization/properties")
  @RestQuery(name = "getorganizationproperties", description = "Returns the current organization's properties.", returnDescription = "", responses = {
          @RestResponse(description = "The organization properties are returned.", responseCode = HttpServletResponse.SC_OK) })
  public Response getOrganizationProperties() {
    final Organization org = securityService.getOrganization();

    List<Field> props = new ArrayList<>();
    for (Entry<String, String> prop : org.getProperties().entrySet()) {
      props.add(f(prop.getKey(), v(prop.getValue(), Jsons.BLANK)));
    }

    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, serializer.toJson(obj(props)));
  }

  @GET
  @Path("info/organization/properties/engageuiurl")
  @RestQuery(name = "getorganizationpropertiesengageuiurl", description = "Returns the engage ui url property.", returnDescription = "", responses = {
          @RestResponse(description = "The engage ui url is returned.", responseCode = HttpServletResponse.SC_OK) })
  public Response getOrganizationPropertiesEngageUiUrl() {
    final Organization org = securityService.getOrganization();

    List<Field> props = new ArrayList<>();
    for (Entry<String, String> prop : org.getProperties().entrySet()) {
      if (prop.getKey().equals("org.opencastproject.engage.ui.url")) {
        props.add(f(prop.getKey(), v(prop.getValue(), Jsons.BLANK)));
        break;
      }
    }
    if (props.size() == 0) {
      props.add(f("org.opencastproject.engage.ui.url", v(UrlSupport.DEFAULT_BASE_URL, Jsons.BLANK)));
    }

    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, serializer.toJson(obj(props)));
  }

  @GET
  @Path("version")
  @RestQuery(name = "getversion", description = "Returns a list of available version as well as the default version.", returnDescription = "", responses = {
          @RestResponse(description = "The default version is returned.", responseCode = HttpServletResponse.SC_OK) })
  public Response getVersion() throws Exception {
    List<JValue> versions = new ArrayList<>();
    versions.add(v(ApiVersion.VERSION_1_0_0.toString()));
    versions.add(v(ApiVersion.VERSION_1_1_0.toString()));
    versions.add(v(ApiVersion.VERSION_1_2_0.toString()));
    versions.add(v(ApiVersion.VERSION_1_3_0.toString()));
    versions.add(v(ApiVersion.VERSION_1_4_0.toString()));
    versions.add(v(ApiVersion.VERSION_1_5_0.toString()));
    JValue json = obj(f("versions", arr(versions)), f("default", v(ApiVersion.CURRENT_VERSION.toString())));
    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, serializer.toJson(json));
  }

  @GET
  @Path("version/default")
  @RestQuery(name = "getversiondefault", description = "Returns the default version.", returnDescription = "", responses = {
          @RestResponse(description = "The default version is returned.", responseCode = HttpServletResponse.SC_OK) })
  public Response getVersionDefault() throws Exception {
    JValue json = obj(f("default", v(ApiVersion.CURRENT_VERSION.toString())));
    return RestUtil.R.ok(MediaType.APPLICATION_JSON_TYPE, serializer.toJson(json));
  }

  @POST
  @Path("clearIndex")
  @RestQuery(name = "clearIndex", description = "Clear the External index",
          returnDescription = "OK if index is cleared", responses = {
          @RestResponse(description = "Index is cleared", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Unable to clear index", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) })
  public Response clearIndex() {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    return securityContext.runInContext(() -> {
      try {
        logger.info("Clear the external index");
        externalIndex.clear();
        return R.ok();
      } catch (Throwable t) {
        logger.error("Clearing the external index failed", t);
        return R.serverError();
      }
    });
  }

  @POST
  @Path("recreateIndex/{service}")
  @RestQuery(name = "recreateIndexFromService",
          description = "Repopulates the external Index from an specific service",
          returnDescription = "OK if repopulation has started", pathParameters = {
          @RestParameter(name = "service", isRequired = true, description = "The service to recreate index from. "
                  + "The available services are: Groups, Acl, Themes, Series, Scheduler, Workflow, AssetManager and Comments. "
                  + "The service order (see above) is very important! Make sure, you do not run index rebuild for more than one "
                  + "service at a time!",
                  type = RestParameter.Type.STRING) }, responses = {
          @RestResponse(description = "OK if repopulation has started", responseCode = HttpServletResponse.SC_OK) })
  public Response recreateIndexFromService(@PathParam("service") final String service) {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    executor.execute(() -> securityContext.runInContext(() -> {
      try {
        logger.info("Starting to repopulate the index from service {}", service);
        indexRebuildService.rebuildIndex(externalIndex, service);
      } catch (Throwable t) {
        logger.error("Repopulating the index failed", t);
      }
    }));
    return R.ok();
  }

  @POST
  @Path("recreateIndex")
  @RestQuery(name = "recreateIndex", description = "Repopulates the External Index directly from the Services", returnDescription = "OK if repopulation has started", responses = {
          @RestResponse(description = "OK if repopulation has started", responseCode = HttpServletResponse.SC_OK) })
  public Response recreateIndex() {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    executor.execute(() -> securityContext.runInContext(() -> {
      try {
        logger.info("Starting to repopulate the external index");
        indexRebuildService.rebuildIndex(externalIndex);
        logger.info("Finished repopulating the external index");
      } catch (Throwable t) {
        logger.error("Repopulating the external index failed", t);
      }
    }));
    return R.ok();
  }

}
