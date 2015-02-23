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
package org.opencastproject.helloworld.impl.endpoint;

import org.opencastproject.helloworld.api.HelloWorldService;

import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

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
import org.opencastproject.helloworld.impl.HelloWorldServiceImpl;

/**
 * The REST endpoint for the {@link VideoSegmenterService} service
 */
@Path("")
@RestService(name = "helloworld", title = "Hello World Service", abstractText = "This is a tutorial service.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class HelloWorldRestEndpoint {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(HelloWorldRestEndpoint.class);

  /** The rest docs */
  protected String docs;

  /** The video segmenter */
  protected HelloWorldService service;


  /**
   * Segments a track.
   *
   * @param name
   *          the optional name of the Person to greet
   * @return The Hello World statement
   * @throws Exception
   */
  @POST
  @Path("")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "helloworld", description = "Simple first module", restParameters = { @RestParameter(description = "name to output", isRequired = false, name = "name", type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(description = "Hello World or Hello, <Name>", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The underlying service could not output something.", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "The text that the service returns.")
  public Response helloWorld(@FormParam("name") String name) throws Exception {
    // Ensure that the POST parameters are present

    service = new HelloWorldServiceImpl();
    logger.info("REST call for Hello World");
    return Response.ok().entity(service.helloWorld(name)).build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

}
