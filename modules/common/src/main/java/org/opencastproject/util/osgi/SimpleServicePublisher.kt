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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */


package org.opencastproject.util.osgi

import org.opencastproject.util.data.Collections.dict
import org.opencastproject.util.data.Collections.list
import org.opencastproject.util.data.Monadics.mlist
import org.opencastproject.util.data.Tuple.tuple
import org.opencastproject.util.data.functions.Functions.noop
import org.opencastproject.util.data.functions.Functions.run
import org.osgi.framework.Constants.SERVICE_DESCRIPTION
import org.osgi.framework.Constants.SERVICE_PID

import org.opencastproject.util.data.Effect0
import org.opencastproject.util.persistence.PersistenceEnv

import org.osgi.framework.ServiceRegistration
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.Closeable
import java.util.Dictionary

/**
 * An implementation of SimpleServicePublisher creates and registers another service with the OSGi environment.
 *
 *
 * Using this class helps to decouple business logic from the OSGi environment. The business class can be kept clean
 * from any OSGi specific needs like dependency setters, activate/deactivate methods or the implementation of OSGi
 * related interfaces like [ManagedService] to receive configuration data. This makes the business logic far
 * easier to test and results in a mucher cleaner design.
 *
 *
 * Another approach to decouple the service logic from the OSGi environment is to create an abstract service class which
 * declares all it's dependencies as abstract getter methods. The OSGi connection is then implemented in an inheriting
 * class. This method, however, has the drawback of having to renounce a constructor implementation since no
 * dependencies have been set yet.
 */
abstract class SimpleServicePublisher : ManagedService {

    private var cc: ComponentContext? = null
    private var shutdown = noop

    class ServiceReg private constructor(val serviceRegistrations: List<ServiceRegistration<*>>, val onShutdown: List<Effect0>) {
        companion object {

            fun reg(serviceRegistrations: List<ServiceRegistration<*>>, onShutdown: List<Effect0>): ServiceReg {
                return ServiceReg(serviceRegistrations, onShutdown)
            }

            fun reg(serviceRegistration: ServiceRegistration<*>, onShutdown: Effect0): ServiceReg {
                return ServiceReg(list<ServiceRegistration>(serviceRegistration), list(onShutdown))
            }

            fun reg(serviceRegistration: ServiceRegistration<*>, vararg onShutdown: Effect0): ServiceReg {
                return ServiceReg(list<ServiceRegistration>(serviceRegistration), list(*onShutdown))
            }

            fun reg(onShutdown: Effect0, vararg serviceRegistration: ServiceRegistration<*>): ServiceReg {
                return ServiceReg(list<ServiceRegistration>(*serviceRegistration), list(onShutdown))
            }
        }
    }

    /**
     * Create and register a service object. Register with the OSGi environment using
     * [.registerService] or
     * `cc.getBundleContext().registerService(..)`.
     *
     * @param properties
     * a configuration object received by the [ManagedService.updated] callback,
     * never null
     * @return a structure of registered services and a list of (optional) effects to run on service shutdown
     */
    @Throws(ConfigurationException::class)
    abstract fun registerService(properties: Dictionary<*, *>, cc: ComponentContext?): ServiceReg

    /**
     * Return false if the service to be registered does not need a configuration dictionary provided by the ConfigAdmin.
     */
    abstract fun needConfig(): Boolean

    /** OSGi callback.  */
    @Synchronized
    @Throws(ConfigurationException::class)
    fun activate(cc: ComponentContext) {
        logger.info("[{}] Start", this.javaClass.getName())
        this.cc = cc
        if (!needConfig()) {
            updated(dict<Any, Any>())
        }
    }

    /** OSGi callback.  */
    @Synchronized
    fun deactivate() {
        logger.info("[{}] Stop", this.javaClass.getName())
        shutdown.apply()
    }

    @Synchronized
    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>?) {
        shutdown.apply()
        if (properties != null) {
            val self = this
            logger.info("[{}] Registering service", self.javaClass.getName())
            val registrations = registerService(properties, cc)
            shutdown = object : Effect0() {
                override fun run() {
                    logger.info("[{}] Unregister service", self.javaClass.getName())
                    for (reg in registrations.serviceRegistrations)
                        reg.unregister()
                    mlist(registrations.onShutdown).each(run)
                }
            }
        } else {
            logger.info("[{}] No config", this.javaClass.getName())
        }
    }

    companion object {
        /** Log facility  */
        private val logger = LoggerFactory.getLogger(SimpleServicePublisher::class.java!!)

        /**
         * Register a service with the OSGi environment in a standardized way. The service properties
         * [org.osgi.framework.Constants.SERVICE_PID] and [org.osgi.framework.Constants.SERVICE_DESCRIPTION] are
         * created properly.
         */
        fun registerService(cc: ComponentContext, o: Any, serviceClass: Class<*>,
                            serviceDescription: String): ServiceRegistration<*> {
            if (!serviceClass.isAssignableFrom(o.javaClass))
                throw Error("Service $o is not of type $serviceClass")
            val props = dict(tuple(SERVICE_PID, o.javaClass.getName()),
                    tuple(SERVICE_DESCRIPTION, serviceDescription))
            return cc.bundleContext.registerService(serviceClass.name, o, props)
        }

        /**
         * Create an effect to unregister a service suitable to return by
         * [.registerService].
         */
        fun unregisterService(sr: ServiceRegistration<*>): Effect0 {
            return object : Effect0() {
                override fun run() {
                    sr.unregister()
                }
            }
        }

        /**
         * Create an effect to close a closeable suitable to return by
         * [.registerService].
         */
        fun close(c: Closeable): Effect0 {
            return object : Effect0.X() {
                @Throws(Exception::class)
                override fun xrun() {
                    c.close()
                }
            }
        }

        /**
         * Create an effect to close a persistence environment suitable to return by
         * [.registerService].
         */
        fun close(penv: PersistenceEnv): Effect0 {
            return object : Effect0.X() {
                @Throws(Exception::class)
                override fun xrun() {
                    penv.close()
                }
            }
        }
    }
}
