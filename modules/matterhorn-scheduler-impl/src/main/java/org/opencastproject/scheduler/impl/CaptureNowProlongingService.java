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
package org.opencastproject.scheduler.impl;

import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TEMPORAL;

import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.data.Effect0;

import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;

/** Prolong immediate recordings before reaching the end, as long as there are no conflicts */
public class CaptureNowProlongingService implements ManagedService {

  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(CaptureNowProlongingService.class);

  private static final String CFG_KEY_INITIAL_TIME = "initial-time";
  private static final String CFG_KEY_PROLONGING_TIME = "prolonging-time";

  private static final String JOB_NAME = "mh-capture-prolonging-job";
  private static final String JOB_GROUP = "mh-capture-prolonging-job-group";
  private static final String TRIGGER_GROUP = "mh-capture-prolonging-trigger-group";
  private static final String JOB_PARAM_PARENT = "parent";

  /** The initial time in millis */
  private int initialTime = -1;

  /** The prolonging time in millis */
  private int prolongingTime = -1;

  /** The quartz scheduler */
  private org.quartz.Scheduler quartz;

  /** The scheduler service */
  private SchedulerService schedulerService;

  /** The security service */
  private SecurityService securityService;

  /** The service registry */
  private ServiceRegistry serviceRegistry;

  /** The organization directory service */
  private OrganizationDirectoryService orgDirectoryService;

  /** The bundle context for this osgi component */
  private ComponentContext componentContext;

  /** Sets the scheduler service */
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  /** Sets the security service */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** Sets the service registry */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /** Sets the organization directory service */
  public void setOrgDirectoryService(OrganizationDirectoryService orgDirectoryService) {
    this.orgDirectoryService = orgDirectoryService;
  }

  /**
   * Activates the component
   * 
   * @param cc
   *          the component's context
   */
  public void activate(ComponentContext cc) {
    componentContext = cc;
    try {
      quartz = new StdSchedulerFactory().getScheduler();
      quartz.start();
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(JOB_NAME, JOB_GROUP, Runner.class);
      job.setDurability(true);
      job.setVolatility(true);
      job.getJobDataMap().put(JOB_PARAM_PARENT, this);
      quartz.addJob(job, true);
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Deactivates the component
   */
  public void deactivate(ComponentContext cc) {
    componentContext = null;
    shutdown();
  }

  @Override
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    initialTime = OsgiUtil.getCfgAsInt(properties, CFG_KEY_INITIAL_TIME);
    if (initialTime <= 90) {
      initialTime = 90000;
    } else {
      initialTime *= 1000; // Configuration holds initial time in seconds, value must be in milliseconds
    }

    prolongingTime = OsgiUtil.getCfgAsInt(properties, CFG_KEY_PROLONGING_TIME);
    if (prolongingTime <= 90) {
      prolongingTime = 90000;
    } else {
      prolongingTime *= 1000; // Configuration holds prolonging time in seconds, value must be in milliseconds
    }
  }

  /**
   * Set the schedule and start or restart the scheduler.
   */
  public void schedule(String agentId) throws org.quartz.SchedulerException {
    logger.debug("Capture prolonging job for agent '{}' is run every minute.", agentId);
    final Trigger trigger = TriggerUtils.makeMinutelyTrigger();
    trigger.setStartTime(DateTime.now().plusMinutes(1).toDate());
    trigger.setName(agentId);
    trigger.setGroup(TRIGGER_GROUP);
    trigger.setJobName(JOB_NAME);
    trigger.setJobGroup(JOB_GROUP);
    if (quartz.getTrigger(agentId, TRIGGER_GROUP) == null) {
      quartz.scheduleJob(trigger);
    } else {
      quartz.rescheduleJob(agentId, TRIGGER_GROUP, trigger);
    }
  }

  public void stop(String agentId) {
    try {
      quartz.unscheduleJob(agentId, TRIGGER_GROUP);
      logger.info("Stopped prolonging capture for agent '{}'", agentId);
    } catch (Exception e) {
      logger.error("Error stopping Quartz job for agent '{}': {}", agentId, e);
    }
  }

  /** Shutdown the scheduler. */
  public void shutdown() {
    try {
      quartz.shutdown();
    } catch (org.quartz.SchedulerException ignore) {
    }
  }

  // just to make sure Quartz is being shut down...
  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    shutdown();
  }

  /**
   * Returns the initial time duration (in milliseconds) of a recording started by the CaptureNow service
   * 
   * @return the initial time
   */
  public int getInitialTime() {
    return initialTime;
  }

  /**
   * Returns the time duration (in milliseconds) a recording is prolonged by the prolonging job.
   * 
   * @return the prolonging time
   */
  public int getProlongingTime() {
    return prolongingTime;
  }

  public SecurityService getSecurityService() {
    return securityService;
  }

  public ComponentContext getComponentContext() {
    return componentContext;
  }

  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public OrganizationDirectoryService getOrgDirectoryService() {
    return orgDirectoryService;
  }

  // --

