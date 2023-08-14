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

package org.opencastproject.elasticsearch.index.endpoint;

import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.rebuild.IndexRebuildService;
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
 * The index endpoint allows the management of the elasticsearch index.
 */
@Path("/")
@RestService(name = "IndexEndpoint", title = "Index Endpoint",
    abstractText = "Provides operations related to the index that serves both the Admin UI and the External API",
    notes = {})
@Component(
        immediate = true,
        property = {
                "service.description=Index Endpoint",
                "opencast.service.type=org.opencastproject.elasticsearch.index.endpoint",
                "opencast.service.path=/index"
        },
        service = { IndexEndpoint.class }
)
public class IndexEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(IndexEndpoint.class);

  /** The executor service */
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  /** The index */
  private ElasticsearchIndex elasticsearchIndex;

  /** The security service */
  protected SecurityService securityService = null;

  /** The index rebuild service **/
  private IndexRebuildService indexRebuildService = null;

  @Reference
  public void setElasticsearchIndex(ElasticsearchIndex elasticsearchIndex) {
    this.elasticsearchIndex = elasticsearchIndex;
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
    logger.info("Activate IndexEndpoint");
  }

  @POST
  @Path("clear")
  @RestQuery(name = "clearIndex", description = "Clear the index",
      returnDescription = "OK if index is cleared", responses = {
      @RestResponse(description = "Index is cleared", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "Unable to clear index",
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) })
  public Response clearIndex() {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
        securityService.getUser());
    return securityContext.runInContext(() -> {
      try {
        logger.info("Clear the index");
        elasticsearchIndex.clear();
        return R.ok();
      } catch (Throwable t) {
        logger.error("Clearing the index failed", t);
        return R.serverError();
      }
    });
  }

  @POST
  @Path("rebuild/{service}")
  @RestQuery(name = "partiallyRebuildIndex",
      description = "Repopulates the Index from an specific service",
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
        indexRebuildService.rebuildIndex(elasticsearchIndex, service);
      } catch (Throwable t) {
        logger.error("Repopulating the index failed", t);
      }
    }));
    return R.ok();
  }

  @POST
  @Path("rebuild")
  @RestQuery(name = "rebuild", description = "Clear and repopulates the Index directly from the "
          + "Services",
      returnDescription = "OK if repopulation has started", responses = {
      @RestResponse(description = "OK if repopulation has started", responseCode = HttpServletResponse.SC_OK) })
  public Response rebuildIndex() {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    executor.execute(() -> securityContext.runInContext(() -> {
      try {
        logger.info("Starting to repopulate the index");
        indexRebuildService.rebuildIndex(elasticsearchIndex);
      } catch (Throwable t) {
        logger.error("Repopulating the index failed", t);
      }
    }));
    return R.ok();
  }

  @POST
  @Path("resume/{service}")
  @RestQuery(name = "resumeIndexRebuild",
          description = "Starts repopulating the Index from an specific service and will then continue with the rest "
                  + "of the services that come afterwards",
          returnDescription = "OK if repopulation has started", pathParameters = {
          @RestParameter(name = "service", isRequired = true, description = "The service to start recreating the index "
                  + "from. "
                  + "The available services are: Themes, Series, Scheduler, Workflow, AssetManager and Comments. "
                  + "All services that come after the specified service in the order above will also run.",
                  type = RestParameter.Type.STRING) }, responses = {
          @RestResponse(description = "OK if repopulation has started", responseCode = HttpServletResponse.SC_OK) })
  public Response resumeIndexRebuild(@PathParam("service") final String service) {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    executor.execute(() -> securityContext.runInContext(() -> {
      try {
        logger.info("Resume repopulating the index from service {}", service);
        indexRebuildService.resumeIndexRebuild(elasticsearchIndex, service);
      } catch (Throwable t) {
        logger.error("Repopulating the index failed", t);
      }
    }));
    return R.ok();
  }
}
