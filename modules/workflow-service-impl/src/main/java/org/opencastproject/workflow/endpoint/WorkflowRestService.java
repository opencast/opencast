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

package org.opencastproject.workflow.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.rest.AbstractJobProducerEndpoint;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.LocalHashMap;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.Configurable;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowDefinitionSet;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowQuery.Sort;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowStatistics;
import org.opencastproject.workflow.impl.WorkflowServiceImpl;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workspace.api.Workspace;

import com.google.common.util.concurrent.Striped;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * A REST endpoint for the {@link WorkflowService}
 */
@Path("/")
@RestService(name = "workflowservice", title = "Workflow Service", abstractText = "This service lists available workflows and starts, stops, suspends and resumes workflow instances.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
public class WorkflowRestService extends AbstractJobProducerEndpoint {

  /** The default number of results returned */
  private static final int DEFAULT_LIMIT = 20;
  /** The constant used to negate a querystring parameter. This is only supported on some parameters. */
  public static final String NEGATE_PREFIX = "-";
  /** The constant used to switch the direction of the sorting querystring parameter. */
  public static final String DESCENDING_SUFFIX = "_DESC";
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowRestService.class);

  /** The default server URL */
  protected String serverUrl = UrlSupport.DEFAULT_BASE_URL;
  /** The default service URL */
  protected String serviceUrl = serverUrl + "/workflow";
  /** The workflow service instance */
  private WorkflowService service;
  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;
  /** The workspace */
  private Workspace workspace;

  /** Resource lock */
  private final Striped<Lock> lock = Striped.lazyWeakLock(1024);

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
   * Sets the workflow service
   *
   * @param service
   *          the workflow service instance
   */
  public void setService(WorkflowService service) {
    this.service = service;
  }

  /**
   * Callback from the OSGi declarative services to set the workspace.
   *
   * @param workspace
   *          the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * OSGI callback for component activation
   *
   * @param cc
   *          the OSGI declarative services component context
   */
  public void activate(ComponentContext cc) {
    // Get the configured server URL
    if (cc == null) {
      serverUrl = UrlSupport.DEFAULT_BASE_URL;
    } else {
      String ccServerUrl = cc.getBundleContext().getProperty(OpencastConstants.SERVER_URL_PROPERTY);
      logger.info("configured server url is {}", ccServerUrl);
      if (ccServerUrl == null) {
        serverUrl = UrlSupport.DEFAULT_BASE_URL;
      } else {
        serverUrl = ccServerUrl;
      }
      serviceUrl = (String) cc.getProperties().get(RestConstants.SERVICE_PATH_PROPERTY);
    }
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @Path("/count")
  @RestQuery(name = "count", description = "Returns the number of workflow instances in a specific state and operation", returnDescription = "Returns the number of workflow instances in a specific state and operation", restParameters = {
          @RestParameter(name = "state", isRequired = false, description = "The workflow state", type = STRING),
          @RestParameter(name = "operation", isRequired = false, description = "The current operation", type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The number of workflow instances.") })
  public Response getCount(@QueryParam("state") WorkflowInstance.WorkflowState state,
          @QueryParam("operation") String operation) {
    try {
      Long count = service.countWorkflowInstances(state, operation);
      return Response.ok(count).build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("/statistics.xml")
  @RestQuery(name = "statisticsasxml", description = "Returns the workflow statistics as XML", returnDescription = "An XML representation of the workflow statistics.", reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the workflow statistics.") })
  public WorkflowStatistics getStatisticsAsXml() throws WorkflowDatabaseException {
    return service.getStatistics();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/statistics.json")
  @RestQuery(name = "statisticsasjson", description = "Returns the workflow statistics as JSON", returnDescription = "A JSON representation of the workflow statistics.", reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the workflow statistics.") })
  public WorkflowStatistics getStatisticsAsJson() throws WorkflowDatabaseException {
    return getStatisticsAsXml();
  }

  @GET
  @Path("definitions.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "definitions", description = "List all available workflow definitions as JSON", returnDescription = "Returns the workflow definitions as JSON", reponses = { @RestResponse(responseCode = SC_OK, description = "The workflow definitions.") })
  public WorkflowDefinitionSet getWorkflowDefinitionsAsJson() throws Exception {
    return getWorkflowDefinitionsAsXml();
  }

  @GET
  @Path("definitions.xml")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(name = "definitions", description = "List all available workflow definitions as XML", returnDescription = "Returns the workflow definitions as XML", reponses = { @RestResponse(responseCode = SC_OK, description = "The workflow definitions.") })
  public WorkflowDefinitionSet getWorkflowDefinitionsAsXml() throws Exception {
    List<WorkflowDefinition> list = service.listAvailableWorkflowDefinitions();
    return new WorkflowDefinitionSet(list);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("definition/{id}.json")
  @RestQuery(name = "definitionasjson", description = "Returns a single workflow definition", returnDescription = "Returns a JSON representation of the workflow definition with the specified identifier", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow definition identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The workflow definition."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Workflow definition not found.") })
  public Response getWorkflowDefinitionAsJson(@PathParam("id") String workflowDefinitionId)
          throws NotFoundException {
    WorkflowDefinition def;
    try {
      def = service.getWorkflowDefinitionById(workflowDefinitionId);
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    }
    return Response.ok(def).build();
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("definition/{id}.xml")
  @RestQuery(name = "definitionasxml", description = "Returns a single workflow definition", returnDescription = "Returns an XML representation of the workflow definition with the specified identifier", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow definition identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The workflow definition."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "Workflow definition not found.") })
  public Response getWorkflowDefinitionAsXml(@PathParam("id") String workflowDefinitionId)
          throws NotFoundException {
    return getWorkflowDefinitionAsJson(workflowDefinitionId);
  }

  /**
   * Returns the workflow configuration panel HTML snippet for the workflow definition specified by
   *
   * @param definitionId
   * @return config panel HTML snippet
   */
  @GET
  @Produces(MediaType.TEXT_HTML)
  @Path("configurationPanel")
  @RestQuery(name = "configpanel", description = "Get the configuration panel for a specific workflow", returnDescription = "The HTML workflow configuration panel", restParameters = { @RestParameter(name = "definitionId", isRequired = false, description = "The workflow definition identifier", type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "The workflow configuration panel.") })
  public Response getConfigurationPanel(@QueryParam("definitionId") String definitionId)
          throws NotFoundException {
    try {
      final WorkflowDefinition def = service.getWorkflowDefinitionById(definitionId);
      final String out = def.getConfigurationPanel();
      return Response.ok(out).build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("instances.xml")
  @RestQuery(name = "workflowsasxml", description = "List all workflow instances matching the query parameters", returnDescription = "An XML representation of the set of workflows matching these query parameters", restParameters = {
          @RestParameter(name = "state", isRequired = false, description = "Filter results by workflows' current state", type = STRING),
          @RestParameter(name = "q", isRequired = false, description = "Filter results by free text query", type = STRING),
          @RestParameter(name = "seriesId", isRequired = false, description = "Filter results by series identifier", type = STRING),
          @RestParameter(name = "seriesTitle", isRequired = false, description = "Filter results by series title", type = STRING),
          @RestParameter(name = "creator", isRequired = false, description = "Filter results by the mediapackage's creator", type = STRING),
          @RestParameter(name = "contributor", isRequired = false, description = "Filter results by the mediapackage's contributor", type = STRING),
          @RestParameter(name = "fromdate", isRequired = false, description = "Filter results by workflow start date.", type = STRING),
          @RestParameter(name = "todate", isRequired = false, description = "Filter results by workflow start date.", type = STRING),
          @RestParameter(name = "language", isRequired = false, description = "Filter results by mediapackage's language.", type = STRING),
          @RestParameter(name = "license", isRequired = false, description = "Filter results by mediapackage's license.", type = STRING),
          @RestParameter(name = "title", isRequired = false, description = "Filter results by mediapackage's title.", type = STRING),
          @RestParameter(name = "subject", isRequired = false, description = "Filter results by mediapackage's subject.", type = STRING),
          @RestParameter(name = "workflowdefinition", isRequired = false, description = "Filter results by workflow definition.", type = STRING),
          @RestParameter(name = "mp", isRequired = false, description = "Filter results by mediapackage identifier.", type = STRING),
          @RestParameter(name = "op", isRequired = false, description = "Filter results by workflows' current operation.", type = STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any "
                  + "of the following: DATE_CREATED, TITLE, SERIES_TITLE, SERIES_ID, MEDIA_PACKAGE_ID, WORKFLOW_DEFINITION_ID, CREATOR, "
                  + "CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", type = STRING),
          @RestParameter(name = "startPage", isRequired = false, description = "The paging offset", type = INTEGER),
          @RestParameter(name = "count", isRequired = false, description = "The number of results to return.", type = INTEGER),
          @RestParameter(name = "compact", isRequired = false, description = "Whether to return a compact version of "
                  + "the workflow instance, with mediapackage elements, workflow and workflow operation configurations and "
                  + "non-current operations removed.", type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "An XML representation of the workflow set.") })
  // CHECKSTYLE:OFF
  // The number of method parameters is too large for checkstyle's taste, but we need to handle many potential query
  // parameters. CXF provides a bean approach to accepting many parameters, but it is not part of the JAX-RS spec.
  // So for now, we disable checkstyle here.
  public Response getWorkflowsAsXml(@QueryParam("state") List<String> states, @QueryParam("q") String text,
          @QueryParam("seriesId") String seriesId, @QueryParam("seriesTitle") String seriesTitle,
          @QueryParam("creator") String creator, @QueryParam("contributor") String contributor,
          @QueryParam("fromdate") String fromDate, @QueryParam("todate") String toDate,
          @QueryParam("language") String language, @QueryParam("license") String license,
          @QueryParam("title") String title, @QueryParam("subject") String subject,
          @QueryParam("workflowdefinition") String workflowDefinitionId, @QueryParam("mp") String mediapackageId,
          @QueryParam("op") List<String> currentOperations, @QueryParam("sort") String sort,
          @QueryParam("startPage") int startPage, @QueryParam("count") int count, @QueryParam("compact") boolean compact)
          throws Exception {
    // CHECKSTYLE:ON
    if (count < 1)
      count = DEFAULT_LIMIT;

    WorkflowQuery q = new WorkflowQuery();
    q.withCount(count);
    q.withStartPage(startPage);
    if (states != null && states.size() > 0) {
      try {
        for (String state : states) {
          if (StringUtils.isBlank(state)) {
            continue;
          }
          if (state.startsWith(NEGATE_PREFIX)) {
            q.withoutState(WorkflowState.valueOf(state.substring(1).toUpperCase()));
          } else {
            q.withState(WorkflowState.valueOf(state.toUpperCase()));
          }
        }
      } catch (IllegalArgumentException e) {
        logger.debug("Unknown workflow state.", e);
      }
    }

    q.withText(text);
    q.withSeriesId(seriesId);
    q.withSeriesTitle(seriesTitle);
    q.withSubject(subject);
    q.withMediaPackage(mediapackageId);
    q.withCreator(creator);
    q.withContributor(contributor);
    q.withDateAfter(SolrUtils.parseDate(fromDate));
    q.withDateBefore(SolrUtils.parseDate(toDate));
    q.withLanguage(language);
    q.withLicense(license);
    q.withTitle(title);
    q.withWorkflowDefintion(workflowDefinitionId);

    if (currentOperations != null && currentOperations.size() > 0) {
      for (String op : currentOperations) {
        if (StringUtils.isBlank(op)) {
          continue;
        }
        if (op.startsWith(NEGATE_PREFIX)) {
          q.withoutCurrentOperation(op.substring(1));
        } else {
          q.withCurrentOperation(op);
        }
      }
    }

    if (StringUtils.isNotBlank(sort)) {
      // Parse the sort field and direction
      Sort sortField = null;
      if (sort.endsWith(DESCENDING_SUFFIX)) {
        String enumKey = sort.substring(0, sort.length() - DESCENDING_SUFFIX.length()).toUpperCase();
        try {
          sortField = Sort.valueOf(enumKey);
          q.withSort(sortField, false);
        } catch (IllegalArgumentException e) {
          logger.debug("No sort enum matches '{}'", enumKey);
        }
      } else {
        try {
          sortField = Sort.valueOf(sort);
          q.withSort(sortField);
        } catch (IllegalArgumentException e) {
          logger.debug("No sort enum matches '{}'", sort);
        }
      }
    }

    WorkflowSet set = service.getWorkflowInstances(q);

    // Marshalling of a full workflow takes a long time. Therefore, we strip everything that's not needed.
    if (compact) {
      for (WorkflowInstance instance : set.getItems()) {

        // Remove all operations but the current one
        WorkflowOperationInstance currentOperation = instance.getCurrentOperation();
        List<WorkflowOperationInstance> operations = instance.getOperations();
        operations.clear(); // instance.getOperations() is a copy
        if (currentOperation != null) {
          for (String key : currentOperation.getConfigurationKeys()) {
            currentOperation.removeConfiguration(key);
          }
          operations.add(currentOperation);
        }
        instance.setOperations(operations);

        // Remove all mediapackage elements (but keep the duration)
        MediaPackage mediaPackage = instance.getMediaPackage();
        Long duration = instance.getMediaPackage().getDuration();
        for (MediaPackageElement element : mediaPackage.elements()) {
          mediaPackage.remove(element);
        }
        mediaPackage.setDuration(duration);
      }
    }

    return Response.ok(set).build();
  }

  // CHECKSTYLE:OFF (The number of method parameters is large because we need to handle many potential query parameters)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("instances.json")
  @RestQuery(name = "workflowsasjson", description = "List all workflow instances matching the query parameters", returnDescription = "A JSON representation of the set of workflows matching these query parameters", restParameters = {
          @RestParameter(name = "state", isRequired = false, description = "Filter results by workflows' current state", type = STRING),
          @RestParameter(name = "q", isRequired = false, description = "Filter results by free text query", type = STRING),
          @RestParameter(name = "seriesId", isRequired = false, description = "Filter results by series identifier", type = STRING),
          @RestParameter(name = "seriesTitle", isRequired = false, description = "Filter results by series title", type = STRING),
          @RestParameter(name = "creator", isRequired = false, description = "Filter results by the mediapackage's creator", type = STRING),
          @RestParameter(name = "contributor", isRequired = false, description = "Filter results by the mediapackage's contributor", type = STRING),
          @RestParameter(name = "fromdate", isRequired = false, description = "Filter results by workflow start date.", type = STRING),
          @RestParameter(name = "todate", isRequired = false, description = "Filter results by workflow start date.", type = STRING),
          @RestParameter(name = "language", isRequired = false, description = "Filter results by mediapackage's language.", type = STRING),
          @RestParameter(name = "license", isRequired = false, description = "Filter results by mediapackage's license.", type = STRING),
          @RestParameter(name = "title", isRequired = false, description = "Filter results by mediapackage's title.", type = STRING),
          @RestParameter(name = "subject", isRequired = false, description = "Filter results by mediapackage's subject.", type = STRING),
          @RestParameter(name = "workflowdefinition", isRequired = false, description = "Filter results by workflow definition.", type = STRING),
          @RestParameter(name = "mp", isRequired = false, description = "Filter results by mediapackage identifier.", type = STRING),
          @RestParameter(name = "op", isRequired = false, description = "Filter results by workflows' current operation.", type = STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any "
                  + "of the following: DATE_CREATED, TITLE, SERIES_TITLE, SERIES_ID, MEDIA_PACKAGE_ID, WORKFLOW_DEFINITION_ID, CREATOR, "
                  + "CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT.  Add '_DESC' to reverse the sort order (e.g. TITLE_DESC).", type = STRING),
          @RestParameter(name = "startPage", isRequired = false, description = "The paging offset", type = INTEGER),
          @RestParameter(name = "count", isRequired = false, description = "The number of results to return.", type = INTEGER),
          @RestParameter(name = "compact", isRequired = false, description = "Whether to return a compact version of "
                  + "the workflow instance, with mediapackage elements, workflow and workflow operation configurations and "
                  + "non-current operations removed.", type = STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the workflow set.") })
  public Response getWorkflowsAsJson(@QueryParam("state") List<String> states, @QueryParam("q") String text,
          @QueryParam("seriesId") String seriesId, @QueryParam("seriesTitle") String seriesTitle,
          @QueryParam("creator") String creator, @QueryParam("contributor") String contributor,
          @QueryParam("fromdate") String fromDate, @QueryParam("todate") String toDate,
          @QueryParam("language") String language, @QueryParam("license") String license,
          @QueryParam("title") String title, @QueryParam("subject") String subject,
          @QueryParam("workflowdefinition") String workflowDefinitionId, @QueryParam("mp") String mediapackageId,
          @QueryParam("op") List<String> currentOperations, @QueryParam("sort") String sort,
          @QueryParam("startPage") int startPage, @QueryParam("count") int count, @QueryParam("compact") boolean compact)
          throws Exception {
    // CHECKSTYLE:ON
    return getWorkflowsAsXml(states, text, seriesId, seriesTitle, creator, contributor, fromDate, toDate, language,
            license, title, subject, workflowDefinitionId, mediapackageId, currentOperations, sort, startPage, count,
            compact);
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("instance/{id}.xml")
  @RestQuery(name = "workflowasxml", description = "Get a specific workflow instance.", returnDescription = "An XML representation of a workflow instance", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the workflow instance."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No workflow instance with that identifier exists.") })
  public WorkflowInstance getWorkflowAsXml(@PathParam("id") long id) throws WorkflowDatabaseException,
          NotFoundException, UnauthorizedException {
    return service.getWorkflowById(id);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("instance/{id}.json")
  @RestQuery(name = "workflowasjson", description = "Get a specific workflow instance.", returnDescription = "A JSON representation of a workflow instance", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "A JSON representation of the workflow instance."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No workflow instance with that identifier exists.") })
  public WorkflowInstance getWorkflowAsJson(@PathParam("id") long id) throws WorkflowDatabaseException,
          NotFoundException, UnauthorizedException {
    return getWorkflowAsXml(id);
  }

  @POST
  @Path("start")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "start", description = "Start a new workflow instance.", returnDescription = "An XML representation of the new workflow instance", restParameters = {
          @RestParameter(name = "definition", isRequired = true, description = "The workflow definition ID or an XML representation of a workflow definition", type = TEXT, jaxbClass = WorkflowDefinitionImpl.class),
          @RestParameter(name = "mediapackage", isRequired = true, description = "The XML representation of a mediapackage", type = TEXT, jaxbClass = MediaPackageImpl.class),
          @RestParameter(name = "parent", isRequired = false, description = "An optional parent workflow instance identifier", type = STRING),
          @RestParameter(name = "properties", isRequired = false, description = "An optional set of key=value\\n properties", type = TEXT) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the new workflow instance."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the parent workflow does not exist") })
  public WorkflowInstanceImpl start(@FormParam("definition") String workflowDefinitionXmlOrId,
          @FormParam("mediapackage") MediaPackageImpl mp, @FormParam("parent") String parentWorkflowId,
          @FormParam("properties") LocalHashMap localMap) {
    if (mp == null || StringUtils.isBlank(workflowDefinitionXmlOrId))
      throw new WebApplicationException(Status.BAD_REQUEST);

    WorkflowDefinition workflowDefinition;
    try {
      workflowDefinition = service.getWorkflowDefinitionById(workflowDefinitionXmlOrId);
    } catch (Exception e) {
      // Not an ID. Let's try if it's an XML definition
      try {
        workflowDefinition = WorkflowParser.parseWorkflowDefinition(workflowDefinitionXmlOrId);
      } catch (WorkflowParsingException wpe) {
        throw new WebApplicationException(wpe, Status.BAD_REQUEST);
      }
    }

    return startWorkflow(workflowDefinition, mp, parentWorkflowId, localMap);
  }

  private WorkflowInstanceImpl startWorkflow(WorkflowDefinition workflowDefinition, MediaPackageImpl mp,
          String parentWorkflowId, LocalHashMap localMap) {
    Map<String, String> properties = new HashMap<String, String>();
    if (localMap != null)
      properties = localMap.getMap();

    Long parentIdAsLong = null;
    if (StringUtils.isNotEmpty(parentWorkflowId)) {
      try {
        parentIdAsLong = Long.parseLong(parentWorkflowId);
      } catch (NumberFormatException e) {
        throw new WebApplicationException(e, Status.BAD_REQUEST);
      }
    }

    try {
      return (WorkflowInstanceImpl) service.start(workflowDefinition, mp, parentIdAsLong, properties);
    } catch (WorkflowException e) {
      throw new WebApplicationException(e);
    } catch (NotFoundException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("stop")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "stop", description = "Stops a workflow instance.", returnDescription = "An XML representation of the stopped workflow instance", restParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the stopped workflow instance."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No running workflow instance with that identifier exists.") })
  public WorkflowInstance stop(@FormParam("id") long workflowInstanceId) throws WorkflowException, NotFoundException,
          UnauthorizedException {
    return service.stop(workflowInstanceId);
  }

  @DELETE
  @Path("remove/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "remove", description = "Danger! Permenantly removes a workflow instance including all its child jobs. In most circumstances, /stop is what you should use.", returnDescription = "HTTP 204 No Content", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "If workflow instance could be removed successfully, no content is returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No workflow instance with that identifier exists.") })
  public Response remove(@PathParam("id") long workflowInstanceId) throws WorkflowException, NotFoundException,
          UnauthorizedException {
    service.remove(workflowInstanceId);
    return Response.noContent().build();
  }

  @POST
  @Path("suspend")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "suspend", description = "Suspends a workflow instance.", returnDescription = "An XML representation of the suspended workflow instance", restParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the suspended workflow instance."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No running workflow instance with that identifier exists.") })
  public Response suspend(@FormParam("id") long workflowInstanceId) throws NotFoundException, UnauthorizedException {
    try {
      WorkflowInstance workflow = service.suspend(workflowInstanceId);
      return Response.ok(workflow).build();
    } catch (WorkflowException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("resume")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "resume", description = "Resumes a suspended workflow instance.", returnDescription = "An XML representation of the resumed workflow instance", restParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the resumed workflow instance."),
          @RestResponse(responseCode = SC_CONFLICT, description = "Can not resume workflow not in paused state"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No suspended workflow instance with that identifier exists."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to resume. Maybe you need to authenticate.") })
  public Response resume(@FormParam("id") long workflowInstanceId, @FormParam("properties") LocalHashMap properties)
          throws NotFoundException, UnauthorizedException {
    return resume(workflowInstanceId, null, properties);
  }

  @POST
  @Path("replaceAndresume")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "replaceAndresume", description = "Replaces a suspended workflow instance with an updated version, and resumes the workflow.", returnDescription = "An XML representation of the updated and resumed workflow instance", restParameters = {
          @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING),
          @RestParameter(name = "mediapackage", isRequired = false, description = "The new Mediapackage", type = TEXT),
          @RestParameter(name = "properties", isRequired = false, description = "Properties", type = TEXT) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the updated and resumed workflow instance."),
          @RestResponse(responseCode = SC_CONFLICT, description = "Can not resume workflow not in paused state"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No suspended workflow instance with that identifier exists."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to resume. Maybe you need to authenticate.") })
  public Response resume(@FormParam("id") long workflowInstanceId,
          @FormParam("mediapackage") final String mediaPackage, @FormParam("properties") LocalHashMap properties)
          throws NotFoundException, UnauthorizedException {
    final Map<String, String> map;
    if (properties == null) {
      map = new HashMap<String, String>();
    } else {
      map = properties.getMap();
    }
    final Lock lock = this.lock.get(workflowInstanceId);
    lock.lock();
    try {
      WorkflowInstance workflow = service.getWorkflowById(workflowInstanceId);
      if (!WorkflowState.PAUSED.equals(workflow.getState())) {
        logger.warn("Can not resume workflow '{}', not in state paused but {}", workflow, workflow.getState());
        return Response.status(Status.CONFLICT).build();
      }

      if (mediaPackage != null) {
        MediaPackage newMp = MediaPackageParser.getFromXml(mediaPackage);
        MediaPackage oldMp = workflow.getMediaPackage();

        // Delete removed elements from workspace
        for (MediaPackageElement elem : oldMp.getElements()) {
          if (MediaPackageSupport.contains(elem.getIdentifier(), newMp))
            continue;
          try {
            workspace.delete(elem.getURI());
            logger.info("Deleted removed mediapackge element {}", elem);
          } catch (NotFoundException e) {
            logger.info("Removed mediapackage element {} is already deleted", elem);
          }
        }

        workflow.setMediaPackage(newMp);
        service.update(workflow);
      }
      workflow = service.resume(workflowInstanceId, map);
      return Response.ok(workflow).build();
    } catch (NotFoundException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (UnauthorizedException e) {
      return Response.status(Status.UNAUTHORIZED).build();
    } catch (IllegalStateException e) {
      logger.warn(ExceptionUtils.getMessage(e));
      return Response.status(Status.CONFLICT).build();
    } catch (WorkflowException e) {
      logger.error(ExceptionUtils.getMessage(e), e);
      return Response.serverError().build();
    } catch (Exception e) {
      logger.error(ExceptionUtils.getMessage(e), e);
      return Response.serverError().build();
    }
    finally {
      lock.unlock();
    }
  }

  @POST
  @Path("update")
  @RestQuery(name = "update", description = "Updates a workflow instance.", returnDescription = "No content.", restParameters = { @RestParameter(name = "workflow", isRequired = true, description = "The XML representation of the workflow instance.", type = TEXT) }, reponses = { @RestResponse(responseCode = SC_NO_CONTENT, description = "Workflow instance updated.") })
  public Response update(@FormParam("workflow") String workflowInstance) throws NotFoundException,
          UnauthorizedException {
    try {
      WorkflowInstance instance = WorkflowParser.parseWorkflowInstance(workflowInstance);
      service.update(instance);
      return Response.noContent().build();
    } catch (WorkflowException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("handlers.json")
  @SuppressWarnings("unchecked")
  @RestQuery(name = "handlers", description = "List all registered workflow operation handlers (implementations).", returnDescription = "A JSON representation of the registered workflow operation handlers.", reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the registered workflow operation handlers") })
  public Response getOperationHandlers() {
    JSONArray jsonArray = new JSONArray();
    for (HandlerRegistration reg : ((WorkflowServiceImpl) service).getRegisteredHandlers()) {
      WorkflowOperationHandler handler = reg.getHandler();
      JSONObject jsonHandler = new JSONObject();
      jsonHandler.put("id", handler.getId());
      jsonHandler.put("description", handler.getDescription());
      jsonArray.add(jsonHandler);
    }
    return Response.ok(jsonArray.toJSONString()).header("Content-Type", MediaType.APPLICATION_JSON).build();
  }

  @GET
  @Path("statemappings.json")
  @SuppressWarnings("unchecked")
  @RestQuery(name = "statemappings", description = "Get all workflow state mappings",
      returnDescription = "A JSON representation of the workflow state mappings.",
      reponses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the workflow state mappings") })
  public Response getStateMappings() {
    return Response.ok(new JSONObject(service.getWorkflowStateMappings()).toJSONString())
        .header("Content-Type", MediaType.APPLICATION_JSON).build();
  }

  @Path("/cleanup")
  @RestQuery(name = "cleanup", description = "Cleans up workflow instances", returnDescription = "No return value", reponses = {
          @RestResponse(responseCode = SC_OK, description = "Cleanup OK"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Couldn't parse given state"),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to cleanup. Maybe you need to authenticate."),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "It's not allowed to delete other workflow instance statues than STOPPED, SUCCEEDED and FAILED") }, restParameters = {
          @RestParameter(name = "buffer", type = Type.INTEGER, defaultValue = "30", isRequired = true, description = "Lifetime (buffer) in days a workflow instance should live"),
          @RestParameter(name = "state", type = Type.STRING, isRequired = true, description = "Workflow instance state, only STOPPED, SUCCEEDED and FAILED are allowed values here") })
  public Response cleanup(@FormParam("buffer") int buffer, @FormParam("state") String stateParam)
          throws UnauthorizedException {

    WorkflowInstance.WorkflowState state;
    try {
      state = WorkflowInstance.WorkflowState.valueOf(stateParam);
    } catch (IllegalArgumentException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    if (state != WorkflowInstance.WorkflowState.SUCCEEDED && state != WorkflowInstance.WorkflowState.FAILED
            && state != WorkflowInstance.WorkflowState.STOPPED)
      return Response.status(Status.FORBIDDEN).build();

    try {
      service.cleanupWorkflowInstances(buffer, state);
      return Response.ok().build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    }
  }

  @SuppressWarnings("unchecked")
  protected JSONArray getOperationsAsJson(List<WorkflowOperationInstance> operations) {
    JSONArray jsonArray = new JSONArray();
    for (WorkflowOperationInstance op : operations) {
      JSONObject jsOp = new JSONObject();
      jsOp.put("name", op.getTemplate());
      jsOp.put("description", op.getDescription());
      jsOp.put("state", op.getState().name().toLowerCase());
      jsOp.put("configurations", getConfigsAsJson(op));
      jsonArray.add(jsOp);
    }
    return jsonArray;
  }

  @SuppressWarnings("unchecked")
  protected JSONArray getConfigsAsJson(Configurable entity) {
    JSONArray json = new JSONArray();
    Set<String> keys = entity.getConfigurationKeys();
    if (keys != null) {
      for (String key : keys) {
        JSONObject jsConfig = new JSONObject();
        jsConfig.put(key, entity.getConfiguration(key));
        json.add(jsConfig);
      }
    }
    return json;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.rest.AbstractJobProducerEndpoint#getService()
   */
  @Override
  public JobProducer getService() {
    if (service instanceof JobProducer) {
      return (JobProducer) service;
    } else {
      return null;
    }
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
