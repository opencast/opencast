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
package org.opencastproject.distribution.youtube.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
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

/**
 * Rest endpoint for distributing media to youtube.
 */
@Path("/")
@RestService(name = "youtubedistributionservice", title = "YouTube Distribution Service", abstractText = "", notes = { "" })
public class YoutubeDistributionRestService extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(YoutubeDistributionRestService.class);

  /** The distribution service */
  protected DistributionService service;

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /**
   * Callback from the OSGi declarative services to set the service registry
   * 
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Callback from OSGi to set a reference to the distribution service.
   * 
   * @param service
   *          the service to set
   */
  protected void setService(DistributionService service) {
    this.service = service;
  }

  @POST
  @Path("/")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "distribute", description = "Distribute a media package element to this distribution channel", returnDescription = "The job that can be used to track the distribution", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT),
          @RestParameter(name = "elementId", isRequired = true, description = "The element to distribute", type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the distribution job") })
  public Response distribute(@FormParam("mediapackage") String mediaPackageXml, @FormParam("elementId") String elementId)
          throws Exception {
    Job job = null;
    try {
      MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      job = service.distribute(mediapackage, elementId);
    } catch (Exception e) {
      logger.warn("Error distributing element to YouTube", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(new JaxbJob(job)).build();
  }

  @POST
  @Path("/retract")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "retract", description = "Retract a media package element from this distribution channel", returnDescription = "The job that can be used to track the retraction", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT),
          @RestParameter(name = "elementId", isRequired = true, description = "The element to retract", type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the retraction job") })
  public Response retract(@FormParam("mediapackage") String mediaPackageXml, @FormParam("elementId") String elementId)
          throws Exception {
    Job job = null;
    try {
      MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      job = service.retract(mediapackage, elementId);
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
    if (service instanceof JobProducer)
      return (JobProducer) service;
    else
      return null;
  }

}
