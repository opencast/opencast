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

package org.opencastproject.assetmanager.impl;

import static org.opencastproject.util.data.Option.some;

import org.opencastproject.kernel.scanner.AbstractScanner;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.Log;
import org.opencastproject.util.NeedleEye;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.quartz.CronExpression;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

public class TimedMediaArchiver extends AbstractScanner implements ManagedService {
  private static final Log logger = new Log(LoggerFactory.getLogger(TimedMediaArchiver.class));

  public static final String PARAM_KEY_STORE_ID = "store-id";
  public static final String PARAM_KEY_MAX_AGE = "max-age";
  public static final String JOB_GROUP = "oc-asset-manager-timed-media-archiver-group";
  public static final String JOB_NAME = "oc-asset-manager-timed-media-archive-job";
  public static final String SCANNER_NAME = "Timed media archive offloader";
  public static final String TRIGGER_GROUP = "oc-asset-manager-timed-media-archiver-trigger-group";
  public static final String TRIGGER_NAME = "oc-asset-manager-timed-media-archiver-trigger";

  private TieredStorageAssetManager assetManager;
  private WorkflowService workflowService;
  private String storeId;
  private long ageModifier;

  public TimedMediaArchiver() {
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

  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    String cronExpression;
    boolean enabled;

    unschedule();

    if (properties != null) {
      logger.debug("Updating configuration...");

      enabled = BooleanUtils.toBoolean((String) properties.get(PARAM_KEY_ENABLED));
      setEnabled(enabled);
      logger.info("Timed media offload enabled: " + enabled);
      if (!isEnabled()) {
        return;
      }

      cronExpression = (String) properties.get(PARAM_KEY_CRON_EXPR);
      if (StringUtils.isBlank(cronExpression) || !CronExpression.isValidExpression(cronExpression)) {
        throw new ConfigurationException(PARAM_KEY_CRON_EXPR, "Cron expression must be valid");
      }
      setCronExpression(cronExpression);
      logger.debug("Timed media offload cron expression: '" + cronExpression + "'");

      storeId = (String) properties.get(PARAM_KEY_STORE_ID);
      if (StringUtils.isBlank(storeId)) {
        throw new ConfigurationException(PARAM_KEY_STORE_ID, "Store type is missing");
      }
      logger.debug("Remote media store type: " + storeId);

      try {
        ageModifier = Long.parseLong((String) properties.get(PARAM_KEY_MAX_AGE));
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_MAX_AGE, "Invalid max age");
      }
      if (ageModifier < 0) {
        throw new ConfigurationException(PARAM_KEY_MAX_AGE, "Max age must be greater than zero");
      }
    }

    schedule();
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
    Date maxAge = Calendar.getInstance().getTime();
    maxAge.setTime(maxAge.getTime() - TimeUnit.HOURS.toMillis(ageModifier));
    if (assetManager.getAssetStore(storeId).isNone()) {
      throw new RuntimeException("Store " + storeId + " is not available to the asset manager");
    }

    try {
      // Hardcoded date of zero.  Assumption: there is nothing with a date older than 0 which needs to be auto moved.
      assetManager.moveSnapshotsByDate(new Date(0), maxAge, storeId);
    } catch (NotFoundException e) {
      throw new RuntimeException("Unable to offload asset manager data", e);
    }
  }

  @Override
  public String getScannerName() {
    return SCANNER_NAME;
  }

  public void setAssetManager(TieredStorageAssetManager am) {
    this.assetManager = am;
  }

  /** Quartz job to which offloads old mediapackages from the asset manager to remote storage */
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
        parameters.getAdminContextFor(org.getId()).runInContext(parameters::scan);
      }

      logger.debug("Finished " + parameters.getScannerName() + " job.");
    }
  }
}
