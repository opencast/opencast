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

import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.index.service.util.JSONUtils.safeString;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQueryField;
import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponseBuilder;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.lifecyclemanagement.api.Action;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicy;
import org.opencastproject.lifecyclemanagement.api.LifeCyclePolicyAccessControlEntry;
import org.opencastproject.lifecyclemanagement.api.LifeCycleService;
import org.opencastproject.lifecyclemanagement.api.StartWorkflowParameters;
import org.opencastproject.lifecyclemanagement.api.TargetType;
import org.opencastproject.lifecyclemanagement.api.Timing;
import org.opencastproject.lifecyclemanagement.impl.LifeCyclePolicyAccessControlEntryImpl;
import org.opencastproject.lifecyclemanagement.impl.LifeCyclePolicyImpl;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.rest.RestConstants;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.systems.OpencastConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.requests.SortCriterion;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@Path("/api/lifecyclemanagement")
@Produces({ ApiMediaType.JSON, ApiMediaType.VERSION_1_12_0 })
@RestService(
    name = "externalapilifecyclemanagement",
    title = "External API LifeCycle Management Service",
    notes = {},
    abstractText = "Manage life cycle policies"
)
@Component(
    immediate = true,
    service = LifeCycleManagementEndpoint.class,
    property = {
        "service.description=External API - LifeCycle Management Endpoint",
        "opencast.service.type=org.opencastproject.external.lifecyclemanagement",
        "opencast.service.path=/api/lifecyclemanagement"
    }
)
@JaxrsResource
public class LifeCycleManagementEndpoint {

    /** The logging facility */
    private static final Logger logger = LoggerFactory.getLogger(LifeCycleManagementEndpoint.class);
    private static final Gson gson = new Gson();

    /** Base URL of this endpoint */
    protected String endpointBaseUrl;

    /** The lifecycle service */
    private LifeCycleService service;

    protected IndexService indexService;

    @Reference
    public void setLifeCycleService(LifeCycleService lifeCycleService) {
        this.service = lifeCycleService;
    }

    @Reference
    public void setIndexService(IndexService indexService) {
        this.indexService = indexService;
    }

    private final String actionParametersExampleJSON = "{\n"
        + "  workflowId: noop,\n"
        + "  workflowParameters: {\n"
        + "    straightToPublishing: true\n"
        + "  }\n"
        + "}";

    /** OSGi activation method */
    @Activate
    void activate(ComponentContext cc) {
        logger.info("Activating External API - LifeCycle Management Endpoint");

        final Tuple<String, String> endpointUrl = getEndpointUrl(cc, OpencastConstants.EXTERNAL_API_URL_ORG_PROPERTY,
            RestConstants.SERVICE_PATH_PROPERTY);
        endpointBaseUrl = UrlSupport.concat(endpointUrl.getA(), endpointUrl.getB());
    }

