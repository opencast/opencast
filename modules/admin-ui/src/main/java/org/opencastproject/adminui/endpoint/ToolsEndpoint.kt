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

import com.entwinemedia.fn.Stream.`$`
import com.entwinemedia.fn.data.json.Jsons.arr
import com.entwinemedia.fn.data.json.Jsons.f
import com.entwinemedia.fn.data.json.Jsons.obj
import com.entwinemedia.fn.data.json.Jsons.v
import java.lang.String.format
import java.util.Collections.emptyList
import java.util.Objects.requireNonNull
import javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR
import javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import javax.servlet.http.HttpServletResponse.SC_OK
import org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace
import org.opencastproject.util.data.Tuple.tuple

import org.opencastproject.adminui.impl.AdminUIConfiguration
import org.opencastproject.adminui.impl.ThumbnailImpl
import org.opencastproject.adminui.index.AdminUISearchIndex
import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.AssetManagerException
import org.opencastproject.assetmanager.util.WorkflowPropertiesUtil
import org.opencastproject.assetmanager.util.Workflows
import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.index.service.api.IndexService
import org.opencastproject.index.service.api.IndexService.Source
import org.opencastproject.index.service.exception.IndexServiceException
import org.opencastproject.index.service.impl.index.event.Event
import org.opencastproject.index.service.util.RestUtils
import org.opencastproject.matterhorn.search.SearchIndexException
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElement.Type
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.Stream
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.VideoStream
import org.opencastproject.publication.api.ConfigurablePublicationService
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.urlsigning.exception.UrlSigningException
import org.opencastproject.security.urlsigning.service.UrlSigningService
import org.opencastproject.security.urlsigning.utils.UrlSigningServiceOsgiUtil
import org.opencastproject.smil.api.SmilException
import org.opencastproject.smil.api.SmilResponse
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.smil.entity.media.api.SmilMediaObject
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer
import org.opencastproject.smil.entity.media.element.api.SmilMediaElement
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.RestUtil.R
import org.opencastproject.util.UnknownFileTypeException
import org.opencastproject.util.data.Tuple
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService
import org.opencastproject.workflow.api.ConfiguredWorkflow
import org.opencastproject.workflow.api.WorkflowDatabaseException
import org.opencastproject.workflow.api.WorkflowDefinition
import org.opencastproject.workflow.api.WorkflowService
import org.opencastproject.workflow.api.WorkflowUtil
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Fn
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.data.json.Field
import com.entwinemedia.fn.data.json.JObject
import com.entwinemedia.fn.data.json.JValue
import com.entwinemedia.fn.data.json.Jsons
import com.entwinemedia.fn.data.json.Jsons.Functions

import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.fileupload.FileUploadException
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.util.Streams
import org.apache.commons.io.IOUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.Dictionary
import java.util.LinkedList
import java.util.Optional
import java.util.OptionalDouble
import java.util.stream.Collectors

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.WebApplicationException
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.xml.bind.JAXBException

@Path("/")
@RestService(name = "toolsService", title = "Tools API Service", abstractText = "Provides a location for the tools API.", notes = ["This service provides a location for the tools API for the admin UI.", "<strong>Important:</strong> "
        + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
        + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
        + "DO NOT use this for integration of third-party applications.<em>"])
class ToolsEndpoint : ManagedService {

    private var expireSeconds = UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION

    private var signWithClientIP: Boolean? = UrlSigningServiceOsgiUtil.DEFAULT_SIGN_WITH_CLIENT_IP

    // service references
    private var adminUIConfiguration: AdminUIConfiguration? = null
    private var searchIndex: AdminUISearchIndex? = null
    private var assetManager: AssetManager? = null
    private var composerService: ComposerService? = null
    private var index: IndexService? = null
    private var oaiPmhPublicationService: OaiPmhPublicationService? = null
    private var configurablePublicationService: ConfigurablePublicationService? = null
    private var securityService: SecurityService? = null
    private var smilService: SmilService? = null
    private var urlSigningService: UrlSigningService? = null
    private var workflowService: WorkflowService? = null
    private var workspace: Workspace? = null

    /**
     * Returns a list of workflow definitions that may be applied to a media package after segments have been defined with
     * the editor tool.
     *
     * @return a list of workflow definitions
     */
    private val editingWorkflows: List<WorkflowDefinition>
        get() {
            val workflows: List<WorkflowDefinition>
            try {
                workflows = workflowService!!.listAvailableWorkflowDefinitions()
            } catch (e: WorkflowDatabaseException) {
                logger.warn("Error while retrieving list of workflow definitions: {}", getStackTrace(e))
                return emptyList()
            }

            return `$`(workflows).filter(object : Fn<WorkflowDefinition, Boolean>() {
                override fun apply(a: WorkflowDefinition): Boolean? {
                    return a.containsTag(EDITOR_WORKFLOW_TAG)
                }
            }).toList()
        }

    internal fun setConfigurablePublicationService(configurablePublicationService: ConfigurablePublicationService) {
        this.configurablePublicationService = configurablePublicationService
    }

    /** OSGi DI.  */
    internal fun setAdminUIConfiguration(adminUIConfiguration: AdminUIConfiguration) {
        this.adminUIConfiguration = adminUIConfiguration
    }

    /** OSGi DI  */
    internal fun setAdminUISearchIndex(adminUISearchIndex: AdminUISearchIndex) {
        this.searchIndex = adminUISearchIndex
    }

    /** OSGi DI  */
    internal fun setAssetManager(assetManager: AssetManager) {
        this.assetManager = assetManager
    }

    /** OSGi DI  */
    internal fun setIndexService(index: IndexService) {
        this.index = index
    }

