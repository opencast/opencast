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
package org.opencastproject.publication.oaipmh.endpoint

import javax.servlet.http.HttpServletResponse.SC_OK
import org.opencastproject.publication.api.OaiPmhPublicationService.SEPARATOR

import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.Publication
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestParameter.Type
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.HashSet
import java.util.regex.Pattern
import java.util.stream.Collectors

import javax.ws.rs.DefaultValue
import javax.ws.rs.FormParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

/**
 * Rest endpoint for publishing media to the OAI-PMH publication channel.
 */
@Path("/")
@RestService(name = "oaipmhpublicationservice", title = "OAI-PMH Publication Service", abstractText = "This service " + "publishes a media package element to the Opencast OAI-PMH channel.", notes = ["All paths above are "
        + "relative to the REST endpoint base (something like http://your.server/files).  If the service is down "
        + "or not working it will return a status 503, this means the the underlying service is not working and is "
        + "either restarting or has failed. A status code 500 means a general failure has occurred which is not "
        + "recoverable and was not anticipated. In other words, there is a bug!"])
open class OaiPmhPublicationRestService : AbstractJobProducerEndpoint() {

    /** The OAI-PMH publication service  */
    protected var service: OaiPmhPublicationService

    /** The service registry  */
    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.job.api.AbstractJobProducer.getServiceRegistry
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
     * @param service
     * the service to set
     */
    fun setService(service: OaiPmhPublicationService) {
        this.service = service
    }

