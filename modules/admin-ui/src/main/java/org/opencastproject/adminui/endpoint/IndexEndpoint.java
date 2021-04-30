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

import org.opencastproject.adminui.index.AdminUISearchIndex;
import org.opencastproject.index.rebuild.IndexRebuildService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.ComponentContext;
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
 * The index endpoint allows the management of the elastic search index.
 */
@Path("/")
@RestService(name = "adminuiIndexService", title = "Admin UI Index Service",
  abstractText = "Provides resources and operations related to the Admin UI's elastic search index",
  notes = { "This service offers the event CRUD Operations for the admin UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public class IndexEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(IndexEndpoint.class);

  /** The executor service */
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  /** The admin UI index */
  private AdminUISearchIndex adminUISearchIndex;

  /** The security service */
  protected SecurityService securityService = null;

  private IndexRebuildService indexRebuildService = null;

  /**
   * OSGI DI
   */
  public void setAdminUISearchIndex(AdminUISearchIndex adminUISearchIndex) {
    this.adminUISearchIndex = adminUISearchIndex;
  }

  public void setIndexRebuildService(IndexRebuildService indexRebuildService) {
    this.indexRebuildService = indexRebuildService;
  }

  /**
   * OSGI DI
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  public void activate(ComponentContext cc) {
    logger.info("Activate IndexEndpoint");
  }

  @POST
  @Path("clearIndex")
  @RestQuery(name = "clearIndex", description = "Clear the Admin UI index",
    returnDescription = "OK if index is cleared", responses = {
    @RestResponse(description = "Index is cleared", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "Unable to clear index", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) })
  public Response clearIndex() {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
      securityService.getUser());
    return securityContext.runInContext(() -> {
      try {
        logger.info("Clear the Admin UI index");
        adminUISearchIndex.clear();
        return R.ok();
      } catch (Throwable t) {
        logger.error("Clearing the Admin UI index failed", t);
        return R.serverError();
      }
    });
  }

  @POST
  @Path("recreateIndex/{service}")
  @RestQuery(name = "recreateIndexFromService",
    description = "Repopulates the Admin UI Index from an specific service",
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
        indexRebuildService.rebuildIndex(adminUISearchIndex, service);
      } catch (Throwable t) {
        logger.error("Repopulating the index failed", t);
      }
    }));
    return R.ok();
  }

  @POST
  @Path("recreateIndex")
  @RestQuery(name = "recreateIndex", description = "Clear and repopulates the Admin UI Index directly from the Services",
    returnDescription = "OK if repopulation has started", responses = {
    @RestResponse(description = "OK if repopulation has started", responseCode = HttpServletResponse.SC_OK) })
  public Response recreateIndex() {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    executor.execute(() -> securityContext.runInContext(() -> {
      try {
        logger.info("Starting to repopulate the index");
        indexRebuildService.rebuildIndex(adminUISearchIndex);
      } catch (Throwable t) {
        logger.error("Repopulating the index failed", t);
      }
    }));
    return R.ok();
  }
}
