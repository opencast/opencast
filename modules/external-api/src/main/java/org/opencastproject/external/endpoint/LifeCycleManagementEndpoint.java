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

import static com.entwinemedia.fn.data.json.Jsons.BLANK;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.RestUtil.getEndpointUrl;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;
import static org.opencastproject.util.doc.rest.RestParameter.Type.TEXT;

import org.opencastproject.elasticsearch.index.objects.event.EventSearchQueryField;
import org.opencastproject.external.common.ApiMediaType;
import org.opencastproject.external.common.ApiResponses;
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

import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JValue;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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

@Path("/")
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
public class LifeCycleManagementEndpoint {

    /** The logging facility */
    private static final Logger logger = LoggerFactory.getLogger(LifeCycleManagementEndpoint.class);
    private static final Gson gson = new Gson();

    /** Base URL of this endpoint */
    protected String endpointBaseUrl;

    /** The capture agent service */
    private LifeCycleService service;

    @Reference
    public void setLifeCycleService(LifeCycleService lifeCycleService) {
        this.service = lifeCycleService;
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

            return ApiResponses.Json.ok(acceptHeader, policyToJson(policy));
        } catch (NotFoundException e) {
            return ApiResponses.notFound("Cannot find playlist instance with id '%s'.", id);
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

        if (limit < 0) {
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

        List<JValue> policiesJson = policies.stream()
            .map(p -> policyToJson(p))
            .collect(Collectors.toList());

        return ApiResponses.Json.ok(acceptHeader, arr(policiesJson));
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
            @RestParameter(name = "filters", isRequired = false, description = "Used to select applicable entities. JSON. To find how to structure your JSON check the documentation.", type = TEXT),
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
        @FormParam("filters") String filters,
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
//            Map<String, String> filtersMap = RestUtils.parseFilter(filters);
            Map<String, EventSearchQueryField<String>> filtersMap = new HashMap<>();
            if (filters != null && !filters.isEmpty()) {
                try {
                    filtersMap = gson.fromJson(filters,
                        new TypeToken<Map<String, EventSearchQueryField<String>>>() { }.getType());
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
            @RestParameter(name = "filters", isRequired = false, description = "The filter(s) used to select applicable entities. Format: 'filter1:value1,filter2:value2'", type = TEXT),
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
        @FormParam("filters") String filters,
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

            if (!title.isEmpty()) {
                policy.setTitle(title);
            }
            if (!targetType.isEmpty()) {
                policy.setTargetType(TargetType.valueOf(targetType));
            }
            if (!action.isEmpty()) {
                policy.setAction(Action.valueOf(action));
            }
            if (!actionParametersList.isEmpty()) {
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
            if (!actionDate.isEmpty()) {
                policy.setActionDate(EncodingSchemeUtils.decodeDate(actionDate));
            }
            if (!cronTrigger.isEmpty()) {
                policy.setCronTrigger(cronTrigger);
            }
            if (!timing.isEmpty()) {
                policy.setTiming(Timing.valueOf(timing));
            }
            if (!filters.isEmpty()) {
//                policy.setTargetFilters(RestUtils.parseFilter(filters));
                Map<String, EventSearchQueryField<String>> filtersMap = new HashMap<>();
                if (filters != null && !filters.isEmpty()) {
                    try {
                        filtersMap = gson.fromJson(filters,
                            new TypeToken<Map<String, EventSearchQueryField<String>>>() { }.getType());
                        if (filtersMap == null) {
                            filtersMap = new HashMap<>();
                        }
                        policy.setTargetFilters(filtersMap);
                    } catch (JsonSyntaxException e) {
                        return Response.status(Response.Status.BAD_REQUEST).build();
                    }
                }
            }
            if (!accessControlEntries.isEmpty()) {
                // Check if ACL is well formed
                try {
                    List<LifeCyclePolicyAccessControlEntry> accessControlEntriesParsed = gson.fromJson(accessControlEntries,
                        new TypeToken<List<LifeCyclePolicyAccessControlEntry>>() { }.getType());
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
            return ApiResponses.notFound("Cannot find playlist instance with id '%s'.", id);
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
            return ApiResponses.notFound("Cannot find policy instance with id '%s'.", id);
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    private JValue policyToJson(LifeCyclePolicy policy) {
        List<Field> fields = new ArrayList<>();

        fields.add(f("id", v(policy.getId())));
        fields.add(f("title", v(policy.getTitle(), BLANK)));
        fields.add(f("targetType", enumToJSON(policy.getTargetType())));
        fields.add(f("action", enumToJSON(policy.getAction())));
        fields.add(f("actionParameters", v(policy.getActionParameters(), BLANK)));
        fields.add(f("actionDate", v(policy.getActionDate() != null ? toUTC(policy.getActionDate().getTime()) : null, BLANK)));
        fields.add(f("timing", enumToJSON(policy.getTiming())));
        fields.add(f("isActive", v(policy.isActive(), BLANK)));
        fields.add(f("isCreatedFromConfig", v(policy.isCreatedFromConfig(), BLANK)));
        fields.add(f("targetFilters", v("{" + policy.getTargetFilters().keySet().stream()
            .map(key -> key + ":" + eventSearchQueryFieldToJson(policy.getTargetFilters().get(key)))
            .collect(Collectors.joining(",", "", "")) + "}",
            BLANK
        )));
        fields.add(f("accessControlEntries", arr(policy.getAccessControlEntries()
            .stream()
            .map(this::policyAccessControlEntryToJson)
            .collect(Collectors.toList()))));

        return obj(fields);
    }

    private JValue policyAccessControlEntryToJson(LifeCyclePolicyAccessControlEntry policyAccessControlEntry) {
        List<Field> fields = new ArrayList<>();

        fields.add(f("id", v(policyAccessControlEntry.getId())));
        fields.add(f("allow", v(policyAccessControlEntry.isAllow())));
        fields.add(f("role", v(policyAccessControlEntry.getRole())));
        fields.add(f("action", v(policyAccessControlEntry.getAction())));

        return obj(fields);
    }

    private JValue eventSearchQueryFieldToJson(EventSearchQueryField eventSearchQueryField) {
        List<Field> fields = new ArrayList<>();

        fields.add(f("value", v(eventSearchQueryField.getValue(), BLANK)));
        fields.add(f("type", v(eventSearchQueryField.getType(), BLANK)));
        fields.add(f("must", v(eventSearchQueryField.isMust(), BLANK)));

        return obj(fields);
    }

    private JValue enumToJSON(Enum e) {
        return e == null ? null : v(e.toString());
    }
}
