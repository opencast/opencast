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

package org.opencastproject.api.index.endpoint;

import org.opencastproject.api.index.ApiIndex;
import org.opencastproject.api.index.rebuild.IndexRebuildService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * The index endpoint allows the management of the API index.
 */
@Path("/")

@RestService(name = "ApiIndexEndpoint", title = "API Index Endpoint",
    abstractText = "Provides operations related to the API index that serves both the Admin UI and the External API",
    notes = {})
@Component(
        immediate = true,
        service = ApiIndexEndpoint.class,
        property = {
                "service.description=API Index Endpoint",
                "opencast.service.type=org.opencastproject.api.index.endpoint",
                "opencast.service.path=/index",
        }
)
public class ApiIndexEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ApiIndexEndpoint.class);

  /** The executor service */
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  /** The Api index */
  private ApiIndex apiIndex;

  /** The security service */
  protected SecurityService securityService = null;

  /** The index rebuild service **/
  private IndexRebuildService indexRebuildService = null;

  @Reference
  public void setApiIndex(ApiIndex apiIndex) {
    this.apiIndex = apiIndex;
  }

  @Reference
  public void setIndexRebuildService(IndexRebuildService indexRebuildService) {
    this.indexRebuildService = indexRebuildService;
  }

  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Activate
  public void activate() {
    logger.info("Activate ApiIndexEndpoint");
  }

  @POST
  @Path("clear")
  @RestQuery(name = "clearIndex", description = "Clear the API index",
      returnDescription = "OK if index is cleared", responses = {
      @RestResponse(description = "Index is cleared", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "Unable to clear index",
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) })
  public Response clearIndex() {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
        securityService.getUser());
    return securityContext.runInContext(() -> {
      try {
        logger.info("Clear the Api index");
        apiIndex.clear();
        return R.ok();
      } catch (Throwable t) {
        logger.error("Clearing the API index failed", t);
        return R.serverError();
      }
    });
  }

  @POST
  @Path("rebuild/{service}")
  @RestQuery(name = "partiallyRebuildIndex",
      description = "Repopulates the API Index from an specific service",
      returnDescription = "OK if repopulation has started", pathParameters = {
      @RestParameter(name = "service", isRequired = true, description = "The service to recreate index from. "
        + "The available services are: Themes, Series, Scheduler, Workflow, AssetManager and Comments. "
        + "The service order (see above) is very important! Make sure, you do not run index rebuild for more than one "
        + "service at a time!",
        type = RestParameter.Type.STRING) }, responses = {
      @RestResponse(description = "OK if repopulation has started", responseCode = HttpServletResponse.SC_OK) })
  public Response partiallyRebuildIndex(@PathParam("service") final String service) {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
        securityService.getUser());
    executor.execute(() -> securityContext.runInContext(() -> {
      try {
        logger.info("Starting to repopulate the index from service {}", service);
        indexRebuildService.rebuildIndex(apiIndex, service);
      } catch (Throwable t) {
        logger.error("Repopulating the index failed", t);
      }
    }));
    return R.ok();
  }

  @POST
  @Path("rebuild")
  @RestQuery(name = "rebuild", description = "Clear and repopulates the API Index directly from the "
          + "Services",
      returnDescription = "OK if repopulation has started", responses = {
      @RestResponse(description = "OK if repopulation has started", responseCode = HttpServletResponse.SC_OK) })
  public Response rebuildIndex() {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    executor.execute(() -> securityContext.runInContext(() -> {
      try {
        logger.info("Starting to repopulate the index");
        indexRebuildService.rebuildIndex(apiIndex);
      } catch (Throwable t) {
        logger.error("Repopulating the index failed", t);
      }
    }));
    return R.ok();
  }
}
