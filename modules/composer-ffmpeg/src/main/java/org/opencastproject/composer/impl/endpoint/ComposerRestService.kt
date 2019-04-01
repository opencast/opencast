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

package org.opencastproject.composer.impl.endpoint

import org.opencastproject.util.doc.rest.RestParameter.Type.TEXT

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.EncodingProfileImpl
import org.opencastproject.composer.api.EncodingProfileList
import org.opencastproject.composer.api.LaidOutElement
import org.opencastproject.composer.layout.Dimension
import org.opencastproject.composer.layout.Layout
import org.opencastproject.composer.layout.Serializer
import org.opencastproject.job.api.JaxbJob
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.rest.AbstractJobProducerEndpoint
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.util.JsonObj
import org.opencastproject.util.LocalHashMap
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UrlSupport
import org.opencastproject.util.data.Option
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestParameter.Type
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.ArrayList
import java.util.Arrays
import java.util.LinkedList

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.DefaultValue
import javax.ws.rs.FormParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * A REST endpoint delegating functionality to the [ComposerService]
 */
@Path("/")
@RestService(name = "composer", title = "Composer", abstractText = "This service creates and augments Opencast media packages that include media tracks, metadata " + "catalogs and attachments.", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is " + "not working and is either restarting or has failed", "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"])
class ComposerRestService : AbstractJobProducerEndpoint() {

    /** The base server URL  */
    protected var serverUrl: String

    /** The composer service  */
    protected var composerService: ComposerService? = null

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

    /** The smil service  */
    protected var smilService: SmilService? = null

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.rest.AbstractJobProducerEndpoint.getService
     */
    override val service: JobProducer?
        get() = if (composerService is JobProducer)
            composerService as JobProducer?
        else
            null

    fun setSmilService(smilService: SmilService) {
        this.smilService = smilService
    }

    /**
     * Sets the composer service.
     *
     * @param composerService
     * the composer service
     */
    fun setComposerService(composerService: ComposerService) {
        this.composerService = composerService
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
     * Encodes a track.
     *
     * @param sourceTrackAsXml
     * The source track
     * @param profileId
     * The profile to use in encoding this track
     * @return A response containing the job for this encoding job in the response body.
     * @throws Exception
     */
    @POST
    @Path("encode")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "encode", description = "Starts an encoding process, based on the specified encoding profile ID and the track", restParameters = [RestParameter(description = "The track containing the stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = VIDEO_TRACK_DEFAULT), RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "mp4-medium.http")], reponses = [RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun encode(@FormParam("sourceTrack") sourceTrackAsXml: String, @FormParam("profileId") profileId: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceTrackAsXml) || StringUtils.isBlank(profileId))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build()

        // Deserialize the track
        val sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackAsXml)
        if (!Track.TYPE.equals(sourceTrack.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build()

        try {
            // Asynchronously encode the specified tracks
            val job = composerService!!.encode(sourceTrack as Track, profileId)
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to encode the track: " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Encodes a track to multiple tracks in parallel.
     *
     * @param sourceTrackAsXml
     * The source track
     * @param profileId
     * The profile to use in encoding this track
     * @return A response containing the job for this encoding job in the response body.
     * @throws Exception
     */
    @POST
    @Path("parallelencode")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "parallelencode", description = "Starts an encoding process, based on the specified encoding profile ID and the track", restParameters = [RestParameter(description = "The track containing the stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = VIDEO_TRACK_DEFAULT), RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "mp4-medium.http")], reponses = [RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK)], returnDescription = "")
    @Throws(Exception::class)
    fun parallelencode(@FormParam("sourceTrack") sourceTrackAsXml: String?, @FormParam("profileId") profileId: String?): Response {
        // Ensure that the POST parameters are present
        if (sourceTrackAsXml == null || profileId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build()
        }

        // Deserialize the track
        val sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackAsXml)
        if (!Track.TYPE.equals(sourceTrack.elementType)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build()
        }

        // Asynchronously encode the specified tracks
        val job = composerService!!.parallelEncode(sourceTrack as Track, profileId)
                ?: return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Encoding failed").build()
        return Response.ok().entity(JaxbJob(job)).build()
    }

    /**
     * Trims a track to a new length.
     *
     * @param sourceTrackAsXml
     * The source track
     * @param profileId
     * the encoding profile to use for trimming
     * @param start
     * the new trimming start time
     * @param duration
     * the new video duration
     * @return A response containing the job for this encoding job in the response body.
     * @throws Exception
     */
    @POST
    @Path("trim")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "trim", description = "Starts a trimming process, based on the specified track, start time and duration in ms", restParameters = [RestParameter(description = "The track containing the stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = VIDEO_TRACK_DEFAULT), RestParameter(description = "The encoding profile to use for trimming", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "trim.work"), RestParameter(description = "The start time in milisecond", isRequired = true, name = "start", type = Type.STRING, defaultValue = "0"), RestParameter(description = "The duration in milisecond", isRequired = true, name = "duration", type = Type.STRING, defaultValue = "10000")], reponses = [RestResponse(description = "Results in an xml document containing the job for the trimming task", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If the start time is negative or exceeds the track duration", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "If the duration is negative or, including the new start time, exceeds the track duration", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun trim(@FormParam("sourceTrack") sourceTrackAsXml: String, @FormParam("profileId") profileId: String,
             @FormParam("start") start: Long, @FormParam("duration") duration: Long): Response {
        var start = start
        var duration = duration
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceTrackAsXml) || StringUtils.isBlank(profileId))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build()

        // Deserialize the track
        val sourceElement = MediaPackageElementParser.getFromXml(sourceTrackAsXml)
        if (!Track.TYPE.equals(sourceElement.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build()

        // Make sure the trim times make sense
        val sourceTrack = sourceElement as Track

        if (sourceTrack.duration == null)
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element does not have a duration")
                    .build()

        if (start < 0) {
            start = 0
        } else if (duration <= 0) {
            duration = sourceTrack.duration!! - start
        } else if (start + duration > sourceTrack.duration) {
            duration = sourceTrack.duration!! - start
        }

        try {
            // Asynchronously encode the specified tracks
            val job = composerService!!.trim(sourceTrack, profileId, start, duration)
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to trim the track: " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Encodes a track.
     *
     * @param audioSourceTrackXml
     * The audio source track
     * @param videoSourceTrackXml
     * The video source track
     * @param profileId
     * The profile to use in encoding this track
     * @return A response containing the job for this encoding job in the response body.
     * @throws Exception
     */
    @POST
    @Path("mux")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "mux", description = "Starts an encoding process, which will mux the two tracks using the given encoding profile", restParameters = [RestParameter(description = "The track containing the audio stream", isRequired = true, name = "sourceAudioTrack", type = Type.TEXT, defaultValue = AUDIO_TRACK_DEFAULT), RestParameter(description = "The track containing the video stream", isRequired = true, name = "sourceVideoTrack", type = Type.TEXT, defaultValue = VIDEO_TRACK_DEFAULT), RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "mp4-medium.http")], reponses = [RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if the source tracks aren't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun mux(@FormParam("audioSourceTrack") audioSourceTrackXml: String,
            @FormParam("videoSourceTrack") videoSourceTrackXml: String, @FormParam("profileId") profileId: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(audioSourceTrackXml) || StringUtils.isBlank(videoSourceTrackXml)
                || StringUtils.isBlank(profileId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("audioSourceTrack, videoSourceTrack, and profileId must not be null").build()
        }

        // Deserialize the audio track
        val audioSourceTrack = MediaPackageElementParser.getFromXml(audioSourceTrackXml)
        if (!Track.TYPE.equals(audioSourceTrack.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("audioSourceTrack must be of type track").build()

        // Deserialize the video track
        val videoSourceTrack = MediaPackageElementParser.getFromXml(videoSourceTrackXml)
        if (!Track.TYPE.equals(videoSourceTrack.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("videoSourceTrack must be of type track").build()

        try {
            // Asynchronously encode the specified tracks
            val job = composerService!!.mux(videoSourceTrack as Track, audioSourceTrack as Track, profileId)
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to mux tracks: " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Encodes a track in a media package.
     *
     * @param sourceTrackXml
     * The source track
     * @param profileId
     * The profile to use in encoding this track
     * @param times
     * one or more times in seconds separated by comma
     * @return A [Response] with the resulting track in the response body
     * @throws Exception
     */
    @POST
    @Path("image")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "image", description = "Starts an image extraction process, based on the specified encoding profile ID and the source track", restParameters = [RestParameter(description = "The track containing the video stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = VIDEO_TRACK_DEFAULT), RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "player-preview.http"), RestParameter(description = "The number of seconds (many numbers can be specified, separated by semicolon) into the video to extract the image", isRequired = false, name = "time", type = Type.STRING), RestParameter(description = "An optional set of key=value\\n properties", isRequired = false, name = "properties", type = TEXT)], reponses = [RestResponse(description = "Results in an xml document containing the image attachment", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "The image extraction job")
    @Throws(Exception::class)
    fun image(@FormParam("sourceTrack") sourceTrackXml: String, @FormParam("profileId") profileId: String,
              @FormParam("time") times: String, @FormParam("properties") localMap: LocalHashMap?): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceTrackXml) || StringUtils.isBlank(profileId))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build()

        // Deserialize the source track
        val sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackXml)
        if (!Track.TYPE.equals(sourceTrack.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build()

        var timeBased = false
        var timeArray: DoubleArray? = null
        if (StringUtils.isNotBlank(times)) {
            // parse time codes
            try {
                timeArray = parseTimeArray(times)
            } catch (e: Exception) {
                return Response.status(Response.Status.BAD_REQUEST).entity("could not parse times: invalid format").build()
            }

            timeBased = true
        } else if (localMap == null) {
            return Response.status(Response.Status.BAD_REQUEST).build()
        }

        try {
            // Asynchronously encode the specified tracks
            val job: Job
            if (timeBased) {
                job = composerService!!.image(sourceTrack as Track, profileId, *timeArray)
            } else {
                job = composerService!!.image(sourceTrack as Track, profileId, localMap!!.getMap())
            }
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to extract image(s): " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Encodes a track in a media package.
     *
     * @param sourceTrackXml
     * The source track
     * @param profileId
     * The profile to use in encoding this track
     * @param times
     * one or more times in seconds separated by comma
     * @return A [Response] with the resulting track in the response body
     * @throws Exception
     */
    @POST
    @Path("imagesync")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "imagesync", description = "Synchronously extracts an image, based on the specified encoding profile ID and the source track", restParameters = [RestParameter(description = "The track containing the video stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = VIDEO_TRACK_DEFAULT), RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "player-preview.http"), RestParameter(description = "The number of seconds (many numbers can be specified, separated by semicolon) into the video to extract the image", isRequired = false, name = "time", type = Type.STRING)], reponses = [RestResponse(description = "Results in an xml document containing the image attachment", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "The extracted image")
    @Throws(Exception::class)
    fun imageSync(@FormParam("sourceTrack") sourceTrackXml: String, @FormParam("profileId") profileId: String,
                  @FormParam("time") times: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceTrackXml) || StringUtils.isBlank(profileId) || StringUtils.isBlank(times)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack, times, and profileId must not be null").build()
        }

        // Deserialize the source track
        val sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackXml)
        if (!Track.TYPE.equals(sourceTrack.elementType)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build()
        }

        var timeArray: DoubleArray? = null
        // parse time codes
        try {
            timeArray = parseTimeArray(times)
        } catch (e: Exception) {
            return Response.status(Response.Status.BAD_REQUEST).entity("could not parse times: invalid format").build()
        }

        try {
            val result = composerService!!.imageSync(sourceTrack as Track, profileId, *timeArray)
            return Response.ok().entity(MediaPackageElementParser.getArrayAsXml(result)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to extract image(s): " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Compose two videos into one with an optional watermark.
     *
     * @param compositeSizeJson
     * The composite track dimension as JSON
     * @param lowerTrackXml
     * The lower track of the composition as XML
     * @param lowerLayoutJson
     * The lower layout as JSON
     * @param upperTrackXml
     * The upper track of the composition as XML
     * @param upperLayoutJson
     * The upper layout as JSON
     * @param watermarkAttachmentXml
     * The watermark image attachment of the composition as XML
     * @param watermarkLayoutJson
     * The watermark layout as JSON
     * @param profileId
     * The encoding profile to use
     * @param background
     * The background color
     * @return A [Response] with the resulting track in the response body
     * @throws Exception
     */
    @POST
    @Path("composite")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "composite", description = "Starts a video compositing process, based on the specified resolution, encoding profile ID, the source elements and their layouts", restParameters = [RestParameter(description = "The resolution size of the resulting video as JSON", isRequired = true, name = "compositeSize", type = Type.STRING), RestParameter(description = "The lower source track containing the lower video", isRequired = true, name = "lowerTrack", type = Type.TEXT), RestParameter(description = "The lower layout containing the JSON definition of the layout", isRequired = true, name = "lowerLayout", type = Type.TEXT), RestParameter(description = "The upper source track containing the upper video", isRequired = false, name = "upperTrack", type = Type.TEXT), RestParameter(description = "The upper layout containing the JSON definition of the layout", isRequired = false, name = "upperLayout", type = Type.TEXT), RestParameter(description = "The watermark source attachment containing watermark image", isRequired = false, name = "watermarkTrack", type = Type.TEXT), RestParameter(description = "The watermark layout containing the JSON definition of the layout", isRequired = false, name = "watermarkLayout", type = Type.TEXT), RestParameter(description = "The background color", isRequired = false, name = "background", type = Type.TEXT, defaultValue = "black"), RestParameter(description = "The name of the audio source (lower or upper or both)", isRequired = false, name = "audioSourceName", type = Type.TEXT, defaultValue = ComposerService.BOTH), RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING)], reponses = [RestResponse(description = "Results in an xml document containing the compound video track", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if the source elements aren't from the right type", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun composite(@FormParam("compositeSize") compositeSizeJson: String,
                  @FormParam("lowerTrack") lowerTrackXml: String, @FormParam("lowerLayout") lowerLayoutJson: String,
                  @FormParam("upperTrack") upperTrackXml: String, @FormParam("upperLayout") upperLayoutJson: String,
                  @FormParam("watermarkAttachment") watermarkAttachmentXml: String,
                  @FormParam("watermarkLayout") watermarkLayoutJson: String, @FormParam("profileId") profileId: String,
                  @FormParam("background") @DefaultValue("black") background: String,
                  @FormParam("sourceAudioName") @DefaultValue(ComposerService.BOTH) sourceAudioName: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(compositeSizeJson) || StringUtils.isBlank(lowerTrackXml)
                || StringUtils.isBlank(lowerLayoutJson) || StringUtils.isBlank(profileId))
            return Response.status(Response.Status.BAD_REQUEST).entity("One of the required parameters must not be null")
                    .build()

        // Deserialize the source elements
        val lowerTrack = MediaPackageElementParser.getFromXml(lowerTrackXml)
        val lowerLayout = Serializer.layout(JsonObj.jsonObj(lowerLayoutJson))
        if (!Track.TYPE.equals(lowerTrack.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("lowerTrack element must be of type track").build()
        val lowerLaidOutElement = LaidOutElement(lowerTrack as Track, lowerLayout)

        var upperLaidOutElement = Option.none<LaidOutElement<Track>>()
        if (StringUtils.isNotBlank(upperTrackXml)) {
            val upperTrack = MediaPackageElementParser.getFromXml(upperTrackXml)
            val upperLayout = Serializer.layout(JsonObj.jsonObj(upperLayoutJson))
            if (!Track.TYPE.equals(upperTrack.elementType)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("upperTrack element must be of type track").build()
            }
            upperLaidOutElement = Option.option(LaidOutElement(upperTrack as Track, upperLayout))
        }
        var watermarkLaidOutElement = Option.none<LaidOutElement<Attachment>>()
        if (StringUtils.isNotBlank(watermarkAttachmentXml)) {
            val watermarkLayout = Serializer.layout(JsonObj.jsonObj(watermarkLayoutJson))
            val watermarkAttachment = MediaPackageElementParser.getFromXml(watermarkAttachmentXml)
            if (!Attachment.TYPE.equals(watermarkAttachment.elementType))
                return Response.status(Response.Status.BAD_REQUEST).entity("watermarkTrack element must be of type track")
                        .build()
            watermarkLaidOutElement = Option.some(LaidOutElement(watermarkAttachment as Attachment,
                    watermarkLayout))
        }

        val compositeTrackSize = Serializer.dimension(JsonObj.jsonObj(compositeSizeJson))

        try {
            // Asynchronously composite the specified source elements
            val job = composerService!!.composite(compositeTrackSize, upperLaidOutElement, lowerLaidOutElement,
                    watermarkLaidOutElement, profileId, background, sourceAudioName)
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to composite video: " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Concat multiple tracks having the same codec to a single track.
     *
     * @param sourceTracksXml
     * an array of track to concat in order of the array as XML
     * @param profileId
     * The encoding profile to use
     * @param outputDimension
     * The output dimension as JSON
     * @return A [Response] with the resulting track in the response body
     * @throws Exception
     */
    @POST
    @Path("concat")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "concat", description = "Starts a video concating process from multiple videos, based on the specified encoding profile ID and the source tracks", restParameters = [RestParameter(description = "The source tracks to concat as XML", isRequired = true, name = "sourceTracks", type = Type.TEXT), RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING), RestParameter(description = "The resolution dimension of the concat video as JSON", isRequired = false, name = "outputDimension", type = Type.STRING), RestParameter(description = "The  frame rate of the concat video (should be positive, e.g. 25.0). Negative values and zero will cause no FFmpeg fps filter to be used in the filter chain.", isRequired = false, name = "outputFrameRate", type = Type.STRING), RestParameter(description = "The source files have the same codecs and should not be re-encoded", isRequired = false, name = "sameCodec", type = Type.TEXT, defaultValue = "false")], reponses = [RestResponse(description = "Results in an xml document containing the video track", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceTracks aren't from the type Track or not at least two tracks are present", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun concat(@FormParam("sourceTracks") sourceTracksXml: String, @FormParam("profileId") profileId: String,
               @FormParam("outputDimension") outputDimension: String, @FormParam("outputFrameRate") outputFrameRate: String,
               @FormParam("sameCodec") sameCodec: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceTracksXml) || StringUtils.isBlank(profileId))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTracks and profileId must not be null").build()

        // Deserialize the source track
        val tracks = MediaPackageElementParser.getArrayFromXml(sourceTracksXml)
        if (tracks.size < 2)
            return Response.status(Response.Status.BAD_REQUEST).entity("At least two tracks must be set to concat").build()

        for (elem in tracks) {
            if (!Track.TYPE.equals(elem.elementType))
                return Response.status(Response.Status.BAD_REQUEST).entity("sourceTracks must be of type track").build()
        }
        val fps = NumberUtils.toFloat(outputFrameRate, -1.0f)
        try {
            // Asynchronously concat the specified tracks together
            var dimension: Dimension? = null
            if (StringUtils.isNotBlank(outputDimension)) {
                dimension = Serializer.dimension(JsonObj.jsonObj(outputDimension))
            }
            val hasSameCodec = java.lang.Boolean.parseBoolean(sameCodec)
            var job: Job? = null
            if (fps > 0) {
                job = composerService!!.concat(profileId, dimension, fps, hasSameCodec, *tracks.toTypedArray<Track>())
            } else {
                job = composerService!!.concat(profileId, dimension, hasSameCodec, *tracks.toTypedArray<Track>())
            }
            return Response.ok().entity(JaxbJob(job!!)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to concat videos: " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Transforms an image attachment to a video track
     *
     * @param sourceAttachmentXml
     * The source image attachment
     * @param profileId
     * The profile to use for encoding
     * @param timeString
     * the length of the resulting video track in seconds
     * @return A [Response] with the resulting track in the response body
     * @throws Exception
     */
    @POST
    @Path("imagetovideo")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "imagetovideo", description = "Starts an image converting process to a video, based on the specified encoding profile ID and the source image attachment", restParameters = [RestParameter(description = "The resulting video time in seconds", isRequired = false, name = "time", type = Type.STRING, defaultValue = "1"), RestParameter(description = "The attachment containing the image to convert", isRequired = true, name = "sourceAttachment", type = Type.TEXT), RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING)], reponses = [RestResponse(description = "Results in an xml document containing the video track", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceAttachment isn't from the type Attachment", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun imageToVideo(@FormParam("sourceAttachment") sourceAttachmentXml: String,
                     @FormParam("profileId") profileId: String, @FormParam("time") @DefaultValue("1") timeString: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceAttachmentXml) || StringUtils.isBlank(profileId))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceAttachment and profileId must not be null")
                    .build()

        // parse time
        val time: Double?
        try {
            time = java.lang.Double.parseDouble(timeString)
        } catch (e: Exception) {
            logger.info("Unable to parse time {} as long value!", timeString)
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse time: invalid format").build()
        }

        // Deserialize the source track
        val sourceAttachment = MediaPackageElementParser.getFromXml(sourceAttachmentXml)
        if (!Attachment.TYPE.equals(sourceAttachment.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceAttachment element must be of type attachment")
                    .build()

        try {
            // Asynchronously convert the specified attachment to a video
            val job = composerService!!.imageToVideo(sourceAttachment as Attachment, profileId, time)
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to convert image to video: " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Converts an image to another format.
     *
     * @param sourceImageXml
     * The source image
     * @param profileId
     * The profile to use in image conversion
     * @return A [Response] with the resulting image in the response body
     * @throws Exception
     */
    @POST
    @Path("convertimage")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "convertimage", description = "Starts an image conversion process, based on the specified encoding profile ID and the source image", restParameters = [RestParameter(description = "The original image", isRequired = true, name = "sourceImage", type = Type.TEXT, defaultValue = IMAGE_ATTACHMENT_DEFAULT), RestParameter(description = "A comma separated list of encoding profiles to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "image-conversion.http")], reponses = [RestResponse(description = "Results in an xml document containing the image attachment", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceImage isn't from the type Attachment", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun convertImage(@FormParam("sourceImage") sourceImageXml: String, @FormParam("profileId") profileId: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceImageXml) || StringUtils.isBlank(profileId))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceImage and profileId must not be null").build()

        // Deserialize the source track
        val sourceImage = MediaPackageElementParser.getFromXml(sourceImageXml)
        if (!Attachment.TYPE.equals(sourceImage.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceImage element must be of type track").build()

        try {
            // Asynchronously convert the specified image
            val job = composerService!!.convertImage(sourceImage as Attachment, *StringUtils.split(profileId, ','))
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to convert image: " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Demuxes a track into multiple outputs
     *
     * @param sourceTrackAsXml
     * The source track
     * @param profileId
     * The profile to use in encoding this track
     * @return A response containing the job for this encoding job in the response body.
     * @throws Exception
     * - if it fails
     */
    @POST
    @Path("demux")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "demux", description = "Starts an demux process that produces multiple outputs, based on the specified encoding profile ID and the track", restParameters = [RestParameter(description = "The track containing the stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = VIDEO_TRACK_DEFAULT), RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "demux.work")], reponses = [RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun demux(@FormParam("sourceTrack") sourceTrackAsXml: String, @FormParam("profileId") profileId: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceTrackAsXml) || StringUtils.isBlank(profileId))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build()

        // Deserialize the track
        val sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackAsXml)
        if (!Track.TYPE.equals(sourceTrack.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build()

        try {
            // Asynchronously encode the specified tracks
            val job = composerService!!.demux(sourceTrack as Track, profileId)
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to encode the track: $e")
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * ProcessSmil - encode a video based on descriptions in a smil file into all format in the profileIds
     *
     * @param smilAsXml
     * - smil describing a list of videos and clips in them to make up one video
     * @param trackId
     * - a paramGroup Id in the smil file describing a track
     * @param mediaType
     * - audio only, video only or both
     * @param profileIds
     * - list of encoding profile ids
     * @return a job running the process
     * @throws Exception
     * if it fails
     */
    @POST
    @Path("processsmil")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "processsmil", description = "Starts an encoding process, based on the tracks and edit points in the smil and specified encoding profile IDs", restParameters = [RestParameter(description = "The smil containing the tracks and edit points", isRequired = true, name = "smilAsXml", type = Type.TEXT), RestParameter(description = "The id (paramgroup) of the track to encode", isRequired = false, name = "trackId", type = Type.STRING, defaultValue = ""), RestParameter(description = "MediaType - v for video only, a for audio only, audiovisual otherwise", isRequired = false, name = "mediaType", type = Type.STRING, defaultValue = "o"), RestParameter(description = "The encoding profiles to use", isRequired = true, name = "profileIds", type = Type.STRING)], reponses = [RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun processSmil(@FormParam("smilAsXml") smilAsXml: String, @FormParam("trackId") trackId: String,
                    @FormParam("mediaType") mediaType: String, @FormParam("profileIds") profileIds: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(smilAsXml) || StringUtils.isBlank(profileIds))
            return Response.status(Response.Status.BAD_REQUEST).entity("smil and profileId must not be null").build()

        // Deserialize the data
        val profiles = StringUtils.split(profileIds, ",")
        val smil: Smil
        try {
            smil = smilService!!.fromXml(smilAsXml).smil
        } catch (e: Exception) {
            return Response.status(Response.Status.BAD_REQUEST).entity("smil must be readable").build()
        }

        try {
            // Encode the specified tracks
            val job = composerService!!.processSmil(smil, trackId, mediaType, Arrays.asList(*profiles))
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to process the smil: $e")
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @POST
    @Path("multiencode")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "multiencode", description = "Starts an encoding process that produces multiple outputs, based on the specified encoding profile ID and the track", restParameters = [RestParameter(description = "The track containing the stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = VIDEO_TRACK_DEFAULT), RestParameter(description = "The comma-delimited encoding profiles to use", isRequired = true, name = "profileIds", type = Type.STRING, defaultValue = "mp4-medium.http,mp4-low.http")], reponses = [RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun multiEncode(@FormParam("sourceTrack") sourceTrackAsXml: String,
                    @FormParam("profileIds") profileIds: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceTrackAsXml) || StringUtils.isBlank(profileIds))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileIds must not be null").build()

        // Deserialize the track
        val sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackAsXml)
        if (!Track.TYPE.equals(sourceTrack.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build()

        try {
            // Encode the specified track with the profiles
            val profiles = StringUtils.split(profileIds, ",")
            val job = composerService!!.multiEncode(sourceTrack as Track, Arrays.asList(*profiles))
            return Response.ok().entity(JaxbJob(job)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to encode the track: ", e)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    /**
     * Synchronously converts an image to another format.
     *
     * @param sourceImageXml
     * The source image
     * @param profileIds
     * The encoding profiles to use in image conversion
     * @return A [Response] with the resulting image in the response body
     * @throws Exception
     */
    @POST
    @Path("convertimagesync")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "convertimagesync", description = "Synchronously converts an image, based on the specified encoding profiles and the source image", restParameters = [RestParameter(description = "The original image", isRequired = true, name = "sourceImage", type = Type.TEXT, defaultValue = IMAGE_ATTACHMENT_DEFAULT), RestParameter(description = "The encoding profiles to use", isRequired = true, name = "profileIds", type = Type.STRING, defaultValue = "image-conversion.http")], reponses = [RestResponse(description = "Results in an xml document containing the image attachments", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If required parameters aren't set or if sourceImage isn't from the type attachment", responseCode = HttpServletResponse.SC_BAD_REQUEST)], returnDescription = "")
    @Throws(Exception::class)
    fun convertImageSync(@FormParam("sourceImage") sourceImageXml: String, @FormParam("profileIds")
    profileIds: String): Response {
        // Ensure that the POST parameters are present
        if (StringUtils.isBlank(sourceImageXml) || StringUtils.isBlank(profileIds))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceImage and profileIds must not be null").build()

        // Deserialize the source track
        val sourceImage = MediaPackageElementParser.getFromXml(sourceImageXml)
        if (!Attachment.TYPE.equals(sourceImage.elementType))
            return Response.status(Response.Status.BAD_REQUEST).entity("sourceImage element must be of type track").build()

        try {
            val results = composerService!!.convertImageSync(sourceImage as Attachment,
                    *StringUtils.split(profileIds, ','))
            return Response.ok().entity(MediaPackageElementParser.getArrayAsXml(results)).build()
        } catch (e: EncoderException) {
            logger.warn("Unable to convert image: " + e.message)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
        }

    }

    @GET
    @Path("profiles.xml")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "profiles", description = "Retrieve the encoding profiles", reponses = [RestResponse(description = "Results in an xml document describing the available encoding profiles", responseCode = HttpServletResponse.SC_OK)], returnDescription = "")
    fun listProfiles(): EncodingProfileList {
        val list = ArrayList<EncodingProfileImpl>()
        for (p in composerService!!.listProfiles()) {
            list.add(p as EncodingProfileImpl)
        }
        return EncodingProfileList(list)
    }

    @GET
    @Path("profile/{id}.xml")
    @Produces(MediaType.TEXT_XML)
    @RestQuery(name = "profilesID", description = "Retrieve an encoding profile", pathParameters = [RestParameter(name = "id", description = "the profile ID", isRequired = false, type = RestParameter.Type.STRING)], reponses = [RestResponse(description = "Results in an xml document describing the requested encoding profile", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "If profile has not been found", responseCode = HttpServletResponse.SC_NOT_FOUND)], returnDescription = "")
    @Throws(NotFoundException::class)
    fun getProfile(@PathParam("id") profileId: String): Response {
        val profile = composerService!!.getProfile(profileId) as EncodingProfileImpl ?: throw NotFoundException()
        return Response.ok(profile).build()
    }

    /**
     * Parses string containing times in seconds separated by comma.
     *
     * @param times
     * string to be parsed
     * @return array of times in seconds
     */
    protected fun parseTimeArray(times: String): DoubleArray {
        val timeStringArray = times.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val parsedTimeArray = LinkedList<Double>()
        for (timeString in timeStringArray) {
            val trimmed = StringUtils.trim(timeString)
            if (StringUtils.isNotBlank(trimmed)) {
                parsedTimeArray.add(java.lang.Double.parseDouble(timeString))
            }
        }
        val timeArray = DoubleArray(parsedTimeArray.size)
        for (i in parsedTimeArray.indices) {
            timeArray[i] = parsedTimeArray[i]
        }
        return timeArray
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(ComposerRestService::class.java)

        private val VIDEO_TRACK_DEFAULT = ("<track id=\"track-1\" type=\"presentation/source\">\n"
                + "  <mimetype>video/quicktime</mimetype>\n"
                + "  <url>http://localhost:8080/workflow/samples/camera.mpg</url>\n"
                + "  <checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum>\n"
                + "  <duration>14546</duration>\n" + "  <video>\n"
                + "    <device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />\n"
                + "    <encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" />\n"
                + "    <resolution>640x480</resolution>\n"
                + "    <scanType type=\"progressive\" />\n"
                + "    <bitrate>540520</bitrate>\n"
                + "    <frameRate>2</frameRate>\n"
                + "  </video>\n"
                + "</track>")

        private val AUDIO_TRACK_DEFAULT = ("<track id=\"track-2\" type=\"presentation/source\">\n"
                + "  <mimetype>audio/mp3</mimetype>\n"
                + "  <url>serverUrl/workflow/samples/audio.mp3</url>\n"
                + "  <checksum type=\"md5\">950f9fa49caa8f1c5bbc36892f6fd062</checksum>\n"
                + "  <duration>10472</duration>\n"
                + "  <audio>\n"
                + "    <channels>2</channels>\n"
                + "    <bitdepth>0</bitdepth>\n"
                + "    <bitrate>128004.0</bitrate>\n"
                + "    <samplingrate>44100</samplingrate>\n"
                + "  </audio>\n"
                + "</track>")

        private val MEDIA_TRACK_DEFAULT = ("<track id=\"track-3\">\n"
                + "  <mimetype>video/quicktime</mimetype>\n"
                + "  <url>serverUrl/workflow/samples/slidechanges.mov</url>\n"
                + "  <checksum type=\"md5\">4cbcc9223c0425a54c3f253823487d5f</checksum>\n"
                + "  <duration>27626</duration>\n"
                + "  <video>\n"
                + "    <resolution>1024x768</resolution>"
                + "  </video>\n"
                + "</track>")

        private val CAPTIONS_CATALOGS_DEFAULT = ("<captions>\n"
                + "  <catalog id=\"catalog-1\">\n"
                + "    <mimetype>application/x-subrip</mimetype>\n"
                + "    <url>serverUrl/workflow/samples/captions_test_eng.srt</url>\n"
                + "    <checksum type=\"md5\">55d70b062896aa685e2efc4226b32980</checksum>\n" + "    <tags>\n"
                + "      <tag>lang:en</tag>\n"
                + "    </tags>\n"
                + "  </catalog>\n"
                + "  <catalog id=\"catalog-2\">\n"
                + "    <mimetype>application/x-subrip</mimetype>\n"
                + "    <url>serverUrl/workflow/samples/captions_test_fra.srt</url>\n"
                + "    <checksum type=\"md5\">8f6cd99bbb6d591107f3b5c47ee51f2c</checksum>\n" + "    <tags>\n"
                + "      <tag>lang:fr</tag>\n"
                + "    </tags>\n"
                + "  </catalog>\n"
                + "</captions>\n")

        private val IMAGE_ATTACHMENT_DEFAULT = ("<attachment id=\"track-3\">\n"
                + "  <mimetype>image/jpeg</mimetype>\n"
                + "  <url>serverUrl/workflow/samples/image.jpg</url>\n"
                + "</attachment>")
    }

}
