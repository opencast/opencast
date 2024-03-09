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

import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.HostStatistics;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.requests.SortCriterion;
import org.opencastproject.util.requests.SortCriterion.Order;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/admin-ng/server")
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

  private static final Gson gson = new Gson();

  private enum Sort {
    CORES, HOSTNAME, MAINTENANCE, NODENAME, ONLINE, QUEUED, RUNNING
  }

  private enum Status {
    ONLINE, OFFLINE, MAINTENANCE
  }

  // List of filter keys
  private static final String KEY_HOSTNAME = "hostname";
  private static final String KEY_NODE_NAME = "nodeName";
  private static final String KEY_STATUS = "status";
  private static final String KEY_TEXT_FILTER = "textFilter";

  /** Cache time */
  private static final long CACHE_SECONDS = 60;

  /**
   * Comparator for the servers list
   */
  private class ServerComparator implements Comparator<Server> {

    private final Sort sortType;
    private final boolean ascending;

    ServerComparator(Sort sortType, Boolean ascending) {
      this.sortType = sortType;
      this.ascending = ascending;
    }

    @Override
    public int compare(Server host1, Server host2) {
      return (ascending ? 1 : -1) * compareByType(host1, host2);
    }

    private int compareByType(final Server host1, final Server host2) {
      switch (sortType) {
        case ONLINE:
          return Boolean.compare(host1.online, host2.online);
        case CORES:
          return Long.compare(host1.cores, host2.cores);
        case QUEUED:
          return Long.compare(host1.queued, host2.queued);
        case MAINTENANCE:
          return Boolean.compare(host1.maintenance, host2.maintenance);
        case RUNNING:
          return Long.compare(host1.running, host2.running);
        case NODENAME:
          return host1.nodeName.compareTo(host2.nodeName);
        case HOSTNAME:
        default:
          return host1.hostname.compareTo(host2.hostname);
      }
    }
  }

  private class Server {
    protected boolean online;
    protected boolean maintenance;
    protected String hostname;
    protected String nodeName;
    protected long cores;
    protected long running;
    protected long queued;
  }

  private static final Logger logger = LoggerFactory.getLogger(ServerEndpoint.class);

  private ServiceRegistry serviceRegistry;

  private long lastUpdated = 0;
  private final List<Server> serverData = new ArrayList<>();

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
          @RestParameter(name = "sort", description = "The sort order.  May include any of the following: "
                  + "CORES, HOSTNAME, MAINTENANCE, ONLINE, QUEUED (jobs), RUNNING (jobs)."
                  + "The suffix must be :ASC for ascending or :DESC for descending sort order (e.g. HOSTNAME:DESC).", isRequired = false, type = STRING) },
          responses = { @RestResponse(description = "Returns the list of jobs from Opencast", responseCode = HttpServletResponse.SC_OK) },
          returnDescription = "The list of servers")
  public Response getServers(@QueryParam("limit") int limit, @QueryParam("offset") int offset,
          @QueryParam("filter") String filter, @QueryParam("sort") String sort)
          throws Exception {

    final Map<String, String> filters;
    try {
      filters = Arrays.stream(StringUtils.split(Objects.toString(filter, ""), ","))
          .map(f -> f.split(":", 2))
          .collect(Collectors.toMap(f -> f[0], f -> f[1]));
    } catch (ArrayIndexOutOfBoundsException e) {
      return badRequest("Invalid filter string", e);
    }

    List<Server> servers = new ArrayList<>();
    for (Server server: getServerData()) {
      if (!filters.getOrDefault(KEY_HOSTNAME, server.hostname).equalsIgnoreCase(server.hostname)) {
        continue;
      }

      if (!StringUtils.equalsIgnoreCase(filters.getOrDefault(KEY_NODE_NAME, server.nodeName), server.nodeName)) {
        continue;
      }

      if (filters.containsKey(KEY_STATUS)) {
        final Status status = Status.valueOf(filters.get(KEY_STATUS).toUpperCase());
        if (Status.ONLINE.equals(status) && !server.online) {
          continue;
        }
        if (Status.OFFLINE.equals(status) && server.online) {
          continue;
        }
        if (Status.MAINTENANCE.equals(status) && !server.maintenance) {
          continue;
        }
      }

      final String text = filters.getOrDefault(KEY_TEXT_FILTER, "");
      if (Stream.of(server.hostname, server.nodeName).noneMatch(v -> StringUtils.containsIgnoreCase(v, text))) {
        continue;
      }

      servers.add(server);
    }

    // Sorting
    Sort sortKey = Sort.HOSTNAME;
    boolean ascending = true;
    if (StringUtils.isNotBlank(sort)) {
      try {
        SortCriterion sortCriterion = RestUtils.parseSortQueryParameter(sort).iterator().next();
        sortKey = Sort.valueOf(sortCriterion.getFieldName().toUpperCase());
        ascending = Order.Ascending == sortCriterion.getOrder() || Order.None == sortCriterion.getOrder();
      } catch (WebApplicationException | IllegalArgumentException e) {
        return badRequest(String.format("Invalid sort parameter `%s`", sort), e);
      }
    }
    servers.sort(new ServerComparator(sortKey, ascending));

    offset = Math.min(offset, servers.size());
    final List<Server> serverResults;
    try {
      serverResults = servers.subList(offset, Math.min(servers.size(), limit + offset));
    } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
      return badRequest("Invalid offset and limit", e);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("total", servers.size());
    result.put("offset", offset);
    result.put("count", serverResults.size());
    result.put("limit", limit);
    result.put("results", serverResults);
    return Response.ok(gson.toJson(result)).build();
  }

  /**
   * Get service statistics for all hosts and services
   * @return List of all servers
   * @throws ServiceRegistryException
   *          If the host data could not be retrieved
   */
  private synchronized List<Server> getServerData() throws ServiceRegistryException {
    // Check if cache is still valid
    if (lastUpdated + CACHE_SECONDS > Instant.now().getEpochSecond()) {
      logger.debug("Using server data cache.");
      return serverData;
    }

    // Update cache
    serverData.clear();
    logger.debug("Updating server data");
    HostStatistics statistics = serviceRegistry.getHostStatistics();
    for (HostRegistration host : serviceRegistry.getHostRegistrations()) {
      // Calculate statistics per server
      Server server = new Server();
      server.online = host.isOnline();
      server.maintenance = host.isMaintenanceMode();
      server.hostname = host.getBaseUrl();
      server.nodeName = host.getNodeName();
      server.cores = host.getCores();
      server.running = statistics.runningJobs(host.getId());
      server.queued = statistics.queuedJobs(host.getId());
      serverData.add(server);
    }
    lastUpdated = Instant.now().getEpochSecond();
    return serverData;
  }

  /**
   * Return a bad request response but log additional details in debug mode.
   *
   * @param message
   *          Message to send
   * @param e
   *          Exception to log. If <pre>null</pre>, a new exception is created to log a stack trace.
   * @return 400 BAD REQUEST HTTP response
   */
  private Response badRequest(final String message, final Exception e) {
    logger.debug(message, e == null && logger.isDebugEnabled() ? new IllegalArgumentException(message) : e);
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(gson.toJson(message))
        .build();
  }

}
