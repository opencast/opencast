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

package org.opencastproject.adminui.endpoint;

import static com.entwinemedia.fn.data.Opt.nul;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.index.service.util.RestUtils.okJson;
import static org.opencastproject.workflow.api.ConfiguredWorkflow.workflow;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.index.service.util.JSONUtils;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.data.Arrays;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParsingException;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jettison.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
@RestService(name = "TasksService", title = "UI Tasks",
  abstractText = "Provides resources and operations related to the tasks",
  notes = { "All paths above are relative to the REST endpoint base (something like http://your.server/files)",
            "If the service is down or not working it will return a status 503, this means the the underlying service is "
              + "not working and is either restarting or has failed",
            "A status code 500 means a general failure has occurred which is not recoverable and was not anticipated. In "
              + "other words, there is a bug! You should file an error report with your server logs from the time when the "
              + "error occurred: <a href=\"https://opencast.jira.com\">Opencast Issue Tracker</a>",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public class TasksEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(TasksEndpoint.class);

  private WorkflowService workflowService;

  private AssetManager assetManager;

  private Workspace workspace;

  /** A parser for handling JSON documents inside the body of a request. **/
  private final JSONParser parser = new JSONParser();

  /** OSGi callback for the workflow service. */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** OSGi callback to set the asset manager. */
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /** OSGi callback to set the workspace. */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  protected void activate(BundleContext bundleContext) {
    logger.info("Activate tasks endpoint");
  }

  @GET
  @Path("processing.json")
  @RestQuery(name = "getProcessing", description = "Returns all the data related to the processing tab in the new tasks modal as JSON", returnDescription = "All the data related to the tasks processing tab as JSON", restParameters = { @RestParameter(name = "tags", isRequired = false, description = "A comma separated list of tags to filter the workflow definitions", type = RestParameter.Type.STRING) }, reponses = { @RestResponse(responseCode = SC_OK, description = "Returns all the data related to the tasks processing tab as JSON") })
  public Response getProcessing(@QueryParam("tags") String tagsString) {
    List<String> tags = RestUtil.splitCommaSeparatedParam(Option.option(tagsString)).value();

    // This is the JSON Object which will be returned by this request
    List<JValue> actions = new ArrayList<>();
    try {
      List<WorkflowDefinition> workflowsDefinitions = workflowService.listAvailableWorkflowDefinitions();
      for (WorkflowDefinition wflDef : workflowsDefinitions) {
        if (wflDef.containsTag(tags)) {
          actions.add(obj(f("id", v(wflDef.getId())), f("title", v(nul(wflDef.getTitle()).getOr(""))),
                  f("description", v(nul(wflDef.getDescription()).getOr(""))),
                  f("configuration_panel", v(nul(wflDef.getConfigurationPanel()).getOr("")))));
        }
      }
    } catch (WorkflowDatabaseException e) {
      logger.error("Unable to get available workflow definitions: {}", ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }

    return okJson(arr(actions));
  }

  @POST
  @Path("/new")
  @RestQuery(name = "createNewTask", description = "Creates a new task by the given metadata as JSON",
          returnDescription = "The task identifiers",
          restParameters = { @RestParameter(name = "metadata", isRequired = true, description = "The metadata as JSON",
                  type = RestParameter.Type.TEXT) }, reponses = {
          @RestResponse(responseCode = HttpServletResponse.SC_CREATED, description = "Task sucessfully added"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "If the workflow definition is not found"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "If the metadata is not set, "
                  + "couldn't be parsed or at least one task couldn't be created") })
  public Response createNewTask(@FormParam("metadata") String metadata) throws NotFoundException {
    if (StringUtils.isBlank(metadata)) {
      logger.warn("No metadata set");
      return RestUtil.R.badRequest("No metadata set");
    }

    JSONObject metadataJson;
    try {
      metadataJson = (JSONObject) parser.parse(metadata);
    } catch (Exception e) {
      logger.warn("Unable to parse metadata {}", metadata);
      return RestUtil.R.badRequest("Unable to parse metadata");
    }

    String workflowId = (String) metadataJson.get("workflow");
    if (StringUtils.isBlank(workflowId))
      return RestUtil.R.badRequest("No workflow set");

    JSONArray eventIds = (JSONArray) metadataJson.get("eventIds");
    if (eventIds == null)
      return RestUtil.R.badRequest("No eventIds set");

    JSONObject configuration = (JSONObject) metadataJson.get("configuration");
    Stream<Entry<String, String>> options = Stream.empty();
    if (configuration != null) {
      try {
        options = options.append(JSONUtils.toMap(
                new org.codehaus.jettison.json.JSONObject(configuration.toJSONString())).entrySet());
      } catch (JSONException e) {
        logger.warn("Unable to parse workflow options to map: {}", ExceptionUtils.getStackTrace(e));
        return RestUtil.R.badRequest("Unable to parse workflow options to map");
      }
    }

    Map<String, String> optionsMap = options.filter(new Fn<Map.Entry<String, String>, Boolean>() {
      @Override
      public Boolean apply(Entry<String, String> a) {
        return !"eventIds".equalsIgnoreCase(a.getKey());
      }
    }).foldl(new HashMap<String, String>(), TasksEndpoint.<String, String> mapFold());

    WorkflowDefinition wfd;
    try {
      wfd = workflowService.getWorkflowDefinitionById(workflowId);
    } catch (WorkflowDatabaseException e) {
      logger.error("Unable to get workflow definition {}: {}", workflowId, ExceptionUtils.getStackTrace(e));
      return RestUtil.R.serverError();
    }

    JSONObject json = new JSONObject();
    int successfulEventsCount = 0;

    final Workflows workflowsX = new Workflows(assetManager, workspace, workflowService);
    for (Object eventId : eventIds) {

      JSONObject jsonEvent = new JSONObject();
      json.put((String)eventId, jsonEvent);

      Snapshot snapshot = workflowsX.getLatestVersion((String)eventId);
      if (snapshot != null) {
        MediaPackage mp = workflowsX.putInWorkspace(snapshot); //TODO

        try {
          WorkflowInstance wfInstance = workflowService.start(
            workflow(wfd, optionsMap).getWorkflowDefinition(), mp, optionsMap);

          jsonEvent.put("successful", true);
          jsonEvent.put("workflow", wfInstance.getId());
          successfulEventsCount++;

        } catch (WorkflowDatabaseException ex) {
          logger.error("Starting workflow '{}' for event '{}' failed "
                  + "because workflow instance could not be persisted: {}",
                  Arrays.array(wfd.getTitle(), eventId, ExceptionUtils.getStackTrace(ex)));

          jsonEvent.put("successful", false);
          jsonEvent.put("message", "ERRORS.WORKFLOW_NOT_PERSISTED");
        } catch (WorkflowParsingException ex) {
          logger.error("Starting workflow '{}' for event '{}' failed  "
                  + "because workflow could not be parsed: {}",
                  Arrays.array(wfd.getTitle(), eventId, ExceptionUtils.getStackTrace(ex)));

          jsonEvent.put("successful", false);
          jsonEvent.put("message", "ERRORS.WORKFLOW_NOT_PARSED");

        } catch (IllegalStateException ex) {
          logger.warn(ex.getMessage());

          jsonEvent.put("successful", false);
          jsonEvent.put("message", "ERRORS.ANOTHER_WORKFLOW_ACTIVE");
        }
      }
    }
    if (successfulEventsCount < eventIds.size()) {
      return Response.status(Status.BAD_REQUEST)
              .entity(json.toJSONString()).build();
    }
    return Response.status(Status.CREATED)
            .entity(json.toJSONString()).build();
  }

  private static <A, B> Fn2<Map<A, B>, Entry<A, B>, Map<A, B>> mapFold() {
    return new Fn2<Map<A, B>, Entry<A, B>, Map<A, B>>() {
      @Override
      public Map<A, B> apply(Map<A, B> sum, Entry<A, B> a) {
        sum.put(a.getKey(), a.getValue());
        return sum;
      }
    };
  }

  private static final Fn<WorkflowInstance, String> getWorkflowIds = new Fn<WorkflowInstance, String>() {
    @Override
    public String apply(WorkflowInstance a) {
      return Long.toString(a.getId());
    }
  };

}
