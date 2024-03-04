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

package org.opencastproject.elasticsearch.index.rebuild;

import static java.lang.String.format;

import org.opencastproject.elasticsearch.index.ElasticsearchIndex;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The bundle activator is defined in the pom.xml of this bundle.
 */
public class IndexRebuildService implements BundleActivator {

  /*
   * How starting and stopping this service works:
   *
   * The Index Rebuild can only be started when all services that feed data into the ElasticSearch index, called
   * IndexProducers, are available via OSGI. To check for this, we use a service listener (see inner class at the
   * bottom) that reacts whenever an IndexProducer becomes available or is no longer available. We keep these
   * IndexProducers in an internal map.
   *
   * When our requirements - at least one IndexProducer of each type (defined by the Service enum below) available - are
   * fulfilled, we register the IndexRebuildService with OSGI so it can be used. If our requirements are no longer
   * fulfilled, we unregister it.
   *
   * We make this work by hooking into the OSGI lifecycle with the BundleActivator interface - this way we can start
   * the listener in the beginning and make sure we properly shut down in the end.
   */

  /**
   * The services whose data is indexed by ElasticSearch.
   * Attention: The order is relevant for the index rebuild and should not be changed!
   */
  public enum Service {
    Themes, Series, Scheduler, AssetManager, Comments, Workflow
  }

  public enum State {
    PENDING, RUNNING, OK, ERROR
  }

  public enum ServicePart {
    ACL
  }

  private static final Logger logger = LoggerFactory.getLogger(IndexRebuildService.class);
  private final Map<IndexRebuildService.Service, IndexProducer> indexProducers = new ConcurrentHashMap<>();
  private ServiceRegistration<?> serviceRegistration = null;

  /**
   * Called by OSGI when this bundle is started.
   *
   * @param bundleContext
   *         The bundle context.
   *
   * @throws Exception
   */
  @Override
  public void start(BundleContext bundleContext) throws Exception {
    // check if there are already IndexProducers available
    ServiceReference<?>[] serviceReferences = bundleContext.getAllServiceReferences(IndexProducer.class.getName(),
            null);
    if (serviceReferences != null) {
      for (ServiceReference<?> serviceReference : serviceReferences) {
        addIndexProducer((IndexProducer) bundleContext.getService(serviceReference), bundleContext);
      }
    }

    // Set rebuild service repopulation states to default values
    setAllRebuildStates(IndexRebuildService.State.OK);

    // listen to changes in availability
    bundleContext.addServiceListener(new IndexProducerListener(bundleContext),
            "(objectClass=" + IndexProducer.class.getName() + ")");
  }

