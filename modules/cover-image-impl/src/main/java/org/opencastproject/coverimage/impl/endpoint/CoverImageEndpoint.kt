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

package org.opencastproject.coverimage.impl.endpoint

import org.opencastproject.coverimage.CoverImageException
import org.opencastproject.coverimage.CoverImageService
import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestParameter.Type
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.FormParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * REST endpoint for [CoverImageService]
 */
@Path("/")
@RestService(name = "coverimage", title = "Cover Image Service", abstractText = "This endpoint triggers generation of cover images", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is " + "not working and is either restarting or has failed", "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"])
class CoverImageEndpoint : AbstractJobProducerEndpoint() {

    /** Reference to the service registry service  */
    /**
     * OSGi callback to set the a reference to the service registry.
     *
     * @param serviceRegistry
     * the service registry
     */
    override var serviceRegistry: ServiceRegistry? = null
        protected set

    /** Reference to the cover image service  */
    private var coverImageService: CoverImageService? = null

    override val service: JobProducer?
        get() = if (coverImageService is JobProducer)
            coverImageService as JobProducer?
        else
            null

    @POST
    @Path("generate")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "generate", description = "Generates a cover image based on the given metadata", restParameters = [RestParameter(description = "Metadata XML", isRequired = false, name = "xml", type = Type.TEXT), RestParameter(description = "XSLT stylesheet", isRequired = true, name = "xsl", type = Type.TEXT), RestParameter(description = "Width of the cover image", isRequired = true, name = "width", type = Type.INTEGER, defaultValue = "1600"), RestParameter(description = "Height of the cover image", isRequired = true, name = "height", type = Type.INTEGER, defaultValue = "900"), RestParameter(description = "URI of poster image", isRequired = false, name = "posterimage", type = Type.STRING), RestParameter(description = "Flavor of target cover image", isRequired = true, name = "targetflavor", type = Type.STRING, defaultValue = "image/cover")], reponses = [RestResponse(description = "Results in an xml document containing the job for the cover image generation task", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    fun generateCoverImage(@FormParam("xml") xml: String, @FormParam("xsl") xsl: String,
                           @FormParam("width") width: String, @FormParam("height") height: String,
                           @FormParam("posterimage") posterFlavor: String, @FormParam("targetflavor") targetFlavor: String): Response {
        try {
            val job = coverImageService!!.generateCoverImage(xml, xsl, width, height, posterFlavor, targetFlavor)
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: CoverImageException) {
            logger.warn("Error while creating cover image job via REST endpoint: {}", e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * OSGi callback to set the a reference to the cover image service.
     *
     * @param coverImageService
     */
    protected fun setCoverImageService(coverImageService: CoverImageService) {
        this.coverImageService = coverImageService
    }

    /**
     * Callback from OSGi that is called when this service is activated.
     *
     * @param cc
     * OSGi component context
     */
    protected fun activate(cc: ComponentContext) {
        logger.info("Cover Image REST Endpoint started")
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(CoverImageEndpoint::class.java)
    }

}
