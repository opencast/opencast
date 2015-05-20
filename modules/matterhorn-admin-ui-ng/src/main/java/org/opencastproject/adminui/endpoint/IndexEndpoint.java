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

import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.util.RestUtil.R;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * The index endpoint allows the management of the elastic search index.
 */
@Path("/")
@RestService(name = "adminuiIndexService", title = "Admin UI Index Service", notes = "", abstractText = "Provides resources and operations related to the Admin UI's elastic search index")
public class IndexEndpoint {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(IndexEndpoint.class);

  /** The executor service */
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  /** The admin UI index */
  private AdminUISearchIndex adminUISearchIndex;

  /** The security service */
  protected SecurityService securityService = null;

  /**
   * OSGI DI
   */
  public void setAdminUISearchIndex(AdminUISearchIndex adminUISearchIndex) {
    this.adminUISearchIndex = adminUISearchIndex;
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
  @Path("recreateIndex")
  @RestQuery(name = "recreateIndex", description = "Repopulates the Admin UI Index directly from the Services", returnDescription = "OK if repopulation has started", reponses = { @RestResponse(description = "OK if repopulation has started", responseCode = HttpServletResponse.SC_OK) })
  public Response getCatalogAdapters() {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    executor.execute(new Runnable() {
      @Override
      public void run() {
        securityContext.runInContext(new Effect0() {
          @Override
          protected void run() {
            try {
              logger.info("Starting to repopulate the index");
              adminUISearchIndex.recreateIndex();
              logger.info("Finished repopulating the index");
            } catch (InterruptedException e) {
              logger.error("Repopulating the index was interrupted {}", ExceptionUtils.getStackTrace(e));
            } catch (CancellationException e) {
              logger.trace("Listening for index messages has been cancelled.");
            } catch (ExecutionException e) {
              logger.error("Repopulating the index failed to execute because {}", ExceptionUtils.getStackTrace(e));
            } catch (Throwable t) {
              logger.error("Repopulating the index failed because {}", ExceptionUtils.getStackTrace(t));
            }
          }
        });
      }
    });
    return R.ok();
  }

}
