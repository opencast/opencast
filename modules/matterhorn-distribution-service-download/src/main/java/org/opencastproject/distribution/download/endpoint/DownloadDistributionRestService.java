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
package org.opencastproject.distribution.download.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.Response.status;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.R.serverError;

import org.opencastproject.distribution.api.DownloadDistributionService;
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

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Rest endpoint for distributing media to the local distribution channel.
 */
@Path("/")
@RestService(name = "localdistributionservice", title = "Local Distribution Service",
  abstractText = "This service distributes media packages to the Matterhorn feed and engage services.",
  notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
        + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class DownloadDistributionRestService extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(DownloadDistributionRestService.class);

  /** The download distribution service */
  protected DownloadDistributionService service;

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
   * @param service
   *          the service to set
   */
  public void setService(DownloadDistributionService service) {
    this.service = service;
  }

  /**
   * Callback from OSGi that is called when this service is activated.
   *
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
  }

  @POST
  @Path("/")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "distribute",
             description = "Distribute a media package element to this distribution channel",
             returnDescription = "The job that can be used to track the distribution",
             restParameters = {
                     @RestParameter(name = "mediapackage",
                                    isRequired = true,
                                    description = "The mediapackage",
                                    type = Type.TEXT),
                     @RestParameter(name = "channelId",
                                    isRequired = true,
                                    description = "The publication channel ID",
                                    type = Type.TEXT),
                     @RestParameter(name = "elementId",
                                    isRequired = true,
                                    description = "The element to distribute",
                                    type = Type.STRING) },
             reponses = {
                     @RestResponse(responseCode = SC_OK,
                                   description = "An XML representation of the distribution job") })
  public Response distribute(@FormParam("mediapackage") String mediaPackageXml,
                             @FormParam("elementId") String elementId,
                             @FormParam("channelId") String channelId,
                             @DefaultValue("true") @FormParam("checkAvailability") boolean checkAvailability)
          throws Exception {
    try {
      final MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      final Job job = service.distribute(channelId, mediapackage, elementId, checkAvailability);
      return ok(new JaxbJob(job));
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to distribute element: {}", e.getMessage());
      return status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Error distributing element", e);
      return serverError();
    }
  }

  @POST
  @Path("/retract")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "retract", description = "Retract a media package element from this distribution channel", returnDescription = "The job that can be used to track the retraction",
             restParameters = {
                     @RestParameter(name = "mediapackage",
                                    isRequired = true,
                                    description = "The mediapackage",
                                    type = Type.TEXT),
                     @RestParameter(name = "channelId",
                                    isRequired = true,
                                    description = "The publication channel ID",
                                    type = Type.TEXT),
                     @RestParameter(name = "elementId",
                                    isRequired = true,
                                    description = "The element to retract",
                                    type = Type.STRING) },
             reponses = {
                     @RestResponse(responseCode = SC_OK,
                                   description = "An XML representation of the retraction job") })
  public Response retract(@FormParam("mediapackage") String mediaPackageXml,
                          @FormParam("elementId") String elementId,
                          @FormParam("channelId") String channelId)
          throws Exception {
    try {
      final MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      final Job job = service.retract(channelId, mediapackage, elementId);
      return ok(new JaxbJob(job));
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to retract element: {}", e.getMessage());
      return status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Unable to retract mediapackage '{}' from download channel: {}", new Object[] { mediaPackageXml, e });
      return serverError();
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
}
