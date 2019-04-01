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

class HostRegistrationInMemory(override var baseUrl: String?, override var ipAddress: String?, override var maxLoad: Float, override var cores: Int, override var memory: Long) : HostRegistration {

    override var isOnline: Boolean = false

    override var isActive: Boolean = false

    override var isMaintenanceMode: Boolean = false

    init {
        this.isOnline = true
        this.isActive = true
        this.isMaintenanceMode = false
    }

}
