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
package org.opencastproject.external.endpoint;

import static com.entwinemedia.fn.data.json.Jsons.BLANK;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.matterhorn.search.SearchQuery.Order.Descending;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.RetryStrategy;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowOperationDefinition;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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

@Path("/")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_1_0 })
@RestService(name = "externalapiworkflowdefinitions", title = "External API Workflow Definitions Service", notes = "", abstractText = "Provides resources and operations related to the workflow definitions")
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
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /**
   * OSGi activation method
   */
  void activate(ComponentContext cc) {
    logger.info("Activating External API - Workflow Definitions Endpoint");
  }

  @GET
  @Path("/")
  @RestQuery(name = "getworkflowdefinitions", description = "Returns a list of workflow definition.", returnDescription = "", restParameters = {
          @RestParameter(name = "withoperations", description = "Whether the workflow operations should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "withconfigurationpanel", description = "Whether the workflow configuration panel should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "filter", description = "A comma seperated list of filters to limit the results with. A filter is the filter's name followed by a colon \":\" and then the value to filter with so it is the form <Filter Name>:<Value to Filter With>.", isRequired = false, type = STRING),
          @RestParameter(name = "sort", description = "Sort the results based upon a list of comma seperated sorting criteria. In the comma seperated list each type of sorting is specified as a pair such as: <Sort Name>:ASC or <Sort Name>:DESC. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory.", isRequired = false, type = STRING),
          @RestParameter(name = "limit", description = "The maximum number of results to return for a single request.", isRequired = false, type = INTEGER),
          @RestParameter(name = "offset", description = "The index of the first result to return.", isRequired = false, type = INTEGER) }, reponses = {
          @RestResponse(description = "A (potentially empty) list of workflow definitions is returned.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response getWorkflowDefinitions(@HeaderParam("Accept") String acceptHeader,
          @QueryParam("withoperations") boolean withOperations,
          @QueryParam("withconfigurationpanel") boolean withConfigurationPanel, @QueryParam("filter") String filter,
          @QueryParam("sort") String sort, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
    Stream<WorkflowDefinition> workflowDefinitions;
    try {
      workflowDefinitions = workflowService.listAvailableWorkflowDefinitions().stream();
    } catch (WorkflowDatabaseException e) {
      logger.error("The workflow service was not able to get the workflow definitions: {}", getStackTrace(e));
      return ApiResponses.serverError("Could not retrieve workflow definitions, reason: '%s'", getMessage(e));
    }

    // Apply filter
    if (StringUtils.isNotBlank(filter)) {
      for (String f : filter.split(",")) {
        int sepIdx = f.indexOf(':');
        if (sepIdx < 0 || sepIdx == f.length() - 1) {
          logger.info("No value for filter {} in filters list: {}", f, filter);
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
      Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(sort);
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

    List<JValue> json = workflowDefinitions.map(
            wd -> workflowDefinitionToJSON(wd, withOperations, withConfigurationPanel)).collect(Collectors.toList());

    return ApiResponses.Json.ok(acceptHeader, arr(json));
  }

  @GET
  @Path("{workflowDefinitionId}")
  @RestQuery(name = "getworkflowdefinition", description = "Returns a single workflow definition.", returnDescription = "", pathParameters = {
          @RestParameter(name = "workflowDefinitionId", description = "The workflow definition id", isRequired = true, type = STRING) }, restParameters = {
          @RestParameter(name = "withoperations", description = "Whether the workflow operations should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "withconfigurationpanel", description = "Whether the workflow configuration panel should be included in the response", isRequired = false, type = BOOLEAN) }, reponses = {
          @RestResponse(description = "The workflow definition is returned.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The specified workflow definition does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getWorkflowDefinition(@HeaderParam("Accept") String acceptHeader,
          @PathParam("workflowDefinitionId") String id, @QueryParam("withoperations") boolean withOperations,
          @QueryParam("withconfigurationpanel") boolean withConfigurationPanel) throws Exception {
    WorkflowDefinition wd;
    try {
      wd = workflowService.getWorkflowDefinitionById(id);
    } catch (NotFoundException e) {
      return ApiResponses.notFound("Cannot find workflow definition with id '%s'.", id);
    }

    return ApiResponses.Json.ok(acceptHeader, workflowDefinitionToJSON(wd, withOperations, withConfigurationPanel));
  }

  private JValue workflowDefinitionToJSON(WorkflowDefinition wd, boolean withOperations,
          boolean withConfigurationPanel) {
    List<Field> fields = new ArrayList<>();

    fields.add(f("identifier", v(wd.getId())));
    fields.add(f("title", v(wd.getTitle(), BLANK)));
    fields.add(f("description", v(wd.getDescription(), BLANK)));
    fields.add(f("tags", arr(Arrays.stream(wd.getTags()).map(Jsons::v).collect(Collectors.toList()))));
    if (withConfigurationPanel) {
      fields.add(f("configuration_panel", v(wd.getConfigurationPanel(), BLANK)));
    }
    if (withOperations) {
      fields.add(f("operations", arr(wd.getOperations()
                                       .stream()
                                       .map(this::workflowOperationDefinitionToJSON)
                                       .collect(Collectors.toList()))));
    }

    return obj(fields);
  }

  private JValue workflowOperationDefinitionToJSON(WorkflowOperationDefinition wod) {
    List<Field> fields = new ArrayList<>();

    fields.add(f("operation", v(wod.getId())));
    fields.add(f("description", v(wod.getDescription(), BLANK)));
    fields.add(f("configuration", obj(wod.getConfigurationKeys()
                                         .stream()
                                         .map(key -> f(key, wod.getConfiguration(key)))
                                         .collect(Collectors.toList()))));
    fields.add(f("if", v(wod.getExecutionCondition(), BLANK)));
    fields.add(f("unless", v(wod.getSkipCondition(), BLANK)));
    fields.add(f("fail_workflow_on_error", v(wod.isFailWorkflowOnException())));
    fields.add(f("error_handler_workflow", v(wod.getExceptionHandlingWorkflow(), BLANK)));
    fields.add(f("retry_strategy", v(new RetryStrategy.Adapter().marshal(wod.getRetryStrategy()), BLANK)));
    fields.add(f("max_attempts", v(wod.getMaxAttempts())));

    return obj(fields);
  }
}
