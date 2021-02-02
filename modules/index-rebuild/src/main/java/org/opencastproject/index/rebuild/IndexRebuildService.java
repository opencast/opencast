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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class IndexRebuildService implements BundleActivator {

  /**
   * The Services whose data is indexed by ElasticSearch
   * Attention: The order is relevant for the index rebuild and should not be changed!
   */
  public enum Service {
    Groups, Acl, Themes, Series, Scheduler, Workflow, AssetManager, Comments
  }

  private static final Logger logger = LoggerFactory.getLogger(IndexRebuildService.class);

  private Map<IndexRebuildService.Service, IndexProducer> indexProducers = new HashMap<>();

  private ServiceRegistration serviceRegistration = null;

  private void addIndexProducer(IndexProducer indexProducer) {
    indexProducers.put(indexProducer.getService(), indexProducer);
    logger.info("Service {} registered.", indexProducer.getService());
  }

  private void registerIndexRebuildService(BundleContext bundleContext) {
    logger.info("All Services registered.");
    serviceRegistration = bundleContext.registerService(this.getClass().getName(), IndexRebuildService.this, null);
  }

  @Override
  public void start(BundleContext bundleContext) throws Exception {

    // check if there are already indexProducers available
    ServiceReference<?>[] serviceReferences = bundleContext.getAllServiceReferences(IndexProducer.class.getName(), null);
    if (serviceReferences != null) {
      for (ServiceReference serviceReference : serviceReferences) {
        addIndexProducer((IndexProducer) bundleContext.getService(serviceReference));
      }
    }

    // all available?
    if (indexProducers.size() == IndexRebuildService.Service.values().length) {
      registerIndexRebuildService(bundleContext);
    } else {  // wait for the rest
      bundleContext.addServiceListener(new ServiceListener() {

        @Override
        public void serviceChanged(ServiceEvent serviceEvent) {
          if (serviceEvent.getType() == ServiceEvent.REGISTERED) {
            ServiceReference serviceReference = serviceEvent.getServiceReference();
            addIndexProducer((IndexProducer) bundleContext.getService(serviceReference));

            if (indexProducers.size() == IndexRebuildService.Service.values().length) {
              registerIndexRebuildService(bundleContext);
              bundleContext.removeServiceListener(this);
            }
          }
        }
      }, "(objectClass=" + IndexProducer.class.getName() + ")");
    }
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    if (serviceRegistration != null)  {
      serviceRegistration.unregister();
    }
  }

  /**
   * Recreate the index from all of the services that provide data.
   *
   * @throws InterruptedException
   *           Thrown if the process is interupted.
   * @throws CancellationException
   *           Thrown if listeing to messages has been canceled.
   * @throws ExecutionException
   *           Thrown if there is a problem executing the process.
   * @throws IOException
   *           Thrown if the index cannot be cleared.
   */
  public synchronized void recreateIndex(AbstractSearchIndex index)
          throws InterruptedException, CancellationException, ExecutionException, IOException, IndexRebuildException {
    index.clear();
    for (IndexRebuildService.Service service: IndexRebuildService.Service.values()) {
      recreateService(index, service);
    }
  }

  /**
   * Ask for data to be rebuilt from a service.
   *
   * @param service
   *          The {@link IndexRebuildService.Service} representing the service to start re-sending the data from.
   * @throws InterruptedException
   *           Thrown if the process of re-sending the data is interupted.
   * @throws CancellationException
   *           Thrown if listening to messages has been canceled.
   * @throws ExecutionException
   *           Thrown if the process of re-sending the data has an error.
   */
  private void recreateService(AbstractSearchIndex index, IndexRebuildService.Service service)
          throws InterruptedException, CancellationException, ExecutionException, IndexRebuildException {

    IndexProducer indexProducer = indexProducers.get(service);
    logger.info("Starting to recreate index {} for service '{}'", index.getIndexName(), service);
    try {
      indexProducer.repopulate(index.getIndexName());
    } catch (Exception e) {
      logger.error("Error recreating index {} for service '{}' ", index.getIndexName(), service);
      throw new IndexRebuildException(format("Index Rebuild of Index %s for Service %s failed.", index.getIndexName(),
              service.name()), e);
    }
    logger.info("Finished to recreate index {} for service '{}'", index.getIndexName(), service);
  }

  /**
   * Recreate the index from a specific service that provide data.
   *
   * @param serviceName
   *           The service name. The available services are:
   *           Groups, Acl, Themes, Series, Scheduler, Workflow, AssetManager, Comments
   *
   * @throws IllegalArgumentException
   *           Thrown if the service name is invalid
   * @throws InterruptedException
   *           Thrown if the process is interupted.
   * @throws ExecutionException
   *           Thrown if there is a problem executing the process.
   */
  public synchronized void recreateIndex(AbstractSearchIndex index, String serviceName)
          throws IllegalArgumentException, InterruptedException, ExecutionException, IndexRebuildException {

    IndexRebuildService.Service service = IndexRebuildService.Service.valueOf(serviceName);
    recreateService(index, service);
  }
}
