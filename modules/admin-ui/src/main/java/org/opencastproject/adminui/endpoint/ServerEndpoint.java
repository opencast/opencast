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

import static com.entwinemedia.fn.data.json.Jsons.BLANK;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.index.service.resources.list.provider.ServersListProvider;
import org.opencastproject.index.service.resources.list.query.ServersListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.util.SmartIterator;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import com.entwinemedia.fn.data.json.JValue;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "ServerProxyService", title = "UI Servers",
  abstractText = "This service provides the server data for the UI.",
  notes = { "These Endpoints deliver informations about the server required for the UI.",
            "<strong>Important:</strong> "
              + "<em>This service is for exclusive use by the module admin-ui. Its API might change "
              + "anytime without prior notice. Any dependencies other than the admin UI will be strictly ignored. "
              + "DO NOT use this for integration of third-party applications.<em>"})
@Component(
  immediate = true,
  service = ServerEndpoint.class,
  property = {
    "service.description=Admin UI - Server facade Endpoint",
    "opencast.service.type=org.opencastproject.adminui.endpoint.ServerEndpoint",
    "opencast.service.path=/admin-ng/server"
  }
)
public class ServerEndpoint {

  private enum Sort {
    COMPLETED, CORES, HOSTNAME, MAINTENANCE, MEANQUEUETIME, MEANRUNTIME, NODENAME, ONLINE, QUEUED, RUNNING
  }

  // List of property keys for the JSON job object
  private static final String KEY_ONLINE = "online";
  private static final String KEY_MAINTENANCE = "maintenance";
  private static final String KEY_HOSTNAME = "hostname";
  private static final String KEY_NODE_NAME = "nodeName";
  private static final String KEY_CORES = "cores";
  private static final String KEY_RUNNING = "running";
  private static final String KEY_COMPLETED = "completed";
  private static final String KEY_QUEUED = "queued";
  private static final String KEY_MEAN_RUN_TIME = "meanRunTime";
  private static final String KEY_MEAN_QUEUE_TIME = "meanQueueTime";

  /**
   * Comparator for the servers list
   */
  private class ServerComparator implements Comparator<JSONObject> {

    private Sort sortType;
    private Boolean ascending = true;

    ServerComparator(Sort sortType, Boolean ascending) {
      this.sortType = sortType;
      this.ascending = ascending;
    }

