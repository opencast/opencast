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

import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceStatistics;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.workflow.api.WorkflowQuery.Sort;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@RestService(name = "ServerProxyService", title = "UI Servers", notes = "These Endpoints deliver informations about the server required for the UI.", abstractText = "This service provides the server data for the UI.")
public class ServerEndpoint {

  private static enum SORT {
    STATUS, HOSTNAME, CORES, COMPLETED, MAINTENANCE, RUNNING, QUEUED, QUEUETIME, RUNTIME
  }

  // List of property keys for the JSON job object
  private static final String KEY_ONLINE = "online";
  private static final String KEY_MAINTENANCE = "maintenance";
  private static final String KEY_NAME = "name";
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

    private SORT sortType;
    private Boolean ascending = true;

    public ServerComparator(SORT sortType, Boolean ascending) {
      this.sortType = sortType;
      this.ascending = ascending;
    }

    @Override
    public int compare(JSONObject host1, JSONObject host2) {
      int result;

      switch (sortType) {
        case STATUS:
          Boolean status1 = (Boolean) host1.get(KEY_ONLINE);
          Boolean status2 = (Boolean) host2.get(KEY_ONLINE);
          result = status1.compareTo(status2);
          break;
        case CORES:
          result = ((Integer) host1.get(KEY_CORES)).compareTo((Integer) host2.get(KEY_CORES));
          break;
        case COMPLETED:
          result = ((Integer) host1.get(KEY_COMPLETED)).compareTo((Integer) host2.get(KEY_COMPLETED));
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
        case QUEUETIME:
          result = ((Integer) host1.get(KEY_MEAN_QUEUE_TIME)).compareTo((Integer) host2.get(KEY_MEAN_QUEUE_TIME));
          break;
        case RUNTIME:
          result = ((Integer) host1.get(KEY_MEAN_QUEUE_TIME)).compareTo((Integer) host2.get(KEY_MEAN_QUEUE_TIME));
          break;
        case HOSTNAME:
        default:
          String name1 = (String) host1.get(KEY_NAME);
          String name2 = (String) host2.get(KEY_NAME);
          result = name1.compareTo(name2);
      }

      return ascending ? result : -1 * result;
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(ServerEndpoint.class);

  public static final Response UNAUTHORIZED = Response.status(Response.Status.UNAUTHORIZED).build();
  public static final Response NOT_FOUND = Response.status(Response.Status.NOT_FOUND).build();
  public static final Response SERVER_ERROR = Response.serverError().build();

  private static final String DESCENDING_SUFFIX = "_DESC";

  private ServiceRegistry serviceRegistry;

  /** OSGi callback for the service registry. */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  protected void activate(BundleContext bundleContext) {
    logger.info("Activate job endpoint");
  }

  @GET
  @Path("servers.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(description = "Returns the list of servers", name = "servers", restParameters = {
          @RestParameter(name = "limit", description = "The maximum number of items to return per page", isRequired = false, type = INTEGER),
          @RestParameter(name = "offset", description = "The offset", isRequired = false, type = INTEGER),
          @RestParameter(name = "online", isRequired = false, description = "Filter results by server current online status", type = BOOLEAN),
          @RestParameter(name = "offline", isRequired = false, description = "Filter results by server current offline status", type = BOOLEAN),
          @RestParameter(name = "maintenance", isRequired = false, description = "Filter results by server current maintenance status", type = BOOLEAN),
          @RestParameter(name = "q", isRequired = false, description = "Filter results by free text query", type = STRING),
          @RestParameter(name = "types", isRequired = false, description = "Filter results by sevices types registred on the server", type = STRING),
          @RestParameter(name = "ipaddress", isRequired = false, description = "Filter results by the server ip address", type = STRING),
          @RestParameter(name = "cores", isRequired = false, description = "Filter results by the number of cores", type = INTEGER, defaultValue = "-1"),
          @RestParameter(name = "memory", isRequired = false, description = "Filter results by the server memory available in bytes", type = INTEGER),
          @RestParameter(name = "path", isRequired = false, description = "Filter results by the server path", type = STRING),
          @RestParameter(name = "maxjobs", isRequired = false, description = "Filter results by the maximum of jobs that can be run at the same time", type = INTEGER, defaultValue = "-1"),
          @RestParameter(name = "sort", isRequired = false, description = "The sort order.  May include any "
                  + "of the following: STATUS, NAME, CORES, COMPLETED (jobs), RUNNING (jobs), QUEUED (jobs), QUEUETIME (mean for jobs), MAINTENANCE, RUNTIME (mean for jobs)."
                  + "Add '_DESC' to reverse the sort order (e.g. NAME_DESC).", type = STRING) }, reponses = { @RestResponse(description = "Returns the list of jobs from Matterhorn", responseCode = HttpServletResponse.SC_OK) }, returnDescription = "The list ")
  public Response getServers(@QueryParam("limit") final int limit, @QueryParam("offset") final int offset,
          @QueryParam("online") boolean fOnline, @QueryParam("offline") boolean fOffline,
          @QueryParam("q") String fText, @QueryParam("maintenance") boolean fMaintenance,
          @QueryParam("types") List<String> fTypes, @QueryParam("ipaddress") String ipAddress,
          @QueryParam("cores") Integer fCores, @QueryParam("memory") Integer fMemory, @QueryParam("path") String fPath,
          @QueryParam("maxjobs") int fMaxJobs, @QueryParam("sort") String sort, @Context HttpHeaders headers)
          throws Exception {

    JSONArray jsonList = new JSONArray();
    List<HostRegistration> allServers = serviceRegistry.getHostRegistrations();
    List<JSONObject> sortedServers = new ArrayList<JSONObject>();

    int i = 0;
    for (HostRegistration server : allServers) {
      if (i++ < offset) {
        continue;
      } else if (limit != 0 && sortedServers.size() == limit) {
        break;
      } else {

        // Get all the services statistics pro host
        // TODO improve the service registry to get service statistics by host
        List<ServiceStatistics> servicesStatistics = serviceRegistry.getServiceStatistics();
        int jobsCompleted = 0;
        int jobsRunning = 0;
        int jobsQueued = 0;
        int sumMeanRuntime = 0;
        int sumMeanQueueTime = 0;
        int totalServiceOnHost = 0;
        Set<String> serviceTypes = new HashSet<String>();
        for (ServiceStatistics serviceStat : servicesStatistics) {
          if (server.getBaseUrl().equals(serviceStat.getServiceRegistration().getHost())) {
            totalServiceOnHost++;
            jobsCompleted += serviceStat.getFinishedJobs();
            jobsRunning += serviceStat.getRunningJobs();
            jobsQueued += serviceStat.getQueuedJobs();
            sumMeanRuntime += serviceStat.getMeanRunTime();
            sumMeanQueueTime += serviceStat.getQueuedJobs();
            serviceTypes.add(serviceStat.getServiceRegistration().getServiceType());
          }
        }
        int meanRuntime = sumMeanRuntime / totalServiceOnHost;
        int meanQueueTime = sumMeanQueueTime / totalServiceOnHost;

        boolean vOnline = server.isOnline();
        boolean vMaintenance = server.isMaintenanceMode();
        String vName = server.getBaseUrl();
        int vCores = server.getCores();
        int vRunning = jobsRunning;
        int vQueued = jobsQueued;
        int vCompleted = jobsCompleted;

        if (fOffline && vOnline)
          continue;
        if (fOnline && !vOnline)
          continue;
        if (fMaintenance && !vMaintenance)
          continue;
        if (fMaxJobs > 0 && fMaxJobs < (vRunning + vQueued + vCompleted))
          continue;
        if (fCores != null && fCores > 0 && fCores != vCores)
          continue;
        if (fMemory != null && fMemory > 0 && ((Integer) fMemory).longValue() != server.getMemory())
          continue;
        if (StringUtils.isNotBlank(fPath) && !vName.toLowerCase().contains(fPath.toLowerCase()))
          continue;
        if (StringUtils.isNotBlank(fText) && !vName.toLowerCase().contains(fText.toLowerCase())) {
          String allString = vName.toLowerCase().concat(server.getIpAddress().toLowerCase());
          if (!allString.contains(fText.toLowerCase()))
            continue;
        }

        JSONObject jsonServer = new JSONObject();
        jsonServer.put(KEY_ONLINE, server.isOnline());
        jsonServer.put(KEY_MAINTENANCE, server.isMaintenanceMode());
        jsonServer.put(KEY_NAME, server.getBaseUrl());
        jsonServer.put(KEY_CORES, server.getCores());
        jsonServer.put(KEY_RUNNING, jobsRunning);
        jsonServer.put(KEY_QUEUED, jobsQueued);
        jsonServer.put(KEY_COMPLETED, jobsCompleted);
        jsonServer.put(KEY_MEAN_RUN_TIME, meanRuntime);
        jsonServer.put(KEY_MEAN_QUEUE_TIME, meanQueueTime);
        sortedServers.add(jsonServer);
      }
    }

    // Sorting
    SORT sortKey = SORT.HOSTNAME;
    Boolean ascending = true;
    if (StringUtils.isNotBlank(sort)) {
      // Parse the sort field and direction
      Sort sortField = null;
      if (sort.endsWith(DESCENDING_SUFFIX)) {
        ascending = false;
        String enumKey = sort.substring(0, sort.length() - DESCENDING_SUFFIX.length()).toUpperCase();
        try {
          sortKey = SORT.valueOf(enumKey);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", enumKey);
        }
      } else {
        try {
          sortKey = SORT.valueOf(sort);
        } catch (IllegalArgumentException e) {
          logger.warn("No sort enum matches '{}'", sort);
        }
      }
    }
    Collections.sort(sortedServers, new ServerComparator(sortKey, ascending));

    jsonList.addAll(sortedServers);

    JSONObject response = new JSONObject();
    response.put("results", jsonList);
    response.put("count", jsonList.size());
    response.put("offset", offset);
    response.put("limit", limit);
    response.put("total", allServers.size());

    return Response.ok(response.toString()).build();
  }
}
