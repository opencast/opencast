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
import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

@Component(
        property = {
                "service.description=Index Rebuild Service"
        },
        immediate = true,
        service = { IndexRebuildService.class }
)
public class IndexRebuildService implements BundleActivator {

  private static final Logger logger = LoggerFactory.getLogger(IndexRebuildService.class);

  /** The message receiver */
  private MessageReceiver messageReceiver;

  /** An Executor to get messages */
  private ExecutorService executor = Executors.newSingleThreadExecutor();

  private Map<IndexRecreateObject.Service, IndexProducer> indexProducers = new HashMap<>();

  private ServiceRegistration serviceRegistration = null;

  @Reference(name = "messageReceiver")
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

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
    if (indexProducers.size() == IndexRecreateObject.Service.values().length) {
      registerIndexRebuildService(bundleContext);
    } else {  // wait for the rest
      bundleContext.addServiceListener(new ServiceListener() {

        @Override
        public void serviceChanged(ServiceEvent serviceEvent) {
          if (serviceEvent.getType() == ServiceEvent.REGISTERED) {
            ServiceReference serviceReference = serviceEvent.getServiceReference();
            addIndexProducer((IndexProducer) bundleContext.getService(serviceReference));

            if (indexProducers.size() == IndexRecreateObject.Service.values().length) {
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
    for (IndexRecreateObject.Service service: IndexRecreateObject.Service.values()) {
      recreateService(index, service);
    }
  }

  /**
   * Ask for data to be rebuilt from a service.
   *
   * @param service
   *          The {@link IndexRecreateObject.Service} representing the service to start re-sending the data from.
   * @throws InterruptedException
   *           Thrown if the process of re-sending the data is interupted.
   * @throws CancellationException
   *           Thrown if listening to messages has been canceled.
   * @throws ExecutionException
   *           Thrown if the process of re-sending the data has an error.
   */
  private void recreateService(AbstractSearchIndex index, IndexRecreateObject.Service service)
          throws InterruptedException, CancellationException, ExecutionException, IndexRebuildException {


    IndexProducer indexProducer = indexProducers.get(service);
    logger.info("Starting to recreate index for service '{}'", service);
    try {
      indexProducer.repopulate(index.getIndexName());
    } catch (Exception e) {
      throw new IndexRebuildException(format("Index Rebuild of Index %s for Service %s failed.", index.getIndexName(),
              service.name()), e);
    }

    boolean done = false;
    while (!done) {
      FutureTask<Serializable> future = messageReceiver.receiveSerializable(IndexProducer.RESPONSE_QUEUE,
              MessageSender.DestinationType.Queue);
      executor.execute(future);
      BaseMessage message = (BaseMessage) future.get();
      if (message.getObject() instanceof IndexRecreateObject) {
        IndexRecreateObject indexRecreateObject = (IndexRecreateObject) message.getObject();
        switch (indexRecreateObject.getStatus()) {
          case Update:
            logger.info("Updating service: '{}' with {}/{} finished, {}% complete.",
                indexRecreateObject.getService(),
                indexRecreateObject.getCurrent(),
                indexRecreateObject.getTotal(),
                (int) (indexRecreateObject.getCurrent() * 100 / indexRecreateObject.getTotal())
            );
            if (indexRecreateObject.getCurrent() == indexRecreateObject.getTotal()) {
              logger.info("Waiting for service '{}' indexing to complete", indexRecreateObject.getService());
            }
            break;
          case End:
            done = true;
            logger.info("Finished re-creating data for service '{}'", indexRecreateObject.getService());
            break;
          case Error:
            logger.error("Error updating service '{}' with {}/{} finished.",
                    indexRecreateObject.getService(), indexRecreateObject.getCurrent(),
                    indexRecreateObject.getTotal());
            throw new IndexRebuildException(
                    format("Error updating service '%s' with %s/%s finished.", indexRecreateObject.getService(),
                            indexRecreateObject.getCurrent(), indexRecreateObject.getTotal()));
          default:
            logger.error("Unable to handle the status '{}' for service '{}'", indexRecreateObject.getStatus(),
                    indexRecreateObject.getService());
            throw new IllegalArgumentException(format("Unable to handle the status '%s' for service '%s'",
                    indexRecreateObject.getStatus(), indexRecreateObject.getService()));

        }
      }
    }
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

    IndexRecreateObject.Service service = IndexRecreateObject.Service.valueOf(serviceName);
    recreateService(index, service);
  }
}
