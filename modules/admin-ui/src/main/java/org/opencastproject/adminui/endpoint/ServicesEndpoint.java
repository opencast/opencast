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

import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.index.service.resources.list.query.ServicesListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceState;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.util.SmartIterator;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.json.JValue;
import com.entwinemedia.fn.data.json.Jsons;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "ServicesProxyService", title = "UI Services",
  abstractText = "This service provides the services data for the UI.",
  notes = { "These Endpoints deliver informations about the services required for the UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
public class ServicesEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(ServicesEndpoint.class);
  private ServiceRegistry serviceRegistry;

  private static final String SERVICE_STATUS_TRANSLATION_PREFIX = "SYSTEMS.SERVICES.STATUS.";


  @GET
  @Path("services.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(description = "Returns the list of services", name = "services", restParameters = {
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "offset", description = "The offset", isRequired = false, type = RestParameter.Type.INTEGER),
          @RestParameter(name = "filter", description = "Filter results by name, host, actions, status or free text query", isRequired = false, type = STRING),
          @RestParameter(name = "sort", description = "The sort order.  May include any "
                  + "of the following: host, name, running, queued, completed,  meanRunTime, meanQueueTime, "
                  + "status. The sort suffix must be :asc for ascending sort order and :desc for descending.", isRequired = false, type = STRING)
  }, reponses = { @RestResponse(description = "Returns the list of services from Opencast", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "The list of services")
  public Response getServices(@QueryParam("limit") final int limit, @QueryParam("offset") final int offset,
          @QueryParam("filter") String filter, @QueryParam("sort") String sort) throws Exception {

    Option<String> sortOpt = Option.option(StringUtils.trimToNull(sort));
    ServicesListQuery query = new ServicesListQuery();
    EndpointUtil.addRequestFiltersToQuery(filter, query);

    String fName = null;
    if (query.getName().isSome())
      fName = StringUtils.trimToNull(query.getName().get());
    String fHostname = null;
    if (query.getHostname().isSome())
      fHostname = StringUtils.trimToNull(query.getHostname().get());
    String fStatus = null;
    if (query.getStatus().isSome())
      fStatus = StringUtils.trimToNull(query.getStatus().get());
    String fFreeText = null;
    if (query.getFreeText().isSome())
      fFreeText = StringUtils.trimToNull(query.getFreeText().get());

    List<Service> services = new ArrayList<Service>();
    for (ServiceStatistics stats : serviceRegistry.getServiceStatistics()) {
      Service service = new Service(stats);
      if (fName != null && !StringUtils.equalsIgnoreCase(service.getName(), fName))
        continue;

      if (fHostname != null && !StringUtils.equalsIgnoreCase(service.getHost(), fHostname))
        continue;

      if (fStatus != null && !StringUtils.equalsIgnoreCase(service.getStatus().toString(), fStatus))
        continue;

      if (query.getActions().isSome()) {
        ServiceState serviceState = service.getStatus();

        if (query.getActions().get()) {
          if (ServiceState.NORMAL == serviceState)
            continue;
        } else {
          if (ServiceState.NORMAL != serviceState)
            continue;
        }
      }

      if (fFreeText != null && !StringUtils.containsIgnoreCase(service.getName(), fFreeText)
                && !StringUtils.containsIgnoreCase(service.getHost(), fFreeText)
                && !StringUtils.containsIgnoreCase(service.getStatus().toString(), fFreeText))
        continue;

      services.add(service);
    }
    int total = services.size();

    if (sortOpt.isSome()) {
      Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(sortOpt.get());
      if (!sortCriteria.isEmpty()) {
        try {
          SortCriterion sortCriterion = sortCriteria.iterator().next();
          Collections.sort(services, new ServiceStatisticsComparator(
                  sortCriterion.getFieldName(),
                  sortCriterion.getOrder() == SearchQuery.Order.Ascending));
        } catch (Exception ex) {
          logger.warn("Failed to sort services collection.", ex);
        }
      }
    }

    List<JValue> jsonList = new ArrayList<JValue>();
    for (Service s : new SmartIterator<Service>(limit, offset).applyLimitAndOffset(services)) {
      jsonList.add(s.toJSON());
    }
    return RestUtils.okJsonList(jsonList, offset, limit, total);
  }

  /**
   * Service UI model. Wrapper class for a {@code ServiceStatistics} class.
   */
  class Service implements JSONAware {
    /** Completed model field name. */
    public static final String COMPLETED_NAME = "completed";
    /** Host model field name. */
    public static final String HOST_NAME = "hostname";
    /** MeanQueueTime model field name. */
    public static final String MEAN_QUEUE_TIME_NAME = "meanQueueTime";
    /** MeanRunTime model field name. */
    public static final String MEAN_RUN_TIME_NAME = "meanRunTime";
    /** (Service-) Name model field name. */
    public static final String NAME_NAME = "name";
    /** Queued model field name. */
    public static final String QUEUED_NAME = "queued";
    /** Running model field name. */
    public static final String RUNNING_NAME = "running";
    /** Status model field name. */
    public static final String STATUS_NAME = "status";

    /** Wrapped {@code ServiceStatistics} instance. */
    private final ServiceStatistics serviceStatistics;

    /** Constructor, set {@code ServiceStatistics} instance to a final private property. */
    Service(ServiceStatistics serviceStatistics) {
      this.serviceStatistics = serviceStatistics;
    }

    /**
     * Returns completed jobs count.
     * @return completed jobs count
     */
    public int getCompletedJobs() {
      return serviceStatistics.getFinishedJobs();
    }

    /**
     * Returns service host name.
     * @return service host name
     */
    public String getHost() {
      return serviceStatistics.getServiceRegistration().getHost();
    }

    /**
     * Returns service mean queue time in seconds.
     * @return service mean queue time in seconds
     */
    public long getMeanQueueTime() {
      return TimeUnit.MILLISECONDS.toSeconds(serviceStatistics.getMeanQueueTime());
    }

    /**
     * Returns service mean run time in seconds.
     * @return service mean run time in seconds
     */
    public long getMeanRunTime() {
      return TimeUnit.MILLISECONDS.toSeconds(serviceStatistics.getMeanRunTime());
    }

    /**
     * Returns service name.
     * @return service name
     */
    public String getName() {
      return serviceStatistics.getServiceRegistration().getServiceType();
    }

    /**
     * Returns queued jobs count.
     * @return queued jobs count
     */
    public int getQueuedJobs() {
      return serviceStatistics.getQueuedJobs();
    }

    /**
     * Returns running jobs count.
     * @return running jobs count
     */
    public int getRunningJobs() {
      return serviceStatistics.getRunningJobs();
    }

    /**
     * Returns service status.
     * @return service status
     */
    public ServiceState getStatus() {
      return serviceStatistics.getServiceRegistration().getServiceState();
    }

    /**
     * Returns a map of all service fields.
     * @return a map of all service fields
     */
    public Map<String, String> toMap() {
      Map<String, String> serviceMap = new HashMap<String, String>();
      serviceMap.put(COMPLETED_NAME, Integer.toString(getCompletedJobs()));
      serviceMap.put(HOST_NAME, getHost());
      serviceMap.put(MEAN_QUEUE_TIME_NAME, Long.toString(getMeanQueueTime()));
      serviceMap.put(MEAN_RUN_TIME_NAME, Long.toString(getMeanRunTime()));
      serviceMap.put(NAME_NAME, getName());
      serviceMap.put(QUEUED_NAME, Integer.toString(getQueuedJobs()));
      serviceMap.put(RUNNING_NAME, Integer.toString(getRunningJobs()));
      serviceMap.put(STATUS_NAME, getStatus().name());
      return serviceMap;
    }

    /**
     * Returns a json representation of a service as {@code String}.
     * @return a json representation of a service as {@code String}
     */
    @Override
    public String toJSONString() {
      return JSONObject.toJSONString(toMap());
    }

    /**
     * Returns a json representation of a service as {@code JValue}.
     * @return a json representation of a service as {@code JValue}
     */
    public JValue toJSON() {
      return obj(f(COMPLETED_NAME, v(getCompletedJobs())), f(HOST_NAME, v(getHost(), Jsons.BLANK)),
              f(MEAN_QUEUE_TIME_NAME, v(getMeanQueueTime())), f(MEAN_RUN_TIME_NAME, v(getMeanRunTime())),
              f(NAME_NAME, v(getName(), Jsons.BLANK)), f(QUEUED_NAME, v(getQueuedJobs())),
              f(RUNNING_NAME, v(getRunningJobs())),
              f(STATUS_NAME, v(SERVICE_STATUS_TRANSLATION_PREFIX + getStatus().name(), Jsons.BLANK)));
    }
  }

  /**
   * {@code Service} comparator. Can compare service instances based on the given sort criterion and sort order.
   */
  class ServiceStatisticsComparator implements Comparator<Service> {

    /** Sort criterion. */
    private final String sortBy;
    /** Sort order (true if ascending, false otherwise). */
    private final boolean ascending;

    /** Constructor. */
    ServiceStatisticsComparator(String sortBy, boolean ascending) {
      if (StringUtils.equalsIgnoreCase(Service.COMPLETED_NAME, sortBy)) {
        this.sortBy = Service.COMPLETED_NAME;
      } else if (StringUtils.equalsIgnoreCase(Service.HOST_NAME, sortBy)) {
        this.sortBy = Service.HOST_NAME;
      } else if (StringUtils.equalsIgnoreCase(Service.MEAN_QUEUE_TIME_NAME, sortBy)) {
        this.sortBy = Service.MEAN_QUEUE_TIME_NAME;
      } else if (StringUtils.equalsIgnoreCase(Service.MEAN_RUN_TIME_NAME, sortBy)) {
        this.sortBy = Service.MEAN_RUN_TIME_NAME;
      } else if (StringUtils.equalsIgnoreCase(Service.NAME_NAME, sortBy)) {
        this.sortBy = Service.NAME_NAME;
      } else if (StringUtils.equalsIgnoreCase(Service.QUEUED_NAME, sortBy)) {
        this.sortBy = Service.QUEUED_NAME;
      } else if (StringUtils.equalsIgnoreCase(Service.RUNNING_NAME, sortBy)) {
        this.sortBy = Service.RUNNING_NAME;
      } else if (StringUtils.equalsIgnoreCase(Service.STATUS_NAME, sortBy)) {
        this.sortBy = Service.STATUS_NAME;
      } else {
        throw new IllegalArgumentException(String.format("Can't sort services by %s.", sortBy));
      }
      this.ascending = ascending;
    }

    /**
     * Compare two service instances.
     * @param s1 first {@code Service} instance to compare
     * @param s2 second {@code Service} instance to compare
     * @return
     */
    @Override
    public int compare(Service s1, Service s2) {
      int result = 0;
      switch (sortBy) {
        case Service.COMPLETED_NAME:
          result = s1.getCompletedJobs() - s2.getCompletedJobs();
          break;
        case Service.HOST_NAME:
          result = s1.getHost().compareToIgnoreCase(s2.getHost());
          break;
        case Service.MEAN_QUEUE_TIME_NAME:
          result = (int) (s1.getMeanQueueTime() - s2.getMeanQueueTime());
          break;
        case Service.MEAN_RUN_TIME_NAME:
          result = (int) (s1.getMeanRunTime() - s2.getMeanRunTime());
          break;
        case Service.QUEUED_NAME:
          result = s1.getQueuedJobs() - s2.getQueuedJobs();
          break;
        case Service.RUNNING_NAME:
          result = s1.getRunningJobs() - s2.getRunningJobs();
          break;
        case Service.STATUS_NAME:
          result = s1.getStatus().compareTo(s2.getStatus());
          break;
        case Service.NAME_NAME: // default sorting criterium
        default:
          result = s1.getName().compareToIgnoreCase(s2.getName());
      }
      return ascending ? result : 0 - result;
    }
  }

  /** OSGI activate method. */
  public void activate() {
    logger.info("ServicesEndpoint is activated!");
  }

  /**
   * @param serviceRegistry
   *          the serviceRegistry to set
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }
}
