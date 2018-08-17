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

package org.opencastproject.workflow.impl.jmx;

/**
 * JMX Bean interface exposing workflow statistics.
 */
public interface WorkflowsStatisticsMXBean {

  /**
   * Gets the total number of workflows
   *
   * @return the number of workflows
   */
  int getTotal();

  /**
   * Gets the number of running workflows
   *
   * @return the number of running workflows
   */
  int getRunning();

  /**
   * Gets the number of workflows on hold
   *
   * @return the number of workflows on hold
   */
  int getOnHold();

  /**
   * Gets the number of finished workflows
   *
   * @return the number of finished workflows
   */
  int getFinished();

  /**
   * Gets the number of failed workflows
   *
   * @return the number of failed workflows
   */
  int getFailed();

  /**
   * Gets the number of instantiated workflows
   *
   * @return the number of instantiated workflows
   */
  int getInstantiated();

  /**
   * Gets the number of stopped workflows
   *
   * @return the number of stopped workflows
   */
  int getStopped();

  /**
   * Gets the number of failing workflows
   *
   * @return the number of failing workflows
   */
  int getFailing();

  /**
   * Gets a list of workflows on hold
   *
   * @return an array including a list of workflows on hold
   */
  String[] getWorkflowsOnHold();

  /**
   * Gets a list of average workflow processing times
   *
   * @return an array including a list of average workflow processing times
   */
  String[] getAverageWorkflowProcessingTime();

  /**
   * Gets a list of average workflow queue times
   *
   * @return an array including a list of average workflow queue times
   */
  String[] getAverageWorkflowQueueTime();

  /**
   * Gets a list of average workflow hold times
   *
   * @return an array including a list of average workflow hold times
   */
  String[] getAverageWorkflowHoldTime();

}
