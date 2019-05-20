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

package org.opencastproject.ingestdownloadservice.impl.endpoint;

import org.opencastproject.ingestdownloadservice.api.IngestDownloadService;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

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
 * The REST endpoint for the {@link IngestDownloadService} service
 */
@Path("/")
@RestService(name = "IngestDownloadServiceEndpoint",
    title = "Ingest download REST endpoint",
    abstractText = "The REST endpoint for the ingest download service.",
    notes = {})
public class IngestDownloadServiceEndpoint extends AbstractJobProducerEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(IngestDownloadServiceEndpoint.class);

  /** The service */
  private IngestDownloadService service;
  private ServiceRegistry serviceRegistry;

  @POST
  @Path("ingestdownload")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "ingestdownload",description = "Downloads mediapackage elements to workspace",
          restParameters = { @RestParameter(description = "mediapackage as xml", isRequired = true, name = "mediapackage",
                  type = RestParameter.Type.TEXT),
          @RestParameter(description = "sourceFlavors as String seperated by , (presenter/source,presentation/source) ", isRequired = false, name = "sourceFlavors",
          type = RestParameter.Type.STRING),
          @RestParameter(description = "sourceTags as String seperated by , (archive,download)", isRequired = false, name = "sourceTags",
          type = RestParameter.Type.STRING),
          @RestParameter(description = "delete from external workingfile repository: Boolean true / false", isRequired = false, name = "deleteExternal",
          type = RestParameter.Type.STRING),
          @RestParameter(description = "select both tags and flavors: Boolean true / false", isRequired = false, name = "tagsAndFlavor",
          type = RestParameter.Type.STRING),
          },
      reponses =  {@RestResponse(description = "Mediapackage as xml", responseCode = HttpServletResponse.SC_OK)},
          returnDescription = "Mediapackage as xml with element urls in workspace.")
  public Response ingestdownload(@FormParam("mediapackage") String mediapackageString,
          @FormParam("sourceFlavors") String sourceFlavors,
          @FormParam("sourceTags") String sourceTags,
          @FormParam("deleteExternal") String deleteExternal,
          @FormParam("tagsAndFlavor") String tagsAndFlavor) throws Exception {
    logger.info("starting ingest-download Service");
    MediaPackage mediapackage = MediaPackageParser.getFromXml(mediapackageString);
    boolean boolTagsAndFlavor = false;
    boolean boolDeleteExternal = false;
    //set Defaults
    if (sourceFlavors.isEmpty()) { sourceFlavors = "*/*"; }
    if (!tagsAndFlavor.isEmpty()) { boolTagsAndFlavor = Boolean.parseBoolean(tagsAndFlavor); }
    if (!deleteExternal.isEmpty()) { boolDeleteExternal = Boolean.parseBoolean(deleteExternal); }

    try {
      final Job retJob = service.ingestDownload(mediapackage ,sourceFlavors,sourceTags,boolDeleteExternal,
              boolTagsAndFlavor);
      return Response.ok().entity(new JaxbJob(retJob)).build();
    } catch (ServiceRegistryException e) {
      logger.warn("Unable to start IngestDownload: ", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }


  public void setIngestDownloadService(IngestDownloadService service) {
    this.service = service;
  }

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
   * Callback from the OSGi declarative services to set the service registry.
   *
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }
}
