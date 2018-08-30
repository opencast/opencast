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
import static com.entwinemedia.fn.data.json.Jsons.ZERO;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static java.time.ZoneOffset.UTC;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.elasticsearch.common.lang3.StringUtils.isNoneBlank;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponses;
import org.opencastproject.external.common.ApiVersion;
import org.opencastproject.external.impl.index.ExternalIndex;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.RetryStrategy;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workflow.api.WorkflowStateException;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_1_0 })
@RestService(name = "externalapiworkflowinstances", title = "External API Workflow Instances Service", notes = "", abstractText = "Provides resources and operations related to the workflow instances")
public class WorkflowsEndpoint {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowsEndpoint.class);

  /** Base URL of this endpoint */
  protected String endpointBaseUrl;

  /* OSGi service references */
  private WorkflowService workflowService;
  private ExternalIndex externalIndex;
  private IndexService indexService;

  /** OSGi DI */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** OSGi DI */
  public void setExternalIndex(ExternalIndex externalIndex) {
    this.externalIndex = externalIndex;
  }

  /** OSGi DI */
  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  /**
   * OSGi activation method
   */
  void activate(ComponentContext cc) {
    logger.info("Activating External API - Workflow Instances Endpoint");

    final Tuple<String, String> endpointUrl = getEndpointUrl(cc, OpencastConstants.EXTERNAL_API_URL_ORG_PROPERTY,
            RestConstants.SERVICE_PATH_PROPERTY);
    endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
    logger.debug("Configured service endpoint is {}", endpointBaseUrl);
  }

  @GET
  @Path("")
  @RestQuery(name = "getworkflowinstances", description = "Returns a list of workflow instance.", returnDescription = "", restParameters = {
          @RestParameter(name = "withoperations", description = "Whether the workflow operations should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "withconfiguration", description = "Whether the workflow configuration should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "filter", description = "A comma seperated list of filters to limit the results with. A filter is the filter's name followed by a colon \":\" and then the value to filter with so it is the form <Filter Name>:<Value to Filter With>.", isRequired = false, type = STRING),
          @RestParameter(name = "sort", description = "Sort the results based upon a list of comma seperated sorting criteria. In the comma seperated list each type of sorting is specified as a pair such as: <Sort Name>:ASC or <Sort Name>:DESC. Adding the suffix ASC or DESC sets the order as ascending or descending order and is mandatory.", isRequired = false, type = STRING),
          @RestParameter(name = "limit", description = "The maximum number of results to return for a single request.", isRequired = false, type = INTEGER),
          @RestParameter(name = "offset", description = "The index of the first result to return.", isRequired = false, type = INTEGER) }, reponses = {
          @RestResponse(description = "A (potentially empty) list of workflow instances is returned.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST) })
  public Response getWorkflowInstances(@HeaderParam("Accept") String acceptHeader,
          @QueryParam("withoperations") boolean withOperations,
          @QueryParam("withconfiguration") boolean withConfiguration, @QueryParam("filter") String filter,
          @QueryParam("sort") String sort, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
    WorkflowQuery query = new WorkflowQuery();

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

        switch (name) {
          case "state":
            try {
              query.withState(jsonToEnum(WorkflowInstance.WorkflowState.class, value));
            } catch (IllegalArgumentException e) {
              return RestUtil.R.badRequest(String.format("Invalid workflow state '%s'", value));
            }
            break;
          case "state_not":
            try {
              query.withoutState(jsonToEnum(WorkflowInstance.WorkflowState.class, value));
            } catch (IllegalArgumentException e) {
              return RestUtil.R.badRequest(String.format("Invalid workflow state '%s'", value));
            }
            break;
          case "current_operation":
            query.withCurrentOperation(value);
            break;
          case "current_operation_not":
            query.withoutCurrentOperation(value);
            break;
          case "workflow_definition_identifier":
            query.withWorkflowDefintion(value);
            break;
          case "event_identifier":
            query.withMediaPackage(value);
            break;
          case "event_title":
            query.withTitle(value);
            break;
          case "event_created":
            try {
              Tuple<Date, Date> fromAndToCreationRange = RestUtils.getFromAndToDateRange(value);
              query.withDateAfter(fromAndToCreationRange.getA());
              query.withDateBefore(fromAndToCreationRange.getB());
            } catch (Exception e) {
              return RestUtil.R.badRequest(
                      String.format("Filter 'event_created' could not be parsed: %s", e.getMessage()));
            }
            break;
          case "event_creator":
            query.withCreator(value);
            break;
          case "event_contributor":
            query.withContributor(value);
            break;
          case "event_language":
            query.withLanguage(value);
            break;
          case "event_license":
            query.withLicense(value);
            break;
          case "event_subject":
            query.withSubject(value);
            break;
          case "series_identifier":
            query.withSeriesId(value);
            break;
          case "series_title":
            query.withSeriesTitle(value);
            break;
          case "textFilter":
            query.withText(value);
            break;
          default:
            return RestUtil.R.badRequest(String.format("Unknown filter criterion in request: %s", name));
        }
      }
    }

    // Apply sort
    // TODO: this only uses the last sorting criteria
    if (StringUtils.isNoneBlank(sort)) {
      Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(sort);
      for (SortCriterion criterion : sortCriteria) {
        boolean isASC = criterion.getOrder() != SearchQuery.Order.Descending;
        switch (criterion.getFieldName()) {
          case "event_identifier":
            // FIXME: sorting by event_identifier leads to an Solr exception
            query.withSort(WorkflowQuery.Sort.MEDIA_PACKAGE_ID, isASC);
            break;
          case "event_title":
            query.withSort(WorkflowQuery.Sort.TITLE, isASC);
            break;
          case "event_created":
            query.withSort(WorkflowQuery.Sort.DATE_CREATED, isASC);
            break;
          case "event_creator":
            query.withSort(WorkflowQuery.Sort.CREATOR, isASC);
            break;
          case "event_contributor":
            query.withSort(WorkflowQuery.Sort.CONTRIBUTOR, isASC);
            break;
          case "event_language":
            query.withSort(WorkflowQuery.Sort.LANGUAGE, isASC);
            break;
          case "event_license":
            query.withSort(WorkflowQuery.Sort.LICENSE, isASC);
            break;
          case "event_subject":
            query.withSort(WorkflowQuery.Sort.SUBJECT, isASC);
            break;
          case "series_identifier":
            query.withSort(WorkflowQuery.Sort.SERIES_ID, isASC);
            break;
          case "series_title":
            query.withSort(WorkflowQuery.Sort.SERIES_TITLE, isASC);
            break;
          case "workflow_definition_identifier":
            query.withSort(WorkflowQuery.Sort.WORKFLOW_DEFINITION_ID, isASC);
            break;
          default:
            return RestUtil.R.badRequest(
                    String.format("Unknown search criterion in request: %s", criterion.getFieldName()));
        }
      }
    }

    // Apply offset
    if (offset != null && offset > 0) {
      query.withStartIndex(offset);
    }

    // Apply limit
    if (limit != null && limit > 0) {
      query.withCount(limit);
    }

    // Get results
    WorkflowSet workflowInstances;
    try {
      workflowInstances = workflowService.getWorkflowInstances(query);
    } catch (Exception e) {
      logger.error("The workflow service was not able to get the workflow instances: {}", getStackTrace(e));
      return ApiResponses.serverError("Could not retrieve workflow instances, reason: '%s'", getMessage(e));
    }

    List<JValue> json = Arrays.stream(workflowInstances.getItems())
                              .map(wi -> workflowInstanceToJSON(wi, withOperations, withConfiguration))
                              .collect(Collectors.toList());

    return ApiResponses.Json.ok(acceptHeader, arr(json));
  }

  @POST
  @Path("")
  @RestQuery(name = "createworkflowinstance", description = "Creates a workflow instance.", returnDescription = "", restParameters = {
          @RestParameter(name = "event_identifier", description = "The event identifier this workflow should run against", isRequired = true, type = STRING),
          @RestParameter(name = "workflow_definition_identifier", description = "The identifier of the workflow definition to use", isRequired = true, type = STRING),
          @RestParameter(name = "configuration", description = "The optional configuration for this workflow", isRequired = false, type = STRING),
          @RestParameter(name = "withoperations", description = "Whether the workflow operations should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "withconfiguration", description = "Whether the workflow configuration should be included in the response", isRequired = false, type = BOOLEAN), }, reponses = {
          @RestResponse(description = "A new workflow is created and its identifier is returned in the Location header.", responseCode = HttpServletResponse.SC_CREATED),
          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "The event or workflow definition could not be found.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response createWorkflowInstance(@HeaderParam("Accept") String acceptHeader,
          @FormParam("event_identifier") String eventId,
          @FormParam("workflow_definition_identifier") String workflowDefinitionIdentifier,
          @FormParam("configuration") String configuration, @QueryParam("withoperations") boolean withOperations,
          @QueryParam("withconfiguration") boolean withConfiguration) {
    if (isBlank(eventId)) {
      return RestUtil.R.badRequest("Required parameter 'event_identifier' is missing or invalid");
    }

    if (isBlank(workflowDefinitionIdentifier)) {
      return RestUtil.R.badRequest("Required parameter 'workflow_definition_identifier' is missing or invalid");
    }

    try {
      // Media Package
      Opt<Event> event = indexService.getEvent(eventId, externalIndex);
      if (event.isNone()) {
        return ApiResponses.notFound("Cannot find an event with id '%s'.", eventId);
      }
      MediaPackage mp = indexService.getEventMediapackage(event.get());

      // Workflow definition
      WorkflowDefinition wd;
      try {
        wd = workflowService.getWorkflowDefinitionById(workflowDefinitionIdentifier);
      } catch (NotFoundException e) {
        return ApiResponses.notFound("Cannot find a workflow definition with id '%s'.", workflowDefinitionIdentifier);
      }

      // Configuration
      Map<String, String> properties = new HashMap<>();
      if (isNoneBlank(configuration)) {
        JSONParser parser = new JSONParser();
        try {
          properties.putAll((JSONObject) parser.parse(configuration));
        } catch (ParseException e) {
          return RestUtil.R.badRequest("Passed parameter 'configuration' is invalid JSON.");
        }
      }

      // Start workflow
      WorkflowInstance wi = workflowService.start(wd, mp, null, properties);
      return ApiResponses.Json.created(acceptHeader, URI.create(getWorkflowUrl(wi.getId())),
              workflowInstanceToJSON(wi, withOperations, withConfiguration));
    } catch (IllegalStateException e) {
      final ApiVersion requestedVersion = ApiMediaType.parse(acceptHeader).getVersion();
      return ApiResponses.Json.conflict(requestedVersion, obj(f("message", v(getMessage(e), BLANK))));
    } catch (Exception e) {
      logger.error("Could not create workflow instances: {}", getStackTrace(e));
      return ApiResponses.serverError("Could not create workflow instances, reason: '%s'", getMessage(e));
    }
  }

  @GET
  @Path("{workflowInstanceId}")
  @RestQuery(name = "getworkflowinstance", description = "Returns a single workflow instance.", returnDescription = "", pathParameters = {
          @RestParameter(name = "workflowInstanceId", description = "The workflow instance id", isRequired = true, type = INTEGER) }, restParameters = {
          @RestParameter(name = "withoperations", description = "Whether the workflow operations should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "withconfiguration", description = "Whether the workflow configuration should be included in the response", isRequired = false, type = BOOLEAN) }, reponses = {
          @RestResponse(description = "The workflow instance is returned.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The user doesn't have the rights to make this request.", responseCode = HttpServletResponse.SC_FORBIDDEN),
          @RestResponse(description = "The specified workflow instance does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND) })
  public Response getWorkflowInstance(@HeaderParam("Accept") String acceptHeader,
          @PathParam("workflowInstanceId") Long id, @QueryParam("withoperations") boolean withOperations,
          @QueryParam("withconfiguration") boolean withConfiguration) {
    WorkflowInstance wi;
    try {
      wi = workflowService.getWorkflowById(id);
    } catch (NotFoundException e) {
      return ApiResponses.notFound("Cannot find workflow instance with id '%d'.", id);
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (Exception e) {
      logger.error("The workflow service was not able to get the workflow instance: {}", getStackTrace(e));
      return ApiResponses.serverError("Could not retrieve workflow instance, reason: '%s'", getMessage(e));
    }

    return ApiResponses.Json.ok(acceptHeader, workflowInstanceToJSON(wi, withOperations, withConfiguration));
  }

  @PUT
  @Path("{workflowInstanceId}")
  @RestQuery(name = "updateworkflowinstance", description = "Creates a workflow instance.", returnDescription = "", pathParameters = {
          @RestParameter(name = "workflowInstanceId", description = "The workflow instance id", isRequired = true, type = INTEGER) }, restParameters = {
          @RestParameter(name = "configuration", description = "The optional configuration for this workflow", isRequired = false, type = STRING),
          @RestParameter(name = "state", description = "The optional state transition for this workflow", isRequired = false, type = STRING),
          @RestParameter(name = "withoperations", description = "Whether the workflow operations should be included in the response", isRequired = false, type = BOOLEAN),
          @RestParameter(name = "withconfiguration", description = "Whether the workflow configuration should be included in the response", isRequired = false, type = BOOLEAN), }, reponses = {
          @RestResponse(description = "The workflow instance is updated.", responseCode = HttpServletResponse.SC_OK),
          @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
          @RestResponse(description = "The user doesn't have the rights to make this request.", responseCode = HttpServletResponse.SC_FORBIDDEN),
          @RestResponse(description = "The workflow instance could not be found.", responseCode = HttpServletResponse.SC_NOT_FOUND),
          @RestResponse(description = "The workflow instance cannot transition to this state.", responseCode = HttpServletResponse.SC_CONFLICT) })
  public Response updateWorkflowInstance(@HeaderParam("Accept") String acceptHeader,
          @PathParam("workflowInstanceId") Long id, @FormParam("configuration") String configuration,
          @FormParam("state") String stateStr, @QueryParam("withoperations") boolean withOperations,
          @QueryParam("withconfiguration") boolean withConfiguration) {
    try {
      boolean changed = false;
      WorkflowInstance wi = workflowService.getWorkflowById(id);

      // Configuration
      if (isNoneBlank(configuration)) {
        JSONParser parser = new JSONParser();
        try {
          Map<String, String> properties = new HashMap<>((JSONObject) parser.parse(configuration));

          // Remove old configuration
          wi.getConfigurationKeys().forEach(wi::removeConfiguration);
          // Add new configuration
          properties.forEach(wi::setConfiguration);

          changed = true;
        } catch (ParseException e) {
          return RestUtil.R.badRequest("Passed parameter 'configuration' is invalid JSON.");
        }
      }

      // TODO: does it make sense to change the media package?

      if (changed) {
        workflowService.update(wi);
      }

      // State change
      if (isNoneBlank(stateStr)) {
        WorkflowInstance.WorkflowState state;
        try {
          state = jsonToEnum(WorkflowInstance.WorkflowState.class, stateStr);
        } catch (IllegalArgumentException e) {
          return RestUtil.R.badRequest(String.format("Invalid workflow state '%s'", stateStr));
        }

        WorkflowInstance.WorkflowState currentState = wi.getState();
        if (state != currentState) {
          // Allowed transitions:
          //
          //   instantiated -> paused, stopped, running
          //   running      -> paused, stopped
          //   failing      -> paused, stopped
          //   paused       -> paused, stopped, running
          //   succeeded    -> paused, stopped
          //   stopped      -> paused, stopped
          //   failed       -> paused, stopped
          switch (state) {
            case PAUSED:
              workflowService.suspend(wi.getId());
              break;
            case STOPPED:
              workflowService.stop(wi.getId());
              break;
            case RUNNING:
              if (currentState == WorkflowInstance.WorkflowState.INSTANTIATED
                      || currentState == WorkflowInstance.WorkflowState.PAUSED) {
                workflowService.resume(wi.getId());
              } else {
                return RestUtil.R.conflict(
                        String.format("Cannot resume from workflow state '%s'", currentState.toString().toLowerCase()));
              }
              break;
            default:
              return RestUtil.R.conflict(
                      String.format("Cannot transition state from '%s' to '%s'", currentState.toString().toLowerCase(),
                              stateStr));
          }
        }
      }

      wi = workflowService.getWorkflowById(id);
      return ApiResponses.Json.ok(acceptHeader, workflowInstanceToJSON(wi, withOperations, withConfiguration));
    } catch (NotFoundException e) {
      return ApiResponses.notFound("Cannot find workflow instance with id '%d'.", id);
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (Exception e) {
      logger.error("The workflow service was not able to get the workflow instance: {}", getStackTrace(e));
      return ApiResponses.serverError("Could not retrieve workflow instance, reason: '%s'", getMessage(e));
    }
  }

  @DELETE
  @Path("{workflowInstanceId}")
  @RestQuery(name = "deleteworkflowinstance", description = "Deletes a workflow instance.", returnDescription = "", pathParameters = {
          @RestParameter(name = "workflowInstanceId", description = "The workflow instance id", isRequired = true, type = INTEGER) }, reponses = {
          @RestResponse(description = "The workflow instance has been deleted.", responseCode = HttpServletResponse.SC_NO_CONTENT),
          @RestResponse(description = "The user doesn't have the rights to make this request.", responseCode = HttpServletResponse.SC_FORBIDDEN),
          @RestResponse(description = "The specified workflow instance does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND),
          @RestResponse(description = "The workflow instance cannot be deleted in this state.", responseCode = HttpServletResponse.SC_CONFLICT) })
  public Response deleteWorkflowInstance(@HeaderParam("Accept") String acceptHeader,
          @PathParam("workflowInstanceId") Long id) {
    try {
      workflowService.remove(id);
    } catch (WorkflowStateException e) {
      return RestUtil.R.conflict("Cannot delete workflow instance in this workflow state");
    } catch (NotFoundException e) {
      return ApiResponses.notFound("Cannot find workflow instance with id '%d'.", id);
    } catch (UnauthorizedException e) {
      return Response.status(Response.Status.FORBIDDEN).build();
    } catch (Exception e) {
      logger.error("Could not delete workflow instances: {}", getStackTrace(e));
      return ApiResponses.serverError("Could not delete workflow instances, reason: '%s'", getMessage(e));
    }

    return Response.noContent().build();
  }

  private JValue workflowInstanceToJSON(WorkflowInstance wi, boolean withOperations, boolean withConfiguration) {
    List<Field> fields = new ArrayList<>();

    fields.add(f("identifier", v(wi.getId())));
    fields.add(f("title", v(wi.getTitle(), BLANK)));
    fields.add(f("description", v(wi.getDescription(), BLANK)));
    fields.add(f("workflow_definition_identifier", v(wi.getTemplate(), BLANK)));
    fields.add(f("event_identifier", v(wi.getMediaPackage().getIdentifier().toString())));
    fields.add(f("creator", v(wi.getCreator().getName())));
    fields.add(f("state", enumToJSON(wi.getState())));
    if (withOperations) {
      fields.add(f("operations", arr(wi.getOperations()
                                       .stream()
                                       .map(this::workflowOperationInstanceToJSON)
                                       .collect(Collectors.toList()))));
    }
    if (withConfiguration) {
      fields.add(f("configuration", obj(wi.getConfigurationKeys()
                                          .stream()
                                          .map(key -> f(key, wi.getConfiguration(key)))
                                          .collect(Collectors.toList()))));
    }

    return obj(fields);
  }

  private JValue workflowOperationInstanceToJSON(WorkflowOperationInstance woi) {
    List<Field> fields = new ArrayList<>();
    DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;

    // The job ID can be null if the workflow was just created
    fields.add(f("identifier", v(woi.getId(), BLANK)));
    fields.add(f("operation", v(woi.getTemplate())));
    fields.add(f("description", v(woi.getDescription(), BLANK)));
    fields.add(f("state", enumToJSON(woi.getState())));
    fields.add(f("time_in_queue", v(woi.getTimeInQueue(), ZERO)));
    fields.add(f("host", v(woi.getExecutionHost(), BLANK)));
    fields.add(f("if", v(woi.getExecutionCondition(), BLANK)));
    fields.add(f("unless", v(woi.getSkipCondition(), BLANK)));
    fields.add(f("fail_workflow_on_error", v(woi.isFailWorkflowOnException())));
    fields.add(f("error_handler_workflow", v(woi.getExceptionHandlingWorkflow(), BLANK)));
    fields.add(f("retry_strategy", v(new RetryStrategy.Adapter().marshal(woi.getRetryStrategy()), BLANK)));
    fields.add(f("max_attempts", v(woi.getMaxAttempts())));
    fields.add(f("failed_attempts", v(woi.getFailedAttempts())));
    fields.add(f("configuration", obj(woi.getConfigurationKeys()
                                         .stream()
                                         .map(key -> f(key, woi.getConfiguration(key)))
                                         .collect(Collectors.toList()))));
    if (woi.getDateStarted() != null) {
      fields.add(f("start", v(dateFormatter.format(woi.getDateStarted().toInstant().atZone(UTC)))));
    } else {
      fields.add(f("start", BLANK));
    }
    if (woi.getDateCompleted() != null) {
      fields.add(f("completion", v(dateFormatter.format(woi.getDateCompleted().toInstant().atZone(UTC)))));
    } else {
      fields.add(f("completion", BLANK));
    }

    return obj(fields);
  }

  private JValue enumToJSON(Enum e) {
    return e == null ? null : v(e.toString().toLowerCase());
  }

  private <T extends Enum<T>> T jsonToEnum(Class<T> enumType, String name) {
    return Enum.valueOf(enumType, name.toUpperCase());
  }

  private String getWorkflowUrl(long workflowInstanceId) {
    return UrlSupport.concat(endpointBaseUrl, Long.toString(workflowInstanceId));
  }
}
