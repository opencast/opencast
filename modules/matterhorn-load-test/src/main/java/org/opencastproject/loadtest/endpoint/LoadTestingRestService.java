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
package org.opencastproject.loadtest.endpoint;

import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.loadtest.impl.LoadTestFactory;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The REST endpoint for the load testing service.
 */
@Path("/")
@RestService(name = "loadtesting", title = "Load Testing",
  abstractText = "This service creates and augments Matterhorn media packages that include media tracks, metadata "
               + "catalogs and attachments.",
  notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
        + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class LoadTestingRestService {
  private static final Logger logger = LoggerFactory.getLogger(LoadTestingRestService.class);

  private LoadTestFactory loadTestFactory;

  protected String docs = null;

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
  }

  /**
   * Set {@link org.opencastproject.loadtest.impl.LoadTestFactory} service.
   * 
   * @param service
   *          Service implemented {@link org.opencastproject.loadtest.impl.LoadTestFactory}
   */
  public void setService(LoadTestFactory loadTestFactory) {
    this.loadTestFactory = loadTestFactory;
  }

  /**
   * Unset {@link org.opencastproject.loadtest.impl.LoadTestFactory} service.
   * 
   * @param service
   *          Service implemented {@link org.opencastproject.loadtest.impl.LoadTestFactory}
   */
  public void unsetService(LoadTestFactory loadTestFactory) {
    this.loadTestFactory = null;
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("configuration.xml")
  @RestQuery(name = "config", description = "Returns a list with the default load test configuration properties.  This can be used for the startLoadTesting endpoint.", pathParameters = { }, restParameters = { }, reponses = {
          @RestResponse(description = "the configuration values are returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "the configuration properties could not be retrieved", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR),
          @RestResponse(description = "Load Test Service is unavailable", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getConfiguration() {
    if (loadTestFactory == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).entity(
              "Load Test Service is unavailable, please wait...").build();
    }

    try {
      Properties properties = loadTestFactory.getProperties();
      logger.info("Properties: " + properties.toString());
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      String returnConfiguration = "";
      properties.storeToXML(baos, "Load Test Configuration");
      returnConfiguration += baos.toString();
      return Response.ok(returnConfiguration).type(MediaType.TEXT_XML).build();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity(
              "Exception while trying to obtain metadata: " + e.getMessage() + ".").build();
    }
  }
  
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @Path("startLoadTesting")
  @RestQuery(name = "startLoadTesting", description = "Starts loadTesting with the supplied properties", pathParameters = { }, restParameters = { @RestParameter(description = "The properties to set for this recording. "
          + "Those are specified in key-value pairs as described in "
          + "<a href=\"http://download.oracle.com/javase/6/docs/api/java/util/Properties.html#loadFromXML%28java.io.InputStream%29\"> "
          + "this JavaDoc</a>. The current default properties can be found at "
          + "<a href=\"http://opencast.jira.com/svn/MH/trunk/docs/felix/conf/"
          + "services/org.opencastproject.loadtest.impl.LoadTesting.properties\"> " + "this location</a>", isRequired = false, name = "config", type = TEXT) }, reponses = {
          @RestResponse(description = "valid request, results returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "couldn't start load testing with default parameters", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR) }, returnDescription = "If the load testing has begun.")
  public Response startLoadTesting(@FormParam("config") String propertiesString) {
    if (loadTestFactory == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE).entity(
              "Load Testing is unavailable, please wait...").build();
    }

    Properties properties = new Properties();
    try {
      properties.loadFromXML(new ByteArrayInputStream(propertiesString.getBytes()));
    } catch (IOException e1) {
      logger.warn("Unable to parse configuration string into valid load testing config. Continuing with default settings.");
    }

    try {
      loadTestFactory.startLoadTesting(properties);
      return Response.ok("Started load testing ").build();
    } catch (Exception e) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity(
              "Exception while trying to start load testing: " + e.getMessage() + ".").build();
    }
  }
}
