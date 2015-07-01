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

package org.opencastproject.publication.youtube.endpoint;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.publication.api.YouTubePublicationService;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.RestUtil.R.badRequest;
import static org.opencastproject.util.RestUtil.R.serverError;

/**
 * Rest endpoint for publishing media to youtube.
 */
@Path("/")
@RestService(name = "youtubepublicationservice",
    title = "YouTube Publication Service",
    abstractText = "",
    notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
            + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
            + "other words, there is a bug! You should file an error report with your server logs from the time when the "
            + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class YouTubePublicationRestService extends AbstractJobProducerEndpoint {

  private final Logger logger = LoggerFactory.getLogger(YouTubePublicationRestService.class);

  protected YouTubePublicationService service;

  protected ServiceRegistry serviceRegistry = null;

  @POST
  @Path("/")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "publish",
      description = "Publish a media package element to youtube publication channel",
      returnDescription = "The job that can be used to track the publication",
      restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT),
          @RestParameter(name = "elementId", isRequired = true, description = "The element to publish", type = Type.STRING) },
      reponses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the publication job"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "elementId does not reference a track") })
  public Response publish(@FormParam("mediapackage") final String mediaPackageXml, @FormParam("elementId") final String elementId) {
    final Job job;
    try {
      final MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      final Track track = mediapackage.getTrack(elementId);
      if (track != null) {
        job = service.publish(mediapackage, track);
      } else {
        return badRequest();
      }
    } catch (Exception e) {
      logger.warn("Error publishing element '{}' to YouTube", elementId, e);
      return serverError();
    }
    return Response.ok(new JaxbJob(job)).build();
  }

  @POST
  @Path("/retract")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "retract",
      description = "Retract a media package from the youtube publication channel",
      returnDescription = "The job that can be used to track the retraction",
      restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT) },
      reponses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the retraction job") })
  public Response retract(@FormParam("mediapackage") final String mediaPackageXml) {
    final Job job;
    try {
      final MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      job = service.retract(mediapackage);
    } catch (Exception e) {
      logger.warn("Unable to retract mediapackage '{}' from YouTube: {}", new Object[] { mediaPackageXml, e });
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(new JaxbJob(job)).build();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    return (service instanceof JobProducer) ? (JobProducer) service : null;
  }

  /**
   * Callback from the OSGi declarative services to set the service registry
   *
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(final ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
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
   * Callback from OSGi to set a reference to the youtube publication service.
   *
   * @param service
   *          the service to set
   */
  protected void setService(final YouTubePublicationService service) {
    this.service = service;
  }

}
