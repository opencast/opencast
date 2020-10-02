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

package org.opencastproject.animate.impl.endpoint;

import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.animate.api.AnimateService;
import org.opencastproject.animate.api.AnimateServiceException;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A service endpoint to expose the {@link AnimateService} via REST.
 */
@Path("/")
@RestService(
  name = "animate",
  title = "Animate Service",
  abstractText = "Create animated video clips using Synfig.",
  notes = {
    "Use <a href=https://www.synfig.org/>Synfig Studio</a> to create animation files"})
@Component(
    immediate = true,
    service = AnimateServiceRestEndpoint.class,
    property = {
        "service.description=Animate Service REST Endpoint",
        "opencast.service.type=org.opencastproject.animate",
        "opencast.service.path=/animate",
        "opencast.service.jobproducer=true"
    }
)
public class AnimateServiceRestEndpoint extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(AnimateServiceRestEndpoint.class);

  /** The inspection service */
  private AnimateService animateService;

  /** The service registry */
  private ServiceRegistry serviceRegistry = null;

  private static final Type stringMapType = new TypeToken<Map<String, String>>() { }.getType();
  private static final Type stringListType = new TypeToken<List<String>>() { }.getType();

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
   * Sets the animate service
   *
   * @param animateService
   *          the animate service
   */
  @Reference
  public void setAnimateService(AnimateService animateService) {
    this.animateService = animateService;
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("animate")
  @RestQuery(name = "animate", description = "Create animates video clip",
    restParameters = {
      @RestParameter(name = "animation", isRequired = true, type = STRING,
              description = "Location of to the animation"),
      @RestParameter(name = "arguments", isRequired = true, type = STRING,
              description = "Synfig command line arguments as JSON array"),
      @RestParameter(name = "metadata", isRequired = true, type = STRING,
              description = "Metadata for replacement as JSON object") },
    responses = {
      @RestResponse(description = "Animation created successfully", responseCode = HttpServletResponse.SC_OK),
      @RestResponse(description = "Invalid data", responseCode = HttpServletResponse.SC_BAD_REQUEST),
      @RestResponse(description = "Internal error", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
    returnDescription = "Returns the path to the generated animation video")
  public Response animate(
          @FormParam("animation") String animation,
          @FormParam("arguments") String argumentsString,
          @FormParam("metadata") String metadataString) {
    Gson gson = new Gson();
    try {
      Map<String, String> metadata = gson.fromJson(metadataString, stringMapType);
      List<String> arguments = gson.fromJson(argumentsString, stringListType);
      logger.debug("Start animation");
      Job job = animateService.animate(new URI(animation), metadata, arguments);
      return Response.ok(new JaxbJob(job)).build();
    } catch (JsonSyntaxException | URISyntaxException | NullPointerException e) {
      logger.debug("Invalid data passed to REST endpoint:\nanimation: {}\nmetadata: {}\narguments: {})",
              animation, metadataString, argumentsString);
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (AnimateServiceException e) {
      logger.error("Error animating file {}", animation, e);
      return Response.serverError().build();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    if (animateService instanceof JobProducer) {
      logger.debug("get animate service");
      return (JobProducer) animateService;
    }
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
