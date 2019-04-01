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

package org.opencastproject.rest

/**
 * Constant definitions used to define and consume Opencast REST services.
 */
interface RestConstants {
    companion object {

        /** The service property indicating the type of service. This is an arbitrary ID, not necessarily a java interface.  */
        val SERVICE_TYPE_PROPERTY = "opencast.service.type"

        /** The service property indicating the URL path that the service is attempting to claim  */
        val SERVICE_PATH_PROPERTY = "opencast.service.path"

        /** The service property indicating whether the service should be published in the service registry  */
        val SERVICE_PUBLISH_PROPERTY = "opencast.service.publish"

        /** The service property indicating that this service should be registered in the remote service registry  */
        val SERVICE_JOBPRODUCER_PROPERTY = "opencast.service.jobproducer"

        /** The ID by which this http context is known by the extended http service  */
        val HTTP_CONTEXT_ID = "opencast.httpcontext"

        /** The OSGI service filter that returns all registered services published as REST endpoints  */
        val SERVICES_FILTER = "(&(!(objectClass=javax.servlet.Servlet))(" + RestConstants.SERVICE_PATH_PROPERTY + "=*))"

        /** The bundle header used to find the static resource URL alias  */
        val HTTP_ALIAS = "Http-Alias"

        /** The bundle header used to find the static resource classpath  */
        val HTTP_CLASSPATH = "Http-Classpath"

        /** The bundle header used to find the static resource welcome file  */
        val HTTP_WELCOME = "Http-Welcome"

        /**
         * The amount of time in seconds to wait for a session to be inactive before deallocating it. Applied to all sessions
         * with the last filter in the chain.
         */
        val MAX_INACTIVE_INTERVAL = 1800
    }
}
