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
 * Provides statistics for a service registration
 */
interface ServiceStatistics {
    /** The service for which these statistics apply  */
    val serviceRegistration: ServiceRegistration

    /** The number of milliseconds a job takes, on average, to run  */
    val meanRunTime: Long

    /** The number of milliseconds a job sits in a queue, on average  */
    val meanQueueTime: Long

    /** The number of jobs that this service has successfully finished */
    val finishedJobs: Int

    /** The number of job that this service is currently running  */
    val runningJobs: Int

    /** The number of job that are currently waiting to be run by this service  */
    val queuedJobs: Int
}
