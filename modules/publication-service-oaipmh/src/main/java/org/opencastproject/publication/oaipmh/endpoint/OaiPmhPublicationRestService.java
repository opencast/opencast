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
import static org.opencastproject.publication.api.OaiPmhPublicationService.SEPARATOR;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        + "publishes a media package element to the Opencast OAI-PMH channel.", notes = { "All paths above are "
        + "relative to the REST endpoint base (something like http://your.server/files).  If the service is down "
        + "or not working it will return a status 503, this means the the underlying service is not working and is "
        + "either restarting or has failed. A status code 500 means a general failure has occurred which is not "
        + "recoverable and was not anticipated. In other words, there is a bug!" })
public class OaiPmhPublicationRestService extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(OaiPmhPublicationRestService.class);
  private static final Pattern SEPARATE_PATTERN = Pattern.compile(SEPARATOR);

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

  private static Set<String> split(final String s) {
    if (s == null)
      return java.util.Collections.emptySet();
    return SEPARATE_PATTERN.splitAsStream(s).collect(Collectors.toSet());
  }

  @POST
  @Path("/")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "publish", description = "Publish a media package element to this publication channel", returnDescription = "The job that can be used to track the publication", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The media package", type = Type.TEXT),
          @RestParameter(name = "channel", isRequired = true, description = "The channel name", type = Type.STRING),
          @RestParameter(name = "downloadElementIds", isRequired = true, description = "The elements to publish to download separated by '" + SEPARATOR + "'", type = Type.STRING),
          @RestParameter(name = "streamingElementIds", isRequired = true, description = "The elements to publish to streaming separated by '" + SEPARATOR + "'", type = Type.STRING),
          @RestParameter(name = "checkAvailability", isRequired = false, description = "Whether to check for availability", type = Type.BOOLEAN, defaultValue = "true") }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the publication job") })
  public Response publish(@FormParam("mediapackage") String mediaPackageXml, @FormParam("channel") String channel,
          @FormParam("downloadElementIds") String downloadElementIds,
          @FormParam("streamingElementIds") String streamingElementIds,
          @FormParam("checkAvailability") @DefaultValue("true") boolean checkAvailability) throws Exception {
    final Job job;
    try {
      final MediaPackage mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      job = service.publish(mediaPackage, channel, split(downloadElementIds), split(streamingElementIds), checkAvailability);
    } catch (IllegalArgumentException e) {
      logger.warn("Unable to create an publication job", e);
      return Response.status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Error publishing element", e);
      return Response.serverError().build();
    }
    return Response.ok(new JaxbJob(job)).build();
  }

  @POST
  @Path("/replace")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "replace", description = "Replace a media package in this publication channel", returnDescription = "The job that can be used to track the publication", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The media package", type = Type.TEXT),
          @RestParameter(name = "channel", isRequired = true, description = "The channel name", type = Type.STRING),
          @RestParameter(name = "downloadElements", isRequired = true, description = "The additional elements to publish to download", type = Type.STRING),
          @RestParameter(name = "streamingElements", isRequired = true, description = "The additional elements to publish to streaming", type = Type.STRING),
          @RestParameter(name = "retractDownloadFlavors", isRequired = true, description = "The flavors of the elements to retract from download separated by  '" + SEPARATOR + "'", type = Type.STRING),
          @RestParameter(name = "retractStreamingFlavors", isRequired = true, description = "The flavors of the elements to retract from streaming separated by  '" + SEPARATOR + "'", type = Type.STRING),
          @RestParameter(name = "publications", isRequired = true, description = "The publications to update", type = Type.STRING),
          @RestParameter(name = "checkAvailability", isRequired = false, description = "Whether to check for availability", type = Type.BOOLEAN, defaultValue = "true") }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the publication job") })
  public Response replace(
          @FormParam("mediapackage") final String mediaPackageXml,
          @FormParam("channel") final String channel,
          @FormParam("downloadElements") final String downloadElementsXml,
          @FormParam("streamingElements") final String streamingElementsXml,
          @FormParam("retractDownloadFlavors") final String retractDownloadFlavorsString,
          @FormParam("retractStreamingFlavors") final String retractStreamingFlavorsString,
          @FormParam("publications") final String publicationsXml,
          @FormParam("checkAvailability") @DefaultValue("true") final boolean checkAvailability) {
    final Job job;
    try {
      final MediaPackage mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      final Set<? extends MediaPackageElement> downloadElements = new HashSet<MediaPackageElement>(
              MediaPackageElementParser.getArrayFromXml(downloadElementsXml));
      final Set<? extends MediaPackageElement> streamingElements = new HashSet<MediaPackageElement>(
              MediaPackageElementParser.getArrayFromXml(streamingElementsXml));
      final Set<MediaPackageElementFlavor> retractDownloadFlavors = split(retractDownloadFlavorsString).stream()
          .filter(s -> !s.isEmpty())
          .map(MediaPackageElementFlavor::parseFlavor)
          .collect(Collectors.toSet());
      final Set<MediaPackageElementFlavor> retractStreamingFlavors = split(retractStreamingFlavorsString).stream()
          .filter(s -> !s.isEmpty())
          .map(MediaPackageElementFlavor::parseFlavor)
          .collect(Collectors.toSet());
      final Set<? extends Publication> publications = MediaPackageElementParser.getArrayFromXml(publicationsXml)
          .stream().map(p -> (Publication) p).collect(Collectors.toSet());
      job = service.replace(mediaPackage, channel, downloadElements, streamingElements, retractDownloadFlavors,
          retractStreamingFlavors, publications, checkAvailability);
    } catch (IllegalArgumentException e) {
      logger.warn("Unable to create a publication job", e);
      return Response.status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Error publishing or retracting element", e);
      return Response.serverError().build();
    }
    return Response.ok(new JaxbJob(job)).build();
  }

  @POST
  @Path("/replacesync")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "replacesync", description = "Synchronously Replace a media package in this publication channel", returnDescription = "The publication", restParameters = {
      @RestParameter(name = "mediapackage", isRequired = true, description = "The media package", type = Type.TEXT),
      @RestParameter(name = "channel", isRequired = true, description = "The channel name", type = Type.STRING),
      @RestParameter(name = "downloadElements", isRequired = true, description = "The additional elements to publish to download", type = Type.STRING),
      @RestParameter(name = "streamingElements", isRequired = true, description = "The additional elements to publish to streaming", type = Type.STRING),
      @RestParameter(name = "retractDownloadFlavors", isRequired = true, description = "The flavors of the elements to retract from download separated by  '" + SEPARATOR + "'", type = Type.STRING),
      @RestParameter(name = "retractStreamingFlavors", isRequired = true, description = "The flavors of the elements to retract from streaming separated by  '" + SEPARATOR + "'", type = Type.STRING),
      @RestParameter(name = "publications", isRequired = true, description = "The publications to update", type = Type.STRING),
      @RestParameter(name = "checkAvailability", isRequired = false, description = "Whether to check for availability", type = Type.BOOLEAN, defaultValue = "true") }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the publication") })
  public Response replaceSync(
      @FormParam("mediapackage") final String mediaPackageXml,
      @FormParam("channel") final String channel,
      @FormParam("downloadElements") final String downloadElementsXml,
      @FormParam("streamingElements") final String streamingElementsXml,
      @FormParam("retractDownloadFlavors") final String retractDownloadFlavorsString,
      @FormParam("retractStreamingFlavors") final String retractStreamingFlavorsString,
      @FormParam("publications") final String publicationsXml,
      @FormParam("checkAvailability") @DefaultValue("true") final boolean checkAvailability) throws MediaPackageException {
    final Publication publication;
    try {
      final MediaPackage mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      final Set<? extends MediaPackageElement> downloadElements = new HashSet<MediaPackageElement>(
          MediaPackageElementParser.getArrayFromXml(downloadElementsXml));
      final Set<? extends MediaPackageElement> streamingElements = new HashSet<MediaPackageElement>(
          MediaPackageElementParser.getArrayFromXml(streamingElementsXml));
      final Set<MediaPackageElementFlavor> retractDownloadFlavors = split(retractDownloadFlavorsString).stream()
          .filter(s -> !s.isEmpty())
          .map(MediaPackageElementFlavor::parseFlavor)
          .collect(Collectors.toSet());
      final Set<MediaPackageElementFlavor> retractStreamingFlavors = split(retractStreamingFlavorsString).stream()
          .filter(s -> !s.isEmpty())
          .map(MediaPackageElementFlavor::parseFlavor)
          .collect(Collectors.toSet());
      final Set<? extends Publication> publications = MediaPackageElementParser.getArrayFromXml(publicationsXml)
          .stream().map(p -> (Publication) p).collect(Collectors.toSet());
      publication = service.replaceSync(mediaPackage, channel, downloadElements, streamingElements, retractDownloadFlavors,
          retractStreamingFlavors, publications, checkAvailability);
    } catch (Exception e) {
      logger.warn("Error publishing or retracting element", e);
      return Response.serverError().build();
    }
    return Response.ok(MediaPackageElementParser.getAsXml(publication)).build();
  }

  @POST
  @Path("/retract")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "retract", description = "Retract a media package element from this publication channel", returnDescription = "The job that can be used to track the retraction", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The media package", type = Type.TEXT),
          @RestParameter(name = "channel", isRequired = true, description = "The OAI-PMH channel to retract from", type = Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the retraction job") })
  public Response retract(@FormParam("mediapackage") String mediaPackageXml, @FormParam("channel") String channel)
          throws Exception {
    Job job = null;
    MediaPackage mediaPackage = null;
    try {
      mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      job = service.retract(mediaPackage, channel);
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to create an retract job", e);
      return Response.status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Unable to retract media package '{}' from the OAI-PMH channel {}",
              mediaPackage != null ? mediaPackage.getIdentifier().compact() : "<parsing error>", channel, e);
      return Response.serverError().build();
    }
    return Response.ok(new JaxbJob(job)).build();
  }

  @POST
  @Path("/updateMetadata")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "update", description = "Update metadata of an published media package. "
          + "This endpoint does not update any media files. If you want to update the whole media package, use the "
          + "publish endpoint.",
          returnDescription = "The job that can be used to update the metadata of an media package", restParameters = {
          @RestParameter(name = "mediapackage", isRequired = true, description = "The updated media package", type = Type.TEXT),
          @RestParameter(name = "channel", isRequired = true, description = "The channel name", type = Type.STRING),
          @RestParameter(name = "flavors", isRequired = true, description = "The element flavors to be updated, separated by '" + SEPARATOR + "'", type = Type.STRING),
          @RestParameter(name = "tags", isRequired = true, description = "The element tags to be updated, separated by '" + SEPARATOR + "'", type = Type.STRING),
          @RestParameter(name = "checkAvailability", isRequired = false, description = "Whether to check for availability", type = Type.BOOLEAN, defaultValue = "true") },
          reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the publication job") })
  public Response updateMetadata(@FormParam("mediapackage") String mediaPackageXml,
          @FormParam("channel") String channel,
          @FormParam("flavors") String flavors,
          @FormParam("tags") String tags,
          @FormParam("checkAvailability") @DefaultValue("true") boolean checkAvailability) throws Exception {
    final Job job;
    try {
      final MediaPackage mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      job = service.updateMetadata(mediaPackage, channel, split(flavors), split(tags), checkAvailability);
    } catch (IllegalArgumentException e) {
      logger.debug("Unable to create an update metadata job", e);
      return Response.status(Status.BAD_REQUEST).build();
    } catch (Exception e) {
      logger.warn("Error publishing element", e);
      return Response.serverError().build();
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
