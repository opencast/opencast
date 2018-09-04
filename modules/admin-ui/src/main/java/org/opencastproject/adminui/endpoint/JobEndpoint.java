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

import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.data.Opt.nul;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.opencastproject.index.service.util.RestUtils.stream;
import static org.opencastproject.util.DateTimeSupport.toUTC;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.adminui.exception.JobEndpointException;
import org.opencastproject.index.service.resources.list.query.JobsListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.IncidentTree;
import org.opencastproject.job.api.Job;
import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.serviceregistry.api.IncidentL10n;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.IncidentServiceException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.SmartIterator;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowQuery.Sort;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.json.Field;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;
import com.entwinemedia.fn.data.json.SimpleSerializer;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "JobProxyService", title = "UI Jobs",
  abstractText = "This service provides the job data for the UI.",
  notes = { "These Endpoints deliver informations about the job required for the UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public class JobEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(JobEndpoint.class);
  private static final SimpleSerializer serializer = new SimpleSerializer();

  public static final Response UNAUTHORIZED = Response.status(Response.Status.UNAUTHORIZED).build();
  public static final Response NOT_FOUND = Response.status(Response.Status.NOT_FOUND).build();
  public static final Response SERVER_ERROR = Response.serverError().build();

  private enum JobSort {
    CREATOR, OPERATION, PROCESSINGHOST, STATUS, STARTED, SUBMITTED, TYPE, ID
  }

  private static final String NEGATE_PREFIX = "-";
  private static final String WORKFLOW_STATUS_TRANSLATION_PREFIX = "EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.";
  private static final String JOB_STATUS_TRANSLATION_PREFIX = "SYSTEMS.JOBS.STATUS.";

  private WorkflowService workflowService;
  private ServiceRegistry serviceRegistry;
  private IncidentService incidentService;

  /** OSGi callback for the workflow service. */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** OSGi callback for the service registry. */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /** OSGi callback for the incident service. */
  public void setIncidentService(IncidentService incidentService) {
    this.incidentService = incidentService;
  }

  protected void activate(BundleContext bundleContext) {
    logger.info("Activate job endpoint");
  }

  @GET
  @Path("jobs.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(description = "Returns the list of active jobs", name = "jobs", restParameters = {
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "offset", description = "The offset", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "filter", description = "Filter results by hostname, status or free text query", isRequired = false, type = RestParameter.Type.STRING),
          @RestParameter(name = "sort", description = "The sort order. May include any of the following: CREATOR, OPERATION, PROCESSINGHOST, STATUS, STARTED, SUBMITTED or TYPE. "
                  + "The suffix must be :ASC for ascending or :DESC for descending sort order (e.g. OPERATION:DESC)", isRequired = false, type = RestParameter.Type.STRING)},
          reponses = { @RestResponse(description = "Returns the list of active jobs from Opencast", responseCode = HttpServletResponse.SC_OK) },
          returnDescription = "The list of jobs as JSON")
  public Response getJobs(@QueryParam("limit") final int limit, @QueryParam("offset") final int offset,
          @QueryParam("filter") final String filter, @QueryParam("sort") final String sort) {
    JobsListQuery query = new JobsListQuery();
    EndpointUtil.addRequestFiltersToQuery(filter, query);
    query.setLimit(limit);
    query.setOffset(offset);

    String fHostname = null;
    if (query.getHostname().isSome())
      fHostname = StringUtils.trimToNull(query.getHostname().get());
    String fStatus = null;
    if (query.getStatus().isSome())
      fStatus = StringUtils.trimToNull(query.getStatus().get());
    String fFreeText = null;
    if (query.getFreeText().isSome())
      fFreeText = StringUtils.trimToNull(query.getFreeText().get());

    List<Job> jobs = new ArrayList<>();
    try {
      for (Job job : serviceRegistry.getActiveJobs()) {
        // filter workflow jobs
        if (StringUtils.equals(WorkflowService.JOB_TYPE, job.getJobType())
                && StringUtils.equals("START_WORKFLOW", job.getOperation()))
          continue;

        // filter by hostname
        if (fHostname != null && !StringUtils.equalsIgnoreCase(job.getProcessingHost(), fHostname))
          continue;

        // filter by status
        if (fStatus != null && !StringUtils.equalsIgnoreCase(job.getStatus().toString(), fStatus))
          continue;

        // fitler by user free text
        if (fFreeText != null
              && !StringUtils.equalsIgnoreCase(job.getProcessingHost(), fFreeText)
              && !StringUtils.equalsIgnoreCase(job.getJobType(), fFreeText)
              && !StringUtils.equalsIgnoreCase(job.getOperation(), fFreeText)
              && !StringUtils.equalsIgnoreCase(job.getCreator(), fFreeText)
              && !StringUtils.equalsIgnoreCase(job.getStatus().toString(), fFreeText)
              && !StringUtils.equalsIgnoreCase(Long.toString(job.getId()), fFreeText)
              && (job.getRootJobId() != null && !StringUtils.equalsIgnoreCase(Long.toString(job.getRootJobId()), fFreeText)))
          continue;
        jobs.add(job);
      }
    } catch (ServiceRegistryException ex) {
      logger.error("Failed to retrieve jobs list from service registry.", ex);
      return RestUtil.R.serverError();
    }

    JobSort sortKey = JobSort.SUBMITTED;
    boolean ascending = true;
    if (StringUtils.isNotBlank(sort)) {
      try {
        SortCriterion sortCriterion = RestUtils.parseSortQueryParameter(sort).iterator().next();
        sortKey = JobSort.valueOf(sortCriterion.getFieldName().toUpperCase());
        ascending = SearchQuery.Order.Ascending == sortCriterion.getOrder()
                || SearchQuery.Order.None == sortCriterion.getOrder();
      } catch (WebApplicationException ex) {
        logger.warn("Failed to parse sort criterion \"{}\", invalid format.", sort);
      } catch (IllegalArgumentException ex) {
        logger.warn("Can not apply sort criterion \"{}\", no field with this name.", sort);
      }
    }

    JobComparator comparator = new JobComparator(sortKey, ascending);
    Collections.sort(jobs, comparator);
    List<JValue> json = getJobsAsJSON(new SmartIterator(
            query.getLimit().getOrElse(0),
            query.getOffset().getOrElse(0))
            .applyLimitAndOffset(jobs));

    return RestUtils.okJsonList(json, offset, limit, jobs.size());
  }

  @GET
  @Path("tasks.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(description = "Returns the list of tasks", name = "tasks", restParameters = {
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "offset", description = "The offset", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "status", isRequired = false, description = "Filter results by workflows' current state", type = STRING),
          @RestParameter(name = "q", isRequired = false, description = "Filter results by free text query", type = STRING),
          @RestParameter(name = "seriesId", isRequired = false, description = "Filter results by series identifier", type = STRING),
          @RestParameter(name = "seriesTitle", isRequired = false, description = "Filter results by series title", type = STRING),
          @RestParameter(name = "creator", isRequired = false, description = "Filter results by the mediapackage's creator", type = STRING),
          @RestParameter(name = "contributor", isRequired = false, description = "Filter results by the mediapackage's contributor", type = STRING),
          @RestParameter(name = "fromdate", isRequired = false, description = "Filter results by workflow start date.", type = STRING),
          @RestParameter(name = "todate", isRequired = false, description = "Filter results by workflow start date.", type = STRING),
          @RestParameter(name = "language", isRequired = false, description = "Filter results by mediapackage's language.", type = STRING),
          @RestParameter(name = "title", isRequired = false, description = "Filter results by mediapackage's title.", type = STRING),
          @RestParameter(name = "subject", isRequired = false, description = "Filter results by mediapackage's subject.", type = STRING),
          @RestParameter(name = "workflow", isRequired = false, description = "Filter results by workflow definition.", type = STRING),
          @RestParameter(name = "operation", isRequired = false, description = "Filter results by workflows' current operation.", type = STRING),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any "
                  + "of the following: DATE_CREATED, TITLE, SERIES_TITLE, SERIES_ID, MEDIA_PACKAGE_ID, WORKFLOW_DEFINITION_ID, CREATOR, "
                  + "CONTRIBUTOR, LANGUAGE, LICENSE, SUBJECT.  The suffix must be :ASC for ascending or :DESC for descending sort order (e.g. TITLE:DESC).", type = STRING) },
          reponses = { @RestResponse(description = "Returns the list of tasks from Opencast", responseCode = HttpServletResponse.SC_OK) },
          returnDescription = "The list of tasks as JSON")
  public Response getTasks(@QueryParam("limit") final int limit, @QueryParam("offset") final int offset,
          @QueryParam("status") List<String> states, @QueryParam("q") String text,
          @QueryParam("seriesId") String seriesId, @QueryParam("seriesTitle") String seriesTitle,
          @QueryParam("creator") String creator, @QueryParam("contributor") String contributor,
          @QueryParam("fromdate") String fromDate, @QueryParam("todate") String toDate,
          @QueryParam("language") String language, @QueryParam("title") String title,
          @QueryParam("subject") String subject, @QueryParam("workflowdefinition") String workflowDefinitionId,
          @QueryParam("mp") String mediapackageId, @QueryParam("operation") List<String> currentOperations,
          @QueryParam("sort") String sort, @Context HttpHeaders headers) throws JobEndpointException {
    WorkflowQuery query = new WorkflowQuery();
    query.withStartPage(offset);
    query.withCount(limit);

    // Add filters
    query.withText(text);
    query.withSeriesId(seriesId);
    query.withSeriesTitle(seriesTitle);
    query.withSubject(subject);
    query.withMediaPackage(mediapackageId);
    query.withCreator(creator);
    query.withContributor(contributor);
    try {
      query.withDateAfter(SolrUtils.parseDate(fromDate));
    } catch (ParseException e) {
      logger.error("Not able to parse the date {}: {}", fromDate, e.getMessage());
    }

    try {
      query.withDateBefore(SolrUtils.parseDate(toDate));
    } catch (ParseException e) {
      logger.error("Not able to parse the date {}: {}", fromDate, e.getMessage());
    }
    query.withLanguage(language);
    query.withTitle(title);
    query.withWorkflowDefintion(workflowDefinitionId);

    if (states != null && states.size() > 0) {
      try {
        for (String state : states) {
          if (StringUtils.isBlank(state)) {
            continue;
          } else if (state.startsWith(NEGATE_PREFIX)) {
            query.withoutState(WorkflowState.valueOf(state.substring(1).toUpperCase()));
          } else {
            query.withState(WorkflowState.valueOf(state.toUpperCase()));
          }
        }
      } catch (IllegalArgumentException e) {
        logger.debug("Unknown workflow state.", e);
      }
    }

    if (currentOperations != null && currentOperations.size() > 0) {
      for (String op : currentOperations) {
        if (StringUtils.isBlank(op)) {
          continue;
        }
        if (op.startsWith(NEGATE_PREFIX)) {
          query.withoutCurrentOperation(op.substring(1));
        } else {
          query.withCurrentOperation(op);
        }
      }
    }

    // Sorting
    if (StringUtils.isNotBlank(sort)) {
      try {
        SortCriterion sortCriterion = RestUtils.parseSortQueryParameter(sort).iterator().next();
        Sort sortKey = Sort.valueOf(sortCriterion.getFieldName().toUpperCase());
        boolean ascending = SearchQuery.Order.Ascending == sortCriterion.getOrder()
                || SearchQuery.Order.None == sortCriterion.getOrder();

        query.withSort(sortKey, ascending);
      } catch (WebApplicationException ex) {
        logger.warn("Failed to parse sort criterion \"{}\", invalid format.", sort);
      } catch (IllegalArgumentException ex) {
        logger.warn("Can not apply sort criterion \"{}\", no field with this name.", sort);
      }
    }

    JObject json;
    try {
      json = getTasksAsJSON(query);
    } catch (NotFoundException e) {
      return NOT_FOUND;
    }

    return Response.ok(stream(serializer.fn.toJson(json)), MediaType.APPLICATION_JSON_TYPE).build();
  }

  public List<JValue> getJobsAsJSON(List<Job> jobs) {
    List<JValue> jsonList = new ArrayList<>();
    for (Job job : jobs) {
      long id = job.getId();
      String jobType = job.getJobType();
      String operation = job.getOperation();
      Job.Status status = job.getStatus();
      Date dateCreated = job.getDateCreated();
      String created = null;
      if (dateCreated != null)
        created = DateTimeSupport.toUTC(dateCreated.getTime());
      Date dateStarted = job.getDateStarted();
      String started = null;
      if (dateStarted != null)
        started = DateTimeSupport.toUTC(dateStarted.getTime());
      String creator = job.getCreator();
      String processingHost = job.getProcessingHost();

      jsonList.add(obj(f("id", v(id)),
              f("type", v(jobType)),
              f("operation", v(operation)),
              f("status", v(JOB_STATUS_TRANSLATION_PREFIX + status.toString())),
              f("submitted", v(created, Jsons.BLANK)),
              f("started", v(started, Jsons.BLANK)),
              f("creator", v(creator, Jsons.BLANK)),
              f("processingHost", v(processingHost, Jsons.BLANK))));
    }

    return jsonList;
  }

  /**
   * Returns the list of tasks matching the given query as JSON Object
   *
   * @param query
   *          The worklfow query
   * @return The list of matching tasks as JSON Object
   * @throws JobEndpointException
   * @throws NotFoundException
   */
  public JObject getTasksAsJSON(WorkflowQuery query) throws JobEndpointException, NotFoundException {
    // Get results
    WorkflowSet workflowInstances = null;
    long totalWithoutFilters = 0;
    List<JValue> jsonList = new ArrayList<>();

    try {
      workflowInstances = workflowService.getWorkflowInstances(query);
      totalWithoutFilters = workflowService.countWorkflowInstances();
    } catch (WorkflowDatabaseException e) {
      throw new JobEndpointException(String.format("Not able to get the list of job from the database: %s", e),
              e.getCause());
    }

    WorkflowInstance[] items = workflowInstances.getItems();

    for (WorkflowInstance instance : items) {
      long instanceId = instance.getId();
      String series = instance.getMediaPackage().getSeriesTitle();

      // Retrieve submission date with the workflow instance main job
      Date created;
      try {
        created = serviceRegistry.getJob(instanceId).getDateCreated();
      } catch (ServiceRegistryException e) {
        throw new JobEndpointException(String.format("Error when retrieving job %s from the service registry: %s",
                instanceId, e), e.getCause());
      }

      jsonList.add(obj(f("id", v(instanceId)), f("title", v(nul(instance.getMediaPackage().getTitle()).getOr(""))),
              f("series", v(series, Jsons.BLANK)), f("workflow", v(instance.getTitle(), Jsons.BLANK)),
              f("status", v(WORKFLOW_STATUS_TRANSLATION_PREFIX + instance.getState().toString())),
              f("submitted", v(created != null ? DateTimeSupport.toUTC(created.getTime()) : ""))));
    }

    JObject json = obj(f("results", arr(jsonList)), f("count", v(workflowInstances.getTotalCount())),
            f("offset", v(query.getStartPage())), f("limit", v(jsonList.size())), f("total", v(totalWithoutFilters)));
    return json;
  }

  /**
   * Returns the single task with the given Id as JSON Object
   *
   * @param id
   * @return The job as JSON Object
   * @throws JobEndpointException
   * @throws NotFoundException
   */
  public JObject getTasksAsJSON(long id) throws JobEndpointException, NotFoundException {
    WorkflowInstance instance = getWorkflowById(id);

    // Retrieve submission date with the workflow instance main job
    Date created;
    long duration = 0;
    try {
      Job job = serviceRegistry.getJob(id);
      created = job.getDateCreated();
      Date completed = job.getDateCompleted();
      if (completed == null)
        completed = new Date();

      duration = (completed.getTime() - created.getTime());
    } catch (ServiceRegistryException e) {
      throw new JobEndpointException(
              String.format("Error when retrieving job %s from the service registry: %s", id, e), e.getCause());
    }

    MediaPackage mp = instance.getMediaPackage();

    List<Field> fields = new ArrayList<>();
    for (String key : instance.getConfigurationKeys()) {
      fields.add(f(key, v(instance.getConfiguration(key), Jsons.BLANK)));
    }

    return obj(f("start", v(created != null ? toUTC(created.getTime()) : "", Jsons.BLANK)),
               f("state", v(WORKFLOW_STATUS_TRANSLATION_PREFIX + instance.getState(), Jsons.BLANK)),
               f("description", v(instance.getDescription(), Jsons.BLANK)), f("duration", v(duration, Jsons.BLANK)),
               f("id", v(instance.getId(), Jsons.BLANK)), f("workflow", v(instance.getTitle(), Jsons.BLANK)),
               f("workflowId", v(instance.getTemplate(), Jsons.BLANK)), f("title", v(mp.getTitle(), Jsons.BLANK)),
               f("series", v(mp.getSeries(), Jsons.BLANK)), f("series_title", v(mp.getSeriesTitle(), Jsons.BLANK)),
               f("license", v(mp.getLicense(), Jsons.BLANK)), f("configuration", obj(fields)));
  }

  /**
   * Returns the list of operations for a given workflow instance
   *
   * @param jobId
   *          the workflow instance id
   * @return the list of workflow operations as JSON object
   * @throws JobEndpointException
   * @throws NotFoundException
   */
  public JValue getOperationsAsJSON(long jobId) throws JobEndpointException, NotFoundException {
    WorkflowInstance instance = getWorkflowById(jobId);

    List<WorkflowOperationInstance> operations = instance.getOperations();
    List<JValue> operationsJSON = new ArrayList<>();

    for (WorkflowOperationInstance wflOp : operations) {
      List<Field> fields = new ArrayList<>();
      for (String key : wflOp.getConfigurationKeys()) {
        fields.add(f(key, v(wflOp.getConfiguration(key), Jsons.BLANK)));
      }
      operationsJSON.add(obj(f("status", v(WORKFLOW_STATUS_TRANSLATION_PREFIX + wflOp.getState(), Jsons.BLANK)), f("title", v(wflOp.getTemplate(), Jsons.BLANK)),
              f("description", v(wflOp.getDescription(), Jsons.BLANK)), f("id", v(wflOp.getId(), Jsons.BLANK)), f("configuration", obj(fields))));
    }

    return arr(operationsJSON);
  }

  /**
   * Returns the operation with the given id from the given workflow instance
   *
   * @param jobId
   *          the workflow instance id
   * @param operationPosition
   *          the operation position
   * @return the operation as JSON object
   * @throws JobEndpointException
   * @throws NotFoundException
   */
  public JObject getOperationAsJSON(long jobId, int operationPosition)
          throws JobEndpointException, NotFoundException {
    WorkflowInstance instance = getWorkflowById(jobId);

    List<WorkflowOperationInstance> operations = instance.getOperations();

    if (operations.size() > operationPosition) {
      WorkflowOperationInstance wflOp = operations.get(operationPosition);
      return obj(f("retry_strategy", v(wflOp.getRetryStrategy(), Jsons.BLANK)),
              f("execution_host", v(wflOp.getExecutionHost(), Jsons.BLANK)),
              f("failed_attempts", v(wflOp.getFailedAttempts())),
              f("max_attempts", v(wflOp.getMaxAttempts())),
              f("exception_handler_workflow", v(wflOp.getExceptionHandlingWorkflow(), Jsons.BLANK)),
              f("fail_on_error", v(wflOp.isFailWorkflowOnException())),
              f("description", v(wflOp.getDescription(), Jsons.BLANK)),
              f("state", v(WORKFLOW_STATUS_TRANSLATION_PREFIX + wflOp.getState(), Jsons.BLANK)),
              f("job", v(wflOp.getId(), Jsons.BLANK)),
              f("name", v(wflOp.getTemplate(), Jsons.BLANK)),
              f("time_in_queue", v(wflOp.getTimeInQueue(), v(0))),
              f("started", wflOp.getDateStarted() != null ? v(toUTC(wflOp.getDateStarted().getTime())) : Jsons.BLANK),
              f("completed", wflOp.getDateCompleted() != null ? v(toUTC(wflOp.getDateCompleted().getTime())) : Jsons.BLANK)
      );
    }

    return null;
  }

  /**
   * Returns the list of incidents for a given workflow instance
   *
   * @param jobId
   *          the workflow instance id
   * @param locale
   *          the language in which title and description shall be returned
   * @param cascade
   *          if true, return the incidents of the given job and those of of its descendants
   * @return the list incidents as JSON array
   * @throws JobEndpointException
   * @throws NotFoundException
   */
  public JValue getIncidentsAsJSON(long jobId, final Locale locale, boolean cascade)
          throws JobEndpointException, NotFoundException {
    final List<Incident> incidents;
    try {
      final IncidentTree it = incidentService.getIncidentsOfJob(jobId, cascade);
      incidents = cascade ? flatten(it) : it.getIncidents();
    } catch (IncidentServiceException e) {
      throw new JobEndpointException(String.format(
              "Not able to get the incidents for the job %d from the incident service : %s", jobId, e), e.getCause());
    }
    final Stream<JValue> json = $(incidents).map(new Fn<Incident, JValue>() {
      @Override
      public JValue apply(Incident i) {
        return obj(f("id", v(i.getId())), f("severity", v(i.getSeverity(), Jsons.BLANK)),
                f("timestamp", v(toUTC(i.getTimestamp().getTime()), Jsons.BLANK))).merge(
                localizeIncident(i, locale));
      }
    });
    return arr(json);
  }

  /**
   * Flatten a tree of incidents.
   *
   * @return a list of incidents
   */
  private List<Incident> flatten(IncidentTree incidentsTree) {
    final List<Incident> incidents = new ArrayList<>();
    incidents.addAll(incidentsTree.getIncidents());
    for (IncidentTree descendantTree : incidentsTree.getDescendants()) {
      incidents.addAll(flatten(descendantTree));
    }
    return incidents;
  }

  /**
   * Return localized title and description of an incident as JSON.
   *
   * @param incident
   *          the incident to localize
   * @param locale
   *          the locale to be used to create title and description
   * @return JSON object
   */
  private JObject localizeIncident(Incident incident, Locale locale) {
    try {
      final IncidentL10n loc = incidentService.getLocalization(incident.getId(), locale);
      return obj(f("title", v(loc.getTitle(), Jsons.BLANK)), f("description", v(loc.getDescription(), Jsons.BLANK)));
    } catch (Exception e) {
      return obj(f("title", v("")), f("description", v("")));
    }
  }

  /**
   * Returns the workflow by the given identifier. This also returns STOPPED workflows, which is the reason for not
   * using the existing {@link WorkflowService:getWorkflowById()} method.
   *
   * @param id
   *          the workflow identifier
   * @return the workflow instance
   * @throws NotFoundException
   *           it the workflow was not found
   * @throws JobEndpointException
   *           if there was an issue reading the workflow from the database
   */
  private WorkflowInstance getWorkflowById(long id) throws NotFoundException, JobEndpointException {
    try {
      WorkflowSet workflowInstances = workflowService
              .getWorkflowInstances(new WorkflowQuery().withId(Long.toString(id)));
      if (workflowInstances.getItems().length == 0)
        throw new NotFoundException();

      return workflowInstances.getItems()[0];
    } catch (WorkflowDatabaseException e) {
      throw new JobEndpointException(String.format("Not able to get the list of job from the database: %s", e),
              e.getCause());
    }
  }

  /**
   * Return an incident serialized as JSON.
   *
   * @param id
   *          incident id
   * @param locale
   *          the locale to be used to create title and description
   * @return JSON object
   */
  public JValue getIncidentAsJSON(long id, Locale locale) throws JobEndpointException, NotFoundException {
    final Incident incident;
    try {
      incident = incidentService.getIncident(id);
    } catch (IncidentServiceException e) {
      throw new JobEndpointException(String.format("Not able to get the incident %d: %s", id, e), e.getCause());
    }
    return obj(f("id", v(incident.getId(), Jsons.BLANK)), f("job_id", v(incident.getJobId(), Jsons.BLANK)),
            f("severity", v(incident.getSeverity(), Jsons.BLANK)),
            f("timestamp", v(toUTC(incident.getTimestamp().getTime()), Jsons.BLANK)),
            f("processing_host", v(incident.getProcessingHost(), Jsons.BLANK)), f("service_type", v(incident.getServiceType(), Jsons.BLANK)),
            f("technical_details", v(incident.getDescriptionParameters(), Jsons.BLANK)),
            f("details", arr($(incident.getDetails()).map(errorDetailToJson))))
      .merge(localizeIncident(incident, locale));
  }

  private final Fn<Tuple<String, String>, JObject> errorDetailToJson = new Fn<Tuple<String, String>, JObject>() {
    @Override
    public JObject apply(Tuple<String, String> detail) {
      return obj(f("name", v(detail.getA(), Jsons.BLANK)), f("value", v(detail.getB(), Jsons.BLANK)));
    }
  };

  private final Fn<Job, Boolean> removeWorkflowJobs = new Fn<Job, Boolean>() {
    @Override
    public Boolean apply(Job job) {
      if (WorkflowService.JOB_TYPE.equals(job.getJobType())
              && ("START_WORKFLOW".equals(job.getOperation()) || "START_OPERATION".equals(job.getOperation())))
        return false;
      return true;
    }
  };

  private class JobComparator implements Comparator<Job> {

    private JobSort sortType;
    private boolean ascending;

    JobComparator(JobSort sortType, boolean ascending) {
      this.sortType = sortType;
      this.ascending = ascending;
    }

    @Override
    public int compare(Job job1, Job job2) {
      int result = 0;
      Object value1 = null;
      Object value2 = null;
      switch (sortType) {
        case CREATOR:
          value1 = job1.getCreator();
          value2 = job2.getCreator();
          break;
        case OPERATION:
          value1 = job1.getOperation();
          value2 = job2.getOperation();
          break;
        case PROCESSINGHOST:
          value1 = job1.getProcessingHost();
          value2 = job2.getProcessingHost();
          break;
        case STARTED:
          value1 = job1.getDateStarted();
          value2 = job2.getDateStarted();
          break;
        case STATUS:
          value1 = job1.getStatus();
          value2 = job2.getStatus();
          break;
        case SUBMITTED:
          value1 = job1.getDateCreated();
          value2 = job2.getDateCreated();
          break;
        case TYPE:
          value1 = job1.getJobType();
          value2 = job2.getJobType();
          break;
        case ID:
          value1 = job1.getId();
          value2 = job2.getId();
          break;
        default:
      }

      if (value1 == null) {
        return value2 == null ? 0 : 1;
      }
      if (value2 == null) {
        return -1;
      }
      try {
        result = ((Comparable)value1).compareTo(value2);
      } catch (ClassCastException ex) {
        logger.debug("Can not compare \"{}\" with \"{}\": {}",
                value1, value2, ex);
      }

      return ascending ? result : -1 * result;
    }
  }
}
