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
package org.opencastproject.coverimage.impl.endpoint;

import org.opencastproject.coverimage.CoverImageException;
import org.opencastproject.coverimage.CoverImageService;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

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
 * REST endpoint for {@link CoverImageService}
 */
@Path("/")
@RestService(name = "coverimage", title = "Cover Image Service", abstractText = "This endpoint triggers generation of cover images", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class CoverImageEndpoint extends AbstractJobProducerEndpoint {

  /** Reference to the service registry service */
  private ServiceRegistry serviceRegistry;

  /** Reference to the cover image service */
  private CoverImageService coverImageService;

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CoverImageEndpoint.class);

  @Override
  public JobProducer getService() {
    if (coverImageService instanceof JobProducer)
      return (JobProducer) coverImageService;
    else
      return null;
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @POST
  @Path("generate")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "generate", description = "Generates a cover image based on the given metadata", restParameters = {
          @RestParameter(description = "Metadata XML", isRequired = false, name = "xml", type = Type.TEXT),
          @RestParameter(description = "XSLT stylesheet", isRequired = true, name = "xsl", type = Type.TEXT),
          @RestParameter(description = "Width of the cover image", isRequired = true, name = "width", type = Type.INTEGER, defaultValue = "1600"),
          @RestParameter(description = "Height of the cover image", isRequired = true, name = "height", type = Type.INTEGER, defaultValue = "900"),
          @RestParameter(description = "URI of poster image", isRequired = false, name = "posterimage", type = Type.STRING),
          @RestParameter(description = "Flavor of target cover image", isRequired = true, name = "targetflavor", type = Type.STRING, defaultValue = "image/cover") }, reponses = {
          @RestResponse(description = "Results in an xml document containing the job for the cover image generation task", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "If required parameters aren't set or not valid", responseCode = HttpServletResponse.SC_BAD_REQUEST) }, returnDescription = "")
  public Response generateCoverImage(@FormParam("xml") String xml, @FormParam("xsl") String xsl,
          @FormParam("width") String width, @FormParam("height") String height,
          @FormParam("posterimage") String posterFlavor, @FormParam("targetflavor") String targetFlavor) {
    try {
      Job job = coverImageService.generateCoverImage(xml, xsl, width, height, posterFlavor, targetFlavor);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (CoverImageException e) {
      logger.warn("Error while creating cover image job via REST endpoint: {}", e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * OSGi callback to set the a reference to the service registry.
   *
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * OSGi callback to set the a reference to the cover image service.
   *
   * @param coverImageService
   */
  protected void setCoverImageService(CoverImageService coverImageService) {
    this.coverImageService = coverImageService;
  }

  /**
   * Callback from OSGi that is called when this service is activated.
   *
   * @param cc
   *          OSGi component context
   */
  protected void activate(ComponentContext cc) {
    logger.info("Cover Image REST Endpoint started");
  }

}
