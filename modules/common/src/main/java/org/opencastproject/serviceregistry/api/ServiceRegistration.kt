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

package org.opencastproject.serviceregistry.api

import java.util.Date

/**
 * Manages clustered services and the Jobs they may create to enable asynchronous job handling.
 */
interface ServiceRegistration {

    /**
     * @return the type of service
     */
    val serviceType: String

    /**
     * @return the host providing the service endpoint.
     */
    val host: String

    /**
     * @return The relative path to the service endpoint.
     */
    val path: String

    /**
     * @return Whether the service performs long running operations using Jobs.
     */
    val isJobProducer: Boolean

    /**
     * @return Whether the service is active
     */
    val isActive: Boolean

    /**
     * @return Whether the service is online
     */
    val isOnline: Boolean

    /**
     * Whether the service is in maintenance mode. If a server was in maintenance mode when shut down, it will remain in
     * maintenance mode when it comes back online
     */
    val isInMaintenanceMode: Boolean

    /**
     * Gets the last time the service has been declared online
     *
     * @return the onlineFrom
     */
    val onlineFrom: Date

    /**
     * Gets the current state of the service
     *
     * @return current state
     */
    val serviceState: ServiceState

    /**
     * Gets the last date when state was changed
     *
     * @return last date when state was changed
     */
    val stateChanged: Date

    /**
     * Gets the job signature which changed last time the service state to error.
     *
     * @return the signature from error state trigger job
     */
    val errorStateTrigger: Int

    /**
     * Gets the job signature which changed last time the service state to warning
     *
     * @return the signature from warning state trigger job
     */
    val warningStateTrigger: Int

}
