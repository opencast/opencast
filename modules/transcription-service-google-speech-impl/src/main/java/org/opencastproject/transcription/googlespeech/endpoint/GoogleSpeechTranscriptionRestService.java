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
package org.opencastproject.transcription.googlespeech.endpoint;

import org.opencastproject.job.api.JobProducer;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.transcription.api.TranscriptionServiceException;
import org.opencastproject.transcription.googlespeech.GoogleSpeechTranscriptionService;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "GoogleSpeechTranscriptionRestService", title = "Transcription Service REST Endpoint (uses Google Speech services)", abstractText = "Uses external service to generate transcriptions of recordings.", notes = {
  "All paths above are relative to the REST endpoint base (something like http://your.server/transcripts)"})
public class GoogleSpeechTranscriptionRestService extends AbstractJobProducerEndpoint {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(GoogleSpeechTranscriptionRestService.class);

  /**
   * The transcription service
   */
  protected GoogleSpeechTranscriptionService service;

  /**
   * The service registry
   */
  protected ServiceRegistry serviceRegistry = null;

  public void activate(ComponentContext cc) {
  }

  public void setTranscriptionService(GoogleSpeechTranscriptionService service) {
    this.service = service;
  }

  public void setServiceRegistry(ServiceRegistry service) {
    this.serviceRegistry = service;
  }

  @Override
  public JobProducer getService() {
    return service;
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @GET
  @Path("results")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "results", description = "Get JSON result of a submitted transcription job", returnDescription = "Returns a JSON representation of the transcription result", restParameters = {
    @RestParameter(name = "jobId", description = "job id of the submitted transcription (can be found in Google Speech database table)", isRequired = true, type = Type.STRING)}, responses = {
    @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "If no errors"),
    @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "An error occurred")})
  // GET v1/operations/7022084740052439959
  public Response getTranscriptionResult(@QueryParam("jobId") String jobId,
          @Context HttpServletRequest request) throws TranscriptionServiceException, IOException {
    logger.debug("REST endpoint getTranscriptionResult called with job id: '{}'", jobId);
    String jobResult = null;
    try {
      jobResult = service.getTranscriptionResults(jobId);
    } catch (TranscriptionServiceException e) {
      logger.warn("Could not get transcription result for jobId : {}", jobId);
      return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    return Response.ok(jobResult).build();
  }

  @GET
  @Path("status")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "status", description = "Get mediapackage transcription status", returnDescription = "Returns transcription status", restParameters = {
    @RestParameter(name = "mediaPackageID", description = "the mediapackage identifier", isRequired = true, type = Type.STRING)}, responses = {
    @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "If no errors"),
    @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "An error occurred")})
  public Response getTranscriptionStatus(@QueryParam("mediaPackageID") String mpId,
          @Context HttpServletRequest request) throws TranscriptionServiceException {
    logger.debug("REST endpoint getTranscriptionStatus called with mediapackage id '{}'", mpId);
    String status = null;
    try {
      status = service.getTranscriptionStatus(mpId);
    } catch (TranscriptionServiceException e) {
      logger.warn("Could not get mediapackage transcription status for mediapackageId : {}", mpId);
      return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    return Response.ok(status).build();
  }

}