    /** OSGi DI  */
    internal fun setOaiPmhPublicationService(oaiPmhPublicationService: OaiPmhPublicationService) {
        this.oaiPmhPublicationService = oaiPmhPublicationService
    }

    /** OSGi DI  */
    internal fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /** OSGi DI  */
    internal fun setSmilService(smilService: SmilService) {
        this.smilService = smilService
    }

    /** OSGi DI  */
    internal fun setUrlSigningService(urlSigningService: UrlSigningService) {
        this.urlSigningService = urlSigningService
    }

    /** OSGi DI  */
    internal fun setWorkflowService(workflowService: WorkflowService) {
        this.workflowService = workflowService
    }

    /** OSGi DI  */
    internal fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    /** OSGi DI  */
    internal fun setComposerService(composerService: ComposerService) {
        this.composerService = composerService
    }

    /** OSGi callback if properties file is present  */
    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<String, *>?) {
        if (properties == null) {
            logger.info("No configuration available, using defaults")
            return
        }

        expireSeconds = UrlSigningServiceOsgiUtil.getUpdatedSigningExpiration(properties, this.javaClass.simpleName)
        signWithClientIP = UrlSigningServiceOsgiUtil.getUpdatedSignWithClientIP(properties,
                this.javaClass.simpleName)
    }

    @GET
    @Path("{mediapackageid}.json")
    @RestQuery(name = "getAvailableTools", description = "Returns a list of tools which are currently available for the given media package.", returnDescription = "A JSON array with tools identifiers", pathParameters = [RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Available tools evaluated", responseCode = HttpServletResponse.SC_OK)])
    fun getAvailableTools(@PathParam("mediapackageid") mediaPackageId: String): Response {
        val jTools = ArrayList<JValue>()
        if (isEditorAvailable(mediaPackageId))
            jTools.add(v("editor"))

        return RestUtils.okJson(obj(f("available", arr(jTools))))
    }

    private fun getPreviewElementsFromPublication(publication: Opt<Publication>): List<MediaPackageElement> {
        val previewElements = LinkedList<MediaPackageElement>()
        for (p in publication) {
            for (attachment in p.attachments) {
                if (elementHasPreviewFlavor(attachment)) {
                    previewElements.add(attachment)
                }
            }
            for (catalog in p.catalogs) {
                if (elementHasPreviewFlavor(catalog)) {
                    previewElements.add(catalog)
                }
            }
            for (track in p.tracks) {
                if (elementHasPreviewFlavor(track)) {
                    previewElements.add(track)
                }
            }
        }
        return previewElements
    }

    private fun elementHasPreviewFlavor(element: MediaPackageElement): Boolean {
        return element.flavor != null && adminUIConfiguration!!.previewSubtype == element.flavor.subtype
    }

    @GET
    @Path("{mediapackageid}/editor.json")
    @Produces(MediaType.APPLICATION_JSON)
    @RestQuery(name = "getVideoEditor", description = "Returns all the information required to get the editor tool started", returnDescription = "JSON object", pathParameters = [RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Media package found", responseCode = SC_OK), RestResponse(description = "Media package not found", responseCode = SC_NOT_FOUND)])
    @Throws(IndexServiceException::class, NotFoundException::class)
    fun getVideoEditor(@PathParam("mediapackageid") mediaPackageId: String): Response {

        // Select tracks
        val event = getEvent(mediaPackageId).get()
        val mp = index!!.getEventMediapackage(event)
        val previewPublications = getPreviewElementsFromPublication(getInternalPublication(mp))

        // Collect previews and tracks
        val jPreviews = ArrayList<JValue>()
        val jTracks = ArrayList<JValue>()
        for (element in previewPublications) {
            val elementUri: URI
            if (urlSigningService!!.accepts(element.getURI().toString())) {
                try {
                    var clientIP: String? = null
                    if (signWithClientIP!!) {
                        clientIP = securityService!!.userIP
                    }
                    elementUri = URI(urlSigningService!!.sign(element.getURI().toString(), expireSeconds, null, clientIP))
                } catch (e: URISyntaxException) {
                    logger.error("Error while trying to sign the preview urls because: {}", getStackTrace(e))
                    throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
                } catch (e: UrlSigningException) {
                    logger.error("Error while trying to sign the preview urls because: {}", getStackTrace(e))
                    throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
                }

            } else {
                elementUri = element.getURI()
            }
            var jPreview = obj(f("uri", v(elementUri.toString())))
            // Get the elements frame rate for frame by frame skipping in the editor
            // Note that this assumes that the resulting video will have the same frame rate as the preview
            // and also that there is only one video stream for any preview element.
            if (element is Track) {
                for (stream in element.streams) {
                    if (stream is VideoStream) {
                        jPreview = jPreview.merge(obj(f("frameRate", v(stream.frameRate!!))))
                        break
                    }
                }
            }
            jPreviews.add(jPreview)

            if (Type.Track != element.elementType)
                continue

            val jTrack = obj(f("id", v(element.identifier)), f("flavor", v(element.flavor.type!!)))
            // Check if there's a waveform for the current track
            val optWaveform = getWaveformForTrack(mp, element)
            if (optWaveform.isSome) {
                val waveformUri: URI
                if (urlSigningService!!.accepts(element.getURI().toString())) {
                    try {
                        waveformUri = URI(
                                urlSigningService!!.sign(optWaveform.get().getURI().toString(), expireSeconds, null, null))
                    } catch (e: URISyntaxException) {
                        logger.error("Error while trying to serialize the waveform urls because: {}", getStackTrace(e))
                        throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
                    } catch (e: UrlSigningException) {
                        logger.error("Error while trying to sign the preview urls because: {}", getStackTrace(e))
                        throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
                    }

                } else {
                    waveformUri = optWaveform.get().getURI()
                }
                jTracks.add(jTrack.merge(obj(f("waveform", v(waveformUri.toString())))))
            } else {
                jTracks.add(jTrack)
            }

        }

        // Get existing segments
        val jSegments = ArrayList<JValue>()
        for (segment in getSegments(mp)) {
            jSegments.add(obj(f(START_KEY, v(segment.a)), f(END_KEY, v(segment.b))))
        }

        // Get workflows
        val jWorkflows = ArrayList<JValue>()
        for (workflow in editingWorkflows) {
            jWorkflows.add(obj(f("id", v(workflow.id)), f("name", v(workflow.title, Jsons.BLANK)),
                    f("displayOrder", v(workflow.displayOrder))))
        }

        // Get thumbnail
        val thumbnailFields = ArrayList<Field>()
        try {
            val thumbnailImpl = newThumbnailImpl()
            val optThumbnail = thumbnailImpl
                    .getThumbnail(mp, urlSigningService, expireSeconds)

            optThumbnail.ifPresent { thumbnail ->
                thumbnailFields.add(f("type", thumbnail.type.name))
                thumbnailFields.add(f("url", thumbnail.url.toString()))
                thumbnailFields.add(f("defaultPosition", thumbnailImpl.defaultPosition))
                thumbnail.position.ifPresent { p -> thumbnailFields.add(f("position", p)) }
                thumbnail.track.ifPresent { t -> thumbnailFields.add(f("track", t)) }
            }
        } catch (e: UrlSigningException) {
            logger.error("Error while trying to serialize the thumbnail url because: {}", getStackTrace(e))
            throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
        } catch (e: URISyntaxException) {
            logger.error("Error while trying to serialize the thumbnail url because: {}", getStackTrace(e))
            throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
        }

        val latestWfProperties = WorkflowPropertiesUtil
                .getLatestWorkflowProperties(assetManager!!, mediaPackageId)
        // The properties have the format "hide_flavor_audio" or "hide_flavor_video", where flavor is preconfigured.
        // We filter all the properties that have this format, and then those which have values "true".
        val hiddens = latestWfProperties.entries
                .stream()
                .map<Any> { p -> Tuple.tuple(p.key.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), p.value) }
                .filter { p -> p.getA().length === 3 }
                .filter { p -> p.getA()[0].equals("hide") }
                .filter { p -> p.getB().equals("true") }
                .map<Any> { p -> Tuple.tuple(p.getA()[1], p.getA()[2]) }
                .collect<Collection<Tuple<String, String>>, Any>(Collectors.toSet<Any>())

        val acceptedFlavors = Arrays
                .asList(this.adminUIConfiguration!!.sourceTrackLeftFlavor,
                        this.adminUIConfiguration!!.sourceTrackRightFlavor)

        // We already know the internal publication exists, so just "get" it here.
        val internalPub = getInternalPublication(mp).get()

        val sourceTracks = Arrays.stream(mp.elements)
                .filter { e -> e.elementType == Type.Track }
                .map { e -> e as Track }
                .filter { e -> acceptedFlavors.contains(e.flavor) }
                .map { e ->
                    var side: String? = null
                    if (e.flavor == this.adminUIConfiguration!!.sourceTrackLeftFlavor) {
                        side = "left"
                    } else if (e.flavor == this.adminUIConfiguration!!.sourceTrackRightFlavor) {
                        side = "right"
                    }
                    val audioHidden = hiddens.contains(Tuple.tuple(e.flavor.type, "audio"))
                    val audioPreview = Arrays.stream(internalPub.attachments)
                            .filter { a -> a.flavor.type == e.flavor.type }
                            .filter { a -> a.flavor.subtype == this.adminUIConfiguration!!.previewAudioSubtype }
                            .map(Function<Attachment, Any> { getURI() }).map(Function<Any, R> { this.signUrl(it) })
                            .findAny()
                            .orElse(null)
                    val audio = SourceTrackSubInfo(e.hasAudio(), audioPreview,
                            audioHidden)
                    val videoHidden = hiddens.contains(Tuple.tuple(e.flavor.type, "video"))
                    val videoPreview = Arrays.stream(internalPub.attachments)
                            .filter { a -> a.flavor.type == e.flavor.type }
                            .filter { a -> a.flavor.subtype == this.adminUIConfiguration!!.previewVideoSubtype }
                            .map(Function<Attachment, Any> { getURI() }).map(Function<Any, R> { this.signUrl(it) })
                            .findAny()
                            .orElse(null)
                    val video = SourceTrackSubInfo(e.hasVideo(), videoPreview,
                            videoHidden)
                    SourceTrackInfo(e.flavor.type, e.flavor.subtype, audio, video, side)
                }
                .map<JObject>(Function<SourceTrackInfo, JObject> { it.toJson() })
                .collect<List<JValue>, Any>(Collectors.toList())

        return RestUtils.okJson(obj(f("title", v(mp.title, Jsons.BLANK)),
                f("date", v(event.recordingStartDate, Jsons.BLANK)),
                f("series", obj(f("id", v(event.seriesId, Jsons.BLANK)), f("title", v(event.seriesName, Jsons.BLANK)))),
                f("presenters", arr(`$`(event.presenters).map(Functions.stringToJValue))),
                f(SOURCE_TRACKS_KEY, arr(sourceTracks)),
                f("previews", arr(jPreviews)), f(TRACKS_KEY, arr(jTracks)),
                f("thumbnail", obj(thumbnailFields)),
                f("duration", v(mp.duration!!)), f(SEGMENTS_KEY, arr(jSegments)), f("workflows", arr(jWorkflows))))
    }

    private fun newThumbnailImpl(): ThumbnailImpl {
        return ThumbnailImpl(adminUIConfiguration!!, workspace, oaiPmhPublicationService, configurablePublicationService,
                assetManager, composerService)
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{mediapackageid}/thumbnail.json")
    @Throws(IndexServiceException::class, NotFoundException::class, DistributionException::class, MediaPackageException::class)
    fun changeThumbnail(@PathParam("mediapackageid") mediaPackageId: String,
                        @Context request: HttpServletRequest): Response {

        val optEvent = getEvent(mediaPackageId)
        if (optEvent.isNone) {
            return R.notFound()
        }

        if (WorkflowUtil.isActive(optEvent.get().workflowState)) {
            return R.locked()
        }

        val mp = index!!.getEventMediapackage(optEvent.get())

        try {
            val thumbnail = newThumbnailImpl()

            var track = Optional.empty<String>()
            var position = OptionalDouble.empty()

            val iter = ServletFileUpload().getItemIterator(request)
            while (iter.hasNext()) {
                val current = iter.next()
                if (!current.isFormField && THUMBNAIL_FILE.equals(current.fieldName, ignoreCase = true)) {
                    val distElement = thumbnail.upload(mp, current.openStream(), current.contentType)
                    return RestUtils.okJson(obj(f("thumbnail",
                            obj(
                                    f("position", thumbnail.defaultPosition),
                                    f("defaultPosition", thumbnail.defaultPosition),
                                    f("type", ThumbnailImpl.ThumbnailSource.UPLOAD.name),
                                    f("url", signUrl(distElement.getURI()))))))
                } else if (current.isFormField && THUMBNAIL_TRACK.equals(current.fieldName, ignoreCase = true)) {
                    val value = Streams.asString(current.openStream())
                    if (!THUMBNAIL_DEFAULT.equals(value, ignoreCase = true)) {
                        track = Optional.of(value)
                    }
                } else if (current.isFormField && THUMBNAIL_POSITION.equals(current.fieldName, ignoreCase = true)) {
                    val value = Streams.asString(current.openStream())
                    position = OptionalDouble.of(java.lang.Double.parseDouble(value))
                }
            }

            if (!position.isPresent) {
                return R.badRequest("Missing thumbnail position")
            }

            val distributedElement: MediaPackageElement
            val thumbnailSource: ThumbnailImpl.ThumbnailSource
            if (track.isPresent) {
                distributedElement = thumbnail.chooseThumbnail(mp, track.get(), position.asDouble)
                thumbnailSource = ThumbnailImpl.ThumbnailSource.SNAPSHOT
            } else {
                distributedElement = thumbnail.chooseDefaultThumbnail(mp, position.asDouble)
                thumbnailSource = ThumbnailImpl.ThumbnailSource.DEFAULT
            }
            return RestUtils.okJson(obj(f("thumbnail", obj(
                    f("type", thumbnailSource.name),
                    f("position", position.asDouble),
                    f("defaultPosition", thumbnail.defaultPosition),
                    f("url", signUrl(distributedElement.getURI()))
            ))))
        } catch (e: IOException) {
            logger.error("Error reading request body: {}", getStackTrace(e))
            return R.serverError()
        } catch (e: FileUploadException) {
            logger.error("Error reading request body: {}", getStackTrace(e))
            return R.serverError()
        } catch (e: PublicationException) {
            logger.error("Could not generate or publish thumbnail", e)
            return R.serverError()
        } catch (e: UnknownFileTypeException) {
            logger.error("Could not generate or publish thumbnail", e)
            return R.serverError()
        } catch (e: EncoderException) {
            logger.error("Could not generate or publish thumbnail", e)
            return R.serverError()
        }

    }

    @POST
    @Path("{mediapackageid}/editor.json")
    @Consumes(MediaType.APPLICATION_JSON)
    @RestQuery(name = "editVideo", description = "Takes editing information from the client side and processes it", returnDescription = "", pathParameters = [RestParameter(name = "mediapackageid", description = "The id of the media package", isRequired = true, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Editing information saved and processed", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "Media package not found", responseCode = HttpServletResponse.SC_NOT_FOUND), RestResponse(description = "The editing information cannot be parsed", responseCode = HttpServletResponse.SC_BAD_REQUEST)])
    @Throws(IndexServiceException::class, NotFoundException::class)
    fun editVideo(@PathParam("mediapackageid") mediaPackageId: String,
                  @Context request: HttpServletRequest): Response {
        var details: String
        try {
            request.inputStream.use { `is` -> details = IOUtils.toString(`is`, request.characterEncoding) }
        } catch (e: IOException) {
            logger.error("Error reading request body: {}", getStackTrace(e))
            return R.serverError()
        }

        val parser = JSONParser()
        val editingInfo: EditingInfo
        try {
            val detailsJSON = parser.parse(details) as JSONObject
            editingInfo = EditingInfo.parse(detailsJSON)
        } catch (e: Exception) {
            logger.warn("Unable to parse concat information ({})", details, e)
            return R.badRequest("Unable to parse details")
        }

        val optEvent = getEvent(mediaPackageId)
        if (optEvent.isNone) {
            return R.notFound()
        } else {
            if (WorkflowUtil.isActive(optEvent.get().workflowState)) {
                return R.locked()
            }

            val mediaPackage = index!!.getEventMediapackage(optEvent.get())
            val smil: Smil
            try {
                smil = createSmilCuttingCatalog(editingInfo, mediaPackage)
            } catch (e: Exception) {
                logger.warn("Unable to create a SMIL cutting catalog ({}): {}", details, getStackTrace(e))
                return R.badRequest("Unable to create SMIL cutting catalog")
            }

            val workflowProperties = java.util.stream.Stream
                    .of(this.adminUIConfiguration!!.sourceTrackLeftFlavor, this.adminUIConfiguration!!.sourceTrackRightFlavor)
                    .flatMap { flavor ->
                        val r = java.util.stream.Stream.builder<Tuple<String, String>>()
                        val track = editingInfo.sourceTracks.stream().filter { s -> s.flavor == flavor }.findAny()
                        val audioHidden = track.map { e -> e.audio.hidden }.orElse(false)
                        r.accept(Tuple.tuple("hide_" + flavor.type + "_audio", java.lang.Boolean.toString(audioHidden)))
                        val videoHidden = track.map { e -> e.video.hidden }.orElse(false)
                        r.accept(Tuple.tuple("hide_" + flavor.type + "_video", java.lang.Boolean.toString(videoHidden)))
                        r.build()
                    }.collect<Map<String, String>, Any>(Collectors.toMap(Function<Tuple<String, String>, String> { it.getA() }, Function<Tuple<String, String>, String> { it.getB() }))

            WorkflowPropertiesUtil.storeProperties(assetManager!!, mediaPackage, workflowProperties)

            try {
                addSmilToArchive(mediaPackage, smil)
            } catch (e: IOException) {
                logger.warn("Unable to add SMIL cutting catalog to archive: {}", getStackTrace(e))
                return R.serverError()
            }

            // Update default thumbnail (if used) since position may change due to cutting
            var distributedThumbnail: MediaPackageElement? = null
            if (editingInfo.defaultThumbnailPosition.isPresent) {
                try {
                    val thumbnailImpl = newThumbnailImpl()
                    val optThumbnail = thumbnailImpl
                            .getThumbnail(mediaPackage, urlSigningService, expireSeconds)
                    if (optThumbnail.isPresent && optThumbnail.get().type == ThumbnailImpl.ThumbnailSource.DEFAULT) {
                        distributedThumbnail = thumbnailImpl
                                .chooseDefaultThumbnail(mediaPackage, editingInfo.defaultThumbnailPosition.asDouble)
                    }
                } catch (e: UrlSigningException) {
                    logger.error("Error while trying to serialize the thumbnail url because: {}", getStackTrace(e))
                    return R.serverError()
                } catch (e: URISyntaxException) {
                    logger.error("Error while trying to serialize the thumbnail url because: {}", getStackTrace(e))
                    return R.serverError()
                } catch (e: IOException) {
                    logger.error("Error while updating default thumbnail because: {}", getStackTrace(e))
                    return R.serverError()
                } catch (e: DistributionException) {
                    logger.error("Error while updating default thumbnail because: {}", getStackTrace(e))
                    return R.serverError()
                } catch (e: EncoderException) {
                    logger.error("Error while updating default thumbnail because: {}", getStackTrace(e))
                    return R.serverError()
                } catch (e: PublicationException) {
                    logger.error("Error while updating default thumbnail because: {}", getStackTrace(e))
                    return R.serverError()
                } catch (e: UnknownFileTypeException) {
                    logger.error("Error while updating default thumbnail because: {}", getStackTrace(e))
                    return R.serverError()
                } catch (e: MediaPackageException) {
                    logger.error("Error while updating default thumbnail because: {}", getStackTrace(e))
                    return R.serverError()
                }

            }

            if (editingInfo.postProcessingWorkflow.isPresent) {
                val workflowId = editingInfo.postProcessingWorkflow.get()
                try {
                    val workflowParameters = WorkflowPropertiesUtil
                            .getLatestWorkflowProperties(assetManager!!, mediaPackage.identifier.compact())
                    val workflows = Workflows(assetManager, workspace, workflowService)
                    workflows.applyWorkflowToLatestVersion(`$`(mediaPackage.identifier.toString()),
                            ConfiguredWorkflow.workflow(workflowService!!.getWorkflowDefinitionById(workflowId), workflowParameters))
                            .run()
                } catch (e: AssetManagerException) {
                    logger.warn("Unable to start workflow '{}' on archived media package '{}': {}",
                            workflowId, mediaPackage, getStackTrace(e))
                    return R.serverError()
                } catch (e: WorkflowDatabaseException) {
                    logger.warn("Unable to load workflow '{}' from workflow service: {}", workflowId, getStackTrace(e))
                    return R.serverError()
                } catch (e: NotFoundException) {
                    logger.warn("Workflow '{}' not found", workflowId)
                    return R.badRequest("Workflow not found")
                }

            }

            if (distributedThumbnail != null) {
                return getVideoEditor(mediaPackageId)
            }
        }

        return R.ok()
    }

    /**
     * Creates a SMIL cutting catalog based on the passed editing information and the media package.
     *
     * @param editingInfo
     * the editing information
     * @param mediaPackage
     * the media package
     * @return a SMIL catalog
     * @throws SmilException
     * if creating the SMIL catalog failed
     */
    @Throws(SmilException::class)
    internal fun createSmilCuttingCatalog(editingInfo: EditingInfo, mediaPackage: MediaPackage): Smil {
        // Create initial SMIL catalog
        var smilResponse = smilService!!.createNewSmil(mediaPackage)

        // Add tracks to the SMIL catalog
        val tracks = ArrayList<Track>()

        for (trackId in editingInfo.concatTracks) {
            var track: Track? = mediaPackage.getTrack(trackId)
            if (track == null) {
                val trackOpt = getInternalPublication(mediaPackage).toStream().bind(object : Fn<Publication, List<Track>>() {
                    override fun apply(a: Publication): List<Track> {
                        return Arrays.asList(*a.tracks)
                    }
                }).filter(object : Fn<Track, Boolean>() {
                    override fun apply(a: Track): Boolean? {
                        return trackId == a.identifier
                    }
                }).head()
                if (trackOpt.isNone)
                    throw IllegalStateException(
                            format("The track '%s' doesn't exist in media package '%s'", trackId, mediaPackage))

                track = trackOpt.get()
            }
            tracks.add(track)
        }

        for (segment in editingInfo.concatSegments) {
            smilResponse = smilService!!.addParallel(smilResponse.smil)
            val parentId = smilResponse.entity.id

            val duration = segment.b - segment.a
            smilResponse = smilService!!.addClips(smilResponse.smil, parentId, tracks.toTypedArray(),
                    segment.a, duration)
        }

        return smilResponse.smil
    }

    /**
     * Adds the SMIL file as [Catalog] to the media package and sends the updated media package to the archive.
     *
     * @param mediaPackage
     * the media package to at the SMIL catalog
     * @param smil
     * the SMIL catalog
     * @return the updated media package
     * @throws IOException
     * if the SMIL catalog cannot be read or not be written to the archive
     */
    @Throws(IOException::class)
    internal fun addSmilToArchive(mediaPackage: MediaPackage, smil: Smil): MediaPackage {
        val mediaPackageElementFlavor = adminUIConfiguration!!.smilCatalogFlavor
        //set default catalog Id if there is none existing
        var catalogId = smil.id
        val catalogs = mediaPackage.catalogs

        //get the first smil/cutting  catalog-ID to overwrite it with new smil info
        for (p in catalogs) {
            if (p.flavor.matches(mediaPackageElementFlavor)) {
                logger.debug("Set Idendifier for Smil-Catalog to: " + p.identifier)
                catalogId = p.identifier
                break
            }
        }
        var catalog: Catalog? = mediaPackage.getCatalog(catalogId)

        var smilURI: URI
        try {
            IOUtils.toInputStream(smil.toXML(), "UTF-8").use { `is` -> smilURI = workspace!!.put(mediaPackage.identifier.compact(), catalogId, TARGET_FILE_NAME, `is`) }
        } catch (e: SAXException) {
            logger.error("Error while serializing the SMIL catalog to XML: {}", e.message)
            throw IOException(e)
        } catch (e: JAXBException) {
            logger.error("Error while serializing the SMIL catalog to XML: {}", e.message)
            throw IOException(e)
        }

        if (catalog == null) {
            val mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            catalog = mpeBuilder.elementFromURI(smilURI, MediaPackageElement.Type.Catalog,
                    adminUIConfiguration!!.smilCatalogFlavor) as Catalog
            mediaPackage.add(catalog)
        }
        catalog.setURI(smilURI)
        catalog.identifier = catalogId
        catalog.mimeType = MimeTypes.XML
        for (tag in adminUIConfiguration!!.smilCatalogTags) {
            catalog.addTag(tag)
        }
        // setting the URI to a new source so the checksum will most like be invalid
        catalog.checksum = null

        try {
            assetManager!!.takeSnapshot(mediaPackage)
        } catch (e: AssetManagerException) {
            logger.error("Error while adding the updated media package ({}) to the archive", mediaPackage.identifier, e)
            throw IOException(e)
        }

        return mediaPackage
    }

    private fun getInternalPublication(mp: MediaPackage): Opt<Publication> {
        return `$`(*mp.publications).filter(object : Fn<Publication, Boolean>() {
            override fun apply(a: Publication): Boolean? {
                return InternalPublicationChannel.CHANNEL_ID == a.channel
            }
        }).head()
    }

    /**
     * Returns `true` if the media package is ready to be edited.
     *
     * @param mediaPackageId
     * the media package identifier
     */
    private fun isEditorAvailable(mediaPackageId: String): Boolean {
        val optEvent = getEvent(mediaPackageId)
        return if (optEvent.isSome) {
            Source.ARCHIVE == index!!.getEventSource(optEvent.get())
        } else {
            // No event found
            false
        }
    }

    /**
     * Get an [Event]
     *
     * @param mediaPackageId
     * The mediapackage id that is also the event id.
     * @return The event if available or none if it is missing.
     */
    private fun getEvent(mediaPackageId: String): Opt<Event> {
        try {
            return index!!.getEvent(mediaPackageId, searchIndex)
        } catch (e: SearchIndexException) {
            logger.error("Error while reading event '{}' from search index: {}", mediaPackageId, getStackTrace(e))
            return Opt.none()
        }

    }

    /**
     * Tries to find a waveform for a given track in the media package. If a waveform is found the corresponding
     * [Publication] is returned, [Opt.none] otherwise.
     *
     * @param mp
     * the media package to scan for the waveform
     * @param track
     * the track
     */
    private fun getWaveformForTrack(mp: MediaPackage, track: MediaPackageElement): Opt<Attachment> {
        return `$`(getInternalPublication(mp)).bind(object : Fn<Publication, List<Attachment>>() {
            override fun apply(a: Publication): List<Attachment> {
                return Arrays.asList(*a.attachments)
            }
        }).filter(object : Fn<Attachment, Boolean>() {
            override fun apply(att: Attachment): Boolean? {
                return if (track.flavor == null || att.flavor == null) false else track.flavor.type == att.flavor.type && att.flavor.subtype == adminUIConfiguration!!.waveformSubtype

            }
        }).head()
    }

    /**
     * Analyzes the media package and tries to get information about segments out of it.
     *
     * @param mediaPackage
     * the media package
     * @return a list of segments or an empty list if no segments could be found.
     */
    private fun getSegments(mediaPackage: MediaPackage): List<Tuple<Long, Long>> {
        var segments: MutableList<Tuple<Long, Long>> = ArrayList()
        for (smilCatalog in mediaPackage.getCatalogs(adminUIConfiguration!!.smilCatalogFlavor)) {
            try {
                val smil = smilService!!.fromXml(workspace!!.get(smilCatalog.getURI())).smil
                segments = mergeSegments(segments, getSegmentsFromSmil(smil))
            } catch (e: NotFoundException) {
                logger.warn("File '{}' could not be loaded by workspace service: {}", smilCatalog.getURI(), getStackTrace(e))
            } catch (e: IOException) {
                logger.warn("Reading file '{}' from workspace service failed: {}", smilCatalog.getURI(), getStackTrace(e))
            } catch (e: SmilException) {
                logger.warn("Error while parsing SMIL catalog '{}': {}", smilCatalog.getURI(), getStackTrace(e))
            }

        }

        if (!segments.isEmpty())
            return segments

        // Read from silence detection flavors
        for (smilCatalog in mediaPackage.getCatalogs(adminUIConfiguration!!.smilSilenceFlavor)) {
            try {
                val smil = smilService!!.fromXml(workspace!!.get(smilCatalog.getURI())).smil
                segments = mergeSegments(segments, getSegmentsFromSmil(smil))
            } catch (e: NotFoundException) {
                logger.warn("File '{}' could not be loaded by workspace service: {}", smilCatalog.getURI(), getStackTrace(e))
            } catch (e: IOException) {
                logger.warn("Reading file '{}' from workspace service failed: {}", smilCatalog.getURI(), getStackTrace(e))
            } catch (e: SmilException) {
                logger.warn("Error while parsing SMIL catalog '{}': {}", smilCatalog.getURI(), getStackTrace(e))
            }

        }

        // Check for single segment to ignore
        if (segments.size == 1) {
            val singleSegment = segments[0]
            if (singleSegment.a == 0 && singleSegment.b >= mediaPackage.duration)
                segments.removeAt(0)
        }

        return segments
    }

    fun mergeSegments(segments: MutableList<Tuple<Long, Long>>, segments2: MutableList<Tuple<Long, Long>>): MutableList<Tuple<Long, Long>> {
        // Merge conflicting segments
        val mergedSegments = mergeInternal(segments, segments2)

        // Sort segments
        Collections.sort(mergedSegments) { t1, t2 -> t1.a.compareTo(t2.a) }

        return mergedSegments
    }

    /**
     * Merges two different segments lists together. Keeps untouched segments and combines touching segments by the
     * overlapping points.
     *
     * @param segments
     * the first segments to be merge
     * @param segments2
     * the second segments to be merge
     * @return the merged segments
     */
    private fun mergeInternal(segments: MutableList<Tuple<Long, Long>>, segments2: MutableList<Tuple<Long, Long>>): List<Tuple<Long, Long>> {
        val it = segments.iterator()
        while (it.hasNext()) {
            val seg = it.next()
            val it2 = segments2.iterator()
            while (it2.hasNext()) {
                val seg2 = it2.next()
                val combinedStart = Math.max(seg.a, seg2.a)
                val combinedEnd = Math.min(seg.b, seg2.b)
                if (combinedEnd > combinedStart) {
                    it.remove()
                    it2.remove()
                    val newSegments = ArrayList(segments)
                    newSegments.add(tuple(combinedStart, combinedEnd))
                    return mergeInternal(newSegments, segments2)
                }
            }
        }
        segments.addAll(segments2)
        return segments
    }

    /**
     * Extracts the segments of a SMIL catalog and returns them as a list of tuples (start, end).
     *
     * @param smil
     * the SMIL catalog
     * @return the list of segments
     */
    internal fun getSegmentsFromSmil(smil: Smil): MutableList<Tuple<Long, Long>> {
        val segments = ArrayList<Tuple<Long, Long>>()
        for (elem in smil.body.mediaElements) {
            if (elem is SmilMediaContainer) {
                for (video in elem.elements) {
                    if (video is SmilMediaElement) {
                        try {
                            segments.add(Tuple.tuple(video.clipBeginMS, video.clipEndMS))
                            break
                        } catch (e: SmilException) {
                            logger.warn("Media element '{}' of SMIL catalog '{}' seems to be invalid: {}",
                                    video, smil, e)
                        }

                    }
                }
            }
        }
        return segments
    }

    private fun signUrl(baseUrl: URI): String {
        val url = baseUrl.toString()
        if (urlSigningService!!.accepts(url)) {
            logger.trace("URL signing service has accepted '{}'", url)
            try {
                val signedUrl = URI(urlSigningService!!.sign(url, expireSeconds, null, null))
                return signedUrl.toString()
            } catch (e: URISyntaxException) {
                logger.error("Error while trying to sign the preview urls because: {}", getStackTrace(e))
                throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
            } catch (e: UrlSigningException) {
                logger.error("Error while trying to sign the preview urls because: {}", getStackTrace(e))
                throw WebApplicationException(e, SC_INTERNAL_SERVER_ERROR)
            }

        } else {
            logger.trace("URL signing service did not accept '{}'", url)
            return url
        }
    }

    internal class SourceTrackSubInfo(private val present: Boolean, private val previewImage: String?, private val hidden: Boolean) {

        fun toJson(): JObject {
            return if (present) {
                obj(f("present", true), f("preview_image", previewImage?.let { v(it) } ?: Jsons.NULL),
                        f("hidden", hidden))
            } else obj(f("present", false))
        }

        companion object {

            fun parse(`object`: JSONObject): SourceTrackSubInfo {
                var hidden: Boolean? = `object`["hidden"] as Boolean
                if (hidden == null) {
                    hidden = java.lang.Boolean.FALSE
                }
                return SourceTrackSubInfo(`object`["present"] as Boolean, `object`["preview_image"] as String, hidden!!)
            }
        }
    }

    internal class SourceTrackInfo(private val flavorType: String, private val flavorSubtype: String, private val audio: SourceTrackSubInfo,
                                   private val video: SourceTrackSubInfo, private val side: String) {

        val flavor: MediaPackageElementFlavor
            get() = MediaPackageElementFlavor(flavorType, flavorSubtype)

        fun toJson(): JObject {
            val flavor = obj(f("type", flavorType), f("subtype", flavorSubtype))
            return obj(f("flavor", flavor), f("audio", audio.toJson()), f("video", video.toJson()), f("side", side))
        }

        companion object {

            fun parse(`object`: JSONObject): SourceTrackInfo {
                val flavor = `object`["flavor"] as JSONObject
                return SourceTrackInfo(flavor["type"] as String, flavor["subtype"] as String,
                        SourceTrackSubInfo.parse(`object`["audio"] as JSONObject),
                        SourceTrackSubInfo.parse(`object`["video"] as JSONObject),
                        `object`["side"] as String)
            }
        }
    }

    /** Provides access to the parsed editing information  */
    internal class EditingInfo private constructor(private val segments: List<Tuple<Long, Long>>, private val tracks: List<String>, private val sourceTracks: List<SourceTrackInfo>,
                                                   /** Returns the optional workflow to start  */
                                                   val postProcessingWorkflow: Optional<String>,
                                                   /** Returns the optional default thumbnail position.  */
                                                   val defaultThumbnailPosition: OptionalDouble) {

        /**
         * Returns a list of [Tuple] that each represents a segment. [Tuple.getA] marks the start point,
         * [Tuple.getB] the endpoint of the segement.
         */
        val concatSegments: List<Tuple<Long, Long>>
            get() = Collections.unmodifiableList(segments)

        /** Returns a list of track identifiers.  */
        val concatTracks: List<String>
            get() = Collections.unmodifiableList(tracks)

        companion object {

            /**
             * Parse [JSONObject] to [EditingInfo].
             *
             * @param obj
             * the JSON object to parse
             * @return all editing information found in the JSON object
             */
            fun parse(obj: JSONObject): EditingInfo {

                val concatObject = requireNonNull(obj[CONCAT_KEY] as JSONObject)
                val jsonSegments = requireNonNull(concatObject[SEGMENTS_KEY] as JSONArray)
                val jsonTracks = requireNonNull(concatObject[TRACKS_KEY] as JSONArray)
                val jsonSourceTracks = requireNonNull(concatObject[SOURCE_TRACKS_KEY] as JSONArray)

                val segments = ArrayList<Tuple<Long, Long>>()
                for (segment in jsonSegments) {
                    val jSegment = segment as JSONObject
                    val start = jSegment[START_KEY] as Long
                    val end = jSegment[END_KEY] as Long
                    if (end < start)
                        throw IllegalArgumentException("The end date of a segment must be after the start date of the segment")
                    segments.add(Tuple.tuple(start, end))
                }

                val tracks = ArrayList<String>()
                for (track in jsonTracks) {
                    tracks.add(track as String)
                }

                var defaultThumbnailPosition = OptionalDouble.empty()
                val defaultThumbnailPositionObj = obj[DEFAULT_THUMBNAIL_POSITION_KEY]
                if (defaultThumbnailPositionObj != null) {
                    defaultThumbnailPosition = OptionalDouble.of(java.lang.Double.parseDouble(defaultThumbnailPositionObj.toString()))
                }

                val sourceTracks = ArrayList<SourceTrackInfo>()
                for (sourceTrack in jsonSourceTracks) {
                    sourceTracks.add(SourceTrackInfo.parse(sourceTrack as JSONObject))
                }

                return EditingInfo(
                        segments,
                        tracks,
                        sourceTracks,
                        Optional.ofNullable(obj["workflow"] as String),
                        defaultThumbnailPosition)
            }
        }
    }

    companion object {
        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ToolsEndpoint::class.java)

        /** The default file name for generated Smil catalogs.  */
        private val TARGET_FILE_NAME = "cut.smil"

        /** The Json key for the cutting details object.  */
        private val CONCAT_KEY = "concat"

        /** The Json key for the end of a segment.  */
        private val END_KEY = "end"

        /** The Json key for the beginning of a segment.  */
        private val START_KEY = "start"

        /** The Json key for the segments array.  */
        private val SEGMENTS_KEY = "segments"

        /** The Json key for the tracks array.  */
        private val TRACKS_KEY = "tracks"

        /** The Json key for the default thumbnail position.  */
        private val DEFAULT_THUMBNAIL_POSITION_KEY = "defaultThumbnailPosition"

        /** The Json key for the source_tracks array.  */
        private val SOURCE_TRACKS_KEY = "source_tracks"

        /** Tag that marks workflow for being used from the editor tool  */
        private val EDITOR_WORKFLOW_TAG = "editor"

        /** Field names in thumbnail request.  */
        private val THUMBNAIL_FILE = "FILE"
        private val THUMBNAIL_TRACK = "TRACK"
        private val THUMBNAIL_POSITION = "POSITION"
        private val THUMBNAIL_DEFAULT = "DEFAULT"
    }

}