    @Override
    public int compare(JSONObject host1, JSONObject host2) {
      int result;

      switch (sortType) {
        case ONLINE:
          Boolean status1 = (Boolean) host1.get(KEY_ONLINE);
          Boolean status2 = (Boolean) host2.get(KEY_ONLINE);
          result = status1.compareTo(status2);
          break;
        case CORES:
          result = ((Integer) host1.get(KEY_CORES)).compareTo((Integer) host2.get(KEY_CORES));
          break;
        case COMPLETED:
          result = ((Long) host1.get(KEY_COMPLETED)).compareTo((Long) host2.get(KEY_COMPLETED));
          break;
        case QUEUED:
          result = ((Integer) host1.get(KEY_QUEUED)).compareTo((Integer) host2.get(KEY_QUEUED));
          break;
        case MAINTENANCE:
          Boolean mtn1 = (Boolean) host1.get(KEY_MAINTENANCE);
          Boolean mtn2 = (Boolean) host2.get(KEY_MAINTENANCE);
          result = mtn1.compareTo(mtn2);
          break;
        case RUNNING:
          result = ((Integer) host1.get(KEY_RUNNING)).compareTo((Integer) host2.get(KEY_RUNNING));
          break;
        case MEANQUEUETIME:
          result = ((Long) host1.get(KEY_MEAN_QUEUE_TIME)).compareTo((Long) host2.get(KEY_MEAN_QUEUE_TIME));
          break;
        case MEANRUNTIME:
          result = ((Long) host1.get(KEY_MEAN_RUN_TIME)).compareTo((Long) host2.get(KEY_MEAN_RUN_TIME));
          break;
        case NODENAME:
        {
          String name1 = (String) host1.get(KEY_NODE_NAME);
          String name2 = (String) host2.get(KEY_NODE_NAME);
          result = name1.compareTo(name2);
          break;
        }
        case HOSTNAME:
        default:
        {
          String name1 = (String) host1.get(KEY_HOSTNAME);
          String name2 = (String) host2.get(KEY_HOSTNAME);
          result = name1.compareTo(name2);
        }
      }

      return ascending ? result : -1 * result;
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(ServerEndpoint.class);

  public static final Response UNAUTHORIZED = Response.status(Response.Status.UNAUTHORIZED).build();
  public static final Response NOT_FOUND = Response.status(Response.Status.NOT_FOUND).build();
  public static final Response SERVER_ERROR = Response.serverError().build();

  private ServiceRegistry serviceRegistry;

  /** OSGi callback for the service registry. */
  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Activate
  protected void activate(BundleContext bundleContext) {
    logger.info("Activate job endpoint");
  }

  @GET
  @Path("servers.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(description = "Returns the list of servers", name = "servers", restParameters = {
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = INTEGER),
          @RestParameter(name = "offset", description = "The offset", isRequired = false, type = INTEGER),
          @RestParameter(name = "filter", description = "Filter results by hostname, status or free text query", isRequired = false, type = STRING),
          @RestParameter(name = "sort", description = "The sort order.  May include any "
                  + "of the following: COMPLETED (jobs), CORES, HOSTNAME, MAINTENANCE, MEANQUEUETIME (mean for jobs), "
                  + "MEANRUNTIME (mean for jobs), ONLINE, QUEUED (jobs), RUNNING (jobs)."
                  + "The suffix must be :ASC for ascending or :DESC for descending sort order (e.g. HOSTNAME:DESC).", isRequired = false, type = STRING) },
          responses = { @RestResponse(description = "Returns the list of jobs from Opencast", responseCode = HttpServletResponse.SC_OK) },
          returnDescription = "The list of servers")
  public Response getServers(@QueryParam("limit") final int limit, @QueryParam("offset") final int offset,
          @QueryParam("filter") String filter, @QueryParam("sort") String sort)
          throws Exception {

    ServersListQuery query = new ServersListQuery();
    EndpointUtil.addRequestFiltersToQuery(filter, query);
    query.setLimit(limit);
    query.setOffset(offset);

    List<JSONObject> servers = new ArrayList<>();
    // Get service statistics for all hosts and services
    List<ServiceStatistics> servicesStatistics = serviceRegistry.getServiceStatistics();
    for (HostRegistration server : serviceRegistry.getHostRegistrations()) {
      // Calculate statistics per server
      long jobsCompleted = 0;
      int jobsRunning = 0;
      int jobsQueued = 0;
      long sumMeanRuntime = 0;
      long sumMeanQueueTime = 0;
      int totalServiceOnHost = 0;
      int offlineJobProducerServices = 0;
      int totalJobProducerServices = 0;
      Set<String> serviceTypes = new HashSet<>();
      for (ServiceStatistics serviceStat : servicesStatistics) {
        if (server.getBaseUrl().equals(serviceStat.getServiceRegistration().getHost())) {
          totalServiceOnHost++;
          jobsCompleted += serviceStat.getFinishedJobs();
          jobsRunning += serviceStat.getRunningJobs();
          jobsQueued += serviceStat.getQueuedJobs();
          // mean time values are given in milliseconds,
          // we should convert them to seconds,
          // because the adminNG UI expect it in this format
          sumMeanRuntime += TimeUnit.MILLISECONDS.toSeconds(serviceStat.getMeanRunTime());
          sumMeanQueueTime += TimeUnit.MILLISECONDS.toSeconds(serviceStat.getMeanQueueTime());
          if (!serviceStat.getServiceRegistration().isOnline()
                  && serviceStat.getServiceRegistration().isJobProducer()) {
            offlineJobProducerServices++;
            totalJobProducerServices++;
          } else if (serviceStat.getServiceRegistration().isJobProducer()) {
            totalJobProducerServices++;
          }
          serviceTypes.add(serviceStat.getServiceRegistration().getServiceType());
        }
      }
      long meanRuntime = totalServiceOnHost > 0 ? Math.round((double)sumMeanRuntime / totalServiceOnHost) : 0L;
      long meanQueueTime = totalServiceOnHost > 0 ? Math.round((double)sumMeanQueueTime / totalServiceOnHost) : 0L;

      boolean vOnline = server.isOnline();
      boolean vMaintenance = server.isMaintenanceMode();
      String vHostname = server.getBaseUrl();
      String vNodeName = server.getNodeName();
      int vCores = server.getCores();

      if (query.getHostname().isSome()
              && !StringUtils.equalsIgnoreCase(vHostname, query.getHostname().get()))
          continue;

      if (query.getNodeName().isSome()
              && !StringUtils.equalsIgnoreCase(vNodeName, query.getNodeName().get()))
          continue;

      if (query.getStatus().isSome()) {
        if (StringUtils.equalsIgnoreCase(
                ServersListProvider.SERVER_STATUS_ONLINE,
                query.getStatus().get())
                && !vOnline)
          continue;
        if (StringUtils.equalsIgnoreCase(
                ServersListProvider.SERVER_STATUS_OFFLINE,
                query.getStatus().get())
                && vOnline)
          continue;
        if (StringUtils.equalsIgnoreCase(
                ServersListProvider.SERVER_STATUS_MAINTENANCE,
                query.getStatus().get())
                && !vMaintenance)
          continue;
      }

      if (query.getFreeText().isSome()
                && !StringUtils.containsIgnoreCase(vHostname, query.getFreeText().get())
                && !StringUtils.containsIgnoreCase(vNodeName, query.getFreeText().get())
                && !StringUtils.containsIgnoreCase(server.getIpAddress(), query.getFreeText().get()))
        continue;

      JSONObject jsonServer = new JSONObject();
      jsonServer.put(KEY_ONLINE, vOnline && offlineJobProducerServices <= totalJobProducerServices / 2);
      jsonServer.put(KEY_MAINTENANCE, vMaintenance);
      jsonServer.put(KEY_HOSTNAME, vHostname);
      jsonServer.put(KEY_NODE_NAME, vNodeName);
      jsonServer.put(KEY_CORES, vCores);
      jsonServer.put(KEY_RUNNING, jobsRunning);
      jsonServer.put(KEY_QUEUED, jobsQueued);
      jsonServer.put(KEY_COMPLETED, jobsCompleted);
      jsonServer.put(KEY_MEAN_RUN_TIME, meanRuntime);
      jsonServer.put(KEY_MEAN_QUEUE_TIME, meanQueueTime);
      servers.add(jsonServer);
    }

    // Sorting
    Sort sortKey = Sort.HOSTNAME;
    Boolean ascending = true;
    if (StringUtils.isNotBlank(sort)) {
      try {
        SortCriterion sortCriterion = RestUtils.parseSortQueryParameter(sort).iterator().next();
        sortKey = Sort.valueOf(sortCriterion.getFieldName().toUpperCase());
        ascending = SearchQuery.Order.Ascending == sortCriterion.getOrder()
                || SearchQuery.Order.None == sortCriterion.getOrder();
      } catch (WebApplicationException ex) {
        logger.warn("Failed to parse sort criterion \"{}\", invalid format.", sort);
      } catch (IllegalArgumentException ex) {
        logger.warn("Can not apply sort criterion \"{}\", no field with this name.", sort);
      }
    }

    JSONArray jsonList = new JSONArray();
    if (!servers.isEmpty()) {
      Collections.sort(servers, new ServerComparator(sortKey, ascending));
      jsonList.addAll(new SmartIterator(
              query.getLimit().getOrElse(0),
              query.getOffset().getOrElse(0))
              .applyLimitAndOffset(servers));
    }

    return RestUtils.okJsonList(
            getServersListAsJson(jsonList),
            query.getOffset().getOrElse(0),
            query.getLimit().getOrElse(0),
            servers.size());
  }

