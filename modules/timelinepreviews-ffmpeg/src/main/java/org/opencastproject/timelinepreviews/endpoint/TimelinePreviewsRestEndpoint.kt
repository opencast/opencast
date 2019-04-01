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

package org.opencastproject.timelinepreviews.endpoint

import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.timelinepreviews.api.TimelinePreviewsException
import org.opencastproject.timelinepreviews.api.TimelinePreviewsService
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * The REST endpoint for the [TimelinePreviewsService] service
 */
@Path("/")
@RestService(name = "TimelinePreviewsEndpoint", title = "Timeline Previews Service REST Endpoint", abstractText = "This service generates timeline preview images from media files that contain a video.", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is " + "not working and is either restarting or has failed", "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time "
        + "when the error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"])
class TimelinePreviewsRestEndpoint : AbstractJobProducerEndpoint() {

    /** The rest docs  */
    @get:GET
    @get:Produces(MediaType.TEXT_HTML)
    @get:Path("docs")
    var docs: String? = null
        protected set

    /** The timeline previews service  */
    protected var service: TimelinePreviewsService

    /** The service registry  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.rest.AbstractJobProducerEndpoint.getServiceRegistry
     */
    /**
     * Callback from the OSGi declarative services to set the service registry.
     *
     * @param serviceRegistry
     * the service registry
     */
    override var serviceRegistry: ServiceRegistry? = null
        protected set

    /**
     * Sets the timeline previews service
     *
     * @param timelinePreviewsService
     * the timeline previews service
     */
    protected fun setTimelinePreviewsService(timelinePreviewsService: TimelinePreviewsService) {
        this.service = timelinePreviewsService
    }

    /**
     * Generates timeline preview images for a track.
     *
     * @param trackAsXml
     * the track xml to create preview images for
     * @return the job in the body of a JAX-RS response
     * @throws Exception
     */
    @POST
    @Path("/create")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "create", description = "Create preview images from the given track.", restParameters = [RestParameter(description = "The track to generate timeline preview images for.", isRequired = true, name = "track", type = RestParameter.Type.FILE), RestParameter(description = "The number of timeline preview images to generate.", isRequired = true, name = "imageCount", type = RestParameter.Type.INTEGER)], reponses = [RestResponse(description = "Timeline previews job successfully created", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "The given track can't be parsed.", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "Internal server error.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "The job ID to use when polling for the resulting media package attachment, " + "that contains the generated timeline preview images.")
    @Throws(Exception::class)
    fun createTimelinePreviews(@FormParam("track") trackAsXml: String, @FormParam("imageCount") imageCount: Int): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(trackAsXml))
            return Response.status(Response.Status.BAD_REQUEST).entity("track must not be null").build()

        // Deserialize the track
        val sourceTrack = MediaPackageElementParser.getFromXml(trackAsXml)
        if (!Track.TYPE.equals(sourceTrack.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("mediapackage element must be of type track").build()

        try {
            val job = service.createTimelinePreviewImages(sourceTrack as Track, imageCount)
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: TimelinePreviewsException) {
            logger.warn("Generation of timeline preview images failed: " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.rest.AbstractJobProducerEndpoint.getService
     */
    override fun getService(): JobProducer? {
        return if (service is JobProducer)
            service as JobProducer
        else
            null
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(TimelinePreviewsRestEndpoint::class.java)
    }

}