  /** Quartz work horse. */
  public static class Runner implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      logger.debug("Start capture prolonging job for agent '{}'", jobExecutionContext.getTrigger().getName());
      try {
        execute((CaptureNowProlongingService) jobExecutionContext.getJobDetail().getJobDataMap().get(JOB_PARAM_PARENT),
                jobExecutionContext.getTrigger().getName());
      } catch (Exception e) {
        throw new JobExecutionException("An error occurred while prolonging captures", e);
      }
      logger.debug("Finished capture prolonging job for agent '{}'", jobExecutionContext.getTrigger().getName());
    }

    private void execute(final CaptureNowProlongingService prolongingService, final String agentId) {
      try {
        DublinCoreCatalog eventCatalog = prolongingService.getImmediateEvent(agentId);
        Date endDate = EncodingSchemeUtils.decodeMandatoryPeriod(eventCatalog.getFirst(DublinCore.PROPERTY_TEMPORAL))
                .getEnd();

        if (endDate.before(DateTime.now().plusSeconds(90).toDate())) {
          prolong(prolongingService, eventCatalog, agentId);
        } else {
          logger.debug("Wait another minute to check for prolinging the immediate recording for agent '{}'", agentId);
        }
      } catch (Exception e) {
        logger.error("Unable to get the immediate recording for agent '{}': {}", agentId, e);
      }
    }

    private void prolong(final CaptureNowProlongingService prolongingService, final DublinCoreCatalog eventCatalog,
            final String agentId) throws NotFoundException, ServiceRegistryException {
      long eventId = Integer.parseInt(eventCatalog.getFirst(PROPERTY_IDENTIFIER));
      org.opencastproject.job.api.Job job = prolongingService.getServiceRegistry().getJob(eventId);
      Organization organization = prolongingService.getOrgDirectoryService().getOrganization(job.getOrganization());

      User user = SecurityUtil.createSystemUser(prolongingService.getComponentContext(), organization);
      SecurityUtil.runAs(prolongingService.getSecurityService(), organization, user, new Effect0() {
        @Override
        protected void run() {
          try {
            prolongingService.prolongEvent(eventCatalog, agentId);
            logger.info("Prolonged immediate recording for agent '{}'", agentId);
          } catch (UnauthorizedException e) {
            logger.error(
                    "Unable to update the prolonged recording for agent '{}': You don't have the right permissions!",
                    agentId);
          } catch (NotFoundException e) {
            logger.warn(
                    "Unable to update the prolonged recording for agent '{}': No immedia capture found for updating!",
                    agentId);
          } catch (Exception e) {
            logger.error("Unable to update the prolonged recording for agent '{}': {}", agentId, e);
          }
        }
      });

    }

  }

  public DublinCoreCatalog getImmediateEvent(String agentId) throws NotFoundException, SchedulerException {
    SchedulerQuery q = new SchedulerQuery().setSpatial(agentId).setStartsTo(new Date()).setEndsFrom(new Date());
    DublinCoreCatalogList search = schedulerService.search(q);
    if (search.getCatalogList().isEmpty()) {
      logger.warn("Unable to prolong capture, no recording found for agent '{}'!", agentId);
      throw new NotFoundException("No immediate recording found for agent: " + agentId);
    }
    return search.getCatalogList().get(0);
  }

  public void prolongEvent(DublinCoreCatalog eventCatalog, String agentId) throws UnauthorizedException,
          NotFoundException, SchedulerException {
    long eventId = Integer.parseInt(eventCatalog.getFirst(PROPERTY_IDENTIFIER));

    DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(eventCatalog.getFirst(DublinCore.PROPERTY_TEMPORAL));

    Date prolongedEndDate = new DateTime(period.getEnd()).plus(getProlongingTime()).toDate();

    eventCatalog.set(PROPERTY_TEMPORAL,
            EncodingSchemeUtils.encodePeriod(new DCMIPeriod(period.getStart(), prolongedEndDate), Precision.Second));

    DublinCoreCatalogList events = schedulerService.findConflictingEvents(agentId, period.getStart(), prolongedEndDate);
    for (DublinCoreCatalog conflictCatalog : events.getCatalogList()) {
      if (eventId == Integer.parseInt(conflictCatalog.getFirst(PROPERTY_IDENTIFIER)))
        continue;

      Date conflictingStartDate = EncodingSchemeUtils.decodeMandatoryPeriod(
              conflictCatalog.getFirst(DublinCore.PROPERTY_TEMPORAL)).getStart();

      prolongedEndDate = new DateTime(conflictingStartDate).minusMinutes(1).toDate();

      eventCatalog.set(PROPERTY_TEMPORAL,
              EncodingSchemeUtils.encodePeriod(new DCMIPeriod(period.getStart(), prolongedEndDate), Precision.Second));

      logger.info(
              "An existing event is in a conflict with the one to be prolong on the agent '{}'. Prolong to a minute before the conflicting event.",
              agentId);
      stop(agentId);
      break;
    }

    schedulerService.updateEvent(eventId, eventCatalog, new HashMap<String, String>());
  }

}
