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
package org.opencastproject.sox.impl.endpoint;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.sox.api.SoxException;
import org.opencastproject.sox.api.SoxService;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A REST endpoint delegating functionality to the {@link SoxService}
 */
@Path("/")
@RestService(name = "sox", title = "Sox", abstractText = "This service creates and augments Matterhorn media packages that include media tracks, metadata "
        + "catalogs and attachments.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class SoxRestService extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SoxRestService.class);

  /** The rest documentation */
  protected String docs;

  /** The base server URL */
  protected String serverUrl;

  /** The composer service */
  protected SoxService soxService = null;

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
   * Sets the SoX service.
   *
   * @param soxService
   *          the SoX service
   */
  public void setSoxService(SoxService soxService) {
    this.soxService = soxService;
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
   * Analyze an audio track.
   *
   * @param sourceAudioTrack
   *          The source audio track
   * @return A response containing the job for this audio analyzing job in the response body.
   * @throws Exception
   */
  @POST
  @Path("analyze")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "analyze", description = "Starts an audio analyzing process", restParameters = { @RestParameter(description = "The track just containing the audio stream", isRequired = true, name = "sourceAudioTrack", type = Type.TEXT) }, reponses = {
          @RestResponse(description = "Results in an xml document containing the job for the analyzing task", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "If required parameters aren't set or if sourceAudioTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response analyze(@FormParam("sourceAudioTrack") String sourceAudioTrackAsXml) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceAudioTrackAsXml))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceAudioTrack must not be null").build();

    // Deserialize the track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceAudioTrackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceAudioTrack element must be of type track")
              .build();

    try {
      // Asynchronously analyze the specified audio track
      Job job = soxService.analyze((Track) sourceTrack);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (SoxException e) {
      logger.warn("Unable to analyze the audio track: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Normalize an audio track.
   *
   * @param sourceAudioTrack
   *          The source audio track
   * @param targetRmsLevDb
   *          the target RMS level dB
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("normalize")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "normalize", description = "Starts audio normalization process", restParameters = {
          @RestParameter(description = "The track containing the audio stream", isRequired = true, name = "sourceAudioTrack", type = Type.TEXT),
          @RestParameter(description = "The target RMS level dB", isRequired = true, name = "targetRmsLevDb", type = Type.INTEGER) }, reponses = {
          @RestResponse(description = "Results in an xml document containing the job for the audio normalization task", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "If required parameters aren't set or if sourceAudioTrack isn't from the type Track", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response normalize(@FormParam("sourceAudioTrack") String sourceAudioTrackAsXml,
          @FormParam("targetRmsLevDb") Float targetRmsLevDb) throws Exception {
    // Ensure that the POST parameters are present
    if (StringUtils.isBlank(sourceAudioTrackAsXml))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceAudioTrack must not be null").build();
    if (targetRmsLevDb == null)
      return Response.status(Response.Status.BAD_REQUEST).entity("targetRmsLevDb must not be null").build();

    // Deserialize the track
    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(sourceAudioTrackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType()))
      return Response.status(Response.Status.BAD_REQUEST).entity("sourceAudioTrack element must be of type track")
              .build();

    try {
      // Asynchronously normalyze the specified audio track
      Job job = soxService.normalize((Track) sourceTrack, targetRmsLevDb);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (SoxException e) {
      logger.warn("Unable to normalize the track: " + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    if (soxService instanceof JobProducer)
      return (JobProducer) soxService;
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
