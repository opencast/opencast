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

package org.opencastproject.serviceregistry.impl.jmx;

/**
 * JMX Bean interface exposing jobs statistics.
 */
public interface JobsStatisticsMXBean {

  /**
   * Gets the total number of jobs
   *
   * @return the number of jobs
   */
  int getJobCount();

  /**
   * Gets the number of running jobs
   *
   * @return the number of running jobs
   */
  int getRunningJobCount();

  /**
   * Gets the number of queued jobs
   *
   * @return the number of queued jobs
   */
  int getQueuedJobCount();

  /**
   * Gets the number of finished jobs
   *
   * @return the number of finished jobs
   */
  int getFinishedJobCount();

  /**
   * Gets the number of failed jobs
   *
   * @return the number of failed jobs
   */
  int getFailedJobCount();

  /**
   * Gets the number of jobs of this JMX node
   *
   * @return the number of jobs
   */
  int getJobCountByNode();

  /**
   * Gets the number of running jobs of this JMX node
   *
   * @return the number of running jobs
   */
  int getRunningJobCountByNode();

  /**
   * Gets the number of queued jobs of this JMX node
   *
   * @return the number of queued jobs
   */
  int getQueuedJobCountByNode();

  /**
   * Gets the number of finished jobs of this JMX node
   *
   * @return the number of finished jobs
   */
  int getFinishedJobCountByNode();

  /**
   * Gets the number of failed jobs of this JMX node
   *
   * @return the number of failed jobs
   */
  int getFailedJobCountByNode();

  /**
   * Gets a list of all jobs
   *
   * @return an array including all jobs
   */
  String[] getJobs();

  /**
   * Gets a list of all running jobs
   *
   * @return an array including all running jobs
   */
  String[] getRunningJobs();

  /**
   * Gets a list of all queued jobs
   *
   * @return an array including all queued jobs
   */
  String[] getQueuedJobs();

  /**
   * Gets a list of all finished jobs
   *
   * @return an array including all finished jobs
   */
  String[] getFinishedJobs();

  /**
   * Gets a list of all failed jobs
   *
   * @return an array including all failed jobs
   */
  String[] getFailedJobs();

  /**
   * Gets a list of all jobs of this JMX node
   *
   * @return an array including all jobs
   */
  String[] getJobsByNode();

  /**
   * Gets a list of running jobs of this JMX node
   *
   * @return an array including running jobs
   */
  String[] getRunningJobsByNode();

  /**
   * Gets a list of queued jobs of this JMX node
   *
   * @return an array including queued jobs
   */
  String[] getQueuedJobsByNode();

  /**
   * Gets a list of finished jobs of this JMX node
   *
   * @return an array including finished jobs
   */
  String[] getFinishedJobsByNode();

  /**
   * Gets a list of failed jobs of this JMX node
   *
   * @return an array including failed jobs
   */
  String[] getFailedJobsByNode();

  /**
   * Gets a list of average job run times
   *
   * @return an array including average job run times
   */
  String[] getAverageJobRunTime();

  /**
   * Gets a list of average job queue times
   *
   * @return an array including average job queue times
   */
  String[] getAverageJobQueueTime();

}