  /**
   * Transform each list item to JValue representation.
   * @param servers list with servers JSONObjects
   * @return servers list
   */
  private List<JValue> getServersListAsJson(List<JSONObject> servers) {
    List<JValue> jsonServers = new ArrayList<JValue>();
    for (JSONObject server : servers) {
      Boolean vOnline = (Boolean) server.get(KEY_ONLINE);
      Boolean vMaintenance = (Boolean) server.get(KEY_MAINTENANCE);
      String vHostname = (String) server.get(KEY_HOSTNAME);
      String vNodeName = (String) server.get(KEY_NODE_NAME);
      Integer vCores = (Integer) server.get(KEY_CORES);
      Integer vRunning = (Integer) server.get(KEY_RUNNING);
      Integer vQueued = (Integer) server.get(KEY_QUEUED);
      Long vCompleted = (Long) server.get(KEY_COMPLETED);
      Long vMeanRunTime = (Long) server.get(KEY_MEAN_RUN_TIME);
      Long vMeanQueueTime = (Long) server.get(KEY_MEAN_QUEUE_TIME);

      jsonServers.add(obj(f(KEY_ONLINE, v(vOnline)),
              f(KEY_MAINTENANCE, v(vMaintenance)),
              f(KEY_HOSTNAME, v(vHostname, BLANK)),
              f(KEY_NODE_NAME, v(vNodeName, BLANK)),
              f(KEY_CORES, v(vCores)),
              f(KEY_RUNNING, v(vRunning)),
              f(KEY_QUEUED, v(vQueued)),
              f(KEY_COMPLETED, v(vCompleted)),
              f(KEY_MEAN_RUN_TIME, v(vMeanRunTime)),
              f(KEY_MEAN_QUEUE_TIME, v(vMeanQueueTime))));
    }
    return jsonServers;
  }
}
