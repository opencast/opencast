/*
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
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.LocalHashMap;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestParameter.Type;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.JaxbWorkflowInstance;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowDefinitionSet;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSetImpl;
import org.opencastproject.workflow.api.WorkflowStateException;
import org.opencastproject.workflow.api.XmlWorkflowParser;
import org.opencastproject.workflow.impl.WorkflowServiceImpl;
import org.opencastproject.workflow.impl.WorkflowServiceImpl.HandlerRegistration;
import org.opencastproject.workspace.api.Workspace;

import com.google.common.util.concurrent.Striped;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
@Path("/workflow")
@RestService(name = "workflowservice", title = "Workflow Service", abstractText = "This service lists available workflows and starts, stops, suspends and resumes workflow instances.", notes = {
        "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
        "If the service is down or not working it will return a status 503, this means the the underlying service is "
                + "not working and is either restarting or has failed",
        "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
                + "other words, there is a bug! You should file an error report with your server logs from the time when the "
                + "error occurred: <a href=\"https://github.com/opencast/opencast/issues\">Opencast Issue Tracker</a>" })
@Component(
    immediate = true,
    service = WorkflowRestService.class,
    property = {
        "service.description=Workflow REST Endpoint",
        "opencast.service.type=org.opencastproject.workflow",
        "opencast.service.path=/workflow",
        "opencast.service.jobproducer=true"
    }
)
@JaxrsResource
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
  @Reference
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * Sets the workflow service
   *
   * @param service
   *          the workflow service instance
   */
  @Reference
  public void setService(WorkflowService service) {
    this.service = service;
  }

  /**
   * Callback from the OSGi declarative services to set the workspace.
   *
   * @param workspace
   *          the workspace
   */
  @Reference
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * OSGI callback for component activation
   *
   * @param cc
   *          the OSGI declarative services component context
   */
  @Activate
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
  @RestQuery(name = "count", description = "Returns the number of workflow instances in a specific state", returnDescription = "Returns the number of workflow instances in a specific state", restParameters = {
          @RestParameter(name = "state", isRequired = false, description = "The workflow state", type = STRING)},
          responses = { @RestResponse(responseCode = SC_OK, description = "The number of workflow instances.") })
  public Response getCount(@QueryParam("state") WorkflowInstance.WorkflowState state,
          @QueryParam("operation") String operation) {
    try {
      Long count = service.countWorkflowInstances(state);
      return Response.ok(count).build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("definitions.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "definitions", description = "List all available workflow definitions as JSON", returnDescription = "Returns the workflow definitions as JSON", responses = { @RestResponse(responseCode = SC_OK, description = "The workflow definitions.") })
  public WorkflowDefinitionSet getWorkflowDefinitionsAsJson() throws Exception {
    return getWorkflowDefinitionsAsXml();
  }

  @GET
  @Path("definitions.xml")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(name = "definitions", description = "List all available workflow definitions as XML", returnDescription = "Returns the workflow definitions as XML", responses = { @RestResponse(responseCode = SC_OK, description = "The workflow definitions.") })
  public WorkflowDefinitionSet getWorkflowDefinitionsAsXml() throws Exception {
    List<WorkflowDefinition> list = service.listAvailableWorkflowDefinitions();
    return new WorkflowDefinitionSet(list);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("definition/{id}.json")
  @RestQuery(name = "definitionasjson", description = "Returns a single workflow definition", returnDescription = "Returns a JSON representation of the workflow definition with the specified identifier", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow definition identifier", type = STRING) }, responses = {
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
  @RestQuery(name = "definitionasxml", description = "Returns a single workflow definition", returnDescription = "Returns an XML representation of the workflow definition with the specified identifier", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow definition identifier", type = STRING) }, responses = {
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
  @RestQuery(name = "configpanel", description = "Get the configuration panel for a specific workflow", returnDescription = "The HTML workflow configuration panel", restParameters = { @RestParameter(name = "definitionId", isRequired = false, description = "The workflow definition identifier", type = STRING) }, responses = { @RestResponse(responseCode = SC_OK, description = "The workflow configuration panel.") })
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
  @Produces(MediaType.APPLICATION_JSON)
  @Path("mediaPackage/{id}/hasActiveWorkflows")
  @RestQuery(name = "hasactiveworkflows", description = "Returns if a media package has active workflows",
          returnDescription = "Returns wether the media package has active workflows as a boolean.",
          pathParameters = {
                  @RestParameter(name = "id", isRequired = true, description = "The media package identifier", type = STRING) },
          responses = {
                  @RestResponse(responseCode = SC_OK, description = "Whether the media package has active workflows.")})
  public Response mediaPackageHasActiveWorkflows(@PathParam("id") String mediaPackageId) {
    try {
      return Response.ok(Boolean.toString(service.mediaPackageHasActiveWorkflows(mediaPackageId))).build();

    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("mediaPackage/{id}/instances.json")
  @RestQuery(name = "workflowsofmediapackage", description = "Returns the workflows for a media package",
          returnDescription = "Returns the workflows that are associated with the media package as JSON.",
          pathParameters = {
                  @RestParameter(name = "id", isRequired = true, description = "The media package identifier", type = STRING) },
          responses = {
                  @RestResponse(responseCode = SC_OK, description = "Returns the workflows for a media package.")})
  public Response getWorkflowsOfMediaPackage(@PathParam("id") String mediaPackageId) {
    try {
      return Response.ok(new WorkflowSetImpl(service.getWorkflowInstancesByMediaPackage(mediaPackageId))).build();
    } catch (UnauthorizedException e) {
      return Response.status(Status.FORBIDDEN).build();
    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("mediaPackage/{id}/currentInstance.json")
  @RestQuery(name = "currentworkflowofmediapackage", description = "Returns the current workflow for a media package",
          returnDescription = "Returns the currentworkflow that are associated with the media package as JSON.",
          pathParameters = {
                  @RestParameter(name = "id", isRequired = true, description = "The media package identifier", type = STRING) },
          responses = {
                  @RestResponse(responseCode = SC_OK, description = "Returns the workflows for a media package."),
                  @RestResponse(responseCode = SC_NOT_FOUND, description = "Current workflow not found.") })
  public Response getRunningWorkflowOfMediaPackage(@PathParam("id") String mediaPackageId) {
    try {
      Optional<WorkflowInstance> optWorkflowInstance = service.
              getRunningWorkflowInstanceByMediaPackage(mediaPackageId, Permissions.Action.READ.toString());
      if (optWorkflowInstance.isPresent()) {
        return Response.ok(new JaxbWorkflowInstance(optWorkflowInstance.get())).build();
      } else {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

    } catch (WorkflowException | UnauthorizedException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("user/{id}/hasActiveWorkflows")
  @RestQuery(name = "userhasactiveworkflows", description = "Returns if there are currently workflow(s) running that"
          + "were started by the given user",
          returnDescription = "Returns if there are currently workflow(s) running that were started by the given user "
                  + "as a boolean.",
          pathParameters = {
                  @RestParameter(name = "id", isRequired = true, description = "The user identifier", type = STRING) },
          responses = {
                  @RestResponse(responseCode = SC_OK, description = "Whether there are active workflow for the user.")})
  public Response userHasActiveWorkflows(@PathParam("id") String userId) {
    try {
      return Response.ok(Boolean.toString(service.userHasActiveWorkflows(userId))).build();

    } catch (WorkflowDatabaseException e) {
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @Path("instance/{id}.xml")
  @RestQuery(name = "workflowasxml", description = "Get a specific workflow instance.", returnDescription = "An XML representation of a workflow instance", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, responses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the workflow instance."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No workflow instance with that identifier exists.") })
  public JaxbWorkflowInstance getWorkflowAsXml(@PathParam("id") long id) throws WorkflowDatabaseException,
          NotFoundException, UnauthorizedException {
    return new JaxbWorkflowInstance(service.getWorkflowById(id));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("instance/{id}.json")
  @RestQuery(name = "workflowasjson", description = "Get a specific workflow instance.", returnDescription = "A JSON representation of a workflow instance", pathParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, responses = {
          @RestResponse(responseCode = SC_OK, description = "A JSON representation of the workflow instance."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No workflow instance with that identifier exists.") })
  public JaxbWorkflowInstance getWorkflowAsJson(@PathParam("id") long id) throws WorkflowDatabaseException,
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
          @RestParameter(name = "properties", isRequired = false, description = "An optional set of key=value\\n properties", type = TEXT) }, responses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the new workflow instance."),
          @RestResponse(responseCode = SC_UNAUTHORIZED, description = "You do not have permission to resume. Maybe you need to authenticate."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the parent workflow does not exist") })
  public JaxbWorkflowInstance start(@FormParam("definition") String workflowDefinitionXmlOrId,
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
        workflowDefinition = XmlWorkflowParser.parseWorkflowDefinition(workflowDefinitionXmlOrId);
      } catch (WorkflowParsingException wpe) {
        throw new WebApplicationException(wpe, Status.BAD_REQUEST);
      }
    }

    WorkflowInstance instance = null;
    try {
      instance = startWorkflow(workflowDefinition, mp, parentWorkflowId, localMap);
    } catch (UnauthorizedException e) {
      throw new WebApplicationException(e, Status.UNAUTHORIZED);
    }
    return new JaxbWorkflowInstance(instance);
  }

  private WorkflowInstance startWorkflow(WorkflowDefinition workflowDefinition, MediaPackageImpl mp,
          String parentWorkflowId, LocalHashMap localMap) throws UnauthorizedException {
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
      return (WorkflowInstance) service.start(workflowDefinition, mp, parentIdAsLong, properties);
    } catch (WorkflowException e) {
      throw new WebApplicationException(e);
    } catch (NotFoundException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("stop")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "stop", description = "Stops a workflow instance.", returnDescription = "An XML representation of the stopped workflow instance", restParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, responses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the stopped workflow instance."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No running workflow instance with that identifier exists.") })
  public JaxbWorkflowInstance stop(@FormParam("id") long workflowInstanceId) throws WorkflowException, NotFoundException,
          UnauthorizedException {
    WorkflowInstance instance = service.stop(workflowInstanceId);
    return new JaxbWorkflowInstance(instance);
  }

  @DELETE
  @Path("remove/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  @RestQuery(name = "remove", description = "Danger! Permenantly removes a workflow instance including all its child jobs. In most circumstances, /stop is what you should use.", returnDescription = "HTTP 204 No Content", pathParameters = {
          @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING)}, restParameters = {
          @RestParameter(name = "force", isRequired = false, description = "If the workflow status should be ignored and the workflow removed anyway", type = Type.BOOLEAN, defaultValue = "false")}, responses = {
          @RestResponse(responseCode = HttpServletResponse.SC_NO_CONTENT, description = "If workflow instance could be removed successfully, no content is returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No workflow instance with that identifier exists."),
          @RestResponse(responseCode = SC_FORBIDDEN, description = "It's not allowed to remove other workflow instance statues than STOPPED, SUCCEEDED and FAILED (use force parameter to override AT YOUR OWN RISK).") })
  public Response remove(@PathParam("id") long workflowInstanceId, @QueryParam("force") boolean force) throws WorkflowException, NotFoundException,
          UnauthorizedException {
    try {
      service.remove(workflowInstanceId, force);
    } catch (WorkflowStateException e) {
      return Response.status(Status.FORBIDDEN).build();
    }
    return Response.noContent().build();
  }

  @POST
  @Path("suspend")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "suspend", description = "Suspends a workflow instance.", returnDescription = "An XML representation of the suspended workflow instance", restParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, responses = {
          @RestResponse(responseCode = SC_OK, description = "An XML representation of the suspended workflow instance."),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "No running workflow instance with that identifier exists.") })
  public Response suspend(@FormParam("id") long workflowInstanceId) throws NotFoundException, UnauthorizedException {
    try {
      WorkflowInstance workflow = service.suspend(workflowInstanceId);
      return Response.ok(new JaxbWorkflowInstance(workflow)).build();
    } catch (WorkflowException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("resume")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "resume", description = "Resumes a suspended workflow instance.", returnDescription = "An XML representation of the resumed workflow instance", restParameters = { @RestParameter(name = "id", isRequired = true, description = "The workflow instance identifier", type = STRING) }, responses = {
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
          @RestParameter(name = "properties", isRequired = false, description = "Properties", type = TEXT) }, responses = {
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
      return Response.ok(new JaxbWorkflowInstance(workflow)).build();
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
  @RestQuery(name = "update", description = "Updates a workflow instance.", returnDescription = "No content.", restParameters = { @RestParameter(name = "workflow", isRequired = true, description = "The XML representation of the workflow instance.", type = TEXT) }, responses = { @RestResponse(responseCode = SC_NO_CONTENT, description = "Workflow instance updated.") })
  public Response update(@FormParam("workflow") String workflowInstance) throws NotFoundException,
          UnauthorizedException {
    try {
      WorkflowInstance instance = XmlWorkflowParser.parseWorkflowInstance(workflowInstance);
      service.update(instance);
      return Response.noContent().build();
    } catch (WorkflowException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("handlers.json")
  @SuppressWarnings("unchecked")
  @RestQuery(name = "handlers", description = "List all registered workflow operation handlers (implementations).", returnDescription = "A JSON representation of the registered workflow operation handlers.", responses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the registered workflow operation handlers") })
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
      responses = { @RestResponse(responseCode = SC_OK, description = "A JSON representation of the workflow state mappings") })
  public Response getStateMappings() {
    return Response.ok(new JSONObject(service.getWorkflowStateMappings()).toJSONString())
        .header("Content-Type", MediaType.APPLICATION_JSON).build();
  }

  @Path("/cleanup")
  @RestQuery(name = "cleanup", description = "Cleans up workflow instances", returnDescription = "No return value", responses = {
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
