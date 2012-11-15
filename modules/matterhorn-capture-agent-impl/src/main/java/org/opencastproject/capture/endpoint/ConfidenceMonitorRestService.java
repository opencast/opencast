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
package org.opencastproject.capture.endpoint;

import org.opencastproject.capture.api.ConfidenceMonitor;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

@Path("/")
@RestService(name = "confidence", title = "Confidence Monitoring Service",
  abstractText = "This service creates and augments Matterhorn media packages that include media tracks, metadata "
               + "catalogs and attachments.",
  notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
        + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
        + "other words, there is a bug! You should file an error report with your server logs from the time when the "
        + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>" })
public class ConfidenceMonitorRestService {

  private static final Logger logger = LoggerFactory.getLogger(ConfidenceMonitorRestService.class);

  private ConfidenceMonitor service;

  protected String docs;

  /**
   * Callback from OSGi that is called when this service is activated.
   * 
   * @param cc
   *          OSGi component context
   */
  public void activate(ComponentContext cc) {
  }

  
  /**
   * OSGI activate method. Will be called on service activation.
   */
  public void activate() {
    logger.info("Video Monitoring Service Activated");
  }

  /**
   * Set {@link org.opencastproject.capture.api.ConfidenceMonitor} service.
   * 
   * @param service
   *          Service implemented {@link org.opencastproject.capture.api.ConfidenceMonitor}
   */
  public void setService(ConfidenceMonitor service) {
    this.service = service;
  }

  /**
   * Unset {@link org.opencastproject.capture.api.ConfidenceMonitor} service.
   * 
   * @param service
   *          Service implemented {@link org.opencastproject.capture.api.ConfidenceMonitor}
   */
  public void unsetService(ConfidenceMonitor service) {
    this.service = null;
  }

  /**
   * Gets the most recent frame from the monitoring service
   * 
   * @param device
   *          The name of the device from which you want a frame
   * @return The image as a image/jpeg
   */
  @GET
  @Produces("image/jpeg")
  @Path("{name}")
  @RestQuery(name = "grabFrame", description = "Loads a JPEG image from the device specified", pathParameters = { @RestParameter(description = "The device to grab a frame from", isRequired = true, name = "name", type = RestParameter.Type.STRING) } , restParameters = { } , reponses = { @RestResponse(description = "OK, valid request, results returned", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response grabFrame(@PathParam("name") String device) {
    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Confidence monitor unavailable, please wait...").build();
    }

    CacheControl cc = new CacheControl();
    cc.setNoCache(true);
    try {
      return Response.ok(service.grabFrame(device)).cacheControl(cc).build();
    } catch (Exception ex) {
      return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Can not get frame from resource").build();
    }
  }

  /**
   * Gets a list of available devices from the monitoring service
   * 
   * @return The list of devices in XML format
   */
  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("devices")
  @RestQuery(name = "getDevices", description = "Lists devices accessible on capture agent", pathParameters = { }, restParameters = { }, reponses = {
          @RestResponse(description = "OK, valid request, results returned", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "Confidence monitor unavailable", responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE) }, returnDescription = "")
  public Response getDevices() {

    if (service == null) {
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Confidence monitor unavailable, please wait...").build();
    }

    List<String> names = service.getFriendlyNames();
    try {
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      docBuilderFactory.setNamespaceAware(true);
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document document = docBuilder.newDocument();

      Element devices = document.createElement("agent-devices");
      for (String name : names) {
        String[] nameType = name.split(",");
        AgentDevice device = new AgentDevice(nameType[0], nameType[1]);
        Node deviceNode = device.toXml(document);
        if (deviceNode != null)
          devices.appendChild(deviceNode);
      }
      document.appendChild(devices);

      DOMSource ds = new DOMSource(document);
      StreamResult result = new StreamResult(new StringWriter());
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.transform(ds, result);
      return Response.ok(result.getWriter().toString()).build();

    } catch (TransformerException ex) {
      logger.error("", ex);
      return Response.serverError().entity(ex.getMessage()).build();
    } catch (ParserConfigurationException ex) {
      logger.error("", ex);
      return Response.serverError().entity(ex.getMessage()).build();
    }
  }

  /**
   * Returns the RMS values for device after a given Unix timestamp.
   * 
   * @param device
   *          Friendly name of the audio device
   * @param timestamp
   *          A Unix timestamp (set to 0 to get all values stored)
   * @return application/json with keys start, interval and samples
   */
  @SuppressWarnings("unchecked")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("audio/{name}/{timestamp}")
  @RestQuery(name = "getRMSValues", description = "Retrieve all RMS data for device {name} after Unix time {timestamp}", pathParameters = {
          @RestParameter(description = "The device to get RMS values from", isRequired = true, name = "name", type = RestParameter.Type.STRING),
          @RestParameter(description = "The timestamp to start getting RMS values from", isRequired = true, name = "timestamp", type = RestParameter.Type.STRING) }, restParameters = { }, reponses = { @RestResponse(description = "OK, valid request, results returned", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "")
  public Response getRMSValues(@PathParam("name") String device, @PathParam("timestamp") double timestamp) {
    JSONObject jsonOutput = new JSONObject();
    if (service == null) {
      // Error code 500 -> 503
      return Response.serverError().status(Response.Status.SERVICE_UNAVAILABLE)
              .entity("Confidence monitor unavailable, please wait...").build();
    }
    // Attempt to grab audio information, if exception is thrown the device does not exist
    try {
      List<Double> rmsValues = service.getRMSValues(device, timestamp);
      Collections.reverse(rmsValues);
      for (int i = 0; i < rmsValues.size(); i++) {
        double valueDB = rmsValues.get(i);
        double rms = Math.pow(10, valueDB / 20);
        rms = Math.round(rms * 100.00) / 100.00;
        rmsValues.set(i, rms);
      }
      jsonOutput.put("start", timestamp);
      jsonOutput.put("interval", "100");
      jsonOutput.put("samples", rmsValues);
      return Response.ok(jsonOutput.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
    } catch (NullPointerException e) {
      return Response.ok("Device " + device + " does not exist.").build();
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("core/url")
  public Response getCoreUrl() {
    JSONObject json = new JSONObject();
    json.put("url", service.getCoreUrl());
    return Response.ok(json.toJSONString()).build();
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("docs")
  public Response getDocumentation() {
    return Response.ok(docs).build();
  }

}