  /**
   * Called by OSGI when this bundle is stopped.
   *
   * @param bundleContext
   *         The bundle context.
   *
   * @throws Exception
   */
  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    // unregister this service from OSGI
    unregisterIndexRebuildService();
  }

  /**
   * Clear and rebuild the index from all services.
   *
   * @param index
   *           The index to rebuild.
   *
   * @throws IOException
   *           Thrown if the index cannot be cleared.
   * @throws IndexRebuildException
   *           Thrown if the index rebuild failed.
   */
  public synchronized void rebuildIndex(ElasticsearchIndex index)
          throws IOException, IndexRebuildException {
    index.clear();
    logger.info("{} Index cleared, starting complete rebuild.", index.getIndexName());
    setAllRebuildStates(IndexRebuildService.State.PENDING);
    for (IndexRebuildService.Service service: IndexRebuildService.Service.values()) {
      rebuildIndex(index, service);
    }
  }

  /**
   * Partially rebuild the index from a specific service.
   *
   * @param index
   *           The index to rebuild.
   * @param serviceName
   *           The name of the {@link Service}.
   *
   * @throws IllegalArgumentException
   *           Thrown if the service doesn't exist.
   * @throws IndexRebuildException
   *           Thrown if the index rebuild failed.
   */
  public synchronized void rebuildIndex(ElasticsearchIndex index, String serviceName)
          throws IllegalArgumentException, IndexRebuildException {
    rebuildIndex(index, serviceName, null);
  }

  public synchronized void rebuildIndex(ElasticsearchIndex index, String serviceName, ServicePart part)
          throws IllegalArgumentException, IndexRebuildException {
    IndexRebuildService.Service service = IndexRebuildService.Service.valueOf("AssetManager");
    logger.info("Starting partial rebuild of the {} index from service '{}'.", index.getIndexName(), service);
    setRebuildState(service, IndexRebuildService.State.PENDING);
    rebuildIndex(index, service, part);
  }

  /**
   * Start Index Rebuild from the specified service and then do all that follow. Can be used to resume a complete index
   * rebuild that was interrupted.
   *
   * @param index
   *           The index to rebuild.
   * @param serviceName
   *           The name of the {@link Service} to start with.
   *
   * @throws IllegalArgumentException
   *           Thrown if the service doesn't exist.
   * @throws IndexRebuildException
   *           Thrown if the index rebuild failed.
   */
  public synchronized void resumeIndexRebuild(ElasticsearchIndex index, String serviceName)
          throws IllegalArgumentException, IndexRebuildException {
    IndexRebuildService.Service startingService = IndexRebuildService.Service.valueOf(serviceName);
    logger.info("Resuming rebuild of {} index with service '{}'.", index.getIndexName(), startingService);
    setSubsetOfRebuildStates(startingService, IndexRebuildService.State.PENDING);
    Service[] services = IndexRebuildService.Service.values();
    for (int i = startingService.ordinal(); i < services.length; i++) {
      rebuildIndex(index, services[i]);
    }
  }

  /**
   * Trigger repopulation of the index with data from a specific service.
   *
   * @param index
   *           The index to rebuild.
   * @param service
   *          The {@link IndexRebuildService.Service} to re-add data from.
   *
   * @throws IndexRebuildException
   *           Thrown if the index rebuild failed.
   */
  private void rebuildIndex(ElasticsearchIndex index, IndexRebuildService.Service service)
          throws IndexRebuildException {
    rebuildIndex(index, service, null);
  }

  private void rebuildIndex(ElasticsearchIndex index, IndexRebuildService.Service service, ServicePart part)
          throws IndexRebuildException {

    if (!indexProducers.containsKey(service)) {
      throw new IllegalStateException(format("Service %s is not available", service));
    }

    IndexProducer indexProducer = indexProducers.get(service);
    logger.info("Starting to rebuild the {} index from service '{}'", index.getIndexName(), service);
    setRebuildState(service, IndexRebuildService.State.RUNNING);
    try {
      indexProducer.repopulate(part);
      setRebuildState(service, IndexRebuildService.State.OK);
    } catch (IndexRebuildException e) {
      setRebuildState(service, IndexRebuildService.State.ERROR);
    }
    logger.info("Finished to rebuild the {} index from service '{}'", index.getIndexName(), service);
  }

  /**
   * Add IndexProducer service to internal map.
   *
   * @param indexProducer
   *           The IndexProducer to add.
   * @param bundleContext
   *           The bundle context.
   */
  private void addIndexProducer(IndexProducer indexProducer, BundleContext bundleContext) {
    // add only if there's not already a service of the same type in there
    if (indexProducers.putIfAbsent(indexProducer.getService(), indexProducer) == null) {
      logger.info("Service {} added.", indexProducer.getService());

      // all required IndexProducers found? Register this service at OSGI
      if (indexProducers.size() == IndexRebuildService.Service.values().length) {
        registerIndexRebuildService(bundleContext);
      }
    }
  }

  /**
   * Remove IndexProducer service from internal map.
   *
   * @param indexProducer
   *           The IndexProducer to remove.
   */
  private void removeIndexProducer(IndexProducer indexProducer) {
    // remove only if it's in there
    if (indexProducers.remove(indexProducer.getService(), indexProducer)) {
      logger.info("Service {} removed.", indexProducer.getService());

      // no longer all required IndexProducers available? Unregister this service from OSGI
      if (indexProducers.size() != IndexRebuildService.Service.values().length) {
        unregisterIndexRebuildService();
      }
    }
  }

  /**
   * Unregister this service from OSGI.
   */
  private synchronized void unregisterIndexRebuildService() {
    // if this service is registered with OSGI, unregister it
    if (serviceRegistration != null)  {
      logger.info("Unregister IndexRebuildService.");
      serviceRegistration.unregister();
      serviceRegistration = null;
    }
  }

  /**
   * Register this service at OSGI.
   *
   * @param bundleContext
   *           The bundle context.
   */
  private synchronized void registerIndexRebuildService(BundleContext bundleContext) {
    // if this service is not registered at OSGI, register it
    if (serviceRegistration == null) {
      logger.info("Register IndexRebuildService.");
      serviceRegistration = bundleContext.registerService(this.getClass().getName(), IndexRebuildService.this, null);
    }
  }

  /**
   * Listen to changes in the availability of IndexProducer services.
   */
  private final class IndexProducerListener implements ServiceListener {

    private final BundleContext bundleContext;

    /**
     * Constructor to hand over the bundle context.
     *
     * @param bundleContext
     *           The bundle context.
     */
    private IndexProducerListener(BundleContext bundleContext) {
      this.bundleContext = bundleContext;
    }

    @Override
    public void serviceChanged(ServiceEvent serviceEvent) {
      // new IndexProducer service available? Add to map
      if (serviceEvent.getType() == ServiceEvent.REGISTERED) {
        ServiceReference<?> serviceReference = serviceEvent.getServiceReference();
        addIndexProducer((IndexProducer) bundleContext.getService(serviceReference), bundleContext);

        // Index Producer no longer available? Remove from map
      } else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING) {
        ServiceReference<?> serviceReference = serviceEvent.getServiceReference();
        removeIndexProducer((IndexProducer) bundleContext.getService(serviceReference));
      }
    }
  }

  private final Map<Service, State> rebuildStates = new HashMap<>();

  /**
   * @return All rebuild service repopulation states.
   */
  public Map<String, String> getRebuildStates() {
    Map <String, String> statesAsString = new HashMap<>();
    for (Map.Entry<IndexRebuildService.Service,IndexRebuildService.State> entry : rebuildStates.entrySet()) {
      statesAsString.put(entry.getKey().toString(), entry.getValue().toString());
    }
    return statesAsString;
  }

  /**
   * @param service
   *           the rebuild service
   * @return the repopulation state of a single rebuild service.
   */
  public String getRebuildState(IndexRebuildService.Service service) {
    return rebuildStates.get(service).toString();
  }

  /**
   * Set all rebuild States.
   *
   * @param state
   *           the state to be set
   */
  private void setAllRebuildStates(IndexRebuildService.State state) {
    for (IndexRebuildService.Service service: IndexRebuildService.Service.values()) {
      setRebuildState(service, state);
    }
  }

  /**
   * Set a subset of rebuild States following the rebuild order.
   *
   * @param startingservice
   *           the service to start from
   * @param state
   *           the state to be set
   */
  private void setSubsetOfRebuildStates(IndexRebuildService.Service startingService, IndexRebuildService.State state) {
    Service[] services = IndexRebuildService.Service.values();
    for (int i = startingService.ordinal(); i < services.length; i++) {
      rebuildStates.put(services[i], state);
    }
  }

  /**
   * Set a single rebuild State.
   *
   * @param service
   *           the service to be set
   * @param state
   *           the state to be set
   */
  private void setRebuildState(IndexRebuildService.Service service, IndexRebuildService.State state) {
    rebuildStates.put(service, state);
  }
}
