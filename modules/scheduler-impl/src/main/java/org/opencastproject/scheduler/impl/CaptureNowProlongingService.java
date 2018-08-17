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

import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TEMPORAL;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.SchedulerException;
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
import org.opencastproject.util.data.Effect0;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  /** The workspace */
  private Workspace workspace;

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

  /** Sets the workspace */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
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
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    // Read configuration for the default initial duration
    try {
      initialTime = Integer.parseInt(StringUtils.defaultIfBlank((String) properties.get(CFG_KEY_INITIAL_TIME), "300"));
    } catch (NumberFormatException e) {
      throw new ConfigurationException(CFG_KEY_INITIAL_TIME, "Not an integer", e);
    }
    initialTime = Math.max(initialTime, 90) * 1000;

    // Read configuration for the prolonging time
    try {
      prolongingTime = Integer.parseInt(
              StringUtils.defaultIfBlank((String) properties.get(CFG_KEY_PROLONGING_TIME), "300"));
    } catch (NumberFormatException e) {
      throw new ConfigurationException(CFG_KEY_PROLONGING_TIME, "Not an integer", e);
    }
    prolongingTime = Math.max(prolongingTime, 90) * 1000;
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

  public Workspace getWorkspace() {
    return workspace;
  }

  // --

  /** Quartz work horse. */
  public static class Runner implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      logger.debug("Starting ad-hoc prolonging job for agent '{}'", jobExecutionContext.getTrigger().getName());
      try {
        execute((CaptureNowProlongingService) jobExecutionContext.getJobDetail().getJobDataMap().get(JOB_PARAM_PARENT),
                jobExecutionContext.getTrigger().getName());
      } catch (Exception e) {
        throw new JobExecutionException("An error occurred while prolonging ad-hoc recordings", e);
      }
      logger.debug("Finished ad-hoc prolonging job for agent '{}'", jobExecutionContext.getTrigger().getName());
    }

    private void execute(final CaptureNowProlongingService prolongingService, final String agentId) {
      for (Organization organization : prolongingService.getOrgDirectoryService().getOrganizations()) {
        User user = SecurityUtil.createSystemUser(prolongingService.getComponentContext(), organization);
        SecurityUtil.runAs(prolongingService.getSecurityService(), organization, user, new Effect0() {
          @Override
          protected void run() {
            try {
              MediaPackage mp = prolongingService.getCurrentRecording(agentId);
              Opt<DublinCoreCatalog> dublinCore = DublinCoreUtil.loadEpisodeDublinCore(prolongingService.getWorkspace(),
                      mp);
              if (dublinCore.isSome()
                      && EncodingSchemeUtils.decodeMandatoryPeriod(dublinCore.get().getFirst(PROPERTY_TEMPORAL))
                              .getEnd().before(DateTime.now().plusSeconds(90).toDate())) {
                prolong(prolongingService, mp, dublinCore.get(), agentId);
              } else {
                logger.debug("Wait another minute before extending the ad-hoc recording for agent '{}'", agentId);
              }
            } catch (NotFoundException e) {
              logger.info("Unable to extend the ad-hoc recording for agent '{}': No ad-hoc recording found", agentId);
            } catch (Exception e) {
              logger.error("Error extending the ad-hoc recording for agent '{}': {}", agentId,
                      ExceptionUtils.getStackTrace(e));
            }
          }
        });
      }
    }

    private void prolong(final CaptureNowProlongingService prolongingService, final MediaPackage event,
            final DublinCoreCatalog dublinCore, final String agentId)
            throws NotFoundException, ServiceRegistryException {
      try {
        logger.info("Extending ad-hoc recording for agent '{}'", agentId);
        prolongingService.prolongEvent(event, dublinCore, agentId);
      } catch (UnauthorizedException e) {
        logger.error("Error extending the ad-hoc recording for agent '{}': Permission denied", agentId);
      } catch (NotFoundException e) {
        logger.warn("Error extending the ad-hoc recording for agent '{}': No ad-hoc recording found", agentId);
      } catch (Exception e) {
        logger.error("Error extending the ad-hoc recording for agent '{}': {}", agentId, e);
      }
    }

  }

  /**
   * Returns the current event for the given capture agent.
   *
   * @param agentId
   *          the capture agent
   * @return the recording
   * @throws NotFoundException
   *           if the there is no current recording
   * @throws UnauthorizedException
   *           if the event cannot be read due to a lack of access rights
   * @throws SchedulerException
   *           if accessing the scheduling database fails
   */
  public MediaPackage getCurrentRecording(String agentId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    List<MediaPackage> search = schedulerService.search(Opt.some(agentId), Opt.<Date> none(), Opt.some(new Date()),
            Opt.some(new Date()), Opt.<Date> none());
    if (search.isEmpty()) {
      logger.warn("Unable to load the current recording for agent '{}': no recording found", agentId);
      throw new NotFoundException("No current recording found for agent '" + agentId + "'");
    }
    return search.get(0);
  }

  /**
   * Extends the current recording.
   *
   * @param event
   *          the recording's media package
   * @param dublinCore
   *          the recording's dublin core catalog
   * @param agentId
   *          the agent
   * @throws UnauthorizedException
   *           if the event cannot be updated due to a lack of access rights
   * @throws NotFoundException
   *           if the event cannot be found
   * @throws SchedulerException
   *           if updating the scheduling data fails
   * @throws IOException
   *           if updating the calendar to the worksapce fails
   * @throws IllegalArgumentException
   *           if a URI cannot be created using the arguments provided
   */
  public void prolongEvent(MediaPackage event, DublinCoreCatalog dublinCore, String agentId)
          throws UnauthorizedException, NotFoundException, SchedulerException, IllegalArgumentException, IOException {
    String eventId = event.getIdentifier().compact();

    DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(dublinCore.getFirst(DublinCore.PROPERTY_TEMPORAL));

    Date prolongedEndDate = new DateTime(period.getEnd()).plus(getProlongingTime()).toDate();

    dublinCore.set(PROPERTY_TEMPORAL,
            EncodingSchemeUtils.encodePeriod(new DCMIPeriod(period.getStart(), prolongedEndDate), Precision.Second));

    List<MediaPackage> events = schedulerService.findConflictingEvents(agentId, period.getStart(), prolongedEndDate);
    for (MediaPackage conflictMediaPackage : events) {
      if (eventId.equals(conflictMediaPackage.getIdentifier().compact()))
        continue;

      Opt<DublinCoreCatalog> conflictingDc = DublinCoreUtil.loadEpisodeDublinCore(workspace, conflictMediaPackage);
      if (conflictingDc.isNone())
        continue;

      Date conflictingStartDate = EncodingSchemeUtils
              .decodeMandatoryPeriod(conflictingDc.get().getFirst(DublinCore.PROPERTY_TEMPORAL)).getStart();

      prolongedEndDate = new DateTime(conflictingStartDate).minusMinutes(1).toDate();

      dublinCore.set(PROPERTY_TEMPORAL,
              EncodingSchemeUtils.encodePeriod(new DCMIPeriod(period.getStart(), prolongedEndDate), Precision.Second));

      logger.info(
              "A scheduled event is preventing the current recording on agent '{}' to be further extended. Extending to one minute before the conflicting event",
              agentId);
      stop(agentId);
      break;
    }

    // Update the episode dublin core
    Catalog[] episodeCatalogs = event.getCatalogs(MediaPackageElements.EPISODE);
    if (episodeCatalogs.length > 0) {
      Catalog c = episodeCatalogs[0];
      String filename = FilenameUtils.getName(c.getURI().toString());
      URI uri = workspace.put(event.getIdentifier().toString(), c.getIdentifier(), filename,
              IOUtils.toInputStream(dublinCore.toXmlString(), "UTF-8"));
      c.setURI(uri);
      // setting the URI to a new source so the checksum will most like be invalid
      c.setChecksum(null);
    }

    schedulerService.updateEvent(eventId, Opt.<Date> none(), Opt.some(prolongedEndDate), Opt.<String> none(),
            Opt.<Set<String>> none(), Opt.some(event), Opt.<Map<String, String>> none(),
            Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);
  }

}
