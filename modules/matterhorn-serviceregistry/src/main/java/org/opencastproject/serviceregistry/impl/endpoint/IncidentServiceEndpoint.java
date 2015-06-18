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

package org.opencastproject.serviceregistry.impl.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.opencastproject.util.EqualsUtil.eq;
import static org.opencastproject.util.RestUtil.R.badRequest;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.getResponseType;

import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.job.api.IncidentTree;
import org.opencastproject.job.api.JaxbIncident;
import org.opencastproject.job.api.JaxbIncidentDigestList;
import org.opencastproject.job.api.JaxbIncidentFullList;
import org.opencastproject.job.api.JaxbIncidentFullTree;
import org.opencastproject.job.api.JaxbIncidentList;
import org.opencastproject.job.api.JaxbIncidentTree;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.serviceregistry.api.IncidentL10n;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.IncidentServiceException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.LocalHashMap;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/** REST endpoint for Incident Service. */
@Path("/")
@RestService(name = "incidentservice", title = "Incident Service", abstractText = "This service creates, edits and retrieves and helps managing incidents.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>"})
public class IncidentServiceEndpoint {
  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(IncidentServiceEndpoint.class);

  public static final String FMT_FULL = "full";
  public static final String FMT_DIGEST = "digest";
  public static final String FMT_SYS = "sys";
  private static final String FMT_DEFAULT = FMT_SYS;

  /** The incident service */
  protected IncidentService svc;

  /** The remote service manager */
  protected ServiceRegistry serviceRegistry = null;

  /** This server's base URL */
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;

  /** The REST endpoint's base URL */
  protected String serviceUrl = "/incidents";

  /** OSGi callback for setting the incident service. */
  public void setIncidentService(IncidentService incidentService) {
    this.svc = incidentService;
  }

