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
package org.opencastproject.publication.oaipmh.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.data.Collections.set;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Rest endpoint for publishing media to the OAI-PMH publication channel.
 */
@Path("/")
@RestService(name = "oaipmhpublicationservice", title = "OAI-PMH Publication Service", abstractText = "This service "
        + "publishes a media package element to the Matterhorn OAI-PMH channel.", notes = { "All paths above are "
        + "relative to the REST endpoint base (something like http://your.server/files).  If the service is down "
        + "or not working it will return a status 503, this means the the underlying service is not working and is "
        + "either restarting or has failed. A status code 500 means a general failure has occurred which is not "
        + "recoverable and was not anticipated. In other words, there is a bug!" })
public class OaiPmhPublicationRestService extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OaiPmhPublicationRestService.class);

  /** The OAI-PMH publication service */
  protected OaiPmhPublicationService service;

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
  public void setService(OaiPmhPublicationService service) {
    this.service = service;
  }

  @POST
  @Path("/")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "publish", description = "Publish a media package element to this publication channel", returnDescription = "The job that can be used to track the publication", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT),
          @RestParameter(name = "channel", isRequired = true, description = "The channel name", type = Type.STRING),
          @RestParameter(name = "downloadElementIds", isRequired = true, description = "The elements to publish to download seperated by ';;'", type = Type.STRING),
          @RestParameter(name = "streamingElementIds", isRequired = true, description = "The elements to publish to streaming seperated by ';;'", type = Type.STRING),
          @RestParameter(name = "checkAvailability", isRequired = false, description = "Whether to check for availability", type = Type.BOOLEAN, defaultValue = "true") }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the publication job") })
  public Response publish(@FormParam("mediapackage") String mediaPackageXml, @FormParam("channel") String channel,
          @FormParam("downloadElementIds") String downloadElementIds,
          @FormParam("streamingElementIds") String streamingElementIds,
          @FormParam("checkAvailability") @DefaultValue("true") boolean checkAvailability) throws Exception {
    final Job job;
    try {
      Set<String> download = new HashSet<String>();
      Set<String> streaming = new HashSet<String>();

      final MediaPackage mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      final String[] downloadElements = StringUtils.split(downloadElementIds, ";;");
      final String[] streamingElements = StringUtils.split(streamingElementIds, ";;");
      if (downloadElements != null)
        download = set(downloadElements);
      if (streamingElements != null)
        streaming = set(streamingElements);
      job = service.publish(mediaPackage, channel, download, streaming, checkAvailability);
    } catch (Exception e) {
      logger.warn("Error publishing element", e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(new JaxbJob(job)).build();
  }

  @POST
  @Path("/retract")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "retract", description = "Retract a media package element from this publication channel", returnDescription = "The job that can be used to track the retraction", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT),
          @RestParameter(name = "elementId", isRequired = true, description = "The element to retract", type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the retraction job") })
  public Response retract(@FormParam("mediapackage") String mediaPackageXml, @FormParam("elementId") String elementId)
          throws Exception {
    Job job = null;
    try {
      MediaPackage mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      job = service.retract(mediaPackage, elementId);
    } catch (Exception e) {
      logger.warn("Unable to retract mediapackage '{}' from the OAI-PMH channel: {}",
              new Object[] { mediaPackageXml, e });
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

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

}
