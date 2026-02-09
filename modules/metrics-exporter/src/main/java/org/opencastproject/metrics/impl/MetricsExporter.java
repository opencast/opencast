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

package org.opencastproject.metrics.impl;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceState;
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.storage.StorageUsage;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;

/**
 * Opencast metrics endpoint
 */
@Path("/metrics")
@Component(
    property = {
        "service.description=Metrics Endpoint",
        "opencast.service.type=org.opencastproject.metrics",
        "opencast.service.path=/metrics",
        "opencast.service.jobproducer=false"
    },
    immediate = true,
    service = MetricsExporter.class
)
@RestService(
    name = "MetricsEndpoint",
    title = "Metrics Endpoint",
    abstractText = "Opencast metrics endpoint.",
    notes = {
        "The endpoints supports the <a href=https://openmetrics.io>OpenMetrics format</a>",
        "This can be used by <a href=https://prometheus.io>Prometheus</a>"
    }
)
@JaxrsResource
public class MetricsExporter {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(MetricsExporter.class);

  private final List<StorageUsage> storageUsageImplementations = new ArrayList<>();

  // Prometheus metrics registry for exposing metrics
  private final CollectorRegistry registry = CollectorRegistry.defaultRegistry;

  private final Counter requests = Counter.build()
      .name("requests_total")
      .help("Total requests.")
      .register();
  private final Gauge jobLoadMax = Gauge.build()
      .name("opencast_job_load_max")
      .help("Maximum job load")
      .labelNames("host")
      .register();
  private final Gauge jobLoadCurrent = Gauge.build()
      .name("opencast_job_load_current")
      .help("Maximum job load")
      .labelNames("host")
      .register();
  private final Gauge jobsActive = Gauge.build()
      .name("opencast_job_active")
      .help("Active jobs")
      .labelNames("host", "organization")
      .register();
  private final Gauge workflowsActive = Gauge.build()
      .name("opencast_workflow_active")
      .help("Active workflows")
      .labelNames("organization")
      .register();
  private final Gauge servicesTotal = Gauge.build()
      .name("opencast_services_total")
      .help("Number of services in a cluster")
      .labelNames("state")
      .register();
  private final Gauge version = Gauge.build()
      .name("opencast_version")
      .help("Version of Opencast (based on metrics module)")
      .labelNames("part")
      .register();
  private final Gauge storageAvailable = Gauge.build()
      .name("opencast_storage_available")
      .help("Shows available and used up space of different directories of Opencast.")
      .labelNames("storage")
      .register();
  private Gauge eventsInAssetManager;

  /** OSGi services */
  private ServiceRegistry serviceRegistry;
  private OrganizationDirectoryService organizationDirectoryService;
  private AssetManager assetManager;

  @Activate
  public void activate(BundleContext bundleContext) {
    final Version version = bundleContext.getBundle().getVersion();
    this.version.labels("major").set(version.getMajor());
    this.version.labels("minor").set(version.getMinor());
    DefaultExports.initialize();
  }

  @GET
  @Path("/")
  @Produces(TextFormat.CONTENT_TYPE_OPENMETRICS_100)
  @RestQuery(name = "metrics",
      description = "Metrics about Opencast",
      responses = {@RestResponse(description = "Metrics", responseCode = HttpServletResponse.SC_OK)},
      returnDescription = "OpenMetrics about Opencast.")
  public Response metrics() throws Exception {
    // track requests
    requests.inc();

    // track service states
    final List<ServiceState> serviceStates = serviceRegistry.getServiceRegistrations().parallelStream()
        .map(ServiceRegistration::getServiceState)
        .collect(Collectors.toList());
    final long error = serviceStates.parallelStream().filter(ServiceState.ERROR::equals).count();
    final long warn = serviceStates.parallelStream().filter(ServiceState.WARNING::equals).count();
    servicesTotal.labels(ServiceState.NORMAL.name()).set(serviceStates.size() - error - warn);
    servicesTotal.labels(ServiceState.WARNING.name()).set(warn);
    servicesTotal.labels(ServiceState.ERROR.name()).set(error);

    // track host loads
    for (SystemLoad.NodeLoad nodeLoad: serviceRegistry.getCurrentHostLoads().getNodeLoads()) {
      jobLoadCurrent.labels(nodeLoad.getHost()).set(nodeLoad.getCurrentLoad());
      jobLoadMax.labels(nodeLoad.getHost()).set(nodeLoad.getMaxLoad());
    }

    // set workflows by organization
    for (var entry: serviceRegistry.countActiveTypeByOrganization("START_WORKFLOW").entrySet()) {
      workflowsActive.labels(entry.getKey()).set(entry.getValue());
    }

    // set jobs by organization and host
    for (var entry: serviceRegistry.countActiveByOrganizationAndHost().entrySet()) {
      final var org = entry.getKey();
      for (Map.Entry<String, Long> orgEntry: entry.getValue().entrySet()) {
        final var host = orgEntry.getKey();
        final var count = orgEntry.getValue();
        jobsActive.labels(host, org).set(count);
      }
    }

    // Get numbers from asset manager
    if (assetManager != null && organizationDirectoryService.getOrganizations().size() == 1) {
      var org = organizationDirectoryService.getOrganizations().get(0).getId();
      eventsInAssetManager
          .labels(org)
          .set(assetManager.countEvents(null));
    } else if (assetManager != null) {
      for (Organization organization: organizationDirectoryService.getOrganizations()) {
        eventsInAssetManager
            .labels(organization.getId())
            .set(assetManager.countEvents(organization.getId()));
      }
    }

    for (StorageUsage storageUsageImplementation : storageUsageImplementations) {
      String prefix = storageUsageImplementation.getClass().getSimpleName() + "_";
      storageAvailable.labels(prefix + "totalSpace").set(storageUsageImplementation.getTotalSpace().orElse(-1L));
      storageAvailable.labels(prefix + "usableSpace").set(storageUsageImplementation.getUsableSpace().orElse(-1L));
      storageAvailable.labels(prefix + "usedSpace").set(storageUsageImplementation.getUsedSpace().orElse(-1L));
    }

    // collect metrics
    final StringWriter writer = new StringWriter();
    TextFormat.writeOpenMetrics100(writer, registry.metricFamilySamples());
    return Response.ok().entity(writer.toString()).build();
  }

  @Reference
  public void setServiceRegistry(ServiceRegistry service) {
    this.serviceRegistry = service;
  }

  @Reference(
      cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.DYNAMIC,
      unbind = "removeStorageUsage"
  )
  protected synchronized void addStorageUsage(StorageUsage storageUsageImplementation) {
    storageUsageImplementations.add(storageUsageImplementation);
  }

  protected synchronized void removeStorageUsage(StorageUsage storageUsageImplementation) {
    storageUsageImplementations.remove(storageUsageImplementation);
  }

  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  @Reference(
      policy = ReferencePolicy.DYNAMIC,
      cardinality = ReferenceCardinality.OPTIONAL,
      unbind = "unsetAssetManager"
  )
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
    eventsInAssetManager = Gauge.build()
        .name("opencast_asset_manager_events")
        .help("Events in Asset Manager")
        .labelNames("organization")
        .register();
  }

  public void unsetAssetManager(AssetManager assetManager) {
    if (this.assetManager == assetManager) {
      this.assetManager = null;
      registry.unregister(eventsInAssetManager);
    }
  }

}
