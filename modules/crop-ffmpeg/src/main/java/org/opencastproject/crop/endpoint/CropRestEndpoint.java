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

package org.opencastproject.crop.endpoint;

import org.opencastproject.crop.api.CropException;
import org.opencastproject.crop.api.CropService;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the {@link CropService} service.
 */
@Path("")
@RestService(
    name = "crop",
    title = "Video CROP Service",
    abstractText = "This service is not ready",
    notes = "This is a note"
)
public class CropRestEndpoint extends AbstractJobProducerEndpoint {
  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(CropRestEndpoint.class);

  /**
   * The rest docs
   */
  protected String docs;

  /**
   * The cropper
   */
  protected CropService cropService;

  /**
   * The service registry
   */
  protected ServiceRegistry serviceRegistry = null;

  /**
   * Callback from OSGi declarative services to set the service registry.
   *
   * @param serviceRegistry the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Sets the cropper.
   *
   * @param cropService the cropper
   */
  protected void setCropService(CropService cropService) {
    this.cropService = cropService;
  }

  @POST
  @Path("")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(
      name = "crop",
      description = "Submit a track for cropping",
      restParameters = {
          @RestParameter(
              description = "The track to crop.",
              isRequired = true,
              name = "track",
              type = RestParameter.Type.FILE
          )
      },
      responses = {
          @RestResponse(
              description = "The job ID to use when polling for the resulting mpeg7 catalog.",
              responseCode = HttpServletResponse.SC_OK
          ),
          @RestResponse(
              description = "The \"crop\" is NULL or not a valid track type.",
              responseCode = HttpServletResponse.SC_BAD_REQUEST
          ),
          @RestResponse(
              description = "The underlying service could not crop the video.",
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
          )
      },
      returnDescription = "The job ID to use when polling for the resulting mpeg7 catalog."
  )
  public Response crop(@FormParam("track") String trackAsXml) throws Exception {
    if (StringUtils.isBlank(trackAsXml)) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("track must not be null")
          .build();
    }

    MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(trackAsXml);
    if (!Track.TYPE.equals(sourceTrack.getElementType())) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("mediapackage element must be of type track")
          .build();
    }

    try {
      Job job = cropService.crop((Track) sourceTrack);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (CropException e) {
      logger.warn("cropping failed: :" + e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  /**
   * {@inheritDoc}
   *
   * @see AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    if (cropService instanceof JobProducer) {
      return (JobProducer) cropService;
    } else {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see AbstractJobProducerEndpoint#getServiceRegistry()
   */
  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }
}
