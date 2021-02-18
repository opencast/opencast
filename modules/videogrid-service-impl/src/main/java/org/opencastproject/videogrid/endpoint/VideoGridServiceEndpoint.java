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

package org.opencastproject.videogrid.endpoint;

import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

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
import org.opencastproject.videogrid.api.VideoGridService;
import org.opencastproject.videogrid.api.VideoGridServiceException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.codec.EncoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A service endpoint to expose the {@link VideoGridService} via REST.
 */
@Path("/")
@RestService(
        name = "videogrid",
        title = "VideoGrid Service",
        abstractText = "The Video Grid Service offers a way to combine several, partially simultaneously\n"
        + "playing videos into a single video file.",
        notes = { "Does NOT return the URI to a single video file, but instead returns URIs to several"
                + "videos which each make up a part of the final video." })
public class VideoGridServiceEndpoint extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(VideoGridServiceEndpoint.class);

  /** The videogrid service */
  private VideoGridService videoGridService;

  /** The service registry */
  private ServiceRegistry serviceRegistry = null;

  /** For JSON serialization */
  private static final Type stringListOfListType = new TypeToken<List<List<String>>>() { }.getType();

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
   * Sets the videoGridService
   *
   * @param videoGridService
   *          the videoGridService
   */
  public void setVideoGridService(VideoGridService videoGridService) {
    this.videoGridService = videoGridService;
  }

  @POST
  @Produces(MediaType.TEXT_XML)
  @Path("videogrid")
  @RestQuery(name = "videogrid", description = "Create video grid",
          restParameters = {
                  @RestParameter(name = "commands", isRequired = true, type = STRING,
                          description = "A list of ffmpeg commands, one for each part."),
                  @RestParameter(name = "tracks", isRequired = true, type = RestParameter.Type.TEXT,
                          description = "The source tracks to concat as XML") },
          responses = {
                  @RestResponse(description = "VideoGrid created successfully", responseCode = HttpServletResponse.SC_OK),
                  @RestResponse(description = "Invalid data", responseCode = HttpServletResponse.SC_BAD_REQUEST),
                  @RestResponse(description = "Internal error", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
          returnDescription = "Returns the paths to the generated videos for the grid")
  public Response createPartialTracks(
          @FormParam("commands") String commandsString,
          @FormParam("sourceTracks") String tracksXml) throws MediaPackageException, EncoderException {
    Gson gson = new Gson();
    try {
      List<List<String>> commands = gson.fromJson(commandsString, stringListOfListType);
      List<? extends MediaPackageElement> tracks = MediaPackageElementParser.getArrayFromXml(tracksXml);
      logger.debug("Start videogrid");
      Job job = videoGridService.createPartialTracks(commands, tracks.toArray(new Track[tracks.size()]));
      return Response.ok(new JaxbJob(job)).build();
    } catch (JsonSyntaxException | NullPointerException e) {
      logger.debug("Invalid data passed to REST endpoint:\ncommands: {}", commandsString);
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (VideoGridServiceException e) {
      logger.error("Error generating videos {}", commandsString, e);
      return Response.serverError().build();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    if (videoGridService instanceof JobProducer) {
      logger.debug("get videogrid service");
      return (JobProducer) videoGridService;
    }
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
