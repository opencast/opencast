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

package org.opencastproject.util.persistence

import javax.persistence.spi.PersistenceProvider

/**
 * Definition of a contract for the use of a [PersistenceEnv] in an OSGi environment.
 *
 *
 * Use in conjunction with [PersistenceEnvBuilder].
 */
interface OsgiPersistenceEnvUser {

    /**
     * Return the persistence environment.
     *
     *
     * Create the persistence environment in the activate method like so:
     * <pre>
     * penv = PersistenceEnvs.persistenceEnvironment(persistenceProvider, "my.persistence.context", persistenceProperties);
    </pre> *
     * Or better use the [PersistenceEnvBuilder].
     */
    val penv: PersistenceEnv

    /** OSGi callback to set persistence properties.  */
    fun setPersistenceProperties(persistenceProperties: Map<String, Any>)

    /** OSGi callback to set persistence provider.  */
    fun setPersistenceProvider(persistenceProvider: PersistenceProvider)

    /** Close the persistence environment [PersistenceEnv.close]. Call from the deactivate method.  */
    fun closePenv()
}
