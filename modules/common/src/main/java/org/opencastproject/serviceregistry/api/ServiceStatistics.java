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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.serviceregistry.api;

/**
 * Provides statistics for a service registration
 */
public interface ServiceStatistics {
  /** The service for which these statistics apply **/
  ServiceRegistration getServiceRegistration();

  /** The number of milliseconds a job takes, on average, to run **/
  long getMeanRunTime();

  /** The number of milliseconds a job sits in a queue, on average **/
  long getMeanQueueTime();

  /** The number of jobs that this service has successfully finished**/
  int getFinishedJobs();

  /** The number of job that this service is currently running **/
  int getRunningJobs();

  /** The number of job that are currently waiting to be run by this service **/
  int getQueuedJobs();
}
