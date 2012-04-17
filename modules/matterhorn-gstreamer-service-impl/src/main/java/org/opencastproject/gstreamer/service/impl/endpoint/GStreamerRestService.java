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
package org.opencastproject.gstreamer.service.impl.endpoint;

import org.opencastproject.gstreamer.service.api.GStreamerService;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * A REST endpoint delegating functionality to the {@link GStreamerService}
 */
@Path("/")
@RestService(name = "gstreamerserviceimpl", title = "GStreamer Launcher", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is not working and "
                + "is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In other words, there is a bug! "
                + "You should file an error report with your server logs from the time when the error occurred: "
                + "<a href=\"https://issues.opencastproject.org\">Opencast Issue Tracker</a>" }, abstractText = "This service creates and augments Matterhorn media packages that include media tracks, metadata catalogs and "
        + "attachments.")
public class GStreamerRestService extends AbstractJobProducerEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(GStreamerRestService.class);

  /** The rest docs */
  protected String docs;

  /** The base server URL */
  protected String serverUrl;

  /** The gstreamer service */
  protected GStreamerService gstreamerService;

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
  
  @Override
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Sets the gstreamer service
   * 
   * @param gstreamerService
   *          the gstreamer service
   */
  public void setGStreamerService(GStreamerService gstreamerService) {
    this.gstreamerService = gstreamerService;
  }

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
    if (cc == null || cc.getBundleContext().getProperty("org.opencastproject.server.url") == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      serverUrl = cc.getBundleContext().getProperty("org.opencastproject.server.url");
    }
  }

  /**
   * Launches a gstreamer pipeline
   * 
   * @param mediapackage
   *          The mediapackage to draw track locations from. 
   * @param launch
   *          The launch command to run after substitutions have been made.
   * @param outputFiles
   *          The outputFiles to substitute into the launch command.  
   * @return A response containing the job for this encoding job in the response body.
   * @throws Exception
   */
  @POST
  @Path("launch")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "launch", description = "Launches a gstreamer command on files within a media package.", pathParameters = { }, restParameters = {
          @RestParameter(description = "The media package that you want to process.", isRequired = true, name = "mediapackage", type = Type.STRING),
          @RestParameter(description = "The gstreamer command to execute using gstreamer launch like structure.", isRequired = true, name = "launch", type = Type.STRING),
          @RestParameter(description = "The list of files to add to the media package. Currently only supports one.", isRequired = true, name = "outputFiles", type = Type.STRING) }, reponses = { @RestResponse(description = "Results in an xml document containing the job for the encoding task", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response launch(@FormParam("mediapackage") String mediaPackageXml, @FormParam("launch") String launch, @FormParam("outputFiles") String outputFiles)
          throws Exception {
	  logger.debug("Remote call for mediapackage " + mediaPackageXml + " with launch string " + launch + " and output files " + outputFiles);
	  Job job = null;
    try {
      MediaPackage mediapackage = MediaPackageParser.getFromXml(mediaPackageXml);
      job = gstreamerService.launch(mediapackage, launch, outputFiles);
    } catch (Exception e) {
      logger.warn("Error distributing element", e);
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
    if (gstreamerService instanceof JobProducer)
      return (JobProducer) gstreamerService;
    else
      return null;
  }
}
