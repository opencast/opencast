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

package org.opencastproject.scheduler.impl;

import static org.opencastproject.util.data.Option.some;

import org.opencastproject.kernel.scanner.AbstractBufferScanner;
import org.opencastproject.kernel.scanner.AbstractScanner;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NeedleEye;

import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    immediate = true,
    service = ManagedService.class,
    property = {
        "service.description=Cleanup Finished Recordings from the Schedule Scanner"
    }
)
public class OldScheduledScanner extends AbstractBufferScanner implements ManagedService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(OldScheduledScanner.class);
  public static final String SCANNER_NAME = "Cleanup Finished Scheduled Events Scanner";
  public static final String JOB_GROUP = "mh-scheduler-cleanup-job-group";
  public static final String JOB_NAME = "mh-scheduler-cleanup-job";
  public static final String TRIGGER_GROUP = "mh-scheduler-cleanup-trigger";
  public static final String TRIGGER_NAME = "mh-scheduler-cleanup-trigger";

  /** Reference to the scheduler service. */
  private SchedulerService service;

  public OldScheduledScanner() {
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

  @Activate
  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
  }

  @Deactivate
  @Override
  public void deactivate() {
    super.deactivate();
  }

  /**
   * Method to set the service this REST endpoint uses
   *
   * @param service
   */
  @Reference(name = "SchedulerService")
  public void setService(SchedulerService service) {
    this.service = service;
  }

  @Reference(name = "ServiceRegistry")
  @Override
  public void bindServiceRegistry(ServiceRegistry serviceRegistry) {
    super.bindServiceRegistry(serviceRegistry);
  }

  @Reference(name = "OrganizationDirectoryService")
  @Override
  public void bindOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    super.bindOrganizationDirectoryService(organizationDirectoryService);
  }

  @Reference(name = "SecurityService")
  @Override
  public void bindSecurityService(SecurityService securityService) {
    super.bindSecurityService(securityService);
  }

  @Override
  public void setQuartz(Scheduler quartz) {
    this.quartz = quartz;
  }

  /**
   * Method to unset the service this REST endpoint uses
   *
   * @param service
   */
  public void unsetService(SchedulerService service) {
    this.service = null;
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
  public String getScannerName() {
    return SCANNER_NAME;
  }

  @Override
  public void scan() {
    try {
      service.removeScheduledRecordingsBeforeBuffer(getBuffer());
    } catch (UnauthorizedException e) {
      logger.error("Unable to scan for finished recordings, not authorized: ", e);
    } catch (SchedulerException e) {
      logger.error("Unable to scan for finished recordings: ", e);
    }
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
        parameters.getAdminContextFor(org.getId()).runInContext(parameters::scan);
      }

      logger.info("Finished " + parameters.getScannerName() + " job.");
    }
  }
}
