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

package org.opencastproject.timelinepreviews.endpoint;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.timelinepreviews.api.TimelinePreviewsException;
import org.opencastproject.timelinepreviews.api.TimelinePreviewsService;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
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
 * The REST endpoint for the {@link TimelinePreviewsService} service
 */
@Path("/")
@RestService(name = "TimelinePreviewsEndpoint", title = "Timeline Previews Service REST Endpoint",
        abstractText = "This service generates timeline preview images from media files that contain a video.",
        notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time "
                + "when the error occurred: <a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
public class TimelinePreviewsRestEndpoint extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(TimelinePreviewsRestEndpoint.class);

  /** The rest docs */
  protected String docs;

  /** The timeline previews service */
  protected TimelinePreviewsService service;

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
   * Sets the timeline previews service
   *
   * @param timelinePreviewsService
   *          the timeline previews service
   */
  protected void setTimelinePreviewsService(TimelinePreviewsService timelinePreviewsService) {
    this.service = timelinePreviewsService;
  }

  /**
   * Generates timeline preview images for a track.
   *
   * @param trackAsXml
   *          the track xml to create preview images for
   * @return the job in the body of a JAX-RS response
   * @throws Exception
   */
  @POST
  @Path("/create")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "create", description = "Create preview images from the given track.",
          restParameters = {
            @RestParameter(description = "The track to generate timeline preview images for.",
                    isRequired = true, name = "track", type = RestParameter.Type.FILE),
            @RestParameter(description = "The number of timeline preview images to generate.",
                    isRequired = true, name = "imageCount", type = RestParameter.Type.INTEGER)
          },
          reponses = {
            @RestResponse(description = "Timeline previews job successfully created",
                    responseCode = HttpServletResponse.SC_OK),
            @RestResponse(description = "The given track can't be parsed.",
                    responseCode = HttpServletResponse.SC_BAD_REQUEST),
            @RestResponse(description = "Internal server error.",
                    responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
          },
          returnDescription = "The job ID to use when polling for the resulting media package attachment, "
                  + "that contains the generated timeline preview images.")
  public Response createTimelinePreviews(@FormParam("track") String trackAsXml, @FormParam("imageCount") int imageCount)
          throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(trackAsXml))
      return Response.status(Response.Status.BAD_REQUEST).entity("track must not be null").build();

    // Deserialize the track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(trackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("mediapackage element must be of type track").build();

    try {
      Job job = service.createTimelinePreviewImages((Track) sourceTrack, imageCount);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (TimelinePreviewsException e) {
      logger.warn("Generation of timeline preview images failed: " + e.getMessage());
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
