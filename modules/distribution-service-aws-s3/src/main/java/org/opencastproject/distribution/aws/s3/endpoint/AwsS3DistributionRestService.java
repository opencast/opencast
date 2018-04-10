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
package org.opencastproject.distribution.aws.s3.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.Response.status;

import org.opencastproject.distribution.aws.s3.api.AwsS3DistributionService;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
 * Rest endpoint for distributing files to AWS S3.
 */
@Path("/")
@RestService(name = "awss3distributionservice", title = "AWS S3 Distribution Service", abstractText = "This service distributes media package elements to AWS S3.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class AwsS3DistributionRestService extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(AwsS3DistributionRestService.class);

  /** The distribution service */
  protected AwsS3DistributionService service;

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /**
   * OSGi activation callback
   *
   * @param cc
   *          this component's context
   */
  public void activate(ComponentContext cc) {
  }

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
  public void setService(AwsS3DistributionService service) {
    this.service = service;
  }

  @POST
  @Path("/")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "distribute", description = "Distribute a media package element to this distribution channel", returnDescription = "The job that can be used to track the distribution", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT),
          @RestParameter(name = "channelId", isRequired = true, description = "The publication channel ID", type = Type.TEXT),
          @RestParameter(name = "elementId", isRequired = true, description = "The element to distribute", type = Type.STRING),
          @RestParameter(name = "checkAvailability", isRequired = false, description = "If the service should try to access the distributed element", type = Type.BOOLEAN) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the distribution job") })
  public Response distribute(@FormParam("mediapackage") String mediaPackageXml,
          @FormParam("channelId") String channelId, @FormParam("elementId") String elementId,
          @DefaultValue("true") @FormParam("checkAvailability") boolean checkAvailability) throws Exception {
    Job job = null;
    try {
      Gson gson = new Gson();
      Set<String> setElementIds = gson.fromJson(elementId, new TypeToken<Set<String>>() { }.getType());
      MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      job = service.distribute(channelId, mediapackage, setElementIds, checkAvailability);
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to distribute element: {}", e.getMessage());
      return status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Unable to distribute media package {}, element {} to aws s3 channel: {}", mediaPackageXml, elementId, e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(new JaxbJob(job)).build();
  }

  @POST
  @Path("/distributesync")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "distributesync", description = "Synchronously distribute a media package element to this distribution channel", returnDescription = "The distribution", restParameters = {
      @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT),
      @RestParameter(name = "channelId", isRequired = true, description = "The publication channel ID", type = Type.TEXT),
      @RestParameter(name = "elementId", isRequired = true, description = "The element to distribute", type = Type.STRING),
      @RestParameter(name = "checkAvailability", isRequired = false, description = "If the service should try to access the distributed element", type = Type.BOOLEAN) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the distribution") })
  public Response distributeSync(@FormParam("mediapackage") String mediaPackageXml,
                             @FormParam("channelId") String channelId, @FormParam("elementId") String elementId,
                             @DefaultValue("true") @FormParam("checkAvailability") boolean checkAvailability) throws Exception {
    List<MediaPackageElement> result = null;
    try {
      Gson gson = new Gson();
      Set<String> setElementIds = gson.fromJson(elementId, new TypeToken<Set<String>>() { }.getType());
      MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      result = service.distributeSync(channelId, mediapackage, setElementIds, checkAvailability);
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to distribute element: {}", e.getMessage());
      return status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Unable to distribute media package {}, element {} to aws s3 channel: {}", mediaPackageXml, elementId, e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(MediaPackageElementParser.getArrayAsXml(result)).build();
  }

  @POST
  @Path("/retract")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "retract", description = "Retract a media package element from this distribution channel", returnDescription = "The job that can be used to track the retraction", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT),
          @RestParameter(name = "channelId", isRequired = true, description = "The publication channel ID", type = Type.TEXT),
          @RestParameter(name = "elementId", isRequired = true, description = "The element to retract", type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the retraction job") })
  public Response retract(@FormParam("mediapackage") String mediaPackageXml, @FormParam("channelId") String channelId,
          @FormParam("elementId") String elementId) throws Exception {
    Job job = null;
    try {
      Gson gson = new Gson();
      Set<String> setElementIds = gson.fromJson(elementId, new TypeToken<Set<String>>() { }.getType());
      MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      job = service.retract(channelId, mediapackage, setElementIds);
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to retract element: {}", e.getMessage());
      return status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Unable to retract media package {}, element {} from aws s3 channel: {}", mediaPackageXml, elementId, e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(new JaxbJob(job)).build();
  }

  @POST
  @Path("/retractsync")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "retractsync", description = "Synchronously retract a media package element from this distribution channel", returnDescription = "The retraction", restParameters = {
      @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT),
      @RestParameter(name = "channelId", isRequired = true, description = "The publication channel ID", type = Type.TEXT),
      @RestParameter(name = "elementId", isRequired = true, description = "The element to retract", type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the retraction") })
  public Response retractSync(@FormParam("mediapackage") String mediaPackageXml, @FormParam("channelId") String channelId,
                          @FormParam("elementId") String elementId) throws Exception {
    List<MediaPackageElement> result = null;
    try {
      Gson gson = new Gson();
      Set<String> setElementIds = gson.fromJson(elementId, new TypeToken<Set<String>>() { }.getType());
      MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      result = service.retractSync(channelId, mediapackage, setElementIds);
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to retract element: {}", e.getMessage());
      return status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Unable to retract media package {}, element {} from aws s3 channel: {}", mediaPackageXml, elementId, e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(MediaPackageElementParser.getArrayAsXml(result)).build();
  }

  /* TODO
  Commented out due to changes in the way the element IDs are passed (ie, a list rather than individual ones
  per job).  This code is still useful long term, but I don't have time to write the necessary wrapper code
  around it right now.
  @POST
  @Path("/restore")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "restore", description = "Restore a media package element from this distribution channel", returnDescription = "The job that can be used to track the restoration", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The mediapackage", type = Type.TEXT),
          @RestParameter(name = "channelId", isRequired = true, description = "The publication channel ID", type = Type.TEXT),
          @RestParameter(name = "elementId", isRequired = true, description = "The element to retract", type = Type.STRING),
          @RestParameter(name = "fileName", isRequired = false, description = "The filename of the file to restore", type = Type.TEXT)}, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the restoration job") })
  public Response restore(@FormParam("mediapackage") String mediaPackageXml, @FormParam("channelId") String channelId,
          @FormParam("elementId") String elementId, @FormParam("fileName") String fileName) throws Exception {
    Job job = null;
    try {
      MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      if (StringUtils.isNotBlank(fileName)) {
        job = service.restore(channelId, mediapackage, elementId, fileName);
      } else {
        job = service.restore(channelId, mediapackage, elementId);
      }
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to restore element: {}", e.getMessage());
      return status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Unable to restore media package {}, element {} from aws s3 channel: {}", mediaPackageXml, elementId, e);
      return Response.serverError().status(Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(new JaxbJob(job)).build();
  }
  */

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
