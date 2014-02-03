/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.util.osgi;

import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Dictionary;
import java.util.List;

import static org.opencastproject.util.data.Collections.dict;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.data.functions.Functions.noop;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_PID;

/**
 * An implementation of SimpleServicePublisher creates and registers another service with the OSGi environment.
 * <p/>
 * Using this class helps to decouple business logic from the OSGi environment. The business class can be kept clean
 * from any OSGi specific needs like dependency setters, activate/deactivate methods or the implementation of
 * OSGi related interfaces like {@link ManagedService} to receive configuration data. This makes the business
 * logic far easier to test and results in a mucher cleaner design.
 * <p/>
 * Another approach to decouple the service logic from the OSGi environment is to create an abstract service class
 * which declares all it's dependencies as abstract getter methods. The OSGi connection is then implemented in
 * an inheriting class. This method, however, has the drawback of having to renounce a constructor implementation
 * since no dependencies have been set yet.
 */
public abstract class SimpleServicePublisher implements ManagedService {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(SimpleServicePublisher.class);

  private ComponentContext cc;
  private Option<Effect0> shutdown = none();

  /**
   * Create and register a service object. Register with
   * the OSGi environment using {@link #registerService(org.osgi.service.component.ComponentContext, Object, Class, String)}
   * or <code>cc.getBundleContext().registerService(..)</code>.
   *
   * @param properties
   *         a configuration object received by the {@link ManagedService#updated(java.util.Dictionary)} callback, never null
   * @return an effect to unregister the published service(s) and to do other cleanup operations like closing resources.
   *         Consider using {@link #unregisterService(org.osgi.framework.ServiceRegistration)} and
   *         {@link #close(java.io.Closeable)}.
   */
  public abstract Tuple<List<ServiceRegistration>, Effect0> registerService(Dictionary properties, ComponentContext cc)
          throws ConfigurationException;

  /**
   * Return false if the service to be registered does not need a configuration dictionary provided
   * by the ConfigAdmin.
   */
  public abstract boolean needConfig();

  /** OSGi callback. */
  public synchronized void activate(ComponentContext cc) throws ConfigurationException {
    logger.info("[{}] Start", this.getClass().getName());
    this.cc = cc;
    if (!needConfig()) {
      updated(dict());
    }
  }

  /** OSGi callback. */
  public synchronized void deactivate() {
    logger.info("[{}] Stop", this.getClass().getName());
    unregisterService();
  }

  @Override
  public synchronized void updated(Dictionary properties) throws ConfigurationException {
    unregisterService();
    if (properties != null) {
      logger.info("[{}] Registering service", this.getClass().getName());
      final Tuple<List<ServiceRegistration>, Effect0> registrations = registerService(properties, cc);
      shutdown = some(mlist(registrations.getA()).foldl(noop, new Function2<Effect0, ServiceRegistration, Effect0>() {
        @Override public Effect0 apply(Effect0 e, final ServiceRegistration svcReg) {
          return e.then(unregisterService(svcReg)).toEffect();
        }
      }).then(registrations.getB()).toEffect());
    } else {
      logger.info("[{}] No config", this.getClass().getName());
    }
  }

  /**
   * Register a service with the OSGi environment in a standardized way. The service properties
   * {@link org.osgi.framework.Constants#SERVICE_PID} and {@link org.osgi.framework.Constants#SERVICE_DESCRIPTION}
   * are created properly.
   */
  public static ServiceRegistration registerService(ComponentContext cc,
                                                    Object o, Class serviceClass,
                                                    String serviceDescription) {
    if (!serviceClass.isAssignableFrom(o.getClass()))
      throw new Error("Service " + o + " is not of type " + serviceClass);
    final Dictionary props = dict(
            tuple(SERVICE_PID, o.getClass().getName()),
            tuple(SERVICE_DESCRIPTION, serviceDescription));
    return cc.getBundleContext().registerService(serviceClass.getName(), o, props);
  }

  /**
   * Create an effect to unregister a service suitable to return by
   * {@link #registerService(java.util.Dictionary, org.osgi.service.component.ComponentContext)}.
   */
  public static Effect0 unregisterService(final ServiceRegistration sr) {
    return new Effect0() {
      @Override protected void run() {
        sr.unregister();
      }
    };
  }

  /**
   * Create an effect to close a closeable suitable to return by
   * {@link #registerService(java.util.Dictionary, org.osgi.service.component.ComponentContext)}.
   */
  public static Effect0 close(final Closeable c) {
    return new Effect0.X() {
      @Override protected void xrun() throws Exception {
        c.close();
      }
    };
  }

  private void unregisterService() {
    for (Effect0 s : shutdown) {
      logger.info("[{}] Unregister service", this.getClass().getName());
      s.apply();
    }
  }
}
