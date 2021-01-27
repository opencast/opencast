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

package org.opencastproject.helloworld.impl.endpoint;

import org.opencastproject.helloworld.api.HelloWorldService;
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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the {@link HelloWorldService} service
 */
@Component(
  property = {
    "service.description=Hello World REST Endpoint",
    "opencast.service.type=org.opencastproject.helloworld",
    "opencast.service.path=/helloworld",
    "opencast.service.jobproducer=false"
  },
  immediate = true,
  service = HelloWorldRestEndpoint.class
)
@Path("/")
@RestService(name = "HelloWorldServiceEndpoint",
    title = "Hello World Service Endpoint",
    abstractText = "This is a tutorial service.",
    notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated."
                + "In other words, there is a bug! You should file an error report with your server logs from the time"
                + "when the error occurred: "
                + "<a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>"})
public class HelloWorldRestEndpoint {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(HelloWorldRestEndpoint.class);

  /** The rest docs */
  protected String docs;

  /** The service */
  protected HelloWorldService helloWorldService;

  /**
   * Simple example service call
   *
   * @return The Hello World statement
   * @throws Exception
   */
  @GET
  @Path("helloworld")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "helloworld", description = "example service call",
      responses = {@RestResponse(description = "Hello World", responseCode = HttpServletResponse.SC_OK),
        @RestResponse(description = "The underlying service could not output something.",
            responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
      returnDescription = "The text that the service returns.")
  public Response helloWorld() throws Exception {
    logger.info("REST call for Hello World");
    return Response.ok().entity(helloWorldService.helloWorld()).build();
  }

  /**
   * Simple example service call with parameter
   *
   * @param name
   *          the optional name of the Person to greet
   * @return A Hello statement with optional name
   * @throws Exception
   */
  @GET
  @Path("helloname")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "helloname", description = "example service call with parameter",
      restParameters = { @RestParameter(description = "name to output", isRequired = false, name = "name",
          type = RestParameter.Type.TEXT) },
      responses = {@RestResponse(description = "Hello or Hello Name", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The underlying service could not output something.",
              responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) },
      returnDescription = "The text that the service returns.")
  public Response helloName(@FormParam("name") String name) throws Exception {
    logger.info("REST call for Hello Name");
    return Response.ok().entity(helloWorldService.helloName(name)).build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public String getDocs() {
    return docs;
  }

  @Reference(name = "helloworld-service")
  public void setHelloWorldService(HelloWorldService service) {
    this.helloWorldService = service;
  }
}
