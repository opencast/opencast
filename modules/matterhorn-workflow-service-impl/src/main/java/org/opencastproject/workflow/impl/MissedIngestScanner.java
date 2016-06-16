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

package org.opencastproject.workflow.impl;

import static org.opencastproject.util.data.Option.some;

import org.opencastproject.kernel.scanner.AbstractScanner;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.Log;
import org.opencastproject.util.NeedleEye;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.workflow.api.WorkflowDatabaseException;

import org.osgi.service.cm.ManagedService;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.LoggerFactory;

public class MissedIngestScanner extends AbstractWorkflowBufferScanner implements ManagedService {
  private static final Log logger = new Log(LoggerFactory.getLogger(MissedIngestScanner.class));

  public static final String JOB_GROUP = "mh-workflow-find-missed-ingests-job-group";
  public static final String JOB_NAME = "mh-workflow-find-missed-ingests-job";
  public static final String SCANNER_NAME = "Missed Ingest Scanner";
  public static final String TRIGGER_GROUP = "mh-workflow-find-missed-ingests-trigger-group";
  public static final String TRIGGER_NAME = "mh-workflow-find-missed-ingests-trigger";

  public MissedIngestScanner() {
    try {
      quartz = new StdSchedulerFactory().getScheduler();
      quartz.start();
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(getJobName(), getJobGroup(), Runner.class);
      job.setDurability(false);
      job.setVolatility(true);
      job.getJobDataMap().put(JOB_PARAM_PARENT, this);
      quartz.addJob(job, true);
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getJobGroup() {
    return JOB_GROUP;
  }

  @Override
  public String getJobName() {
    return JOB_NAME;
  }

  @Override
  public String getTriggerGroupName() {
    return TRIGGER_GROUP;
  }

  @Override
  public String getTriggerName() {
    return TRIGGER_NAME;
  }

  @Override
  public void scan() {
    try {
      getWorkflowService().moveMissingIngestsFromUpcomingToFailedStatus(getBuffer());
    } catch (WorkflowDatabaseException e) {
      logger.error("Unable to scan for ingests: {}", e);
    }
  }

  @Override
  public String getScannerName() {
    return SCANNER_NAME;
  }

  /** Quartz job to which cleans up the workflow instances */
  public static class Runner extends TypedQuartzJob<AbstractScanner> {
    private static final NeedleEye eye = new NeedleEye();

    public Runner() {
      super(some(eye));
    }

    @Override
    protected void execute(final AbstractScanner parameters, JobExecutionContext ctx) {
      logger.debug("Starting %s job.", parameters.getScannerName());

      // iterate all organizations
      for (final Organization org : parameters.getOrganizationDirectoryService().getOrganizations()) {
        // set the organization on the current thread
        parameters.getAdminContextFor(org.getId()).runInContext(new Effect0() {
          @Override
          protected void run() {
            parameters.scan();
          }
        });
      }

      logger.info("Finished %s job.", parameters.getScannerName());
    }
  }
}
