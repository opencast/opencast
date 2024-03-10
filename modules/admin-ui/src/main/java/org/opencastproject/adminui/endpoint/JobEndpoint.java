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
package org.opencastproject.adminui.endpoint;

import static com.entwinemedia.fn.Stream.$;
import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.opencastproject.util.DateTimeSupport.toUTC;

import org.opencastproject.adminui.exception.JobEndpointException;
import org.opencastproject.index.service.resources.list.query.JobsListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.IncidentTree;
import org.opencastproject.job.api.Job;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.IncidentL10n;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.IncidentServiceException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RestUtil;
import org.opencastproject.util.SmartIterator;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.requests.SortCriterion;
import org.opencastproject.util.requests.SortCriterion.Order;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;
import com.entwinemedia.fn.data.json.SimpleSerializer;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/admin-ng/job")
@RestService(name = "JobProxyService", title = "UI Jobs",
  abstractText = "This service provides the job data for the UI.",
  notes = { "These Endpoints deliver informations about the job required for the UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
@Component(
  immediate = true,
  service = JobEndpoint.class,
  property = {
    "service.description=Admin UI - Job facade Endpoint",
    "opencast.service.type=org.opencastproject.adminui.endpoint.JobEndpoint",
    "opencast.service.path=/admin-ng/job"
  }
)
@JaxrsResource
public class JobEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(JobEndpoint.class);
  private static final SimpleSerializer serializer = new SimpleSerializer();

  public static final Response NOT_FOUND = Response.status(Response.Status.NOT_FOUND).build();

  private enum JobSort {
    CREATOR, OPERATION, PROCESSINGHOST, PROCESSINGNODE, STATUS, STARTED, SUBMITTED, TYPE, ID
  }

  private static final String NEGATE_PREFIX = "-";
  private static final String WORKFLOW_STATUS_TRANSLATION_PREFIX = "EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.";
  private static final String JOB_STATUS_TRANSLATION_PREFIX = "SYSTEMS.JOBS.STATUS.";

  private WorkflowService workflowService;
  private ServiceRegistry serviceRegistry;
  private IncidentService incidentService;
  private UserDirectoryService userDirectoryService;

  /** OSGi callback for the workflow service. */
  @Reference
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** OSGi callback for the service registry. */
  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /** OSGi callback for the incident service. */
  @Reference
  public void setIncidentService(IncidentService incidentService) {
    this.incidentService = incidentService;
  }

  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  @Activate
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
          responses = { @RestResponse(description = "Returns the list of active jobs from Opencast", responseCode = HttpServletResponse.SC_OK) },
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
    String fNodeName = null;
    if (query.getNodeName().isSome())
      fNodeName = StringUtils.trimToNull(query.getNodeName().get());
    String fStatus = null;
    if (query.getStatus().isSome())
      fStatus = StringUtils.trimToNull(query.getStatus().get());
    String fFreeText = null;
    if (query.getFreeText().isSome())
      fFreeText = StringUtils.trimToNull(query.getFreeText().get());

    List<JobExtended> jobs = new ArrayList<>();
    try {
      String vNodeName;
      Optional<HostRegistration> server;
      List<HostRegistration> servers = serviceRegistry.getHostRegistrations();
      for (Job job : serviceRegistry.getActiveJobs()) {
        // filter workflow jobs
        if (StringUtils.equals(WorkflowService.JOB_TYPE, job.getJobType())
                && StringUtils.equals("START_WORKFLOW", job.getOperation()))
          continue;

        // filter by hostname
        if (fHostname != null && !StringUtils.equalsIgnoreCase(job.getProcessingHost(), fHostname))
          continue;

        server = findServerByHost(job.getProcessingHost(), servers);
        vNodeName = server.isPresent() ? server.get().getNodeName() : "";

        // filter by node name
        if (fNodeName != null && (server.isPresent()) && !StringUtils.equalsIgnoreCase(vNodeName, fNodeName))
          continue;

        // filter by status
        if (fStatus != null && !StringUtils.equalsIgnoreCase(job.getStatus().toString(), fStatus))
          continue;

        // fitler by user free text
        if (fFreeText != null
              && !StringUtils.equalsIgnoreCase(job.getProcessingHost(), fFreeText)
              && !StringUtils.equalsIgnoreCase(vNodeName, fFreeText)
              && !StringUtils.equalsIgnoreCase(job.getJobType(), fFreeText)
              && !StringUtils.equalsIgnoreCase(job.getOperation(), fFreeText)
              && !StringUtils.equalsIgnoreCase(job.getCreator(), fFreeText)
              && !StringUtils.equalsIgnoreCase(job.getStatus().toString(), fFreeText)
              && !StringUtils.equalsIgnoreCase(Long.toString(job.getId()), fFreeText)
              && (job.getRootJobId() != null && !StringUtils.equalsIgnoreCase(Long.toString(job.getRootJobId()), fFreeText)))
          continue;
        jobs.add(new JobExtended(job, vNodeName));
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
        ascending = Order.Ascending == sortCriterion.getOrder()
                || Order.None == sortCriterion.getOrder();
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

  /* Class to handle additional information related to a job */
  class JobExtended {

    private final Job job;
    private final String nodeName;

    JobExtended(Job job, String nodeName) {
      this.job = job;
      this.nodeName = nodeName;
    }

    public Job getJob() {
      return job;
    }

    public String getNodeName() {
      return nodeName;
    }
  }

  public List<JValue> getJobsAsJSON(List<JobExtended> jobs) {
    List<JValue> jsonList = new ArrayList<>();
    for (JobExtended jobEx : jobs) {
      Job job = jobEx.getJob();
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
      String processingNode = jobEx.getNodeName();

      jsonList.add(obj(f("id", v(id)),
              f("type", v(jobType)),
              f("operation", v(operation)),
              f("status", v(JOB_STATUS_TRANSLATION_PREFIX + status.toString())),
              f("submitted", v(created, Jsons.BLANK)),
              f("started", v(started, Jsons.BLANK)),
              f("creator", v(creator, Jsons.BLANK)),
              f("processingHost", v(processingHost, Jsons.BLANK)),
              f("processingNode", v(processingNode, Jsons.BLANK))));
    }

    return jsonList;
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

  private class JobComparator implements Comparator<JobExtended> {

    private JobSort sortType;
    private boolean ascending;

    JobComparator(JobSort sortType, boolean ascending) {
      this.sortType = sortType;
      this.ascending = ascending;
    }

    @Override
    public int compare(JobExtended jobEx1, JobExtended jobEx2) {
      int result = 0;
      Object value1 = null;
      Object value2 = null;
      Job job1 = jobEx1.getJob();
      Job job2 = jobEx2.getJob();
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
        case PROCESSINGNODE:
          value1 = jobEx1.getNodeName();
          value2 = jobEx2.getNodeName();
          break;        case STARTED:
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
        logger.debug("Can not compare \"{}\" with \"{}\"",
                value1, value2, ex);
      }

      return ascending ? result : -1 * result;
    }
  }

  /**
   * @param hostname of server to find in list
   * @param servers list of all host registrations
   */
  private Optional<HostRegistration> findServerByHost(String hostname, List<HostRegistration> servers) {
    return servers.stream().filter(o -> o.getBaseUrl().equals(hostname)).findFirst();
  }
}
