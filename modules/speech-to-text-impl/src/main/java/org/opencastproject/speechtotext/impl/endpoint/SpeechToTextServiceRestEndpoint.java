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

package org.opencastproject.speechtotext.impl.endpoint;

import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.speechtotext.api.SpeechToTextService;
import org.opencastproject.speechtotext.api.SpeechToTextServiceException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.JsonSyntaxException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A service endpoint to expose the {@link SpeechToTextService} via REST.
 */
@Path("/")
@RestService(
    name = "speechtotext",
    title = "Speech to Text Service",
    abstractText = "Create subtitles for video or audio clips.",
    notes = { "The file format is WebVTT (*.vtt)" }
)
@Component(
    immediate = true,
    service = SpeechToTextServiceRestEndpoint.class,
    property = {
        "service.description=Speech to Text Service REST Endpoint",
        "opencast.service.type=org.opencastproject.speechtotext",
        "opencast.service.path=/speechtotext",
        "opencast.service.jobproducer=true"
    }
)
public class SpeechToTextServiceRestEndpoint extends AbstractJobProducerEndpoint {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(SpeechToTextServiceRestEndpoint.class);

  /** The speech-to-text service. */
  private SpeechToTextService speechToTextService;

  /** The service registry. */
  private ServiceRegistry serviceRegistry = null;


  //================================================================================
  // REST Endpoints
  //================================================================================

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("speechtotext")
  @RestQuery(name = "speechtotext", description = "Generates subtitles for media files with audio.",
      restParameters = {
      @RestParameter(name = "mediaFilePath", isRequired = true, type = STRING,
              description = "Location of to the media file."),
      @RestParameter(name = "language", isRequired = true, type = STRING,
              description = "Language of the media file.") },
      responses = {
          @RestResponse(description = "Subtitles created successfully", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Invalid data", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "Internal error", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      },
      returnDescription = "Returns the path to the generated subtitles file."
  )
  public Response speechToText(
          @FormParam("mediaFilePath") String mediaFilePath,
          @FormParam("language") String language) {
    try {
      logger.debug("Starting to generate subtitles.");
      Job job = speechToTextService.transcribe(new URI(mediaFilePath), language);
      return Response.ok(new JaxbJob(job)).build();
    } catch (JsonSyntaxException | URISyntaxException | NullPointerException e) {
      logger.debug("Invalid data passed to REST endpoint:\nmediaFilePath: {}\nlanguage: {})",
              mediaFilePath, language);
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (SpeechToTextServiceException e) {
      logger.error("Error generating subtitles from file {}", mediaFilePath, e);
      return Response.serverError().build();
    }
  }


  //================================================================================
  // Getter and Setter
  //================================================================================

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    if (speechToTextService instanceof JobProducer) {
      logger.debug("get speech to text service");
      return (JobProducer) speechToTextService;
    }
    return null;
  }

  /**
   * Callback from the OSGi declarative services to set the service registry.
   *
   * @param serviceRegistry
   *          the service registry
   */
  @Reference
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Sets the speechToText service
   *
   * @param speechToTextService the speechToText service
   */
  @Reference
  public void setSpeechToTextService(SpeechToTextService speechToTextService) {
    this.speechToTextService = speechToTextService;
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
