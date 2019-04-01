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

package org.opencastproject.publication.youtube.endpoint

import javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import javax.servlet.http.HttpServletResponse.SC_OK
import org.opencastproject.util.RestUtil.R.badRequest
import org.opencastproject.util.RestUtil.R.serverError

import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.publication.api.YouTubePublicationService
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestParameter.Type
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.ws.rs.FormParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

/**
 * Rest endpoint for publishing media to youtube.
 */
@Path("/")
@RestService(name = "youtubepublicationservice", title = "YouTube Publication Service", abstractText = "", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is " + "not working and is either restarting or has failed", "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"])
class YouTubePublicationRestService : AbstractJobProducerEndpoint() {

    private val logger = LoggerFactory.getLogger(YouTubePublicationRestService::class.java)

    protected var service: YouTubePublicationService

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.rest.AbstractJobProducerEndpoint.getServiceRegistry
     */
    /**
     * Callback from the OSGi declarative services to set the service registry
     *
     * @param serviceRegistry
     * the service registry
     */
    override var serviceRegistry: ServiceRegistry? = null
        protected set

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "publish", description = "Publish a media package element to youtube publication channel", returnDescription = "The job that can be used to track the publication", restParameters = [RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT), RestParameter(name = "elementId", isRequired = true, description = "The element to publish", type = Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "An XML representation of the publication job"), RestResponse(responseCode = SC_BAD_REQUEST, description = "elementId does not reference a track")])
    fun publish(@FormParam("mediapackage") mediaPackageXml: String, @FormParam("elementId") elementId: String): Response {
        val job: Job
        try {
            val mediapackage = MediaPackageParser.getFromXml(mediaPackageXml)
            val track = mediapackage.getTrack(elementId)
            if (track != null) {
                job = service.publish(mediapackage, track)
            } else {
                return badRequest()
            }
        } catch (e: Exception) {
            logger.warn("Error publishing element '{}' to YouTube", elementId, e)
            return serverError()
        }

        return Response.ok(JaxbJob(job)).build()
    }

    @POST
    @Path("/retract")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "retract", description = "Retract a media package from the youtube publication channel", returnDescription = "The job that can be used to track the retraction", restParameters = [RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT)], reponses = [RestResponse(responseCode = SC_OK, description = "An XML representation of the retraction job")])
    fun retract(@FormParam("mediapackage") mediaPackageXml: String): Response {
        val job: Job
        try {
            val mediapackage = MediaPackageParser.getFromXml(mediaPackageXml)
            job = service.retract(mediapackage)
        } catch (e: Exception) {
            logger.warn("Unable to retract mediapackage '{}' from YouTube: {}", mediaPackageXml, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

        return Response.ok(JaxbJob(job)).build()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.rest.AbstractJobProducerEndpoint.getService
     */
    override fun getService(): JobProducer? {
        return if (service is JobProducer) service as JobProducer else null
    }

    /**
     * Callback from OSGi to set a reference to the youtube publication service.
     *
     * @param service
     * the service to set
     */
    protected fun setService(service: YouTubePublicationService) {
        this.service = service
    }

}
