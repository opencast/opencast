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

package org.opencastproject.serviceregistry.internal;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.RestUtil;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A custom ServiceTracker that registers all locally published servlets so clients can find the most appropriate
 * service on the network to handle new jobs.
 */
@Component(
    immediate = true,
    property = {
        "service.description=Service registry"
    }
)
public class RestServiceTracker extends ServiceTracker<Object, Object> {

  private static final Logger logger = LoggerFactory.getLogger(RestServiceTracker.class);

  protected BundleContext bundleContext;

  private final ServiceRegistry serviceRegistry;

  @Activate
  public RestServiceTracker(@Reference ServiceRegistry serviceRegistry, BundleContext bundleContext)
          throws InvalidSyntaxException {
    super(bundleContext, bundleContext.createFilter(RestConstants.SERVICES_FILTER), null);
    this.bundleContext = bundleContext;
    this.serviceRegistry = serviceRegistry;
    this.open(true);
  }

  @Deactivate
  void deactivate() {
    this.close();
  }

  @Override
  public Object addingService(ServiceReference reference) {
    // We need the service object to determine the service path from its @Path annotation
    Object service = super.addingService(reference);
    if (service == null) {
      logger.debug("REST service {} is not available, not registering it in the service registry", reference);
      return null;
    }

    String serviceType = (String) reference.getProperty(RestConstants.SERVICE_TYPE_PROPERTY);
    boolean publishFlag = Boolean.parseBoolean(
        Objects.requireNonNullElse(
          reference.getProperty(RestConstants.SERVICE_PUBLISH_PROPERTY), "true").toString()
    );
    boolean jobProducer = Boolean.parseBoolean(
        Objects.requireNonNullElse(
            reference.getProperty(RestConstants.SERVICE_JOBPRODUCER_PROPERTY), "false").toString()
    );

    // Only register services that have the "publish" flag set to "true"
    if (publishFlag) {
      try {
        String servicePath = RestUtil.getServicePath(service.getClass());
        serviceRegistry.registerService(serviceType, serviceRegistry.getRegistryHostname(), servicePath,
            jobProducer);
      } catch (ServiceRegistryException e) {
        logger.warn("Unable to register job producer of type " + serviceType + " on host "
            + serviceRegistry.getRegistryHostname());
      } catch (IllegalArgumentException e) {
        logger.warn("Unable to register service of type {}: {}", serviceType, e.getMessage());
      }
    } else {
      logger.debug("Not registering service " + serviceType + " in service registry by configuration");
    }

    return service;
  }

  @Override
  public void removedService(ServiceReference reference, Object service) {
    String serviceType = (String) reference.getProperty(RestConstants.SERVICE_TYPE_PROPERTY);
    boolean publishFlag = Boolean.parseBoolean(Objects.toString(
        reference.getProperty(RestConstants.SERVICE_PUBLISH_PROPERTY),
        "true"));

    // Services that have the "publish" flag set to "true" have been registered before.
    if (publishFlag) {
      try {
        serviceRegistry.unRegisterService(serviceType, serviceRegistry.getRegistryHostname());
      } catch (ServiceRegistryException e) {
        logger.warn("Unable to unregister job producer of type {} on host {}",
            serviceType, serviceRegistry.getRegistryHostname());
      }
    } else {
      logger.trace("Service {} was never registered", reference);
    }

    super.removedService(reference, service);
  }

}
