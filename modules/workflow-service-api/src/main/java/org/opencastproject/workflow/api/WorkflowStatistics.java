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

package org.opencastproject.workflow.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This class encapsualtes statistics for the workflow service.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "statistics", namespace = "http://workflow.opencastproject.org")
@XmlType(name = "statistics", namespace = "http://workflow.opencastproject.org")
public abstract class WorkflowStatistics {


  /** The total number of workflow instances in the system */
  @XmlAttribute
  protected long total = 0;

  /** The total number of instantiated (not yet running) workflow instances in the system */
  @XmlAttribute
  protected long instantiated = 0;

  /** The total number of running workflow instances in the system */
  @XmlAttribute
  protected long running = 0;

  /** The total number of paused workflow instances in the system */
  @XmlAttribute
  protected long paused = 0;

  /** The total number of stopped workflow instances in the system */
  @XmlAttribute
  protected long stopped = 0;

  /** The total number of succeeded workflow instances in the system */
  @XmlAttribute
  protected long succeeded = 0;

  /** The total number of failing workflow instances in the system */
  @XmlAttribute
  protected long failing = 0;

  /** The total number of failed workflow instances in the system */
  @XmlAttribute
  protected long failed = 0;

  /**
   * @return the total
   */
  public long getTotal() {
    return total;
  }

  /**
   * Get the amount of workflows for a specific state
   *
   * @param state
   *         the workflow state
   *
   * @return the amount of workflows with that state
   */
  public long get(WorkflowInstance.WorkflowState state) {
    switch (state) {
      case INSTANTIATED:
        return instantiated;
      case RUNNING:
        return running;
      case PAUSED:
        return paused;
      case STOPPED:
        return stopped;
      case SUCCEEDED:
        return succeeded;
      case FAILING:
        return failing;
      case FAILED:
        return failed;
      default:
        throw new IllegalArgumentException("Unknown workflow state!");
    }
  }

  /**
   * Set the amount of workflows for a specific state
   *
   * @param state
   *         the workflow state
   * @param amount
   *         the amount of workflows with that state
   */
  public void set(WorkflowInstance.WorkflowState state, long amount) {
    switch(state) {
      case INSTANTIATED:
        instantiated = amount;
        break;
      case RUNNING:
        running = amount;
        break;
      case PAUSED:
        paused = amount;
        break;
      case STOPPED:
        stopped = amount;
        break;
      case SUCCEEDED:
        succeeded = amount;
        break;
      case FAILING:
        failing = amount;
        break;
      case FAILED:
        failed = amount;
        break;
      default:
        throw new IllegalArgumentException("Unknown attribute!");
    }
    calculateTotal();
  }

  private void calculateTotal() {
    total = instantiated + running + paused + stopped + succeeded + failing + failed;
  }
}