  /**
   * The method that is called, when the service is activated
   *
   * @param cc
   *         The ComponentContext of this service
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc != null) {
      String ccServerUrl = cc.getBundleContext().getProperty(MatterhornConstants.SERVER_URL_PROPERTY);
      if (StringUtils.isNotBlank(ccServerUrl))
        serverUrl = ccServerUrl;
      serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Path("job/incidents.{type:xml|json}")
  @RestQuery(name = "incidentsofjobaslist",
             description = "Returns the job incidents with the given identifiers.",
             returnDescription = "Returns the job incidents.",
             pathParameters = {
                     @RestParameter(name = "type", isRequired = true,
                                    description = "The media type of the response [xml|json]",
                                    defaultValue = "xml",
                                    type = Type.STRING)},
             restParameters = {
                     @RestParameter(name = "id", isRequired = true, description = "The job identifiers.", type = Type.INTEGER),
                     @RestParameter(name = "format", isRequired = false,
                                    description = "The response format [full|digest|sys]. Defaults to sys",
                                    defaultValue = "sys",
                                    type = Type.STRING)},
             reponses = {
                     @RestResponse(responseCode = SC_OK, description = "The job incidents.")})
  public Response getIncidentsOfJobAsList(
          @Context HttpServletRequest request,
          @QueryParam("id") final List<Long> jobIds,
          @QueryParam("format") @DefaultValue(FMT_DEFAULT) final String format,
          @PathParam("type") final String type) {
    try {
      final List<Incident> incidents = svc.getIncidentsOfJob(jobIds);
      final MediaType mt = getResponseType(type);
      if (eq(FMT_SYS, format)) {
        return ok(mt, new JaxbIncidentList(incidents));
      } else if (eq(FMT_DIGEST, format)) {
        return ok(mt, new JaxbIncidentDigestList(svc, request.getLocale(), incidents));
      } else if (eq(FMT_FULL, format)) {
        return ok(mt, new JaxbIncidentFullList(svc, request.getLocale(), incidents));
      } else {
        return unknownFormat();
      }
    } catch (NotFoundException e) {
      // should not happen
      logger.error("Unable to get job incident for id {}! Consistency issue!");
      throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
    } catch (IncidentServiceException e) {
      logger.warn("Unable to get job incident for id {}: {}", jobIds, e.getMessage());
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Path("job/{id}.{type:xml|json}")
  @RestQuery(name = "incidentsofjobastree",
             description = "Returns the job incident for the job with the given identifier.",
             returnDescription = "Returns the job incident.",
             pathParameters = {
                     @RestParameter(name = "id", isRequired = true, description = "The job identifier.", type = Type.INTEGER),
                     @RestParameter(name = "type", isRequired = true,
                                    description = "The media type of the response [xml|json]",
                                    defaultValue = "xml",
                                    type = Type.STRING)},
             restParameters = {
                     @RestParameter(name = "cascade", isRequired = false, description = "Whether to show the cascaded incidents.",
                                    type = Type.BOOLEAN, defaultValue = "false"),
                     @RestParameter(name = "format", isRequired = false,
                                    description = "The response format [full|digest|sys]. Defaults to sys",
                                    defaultValue = "sys",
                                    type = Type.STRING)},
             reponses = {
                     @RestResponse(responseCode = SC_OK, description = "The job incident."),
                     @RestResponse(responseCode = SC_NOT_FOUND, description = "No job incident with this identifier was found.")})
  public Response getIncidentsOfJobAsTree(
          @Context HttpServletRequest request,
          @PathParam("id") final long jobId,
          @QueryParam("cascade") @DefaultValue("false") boolean cascade,
          @QueryParam("format") @DefaultValue(FMT_DEFAULT) final String format,
          @PathParam("type") final String type)
          throws NotFoundException {
    try {
      final IncidentTree tree = svc.getIncidentsOfJob(jobId, cascade);
      final MediaType mt = getResponseType(type);
      if (eq(FMT_SYS, format)) {
        return ok(mt, new JaxbIncidentTree(tree));
      } else if (eq(FMT_DIGEST, format)) {
        return ok(mt, new JaxbIncidentFullTree(svc, request.getLocale(), tree));
      } else if (eq(FMT_FULL, format)) {
        return ok(mt, new JaxbIncidentFullTree(svc, request.getLocale(), tree));
      } else {
        return unknownFormat();
      }
    } catch (IncidentServiceException e) {
      logger.warn("Unable to get job incident for id {}: {}", jobId, e.getMessage());
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Path("{id}.{type:xml|json}")
  @RestQuery(name = "incidentjson",
             description = "Returns the job incident by it's incident identifier.",
             returnDescription = "Returns the job incident.",
             pathParameters = {
                     @RestParameter(name = "id", isRequired = true, description = "The incident identifier.", type = Type.INTEGER),
                     @RestParameter(name = "type", isRequired = true,
                                    description = "The media type of the response [xml|json]",
                                    defaultValue = "xml",
                                    type = Type.STRING)},
             reponses = {
                     @RestResponse(responseCode = SC_OK, description = "The job incident."),
                     @RestResponse(responseCode = SC_NOT_FOUND, description = "No job incident with this identifier was found.")})
  public Response getIncident(@PathParam("id") final long incidentId, @PathParam("type") final String type)
          throws NotFoundException {
    try {
      Incident incident = svc.getIncident(incidentId);
      return ok(getResponseType(type), new JaxbIncident(incident));
    } catch (IncidentServiceException e) {
      logger.warn("Unable to get job incident for incident id {}: {}", incidentId, e);
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @SuppressWarnings("unchecked")
  @Produces(MediaType.APPLICATION_JSON)
  @Path("localization/{id}")
  @RestQuery(name = "getlocalization",
             description = "Returns the localization of an incident by it's id as JSON",
             returnDescription = "The localization of the incident as JSON",
             pathParameters = {
                     @RestParameter(name = "id", isRequired = true, description = "The incident identifiers.", type = Type.INTEGER)},
             restParameters = {
                     @RestParameter(name = "locale", isRequired = true, description = "The locale.", type = Type.STRING)},
             reponses = {
                     @RestResponse(responseCode = SC_OK, description = "The localization of the given job incidents."),
                     @RestResponse(responseCode = SC_NOT_FOUND, description = "No job incident with this incident identifier was found.")})
  public Response getLocalization(@PathParam("id") final long incidentId, @QueryParam("locale") String locale)
          throws NotFoundException {
    try {
      IncidentL10n localization = svc.getLocalization(incidentId, LocaleUtils.toLocale(locale));
      JSONObject json = new JSONObject();
      json.put("title", localization.getTitle());
      json.put("description", localization.getDescription());
      return Response.ok(json.toJSONString()).build();
    } catch (IncidentServiceException e) {
      logger.warn("Unable to get job localization of jo incident: {}", e);
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_XML)
  @Path("/")
  @RestQuery(name = "postincident", description = "Creates a new job incident and returns it as XML", returnDescription = "Returns the created job incident as XML", restParameters = {
          @RestParameter(name = "job", isRequired = true, description = "The job on where to create the incident", type = Type.TEXT),
          @RestParameter(name = "date", isRequired = true, description = "The incident creation date", type = Type.STRING),
          @RestParameter(name = "code", isRequired = true, description = "The incident error code", type = Type.STRING),
          @RestParameter(name = "severity", isRequired = true, description = "The incident error code", type = Type.STRING),
          @RestParameter(name = "details", isRequired = false, description = "The incident details", type = Type.TEXT),
          @RestParameter(name = "params", isRequired = false, description = "The incident parameters", type = Type.TEXT)}, reponses = {
          @RestResponse(responseCode = SC_CREATED, description = "New job incident has been created"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the one of the form params"),
          @RestResponse(responseCode = SC_CONFLICT, description = "No job incident related job exists")})
  public Response postIncident(@FormParam("job") String jobXml, @FormParam("date") String date,
                               @FormParam("code") String code, @FormParam("severity") String severityString,
                               @FormParam("details") String details, @FormParam("params") LocalHashMap params) {
    Job job;
    Date timestamp;
    Severity severity;
    Map<String, String> map = new HashMap<String, String>();
    List<Tuple<String, String>> list = new ArrayList<Tuple<String, String>>();
    try {
      job = JobParser.parseJob(jobXml);
      timestamp = new Date(DateTimeSupport.fromUTC(date));
      severity = Severity.valueOf(severityString);
      if (params != null)
        map = params.getMap();

      if (StringUtils.isNotBlank(details)) {
        final JSONArray array = (JSONArray) JSONValue.parse(details);
        for (int i = 0; i < array.size(); i++) {
          JSONObject tuple = (JSONObject) array.get(i);
          list.add(Tuple.tuple((String) tuple.get("title"), (String) tuple.get("content")));
        }
      }
    } catch (Exception e) {
      return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
    }

    try {
      Incident incident = svc.storeIncident(job, timestamp, code, severity, map, list);
      String uri = UrlSupport.concat(serverUrl, serviceUrl, Long.toString(incident.getId()), ".xml");
      return Response.created(new URI(uri)).entity(new JaxbIncident(incident)).build();
    } catch (IllegalStateException e) {
      return Response.status(Status.CONFLICT).build();
    } catch (Exception e) {
      logger.warn("Unable to post incident for job {}: {}", job.getId(), e);
      throw new WebApplicationException(INTERNAL_SERVER_ERROR);
    }
  }

  private Response unknownFormat() {
    return badRequest("Unknown format");
  }
}
