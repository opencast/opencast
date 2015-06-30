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
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NeedleEye;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * Implementation of {@link AbstractWorkflowCleanupScheduler} to run in an OSGi environment
 */
public class WorkflowCleanupScanner extends AbstractWorkflowBufferScanner implements ManagedService {
  private static final String SCANNER_NAME = "Workflow Cleanup Scanner";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(WorkflowCleanupScanner.class);

  private static final String JOB_NAME = "mh-workflow-cleanup-job";
  private static final String JOB_GROUP = "mh-workflow-cleanup-job-group";
  private static final String TRIGGER_NAME = "mh-workflow-cleanup-trigger";
  private static final String TRIGGER_GROUP = "mh-workflow-cleanup-trigger-group";

  private static final String PARAM_KEY_BUFFER_SUCCEEDED = "buffer.succeeded";
  private static final String PARAM_KEY_BUFFER_FAILED = "buffer.failed";
  private static final String PARAM_KEY_BUFFER_STOPPED = "buffer.stopped";
  private static final String PARAM_KEY_BUFFER_PARENTLESS = "buffer.parentless";

  /** Buffer of successful jobs in days */
  protected static int bufferForSuccessfulJobs = -1;

  /** Buffer of failed jobs in days */
  protected static int bufferForFailedJobs = -1;

  /** Buffer of failed jobs in days */
  protected static int bufferForStoppedJobs = -1;

  /** Buffer of parentless jobs in days */
  protected static int bufferForParentlessJobs = -1;

  public WorkflowCleanupScanner() {
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
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    boolean enabled = false;
    String cronExpression;
    unschedule();

    if (properties != null) {
      logger.debug("Updating configuration...");

      enabled = BooleanUtils.toBoolean((String) properties.get(AbstractScanner.PARAM_KEY_ENABLED));
      setEnabled(enabled);
      logger.debug("enabled = {}", enabled);

      cronExpression = (String) properties.get(AbstractScanner.PARAM_KEY_CRON_EXPR);
      if (StringUtils.isBlank(cronExpression)) {
        throw new ConfigurationException(AbstractScanner.PARAM_KEY_CRON_EXPR, "Cron expression must be valid");
      }
      setCronExpression(cronExpression);
      logger.debug("cronExpression = {}", cronExpression);

      try {
        bufferForSuccessfulJobs = Integer.valueOf((String) properties.get(PARAM_KEY_BUFFER_SUCCEEDED));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_BUFFER_SUCCEEDED, "Buffer must be a valid integer", e);
      }
      logger.debug("bufferForSuccessfulJobs = {}", bufferForSuccessfulJobs);

      try {
        bufferForFailedJobs = Integer.valueOf((String) properties.get(PARAM_KEY_BUFFER_FAILED));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_BUFFER_FAILED, "Buffer must be a valid integer", e);
      }
      logger.debug("bufferForFailedJobs = {}", bufferForFailedJobs);

      try {
        bufferForStoppedJobs = Integer.valueOf((String) properties.get(PARAM_KEY_BUFFER_STOPPED));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_BUFFER_STOPPED, "Buffer must be a valid integer", e);
      }
      logger.debug("bufferForStoppedJobs = {}", bufferForStoppedJobs);

      try {
        bufferForParentlessJobs = Integer.valueOf((String) properties.get(PARAM_KEY_BUFFER_PARENTLESS));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_BUFFER_PARENTLESS, "Buffer must be a valid integer", e);
      }
      logger.debug("bufferForParentlessJobs = {}", bufferForParentlessJobs);
    }

    schedule();
  }

  @Override
  public void scan() {
    if (bufferForFailedJobs > 0) {
      try {
        getWorkflowService().cleanupWorkflowInstances(bufferForFailedJobs, WorkflowInstance.WorkflowState.FAILED);
      } catch (WorkflowDatabaseException e) {
        logger.error("Unable to cleanup failed jobs: {}", e);
      } catch (UnauthorizedException e) {
        logger.error("Workflow cleanup job doesn't have right to delete jobs!");
        throw new IllegalStateException(e);
      }
    }

    if (bufferForSuccessfulJobs > 0) {
      try {
        getWorkflowService().cleanupWorkflowInstances(bufferForSuccessfulJobs, WorkflowInstance.WorkflowState.SUCCEEDED);
      } catch (WorkflowDatabaseException e) {
        logger.error("Unable to cleanup successful jobs: {}", e);
      } catch (UnauthorizedException e) {
        logger.error("Workflow cleanup job doesn't have right to delete jobs!");
        throw new IllegalStateException(e);
      }
    }

    if (bufferForStoppedJobs > 0) {
      try {
        getWorkflowService().cleanupWorkflowInstances(bufferForStoppedJobs, WorkflowInstance.WorkflowState.STOPPED);
      } catch (WorkflowDatabaseException e) {
        logger.error("Unable to cleanup stopped jobs: {}", e);
      } catch (UnauthorizedException e) {
        logger.error("Workflow cleanup job doesn't have right to delete jobs!");
        throw new IllegalStateException(e);
      }
    }

    if (bufferForParentlessJobs > 0) {
      try {
        getServiceRegistry().removeParentlessJobs(bufferForParentlessJobs);
      } catch (ServiceRegistryException e) {
        logger.error("There was an error while removing parentless jobs: {}", e.getMessage());
      }
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
      logger.debug("Starting " + parameters.getScannerName() + " job.");

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

      logger.info("Finished " + parameters.getScannerName() + " job.");
    }
  }
}
