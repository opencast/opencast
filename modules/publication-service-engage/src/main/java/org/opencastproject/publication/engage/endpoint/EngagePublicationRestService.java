/*
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
package org.opencastproject.publication.engage.endpoint;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.publication.api.EngagePublicationService;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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
 * Rest endpoint, mainly for publishing media to Engage
 */
@Path("/")
@RestService(
    name = "engagepublicationservice",
    title = "Engage Publication Service",
    abstractText = "This service publishes and retracts media package elements to Engage",
    notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files). "
            + "If the service is down or not working it will return a status 503, this means the "
            + "underlying service is not working and is either restarting or has failed. A status "
            + "code 500 means a general failure has occurred which is not recoverable and was not "
            + "anticipated. In other words, there is a bug!"
    }
)
@Component(
    immediate = true,
    service = EngagePublicationRestService.class,
    property = {
        "service.description=Engage Publication REST Endpoint",
        "opencast.service.type=org.opencastproject.publication.engage",
        "opencast.service.path=/publication/engage",
        "opencast.service.jobproducer=true"
    }
)
public class EngagePublicationRestService extends AbstractJobProducerEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(EngagePublicationRestService.class);

  private EngagePublicationService service;
  private ServiceRegistry serviceRegistry;

  @Reference
  public void setService(final EngagePublicationService service) {
    this.service = service;
  }

  @Reference
  public void setServiceRegistry(final ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public JobProducer getService() {
    // The implementation is, of course, resolved by OSGi, so to be "clean", we hold a reference to just
    // the interface in this class, but at _this_ point, we assume it at least implements JobProducer.
    return (JobProducer) this.service;
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return this.serviceRegistry;
  }

  @POST
  @Path("/publish")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(
      name = "publish",
      description = "Publish a media package to Engage",
      returnDescription = "The job that can be used to track the publication",
      restParameters = {
          @RestParameter(
              name = "mediapackage",
              isRequired = true,
              description = "The media package",
              type = RestParameter.Type.TEXT
          ),
          @RestParameter(
              name = "checkAvailability",
              isRequired = false,
              defaultValue = "false",
              description = "Check if the media is reachable",
              type = RestParameter.Type.BOOLEAN
          ),
          @RestParameter(
              name = "strategy",
              isRequired = false,
              defaultValue = "default",
              description = "Strategy to handle repeat publications",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "downloadSourceFlavors",
              isRequired = false,
              description = "Distribute any mediapackage elements with one of these (comma separated) flavors to "
                  + "download",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "downloadSourceTags",
              isRequired = false,
              description = "Distribute any mediapackage elements with one of these (comma separated) tags to download",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "downloadTargetSubflavor",
              isRequired = false,
              description = "Subflavor to use for distributed material",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "downloadTargetTags",
              isRequired = false,
              description = "Add tags (comma separated) to published media",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "streamingSourceFlavors",
              isRequired = false,
              description = "Specifies which media should be published to the streaming server",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "streamingSourceTags",
              isRequired = false,
              description = "Specifies which media should be published to the streaming server",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "streamingTargetSubflavor",
              isRequired = false,
              description = "Subflavor to use for distributed material",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "streamingTargetTags",
              isRequired = false,
              description = "Add tags (comma separated) to published media",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "mergeForceFlavors",
              isRequired = false,
              defaultValue = "dublincore/*,security/*",
              description = "Flavors of elements for which an update is enforced when merging catalogs",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "addForceFlavors",
              isRequired = false,
              description = "Works only if strategy 'merge' is used. Elements with these flavors will be added to an"
                  + "existing publication. No published elements will be deleted or overwritten.",
              type = RestParameter.Type.STRING
          )
      },
      responses = {
          @RestResponse(
              responseCode = HttpServletResponse.SC_OK,
              description = "An XML representation of the publication job"
          )
      }
  )
  public Response publish(@FormParam("mediapackage") final String mediaPackageXml,
      @FormParam("checkAvailability") String checkAvailability, @FormParam("strategy") String strategy,
      @FormParam("downloadSourceFlavors") String downloadSourceFlavors,
      @FormParam("downloadSourceTags") String downloadSourceTags,
      @FormParam("downloadTargetSubflavor") String downloadTargetSubflavor,
      @FormParam("downloadTargetTags") String downloadTargetTags,
      @FormParam("streamingSourceFlavors") String streamingSourceFlavors,
      @FormParam("streamingSourceTags") String streamingSourceTags,
      @FormParam("streamingTargetSubflavor") String streamingTargetSubflavor,
      @FormParam("streamingTargetTags") String streamingTargetTags,
      @FormParam("mergeForceFlavors") String mergeForceFlavors, @FormParam("addForceFlavors") String addForceFlavors) {
    try {
      final MediaPackage mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      Job job = service.publish(mediaPackage, checkAvailability, strategy, downloadSourceFlavors,
          downloadSourceTags, downloadTargetSubflavor, downloadTargetTags, streamingSourceFlavors, streamingSourceTags,
          streamingTargetSubflavor, streamingTargetTags, mergeForceFlavors, addForceFlavors);
      return Response.ok(new JaxbJob(job)).build();
    } catch (Exception e) {
      logger.warn("Error publishing or retracting element", e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("/publishsync")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(
      name = "publishsync",
      description = "Synchronously publish a media package to Engage",
      returnDescription = "The publication",
      restParameters = {
          @RestParameter(
              name = "mediapackage",
              isRequired = true,
              description = "The media package",
              type = RestParameter.Type.TEXT
          ),
          @RestParameter(
              name = "checkAvailability",
              isRequired = false,
              defaultValue = "false",
              description = "Check if the media is reachable",
              type = RestParameter.Type.BOOLEAN
          ),
          @RestParameter(
              name = "strategy",
              isRequired = false,
              defaultValue = "default",
              description = "Strategy to handle repeat publications",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "downloadSourceFlavors",
              isRequired = false,
              description = "Distribute any mediapackage elements with one of these (comma separated) flavors to "
                  + "download",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "downloadSourceTags",
              isRequired = false,
              description = "Distribute any mediapackage elements with one of these (comma separated) tags to download",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "downloadTargetSubflavor",
              isRequired = false,
              description = "Subflavor to use for distributed material",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "downloadTargetTags",
              isRequired = false,
              description = "Add tags (comma separated) to published media",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "streamingSourceFlavors",
              isRequired = false,
              description = "Specifies which media should be published to the streaming server",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "streamingSourceTags",
              isRequired = false,
              description = "Specifies which media should be published to the streaming server",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "streamingTargetSubflavor",
              isRequired = false,
              description = "Subflavor to use for distributed material",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "streamingTargetTags",
              isRequired = false,
              description = "Add tags (comma separated) to published media",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "mergeForceFlavors",
              isRequired = false,
              defaultValue = "dublincore/*,security/*",
              description = "Flavors of elements for which an update is enforced when merging catalogs",
              type = RestParameter.Type.STRING
          ),
          @RestParameter(
              name = "addForceFlavors",
              isRequired = false,
              description = "Works only if strategy 'merge' is used. Elements with these flavors will be added to an"
                  + "existing publication. No published elements will be deleted or overwritten.",
              type = RestParameter.Type.STRING
          )
      },
      responses = {
          @RestResponse(
              responseCode = HttpServletResponse.SC_OK,
              description = "An XML representation of the publication"
          )
      }
  )
  public Response publishSync(@FormParam("mediapackage") final String mediaPackageXml,
      @FormParam("checkAvailability") String checkAvailability, @FormParam("strategy") String strategy,
      @FormParam("downloadSourceFlavors") String downloadSourceFlavors,
      @FormParam("downloadSourceTags") String downloadSourceTags,
      @FormParam("downloadTargetSubflavor") String downloadTargetSubflavor,
      @FormParam("downloadTargetTags") String downloadTargetTags,
      @FormParam("streamingSourceFlavors") String streamingSourceFlavors,
      @FormParam("streamingSourceTags") String streamingSourceTags,
      @FormParam("streamingTargetSubflavor") String streamingTargetSubflavor,
      @FormParam("streamingTargetTags") String streamingTargetTags,
      @FormParam("mergeForceFlavors") String mergeForceFlavors, @FormParam("addForceFlavors") String addForceFlavors) {
    try {
      final MediaPackage mediaPackage = MediaPackageParser.getFromXml(mediaPackageXml);
      Publication publication = service.publishSync(mediaPackage, checkAvailability, strategy,
          downloadSourceFlavors, downloadSourceTags, downloadTargetSubflavor, downloadTargetTags,
          streamingSourceFlavors, streamingSourceTags, streamingTargetSubflavor, streamingTargetTags, mergeForceFlavors,
          addForceFlavors);
      return Response.ok(MediaPackageElementParser.getAsXml(publication)).build();
    } catch (Exception e) {
      logger.warn("Error publishing or retracting element", e);
      return Response.serverError().build();
    }
  }
}
