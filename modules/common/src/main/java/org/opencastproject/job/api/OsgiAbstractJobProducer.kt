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

package org.opencastproject.job.api

import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry

/**
 * Refined implementation of [org.opencastproject.job.api.AbstractJobProducer] suitable for use in an
 * OSGi environment.
 *
 * OSGi dependency injection methods are provided to reduce the amount of boilerplate code needed per
 * service implementation.
 */
abstract class OsgiAbstractJobProducer protected constructor(jobType: String) : AbstractJobProducer(jobType) {
    override var serviceRegistry: ServiceRegistry? = null
    override var securityService: SecurityService? = null
    override var userDirectoryService: UserDirectoryService? = null
    override var organizationDirectoryService: OrganizationDirectoryService? = null
}
