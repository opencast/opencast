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

package org.opencastproject.util.persistencefn

import org.opencastproject.util.data.Collections.map

import org.opencastproject.util.data.Function0
import org.opencastproject.util.data.Lazy

import javax.persistence.spi.PersistenceProvider

/**
 * Builder for persistence environments.
 * Useful in OSGi bound services where required properties are injected by the OSGi environment.
 */
class PersistenceEnvBuilder {
    private var persistenceProperties = map<String, Any>()
    private var persistenceProvider: PersistenceProvider? = null
    private var persistenceUnit: String? = null
    private val penv = Lazy.lazy(object : Function0<org.opencastproject.util.persistencefn.PersistenceEnv>() {
        override fun apply(): org.opencastproject.util.persistencefn.PersistenceEnv {
            if (persistenceProvider == null) {
                throw IllegalStateException("Persistence provider has not been set yet")
            }
            if (persistenceUnit == null) {
                throw IllegalStateException("Persistence unit has not been set yet")
            }
            return PersistenceEnvs.mk(persistenceProvider!!, persistenceUnit, persistenceProperties)
        }
    })

    constructor() {}

    constructor(persistenceUnit: String) {
        this.persistenceUnit = persistenceUnit
    }

    /** Set the mandatory name of the persistence unit.  */
    fun setPersistenceUnit(name: String) {
        this.persistenceUnit = name
    }

    /** Set the optional persistence properties.  */
    fun setPersistenceProperties(properties: Map<String, Any>) {
        this.persistenceProperties = properties
    }

    /** Set the mandatory persistence provider.  */
    fun setPersistenceProvider(provider: PersistenceProvider) {
        this.persistenceProvider = provider
    }

    /** Builds the persistence env. Always returns the same environment so it may be safely called multiple times.  */
    fun get(): PersistenceEnv? {
        return penv.value()
    }
}
