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
package org.opencastproject.inspection.ffmpeg.endpoints;

import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A service endpoint to expose the {@link MediaInspectionService} via REST.
 */
@Path("/")
@RestService(name = "mediainspection", title = "Media Inspection Service", abstractText = "This service extracts technical metadata from media files.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class MediaInspectionRestEndpoint extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionRestEndpoint.class);

  /** The inspection service */
  protected MediaInspectionService service;

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
   * Sets the inspection service
   *
   * @param service
   *          the inspection service
   */
  public void setService(MediaInspectionService service) {
    this.service = service;
  }

  /**
   * Removes the inspection service
   *
   * @param service
   */
  public void unsetService(MediaInspectionService service) {
    this.service = null;
  }

  /**
   * Callback from OSGi that is called when this service is activated.
   *
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    // String serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("inspect")
  @RestQuery(name = "inspect", description = "Analyze a given media file, returning a receipt to check on the status and outcome of the job", restParameters = { @RestParameter(description = "Location of the media file.", isRequired = false, name = "uri", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(description = "XML encoded receipt is returned.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Service unavailabe or not currently present", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE),
          @RestResponse(description = "Problem retrieving media file or invalid media file or URL.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response inspectTrack(@QueryParam("uri") URI uri) {
    checkNotNull(service);
    try {
      Job job = service.inspect(uri);
      return Response.ok(new JaxbJob(job)).build();
    } catch (Exception e) {
      logger.info(e.getMessage());
      return Response.serverError().build();
    }
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("enrich")
  @RestQuery(name = "enrich", description = "Analyze and add missing metadata of a given media file, returning a receipt to check on the status and outcome of the job.", restParameters = {
          @RestParameter(description = "MediaPackage Element, that should be enriched with metadata ", isRequired = true, name = "mediaPackageElement", type = RestParameter.Type.TEXT),
          @RestParameter(description = "Should the existing metadata values remain", isRequired = true, name = "override", type = RestParameter.Type.BOOLEAN) }, reponses = {
          @RestResponse(description = "XML encoded receipt is returned.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Service unavailabe or not currently present", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE),
          @RestResponse(description = "Problem retrieving media file or invalid media file or URL.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "")
  public Response enrichTrack(@FormParam("mediaPackageElement") String mediaPackageElement,
          @FormParam("override") boolean override) {
    checkNotNull(service);
    try {
      Job job = service.enrich(MediaPackageElementParser.getFromXml(mediaPackageElement), override);
      return Response.ok(new JaxbJob(job)).build();
    } catch (Exception e) {
      logger.info(e.getMessage(), e);
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

  /**
   * Checks if the service or services are available, if not it handles it by returning a 503 with a message
   *
   * @param services
   *          an array of services to check
   */
  protected void checkNotNull(Object... services) {
    if (services != null) {
      for (Object object : services) {
        if (object == null) {
          throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE);
        }
      }
    }
  }

}
