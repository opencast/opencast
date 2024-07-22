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
package org.opencastproject.external.endpoint;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.opencastproject.index.service.util.JSONUtils.arrayToJsonArray;
import static org.opencastproject.index.service.util.JSONUtils.safeString;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.requests.SortCriterion.Order.Descending;

import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponseBuilder;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.requests.SortCriterion;
import org.opencastproject.workflow.api.RetryStrategy;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowService;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/api/workflow-definitions")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_1_0, ApiMediaType.VERSION_1_2_0, ApiMediaType.VERSION_1_3_0,
            ApiMediaType.VERSION_1_4_0, ApiMediaType.VERSION_1_5_0, ApiMediaType.VERSION_1_6_0,
            ApiMediaType.VERSION_1_7_0, ApiMediaType.VERSION_1_8_0,
            ApiMediaType.VERSION_1_9_0, ApiMediaType.VERSION_1_10_0, ApiMediaType.VERSION_1_11_0,
            ApiMediaType.VERSION_1_12_0 })
@RestService(name = "externalapiworkflowdefinitions", title = "External API Workflow Definitions Service", notes = {},
             abstractText = "Provides resources and operations related to the workflow definitions")
@Component(
    immediate = true,
    service = WorkflowDefinitionsEndpoint.class,
    property = {
        "service.description=External API - Workflow Definitions Endpoint",
        "opencast.service.type=org.opencastproject.external.workflows.definitions",
        "opencast.service.path=/api/workflow-definitions"
    }
)
@JaxrsResource
public class WorkflowDefinitionsEndpoint {

