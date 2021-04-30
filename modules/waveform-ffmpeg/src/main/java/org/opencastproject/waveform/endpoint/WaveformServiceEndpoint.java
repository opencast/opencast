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
package org.opencastproject.waveform.endpoint;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.waveform.api.WaveformService;
import org.opencastproject.waveform.api.WaveformServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(
    name = "WaveformServiceEndpoint",
    title = "Waveform Service REST Endpoint",
    abstractText = "The Waveform Service generates a waveform image from a media file with at least one audio channel.",
    notes = { "All paths above are relative to the REST endpoint base (something like http://your.server/waveform)" }
)
public class WaveformServiceEndpoint extends AbstractJobProducerEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(WaveformServiceEndpoint.class);

  private ServiceRegistry serviceRegistry = null;
  private WaveformService waveformService = null;

  @POST
  @Path("/create")
  @Produces({MediaType.APPLICATION_XML})
  @RestQuery(name = "create", description = "Create a waveform image from the given track",
          returnDescription = "Media package attachment for the generated waveform.",
          restParameters = {
            @RestParameter(name = "track", type = RestParameter.Type.TEXT,
                    description = "Track with at least one audio channel.", isRequired = true),
            @RestParameter(name = "pixelsPerMinute", type = RestParameter.Type.INTEGER,
                    description = "Width of waveform image in pixels per minute.", isRequired = true),
            @RestParameter(name = "minWidth", type = RestParameter.Type.INTEGER,
                    description = "Minimum width of waveform image.", isRequired = true),
            @RestParameter(name = "maxWidth", type = RestParameter.Type.INTEGER,
                    description = "Maximum width of waveform image.", isRequired = true),
            @RestParameter(name = "height", type = RestParameter.Type.INTEGER,
                    description = "Height of waveform image.", isRequired = true),
            @RestParameter(name = "color", type = RestParameter.Type.STRING, defaultValue = "black",
                    description = "Color of waveform image.", isRequired = true)
          },
          responses = {
            @RestResponse(description = "Waveform generation job successfully created.",
                    responseCode = HttpServletResponse.SC_OK),
            @RestResponse(description = "The given track can't be parsed.",
                    responseCode = HttpServletResponse.SC_BAD_REQUEST),
            @RestResponse(description = "Internal server error.",
                    responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
  })
  public Response createWaveformImage(@FormParam("track") String track,
      @FormParam("pixelsPerMinute") int pixelsPerMinute, @FormParam("minWidth") int minWidth,
      @FormParam("maxWidth") int maxWidth, @FormParam("height") int height, @FormParam("color") String color) {
    try {
      MediaPackageElement sourceTrack = MediaPackageElementParser.getFromXml(track);
      if (!Track.TYPE.equals(sourceTrack.getElementType())) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Track element must be of type track").build();
      }

      Job job = waveformService.createWaveformImage(
          (Track) sourceTrack, pixelsPerMinute, minWidth, maxWidth, height, color);
      return Response.ok().entity(new JaxbJob(job)).build();
    } catch (WaveformServiceException ex) {
      logger.error("Creating waveform job for track {} failed:", track, ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } catch (MediaPackageException ex) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Track element parsing failure").build();
    }
  }

  @Override
  public JobProducer getService() {
    if (waveformService instanceof JobProducer) {
      return (JobProducer) waveformService;
    } else {
      return null;
    }
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  public void setWaveformService(WaveformService waveformService) {
    this.waveformService = waveformService;
  }
}
