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

/**
 * Interface representing a host.
 */
interface HostRegistration {

    /**
     * @return the baseUrl for this host
     */
    /**
     * @param baseUrl
     * the baseUrl to set
     */
    var baseUrl: String

    /**
     * @return the IP address for this host
     */
    /**
     * @param address
     * the IP address to set
     */
    var ipAddress: String

    /**
     * @return the allocated memory of this host
     */
    /**
     * @param memory
     * the memory to set
     */
    var memory: Long

    /**
     * @return the available cores of this host
     */
    /**
     * @param cores
     * the cores to set
     */
    var cores: Int

    /**
     * @return the maxLoad
     */
    /**
     * @param maxLoad
     * the maxLoad to set
     */
    var maxLoad: Float

    /**
     * @return whether this host is active
     */
    /**
     * @param active
     * the active status to set
     */
    var isActive: Boolean

    /**
     * @return whether this host is online
     */
    /**
     * @param online
     * the online status to set
     */
    var isOnline: Boolean

    /**
     * @return the maintenanceMode
     */
    /**
     * @param maintenanceMode
     * the maintenanceMode to set
     */
    var isMaintenanceMode: Boolean

}
