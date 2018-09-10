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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.composer.impl.endpoint;

import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfileImpl;
import org.opencastproject.composer.api.EncodingProfileList;
import org.opencastproject.composer.api.LaidOutElement;
import org.opencastproject.composer.layout.Dimension;
import org.opencastproject.composer.layout.Layout;
import org.opencastproject.composer.layout.Serializer;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.util.JsonObj;
import org.opencastproject.util.LocalHashMap;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A REST endpoint delegating functionality to the {@link ComposerService}
 */
@Path("/")
@RestService(name = "composer", title = "Composer", abstractText = "This service creates and augments Opencast media packages that include media tracks, metadata "
        + "catalogs and attachments.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class ComposerRestService extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ComposerRestService.class);

  /** The rest documentation */
  protected String docs;

  /** The base server URL */
  protected String serverUrl;

  /** The composer service */
  protected ComposerService composerService = null;

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** The smil service */
  protected SmilService smilService = null;

  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }

  /**
   * Callback from the OSGi declarative services to set the service registry.
   *
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Sets the composer service.
   *
   * @param composerService
   *          the composer service
   */
  public void setComposerService(ComposerService composerService) {
    this.composerService = composerService;
  }

  /**
   * Callback from OSGi that is called when this service is activated.
   *
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    if (cc == null || cc.getBundleContext().getProperty("org.opencastproject.server.url") == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      serverUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    }
  }

  /**
   * Encodes a track.
   *
   * @param sourceTrackAsXml
   *          The source track
   * @param profileId
   *          The profile to use in encoding this track
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("encode")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "encode", description = "Starts an encoding process, based on the specified encoding profile ID and the track", restParameters = {
          @RestParameter(description = "The track containing the stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = "${this.videoTrackDefault}"),
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "mp4-medium.http") }, reponses = {
          @RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response encode(@FormParam("sourceTrack") String sourceTrackAsXml, @FormParam("profileId") String profileId)
          throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceTrackAsXml) || StringUtils.isBlank(profileId))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build();

    // Deserialize the track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();

    try {
      // Asynchronously encode the specified tracks
      Job job = composerService.encode((Track) sourceTrack, profileId);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to encode the track: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Encodes a track to multiple tracks in parallel.
   *
   * @param sourceTrackAsXml
   *          The source track
   * @param profileId
   *          The profile to use in encoding this track
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("parallelencode")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "parallelencode", description = "Starts an encoding process, based on the specified encoding profile ID and the track", pathParameters = { }, restParameters = {
          @RestParameter(description = "The track containing the stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = "${this.videoTrackDefault}"),
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "mp4-medium.http") }, reponses = { @RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response parallelencode(@FormParam("sourceTrack") String sourceTrackAsXml, @FormParam("profileId") String profileId)
          throws Exception {
    // Ensure that the POST parameters are present
    if (sourceTrackAsXml == null || profileId == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build();
    }

    // Deserialize the track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();
    }

    // Asynchronously encode the specified tracks
    Job job = composerService.parallelEncode((Track) sourceTrack, profileId);
    if (job == null)
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Encoding failed").build();
    return Response.ok().entity(new JaxbJob(job)).build();
  }

  /**
   * Trims a track to a new length.
   *
   * @param sourceTrackAsXml
   *          The source track
   * @param profileId
   *          the encoding profile to use for trimming
   * @param start
   *          the new trimming start time
   * @param duration
   *          the new video duration
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("trim")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "trim", description = "Starts a trimming process, based on the specified track, start time and duration in ms", restParameters = {
          @RestParameter(description = "The track containing the stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = "${this.videoTrackDefault}"),
          @RestParameter(description = "The encoding profile to use for trimming", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "trim.work"),
          @RestParameter(description = "The start time in milisecond", isRequired = true, name = "start", type = Type.STRING, defaultValue = "0"),
          @RestParameter(description = "The duration in milisecond", isRequired = true, name = "duration", type = Type.STRING, defaultValue = "10000") }, reponses = {
          @RestResponse(description = "Results in an xml document containing the job for the trimming task", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "If the start time is negative or exceeds the track duration", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "If the duration is negative or, including the new start time, exceeds the track duration", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response trim(@FormParam("sourceTrack") String sourceTrackAsXml, @FormParam("profileId") String profileId,
          @FormParam("start") long start, @FormParam("duration") long duration) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceTrackAsXml) || StringUtils.isBlank(profileId))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build();

    // Deserialize the track
    MediaPackageElement sourceElement = MediaPackageElementParser.getFromXml(sourceTrackAsXml);
    if (!Track.TYPE.equals(sourceElement.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();

    // Make sure the trim times make sense
    Track sourceTrack = (Track) sourceElement;

    if (sourceTrack.getDuration() == null)
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element does not have a duration")
              .build();

    if (start < 0) {
      start = 0;
    } else if (duration <= 0) {
      duration = (sourceTrack.getDuration() - start);
    } else if (start + duration > sourceTrack.getDuration()) {
      duration = (sourceTrack.getDuration() - start);
    }

    try {
      // Asynchronously encode the specified tracks
      Job job = composerService.trim(sourceTrack, profileId, start, duration);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to trim the track: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Encodes a track.
   *
   * @param audioSourceTrackXml
   *          The audio source track
   * @param videoSourceTrackXml
   *          The video source track
   * @param profileId
   *          The profile to use in encoding this track
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("mux")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "mux", description = "Starts an encoding process, which will mux the two tracks using the given encoding profile", restParameters = {
          @RestParameter(description = "The track containing the audio stream", isRequired = true, name = "sourceAudioTrack", type = Type.TEXT, defaultValue = "${this.audioTrackDefault}"),
          @RestParameter(description = "The track containing the video stream", isRequired = true, name = "sourceVideoTrack", type = Type.TEXT, defaultValue = "${this.videoTrackDefault}"),
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "mp4-medium.http") }, reponses = {
          @RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "If required parameters aren't set or if the source tracks aren't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response mux(@FormParam("audioSourceTrack") String audioSourceTrackXml,
          @FormParam("videoSourceTrack") String videoSourceTrackXml, @FormParam("profileId") String profileId)
          throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(audioSourceTrackXml) || StringUtils.isBlank(videoSourceTrackXml)
            || StringUtils.isBlank(profileId)) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity("audioSourceTrack, videoSourceTrack, and profileId must not be null").build();
    }

    // Deserialize the audio track
    MediaPackageElement audioSourceTrack = MediaPackageElementParser.getFromXml(audioSourceTrackXml);
    if (!Track.TYPE.equals(audioSourceTrack.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("audioSourceTrack must be of type track").build();

    // Deserialize the video track
    MediaPackageElement videoSourceTrack = MediaPackageElementParser.getFromXml(videoSourceTrackXml);
    if (!Track.TYPE.equals(videoSourceTrack.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("videoSourceTrack must be of type track").build();

    try {
      // Asynchronously encode the specified tracks
      Job job = composerService.mux((Track) videoSourceTrack, (Track) audioSourceTrack, profileId);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to mux tracks: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Encodes a track in a media package.
   *
   * @param sourceTrackXml
   *          The source track
   * @param profileId
   *          The profile to use in encoding this track
   * @param times
   *          one or more times in seconds separated by comma
   * @return A {@link Response} with the resulting track in the response body
   * @throws Exception
   */
  @POST
  @Path("image")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "image", description = "Starts an image extraction process, based on the specified encoding profile ID and the source track", restParameters = {
          @RestParameter(description = "The track containing the video stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = "${this.videoTrackDefault}"),
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "player-preview.http"),
          @RestParameter(description = "The number of seconds (many numbers can be specified, separated by semicolon) into the video to extract the image", isRequired = false, name = "time", type = Type.STRING),
          @RestParameter(description = "An optional set of key=value\\n properties", isRequired = false, name = "properties", type = TEXT) }, reponses = {
          @RestResponse(description = "Results in an xml document containing the image attachment", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "The image extraction job")
  public Response image(@FormParam("sourceTrack") String sourceTrackXml, @FormParam("profileId") String profileId,
          @FormParam("time") String times, @FormParam("properties") LocalHashMap localMap) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceTrackXml) || StringUtils.isBlank(profileId))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build();

    // Deserialize the source track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();

    boolean timeBased = false;
    double[] timeArray = null;
    if (StringUtils.isNotBlank(times)) {
      // parse time codes
      try {
        timeArray = parseTimeArray(times);
      } catch (Exception e) {
        return Response.status(Response.Status.BAD_REQUEST).entity("could not parse times: invalid format").build();
      }
      timeBased = true;
    } else if (localMap == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    try {
      // Asynchronously encode the specified tracks
      Job job;
      if (timeBased) {
        job = composerService.image((Track) sourceTrack, profileId, timeArray);
      } else {
        job = composerService.image((Track) sourceTrack, profileId, localMap.getMap());
      }
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to extract image(s): " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Encodes a track in a media package.
   *
   * @param sourceTrackXml
   *          The source track
   * @param profileId
   *          The profile to use in encoding this track
   * @param times
   *          one or more times in seconds separated by comma
   * @return A {@link Response} with the resulting track in the response body
   * @throws Exception
   */
  @POST
  @Path("imagesync")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "imagesync", description = "Synchronously extracts an image, based on the specified encoding profile ID and the source track", restParameters = {
      @RestParameter(description = "The track containing the video stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = "${this.videoTrackDefault}"),
      @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "player-preview.http"),
      @RestParameter(description = "The number of seconds (many numbers can be specified, separated by semicolon) into the video to extract the image", isRequired = false, name = "time", type = Type.STRING)}, reponses = {
      @RestResponse(description = "Results in an xml document containing the image attachment", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "The extracted image")
  public Response imageSync(@FormParam("sourceTrack") String sourceTrackXml, @FormParam("profileId") String profileId,
                        @FormParam("time") String times) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceTrackXml) || StringUtils.isBlank(profileId) || StringUtils.isBlank(times)) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack, times, and profileId must not be null").build();
    }

    // Deserialize the source track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();
    }

    double[] timeArray = null;
    // parse time codes
    try {
      timeArray = parseTimeArray(times);
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity("could not parse times: invalid format").build();
    }

    try {
      List<Attachment> result = composerService.imageSync((Track) sourceTrack, profileId, timeArray);
      return Response.ok().entity(MediaPackageElementParser.getArrayAsXml(result)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to extract image(s): " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Compose two videos into one with an optional watermark.
   *
   * @param compositeSizeJson
   *          The composite track dimension as JSON
   * @param lowerTrackXml
   *          The lower track of the composition as XML
   * @param lowerLayoutJson
   *          The lower layout as JSON
   * @param upperTrackXml
   *          The upper track of the composition as XML
   * @param upperLayoutJson
   *          The upper layout as JSON
   * @param watermarkAttachmentXml
   *          The watermark image attachment of the composition as XML
   * @param watermarkLayoutJson
   *          The watermark layout as JSON
   * @param profileId
   *          The encoding profile to use
   * @param background
   *          The background color
   * @return A {@link Response} with the resulting track in the response body
   * @throws Exception
   */
  @POST
  @Path("composite")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "composite", description = "Starts a video compositing process, based on the specified resolution, encoding profile ID, the source elements and their layouts", restParameters = {
          @RestParameter(description = "The resolution size of the resulting video as JSON", isRequired = true, name = "compositeSize", type = Type.STRING),
          @RestParameter(description = "The lower source track containing the lower video", isRequired = true, name = "lowerTrack", type = Type.TEXT),
          @RestParameter(description = "The lower layout containing the JSON definition of the layout", isRequired = true, name = "lowerLayout", type = Type.TEXT),
          @RestParameter(description = "The upper source track containing the upper video", isRequired = false, name = "upperTrack", type = Type.TEXT),
          @RestParameter(description = "The upper layout containing the JSON definition of the layout", isRequired = false, name = "upperLayout", type = Type.TEXT),
          @RestParameter(description = "The watermark source attachment containing watermark image", isRequired = false, name = "watermarkTrack", type = Type.TEXT),
          @RestParameter(description = "The watermark layout containing the JSON definition of the layout", isRequired = false, name = "watermarkLayout", type = Type.TEXT),
          @RestParameter(description = "The background color", isRequired = false, name = "background", type = Type.TEXT, defaultValue = "black"),
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING) }, reponses = {
          @RestResponse(description = "Results in an xml document containing the compound video track", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "If required parameters aren't set or if the source elements aren't from the right type", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response composite(@FormParam("compositeSize") String compositeSizeJson,
          @FormParam("lowerTrack") String lowerTrackXml, @FormParam("lowerLayout") String lowerLayoutJson,
          @FormParam("upperTrack") String upperTrackXml, @FormParam("upperLayout") String upperLayoutJson,
          @FormParam("watermarkAttachment") String watermarkAttachmentXml,
          @FormParam("watermarkLayout") String watermarkLayoutJson, @FormParam("profileId") String profileId,
          @FormParam("background") @DefaultValue("black") String background) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(compositeSizeJson) || StringUtils.isBlank(lowerTrackXml)
            || StringUtils.isBlank(lowerLayoutJson) || StringUtils.isBlank(profileId))
      return Response.status(Response.Status.BAD_REQUEST).entity("One of the required parameters must not be null")
              .build();

    // Deserialize the source elements
    MediaPackageElement lowerTrack = MediaPackageElementParser.getFromXml(lowerTrackXml);
    Layout lowerLayout = Serializer.layout(JsonObj.jsonObj(lowerLayoutJson));
    if (!Track.TYPE.equals(lowerTrack.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("lowerTrack element must be of type track").build();
    LaidOutElement<Track> lowerLaidOutElement = new LaidOutElement<Track>((Track) lowerTrack, lowerLayout);

    Option<LaidOutElement<Track>> upperLaidOutElement = Option.<LaidOutElement<Track>> none();
    if (StringUtils.isNotBlank(upperTrackXml)) {
      MediaPackageElement upperTrack = MediaPackageElementParser.getFromXml(upperTrackXml);
      Layout upperLayout = Serializer.layout(JsonObj.jsonObj(upperLayoutJson));
      if (!Track.TYPE.equals(upperTrack.getElementType())) {
        return Response.status(Response.Status.BAD_REQUEST).entity("upperTrack element must be of type track").build();
      }
      upperLaidOutElement = Option.option(new LaidOutElement<Track>((Track) upperTrack, upperLayout));
    }
    Option<LaidOutElement<Attachment>> watermarkLaidOutElement = Option.<LaidOutElement<Attachment>> none();
    if (StringUtils.isNotBlank(watermarkAttachmentXml)) {
      Layout watermarkLayout = Serializer.layout(JsonObj.jsonObj(watermarkLayoutJson));
      MediaPackageElement watermarkAttachment = MediaPackageElementParser.getFromXml(watermarkAttachmentXml);
      if (!Attachment.TYPE.equals(watermarkAttachment.getElementType()))
        return Response.status(Response.Status.BAD_REQUEST).entity("watermarkTrack element must be of type track")
                .build();
      watermarkLaidOutElement = Option.some(new LaidOutElement<Attachment>((Attachment) watermarkAttachment,
              watermarkLayout));
    }

    Dimension compositeTrackSize = Serializer.dimension(JsonObj.jsonObj(compositeSizeJson));

    try {
      // Asynchronously composite the specified source elements
      Job job = composerService.composite(compositeTrackSize, upperLaidOutElement, lowerLaidOutElement,
              watermarkLaidOutElement, profileId, background);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to composite video: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Concat multiple tracks having the same codec to a single track.
   *
   * @param sourceTracksXml
   *          an array of track to concat in order of the array as XML
   * @param profileId
   *          The encoding profile to use
   * @param outputDimension
   *          The output dimension as JSON
   * @return A {@link Response} with the resulting track in the response body
   * @throws Exception
   */
  @POST
  @Path("concat")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "concat", description = "Starts a video concating process from multiple videos, based on the specified encoding profile ID and the source tracks", restParameters = {
          @RestParameter(description = "The source tracks to concat as XML", isRequired = true, name = "sourceTracks", type = Type.TEXT),
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING),
          @RestParameter(description = "The resolution dimension of the concat video as JSON", isRequired = false, name = "outputDimension", type = Type.STRING),
          @RestParameter(description = "The  frame rate of the concat video (should be positive, e.g. 25.0). Negative values and zero will cause no FFmpeg fps filter to be used in the filter chain.",
      isRequired = false, name = "outputFrameRate", type = Type.STRING),
          @RestParameter(description = "The source files have the same codecs and should not be re-encoded", isRequired = false, name = "sameCodec",type = Type.TEXT, defaultValue = "false")}, reponses = {
    @RestResponse(description = "Results in an xml document containing the video track", responseCode = HttpServletResponse.SC_OK),
    @RestResponse(description = "If required parameters aren't set or if sourceTracks aren't from the type Track or not at least two tracks are present",
            responseCode = HttpServletResponse.SC_BAD_REQUEST)}, returnDescription = "")
  public Response concat(@FormParam("sourceTracks") String sourceTracksXml, @FormParam("profileId") String profileId,
          @FormParam("outputDimension") String outputDimension, @FormParam("outputFrameRate") String outputFrameRate,
          @FormParam("sameCodec") String sameCodec) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceTracksXml) || StringUtils.isBlank(profileId))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTracks and profileId must not be null").build();

    // Deserialize the source track
    List<? extends MediaPackageElement> tracks = MediaPackageElementParser.getArrayFromXml(sourceTracksXml);
    if (tracks.size() < 2)
      return Response.status(Response.Status.BAD_REQUEST).entity("At least two tracks must be set to concat").build();

    for (MediaPackageElement elem : tracks) {
      if (!Track.TYPE.equals(elem.getElementType()))
        return Response.status(Response.Status.BAD_REQUEST).entity("sourceTracks must be of type track").build();
    }
    float fps = NumberUtils.toFloat(outputFrameRate, -1.0f);
    try {
      // Asynchronously concat the specified tracks together
      Dimension dimension = null;
      if (StringUtils.isNotBlank(outputDimension)) {
        dimension = Serializer.dimension(JsonObj.jsonObj(outputDimension));
      }
      boolean hasSameCodec = Boolean.parseBoolean(sameCodec);
      Job job = null;
      if (fps > 0) {
        job = composerService.concat(profileId, dimension, fps, hasSameCodec, tracks.toArray(new Track[tracks.size()]));
      } else {
        job = composerService.concat(profileId, dimension, hasSameCodec, tracks.toArray(new Track[tracks.size()]));
      }
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to concat videos: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Transforms an image attachment to a video track
   *
   * @param sourceAttachmentXml
   *          The source image attachment
   * @param profileId
   *          The profile to use for encoding
   * @param timeString
   *          the length of the resulting video track in seconds
   * @return A {@link Response} with the resulting track in the response body
   * @throws Exception
   */
  @POST
  @Path("imagetovideo")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "imagetovideo", description = "Starts an image converting process to a video, based on the specified encoding profile ID and the source image attachment", restParameters = {
          @RestParameter(description = "The resulting video time in seconds", isRequired = false, name = "time", type = Type.STRING, defaultValue = "1"),
          @RestParameter(description = "The attachment containing the image to convert", isRequired = true, name = "sourceAttachment", type = Type.TEXT),
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING) }, reponses = {
          @RestResponse(description = "Results in an xml document containing the video track", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "If required parameters aren't set or if sourceAttachment isn't from the type Attachment", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response imageToVideo(@FormParam("sourceAttachment") String sourceAttachmentXml,
          @FormParam("profileId") String profileId, @FormParam("time") @DefaultValue("1") String timeString)
          throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceAttachmentXml) || StringUtils.isBlank(profileId))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceAttachment and profileId must not be null")
              .build();

    // parse time
    Double time;
    try {
      time = Double.parseDouble(timeString);
    } catch (Exception e) {
      logger.info("Unable to parse time {} as long value!", timeString);
      return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse time: invalid format").build();
    }

    // Deserialize the source track
    MediaPackageElement sourceAttachment = MediaPackageElementParser.getFromXml(sourceAttachmentXml);
    if (!Attachment.TYPE.equals(sourceAttachment.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceAttachment element must be of type attachment")
              .build();

    try {
      // Asynchronously convert the specified attachment to a video
      Job job = composerService.imageToVideo((Attachment) sourceAttachment, profileId, time);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to convert image to video: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Converts an image to another format.
   *
   * @param sourceImageXml
   *          The source image
   * @param profileId
   *          The profile to use in image conversion
   * @return A {@link Response} with the resulting image in the response body
   * @throws Exception
   */
  @POST
  @Path("convertimage")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "convertimage", description = "Starts an image conversion process, based on the specified encoding profile ID and the source image", restParameters = {
          @RestParameter(description = "The original image", isRequired = true, name = "sourceImage", type = Type.TEXT, defaultValue = "${this.imageAttachmentDefault}"),
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "image-conversion.http") }, reponses = {
          @RestResponse(description = "Results in an xml document containing the image attachment", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "If required parameters aren't set or if sourceImage isn't from the type Attachment", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response convertImage(@FormParam("sourceImage") String sourceImageXml, @FormParam("profileId") String profileId)
          throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceImageXml) || StringUtils.isBlank(profileId))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceImage and profileId must not be null").build();

    // Deserialize the source track
    MediaPackageElement sourceImage = MediaPackageElementParser.getFromXml(sourceImageXml);
    if (!Attachment.TYPE.equals(sourceImage.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceImage element must be of type track").build();

    try {
      // Asynchronously convert the specified image
      Job job = composerService.convertImage((Attachment) sourceImage, profileId);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to convert image: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Demuxes a track into multiple outputs
   *
   * @param sourceTrackAsXml
   *          The source track
   * @param profileId
   *          The profile to use in encoding this track
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   *           - if it fails
   */
  @POST
  @Path("demux")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "demux", description = "Starts an demux process that produces multiple outputs, based on the specified encoding profile ID and the track", restParameters = {
          @RestParameter(description = "The track containing the stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = "${this.videoTrackDefault}"),
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "demux.work") }, reponses = {
                  @RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response demux(@FormParam("sourceTrack") String sourceTrackAsXml, @FormParam("profileId") String profileId)
          throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceTrackAsXml) || StringUtils.isBlank(profileId))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build();

    // Deserialize the track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();

    try {
      // Asynchronously encode the specified tracks
      Job job = composerService.demux((Track) sourceTrack, profileId);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to encode the track: " + e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * ProcessSmil - encode a video based on descriptions in a smil file into all format in the profileIds
   *
   * @param smilAsXml
   *          - smil describing a list of videos and clips in them to make up one video
   * @param trackId
   *          - a paramGroup Id in the smil file describing a track
   * @param mediaType
   *          - audio only, video only or both
   * @param profileIds
   *          - list of encoding profile ids
   * @return a job running the process
   * @throws Exception
   *           if it fails
   */
  @POST
  @Path("processsmil")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "processsmil", description = "Starts an encoding process, based on the tracks and edit points in the smil and specified encoding profile IDs", restParameters = {
          @RestParameter(description = "The smil containing the tracks and edit points", isRequired = true, name = "smilAsXml", type = Type.TEXT),
          @RestParameter(description = "The id (paramgroup) of the track to encode", isRequired = false, name = "trackId", type = Type.STRING, defaultValue = ""),
          @RestParameter(description = "MediaType - v for video only, a for audio only, audiovisual otherwise", isRequired = false, name = "mediaType", type = Type.STRING, defaultValue = "o"),
          @RestParameter(description = "The encoding profiles to use", isRequired = true, name = "profileIds", type = Type.STRING) }, reponses = {
                  @RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response processSmil(@FormParam("smilAsXml") String smilAsXml, @FormParam("trackId") String trackId,
          @FormParam("mediaType") String mediaType, @FormParam("profileIds") String profileIds) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(smilAsXml) || StringUtils.isBlank(profileIds))
      return Response.status(Response.Status.BAD_REQUEST).entity("smil and profileId must not be null").build();

    // Deserialize the data
    Smil smil;
    String[] profiles = StringUtils.split(profileIds, ",");
    try {
      smil = smilService.fromXml(smilAsXml).getSmil();
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity("smil must be readable").build();
    }

    try {
      // Encode the specified tracks
      Job job = composerService.processSmil(smil, trackId, mediaType, Arrays.asList(profiles));
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to process the smil: " + e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @POST
  @Path("multiencode")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "multiencode", description = "Starts an encoding process that produces multiple outputs, based on the specified encoding profile ID and the track", restParameters = {
          @RestParameter(description = "The track containing the stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = "${this.videoTrackDefault}"),
          @RestParameter(description = "The comma-delimited encoding profiles to use", isRequired = true, name = "profileIds", type = Type.STRING, defaultValue = "mp4-medium.http,mp4-low.http") }, reponses = {
                  @RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "If required parameters aren't set or if sourceTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response multiEncode(@FormParam("sourceTrack") String sourceTrackAsXml,
          @FormParam("profileIds") String profileIds) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceTrackAsXml) || StringUtils.isBlank(profileIds))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileIds must not be null").build();

    // Deserialize the track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();

    try {
      // Encode the specified track with the profiles
      String[] profiles = StringUtils.split(profileIds, ",");
      Job job = composerService.multiEncode((Track) sourceTrack, Arrays.asList(profiles));
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to encode the track: ", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Synchronously converts an image to another format.
   *
   * @param sourceImageXml
   *          The source image
   * @param profileId
   *          The profile to use in image conversion
   * @return A {@link Response} with the resulting image in the response body
   * @throws Exception
   */
  @POST
  @Path("convertimagesync")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "convertimagesync", description = "Synchronously converts an image, based on the specified encoding profile ID and the source image", restParameters = {
      @RestParameter(description = "The original image", isRequired = true, name = "sourceImage", type = Type.TEXT, defaultValue = "${this.imageAttachmentDefault}"),
      @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "image-conversion.http") }, reponses = {
      @RestResponse(description = "Results in an xml document containing the image attachment", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "If required parameters aren't set or if sourceImage isn't from the type Attachment", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response convertImageSync(@FormParam("sourceImage") String sourceImageXml, @FormParam("profileId") String profileId)
      throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceImageXml) || StringUtils.isBlank(profileId))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceImage and profileId must not be null").build();

    // Deserialize the source track
    MediaPackageElement sourceImage = MediaPackageElementParser.getFromXml(sourceImageXml);
    if (!Attachment.TYPE.equals(sourceImage.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceImage element must be of type track").build();

    try {
      Attachment result = composerService.convertImageSync((Attachment) sourceImage, profileId);
      return Response.ok().entity(MediaPackageElementParser.getAsXml(result)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to convert image: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Path("profiles.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "profiles", description = "Retrieve the encoding profiles", reponses = { @RestResponse(description = "Results in an xml document describing the available encoding profiles", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public EncodingProfileList listProfiles() {
    List<EncodingProfileImpl> list = new ArrayList<EncodingProfileImpl>();
    for (EncodingProfile p : composerService.listProfiles()) {
      list.add((EncodingProfileImpl) p);
    }
    return new EncodingProfileList(list);
  }

  @GET
  @Path("profile/{id}.xml")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "profilesID", description = "Retrieve an encoding profile", pathParameters = { @RestParameter(name = "id", description = "the profile ID", isRequired = false, type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "Results in an xml document describing the requested encoding profile", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "If profile has not been found", responseCode = HttpServletResponse.SC_NOT_FOUND) }, returnDescription = "")
  public Response getProfile(@PathParam("id") String profileId) throws NotFoundException {
    EncodingProfileImpl profile = (EncodingProfileImpl) composerService.getProfile(profileId);
    if (profile == null)
      throw new NotFoundException();
    return Response.ok(profile).build();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    if (composerService instanceof JobProducer)
      return (JobProducer) composerService;
    else
      return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getServiceRegistry()
   */
  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Parses string containing times in seconds separated by comma.
   *
   * @param times
   *          string to be parsed
   * @return array of times in seconds
   */
  protected double[] parseTimeArray(String times) {
    String[] timeStringArray = times.split(";");
    List<Double> parsedTimeArray = new LinkedList<Double>();
    for (String timeString : timeStringArray) {
      String trimmed = StringUtils.trim(timeString);
      if (StringUtils.isNotBlank(trimmed)) {
        parsedTimeArray.add(Double.parseDouble(timeString));
      }
    }
    double[] timeArray = new double[parsedTimeArray.size()];
    for (int i = 0; i < parsedTimeArray.size(); i++) {
      timeArray[i] = parsedTimeArray.get(i);
    }
    return timeArray;
  }

  protected String getVideoTrackDefault() {
    return "<track id=\"track-1\" type=\"presentation/source\">\n" + "  <mimetype>video/quicktime</mimetype>\n"
            + "  <url>" + serverUrl + "/workflow/samples/camera.mpg</url>\n"
            + "  <checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum>\n"
            + "  <duration>14546</duration>\n" + "  <video>\n"
            + "    <device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />\n"
            + "    <encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" />\n"
            + "    <resolution>640x480</resolution>\n" + "    <scanType type=\"progressive\" />\n"
            + "    <bitrate>540520</bitrate>\n" + "    <frameRate>2</frameRate>\n" + "  </video>\n" + "</track>";
  }

  protected String getAudioTrackDefault() {
    return "<track id=\"track-2\" type=\"presentation/source\">\n" + "  <mimetype>audio/mp3</mimetype>\n"
            + "  <url>serverUrl/workflow/samples/audio.mp3</url>\n"
            + "  <checksum type=\"md5\">950f9fa49caa8f1c5bbc36892f6fd062</checksum>\n"
            + "  <duration>10472</duration>\n" + "  <audio>\n" + "    <channels>2</channels>\n"
            + "    <bitdepth>0</bitdepth>\n" + "    <bitrate>128004.0</bitrate>\n"
            + "    <samplingrate>44100</samplingrate>\n" + "  </audio>\n" + "</track>";
  }

  protected String getMediaTrackDefault() {
    return "<track id=\"track-3\">\n" + "  <mimetype>video/quicktime</mimetype>\n"
            + "  <url>serverUrl/workflow/samples/slidechanges.mov</url>\n"
            + "  <checksum type=\"md5\">4cbcc9223c0425a54c3f253823487d5f</checksum>\n"
            + "  <duration>27626</duration>\n" + "  <video>\n" + "    <resolution>1024x768</resolution>"
            + "  </video>\n" + "</track>";
  }

  protected String getCaptionsCatalogsDefault() {
    return "<captions>\n" + "  <catalog id=\"catalog-1\">\n" + "    <mimetype>application/x-subrip</mimetype>\n"
            + "    <url>serverUrl/workflow/samples/captions_test_eng.srt</url>\n"
            + "    <checksum type=\"md5\">55d70b062896aa685e2efc4226b32980</checksum>\n" + "    <tags>\n"
            + "      <tag>lang:en</tag>\n" + "    </tags>\n" + "  </catalog>\n" + "  <catalog id=\"catalog-2\">\n"
            + "    <mimetype>application/x-subrip</mimetype>\n"
            + "    <url>serverUrl/workflow/samples/captions_test_fra.srt</url>\n"
            + "    <checksum type=\"md5\">8f6cd99bbb6d591107f3b5c47ee51f2c</checksum>\n" + "    <tags>\n"
            + "      <tag>lang:fr</tag>\n" + "    </tags>\n" + "  </catalog>\n" + "</captions>\n";
  }

  protected String getImageAttachmentDefault() {
    return "<attachment id=\"track-3\">\n" + "  <mimetype>image/jpeg</mimetype>\n"
            + "  <url>serverUrl/workflow/samples/image.jpg</url>\n" + "</attachment>";
  }

}
