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
package org.opencastproject.publication.configurable.endpoint;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.publication.api.ConfigurablePublicationService;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Rest endpoint, mainly for publishing media to a configurable channel
 */
@Path("/")
@RestService(name = "configurablepublicationservice", title = "Configurable Publication Service", abstractText =
        "This service publishes and retracts media package elements to a configurable channel", notes = { "All paths above are "
        + "relative to the REST endpoint base (something like http://your.server/files).  If the service is down "
        + "or not working it will return a status 503, this means the the underlying service is not working and is "
        + "either restarting or has failed. A status code 500 means a general failure has occurred which is not "
        + "recoverable and was not anticipated. In other words, there is a bug!" })
public class ConfigurablePublicationRestService extends AbstractJobProducerEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurablePublicationRestService.class);

  /* Gson is thread-safe so we use a single instance */
  private Gson gson = new Gson();

  private ConfigurablePublicationService service;
  private ServiceRegistry serviceRegistry;

  public void setService(final ConfigurablePublicationService service) {
    this.service = service;
  }

  public void setServiceRegistry(final ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public JobProducer getService() {
    // The implementation is, of course, resolved by OSGi, so to be "clean", we hold a reference to just the interface
    // in this class, but at _this_ point, we assume it at least implements JobProducer.
    return (JobProducer) this.service;
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return this.serviceRegistry;
  }

  @POST
  @Path("/replace")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "replace", description = "Replace a media package in this publication channel", returnDescription = "The job that can be used to track the publication", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The media package", type = RestParameter.Type.TEXT),
          @RestParameter(name = "channel", isRequired = true, description = "The channel name", type = RestParameter.Type.STRING),
          @RestParameter(name = "addElements", isRequired = true, description =
                  "The media package elements to published", type = RestParameter.Type.STRING),
          @RestParameter(name = "retractElements", isRequired = true, description =
                  "The identifiers of the media package elements to be retracted from the media package", type = RestParameter.Type.STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "An XML representation of the publication job") })
  public Response replace(@FormParam("mediapackage") final String mediaPackageXml,
          @FormParam("channel") final String channel, @FormParam("addElements") final String addElementsXml,
          @FormParam("retractElements") final String retractElements) {
    Response response;
    final Job job;
    try {
      final MediaPackage mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      final Collection<? extends MediaPackageElement> addElements = new HashSet<>(
              MediaPackageElementParser.getArrayFromXml(addElementsXml));
      Set<String> retractElementsIds = gson.fromJson(retractElements, new TypeToken<Set<String>>() { }.getType());
      job = service.replace(mediaPackage, channel, addElements, retractElementsIds);
      response = Response.ok(new JaxbJob(job)).build();
    } catch (IllegalArgumentException e) {
      logger.warn("Unable to create a publication job", e);
      response = Response.status(Response.Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Error publishing or retracting element", e);
      response = Response.serverError().build();
    }
    return response;
  }

  @POST
  @Path("/replacesync")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "replacesync", description = "Synchronously replace a media package in this publication channel", returnDescription = "The publication", restParameters = {
      @RestParameter(name = "mediapackage", isRequired = true, description = "The media package", type = RestParameter.Type.TEXT),
      @RestParameter(name = "channel", isRequired = true, description = "The channel name", type = RestParameter.Type.STRING),
      @RestParameter(name = "addElements", isRequired = true, description =
          "The media package elements to published", type = RestParameter.Type.STRING),
      @RestParameter(name = "retractElements", isRequired = true, description =
          "The identifiers of the media package elements to be retracted from the media package", type = RestParameter.Type.STRING) }, reponses = {
      @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "An XML representation of the publication") })
  public Response replaceSync(@FormParam("mediapackage") final String mediaPackageXml,
                          @FormParam("channel") final String channel, @FormParam("addElements") final String addElementsXml,
                          @FormParam("retractElements") final String retractElements) {
    Response response;
    final Publication publication;
    try {
      final MediaPackage mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      final Collection<? extends MediaPackageElement> addElements = new HashSet<>(
          MediaPackageElementParser.getArrayFromXml(addElementsXml));
      Set<String> retractElementsIds = gson.fromJson(retractElements, new TypeToken<Set<String>>() { }.getType());
      publication = service.replaceSync(mediaPackage, channel, addElements, retractElementsIds);
      response = Response.ok(MediaPackageElementParser.getAsXml(publication)).build();
    } catch (Exception e) {
      logger.warn("Error publishing or retracting element", e);
      response = Response.serverError().build();
    }
    return response;
  }
}
