/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.capture.impl.jobs;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;

public class JobDetailTriggerPair {
  private JobDetail job;
  private CronTrigger trigger;

  /**
   * Basic data class used to aggregate JobDetails and CronTriggers together so that a complete job's requirements can
   * be created by a third party and scheduled easily without calling two functions.
   * 
   * @param job
   *          The JobDetail to use.
   * @param trigger
   *          The CronTrigger to use.
   **/
  public JobDetailTriggerPair(JobDetail job, CronTrigger trigger) {
    this.job = job;
    this.trigger = trigger;
  }

  public JobDetail getJob() {
    return job;
  }

  public void setJob(JobDetail job) {
    this.job = job;
  }

  public CronTrigger getTrigger() {
    return trigger;
  }

  public void setTrigger(CronTrigger trigger) {
    this.trigger = trigger;
  }
}
