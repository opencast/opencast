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

package org.opencastproject.ingest.endpoint

import org.apache.commons.lang3.StringUtils.trimToNull

import org.opencastproject.capture.CaptureParameters
import org.opencastproject.ingest.api.IngestException
import org.opencastproject.ingest.api.IngestService
import org.opencastproject.ingest.impl.IngestServiceImpl
import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.MediaPackageSupport
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.scheduler.api.SchedulerConflictException
import org.opencastproject.scheduler.api.SchedulerException
import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Function0.X
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowParser

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder

import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.fileupload.FileUploadException
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.util.Streams
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.HashMap
import java.util.concurrent.TimeUnit

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.Consumes
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedHashMap
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status

/**
 * Creates and augments Opencast MediaPackages using the api. Stores media into the Working File Repository.
 */
@Path("/")
@RestService(name = "ingestservice", title = "Ingest Service", abstractText = "This service creates and augments Opencast media packages that include media tracks, metadata " + "catalogs and attachments.", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is " + "not working and is either restarting or has failed", "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"])
class IngestRestService : AbstractJobProducerEndpoint() {

    /** The default workflow definition  */
    private var defaultWorkflowDefinitionId: String? = null

    /** The http client  */
    private var httpClient: TrustedHttpClient? = null

    private var factory: MediaPackageBuilderFactory? = null
    private var ingestService: IngestService? = null
    /**
     * OSGi Declarative Services callback to set the reference to the service registry.
     *
     * @param serviceRegistry
     * the service registry
     */
    override var serviceRegistry: ServiceRegistry? = null
        internal set
    private var dublinCoreService: DublinCoreCatalogService? = null
    // The number of ingests this service can handle concurrently.
    /**
     * Returns the maximum number of concurrent ingest operations or `-1` if no limit is enforced.
     *
     * @return the maximum number of concurrent ingest operations
     * @see .isIngestLimitEnabled
     */
    /**
     * Sets the maximum number of concurrent ingest operations. Use `-1` to indicate no limit.
     *
     * @param ingestLimit
     * the limit
     */
    @get:Synchronized
    var ingestLimit = -1
        private set
    /* Stores a map workflow ID and date to update the ingest start times post-hoc */
    private var startCache: Cache<String, Date>? = null
    /* Formatter to for the date into a string */
    private val formatter = SimpleDateFormat(IngestService.UTC_DATE_FORMAT)

    /**
     * Returns `true` if a maximum number of concurrent ingest operations has been defined.
     *
     * @return `true` if there is a maximum number of concurrent ingests
     */
    val isIngestLimitEnabled: Boolean
        @Synchronized get() = ingestLimit >= 0

    override val service: JobProducer?
        get() = ingestService

    init {
        factory = MediaPackageBuilderFactory.newInstance()
        startCache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build()
    }

    /**
     * Callback for activation of this component.
     */
    fun activate(cc: ComponentContext?) {
        if (cc != null) {
            defaultWorkflowDefinitionId = trimToNull(cc.bundleContext.getProperty(DEFAULT_WORKFLOW_DEFINITION))
            if (defaultWorkflowDefinitionId == null) {
                defaultWorkflowDefinitionId = "schedule-and-upload"
            }
            if (cc.bundleContext.getProperty(MAX_INGESTS_KEY) != null) {
                try {
                    ingestLimit = Integer.parseInt(trimToNull(cc.bundleContext.getProperty(MAX_INGESTS_KEY)))
                    if (ingestLimit == 0) {
                        ingestLimit = -1
                    }
                } catch (e: NumberFormatException) {
                    logger.warn("Max ingest property with key " + MAX_INGESTS_KEY
                            + " isn't defined so no ingest limit will be used.")
                    ingestLimit = -1
                }

            }
        }
    }

    @PUT
    @Produces(MediaType.TEXT_XML)
    @Path("createMediaPackageWithID/{id}")
    @RestQuery(name = "createMediaPackageWithID", description = "Create an empty media package with ID /n Overrides Existing Mediapackage ", pathParameters = [RestParameter(description = "The Id for the new Mediapackage", isRequired = true, name = "id", type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun createMediaPackage(@PathParam("id") mediaPackageId: String): Response {
        val mp: MediaPackage
        try {
            mp = ingestService!!.createMediaPackage(mediaPackageId)

            startCache!!.put(mp.identifier.toString(), Date())
            return Response.ok(mp).build()
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("createMediaPackage")
    @RestQuery(name = "createMediaPackage", description = "Create an empty media package", restParameters = [], reponses = [RestResponse(description = "Returns media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun createMediaPackage(): Response {
        val mp: MediaPackage
        try {
            mp = ingestService!!.createMediaPackage()
            startCache!!.put(mp.identifier.toString(), Date())
            return Response.ok(mp).build()
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @POST
    @Path("discardMediaPackage")
    @RestQuery(name = "discardMediaPackage", description = "Discard a media package", restParameters = [RestParameter(description = "Given media package to be destroyed", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], reponses = [RestResponse(description = "", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun discardMediaPackage(@FormParam("mediaPackage") mpx: String): Response {
        logger.debug("discardMediaPackage(MediaPackage): {}", mpx)
        try {
            val mp = factory!!.newMediaPackageBuilder()!!.loadFromXml(mpx)
            ingestService!!.discardMediaPackage(mp)
            return Response.ok().build()
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Path("addTrack")
    @RestQuery(name = "addTrackURL", description = "Add a media track to a given media package using an URL", restParameters = [RestParameter(description = "The location of the media", isRequired = true, name = "url", type = RestParameter.Type.STRING), RestParameter(description = "The kind of media", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(description = "The Tags of the  media track", isRequired = false, name = "tags", type = RestParameter.Type.STRING), RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], reponses = [RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun addMediaPackageTrack(@FormParam("url") url: String, @FormParam("flavor") flavor: String, @FormParam("tags") tags: String?,
                             @FormParam("mediaPackage") mpx: String): Response {
        logger.trace("add media package from url: {} flavor: {} tags: {} mediaPackage: {}", url, flavor, tags, mpx)
        try {
            var mp = factory!!.newMediaPackageBuilder()!!.loadFromXml(mpx)
            if (MediaPackageSupport.sanityCheck(mp).isSome)
                return Response.serverError().status(Status.BAD_REQUEST).build()
            var tagsArray: Array<String>? = null
            if (tags != null) {
                tagsArray = tags.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            }
            mp = ingestService!!.addTrack(URI(url), MediaPackageElementFlavor.parseFlavor(flavor), tagsArray, mp)
            return Response.ok(mp).build()
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("addTrack")
    @RestQuery(name = "addTrackInputStream", description = "Add a media track to a given media package using an input stream", restParameters = [RestParameter(description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(description = "The Tags of the  media track", isRequired = false, name = "tags", type = RestParameter.Type.STRING), RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], bodyParameter = RestParameter(description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = [RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun addMediaPackageTrack(@Context request: HttpServletRequest): Response {
        logger.trace("add track as multipart-form-data")
        return addMediaPackageElement(request, MediaPackageElement.Type.Track)
    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Path("addPartialTrack")
    @RestQuery(name = "addPartialTrackURL", description = "Add a partial media track to a given media package using an URL", restParameters = [RestParameter(description = "The location of the media", isRequired = true, name = "url", type = RestParameter.Type.STRING), RestParameter(description = "The kind of media", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(description = "The start time in milliseconds", isRequired = true, name = "startTime", type = RestParameter.Type.INTEGER), RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], reponses = [RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun addMediaPackagePartialTrack(@FormParam("url") url: String, @FormParam("flavor") flavor: String,
                                    @FormParam("startTime") startTime: Long?, @FormParam("mediaPackage") mpx: String): Response {
        logger.trace("add partial track with url: {} flavor: {} startTime: {} mediaPackage: {}",
                url, flavor, startTime, mpx)
        try {
            var mp = factory!!.newMediaPackageBuilder()!!.loadFromXml(mpx)
            if (MediaPackageSupport.sanityCheck(mp).isSome)
                return Response.serverError().status(Status.BAD_REQUEST).build()

            mp = ingestService!!.addPartialTrack(URI(url), MediaPackageElementFlavor.parseFlavor(flavor), startTime!!, mp)
            return Response.ok(mp).build()
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("addPartialTrack")
    @RestQuery(name = "addPartialTrackInputStream", description = "Add a partial media track to a given media package using an input stream", restParameters = [RestParameter(description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(description = "The start time in milliseconds", isRequired = true, name = "startTime", type = RestParameter.Type.INTEGER), RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], bodyParameter = RestParameter(description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = [RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun addMediaPackagePartialTrack(@Context request: HttpServletRequest): Response {
        logger.trace("add partial track as multipart-form-data")
        return addMediaPackageElement(request, MediaPackageElement.Type.Track)
    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Path("addCatalog")
    @RestQuery(name = "addCatalogURL", description = "Add a metadata catalog to a given media package using an URL", restParameters = [RestParameter(description = "The location of the catalog", isRequired = true, name = "url", type = RestParameter.Type.STRING), RestParameter(description = "The kind of catalog", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], reponses = [RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun addMediaPackageCatalog(@FormParam("url") url: String, @FormParam("flavor") flavor: String,
                               @FormParam("mediaPackage") mpx: String): Response {
        logger.trace("add catalog with url: {} flavor: {} mediaPackage: {}", url, flavor, mpx)
        try {
            val mp = factory!!.newMediaPackageBuilder()!!.loadFromXml(mpx)
            if (MediaPackageSupport.sanityCheck(mp).isSome)
                return Response.serverError().status(Status.BAD_REQUEST).build()
            val resultingMediaPackage = ingestService!!.addCatalog(URI(url),
                    MediaPackageElementFlavor.parseFlavor(flavor), mp)
            return Response.ok(resultingMediaPackage).build()
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("addCatalog")
    @RestQuery(name = "addCatalogInputStream", description = "Add a metadata catalog to a given media package using an input stream", restParameters = [RestParameter(description = "The kind of media catalog", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], bodyParameter = RestParameter(description = "The metadata catalog file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = [RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun addMediaPackageCatalog(@Context request: HttpServletRequest): Response {
        logger.trace("add catalog as multipart-form-data")
        return addMediaPackageElement(request, MediaPackageElement.Type.Catalog)
    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Path("addAttachment")
    @RestQuery(name = "addAttachmentURL", description = "Add an attachment to a given media package using an URL", restParameters = [RestParameter(description = "The location of the attachment", isRequired = true, name = "url", type = RestParameter.Type.STRING), RestParameter(description = "The kind of attachment", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], reponses = [RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun addMediaPackageAttachment(@FormParam("url") url: String, @FormParam("flavor") flavor: String,
                                  @FormParam("mediaPackage") mpx: String): Response {
        logger.trace("add attachment with url: {} flavor: {} mediaPackage: {}", url, flavor, mpx)
        try {
            var mp = factory!!.newMediaPackageBuilder()!!.loadFromXml(mpx)
            if (MediaPackageSupport.sanityCheck(mp).isSome)
                return Response.serverError().status(Status.BAD_REQUEST).build()
            mp = ingestService!!.addAttachment(URI(url), MediaPackageElementFlavor.parseFlavor(flavor), mp)
            return Response.ok(mp).build()
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("addAttachment")
    @RestQuery(name = "addAttachmentInputStream", description = "Add an attachment to a given media package using an input stream", restParameters = [RestParameter(description = "The kind of attachment", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], bodyParameter = RestParameter(description = "The attachment file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = [RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun addMediaPackageAttachment(@Context request: HttpServletRequest): Response {
        logger.trace("add attachment as multipart-form-data")
        return addMediaPackageElement(request, MediaPackageElement.Type.Attachment)
    }

    protected fun addMediaPackageElement(request: HttpServletRequest, type: MediaPackageElement.Type): Response {
        var flavor: MediaPackageElementFlavor? = null
        var `in`: InputStream? = null
        try {
            var fileName: String? = null
            var mp: MediaPackage? = null
            var startTime: Long? = null
            var tags: Array<String>? = null
            /* Only accept multipart/form-data */
            if (!ServletFileUpload.isMultipartContent(request)) {
                logger.trace("request isn't multipart-form-data")
                return Response.serverError().status(Status.BAD_REQUEST).build()
            }
            var isDone = false
            val iter = ServletFileUpload().getItemIterator(request)
            while (iter.hasNext()) {
                val item = iter.next()
                val fieldName = item.fieldName
                if (item.isFormField) {
                    if ("flavor" == fieldName) {
                        val flavorString = Streams.asString(item.openStream(), "UTF-8")
                        logger.trace("flavor: {}", flavorString)
                        if (flavorString != null) {
                            try {
                                flavor = MediaPackageElementFlavor.parseFlavor(flavorString)
                            } catch (e: IllegalArgumentException) {
                                val error = String.format("Could not parse flavor '%s'", flavorString)
                                logger.debug(error, e)
                                return Response.status(Status.BAD_REQUEST).entity(error).build()
                            }

                        }
                    } else if ("tags" == fieldName) {
                        val tagsString = Streams.asString(item.openStream(), "UTF-8")
                        logger.trace("tags: {}", tagsString)
                        tags = tagsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    } else if ("mediaPackage" == fieldName) {
                        try {
                            val mediaPackageString = Streams.asString(item.openStream(), "UTF-8")
                            logger.trace("mediaPackage: {}", mediaPackageString)
                            mp = factory!!.newMediaPackageBuilder()!!.loadFromXml(mediaPackageString)
                        } catch (e: MediaPackageException) {
                            logger.debug("Unable to parse the 'mediaPackage' parameter: {}", ExceptionUtils.getMessage(e))
                            return Response.serverError().status(Status.BAD_REQUEST).build()
                        }

                    } else if ("startTime" == fieldName && "/addPartialTrack" == request.pathInfo) {
                        val startTimeString = Streams.asString(item.openStream(), "UTF-8")
                        logger.trace("startTime: {}", startTime)
                        try {
                            startTime = java.lang.Long.parseLong(startTimeString)
                        } catch (e: Exception) {
                            logger.debug("Unable to parse the 'startTime' parameter: {}", ExceptionUtils.getMessage(e))
                            return Response.serverError().status(Status.BAD_REQUEST).build()
                        }

                    }
                } else {
                    if (flavor == null) {
                        /* A flavor has to be specified in the request prior the video file */
                        logger.debug("A flavor has to be specified in the request prior to the content BODY")
                        return Response.serverError().status(Status.BAD_REQUEST).build()
                    }
                    fileName = item.name
                    `in` = item.openStream()
                    isDone = true
                }
                if (isDone) {
                    break
                }
            }
            /*
       * Check if we actually got a valid request including a message body and a valid mediapackage to attach the
       * element to
       */
            if (`in` == null || mp == null || MediaPackageSupport.sanityCheck(mp).isSome) {
                return Response.serverError().status(Status.BAD_REQUEST).build()
            }
            when (type) {
                MediaPackageElement.Type.Attachment -> mp = ingestService!!.addAttachment(`in`, fileName!!, flavor!!, tags!!, mp)
                MediaPackageElement.Type.Catalog -> mp = ingestService!!.addCatalog(`in`, fileName!!, flavor!!, tags!!, mp)
                MediaPackageElement.Type.Track -> if (startTime == null) {
                    mp = ingestService!!.addTrack(`in`, fileName!!, flavor!!, tags!!, mp)
                } else {
                    mp = ingestService!!.addPartialTrack(`in`, fileName!!, flavor!!, startTime, mp)
                }
                else -> throw IllegalStateException("Type must be one of track, catalog, or attachment")
            }
            return Response.ok(MediaPackageParser.getAsXml(mp)).build()
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("addMediaPackage")
    @RestQuery(name = "addMediaPackage", description = "<p>Create and ingest media package from media tracks with additional Dublin Core metadata. It is "
            + "mandatory to set a title for the recording. This can be done with the 'title' form field or by supplying a DC "
            + "catalog with a title included.  The identifier of the newly created media package will be taken from the "
            + "<em>identifier</em> field or the episode DublinCore catalog (deprecated<sup>*</sup>). If no identifier is "
            + "set, a new random UUIDv4 will be generated. This endpoint is not meant to be used by capture agents for "
            + "scheduled recordings. Its primary use is for manual ingests with command line tools like curl.</p> "
            + "<p>Multiple tracks can be ingested by using multiple form fields. It is important to always set the "
            + "flavor of the next media file <em>before</em> sending the media file itself.</p>"
            + "<b>(*)</b> The special treatment of the identifier field is deprecated and may be removed in future versions "
            + "without further notice in favor of a random UUID generation to ensure uniqueness of identifiers. "
            + "<h3>Example curl command:</h3>"
            + "<p>Ingest one video file:</p>"
            + "<p><pre>\n"
            + "curl -f -i --digest -u opencast_system_account:CHANGE_ME -H 'X-Requested-Auth: Digest' \\\n"
            + "    http://localhost:8080/ingest/addMediaPackage -F creator='John Doe' -F title='Test Recording' \\\n"
            + "    -F 'flavor=presentation/source' -F 'BODY=@test-recording.mp4' \n"
            + "</pre></p>"
            + "<p>Ingest two video files:</p>"
            + "<p><pre>\n"
            + "curl -f -i --digest -u opencast_system_account:CHANGE_ME -H 'X-Requested-Auth: Digest' \\\n"
            + "    http://localhost:8080/ingest/addMediaPackage -F creator='John Doe' -F title='Test Recording' \\\n"
            + "    -F 'flavor=presentation/source' -F 'BODY=@test-recording-vga.mp4' \\\n"
            + "    -F 'flavor=presenter/source' -F 'BODY=@test-recording-camera.mp4' \n"
            + "</pre></p>", restParameters = [RestParameter(description = "The kind of media track. This has to be specified prior to each media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "abstract", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "accessRights", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "available", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "contributor", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "coverage", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "created", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "creator", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "date", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "description", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "extent", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "format", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "identifier", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "isPartOf", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "isReferencedBy", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "isReplacedBy", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "language", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "license", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "publisher", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "relation", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "replaces", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "rights", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "rightsHolder", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "source", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "spatial", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "subject", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "temporal", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "title", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "type", type = RestParameter.Type.STRING), RestParameter(description = "URL of episode DublinCore Catalog", isRequired = false, name = "episodeDCCatalogUri", type = RestParameter.Type.STRING), RestParameter(description = "Episode DublinCore Catalog", isRequired = false, name = "episodeDCCatalog", type = RestParameter.Type.STRING), RestParameter(description = "URL of series DublinCore Catalog", isRequired = false, name = "seriesDCCatalogUri", type = RestParameter.Type.STRING), RestParameter(description = "Series DublinCore Catalog", isRequired = false, name = "seriesDCCatalog", type = RestParameter.Type.STRING), RestParameter(description = "URL of a media track file", isRequired = false, name = "mediaUri", type = RestParameter.Type.STRING)], bodyParameter = RestParameter(description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = [RestResponse(description = "Ingest successfull. Returns workflow instance as xml", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Ingest failed due to invalid requests.", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "Ingest failed. Something went wrong internally. Please have a look at the log files", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun addMediaPackage(@Context request: HttpServletRequest): Response {
        logger.trace("add mediapackage as multipart-form-data")
        return addMediaPackage(request, null)
    }

    @POST
    @Produces(MediaType.TEXT_XML)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("addMediaPackage/{wdID}")
    @RestQuery(name = "addMediaPackage", description = "<p>Create and ingest media package from media tracks with additional Dublin Core metadata. It is "
            + "mandatory to set a title for the recording. This can be done with the 'title' form field or by supplying a DC "
            + "catalog with a title included.  The identifier of the newly created media package will be taken from the "
            + "<em>identifier</em> field or the episode DublinCore catalog (deprecated<sup>*</sup>). If no identifier is "
            + "set, a newa randumm UUIDv4 will be generated. This endpoint is not meant to be used by capture agents for "
            + "scheduled recordings. It's primary use is for manual ingests with command line tools like curl.</p> "
            + "<p>Multiple tracks can be ingested by using multiple form fields. It's important, however, to always set the "
            + "flavor of the next media file <em>before</em> sending the media file itself.</p>"
            + "<b>(*)</b> The special treatment of the identifier field is deprecated any may be removed in future versions "
            + "without further notice in favor of a random UUID generation to ensure uniqueness of identifiers. "
            + "<h3>Example curl command:</h3>"
            + "<p>Ingest one video file:</p>"
            + "<p><pre>\n"
            + "curl -f -i --digest -u opencast_system_account:CHANGE_ME -H 'X-Requested-Auth: Digest' \\\n"
            + "    http://localhost:8080/ingest/addMediaPackage/fast -F creator='John Doe' -F title='Test Recording' \\\n"
            + "    -F 'flavor=presentation/source' -F 'BODY=@test-recording.mp4' \n"
            + "</pre></p>"
            + "<p>Ingest two video files:</p>"
            + "<p><pre>\n"
            + "curl -f -i --digest -u opencast_system_account:CHANGE_ME -H 'X-Requested-Auth: Digest' \\\n"
            + "    http://localhost:8080/ingest/addMediaPackage/fast -F creator='John Doe' -F title='Test Recording' \\\n"
            + "    -F 'flavor=presentation/source' -F 'BODY=@test-recording-vga.mp4' \\\n"
            + "    -F 'flavor=presenter/source' -F 'BODY=@test-recording-camera.mp4' \n"
            + "</pre></p>", pathParameters = [RestParameter(description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING)], restParameters = [RestParameter(description = "The kind of media track. This has to be specified prior to each media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "abstract", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "accessRights", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "available", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "contributor", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "coverage", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "created", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "creator", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "date", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "description", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "extent", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "format", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "identifier", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "isPartOf", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "isReferencedBy", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "isReplacedBy", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "language", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "license", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "publisher", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "relation", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "replaces", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "rights", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "rightsHolder", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "source", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "spatial", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "subject", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "temporal", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "title", type = RestParameter.Type.STRING), RestParameter(description = "Episode metadata value", isRequired = false, name = "type", type = RestParameter.Type.STRING), RestParameter(description = "URL of episode DublinCore Catalog", isRequired = false, name = "episodeDCCatalogUri", type = RestParameter.Type.STRING), RestParameter(description = "Episode DublinCore Catalog", isRequired = false, name = "episodeDCCatalog", type = RestParameter.Type.STRING), RestParameter(description = "URL of series DublinCore Catalog", isRequired = false, name = "seriesDCCatalogUri", type = RestParameter.Type.STRING), RestParameter(description = "Series DublinCore Catalog", isRequired = false, name = "seriesDCCatalog", type = RestParameter.Type.STRING), RestParameter(description = "URL of a media track file", isRequired = false, name = "mediaUri", type = RestParameter.Type.STRING)], bodyParameter = RestParameter(description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = [RestResponse(description = "Ingest successfull. Returns workflow instance as XML", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Ingest failed due to invalid requests.", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "Ingest failed. Something went wrong internally. Please have a look at the log files", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun addMediaPackage(@Context request: HttpServletRequest, @PathParam("wdID") wdID: String?): Response {
        logger.trace("add mediapackage as multipart-form-data with workflow definition id: {}", wdID)
        var flavor: MediaPackageElementFlavor? = null
        try {
            val mp = ingestService!!.createMediaPackage()
            var dcc: DublinCoreCatalog? = null
            val workflowProperties = HashMap<String, String>()
            var seriesDCCatalogNumber = 0
            var episodeDCCatalogNumber = 0
            var hasMedia = false
            if (ServletFileUpload.isMultipartContent(request)) {
                val iter = ServletFileUpload().getItemIterator(request)
                while (iter.hasNext()) {
                    val item = iter.next()
                    if (item.isFormField) {
                        val fieldName = item.fieldName
                        val value = Streams.asString(item.openStream(), "UTF-8")
                        logger.trace("form field {}: {}", fieldName, value)
                        /* Ignore empty fields */
                        if ("" == value) {
                            continue
                        }

                        /* “Remember” the flavor for the next media. */
                        if ("flavor" == fieldName) {
                            try {
                                flavor = MediaPackageElementFlavor.parseFlavor(value)
                            } catch (e: IllegalArgumentException) {
                                val error = String.format("Could not parse flavor '%s'", value)
                                logger.debug(error, e)
                                return Response.status(Status.BAD_REQUEST).entity(error).build()
                            }

                            /* Fields for DC catalog */
                        } else if (dcterms.contains(fieldName)) {
                            if ("identifier" == fieldName) {
                                /* Use the identifier for the mediapackage */
                                mp.identifier = IdImpl(value)
                            }
                            val en = EName(DublinCore.TERMS_NS_URI, fieldName)
                            if (dcc == null) {
                                dcc = dublinCoreService!!.newInstance()
                            }
                            dcc.add(en, value)

                            /* Episode metadata by URL */
                        } else if ("episodeDCCatalogUri" == fieldName) {
                            try {
                                val dcurl = URI(value)
                                updateMediaPackageID(mp, dcurl)
                                ingestService!!.addCatalog(dcurl, MediaPackageElements.EPISODE, mp)
                                episodeDCCatalogNumber += 1
                            } catch (e: java.net.URISyntaxException) {
                                /* Parameter was not a valid URL: Return 400 Bad Request */
                                logger.warn(e.message, e)
                                return Response.serverError().status(Status.BAD_REQUEST).build()
                            }

                            /* Episode metadata DC catalog (XML) as string */
                        } else if ("episodeDCCatalog" == fieldName) {
                            val `is` = ByteArrayInputStream(value.toByteArray(charset("UTF-8")))
                            updateMediaPackageID(mp, `is`)
                            `is`.reset()
                            val fileName = "episode$episodeDCCatalogNumber.xml"
                            episodeDCCatalogNumber += 1
                            ingestService!!.addCatalog(`is`, fileName, MediaPackageElements.EPISODE, mp)

                            /* Series by URL */
                        } else if ("seriesDCCatalogUri" == fieldName) {
                            try {
                                val dcurl = URI(value)
                                ingestService!!.addCatalog(dcurl, MediaPackageElements.SERIES, mp)
                            } catch (e: java.net.URISyntaxException) {
                                /* Parameter was not a valid URL: Return 400 Bad Request */
                                logger.warn(e.message, e)
                                return Response.serverError().status(Status.BAD_REQUEST).build()
                            }

                            /* Series DC catalog (XML) as string */
                        } else if ("seriesDCCatalog" == fieldName) {
                            val fileName = "series$seriesDCCatalogNumber.xml"
                            seriesDCCatalogNumber += 1
                            val `is` = ByteArrayInputStream(value.toByteArray(charset("UTF-8")))
                            ingestService!!.addCatalog(`is`, fileName, MediaPackageElements.SERIES, mp)

                            /* Add media files by URL */
                        } else if ("mediaUri" == fieldName) {
                            if (flavor == null) {
                                /* A flavor has to be specified in the request prior the media file */
                                return Response.serverError().status(Status.BAD_REQUEST).build()
                            }
                            val mediaUrl: URI
                            try {
                                mediaUrl = URI(value)
                            } catch (e: java.net.URISyntaxException) {
                                /* Parameter was not a valid URL: Return 400 Bad Request */
                                logger.warn(e.message, e)
                                return Response.serverError().status(Status.BAD_REQUEST).build()
                            }

                            ingestService!!.addTrack(mediaUrl, flavor, mp)
                            hasMedia = true

                        } else {
                            /* Tread everything else as workflow properties */
                            workflowProperties[fieldName] = value
                        }

                        /* Media files as request parameter */
                    } else {
                        if (flavor == null) {
                            /* A flavor has to be specified in the request prior the video file */
                            logger.debug("A flavor has to be specified in the request prior to the content BODY")
                            return Response.serverError().status(Status.BAD_REQUEST).build()
                        }
                        ingestService!!.addTrack(item.openStream(), item.name, flavor, mp)
                        hasMedia = true
                    }
                }

                /* Check if we got any media. Fail if not. */
                if (!hasMedia) {
                    logger.warn("Rejected ingest without actual media.")
                    return Response.serverError().status(Status.BAD_REQUEST).build()
                }

                /* Add episode mediapackage if metadata were send separately */
                if (dcc != null) {
                    val out = ByteArrayOutputStream()
                    dcc.toXml(out, true)
                    val `in` = ByteArrayInputStream(out.toByteArray())
                    ingestService!!.addCatalog(`in`, "dublincore.xml", MediaPackageElements.EPISODE, mp)

                    /* Check if we have metadata for the episode */
                } else if (episodeDCCatalogNumber == 0) {
                    logger.warn("Rejected ingest without episode metadata. At least provide a title.")
                    return Response.serverError().status(Status.BAD_REQUEST).build()
                }

                val workflow = if (wdID == null)
                    ingestService!!.ingest(mp)
                else
                    ingestService!!.ingest(mp, wdID,
                            workflowProperties)
                return Response.ok(workflow).build()
            }
            return Response.serverError().status(Status.BAD_REQUEST).build()
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Try updating the identifier of a mediapackage with the identifier from a episode DublinCore catalog.
     *
     * @param mp
     * MediaPackage to modify
     * @param is
     * InputStream containing the episode DublinCore catalog
     */
    @Throws(IOException::class)
    private fun updateMediaPackageID(mp: MediaPackage, `is`: InputStream?) {
        val dc = DublinCores.read(`is`!!)
        val en = EName(DublinCore.TERMS_NS_URI, "identifier")
        val id = dc.getFirst(en)
        if (id != null) {
            mp.identifier = IdImpl(id)
        }
    }

    /**
     * Try updating the identifier of a mediapackage with the identifier from a episode DublinCore catalog.
     *
     * @param mp
     * MediaPackage to modify
     * @param uri
     * URI to get the episode DublinCore catalog from
     */
    @Throws(IOException::class)
    private fun updateMediaPackageID(mp: MediaPackage, uri: URI) {
        var `in`: InputStream? = null
        var response: HttpResponse? = null
        try {
            if (uri.toString().startsWith("http")) {
                val get = HttpGet(uri)
                response = httpClient!!.execute(get)
                val httpStatusCode = response.statusLine.statusCode
                if (httpStatusCode != 200) {
                    throw IOException("$uri returns http $httpStatusCode")
                }
                `in` = response.entity.content
            } else {
                `in` = uri.toURL().openStream()
            }
            updateMediaPackageID(mp, `in`)
            `in`!!.close()
        } finally {
            IOUtils.closeQuietly(`in`)
            httpClient!!.close(response!!)
        }
    }

    @POST
    @Path("addZippedMediaPackage/{workflowDefinitionId}")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "addZippedMediaPackage", description = "Create media package from a compressed file containing a manifest.xml document and all media tracks, metadata catalogs and attachments", pathParameters = [RestParameter(description = "Workflow definition id", isRequired = true, name = WORKFLOW_DEFINITION_ID_PARAM, type = RestParameter.Type.STRING)], restParameters = [RestParameter(description = "The workflow instance ID to associate with this zipped mediapackage", isRequired = false, name = WORKFLOW_INSTANCE_ID_PARAM, type = RestParameter.Type.STRING)], bodyParameter = RestParameter(description = "The compressed (application/zip) media package file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = [RestResponse(description = "", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_NOT_FOUND), RestResponse(description = "", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE)], returnDescription = "")
    fun addZippedMediaPackage(@Context request: HttpServletRequest,
                              @PathParam("workflowDefinitionId") wdID: String, @QueryParam("id") wiID: String): Response {
        logger.trace("add zipped media package with workflow definition id: {} and workflow instance id: {}", wdID, wiID)
        if (!isIngestLimitEnabled || ingestLimit > 0) {
            return ingestZippedMediaPackage(request, wdID, wiID)
        } else {
            logger.warn("Delaying ingest because we have exceeded the maximum number of ingests this server is setup to do concurrently.")
            return Response.status(Status.SERVICE_UNAVAILABLE).build()
        }
    }

    @POST
    @Path("addZippedMediaPackage")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "addZippedMediaPackage", description = "Create media package from a compressed file containing a manifest.xml document and all media tracks, metadata catalogs and attachments", restParameters = [RestParameter(description = "The workflow definition ID to run on this mediapackage. "
            + "This parameter has to be set in the request prior to the zipped mediapackage "
            + "(This parameter is deprecated. Please use /addZippedMediaPackage/{workflowDefinitionId} instead)", isRequired = false, name = WORKFLOW_DEFINITION_ID_PARAM, type = RestParameter.Type.STRING), RestParameter(description = "The workflow instance ID to associate with this zipped mediapackage. "
            + "This parameter has to be set in the request prior to the zipped mediapackage "
            + "(This parameter is deprecated. Please use /addZippedMediaPackage/{workflowDefinitionId} with a path parameter instead)", isRequired = false, name = WORKFLOW_INSTANCE_ID_PARAM, type = RestParameter.Type.STRING)], bodyParameter = RestParameter(description = "The compressed (application/zip) media package file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = [RestResponse(description = "", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_NOT_FOUND), RestResponse(description = "", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE)], returnDescription = "")
    fun addZippedMediaPackage(@Context request: HttpServletRequest): Response {
        logger.trace("add zipped media package")
        if (!isIngestLimitEnabled || ingestLimit > 0) {
            return ingestZippedMediaPackage(request, null, null)
        } else {
            logger.warn("Delaying ingest because we have exceeded the maximum number of ingests this server is setup to do concurrently.")
            return Response.status(Status.SERVICE_UNAVAILABLE).build()
        }
    }

    private fun ingestZippedMediaPackage(request: HttpServletRequest, wdID: String?, wiID: String?): Response {
        if (isIngestLimitEnabled) {
            ingestLimit = ingestLimit - 1
            logger.debug("An ingest has started so remaining ingest limit is $ingestLimit")
        }
        var `in`: InputStream? = null
        val started = Date()

        logger.info("Received new request from {} to ingest a zipped mediapackage", request.remoteHost)

        try {
            var workflowDefinitionId = wdID
            var workflowIdAsString = wiID
            var workflowInstanceIdAsLong: Long? = null
            val workflowConfig = HashMap<String, String>()
            if (ServletFileUpload.isMultipartContent(request)) {
                var isDone = false
                val iter = ServletFileUpload().getItemIterator(request)
                while (iter.hasNext()) {
                    val item = iter.next()
                    if (item.isFormField) {
                        val fieldName = item.fieldName
                        val value = Streams.asString(item.openStream(), "UTF-8")
                        logger.trace("{}: {}", fieldName, value)
                        if (WORKFLOW_INSTANCE_ID_PARAM == fieldName) {
                            workflowIdAsString = value
                            continue
                        } else if (WORKFLOW_DEFINITION_ID_PARAM == fieldName) {
                            workflowDefinitionId = value
                            continue
                        } else {
                            logger.debug("Processing form field: $fieldName")
                            workflowConfig[fieldName] = value
                        }
                    } else {
                        logger.debug("Processing file item")
                        // once the body gets read iter.hasNext must not be invoked or the stream can not be read
                        // MH-9579
                        `in` = item.openStream()
                        isDone = true
                    }
                    if (isDone)
                        break
                }
            } else {
                logger.debug("Processing file item")
                `in` = request.inputStream
            }

            // Adding ingest start time to workflow configuration
            val formatter = SimpleDateFormat(IngestService.UTC_DATE_FORMAT)
            workflowConfig[IngestService.START_DATE_KEY] = formatter.format(started)

            /* Legacy support: Try to convert the workflowId to integer */
            if (!StringUtils.isBlank(workflowIdAsString)) {
                try {
                    workflowInstanceIdAsLong = java.lang.Long.parseLong(workflowIdAsString!!)
                } catch (e: NumberFormatException) {
                    // The workflowId is not a long value and might be the media package identifier
                    workflowConfig[IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY] = workflowIdAsString
                }

            }
            if (StringUtils.isBlank(workflowDefinitionId)) {
                workflowDefinitionId = defaultWorkflowDefinitionId
            }

            val workflow: WorkflowInstance
            if (workflowInstanceIdAsLong != null) {
                workflow = ingestService!!.addZippedMediaPackage(`in`!!, workflowDefinitionId!!, workflowConfig,
                        workflowInstanceIdAsLong)
            } else {
                workflow = ingestService!!.addZippedMediaPackage(`in`!!, workflowDefinitionId!!, workflowConfig)
            }
            return Response.ok(WorkflowParser.toXml(workflow)).build()
        } catch (e: NotFoundException) {
            logger.info(e.message)
            return Response.status(Status.NOT_FOUND).build()
        } catch (e: MediaPackageException) {
            logger.warn(e.message)
            return Response.serverError().status(Status.BAD_REQUEST).build()
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        } finally {
            IOUtils.closeQuietly(`in`)
            if (isIngestLimitEnabled) {
                ingestLimit = ingestLimit + 1
                logger.debug("An ingest has finished so increased ingest limit to $ingestLimit")
            }
        }
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("ingest/{wdID}")
    @RestQuery(name = "ingest", description = "Ingest the completed media package into the system, retrieving all URL-referenced files, and starting a specified workflow", pathParameters = [RestParameter(description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING)], restParameters = [RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], reponses = [RestResponse(description = "Returns the media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    fun ingest(@Context request: HttpServletRequest, @PathParam("wdID") wdID: String): Response {
        logger.trace("ingest media package with workflow definition id: {}", wdID)
        return if (StringUtils.isBlank(wdID)) {
            Response.status(Response.Status.BAD_REQUEST).build()
        } else ingest(wdID, request)
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("ingest")
    @RestQuery(name = "ingest", description = "Ingest the completed media package into the system, retrieving all URL-referenced files", restParameters = [RestParameter(description = "The media package", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT), RestParameter(description = "Workflow definition id", isRequired = false, name = WORKFLOW_DEFINITION_ID_PARAM, type = RestParameter.Type.STRING), RestParameter(description = "The workflow instance ID to associate with this zipped mediapackage", isRequired = false, name = WORKFLOW_INSTANCE_ID_PARAM, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns the media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    fun ingest(@Context request: HttpServletRequest): Response {
        return ingest(null, request)
    }

    private fun getWorkflowConfig(formData: MultivaluedMap<String, String>): MutableMap<String, String> {
        val wfConfig = HashMap<String, String>()
        for (key in formData.keys) {
            if ("mediaPackage" != key) {
                wfConfig[key] = formData.getFirst(key)
            }
        }
        return wfConfig
    }

    private fun ingest(wdID: String?, request: HttpServletRequest): Response {
        /* Note: We use a MultivaluedMap here to ensure that we can get any arbitrary form parameters. This is required to
     * enable things like holding for trim or distributing to YouTube. */
        val formData = MultivaluedHashMap<String, String>()
        if (ServletFileUpload.isMultipartContent(request)) {
            // parse form fields
            try {
                val iter = ServletFileUpload().getItemIterator(request)
                while (iter.hasNext()) {
                    val item = iter.next()
                    if (item.isFormField) {
                        val value = Streams.asString(item.openStream(), "UTF-8")
                        formData.putSingle(item.fieldName, value)
                    }
                }
            } catch (e: FileUploadException) {
                return Response.status(Response.Status.BAD_REQUEST).build()
            } catch (e: IOException) {
                return Response.status(Response.Status.BAD_REQUEST).build()
            }

        } else {
            request.parameterMap.forEach { key, value -> formData[key] = Arrays.asList(*value) }
        }

        val wfConfig = getWorkflowConfig(formData)
        if (StringUtils.isNotBlank(wdID))
            wfConfig[WORKFLOW_DEFINITION_ID_PARAM] = wdID

        val mp: MediaPackage
        try {
            mp = factory!!.newMediaPackageBuilder()!!.loadFromXml(formData.getFirst("mediaPackage"))
            if (MediaPackageSupport.sanityCheck(mp).isSome) {
                logger.warn("Rejected ingest with invalid mediapackage {}", mp)
                return Response.status(Status.BAD_REQUEST).build()
            }
        } catch (e: Exception) {
            logger.warn("Rejected ingest without mediapackage")
            return Response.status(Status.BAD_REQUEST).build()
        }

        val workflowInstance = wfConfig[WORKFLOW_INSTANCE_ID_PARAM]
        val workflowDefinition = wfConfig[WORKFLOW_DEFINITION_ID_PARAM]

        // Adding ingest start time to workflow configuration
        wfConfig[IngestService.START_DATE_KEY] = formatter.format(startCache!!.asMap()[mp.identifier.toString()])

        val ingest = object : X<WorkflowInstance>() {
            @Throws(Exception::class)
            override fun xapply(): WorkflowInstance {
                /* Legacy support: Try to convert the workflowInstance to integer */
                var workflowInstanceId: Long? = null
                if (StringUtils.isNotBlank(workflowInstance)) {
                    try {
                        workflowInstanceId = java.lang.Long.parseLong(workflowInstance)
                    } catch (e: NumberFormatException) {
                        // The workflowId is not a long value and might be the media package identifier
                        wfConfig[IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY] = workflowInstance
                    }

                }

                return if (workflowInstanceId != null) {
                    ingestService!!.ingest(mp, trimToNull(workflowDefinition), wfConfig, workflowInstanceId)
                } else {
                    ingestService!!.ingest(mp, trimToNull(workflowDefinition), wfConfig)
                }
            }
        }

        try {
            val workflow = ingest.apply()
            startCache!!.asMap().remove(mp.identifier.toString())
            return Response.ok(WorkflowParser.toXml(workflow)).build()
        } catch (e: Exception) {
            logger.warn(e.message, e)
            return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @POST
    @Path("schedule")
    @RestQuery(name = "schedule", description = "Schedule an event based on the given media package", restParameters = [RestParameter(description = "The media package", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], reponses = [RestResponse(description = "Event scheduled", responseCode = HttpServletResponse.SC_CREATED), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    fun schedule(formData: MultivaluedMap<String, String>): Response {
        logger.trace("pass schedule with default workflow definition id {}", defaultWorkflowDefinitionId)
        return this.schedule(defaultWorkflowDefinitionId, formData)
    }

    @POST
    @Path("schedule/{wdID}")
    @RestQuery(name = "schedule", description = "Schedule an event based on the given media package", pathParameters = [RestParameter(description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING)], restParameters = [RestParameter(description = "The media package", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT)], reponses = [RestResponse(description = "Event scheduled", responseCode = HttpServletResponse.SC_CREATED), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    fun schedule(@PathParam("wdID") wdID: String?, formData: MultivaluedMap<String, String>): Response {
        if (StringUtils.isBlank(wdID)) {
            logger.trace("workflow definition id is not specified")
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        val wfConfig = getWorkflowConfig(formData)
        if (StringUtils.isNotBlank(wdID)) {
            wfConfig[CaptureParameters.INGEST_WORKFLOW_DEFINITION] = wdID
        }
        logger.debug("Schedule with workflow definition '{}'", wfConfig[WORKFLOW_DEFINITION_ID_PARAM])

        val mediaPackageXml = formData.getFirst("mediaPackage")
        if (StringUtils.isBlank(mediaPackageXml)) {
            logger.debug("Rejected schedule without media package")
            return Response.status(Status.BAD_REQUEST).build()
        }

        var mp: MediaPackage? = null
        try {
            mp = factory!!.newMediaPackageBuilder()!!.loadFromXml(mediaPackageXml)
            if (MediaPackageSupport.sanityCheck(mp).isSome) {
                throw MediaPackageException("Insane media package")
            }
        } catch (e: MediaPackageException) {
            logger.debug("Rejected ingest with invalid media package {}", mp)
            return Response.status(Status.BAD_REQUEST).build()
        }

        val mediaPackageElements = mp.getElementsByFlavor(MediaPackageElements.EPISODE)
        if (mediaPackageElements.size != 1) {
            logger.debug("There can be only one (and exactly one) episode dublin core catalog: https://youtu.be/_J3VeogFUOs")
            return Response.status(Status.BAD_REQUEST).build()
        }

        try {
            ingestService!!.schedule(mp, wdID!!, wfConfig)
            return Response.status(Status.CREATED).build()
        } catch (e: IngestException) {
            return Response.status(Status.BAD_REQUEST).entity(e.message).build()
        } catch (e: SchedulerConflictException) {
            return Response.status(Status.CONFLICT).entity(e.message).build()
        } catch (e: NotFoundException) {
            return Response.serverError().build()
        } catch (e: UnauthorizedException) {
            return Response.serverError().build()
        } catch (e: SchedulerException) {
            return Response.serverError().build()
        }

    }

    /**
     * Adds a dublinCore metadata catalog to the MediaPackage and returns the grown mediaPackage. JQuery Ajax functions
     * doesn't support multipart/form-data encoding.
     *
     * @param mp
     * MediaPackage
     * @param dc
     * DublinCoreCatalog
     * @return grown MediaPackage XML
     */
    @POST
    @Produces(MediaType.TEXT_XML)
    @Path("addDCCatalog")
    @RestQuery(name = "addDCCatalog", description = "Add a dublincore episode catalog to a given media package using an url", restParameters = [RestParameter(description = "The media package as XML", isRequired = true, name = "mediaPackage", type = RestParameter.Type.TEXT), RestParameter(description = "DublinCore catalog as XML", isRequired = true, name = "dublinCore", type = RestParameter.Type.TEXT), RestParameter(defaultValue = "dublincore/episode", description = "DublinCore Flavor", isRequired = false, name = "flavor", type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "")
    fun addDCCatalog(@FormParam("mediaPackage") mp: String, @FormParam("dublinCore") dc: String,
                     @FormParam("flavor") flavor: String?): Response {
        logger.trace("add DC catalog: {} with flavor: {} to media package: {}", dc, flavor, mp)
        var dcFlavor = MediaPackageElements.EPISODE
        if (flavor != null) {
            try {
                dcFlavor = MediaPackageElementFlavor.parseFlavor(flavor)
            } catch (e: IllegalArgumentException) {
                logger.warn("Unable to set dublin core flavor to {}, using {} instead", flavor, MediaPackageElements.EPISODE)
            }

        }
        var mediaPackage: MediaPackage
        /* Check if we got a proper mediapackage and try to parse it */
        try {
            mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(mp)
        } catch (e: MediaPackageException) {
            return Response.serverError().status(Status.BAD_REQUEST).build()
        }

        if (MediaPackageSupport.sanityCheck(mediaPackage).isSome) {
            return Response.serverError().status(Status.BAD_REQUEST).build()
        }

        /* Check if we got a proper catalog */
        if (StringUtils.isBlank(dc)) {
            return Response.serverError().status(Status.BAD_REQUEST).build()
        }

        var `in`: InputStream? = null
        try {
            `in` = IOUtils.toInputStream(dc, "UTF-8")
            mediaPackage = ingestService!!.addCatalog(`in`!!, "dublincore.xml", dcFlavor, mediaPackage)
        } catch (e: MediaPackageException) {
            return Response.serverError().status(Status.BAD_REQUEST).build()
        } catch (e: IOException) {
            /* Return an internal server error if we could not write to disk */
            logger.error("Could not write catalog to disk: {}", e.message)
            return Response.serverError().build()
        } catch (e: Exception) {
            logger.error(e.message)
            return Response.serverError().build()
        } finally {
            IOUtils.closeQuietly(`in`)
        }
        return Response.ok(mediaPackage).build()
    }

    /**
     * OSGi Declarative Services callback to set the reference to the ingest service.
     *
     * @param ingestService
     * the ingest service
     */
    internal fun setIngestService(ingestService: IngestService) {
        this.ingestService = ingestService
    }

    /**
     * OSGi Declarative Services callback to set the reference to the dublin core service.
     *
     * @param dcService
     * the dublin core service
     */
    internal fun setDublinCoreService(dcService: DublinCoreCatalogService) {
        this.dublinCoreService = dcService
    }

    /**
     * Sets the trusted http client
     *
     * @param httpClient
     * the http client
     */
    fun setHttpClient(httpClient: TrustedHttpClient) {
        this.httpClient = httpClient
    }

    companion object {

        private val logger = LoggerFactory.getLogger(IngestRestService::class.java)

        /** Key for the default workflow definition in config.properties  */
        val DEFAULT_WORKFLOW_DEFINITION = "org.opencastproject.workflow.default.definition"

        /** Key for the default maximum number of ingests in config.properties  */
        val MAX_INGESTS_KEY = "org.opencastproject.ingest.max.concurrent"

        /** The http request parameter used to provide the workflow instance id  */
        val WORKFLOW_INSTANCE_ID_PARAM = "workflowInstanceId"

        /** The http request parameter used to provide the workflow definition id  */
        protected val WORKFLOW_DEFINITION_ID_PARAM = "workflowDefinitionId"

        /** Dublin Core Terms: http://purl.org/dc/terms/  */
        private val dcterms = Arrays.asList("abstract", "accessRights", "accrualMethod",
                "accrualPeriodicity", "accrualPolicy", "alternative", "audience", "available", "bibliographicCitation",
                "conformsTo", "contributor", "coverage", "created", "creator", "date", "dateAccepted", "dateCopyrighted",
                "dateSubmitted", "description", "educationLevel", "extent", "format", "hasFormat", "hasPart", "hasVersion",
                "identifier", "instructionalMethod", "isFormatOf", "isPartOf", "isReferencedBy", "isReplacedBy",
                "isRequiredBy", "issued", "isVersionOf", "language", "license", "mediator", "medium", "modified",
                "provenance", "publisher", "references", "relation", "replaces", "requires", "rights", "rightsHolder",
                "source", "spatial", "subject", "tableOfContents", "temporal", "title", "type", "valid")
    }

}