    @GET
    @Path("policies/{id}")
    @RestQuery(
        name = "lifeCyclePolicy",
        description = "Get a lifecycle policy.",
        returnDescription = "A lifecycle policy as JSON",
        pathParameters = {
            @RestParameter(name = "id", isRequired = true, description = "The lifecycle policy identifier", type = STRING),
        },
        responses = {
            @RestResponse(description = "Returns the lifecycle policy.", responseCode = HttpServletResponse.SC_OK),
            @RestResponse(description = "The specified lifecycle policy instance does not exist.", responseCode = HttpServletResponse.SC_NOT_FOUND),
            @RestResponse(description = "The user doesn't have the rights to make this request.", responseCode = HttpServletResponse.SC_FORBIDDEN),
            @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
        })
    public Response getPolicy(
        @HeaderParam("Accept") String acceptHeader,
        @PathParam("id") String id) {
        try {
            LifeCyclePolicy policy = service.getLifeCyclePolicyById(id);

            if (!policy.getCronTrigger().isEmpty()) {
                CronParser unixParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
                Cron cron = unixParser.parse(policy.getCronTrigger());
                CronMapper cronMapper = CronMapper.fromQuartzToUnix();
                Cron cron4jCron = cronMapper.map(cron);
                policy.setCronTrigger(cron4jCron.asString());
            }

            return ApiResponseBuilder.Json.ok(acceptHeader, policyToJson(policy));
        } catch (NotFoundException e) {
            return ApiResponseBuilder.notFound("Cannot find playlist instance with id '%s'.", id);
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("policies")
    @RestQuery(
        name = "policies",
        description = "Get policies. Policies that you do not have read access to will not show up.",
        returnDescription = "A JSON object containing an array.",
        restParameters = {
            @RestParameter(name = "limit", isRequired = false, type = INTEGER,
                description = "The maximum number of results to return for a single request.", defaultValue = "100"),
            @RestParameter(name = "offset", isRequired = false, type = INTEGER,
                description = "The index of the first result to return."),
            @RestParameter(name = "sort", isRequired = false, type = STRING,
                description = "Sort the results based upon a sorting criteria. A criteria is specified as a pair such as:"
                    + "<Sort Name>:ASC or <Sort Name>:DESC. Adding the suffix ASC or DESC sets the order as ascending or"
                    + "descending order and is mandatory. Sort Name is case sensitive. Supported Sort Names are 'title'"
                , defaultValue = "title:ASC"),
        },
        responses = {
            @RestResponse(description = "Returns the playlist.", responseCode = HttpServletResponse.SC_OK),
            @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
        })
    public Response getPoliciesAsJson(
        @HeaderParam("Accept") String acceptHeader,
        @QueryParam("limit") int limit,
        @QueryParam("offset") int offset,
        @QueryParam("sort") String sort) {
        if (offset < 0) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (limit < 1) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        SortCriterion sortCriterion = new SortCriterion("", SortCriterion.Order.None);
        Option<String> optSort = Option.option(trimToNull(sort));
        if (optSort.isSome()) {
            sortCriterion = SortCriterion.parse(optSort.get());

            switch (sortCriterion.getFieldName()) {
                case "title":
                    break;
                default:
                    logger.info("Unknown sort criteria {}", sortCriterion.getFieldName());
                    return Response.serverError().status(Response.Status.BAD_REQUEST).build();
            }
        }
        List<LifeCyclePolicy> policies = service.getLifeCyclePolicies(limit, offset, sortCriterion);

        long total = service.getLifeCyclePoliciesTotal();

        JsonArray policiesJsonArray = new JsonArray();
        for (LifeCyclePolicy policy : policies) {
            policiesJsonArray.add(policyToJson(policy));
        }

        JsonObject resultJson = new JsonObject();
        resultJson.addProperty("total", total);
        resultJson.addProperty("limit", limit);
        resultJson.addProperty("offset", offset);
        resultJson.add("results", policiesJsonArray);

        // Return the response
        return ApiResponseBuilder.Json.ok(acceptHeader, resultJson);
    }

    @GET
    @Path("policies/actions")
    @RestQuery(
        name = "actions",
        description = "Get lifecycle policy actions.",
        returnDescription = "Lifecycle policy actions as JSON array",
        responses = {
            @RestResponse(description = "Returns the lifecycle policy actions.", responseCode = HttpServletResponse.SC_OK),
        })
    public Response getActions(@HeaderParam("Accept") String acceptHeader) {
        JsonArray actionsJson = new JsonArray();
        for (Action action : Action.values()) {
            actionsJson.add(new JsonPrimitive(action.toString()));
        }

        return ApiResponseBuilder.Json.ok(acceptHeader, actionsJson);
    }

    @GET
    @Path("policies/targettypes")
    @RestQuery(
        name = "targettypes",
        description = "Get lifecycle policy targettypes.",
        returnDescription = "Lifecycle policy targettypes as JSON array",
        responses = {
            @RestResponse(description = "Returns the lifecycle policy targettypes.", responseCode = HttpServletResponse.SC_OK),
        })
    public Response getTargetTypes(@HeaderParam("Accept") String acceptHeader) {
        JsonArray targetTypesJson = new JsonArray();
        for (TargetType type : TargetType.values()) {
            targetTypesJson.add(new JsonPrimitive(type.toString()));
        }

        return ApiResponseBuilder.Json.ok(acceptHeader, targetTypesJson);
    }

    @GET
    @Path("policies/timings")
    @RestQuery(
        name = "timings",
        description = "Get lifecycle policy timings.",
        returnDescription = "Lifecycle policy timings as JSON array",
        responses = {
            @RestResponse(description = "Returns the lifecycle policy timings.", responseCode = HttpServletResponse.SC_OK),
        })
    public Response getTimings(@HeaderParam("Accept") String acceptHeader) {
        JsonArray timingsJson = new JsonArray();
        for (Timing timing : Timing.values()) {
            timingsJson.add(new JsonPrimitive(timing.toString()));
        }

        return ApiResponseBuilder.Json.ok(acceptHeader, timingsJson);
    }

    @GET
    @Path("policiesForEvent/{eventId}")
    @RestQuery(
        name = "policiesForEvent",
        description = "Get active lifecycle policies that would target the given event were they to be executed now.",
        returnDescription = "Lifecycle policies",
        pathParameters = {
            @RestParameter(name = "eventId", isRequired = true, description = "The lifecycle policy identifier", type = STRING),
        },
        responses = {
            @RestResponse(description = "Returns the lifecycle policies.", responseCode = HttpServletResponse.SC_OK),
        })
    public Response getPoliciesForEvent(@HeaderParam("Accept") String acceptHeader, @PathParam("eventId") String eventId)
        throws SearchIndexException, NotFoundException {
        // Create a filter with the event id
        var eventFilter = new EventSearchQueryField(eventId);

        // Get policies
        var lifeCyclePolicies = service.getActiveLifeCyclePolicies();

        // Run filters
        List<LifeCyclePolicy> policiesForEvent = new ArrayList<>();
        for (var policy : lifeCyclePolicies) {
            var targetFilters = policy.getTargetFilters();
            String catalogFlavor = indexService.getCommonEventCatalogUIAdapter().getFlavor().toString();
            targetFilters
                .computeIfAbsent(catalogFlavor, k -> new HashMap<>())
                .put("uid", eventFilter);
            var events = service.filterForEvents(targetFilters);
            if (!events.isEmpty()) {
                policiesForEvent.add(policy);
            }
        }

        JsonArray policiesJsonArray = new JsonArray();
        for (LifeCyclePolicy policy : policiesForEvent) {
            policiesJsonArray.add(policyToJson(policy));
        }
        return ApiResponseBuilder.Json.ok(acceptHeader, policiesJsonArray);
    }

    @POST
    @Path("policies")
    @RestQuery(
        name = "create",
        description = "Creates a lifecycle policy.",
        returnDescription = "The created lifecycle policy.",
        restParameters = {
            @RestParameter(name = "title", isRequired = true, description = "Policy Title", type = STRING),
            @RestParameter(name = "targetType", description = "EVENT, SERIES", isRequired = true, type = STRING,
                defaultValue = "EVENT"),
            @RestParameter(name = "action", description = "START_WORKFLOW", isRequired = true, type = STRING,
                defaultValue = "START_WORKFLOW"),
            @RestParameter(name = "actionParameters", description = "Depend entirely on the chosen action. JSON. To find how to structure your JSON check the documentation.", isRequired = false, type = TEXT,
                defaultValue = actionParametersExampleJSON),
            @RestParameter(name = "actionDate", description = "Required if timing is SPECIFIC_DATE. E.g. 2023-11-30T16:16:47Z", isRequired = false, type = STRING),
            @RestParameter(name = "cronTrigger", description = "Required if timing is REPEATING. https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html", isRequired = false, type = STRING),
            @RestParameter(name = "timing", description = "SPECIFIC_DATE, REPEATING, ALWAYS", isRequired = true, type = STRING,
                defaultValue = "SPECIFIC_DATE"),
            @RestParameter(name = "targetFilters", isRequired = false, description = "Used to select applicable entities. JSON. To find how to structure your JSON check the documentation.", type = TEXT),
            @RestParameter(name = "accessControlEntries", description = "Which user have what permissions on this policy. JSON. To find how to structure your JSON check the documentation.", isRequired = false, type = TEXT,
                defaultValue = ""),
        },
        responses = {
            @RestResponse(description = "Policy created.", responseCode = HttpServletResponse.SC_CREATED),
            @RestResponse(description = "The user doesn't have the rights to make this request.", responseCode = HttpServletResponse.SC_FORBIDDEN),
            @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
        })
    public Response createAsJson(
        @HeaderParam("Accept") String acceptHeader,
        @FormParam("title") String title,
        @FormParam("targetType") String targetType,
        @FormParam("action") String action,
        @FormParam("actionParameters") String actionParameters,
        @FormParam("actionDate") String actionDate,
        @FormParam("cronTrigger") String cronTrigger,
        @FormParam("timing") String timing,
        @FormParam("targetFilters") String targetFilters,
        @FormParam("accessControlEntries") String accessControlEntries
    ) {
        try {
            // Check if required fields are present
            if (title == null || title.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (targetType == null || targetType.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (action == null || action.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            if (timing == null || timing.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            // Check if conditionally required fields are present
            if (Timing.valueOf(timing) == Timing.SPECIFIC_DATE) {
                if (actionDate == null || actionDate.isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }
            if (Timing.valueOf(timing) == Timing.REPEATING) {
                if (cronTrigger == null || cronTrigger.isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }

            // Check if cron string is valid
            if (cronTrigger != null && !cronTrigger.isEmpty()) {
                CronParser unixParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
                Cron cron = unixParser.parse(cronTrigger);
                CronMapper cronMapper = CronMapper.fromUnixToQuartz();
                Cron cron4jCron = cronMapper.map(cron);
                cronTrigger = cron4jCron.asString();

                if (!org.quartz.CronExpression.isValidExpression(cronTrigger)) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }

            // Check if action parameters are well formed
            try {
                if (Action.valueOf(action) == Action.START_WORKFLOW) {
                    StartWorkflowParameters actionParametersParsed = gson.fromJson(actionParameters,
                        StartWorkflowParameters.class);
                    if (actionParametersParsed.getWorkflowId() == null) {
                        return Response.status(Response.Status.BAD_REQUEST).build();
                    }
                }
            } catch (JsonSyntaxException e) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            // Check if ACL is well formed
            List<LifeCyclePolicyAccessControlEntryImpl> accessControlEntriesParsed = new ArrayList<>();
            if (accessControlEntries != null && !accessControlEntries.isEmpty()) {
                try {
                    accessControlEntriesParsed = gson.fromJson(accessControlEntries,
                        new TypeToken<List<LifeCyclePolicyAccessControlEntryImpl>>() { }.getType());
                    if (accessControlEntriesParsed == null) {
                        accessControlEntriesParsed = new ArrayList<>();
                    }
                } catch (JsonSyntaxException e) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }

            // Convert filters
            Map<String, Map<String, EventSearchQueryField<String>>> filtersMap = new HashMap<>();
            if (targetFilters != null && !targetFilters.isEmpty()) {
                try {
                    filtersMap = gson.fromJson(targetFilters,
                        new TypeToken<Map<String, Map<String, EventSearchQueryField<String>>> >() { }.getType());
                    if (filtersMap == null) {
                        filtersMap = new HashMap<>();
                    }
                } catch (JsonSyntaxException e) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }

            LifeCyclePolicy policy = new LifeCyclePolicyImpl(
                title,
                TargetType.valueOf(targetType),
                Action.valueOf(action),
                actionParameters,
                EncodingSchemeUtils.decodeDate(actionDate),
                cronTrigger,
                Timing.valueOf(timing),
                filtersMap,
                accessControlEntriesParsed
            );

            service.createLifeCyclePolicy(policy);
            return Response.status(Response.Status.CREATED).build();
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @PUT
    @Path("policies/{id}")
    @RestQuery(
        name = "update",
        description = "Updates a lifecycle policy.",
        returnDescription = "The updated lifecycle policy.",
        pathParameters = {
            @RestParameter(name = "id", isRequired = true, description = "Policy identifier", type = STRING)
        },
        restParameters = {
            @RestParameter(name = "title", isRequired = false, description = "Policy Title", type = STRING),
            @RestParameter(name = "targetType", description = "EVENT, SERIES", isRequired = false, type = STRING),
            @RestParameter(name = "action", description = "START_WORKFLOW", isRequired = false, type = STRING),
            @RestParameter(name = "actionParameters", description = "Depend entirely on the chosen action. JSON. To find how to structure your JSON check the documentation.", isRequired = false, type = TEXT),
            @RestParameter(name = "actionDate", description = "Required if timing is SPECIFIC_DATE. E.g. 2023-11-30T16:16:47Z", isRequired = false, type = STRING),
            @RestParameter(name = "cronTrigger", description = "Required if timing is REPEATING. https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html", isRequired = false, type = STRING),
            @RestParameter(name = "timing", description = "SPECIFIC_DATE, REPEATING, ALWAYS", isRequired = false, type = STRING),
            @RestParameter(name = "targetFilters", isRequired = false, description = "The filter(s) used to select applicable entities. Format: 'filter1:value1,filter2:value2'", type = TEXT),
            @RestParameter(name = "accessControlEntries", description = "JSON. To find how to structure your JSON check the documentation.", isRequired = false, type = TEXT),
        },
        responses = {
            @RestResponse(description = "Policy updated.", responseCode = HttpServletResponse.SC_OK),
            @RestResponse(description = "The user doesn't have the rights to make this request.", responseCode = HttpServletResponse.SC_FORBIDDEN),
            @RestResponse(description = "The request is invalid or inconsistent.", responseCode = HttpServletResponse.SC_BAD_REQUEST),
        })
    public Response updateAsJson(
        @HeaderParam("Accept") String acceptHeader,
        @PathParam("id") String id,
        @FormParam("title") String title,
        @FormParam("targetType") String targetType,
        @FormParam("action") String action,
        @FormParam("actionParameters") String actionParameters,
        @FormParam("actionDate") String actionDate,
        @FormParam("cronTrigger") String cronTrigger,
        @FormParam("timing") String timing,
        @FormParam("targetFilters") String targetFilters,
        @FormParam("accessControlEntries") String accessControlEntries
    ) {
        try {
            LifeCyclePolicy policy = service.getLifeCyclePolicyById(id);

            List<String> actionParametersList = new ArrayList<>();
            if (StringUtils.isNotBlank(actionParameters)) {
                for (String actionParameter : StringUtils.split(actionParameters, ",")) {
                    actionParametersList.add(actionParameter);
                }
            }

            if (title != null && !title.isEmpty()) {
                policy.setTitle(title);
            }
            if (targetType != null && !targetType.isEmpty()) {
                policy.setTargetType(TargetType.valueOf(targetType));
            }
            if (action != null && !action.isEmpty()) {
                policy.setAction(Action.valueOf(action));
            }
            if (actionParametersList != null && !actionParametersList.isEmpty()) {
                // Check if action parameters are well formed
                try {
                    if (Action.valueOf(action) == Action.START_WORKFLOW) {
                        StartWorkflowParameters actionParametersParsed = gson.fromJson(actionParameters,
                            StartWorkflowParameters.class);
                        if (actionParametersParsed.getWorkflowId() == null) {
                            return Response.status(Response.Status.BAD_REQUEST).build();
                        }
                    }
                    policy.setActionParameters(actionParameters);
                } catch (JsonSyntaxException e) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }
            if (actionDate != null && !actionDate.isEmpty()) {
                policy.setActionDate(EncodingSchemeUtils.decodeDate(actionDate));
            }
            if (cronTrigger != null && !cronTrigger.isEmpty()) {
                CronParser unixParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
                Cron cron = unixParser.parse(cronTrigger);
                CronMapper cronMapper = CronMapper.fromUnixToQuartz();
                Cron cron4jCron = cronMapper.map(cron);
                cronTrigger = cron4jCron.asString();

                policy.setCronTrigger(cronTrigger);
            }
            if (timing != null && !timing.isEmpty()) {
                policy.setTiming(Timing.valueOf(timing));
            }
            if (targetFilters != null && !targetFilters.isEmpty()) {
                Map<String, Map<String, EventSearchQueryField<String>>>  filtersMap;
                if (targetFilters != null && !targetFilters.isEmpty()) {
                    try {
                        filtersMap = gson.fromJson(targetFilters,
                            new TypeToken<Map<String, Map<String, EventSearchQueryField<String>>>>() { }.getType());
                        if (filtersMap == null) {
                            filtersMap = new HashMap<>();
                        }
                        policy.setTargetFilters(filtersMap);
                    } catch (JsonSyntaxException e) {
                        return Response.status(Response.Status.BAD_REQUEST).build();
                    }
                }
            }
            if (accessControlEntries != null && !accessControlEntries.isEmpty()) {
                // Check if ACL is well formed
                try {
                    List<LifeCyclePolicyAccessControlEntry> accessControlEntriesParsed = gson.fromJson(accessControlEntries,
                        new TypeToken<List<LifeCyclePolicyAccessControlEntryImpl>>() { }.getType());
                    if (accessControlEntriesParsed == null) {
                        accessControlEntriesParsed = new ArrayList<>();
                    }
                    policy.setAccessControlEntries(accessControlEntriesParsed);
                } catch (JsonSyntaxException e) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
            }

            service.updateLifeCyclePolicy(policy);
            return Response.status(Response.Status.OK).build();
        } catch (NotFoundException e) {
            return ApiResponseBuilder.notFound("Cannot find playlist instance with id '%s'.", id);
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @DELETE
    @Path("policies/{id}")
    @RestQuery(
        name = "remove",
        description = "Removes a lifecycle policy.",
        returnDescription = "The removed lifecycle policy.",
        pathParameters = {
            @RestParameter(name = "id", isRequired = true, description = "Policy identifier", type = STRING)
        },
        responses = {
            @RestResponse(responseCode = HttpServletResponse.SC_OK, description = "Policy removed."),
            @RestResponse(responseCode = HttpServletResponse.SC_NOT_FOUND, description = "No lifecycle policy with that identifier exists."),
            @RestResponse(responseCode = HttpServletResponse.SC_UNAUTHORIZED, description = "Not authorized to perform this action")
        })
    public Response remove(
        @HeaderParam("Accept") String acceptHeader,
        @PathParam("id") String id) {
        try {
            service.deleteLifeCyclePolicy(id);
            return Response.status(Response.Status.OK).build();
        } catch (NotFoundException e) {
            return ApiResponseBuilder.notFound("Cannot find policy instance with id '%s'.", id);
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    private JsonObject policyToJson(LifeCyclePolicy policy) {
        JsonObject json = new JsonObject();

        json.addProperty("id", policy.getId());
        json.addProperty("title", safeString(policy.getTitle()));
        json.addProperty("targetType", enumToJson(policy.getTargetType()));
        json.addProperty("action", enumToJson(policy.getAction()));

        if (policy.getAction() == Action.START_WORKFLOW) {
            json.addProperty("actionParameters", startWorkflowParametersToJson(policy.getActionParameters()).toString());
        } else {
            json.addProperty("actionParameters", safeString(policy.getActionParameters()));
        }

        json.addProperty("actionDate", policy.getActionDate() != null ? toUTC(policy.getActionDate().getTime()) : "");
        json.addProperty("cronTrigger", safeString(policy.getCronTrigger()));
        json.addProperty("timing", enumToJson(policy.getTiming()));
        json.addProperty("isActive", policy.isActive());
        json.addProperty("isCreatedFromConfig", policy.isCreatedFromConfig());

        json.addProperty("targetFilters", gson.toJson(policy.getTargetFilters()));


        JsonArray accessControlEntries = new JsonArray();
        for (LifeCyclePolicyAccessControlEntry entry : policy.getAccessControlEntries()) {
            accessControlEntries.add(policyAccessControlEntryToJson(entry));
        }
        json.add("accessControlEntries", accessControlEntries);

        return json;
    }

    private JsonObject policyAccessControlEntryToJson(LifeCyclePolicyAccessControlEntry entry) {
        JsonObject json = new JsonObject();
        json.addProperty("id", entry.getId());
        json.addProperty("allow", entry.isAllow());
        json.addProperty("role", entry.getRole());
        json.addProperty("action", entry.getAction());
        return json;
    }

    private JsonObject eventSearchQueryFieldToJson(EventSearchQueryField field) {
        JsonObject json = new JsonObject();
        json.addProperty("value", safeString(field.getValue()));
        json.addProperty("type", safeString(field.getType()));
        json.addProperty("must", field.isMust());
        return json;
    }

    private JsonObject startWorkflowParametersToJson(String jsonStr) {
        StartWorkflowParameters parameters = gson.fromJson(jsonStr, StartWorkflowParameters.class);

        JsonObject json = new JsonObject();
        json.addProperty("workflowId", parameters.getWorkflowId());

        String jsonString = "{" + parameters.getWorkflowParameters().entrySet().stream()
            .map(entry -> {
                String key = "\"" + entry.getKey() + "\"";
                String value = entry.getValue();

                if ("true".equals(value) || "false".equals(value) || value.matches("-?\\d+(\\.\\d+)?")) {
                    return key + ":" + value;
                }

                String escaped = value.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
                return key + ":\"" + escaped + "\"";
            })
            .collect(Collectors.joining(",")) + "}";
        json.addProperty("workflowParameters", safeString(jsonString));

        return json;
    }

    private String enumToJson(Enum<?> e) {
        return e != null ? e.toString() : null;
    }
}
