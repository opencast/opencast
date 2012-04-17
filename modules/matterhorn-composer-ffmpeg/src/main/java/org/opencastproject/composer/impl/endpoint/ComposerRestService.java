/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.composer.impl.endpoint;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EmbedderException;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.api.EncodingProfileImpl;
import org.opencastproject.composer.api.EncodingProfileList;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A REST endpoint delegating functionality to the {@link ComposerService}
 */
@Path("/")
@RestService(name = "composer", title = "Composer", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is not working and "
                + "is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! "
                + "You should file an error report with your server logs from the time when the error occurred: "
                + "<a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>" }, abstractText = "This service creates and augments Matterhorn media packages that include media tracks, metadata catalogs and "
        + "attachments.")
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
   * @param sourceTrack
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
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "flash.http") }, reponses = { @RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response encode(@FormParam("sourceTrack") String sourceTrackAsXml, @FormParam("profileId") String profileId)
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
    Job job = composerService.encode((Track) sourceTrack, profileId);
    if (job == null)
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Encoding failed").build();
    return Response.ok().entity(new JaxbJob(job)).build();
  }

  /**
   * Trims a track to a new length.
   * 
   * @param sourceTrack
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
          @RestResponse(description = "if the duration is negative or, including the new start time, exceeds the track duration", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response trim(@FormParam("sourceTrack") String sourceTrackAsXml, @FormParam("profileId") String profileId,
          @FormParam("start") long start, @FormParam("duration") long duration) throws Exception {
    // Ensure that the POST parameters are present
    if (sourceTrackAsXml == null || profileId == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build();
    }

    // Deserialize the track
    MediaPackageElement sourceElement = MediaPackageElementParser.getFromXml(sourceTrackAsXml);
    if (!Track.TYPE.equals(sourceElement.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();
    }

    // Make sure the trim times make sense
    Track sourceTrack = (Track) sourceElement;
    if (start < 0) {
      start = 0;
    } else if (duration <= 0) {
      duration = (sourceTrack.getDuration() - start);
    } else if (start + duration > sourceTrack.getDuration()) {
      duration = (sourceTrack.getDuration() - start);
    }

    // Asynchronously encode the specified tracks
    Job job = composerService.trim(sourceTrack, profileId, start, duration);
    if (job == null)
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Trimming failed").build();
    return Response.ok().entity(new JaxbJob(job)).build();
  }

  /**
   * Encodes a track.
   * 
   * @param audioSourceTrack
   *          The audio source track
   * @param videoSourceTrack
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
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "flash.http") }, reponses = { @RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response mux(@FormParam("audioSourceTrack") String audioSourceTrackXml,
          @FormParam("videoSourceTrack") String videoSourceTrackXml, @FormParam("profileId") String profileId)
          throws Exception {
    // Ensure that the POST parameters are present
    if (audioSourceTrackXml == null || videoSourceTrackXml == null || profileId == null) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity("audioSourceTrack, videoSourceTrack, and profileId must not be null").build();
    }

    // Deserialize the audio track
    MediaPackageElement audioSourceTrack = MediaPackageElementParser.getFromXml(audioSourceTrackXml);
    if (!Track.TYPE.equals(audioSourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("audioSourceTrack element must be of type track")
              .build();
    }

    // Deserialize the video track
    MediaPackageElement videoSourceTrack = MediaPackageElementParser.getFromXml(videoSourceTrackXml);
    if (!Track.TYPE.equals(videoSourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("videoSourceTrack element must be of type track")
              .build();
    }

    // Asynchronously encode the specified tracks
    Job job = composerService.mux((Track) videoSourceTrack, (Track) audioSourceTrack, profileId);
    return Response.ok().entity(new JaxbJob(job)).build();
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
          @RestParameter(description = "The number of seconds (many numbers can be specified, separated by comma) into the video to extract the image", isRequired = true, name = "time", type = Type.STRING, defaultValue = "1"),
          @RestParameter(description = "The track containing the video stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = "${this.videoTrackDefault}"),
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "player-preview.http") }, reponses = { @RestResponse(description = "Results in an xml document containing the image attachment", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response image(@FormParam("sourceTrack") String sourceTrackXml, @FormParam("profileId") String profileId,
          @FormParam("time") String times) throws Exception {
    // Ensure that the POST parameters are present
    if (sourceTrackXml == null || profileId == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack and profileId must not be null").build();
    }

    // parse time codes
    long[] timeArray;
    try {
      timeArray = parseTimeArray(times);
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST).entity("could not parse times: invalid format").build();
    }

    // Deserialize the source track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();
    }

    try {
      Job job = composerService.image((Track) sourceTrack, profileId, timeArray);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to extract image(s): " + e.getMessage());
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
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "image-conversion.http") }, reponses = { @RestResponse(description = "Results in an xml document containing the image attachment", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response convertImage(@FormParam("sourceImage") String sourceImageXml, @FormParam("profileId") String profileId)
          throws Exception {
    // Ensure that the POST parameters are present
    if (sourceImageXml == null || profileId == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceImage and profileId must not be null").build();
    }

    // Deserialize the source track
    MediaPackageElement sourceImage = MediaPackageElementParser.getFromXml(sourceImageXml);
    if (!Attachment.TYPE.equals(sourceImage.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceImage element must be of type track").build();
    }

    try {
      Job job = composerService.convertImage((Attachment) sourceImage, profileId);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EncoderException e) {
      logger.warn("Unable to convert image: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Embeds captions in media file.
   * 
   * @param sourceTrackXml
   *          media file to which captions will be embedded
   * @param captionsXml
   *          captions that will be embedded
   * @param language
   *          language of captions
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("captions")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "captions", description = "Starts caption embedding process, based on the specified source track and captions", restParameters = {
          @RestParameter(description = "QuickTime file containg video stream", isRequired = true, name = "mediaTrack", type = Type.TEXT, defaultValue = "${this.mediaTrackDefault}"),
          @RestParameter(description = "Catalog(s) containing captions in SRT format", isRequired = true, name = "captions", type = Type.TEXT, defaultValue = "${this.captionsCatalogsDefault}") }, reponses = { @RestResponse(description = "Result in an xml document containing resulting media file.", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response captions(@FormParam("mediaTrack") String sourceTrackXml, @FormParam("captions") String captionsAsXml,
          @FormParam("language") String language) throws Exception {
    if (sourceTrackXml == null || captionsAsXml == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Source track and captions must not be null").build();
    }

    MediaPackageElement mediaTrack = MediaPackageElementParser.getFromXml(sourceTrackXml);
    if (!Track.TYPE.equals(mediaTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Source track element must be of type track").build();
    }

    MediaPackageElement[] mpElements = toMediaPackageElementArray(captionsAsXml);
    if (mpElements.length == 0) {
      return Response.status(Response.Status.BAD_REQUEST).entity("At least one caption must be present").build();
    }
    // cast to catalogs
    Catalog[] captions = new Catalog[mpElements.length];
    for (int i = 0; i < mpElements.length; i++) {
      if (!Catalog.TYPE.equals(mpElements[i].getElementType())) {
        return Response.status(Response.Status.BAD_REQUEST).entity("All captions must be of type catalog").build();
      }
      captions[i] = (Catalog) mpElements[i];
    }

    try {
      Job job = composerService.captions((Track) mediaTrack, captions);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (EmbedderException e) {
      logger.warn("Unable to embed captions: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * watermarks a track.
   * 
   * @param sourceTrack
   *          The source track
   * @param watermark
   *          Filename of the watermark image (jpg, gif, png)
   * @param profileId
   *          The profile to use in encoding this track
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("watermark")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "watermark", description = "re-encodes a source track with a watermark branding, the position of the watermark can be specified in the profileId, the watermark can be provided as a parameter", restParameters = {
          @RestParameter(description = "The track containing the stream", isRequired = true, name = "sourceTrack", type = Type.TEXT, defaultValue = "${this.videoTrackDefault}"),
          @RestParameter(description = "The watermark image path", isRequired = true, name = "watermark", type = Type.STRING, defaultValue = "$FELIX_HOME/conf/branding/watermark.png"),
          @RestParameter(description = "The encoding profile to use", isRequired = true, name = "profileId", type = Type.STRING, defaultValue = "watermark.branding") }, reponses = { @RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response watermark(@FormParam("sourceTrack") String sourceTrackAsXml,
          @FormParam("watermark") String watermark, @FormParam("profileId") String profileId) throws Exception {
    // Ensure that the POST parameters are present
    if (sourceTrackAsXml == null || profileId == null || watermark == null) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity("sourceTrack, watermark and profileId must not be null").build();
    }

    // Deserialize the track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceTrackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceTrack element must be of type track").build();
    }

    // Asynchronously encode the specified tracks
    Job job = composerService.watermark((Track) sourceTrack, watermark, profileId);
    if (job == null)
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Encoding failed").build();
    return Response.ok().entity(new JaxbJob(job)).build();
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
  @RestQuery(name = "profilesID", description = "Retrieve an encoding profile", pathParameters = { @RestParameter(name = "id", description = "the profile ID", isRequired = false, type = RestParameter.Type.STRING) }, reponses = { @RestResponse(description = "Results in an xml document describing the requested encoding profile", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
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
   * Converts string representation of the one or more catalogs to object array
   * 
   * @param elementsAsXml
   *          the serialized elements array representation
   * @return
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  protected MediaPackageElement[] toMediaPackageElementArray(String elementsAsXml) throws ParserConfigurationException,
          SAXException, IOException {

    List<MediaPackageElement> mpElements = new LinkedList<MediaPackageElement>();

    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.parse(IOUtils.toInputStream(elementsAsXml, "UTF-8"));
    // TODO -> explicit check for root node name?
    NodeList nodeList = doc.getDocumentElement().getChildNodes();

    MediaPackageSerializer serializer = new DefaultMediaPackageSerializerImpl();
    MediaPackageElementBuilder builder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();

    for (int i = 0; i < nodeList.getLength(); i++) {
      if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
        mpElements.add(builder.elementFromManifest(nodeList.item(i), serializer));
      }
    }

    return mpElements.toArray(new MediaPackageElement[mpElements.size()]);
  }

  /**
   * Parses string containing times in seconds separated by comma.
   * 
   * @param times
   *          string to be parsed
   * @return array of times in seconds
   */
  protected long[] parseTimeArray(String times) {
    String[] timeStringArray = times.split(",");
    List<Long> parsedTimeArray = new LinkedList<Long>();
    for (String timeString : timeStringArray) {
      String trimmed = StringUtils.trim(timeString);
      if (StringUtils.isNotBlank(trimmed)) {
        parsedTimeArray.add(Long.parseLong(timeString));
      }
    }
    long[] timeArray = new long[parsedTimeArray.size()];
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
