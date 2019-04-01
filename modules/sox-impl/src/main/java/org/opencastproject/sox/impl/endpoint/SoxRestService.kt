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

package org.opencastproject.sox.impl.endpoint

import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.sox.api.SoxException
import org.opencastproject.sox.api.SoxService
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestParameter.Type
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.apache.commons.lang3.StringUtils
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
 * A REST endpoint delegating functionality to the [SoxService]
 */
@Path("/")
@RestService(name = "sox", title = "Sox", abstractText = "This service creates and augments Opencast media packages that include media tracks, metadata " + "catalogs and attachments.", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is " + "not working and is either restarting or has failed", "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"])
class SoxRestService : AbstractJobProducerEndpoint() {

    /** The rest documentation  */
    protected var docs: String? = null

    /** The base server URL  */
    protected var serverUrl: String

    /** The composer service  */
    protected var soxService: SoxService? = null

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
     * {@inheritDoc}
     *
     * @see org.opencastproject.rest.AbstractJobProducerEndpoint.getService
     */
    override val service: JobProducer?
        get() = if (soxService is JobProducer)
            soxService as JobProducer?
        else
            null

    /**
     * Sets the SoX service.
     *
     * @param soxService
     * the SoX service
     */
    fun setSoxService(soxService: SoxService) {
        this.soxService = soxService
    }

    /**
     * Callback from OSGi that is called when this service is activated.
     *
     * @param cc
     * OSGi component context
     */
    fun activate(cc: ComponentContext?) {
        if (cc == null || cc.bundleContext.getProperty("org.opencastproject.server.url") == null) {
            serverUrl = UrlSupport.DEFAULT_BASE_URL
        } else {
            serverUrl = cc.bundleContext.getProperty("org.opencastproject.server.url")
        }
    }

    /**
     * Analyze an audio track.
     *
     * @param sourceAudioTrackAsXml
     * The source audio track
     * @return A response containing the job for this audio analyzing job in the response body.
     * @throws Exception
     */
    @POST
    @Path("analyze")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "analyze", description = "Starts an audio analyzing process", restParameters = [RestParameter(description = "The track just containing the audio stream", isRequired = true, name = "sourceAudioTrack", type = Type.TEXT)], reponses = [RestResponse(description = "Results in an xml document containing the job for the analyzing task", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceAudioTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun analyze(@FormParam("sourceAudioTrack") sourceAudioTrackAsXml: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceAudioTrackAsXml))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceAudioTrack must not be null").build()

        // Deserialize the track
        val sourceTrack = MediaPackageElementParser.getFromXml(sourceAudioTrackAsXml)
        if (!Track.TYPE.equals(sourceTrack.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceAudioTrack element must be of type track")
                    .build()

        try {
            // Asynchronously analyze the specified audio track
            val job = soxService!!.analyze(sourceTrack as Track)
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: SoxException) {
            logger.warn("Unable to analyze the audio track: " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Normalize an audio track.
     *
     * @param sourceAudioTrackAsXml
     * The source audio track
     * @param targetRmsLevDb
     * the target RMS level dB
     * @return A response containing the job for this encoding job in the response body.
     * @throws Exception
     */
    @POST
    @Path("normalize")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "normalize", description = "Starts audio normalization process", restParameters = [RestParameter(description = "The track containing the audio stream", isRequired = true, name = "sourceAudioTrack", type = Type.TEXT), RestParameter(description = "The target RMS level dB", isRequired = true, name = "targetRmsLevDb", type = Type.INTEGER)], reponses = [RestResponse(description = "Results in an xml document containing the job for the audio normalization task", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceAudioTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun normalize(@FormParam("sourceAudioTrack") sourceAudioTrackAsXml: String,
                  @FormParam("targetRmsLevDb") targetRmsLevDb: Float?): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceAudioTrackAsXml))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceAudioTrack must not be null").build()
        if (targetRmsLevDb == null)
            return Response.status(Response.Status.BAD_REQUEST).entity("targetRmsLevDb must not be null").build()

        // Deserialize the track
        val sourceTrack = MediaPackageElementParser.getFromXml(sourceAudioTrackAsXml)
        if (!Track.TYPE.equals(sourceTrack.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceAudioTrack element must be of type track")
                    .build()

        try {
            // Asynchronously normalyze the specified audio track
            val job = soxService!!.normalize(sourceTrack as Track, targetRmsLevDb)
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: SoxException) {
            logger.warn("Unable to normalize the track: " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(SoxRestService::class.java)
    }

}
