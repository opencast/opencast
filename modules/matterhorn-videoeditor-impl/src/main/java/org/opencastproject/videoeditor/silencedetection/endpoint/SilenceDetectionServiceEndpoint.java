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
package org.opencastproject.videoeditor.silencedetection.endpoint;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.videoeditor.silencedetection.api.SilenceDetectionService;

/**
 * SilenceDetectionService REST Endpoint.
 */
@Path("/")
@RestService(name = "SilenceDetectionServiceEndpoint", title = "Silence Detection Service REST Endpoint",
        abstractText = "Detect silent sequences in audio file.",
        notes = {"All paths above are relative to the REST endpoint base (something like http://your.server/silencedetection)"})
public class SilenceDetectionServiceEndpoint extends AbstractJobProducerEndpoint {

  private SilenceDetectionService silenceDetectionService;
  private ServiceRegistry serviceRegistry;

  @POST
  @Path("/detect")
  @Produces({MediaType.APPLICATION_XML})
  @RestQuery(name = "detect", description = "Create silence detection job.",
          returnDescription = "Silence detection job.",
          restParameters = {
            @RestParameter(name = "track", type = RestParameter.Type.TEXT,
                    description = "Track where to run silence detection.", isRequired = true)
          },
          reponses = {
            @RestResponse(description = "Silence detection job created successfully.", responseCode = HttpServletResponse.SC_OK),
            @RestResponse(description = "Create silence detection job failed.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
          })
  public Response detect(@FormParam("track") String trackXml) {
    try {
      Track track = (Track) MediaPackageElementParser.getFromXml(trackXml);
      Job job = silenceDetectionService.detect(track);
      return Response.ok(new JaxbJob(job)).build();
    } catch (Exception ex) {
      return Response.serverError().entity(ex.getMessage()).build();
    }
  }

  @Override
  public JobProducer getService() {
    if (silenceDetectionService instanceof JobProducer) {
      return (JobProducer) silenceDetectionService;
    } else {
      return null;
    }
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public void setSilenceDetectionService(SilenceDetectionService silenceDetectionService) {
    this.silenceDetectionService = silenceDetectionService;
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }
}