    @POST
    @Path("/")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "publish", description = "Publish a media package element to this publication channel", returnDescription = "The job that can be used to track the publication", restParameters = [RestParameter(name = "mediapackage", isRequired = true, description = "The media package", type = Type.TEXT), RestParameter(name = "channel", isRequired = true, description = "The channel name", type = Type.STRING), RestParameter(name = "downloadElementIds", isRequired = true, description = "The elements to publish to download separated by '$SEPARATOR'", type = Type.STRING), RestParameter(name = "streamingElementIds", isRequired = true, description = "The elements to publish to streaming separated by '$SEPARATOR'", type = Type.STRING), RestParameter(name = "checkAvailability", isRequired = false, description = "Whether to check for availability", type = Type.BOOLEAN, defaultValue = "true")], reponses = [RestResponse(responseCode = SC_OK, description = "An XML representation of the publication job")])
    @Throws(Exception::class)
    fun publish(@FormParam("mediapackage") mediaPackageXml: String, @FormParam("channel") channel: String,
                @FormParam("downloadElementIds") downloadElementIds: String,
                @FormParam("streamingElementIds") streamingElementIds: String,
                @FormParam("checkAvailability") @DefaultValue("true") checkAvailability: Boolean): Response {
        val job: Job
        try {
            val mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml)
            job = service.publish(mediaPackage, channel, split(downloadElementIds), split(streamingElementIds), checkAvailability)
        } catch (e: IllegalArgumentException) {
            logger.warn("Unable to create an publication job", e)
            return Response.status(Status.BAD_REQUEST).build()
        } catch (e: Exception) {
            logger.warn("Error publishing element", e)
            return Response.serverError().build()
        }

        return Response.ok(JaxbJob(job)).build()
    }

    @POST
    @Path("/replace")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "replace", description = "Replace a media package in this publication channel", returnDescription = "The job that can be used to track the publication", restParameters = [RestParameter(name = "mediapackage", isRequired = true, description = "The media package", type = Type.TEXT), RestParameter(name = "channel", isRequired = true, description = "The channel name", type = Type.STRING), RestParameter(name = "downloadElements", isRequired = true, description = "The additional elements to publish to download", type = Type.STRING), RestParameter(name = "streamingElements", isRequired = true, description = "The additional elements to publish to streaming", type = Type.STRING), RestParameter(name = "retractDownloadFlavors", isRequired = true, description = "The flavors of the elements to retract from download separated by  '$SEPARATOR'", type = Type.STRING), RestParameter(name = "retractStreamingFlavors", isRequired = true, description = "The flavors of the elements to retract from streaming separated by  '$SEPARATOR'", type = Type.STRING), RestParameter(name = "publications", isRequired = true, description = "The publications to update", type = Type.STRING), RestParameter(name = "checkAvailability", isRequired = false, description = "Whether to check for availability", type = Type.BOOLEAN, defaultValue = "true")], reponses = [RestResponse(responseCode = SC_OK, description = "An XML representation of the publication job")])
    fun replace(
            @FormParam("mediapackage") mediaPackageXml: String,
            @FormParam("channel") channel: String,
            @FormParam("downloadElements") downloadElementsXml: String,
            @FormParam("streamingElements") streamingElementsXml: String,
            @FormParam("retractDownloadFlavors") retractDownloadFlavorsString: String,
            @FormParam("retractStreamingFlavors") retractStreamingFlavorsString: String,
            @FormParam("publications") publicationsXml: String,
            @FormParam("checkAvailability") @DefaultValue("true") checkAvailability: Boolean): Response {
        val job: Job
        try {
            val mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml)
            val downloadElements = HashSet(
                    MediaPackageElementParser.getArrayFromXml(downloadElementsXml))
            val streamingElements = HashSet(
                    MediaPackageElementParser.getArrayFromXml(streamingElementsXml))
            val retractDownloadFlavors = split(retractDownloadFlavorsString).stream()
                    .filter { s -> !s.isEmpty() }
                    .map(Function<String, Any> { parseFlavor() })
                    .collect(Collectors.toSet<Any>())
            val retractStreamingFlavors = split(retractStreamingFlavorsString).stream()
                    .filter { s -> !s.isEmpty() }
                    .map(Function<String, Any> { parseFlavor() })
                    .collect(Collectors.toSet<Any>())
            val publications = MediaPackageElementParser.getArrayFromXml(publicationsXml)
                    .stream().map { p -> p as Publication }.collect<Set<Publication>, Any>(Collectors.toSet())
            job = service.replace(mediaPackage, channel, downloadElements, streamingElements, retractDownloadFlavors,
                    retractStreamingFlavors, publications, checkAvailability)
        } catch (e: IllegalArgumentException) {
            logger.warn("Unable to create a publication job", e)
            return Response.status(Status.BAD_REQUEST).build()
        } catch (e: Exception) {
            logger.warn("Error publishing or retracting element", e)
            return Response.serverError().build()
        }

        return Response.ok(JaxbJob(job)).build()
    }

    @POST
    @Path("/replacesync")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "replacesync", description = "Synchronously Replace a media package in this publication channel", returnDescription = "The publication", restParameters = [RestParameter(name = "mediapackage", isRequired = true, description = "The media package", type = Type.TEXT), RestParameter(name = "channel", isRequired = true, description = "The channel name", type = Type.STRING), RestParameter(name = "downloadElements", isRequired = true, description = "The additional elements to publish to download", type = Type.STRING), RestParameter(name = "streamingElements", isRequired = true, description = "The additional elements to publish to streaming", type = Type.STRING), RestParameter(name = "retractDownloadFlavors", isRequired = true, description = "The flavors of the elements to retract from download separated by  '$SEPARATOR'", type = Type.STRING), RestParameter(name = "retractStreamingFlavors", isRequired = true, description = "The flavors of the elements to retract from streaming separated by  '$SEPARATOR'", type = Type.STRING), RestParameter(name = "publications", isRequired = true, description = "The publications to update", type = Type.STRING), RestParameter(name = "checkAvailability", isRequired = false, description = "Whether to check for availability", type = Type.BOOLEAN, defaultValue = "true")], reponses = [RestResponse(responseCode = SC_OK, description = "An XML representation of the publication")])
    @Throws(MediaPackageException::class)
    fun replaceSync(
            @FormParam("mediapackage") mediaPackageXml: String,
            @FormParam("channel") channel: String,
            @FormParam("downloadElements") downloadElementsXml: String,
            @FormParam("streamingElements") streamingElementsXml: String,
            @FormParam("retractDownloadFlavors") retractDownloadFlavorsString: String,
            @FormParam("retractStreamingFlavors") retractStreamingFlavorsString: String,
            @FormParam("publications") publicationsXml: String,
            @FormParam("checkAvailability") @DefaultValue("true") checkAvailability: Boolean): Response {
        val publication: Publication
        try {
            val mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml)
            val downloadElements = HashSet(
                    MediaPackageElementParser.getArrayFromXml(downloadElementsXml))
            val streamingElements = HashSet(
                    MediaPackageElementParser.getArrayFromXml(streamingElementsXml))
            val retractDownloadFlavors = split(retractDownloadFlavorsString).stream()
                    .filter { s -> !s.isEmpty() }
                    .map(Function<String, Any> { parseFlavor() })
                    .collect(Collectors.toSet<Any>())
            val retractStreamingFlavors = split(retractStreamingFlavorsString).stream()
                    .filter { s -> !s.isEmpty() }
                    .map(Function<String, Any> { parseFlavor() })
                    .collect(Collectors.toSet<Any>())
            val publications = MediaPackageElementParser.getArrayFromXml(publicationsXml)
                    .stream().map { p -> p as Publication }.collect<Set<Publication>, Any>(Collectors.toSet())
            publication = service.replaceSync(mediaPackage, channel, downloadElements, streamingElements, retractDownloadFlavors,
                    retractStreamingFlavors, publications, checkAvailability)
        } catch (e: Exception) {
            logger.warn("Error publishing or retracting element", e)
            return Response.serverError().build()
        }

        return Response.ok(MediaPackageElementParser.getAsXml(publication)).build()
    }

    @POST
    @Path("/retract")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "retract", description = "Retract a media package element from this publication channel", returnDescription = "The job that can be used to track the retraction", restParameters = [RestParameter(name = "mediapackage", isRequired = true, description = "The media package", type = Type.TEXT), RestParameter(name = "channel", isRequired = true, description = "The OAI-PMH channel to retract from", type = Type.STRING)], reponses = [RestResponse(responseCode = SC_OK, description = "An XML representation of the retraction job")])
    @Throws(Exception::class)
    fun retract(@FormParam("mediapackage") mediaPackageXml: String, @FormParam("channel") channel: String): Response {
        var job: Job? = null
        var mediaPackage: MediaPackage? = null
        try {
            mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml)
            job = service.retract(mediaPackage, channel)
        } catch (e: IllegalArgumentException) {
            logger.debug("Unable to create an retract job", e)
            return Response.status(Status.BAD_REQUEST).build()
        } catch (e: Exception) {
            logger.warn("Unable to retract media package '{}' from the OAI-PMH channel {}",
                    mediaPackage?.identifier?.compact() ?: "<parsing error>", channel, e)
            return Response.serverError().build()
        }

        return Response.ok(JaxbJob(job)).build()
    }

    @POST
    @Path("/updateMetadata")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "update", description = "Update metadata of an published media package. "
            + "This endpoint does not update any media files. If you want to update the whole media package, use the "
            + "publish endpoint.", returnDescription = "The job that can be used to update the metadata of an media package", restParameters = [RestParameter(name = "mediapackage", isRequired = true, description = "The updated media package", type = Type.TEXT), RestParameter(name = "channel", isRequired = true, description = "The channel name", type = Type.STRING), RestParameter(name = "flavors", isRequired = true, description = "The element flavors to be updated, separated by '$SEPARATOR'", type = Type.STRING), RestParameter(name = "tags", isRequired = true, description = "The element tags to be updated, separated by '$SEPARATOR'", type = Type.STRING), RestParameter(name = "checkAvailability", isRequired = false, description = "Whether to check for availability", type = Type.BOOLEAN, defaultValue = "true")], reponses = [RestResponse(responseCode = SC_OK, description = "An XML representation of the publication job")])
    @Throws(Exception::class)
    fun updateMetadata(@FormParam("mediapackage") mediaPackageXml: String,
                       @FormParam("channel") channel: String,
                       @FormParam("flavors") flavors: String,
                       @FormParam("tags") tags: String,
                       @FormParam("checkAvailability") @DefaultValue("true") checkAvailability: Boolean): Response {
        val job: Job
        try {
            val mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml)
            job = service.updateMetadata(mediaPackage, channel, split(flavors), split(tags), checkAvailability)
        } catch (e: IllegalArgumentException) {
            logger.debug("Unable to create an update metadata job", e)
            return Response.status(Status.BAD_REQUEST).build()
        } catch (e: Exception) {
            logger.warn("Error publishing element", e)
            return Response.serverError().build()
        }

        return Response.ok(JaxbJob(job)).build()
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
        private val logger = LoggerFactory.getLogger(OaiPmhPublicationRestService::class.java)
        private val SEPARATE_PATTERN = Pattern.compile(SEPARATOR)

        private fun split(s: String?): Set<String> {
            return if (s == null) emptySet() else SEPARATE_PATTERN.splitAsStream(s).collect<Set<String>, Any>(Collectors.toSet())
        }
    }

}