  /**
   * The logging facility
   */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionsEndpoint.class);

  /**
   * The workflow service
   */
  private WorkflowService workflowService;

  /**
   * OSGi DI
   */
  public WorkflowService getWorkflowService() {
    return workflowService;
  }

  /**
   * OSGi DI
   */
  @Reference
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /**
   * OSGi activation method
   */
  @Activate
  void activate(ComponentContext cc) {
    logger.info("Activating External API - Workflow Definitions Endpoint");
  }

  @GET
  @Path("/")
  @RestQuery(name = "getworkflowdefinitions", description = "Returns a list of workflow definition.", returnDescription = "", restParameters = {
          @RestParameter(name = "withoperations", description = "Whether the workflow operations should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "withconfigurationpanel", description = "Whether the workflow configuration panel should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "filter", description = "Usage [Filter Name]:[Value to Filter With]. Available filter: \"tag\"", isRequired = false, type = STRING),
          @RestParameter(name = "sort", description = "Sort the results based upon a list of comma seperated sorting criteria. In the comma seperated list each type of sorting is specified as a pair such as: <Sort Name>:ASC or <Sort Name>:DESC. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory.", isRequired = false, type = STRING),
          @RestParameter(name = "limit", description = "The maximum number of results to return for a single request.", isRequired = false, type = INTEGER),
          @RestParameter(name = "offset", description = "The index of the first result to return.", isRequired = false, type = INTEGER) }, responses = {
          @RestResponse(description = "A (potentially empty) list of workflow definitions is returned.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response getWorkflowDefinitions(@HeaderParam("Accept") String acceptHeader,
          @QueryParam("withoperations") boolean withOperations,
          @QueryParam("withconfigurationpanel") boolean withConfigurationPanel,
          @QueryParam("withconfigurationpaneljson") boolean withConfigurationPanelJson, @QueryParam("filter") String filter,
          @QueryParam("sort") String sort, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
    Stream<WorkflowDefinition> workflowDefinitions;
    try {
      workflowDefinitions = workflowService.listAvailableWorkflowDefinitions().stream();
    } catch (WorkflowDatabaseException e) {
      logger.error("The workflow service was not able to get the workflow definitions:", e);
      return ApiResponseBuilder.serverError("Could not retrieve workflow definitions, reason: '%s'", getMessage(e));
    }

    // Apply filter
    if (StringUtils.isNotBlank(filter)) {
      for (String f : filter.split(",")) {
        int sepIdx = f.indexOf(':');
        if (sepIdx < 0 || sepIdx == f.length() - 1) {
          logger.debug("No value for filter {} in filters list: {}", f, filter);
          continue;
        }
        String name = f.substring(0, sepIdx);
        String value = f.substring(sepIdx + 1);

        if ("tag".equals(name))
          workflowDefinitions = workflowDefinitions.filter(wd -> ArrayUtils.contains(wd.getTags(), value));
      }
    }

    // Apply sort
    // TODO: this seems to not function as intended
    ComparatorChain<WorkflowDefinition> comparator = new ComparatorChain<>();
    if (StringUtils.isNoneBlank(sort)) {
      ArrayList<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(sort);
      for (SortCriterion criterion : sortCriteria) {
        switch (criterion.getFieldName()) {
          case "identifier":
            comparator.addComparator((wd1, wd2) -> {
              String s1 = defaultString(wd1.getId());
              String s2 = defaultString(wd2.getId());
              if (criterion.getOrder() == Descending)
                return s2.compareTo(s1);
              return s1.compareTo(s2);
            });
            break;
          case "title":
            comparator.addComparator((wd1, wd2) -> {
              String s1 = defaultString(wd1.getTitle());
              String s2 = defaultString(wd2.getTitle());
              if (criterion.getOrder() == Descending)
                return s2.compareTo(s1);
              return s1.compareTo(s2);
            });
            break;
          case "displayorder":
            comparator.addComparator((wd1, wd2) -> {
              if (criterion.getOrder() == Descending)
                return Integer.compare(wd2.getDisplayOrder(), wd1.getDisplayOrder());
              return Integer.compare(wd1.getDisplayOrder(), wd2.getDisplayOrder());
            });
            break;
          default:
            return RestUtil.R.badRequest(
                    String.format("Unknown search criterion in request: %s", criterion.getFieldName()));
        }
      }
    }
    if (comparator.size() > 0) {
      workflowDefinitions = workflowDefinitions.sorted(comparator);
    }

    // Apply offset
    if (offset != null && offset > 0) {
      workflowDefinitions = workflowDefinitions.skip(offset);
    }

    // Apply limit
    if (limit != null && limit > 0) {
      workflowDefinitions = workflowDefinitions.limit(limit);
    }

    List<JsonObject> jsonObjects = workflowDefinitions
        .map(wd -> workflowDefinitionToJSON(wd, withOperations, withConfigurationPanel, withConfigurationPanelJson))
        .collect(Collectors.toList());

    JsonArray jsonArray = new JsonArray();
    for (JsonObject obj : jsonObjects) {
      jsonArray.add(obj);
    }

    return ApiResponseBuilder.Json.ok(acceptHeader, jsonArray);
  }

  @GET
  @Path("{workflowDefinitionId}")
  @RestQuery(name = "getworkflowdefinition", description = "Returns a single workflow definition.", returnDescription = "", pathParameters = {
          @RestParameter(name = "workflowDefinitionId", description = "The workflow definition id", isRequired = true, type = STRING) }, restParameters = {
          @RestParameter(name = "withoperations", description = "Whether the workflow operations should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "withconfigurationpaneljson", description = "Whether the workflow configuration panel in JSON should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "withconfigurationpanel", description = "Whether the workflow configuration panel should be included in the response", isRequired = false, type = BOOLEAN) }, responses = {
          @RestResponse(description = "The workflow definition is returned.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The specified workflow definition does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getWorkflowDefinition(@HeaderParam("Accept") String acceptHeader,
          @PathParam("workflowDefinitionId") String id, @QueryParam("withoperations") boolean withOperations,
          @QueryParam("withconfigurationpanel") boolean withConfigurationPanel,
          @QueryParam("withconfigurationpaneljson") boolean withConfigurationPanelJson) throws Exception {
    WorkflowDefinition wd;
    try {
      wd = workflowService.getWorkflowDefinitionById(id);
    } catch (NotFoundException e) {
      return ApiResponseBuilder.notFound("Cannot find workflow definition with id '%s'.", id);
    }

    return ApiResponseBuilder.Json.ok(acceptHeader, workflowDefinitionToJSON(wd, withOperations, withConfigurationPanel, withConfigurationPanelJson));
  }

  private JsonObject workflowDefinitionToJSON(WorkflowDefinition wd, boolean withOperations,
      boolean withConfigurationPanel, boolean withConfigurationPanelJson) {
    JsonObject json = new JsonObject();

    json.addProperty("identifier", wd.getId());
    json.addProperty("title", safeString(wd.getTitle()));
    json.addProperty("description", safeString(wd.getDescription()));
    json.add("tags", arrayToJsonArray(wd.getTags()));
    if (withConfigurationPanel) {
      json.addProperty("configuration_panel", safeString(wd.getConfigurationPanel()));
    }
    if (withConfigurationPanelJson) {
      json.addProperty("configuration_panel_json", safeString(wd.getConfigurationPanelJson()));
    }
    if (withOperations) {
      JsonArray operationsArray = new JsonArray();
      for (WorkflowOperationDefinition op : wd.getOperations()) {
        operationsArray.add(workflowOperationDefinitionToJSON(op));
      }
      json.add("operations", operationsArray);
    }

    return json;
  }

  private JsonObject workflowOperationDefinitionToJSON(WorkflowOperationDefinition wod) {
    JsonObject json = new JsonObject();

    json.addProperty("operation", wod.getId());
    json.addProperty("description", safeString(wod.getDescription()));
    JsonObject configJson = new JsonObject();
    for (String key : wod.getConfigurationKeys()) {
      String value = wod.getConfiguration(key);
      configJson.addProperty(key, value);
    }
    json.add("configuration", configJson);
    json.addProperty("if", safeString(wod.getExecutionCondition()));
    json.addProperty("unless", safeString(wod.getSkipCondition()));
    json.addProperty("fail_workflow_on_error", wod.isFailWorkflowOnException());
    json.addProperty("error_handler_workflow", safeString(wod.getExceptionHandlingWorkflow()));
    String retryStrategy = new RetryStrategy.Adapter().marshal(wod.getRetryStrategy());
    json.addProperty("retry_strategy", safeString(retryStrategy));
    json.addProperty("max_attempts", wod.getMaxAttempts());

    return json;
  }
}
