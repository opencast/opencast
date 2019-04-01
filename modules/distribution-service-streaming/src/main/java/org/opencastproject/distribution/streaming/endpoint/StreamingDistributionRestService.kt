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

package org.opencastproject.distribution.streaming.endpoint

import javax.servlet.http.HttpServletResponse.SC_NO_CONTENT
import javax.servlet.http.HttpServletResponse.SC_OK
import javax.ws.rs.core.Response.status

import org.opencastproject.distribution.api.StreamingDistributionService
import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestParameter.Type
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.osgi.service.component.ComponentContext
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
 * Rest endpoint for distributing media to the local streaming distribution channel.
 */
@Path("/")
@RestService(name = "localdistributionservice", title = "Local Distribution Service", abstractText = "This service distributes media packages to the Opencast feed and engage services.", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is " + "not working and is either restarting or has failed", "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"])
class StreamingDistributionRestService : AbstractJobProducerEndpoint() {

    /** The distribution service  */
    protected var service: StreamingDistributionService

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
     * OSGi activation callback
     *
     * @param cc
     * this component's context
     */
    fun activate(cc: ComponentContext) {}

    /**
     * @param service
     * the service to set
     */
    fun setService(service: StreamingDistributionService) {
        this.service = service
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "distribute", description = "Distribute a media package element to this distribution channel", returnDescription = "The job that can be used to track the distribution", restParameters = [RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT), RestParameter(name = "channelId", isRequired = true, description = "The publication channel ID", type = Type.TEXT), RestParameter(name = "elementIds", isRequired = true, description = "The elements to distribute as Json Array['IdOne','IdTwo']", type = Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "An XML representation of the distribution job"), RestResponse(responseCode = SC_NO_CONTENT, description = "There is no streaming distribution service available")])
    @Throws(Exception::class)
    fun distribute(@FormParam("mediapackage") mediaPackageXml: String,
                   @FormParam("channelId") channelId: String, @FormParam("elementIds") elementIds: String): Response {
        var job: Job? = null
        try {
            val gson = Gson()
            val setElementIds = gson.fromJson<Set<String>>(elementIds, object : TypeToken<Set<String>>() {

            }.type)
            val mediapackage = MediaPackageParser.getFromXml(mediaPackageXml)
            job = service.distribute(channelId, mediapackage, setElementIds)
            return if (job == null) Response.noContent().build() else Response.ok(JaxbJob(job)).build()

        } catch (e: IllegalArgumentException) {
            logger.debug("Unable to distribute element: {}", e.message)
            return status(Status.BAD_REQUEST).build()
        } catch (e: Exception) {
            logger.warn("Error distributing element", e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @POST
    @Path("/distributesync")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "distributesync", description = "Synchronously distribute a media package element to this distribution channel", returnDescription = "The distribution", restParameters = [RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT), RestParameter(name = "channelId", isRequired = true, description = "The publication channel ID", type = Type.TEXT), RestParameter(name = "elementIds", isRequired = true, description = "The elements to distribute as Json Array['IdOne','IdTwo']", type = Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "An XML representation of the distribution"), RestResponse(responseCode = SC_NO_CONTENT, description = "There is no streaming distribution service available")])
    @Throws(Exception::class)
    fun distributeSync(@FormParam("mediapackage") mediaPackageXml: String,
                       @FormParam("channelId") channelId: String, @FormParam("elementIds") elementIds: String): Response {
        var result: List<MediaPackageElement>? = null
        try {
            val gson = Gson()
            val setElementIds = gson.fromJson<Set<String>>(elementIds, object : TypeToken<Set<String>>() {

            }.type)
            val mediapackage = MediaPackageParser.getFromXml(mediaPackageXml)
            result = service.distributeSync(channelId, mediapackage, setElementIds)
            return if (result == null || result.isEmpty()) Response.noContent().build() else Response.ok(MediaPackageElementParser.getArrayAsXml(result)).build()

        } catch (e: IllegalArgumentException) {
            logger.debug("Unable to distribute element: {}", e.message)
            return status(Status.BAD_REQUEST).build()
        } catch (e: Exception) {
            logger.warn("Error distributing element", e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @POST
    @Path("/retract")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "retract", description = "Retract a media package element from this distribution channel", returnDescription = "The job that can be used to track the retraction", restParameters = [RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT), RestParameter(name = "channelId", isRequired = true, description = "The publication channel ID", type = Type.TEXT), RestParameter(name = "elementIds", isRequired = true, description = "The elements to retract as Json Array['IdOne','IdTwo']", type = Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "An XML representation of the retraction job"), RestResponse(responseCode = SC_NO_CONTENT, description = "There is no streaming distribution service available")])
    @Throws(Exception::class)
    fun retract(@FormParam("mediapackage") mediaPackageXml: String, @FormParam("channelId") channelId: String,
                @FormParam("elementIds") elementIds: String): Response {
        var job: Job? = null
        try {
            val gson = Gson()
            val setElementIds = gson.fromJson<Set<String>>(elementIds, object : TypeToken<Set<String>>() {

            }.type)
            val mediapackage = MediaPackageParser.getFromXml(mediaPackageXml)
            job = service.retract(channelId, mediapackage, setElementIds)
            return if (job == null) Response.noContent().build() else Response.ok(JaxbJob(job)).build()

        } catch (e: IllegalArgumentException) {
            logger.debug("Unable to retract element: {}", e.message)
            return status(Status.BAD_REQUEST).build()
        } catch (e: Exception) {
            logger.warn("Unable to retract mediapackage '{}' from streaming channel: {}", mediaPackageXml, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @POST
    @Path("/retractsync")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "retractsync", description = "Synchronously retract a media package element from this distribution channel", returnDescription = "The retraction", restParameters = [RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT), RestParameter(name = "channelId", isRequired = true, description = "The publication channel ID", type = Type.TEXT), RestParameter(name = "elementIds", isRequired = true, description = "The elements to retract as Json Array['IdOne','IdTwo']", type = Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "An XML representation of the retraction"), RestResponse(responseCode = SC_NO_CONTENT, description = "There is no streaming distribution service available")])
    @Throws(Exception::class)
    fun retractSync(@FormParam("mediapackage") mediaPackageXml: String, @FormParam("channelId") channelId: String,
                    @FormParam("elementIds") elementIds: String): Response {
        var result: List<MediaPackageElement>? = null
        try {
            val gson = Gson()
            val setElementIds = gson.fromJson<Set<String>>(elementIds, object : TypeToken<Set<String>>() {

            }.type)
            val mediapackage = MediaPackageParser.getFromXml(mediaPackageXml)
            result = service.retractSync(channelId, mediapackage, setElementIds)
            return if (result == null || result.isEmpty()) Response.noContent().build() else Response.ok(MediaPackageElementParser.getArrayAsXml(result)).build()

        } catch (e: IllegalArgumentException) {
            logger.debug("Unable to retract element: {}", e.message)
            return status(Status.BAD_REQUEST).build()
        } catch (e: Exception) {
            logger.warn("Unable to retract mediapackage '{}' from streaming channel: {}", mediaPackageXml, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
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
        private val logger = LoggerFactory.getLogger(StreamingDistributionRestService::class.java)
    }

}
