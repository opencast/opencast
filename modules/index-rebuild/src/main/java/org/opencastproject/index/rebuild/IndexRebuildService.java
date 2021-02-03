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

package org.opencastproject.index.rebuild;

import static java.lang.String.format;

import org.opencastproject.elasticsearch.index.AbstractSearchIndex;

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

public class IndexRebuildService implements BundleActivator {

  /**
   * The services whose data is indexed by ElasticSearch.
   * Attention: The order is relevant for the index rebuild and should not be changed!
   */
  public enum Service {
    Groups, Acl, Themes, Series, Scheduler, Workflow, AssetManager, Comments
  }

  private static final Logger logger = LoggerFactory.getLogger(IndexRebuildService.class);
  private final Map<IndexRebuildService.Service, IndexProducer> indexProducers = new HashMap<>();
  private ServiceRegistration<?> serviceRegistration = null;

  @Override
  public void start(BundleContext bundleContext) throws Exception {

    // check if there are already indexProducers available
    ServiceReference<?>[] serviceReferences = bundleContext.getAllServiceReferences(IndexProducer.class.getName(),
            null);
    if (serviceReferences != null) {
      for (ServiceReference<?> serviceReference : serviceReferences) {
        addIndexProducer((IndexProducer) bundleContext.getService(serviceReference));
      }
    }

    // all available?
    if (indexProducers.size() == IndexRebuildService.Service.values().length) {
      registerIndexRebuildService(bundleContext);
    }

    // listen to changes in availability
    bundleContext.addServiceListener(new IndexProducerListener(bundleContext),
            "(objectClass=" + IndexProducer.class.getName() + ")");
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    unregisterIndexRebuildService();
  }

  /**
   * Recreate the index from all of the services that provide data for it.
   *
   * @param index
   *           The index to rebuild.
   *
   * @throws IOException
   *           Thrown if the index cannot be cleared.
   * @throws IndexRebuildException
   *           Thrown if the index rebuild failed.
   */
  public synchronized void recreateIndex(AbstractSearchIndex index)
          throws IOException, IndexRebuildException {
    index.clear();
    for (IndexRebuildService.Service service: IndexRebuildService.Service.values()) {
      recreateService(index, service);
    }
  }

  /**
   * Recreate the index from a specific service that provides data for it.
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
  public synchronized void recreateIndex(AbstractSearchIndex index, String serviceName)
          throws IllegalArgumentException, IndexRebuildException {
    IndexRebuildService.Service service = IndexRebuildService.Service.valueOf(serviceName);
    recreateService(index, service);
  }

  /**
   * Trigger repopulation of the specified index with data from the specified service.
   *
   * @param index
   *           The index to rebuild.
   * @param service
   *          The {@link IndexRebuildService.Service} to re-add data from.
   *
   * @throws IndexRebuildException
   *           Thrown if the index rebuild failed.
   */
  private void recreateService(AbstractSearchIndex index, IndexRebuildService.Service service)
          throws IndexRebuildException {

    if (!indexProducers.containsKey(service)) {
      throw new IllegalStateException(format("Service %s is not available", service));
    }

    IndexProducer indexProducer = indexProducers.get(service);
    logger.info("Starting to recreate index {} for service '{}'", index.getIndexName(), service);
    indexProducer.repopulate(index.getIndexName());
    logger.info("Finished to recreate index {} for service '{}'", index.getIndexName(), service);
  }

  /**
   * Add IndexProducer to internal map.
   *
   * @param indexProducer
   *           The IndexProducer to add.
   */
  private void addIndexProducer(IndexProducer indexProducer) {
    indexProducers.put(indexProducer.getService(), indexProducer);
    logger.info("Service {} added.", indexProducer.getService());
  }

  /**
   * Remove IndexProducer from internal map.
   *
   * @param indexProducer
   *           The IndexProducer to remove.
   */
  private void removeIndexProducer(IndexProducer indexProducer) {
    indexProducers.remove(indexProducer.getService(), indexProducer);
    logger.info("Service {} removed.", indexProducer.getService());
  }

  /**
   * Unregister this service at OSGI.
   */
  private void unregisterIndexRebuildService() {
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
  private void registerIndexRebuildService(BundleContext bundleContext) {
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
     * Constructor.
     *
     * @param bundleContext
     *           The bundle context.
     */
    private IndexProducerListener(BundleContext bundleContext) {
      this.bundleContext = bundleContext;
    }

    @Override
    public void serviceChanged(ServiceEvent serviceEvent) {
      if (serviceEvent.getType() == ServiceEvent.REGISTERED) {
        ServiceReference<?> serviceReference = serviceEvent.getServiceReference();
        addIndexProducer((IndexProducer) bundleContext.getService(serviceReference));

        // all found?
        if (indexProducers.size() == IndexRebuildService.Service.values().length) {
          registerIndexRebuildService(bundleContext);
        }
      } else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING) {
        ServiceReference<?> serviceReference = serviceEvent.getServiceReference();
        removeIndexProducer((IndexProducer) bundleContext.getService(serviceReference));

        // one missing now?
        if (indexProducers.size() != IndexRebuildService.Service.values().length) {
          unregisterIndexRebuildService();
        }
      }
    }
  }
}
