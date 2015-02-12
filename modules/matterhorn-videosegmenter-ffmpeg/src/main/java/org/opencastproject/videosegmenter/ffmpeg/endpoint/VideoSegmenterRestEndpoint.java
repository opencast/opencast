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
package org.opencastproject.videosegmenter.ffmpeg.endpoint;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.videosegmenter.api.VideoSegmenterException;
import org.opencastproject.videosegmenter.api.VideoSegmenterService;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the {@link VideoSegmenterService} service
 */
@Path("")
@RestService(name = "videosegmentation", title = "Video Segmentation Service", abstractText = "This service performs segmentation of media files.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class VideoSegmenterRestEndpoint extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(VideoSegmenterRestEndpoint.class);

  /** The rest docs */
  protected String docs;

  /** The video segmenter */
  protected VideoSegmenterService service;

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
   * Sets the segmenter
   *
   * @param videoSegmenter
   *          the segmenter
   */
  protected void setVideoSegmenter(VideoSegmenterService videoSegmenter) {
    this.service = videoSegmenter;
  }

  /**
   * Segments a track.
   *
   * @param trackAsXml
   *          the track xml to segment
   * @return the job in the body of a JAX-RS response
   * @throws Exception
   */
  @POST
  @Path("")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "segment", description = "Submit a track for segmentation.", restParameters = { @RestParameter(description = "The track to segment.", isRequired = true, name = "track", type = RestParameter.Type.FILE) }, reponses = {
          @RestResponse(description = "The job ID to use when polling for the resulting mpeg7 catalog.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The \"segment\" is NULL or not a valid track type.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "The underlying service could not segment the video.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "The job ID to use when polling for the resulting mpeg7 catalog.")
  public Response segment(@FormParam("track") String trackAsXml) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(trackAsXml))
      return Response.status(Response.Status.BAD_REQUEST).entity("track must not be null").build();

    // Deserialize the track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(trackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("mediapackage element must be of type track").build();

    try {
      // Asynchronously segment the specified track
      Job job = service.segment((Track) sourceTrack);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (VideoSegmenterException e) {
      logger.warn("Segmentation failed: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    if (service instanceof JobProducer)
      return (JobProducer) service;
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

}
