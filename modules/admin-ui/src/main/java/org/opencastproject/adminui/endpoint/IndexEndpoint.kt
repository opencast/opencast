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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.adminui.endpoint

import org.opencastproject.adminui.index.AdminUISearchIndex
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.util.SecurityContext
import org.opencastproject.util.RestUtil.R
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Response

/**
 * The index endpoint allows the management of the elastic search index.
 */
@Path("/")
@RestService(name = "adminuiIndexService", title = "Admin UI Index Service", abstractText = "Provides resources and operations related to the Admin UI's elastic search index", notes = ["This service offers the event CRUD Operations for the admin UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
class IndexEndpoint {

    /** The executor service  */
    private val executor = Executors.newSingleThreadExecutor()

    /** The admin UI index  */
    private var adminUISearchIndex: AdminUISearchIndex? = null

    /** The security service  */
    protected var securityService: SecurityService? = null

    /**
     * OSGI DI
     */
    fun setAdminUISearchIndex(adminUISearchIndex: AdminUISearchIndex) {
        this.adminUISearchIndex = adminUISearchIndex
    }

    /**
     * OSGI DI
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    fun activate(cc: ComponentContext) {
        logger.info("Activate IndexEndpoint")
    }

    @POST
    @Path("clearIndex")
    @RestQuery(name = "clearIndex", description = "Clear the Admin UI index", returnDescription = "OK if index is cleared", reponses = [RestResponse(description = "Index is cleared", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Unable to clear index", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)])
    fun clearIndex(): Response {
        val securityContext = SecurityContext(securityService!!, securityService!!.organization,
                securityService!!.user)
        return securityContext.runInContext<Response>({
            try {
                logger.info("Clear the Admin UI index")
                adminUISearchIndex!!.clear()
                return@securityContext.runInContext R . ok ()
            } catch (t: Throwable) {
                logger.error("Clearing the Admin UI index failed", t)
                return@securityContext.runInContext R . serverError ()
            }
        })
    }

    @POST
    @Path("recreateIndex/{service}")
    @RestQuery(name = "recreateIndexFromService", description = "Repopulates the Admin UI Index from an specific service", returnDescription = "OK if repopulation has started", pathParameters = [RestParameter(name = "service", isRequired = true, description = "The service to recreate index from. "
            + "The available services are: Groups, Acl, Themes, Series, Scheduler, Workflow, AssetManager and Comments. "
            + "The service order (see above) is very important! Make sure, you do not run index rebuild for more than one "
            + "service at a time!", type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "OK if repopulation has started", responseCode = HttpServletResponse.SC_OK)])
    fun recreateIndexFromService(@PathParam("service") service: String): Response {
        val securityContext = SecurityContext(securityService!!, securityService!!.organization,
                securityService!!.user)
        executor.execute {
            securityContext.runInContext({
                try {
                    logger.info("Starting to repopulate the index from service {}", service)
                    adminUISearchIndex!!.recreateIndex(service)
                } catch (e: InterruptedException) {
                    logger.error("Repopulating the index was interrupted", e)
                } catch (e: CancellationException) {
                    logger.trace("Listening for index messages has been cancelled.")
                } catch (e: ExecutionException) {
                    logger.error("Repopulating the index failed to execute", e)
                } catch (t: Throwable) {
                    logger.error("Repopulating the index failed", t)
                }
            })
        }
        return R.ok()
    }

    @POST
    @Path("recreateIndex")
    @RestQuery(name = "recreateIndex", description = "Clear and repopulates the Admin UI Index directly from the Services", returnDescription = "OK if repopulation has started", reponses = [RestResponse(description = "OK if repopulation has started", responseCode = HttpServletResponse.SC_OK)])
    fun recreateIndex(): Response {
        val securityContext = SecurityContext(securityService!!, securityService!!.organization,
                securityService!!.user)
        executor.execute {
            securityContext.runInContext({
                try {
                    logger.info("Starting to repopulate the index")
                    adminUISearchIndex!!.recreateIndex()
                } catch (e: InterruptedException) {
                    logger.error("Repopulating the index was interrupted", e)
                } catch (e: CancellationException) {
                    logger.trace("Listening for index messages has been cancelled.")
                } catch (e: ExecutionException) {
                    logger.error("Repopulating the index failed to execute", e)
                } catch (t: Throwable) {
                    logger.error("Repopulating the index failed", t)
                }
            })
        }
        return R.ok()
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(IndexEndpoint::class.java)
    }
}
