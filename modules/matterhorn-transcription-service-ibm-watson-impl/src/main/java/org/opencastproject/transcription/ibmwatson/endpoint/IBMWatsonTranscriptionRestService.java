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
package org.opencastproject.transcription.ibmwatson.endpoint;

import org.opencastproject.job.api.JobProducer;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.transcription.ibmwatson.IBMWatsonTranscriptionService;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "IBMWatsonTranscriptionRestService", title = "Transcription Service REST Endpoint (uses IBM Watson services)", abstractText = "Uses external service to generate transcriptions of recordings.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/transcription)" })
public class IBMWatsonTranscriptionRestService extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(IBMWatsonTranscriptionRestService.class);

  /** The transcription service */
  protected IBMWatsonTranscriptionService service;

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  public void activate(ComponentContext cc) {
  }

  public void setTranscriptionService(IBMWatsonTranscriptionService service) {
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

  // CALLBACKS with status - default events: recognitions.started, recognitions.completed, and recognitions.failed.

  @GET
  @Path("results")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "results", description = "Called by the speech-to-text service when registering the callback url", returnDescription = "Echo the string sent.", restParameters = {
          @RestParameter(name = "challenge_string", description = "String to be echoed in the response body", isRequired = true, type = Type.STRING) }, reponses = {
                  @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "If no errors"),
                  @RestResponse(responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, description = "An error occurred") })
  // GET /transcripts/watson/results?challenge_string=51WRISEyAbuJq5fE HTTP/1.1" 302 5 "-" "Jersey/2.22.1 (Apache
  // HttpClient 4.5)"
  public Response checkCallbackUrl(@QueryParam("challenge_string") String challengeString,
          @Context HttpServletRequest request) {
    // Registering callback: just echo the challenge string
    logger.debug("==== checkCallback got called: challenge_string is '{}'", challengeString);
    Enumeration en = request.getHeaderNames();
    while (en.hasMoreElements()) {
      String name = (String) en.nextElement();
      logger.debug(String.format("==== %s: %s", name, request.getHeader(name)));
    }
    // return Response.ok(challengeString).build();
    return Response.ok(challengeString).type(MediaType.TEXT_PLAIN).build();
  }

  /**
   * { "id": "{job_id}", "event": "{recognitions_status}", "user_token": "{user_token}" } If the event is
   * recognitions.completed_with_results, the object includes a results field that provides the results of the
   * recognition request. The client should respond to the callback notification with status code 200.
   *
   */
  @POST
  @Path("results")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "results", description = "Called by the speech-to-text service to report status.", returnDescription = "", reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Got notification!") })
  public Response reportStatus(String body) {
    logger.trace("Body is: " + body);

    JSONObject jsonObj = null;
    try {
      JSONParser parser = new JSONParser();
      jsonObj = (JSONObject) parser.parse(body);
      // jsonObj = (JSONObject) parser.parse(request.getReader());
      String mpId = (String) jsonObj.get("user_token");
      String event = (String) jsonObj.get("event");
      logger.info("Transcription notification for mp {} is {}", mpId, event);

      if (IBMWatsonTranscriptionService.JobEvent.COMPLETED_WITH_RESULTS.equals(event))
        service.transcriptionDone(mpId, jsonObj);
      else if (IBMWatsonTranscriptionService.JobEvent.FAILED.equals(event))
        service.transcriptionError(mpId, jsonObj);

      // return Response.ok().build();
      return Response.ok().type(MediaType.APPLICATION_JSON).build();
    } catch (ParseException e) {
      logger.warn("{} occurred. Notification results could not be parsed: {}", e.getClass(),
              jsonObj == null ? jsonObj : jsonObj.toJSONString());
      return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    } catch (Exception e) {
      logger.warn(e.getMessage());
      return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
  }

}
