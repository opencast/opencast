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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.opencastproject.scheduler.impl.Util.getEventIdentifier;
import static org.opencastproject.scheduler.impl.Util.setEventIdentifierImmutable;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.index.IndexProducer;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.index.AbstractIndexProducer;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;
import org.opencastproject.message.broker.api.index.IndexRecreateObject.Service;
import org.opencastproject.message.broker.api.scheduler.SchedulerItem;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.scheduler.api.SchedulerQuery.Sort;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.Log;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.SolrUtils;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.functions.Strings;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;
import com.google.common.cache.Cache;

import com.google.common.cache.CacheBuilder;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgi.framework.ServiceException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link SchedulerService}.
 *
 */
public class SchedulerServiceImpl extends AbstractIndexProducer implements SchedulerService, ManagedService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

  /** The metadata key used to store the workflow identifier in an event's metadata */
  public static final String WORKFLOW_INSTANCE_ID_KEY = "org.opencastproject.workflow.id";

  /** The metadata key used to store the workflow definition in an event's metadata */
  public static final String WORKFLOW_DEFINITION_ID_KEY = "org.opencastproject.workflow.definition";

  /** The workflow configuration prefix */
  public static final String WORKFLOW_CONFIG_PREFIX = "org.opencastproject.workflow.config.";

  /** The schedule workflow operation identifier */
  public static final String CAPTURE_OPERATION_ID = "capture";

  /** The schedule workflow operation identifier */
  public static final String SCHEDULE_OPERATION_ID = "schedule";

  /** The workflow operation property that stores the event start time, as milliseconds since 1970 */
  public static final String WORKFLOW_OPERATION_KEY_SCHEDULE_START = "schedule.start";

  /** The workflow operation property that stores the event stop time, as milliseconds since 1970 */
  public static final String WORKFLOW_OPERATION_KEY_SCHEDULE_STOP = "schedule.stop";

  /** The workflow operation property that stores the event location */
  public static final String WORKFLOW_OPERATION_KEY_SCHEDULE_LOCATION = "schedule.location";

  /** The immediate workflow creation configuration key */
  public static final String IMMEDIATE_WORKFLOW_CREATION = "immediate.workflow.creation";

  /** The last modifed cache configuration key */
  private static final String CFG_KEY_LAST_MODIFED_CACHE_EXPIRE = "last_modified_cache_expire";

  /** The default cache expire time in seconds */
  private static final int DEFAULT_CACHE_EXPIRE = 60;

  /** The Etag for an empty calendar */
  private static final String EMPTY_CALENDAR_ETAG = "mod0";

  private ComponentContext cc;

  /** The last modified cache */
  protected Cache<String, String> lastModifiedCache = CacheBuilder.newBuilder()
          .expireAfterWrite(DEFAULT_CACHE_EXPIRE, TimeUnit.SECONDS).build();

  /** Whether to immediate create and start a workflow for the event */
  protected boolean immediateWorkflowCreation = true;

  /** The message broker sender service */
  protected MessageSender messageSender;

  /** The message broker receiver service */
  protected MessageReceiver messageReceiver;

  /** The security service used to run the security context with. */
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The security service used to run the security context with. */
  protected SecurityService securityService;

  /** The series service */
  protected SeriesService seriesService;

  /** The workflow service */
  protected WorkflowService workflowService;

  /** The capture agent state service */
  protected CaptureAgentStateService agentService;

  /** Persistent storage for events */
  protected SchedulerServiceDatabase persistence;

  /** Solr index for events */
  protected SchedulerServiceIndex index;

  /** Workspace */
  protected Workspace workspace;

  /** The dublin core catalog service */
  protected DublinCoreCatalogService dcService;

  /** Preprocessing workflow definition for scheduler */
  protected WorkflowDefinition preprocessingWorkflowDefinition;

  /**
   * OSGi callback for setting Series Service.
   *
   * @param seriesService
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi callback */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * OSGi callback for setting Workflow Service.
   *
   * @param workflowService
   */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /**
   * OSGi callback for setting Capture Agent State Service.
   *
   * @param agentService
   */
  public void setCaptureAgentStateService(CaptureAgentStateService agentService) {
    this.agentService = agentService;
  }

  /**
   * Method to unset the capture agent state service this REST endpoint uses
   *
   * @param agentService
   */
  public void unsetCaptureAgentStateService(CaptureAgentStateService agentService) {
    this.agentService = null;
  }

  /**
   * OSGi callback to set Persistence Service.
   *
   * @param persistence
   */
  public void setPersistence(SchedulerServiceDatabase persistence) {
    this.persistence = persistence;
  }

  /**
   * OSGi callback to set indexer.
   *
   * @param index
   */
  public void setIndex(SchedulerServiceIndex index) {
    this.index = index;
  }

  /**
   * OSGi callback to set message sender.
   *
   * @param messageSender
   */
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  /**
   * OSGi callback to set message receiver.
   *
   * @param messageReceiver
   */
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /**
   * OSGi callback to set organization directory service.
   *
   * @param organizationDirectoryService
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /**
   * OSGi callback to set security service.
   *
   * @param securityService
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set the dublin core catalog service.
   *
   * @param index
   */
  public void setDublinCoreCatalogService(DublinCoreCatalogService dcService) {
    this.dcService = dcService;
  }

  /**
   * Activates Scheduler Service. Checks whether we are using synchronous or asynchronous indexing. If asynchronous is
   * used, Executor service is set. If index is empty, persistent storage is queried if it contains any series. If that
   * is the case, events are retrieved and indexed.
   *
   * @param cc
   *          ComponentContext
   * @throws Exception
   */
  public void activate(ComponentContext cc) throws Exception {
    logger.info("Activating Scheduler Service");

    long instancesInSolr = 0L;
    try {
      instancesInSolr = this.index.count();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    if (instancesInSolr == 0L) {
      try {
        DublinCoreCatalog[] events = persistence.getAllEvents();
        if (events.length != 0) {
          logger.info("The event index is empty. Populating it with {} events", Integer.valueOf(events.length));

          for (DublinCoreCatalog event : events) {
            final long id = getEventIdentifier(event);
            Properties properties = persistence.getEventMetadata(id);
            logger.debug("Adding recording event '{}' to the scheduler search index", id);
            index.index(event, properties);
            index.indexOptOut(id, persistence.isOptOut(id));
            index.indexBlacklisted(id, persistence.isBlacklisted(id));
          }

          logger.info("Finished populating event search index");
        }
      } catch (Exception e) {
        logger.warn("Unable to index event instances: {}", e.getMessage());
        throw new ServiceException(e.getMessage());
      }
    }
    this.cc = cc;
    super.activate();
  }

  @Override
  public void deactivate() {
    super.deactivate();
  }

  /**
   * Returns WorkflowDefinition for executing when event is created.
   *
   * @return {@link WorkflowDefinition}
   * @throws IllegalStateException
   *           if definition cannot be loaded
   */
  protected WorkflowDefinition getPreprocessingWorkflowDefinition() throws IllegalStateException {
    if (preprocessingWorkflowDefinition == null) {
      InputStream in = null;
      try {
        in = SchedulerServiceImpl.class.getResourceAsStream("/scheduler-workflow-definition.xml");
        preprocessingWorkflowDefinition = WorkflowParser.parseWorkflowDefinition(in);
      } catch (Exception e) {
        throw new IllegalStateException("Unable to load the preprocessing workflow definition", e);
      } finally {
        IOUtils.closeQuietly(in);
      }
    }
    return preprocessingWorkflowDefinition;
  }

  /**
   * Starts workflow for new event. Creates {@link MediaPackage} and populates it with values from
   * {@link DublinCoreCatalog}.
   *
   * @param event
   *          {@link DublinCoreCatalog} associated with event
   * @param startDate
   *          start date of event
   * @param endDate
   *          end date of event
   * @param wfProperties
   *          the workflow configuration properties
   * @return {@link WorkflowInstance} of started worflow
   * @throws WorkflowException
   *           if exception occurred while starting worflow
   * @throws MediaPackageException
   *           if media package cannot be created
   */
  protected WorkflowInstance startWorkflowInstance(DublinCoreCatalog event, Date startDate, Date endDate,
          Map<String, String> wfProperties) throws WorkflowException, MediaPackageException {
    // Build a mediapackage using the event metadata
    MediaPackage mediapackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    try {
      populateMediapackageWithStandardDCFields(mediapackage, event);
    } catch (Exception e) {
      throw new MediaPackageException(e);
    }

    mediapackage.setDate(startDate);
    mediapackage.setDuration(endDate.getTime() - startDate.getTime());

    // Build a properties set for this event
    wfProperties.put(WORKFLOW_OPERATION_KEY_SCHEDULE_START, Long.toString(startDate.getTime()));
    wfProperties.put(WORKFLOW_OPERATION_KEY_SCHEDULE_STOP, Long.toString(endDate.getTime()));
    wfProperties.put(WORKFLOW_OPERATION_KEY_SCHEDULE_LOCATION, event.getFirst(DublinCore.PROPERTY_SPATIAL));

    // Start the workflow
    return workflowService.start(getPreprocessingWorkflowDefinition(), mediapackage, wfProperties);
  }

  /**
   * Updates workflow for the given event.
   * <p>
   * This method will only allow updates to workflows that are either in the "schedule" operation or are in instantiated
   * or paused state.
   *
   * @param event
   *          {@link DublinCoreCatalog} of updated event
   * @param startDate
   *          start date of event
   * @param endDate
   *          end date of event
   * @param wfProperties
   *          the workflow configuration properties
   * @throws SchedulerException
   *           if workflow is not in paused or instantiated state and current operation is not 'schedule'
   * @throws NotFoundException
   *           if workflow with ID from DublinCore cannot be found
   * @throws WorkflowException
   *           if update fails
   * @throws UnauthorizedException
   *           if the current user is not authorized to update the workflow
   */
  protected void updateWorkflow(DublinCoreCatalog event, Date startDate, Date endDate, Map<String, String> wfProperties)
          throws SchedulerException, NotFoundException, WorkflowException, UnauthorizedException {
    WorkflowInstance workflow = workflowService.getWorkflowById(getEventIdentifier(event));
    WorkflowOperationInstance operation = workflow.getCurrentOperation();
    String operationId = operation.getTemplate();

    // if the workflow is not in a hold state or in any of 'schedule' or 'capture' as the current operation, we can't
    // update the event
    if (!SCHEDULE_OPERATION_ID.equals(operationId) && !CAPTURE_OPERATION_ID.equals(operationId)) {
      boolean isPaused = WorkflowInstance.WorkflowState.PAUSED.equals(workflow.getState());
      boolean isInstantiated = WorkflowInstance.WorkflowState.INSTANTIATED.equals(workflow.getState());
      if (!isPaused && !isInstantiated)
        throw new SchedulerException("Workflow " + workflow + " is not in the paused state, so it can not be updated");
    }

    MediaPackage mediapackage = workflow.getMediaPackage();

    // removes old values
    for (String creator : mediapackage.getCreators()) {
      mediapackage.removeCreator(creator);
    }
    for (String contributor : mediapackage.getContributors()) {
      mediapackage.removeContributor(contributor);
    }
    for (String subject : mediapackage.getSubjects()) {
      mediapackage.removeSubject(subject);
    }

    // set new values
    try {
      populateMediapackageWithStandardDCFields(mediapackage, event);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException(e);
    }

    mediapackage.setDate(startDate);
    mediapackage.setDuration(endDate.getTime() - startDate.getTime());

    // Update the properties
    operation.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_START, Long.toString(startDate.getTime()));
    operation.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_STOP, Long.toString(endDate.getTime()));
    operation.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_LOCATION, event.getFirst(DublinCore.PROPERTY_SPATIAL));
    // Set the location in the workflow as well, so that it shows up in the UI properly.
    // Update the same workflow global parameters created in the schedule create method
    workflow.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_START, Long.toString(startDate.getTime()));
    workflow.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_STOP, Long.toString(endDate.getTime()));
    workflow.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_LOCATION, event.getFirst(DublinCore.PROPERTY_SPATIAL));

    for (Entry<String, String> property : wfProperties.entrySet()) {
      workflow.setConfiguration(property.getKey(), property.getValue());
    }

    // update the workflow
    workflowService.update(workflow);
  }

  /**
   * Populates MediaPackage with standard values from DublinCore such as: title, language, license, series id, creators,
   * contributors and subjects.
   *
   * @param mp
   *          {@link MediaPackage} to be updated
   * @param dc
   *          {@link DublinCoreCatalog} for event
   */
  private void populateMediapackageWithStandardDCFields(MediaPackage mp, DublinCoreCatalog dc) throws Exception {
    String seriesId = dc.getFirst(DublinCore.PROPERTY_IS_PART_OF);
    mp.setTitle(dc.getFirst(DublinCore.PROPERTY_TITLE));
    mp.setLanguage(dc.getFirst(DublinCore.PROPERTY_LANGUAGE));
    mp.setLicense(dc.getFirst(DublinCore.PROPERTY_LICENSE));

    if (isBlank(mp.getSeries()) && isNotBlank(seriesId)) {
      // add series dc to mp
      // add the episode catalog
      DublinCoreCatalog seriesCatalog = seriesService.getSeries(seriesId);
      addCatalog(IOUtils.toInputStream(seriesCatalog.toXmlString(), "UTF-8"), "dublincore.xml",
              MediaPackageElements.SERIES, mp);
    } else if (isNotBlank(mp.getSeries()) && !mp.getSeries().equals(seriesId)) {
      // switch to new series dc and remove old
      for (Catalog c : mp.getCatalogs(MediaPackageElements.SERIES)) {
        logger.debug("Deleting existing series dublin core {} from media package {}", c.getIdentifier(), mp
                .getIdentifier().toString());
        mp.remove(c);
        workspace.delete(c.getURI());
      }
      seriesId = mp.getSeries();
      DublinCoreCatalog seriesCatalog = seriesService.getSeries(seriesId);
      addCatalog(IOUtils.toInputStream(seriesCatalog.toXmlString(), "UTF-8"), "dublincore.xml",
              MediaPackageElements.SERIES, mp);
    } else if (isNotBlank(mp.getSeries()) && isBlank(seriesId)) {
      // remove series dc
      for (Catalog c : mp.getCatalogs(MediaPackageElements.SERIES)) {
        logger.debug("Deleting existing series dublin core {} from media package {}", c.getIdentifier(), mp
                .getIdentifier().toString());
        mp.remove(c);
        workspace.delete(c.getURI());
      }
    }

    // set series id
    mp.setSeries(seriesId);

    // set series title
    if (isNotBlank(seriesId)) {
      try {
        DublinCoreCatalog sdc = seriesService.getSeries(seriesId);
        mp.setSeriesTitle(sdc.getFirst(DublinCore.PROPERTY_TITLE));
      } catch (Exception e) {
        logger.error("Unable to find series: " + seriesId, e);
        throw e;
      }
    } else {
      mp.setSeriesTitle(null);
    }

    for (DublinCoreValue value : dc.get(DublinCore.PROPERTY_CREATOR)) {
      mp.addCreator(value.getValue());
    }
    for (DublinCoreValue value : dc.get(DublinCore.PROPERTY_CONTRIBUTOR)) {
      mp.addContributor(value.getValue());
    }
    for (DublinCoreValue value : dc.get(DublinCore.PROPERTY_SUBJECT)) {
      mp.addSubject(value.getValue());
    }
    // remove existing episodes
    for (Catalog c : mp.getCatalogs(MediaPackageElements.EPISODE)) {
      logger.debug("Deleting existing episode dublin core {} from media package {}", c.getIdentifier(), mp
              .getIdentifier().toString());
      mp.remove(c);
      workspace.delete(c.getURI());
    }
    // add the episode catalog
    addCatalog(IOUtils.toInputStream(dc.toXmlString(), "UTF-8"), "dublincore.xml", MediaPackageElements.EPISODE, mp);
  }

  /**
   * Adds a catalog to the working file repository.
   *
   * @param in
   *          the catalog's input stream
   * @param fileName
   *          the catalog file name
   * @param flavor
   *          the catalog's flavor
   * @param mediaPackage
   *          the parent mediapackage
   * @return the modified mediapackage
   * @throws IOException
   *           if the catalog cannot be stored in the working file repository
   */
  private MediaPackage addCatalog(InputStream in, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws IOException {
    try {
      String elementId = UUID.randomUUID().toString();
      URI catalogUrl = workspace.put(mediaPackage.getIdentifier().compact(), elementId, fileName, in);
      logger.info("Adding catalog with flavor {} to mediapackage {}", flavor, mediaPackage);
      MediaPackageElement mpe = mediaPackage.add(catalogUrl, MediaPackageElement.Type.Catalog, flavor);
      mpe.setIdentifier(elementId);
      return mediaPackage;
    } catch (IOException e) {
      throw e;
    }
  }

  /**
   * Stops workflow with specified ID.
   *
   * @param eventID
   *          workflow to be stopped
   * @throws NotFoundException
   *           if there is no workflow with specified ID
   * @throws UnauthorizedException
   *           if the current user is not authorized to stop the workflow
   */
  protected void stopWorkflowInstance(long eventID) throws NotFoundException, UnauthorizedException {
    try {
      WorkflowInstance instance = workflowService.stop(eventID);
      for (MediaPackageElement elem : instance.getMediaPackage().getElements()) {
        workspace.delete(elem.getURI());
      }
    } catch (WorkflowException e) {
      logger.warn("Can not stop workflow {}: {}", eventID, e.getMessage());
    } catch (IOException e) {
      logger.warn("Unable to delete mediapackage element {}", e.getMessage());
    }
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties != null) {
      for (String immediateWorkflowCreationString : OsgiUtil.getOptCfg(properties, IMMEDIATE_WORKFLOW_CREATION)) {
        Boolean immediateBoolean = BooleanUtils.toBooleanObject(immediateWorkflowCreationString);
        if (immediateBoolean != null)
          immediateWorkflowCreation = immediateBoolean.booleanValue();
      }
      for (Integer cacheExpireDuration : OsgiUtil.getOptCfg(properties, CFG_KEY_LAST_MODIFED_CACHE_EXPIRE).bind(
              Strings.toInt)) {
        lastModifiedCache = CacheBuilder.newBuilder().expireAfterWrite(cacheExpireDuration, TimeUnit.SECONDS).build();
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.opencastproject.scheduler.api.SchedulerService#addEvent(org.opencastproject.metadata.dublincore.DublinCoreCatalog
   * , java.lang.String)
   */
  @Override
  public Long addEvent(DublinCoreCatalog eventCatalog, Map<String, String> wfProperties) throws SchedulerException,
          UnauthorizedException {
    DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(eventCatalog.getFirst(DublinCore.PROPERTY_TEMPORAL));
    if (!period.hasEnd() || !period.hasStart()) {
      throw new IllegalArgumentException(
              "Dublin core field dc:temporal does not contain information about start and end of event");
    }

    final long eventId;
    if (immediateWorkflowCreation) {
      Date startDate = period.getStart();
      Date endDate = period.getEnd();

      try {
        eventId = startWorkflowInstance(eventCatalog, startDate, endDate, wfProperties).getId();
      } catch (WorkflowException e) {
        logger.error("Could not start workflow: {}", e.getMessage());
        throw new SchedulerException(e);
      } catch (MediaPackageException e) {
        logger.error("Could not create media package: {}", e.getMessage());
        throw new SchedulerException(e);
      }
    } else {
      eventId = RandomUtils.nextLong(0, Long.MAX_VALUE);
    }

    addEventInternal(setEventIdentifierImmutable(eventId, eventCatalog));
    return eventId;
  }

  /** Add an event. The event DC _must_ have the identfier set. */
  private void addEventInternal(final DublinCoreCatalog event) throws SchedulerException, UnauthorizedException {
    // store event asociated
    final long eventId = getEventIdentifier(event);
    try {
      persistence.storeEvents(event);
      String mediaPackageId = UUID.randomUUID().toString();
      persistence.updateEventMediaPackageId(eventId, mediaPackageId);
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateCatalog(mediaPackageId, event));
    } catch (NotFoundException e) {
      // If this happens the store operation did not work
      throw new IllegalStateException(e);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not store event to persistent storage: {}", e);
      logger.info("Canceling workflow associated with event");
      try {
        stopWorkflowInstance(eventId);
      } catch (NotFoundException e1) {
        // should not happen
      }
      throw new SchedulerException(e);
    }

    try {
      index.index(event);
    } catch (Exception e) {
      logger.warn("Unable to index event with ID '{}': {}", eventId, e.getMessage());
      throw new SchedulerException(e);
    }

    // update with default CA properties
    try {
      updateCaptureAgentMetadata(new Properties(), tuple(eventId, event));
    } catch (NotFoundException e) {
      // should not happen
      throw new IllegalStateException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.scheduler.api.SchedulerService#addReccuringEvent(org.opencastproject.metadata.dublincore.
   * DublinCoreCatalog, java.lang.String, java.util.Date, java.util.Date, long)
   */
  @Override
  public Long[] addReccuringEvent(DublinCoreCatalog templateCatalog, Map<String, String> wfProperties)
          throws SchedulerException, UnauthorizedException {
    final List<DublinCoreCatalog> eventList;
    try {
      eventList = createEventCatalogsFromReccurence(templateCatalog);
    } catch (Exception e) {
      logger.error("Could not create Dublin Cores for events from template: {}", e.getMessage());
      throw new SchedulerException(e);
    }
    List<Long> eventsIDs = new ArrayList<Long>();
    for (DublinCoreCatalog event : eventList) {
      Long eventId = addEvent(event, wfProperties);
      eventsIDs.add(eventId);
    }
    return eventsIDs.toArray(new Long[eventsIDs.size()]);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.scheduler.api.SchedulerService#updateCaptureAgentMetadata(java.lang.Long[],
   * java.util.Properties)
   */
  @Override
  public void updateCaptureAgentMetadata(final Properties configuration, Tuple<Long, DublinCoreCatalog>... events)
          throws NotFoundException, SchedulerException {
    for (Tuple<Long, DublinCoreCatalog> e : events) {
      final long eventId = e.getA();
      final DublinCoreCatalog event = e.getB();
      // create clone and update with matching values from DC
      Properties properties = (Properties) configuration.clone();
      properties.put("event.title", event.getFirst(DublinCore.PROPERTY_TITLE));
      if (StringUtils.isNotBlank(event.getFirst(DublinCore.PROPERTY_IS_PART_OF))) {
        properties.put("event.series", event.getFirst(DublinCore.PROPERTY_IS_PART_OF));
      }
      if (StringUtils.isNotBlank(event.getFirst(DublinCore.PROPERTY_SPATIAL))) {
        properties.put("event.location", event.getFirst(DublinCore.PROPERTY_SPATIAL));
      }
      // store
      try {
        persistence.updateEventWithMetadata(eventId, properties);
        messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
                SchedulerItem.updateProperties(persistence.getMediaPackageId(eventId), properties));
      } catch (SchedulerServiceDatabaseException ex) {
        logger.error("Failed to update capture agent configuration for event '{}': {}", eventId, ex.getMessage());
        throw new SchedulerException(ex);
      }

      try {
        index.index(eventId, properties);
      } catch (Exception ex) {
        logger.warn("Unable to update capture agent properties for event with ID '{}': {}", eventId, ex.getMessage());
        throw new SchedulerException(ex);
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.scheduler.api.SchedulerService#updateEvent(org.opencastproject.metadata.dublincore.
   * DublinCoreCatalog)
   */
  @Override
  public void updateEvent(final long eventId, final DublinCoreCatalog eventCatalog, Map<String, String> wfProperties)
          throws NotFoundException, SchedulerException, UnauthorizedException {
    if (eventCatalog == null) {
      logger.warn("Cannot update <null> event.");
      return;
    }
    updateEvent(setEventIdentifierImmutable(eventId, eventCatalog), wfProperties);
  }

  /** Internal impl of an event update. The event DC _must_ have the identfier set. */
  private void updateEvent(final DublinCoreCatalog event, Map<String, String> wfProperties) throws NotFoundException,
          SchedulerException, UnauthorizedException {
    final DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(event.getFirst(DublinCore.PROPERTY_TEMPORAL));
    if (!period.hasEnd() || !period.hasStart()) {
      throw new IllegalArgumentException(
              "Dublin core field dc:temporal does not contain information about start and end of event");
    }
    final Date startDate = period.getStart();
    final Date endDate = period.getEnd();
    final long eventId = getEventIdentifier(event);
    try {
      verifyActive(eventId);
      persistence.updateEvent(event);
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateCatalog(persistence.getMediaPackageId(eventId), event));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not update event {} in persistent storage: {}", eventId, e.getMessage());
      throw new SchedulerException(e);
    }
    try {
      final Properties p = persistence.getEventMetadata(eventId);
      updateCaptureAgentMetadata(p, tuple(eventId, event));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not update event {} in persistent storage: {}", eventId, e.getMessage());
      throw new SchedulerException(e);
    }

    try {
      index.index(event);
    } catch (Exception e) {
      logger.warn("Unable to index event with ID '{}': {}", eventId, e.getMessage());
      throw new SchedulerException(e);
    }

    // update workflow
    try {
      updateWorkflow(event, startDate, endDate, wfProperties);
    } catch (NotFoundException e) {
      logger.info("No workflow to update: Event with ID {} does not have a workflow yet", eventId);
    } catch (WorkflowException e) {
      logger.error("Could not update workflow for event with ID '{}': {}", eventId, e.getMessage());
      throw new SchedulerException(e);
    }
  }

  /**
   * TODO: Update this function so that it uses a new service function with a single transaction so that it is not slow.
   */
  @Override
  public void updateEvents(List<Long> eventIds, final DublinCoreCatalog eventCatalog) throws NotFoundException,
          SchedulerException, UnauthorizedException {
    StringBuffer errors = new StringBuffer();
    int errorCount = 0;
    SchedulerQuery q = new SchedulerQuery();
    q.withIdInList(eventIds);
    q.withSort(Sort.EVENT_START);
    List<DublinCoreCatalog> catalogs = search(q).getCatalogList();
    for (int i = 0; i < catalogs.size(); i++) {
      DublinCoreCatalog cat = catalogs.get(i);
      for (EName prop : eventCatalog.getProperties()) {
        if (DublinCore.PROPERTY_IDENTIFIER.equals(prop)) {
          // skip
        } else if (!eventCatalog.get(prop).isEmpty()) {
          List<DublinCoreValue> vals = eventCatalog.get(prop);
          if (DublinCore.PROPERTY_TITLE.equals(prop)) {
            List<DublinCoreValue> incrementedVals = new ArrayList<DublinCoreValue>();
            for (DublinCoreValue v : vals) {
              incrementedVals.add(DublinCoreValue.mk(v.getValue().concat(" " + String.valueOf(i + 1)), v.getLanguage(),
                      v.getEncodingScheme()));
            }
            cat.set(prop, incrementedVals);
          } else {
            cat.set(prop, vals);
          }
        }
      }
      try {
        updateEvent(getEventIdentifier(cat), cat, new HashMap<String, String>());
      } catch (SchedulerException se) {
        // TODO: Instead of logging and continuing, should all updates fail if one is in the past?
        errors.append((errors.length() > 0 ? " " : "") + se.getMessage());
        errorCount++;
        logger.error("Could not update catalog for event with ID '{}': {}", getEventIdentifier(cat), se.getMessage());
      }
    }
    // After updating what is possible, throw error on found issues
    if (errors.length() > 0) {
      // Already logged above, but allow information to be thrown to client
      // TODO: convert to an SchedulerEventEndedException
      throw new SchedulerException("Could not update " + errorCount + " of " + catalogs.size() + " events: "
              + errors.toString());
    }
  }

  /**
   * Given recurrence pattern and template DublinCore, DublinCores for multiple events are generated. Each event will
   * have template's title plus sequential number. Spatial property of DublinCore is set to represent time period in
   * which event will take place.
   *
   * @param template
   *          {@link DublinCoreCatalog} used as template
   * @return list of {@link DublinCoreCatalog}s
   * @throws ParseException
   *           if recurrence pattern cannot be parsed
   */
  protected List<DublinCoreCatalog> createEventCatalogsFromReccurence(DublinCoreCatalog template)
          throws ParseException, IllegalArgumentException {

    if (!template.hasValue(DublinCores.OC_PROPERTY_RECURRENCE)) {
      throw new IllegalArgumentException("Event has no recurrence pattern.");
    }

    DCMIPeriod temporal = EncodingSchemeUtils.decodeMandatoryPeriod(template.getFirst(DublinCore.PROPERTY_TEMPORAL));
    if (!temporal.hasEnd() || !temporal.hasStart()) {
      throw new IllegalArgumentException(
              "Dublin core field dc:temporal does not contain information about start and end of event");
    }

    Date start = temporal.getStart();
    Date end = temporal.getEnd();
    Long duration = 0L;

    TimeZone tz = null; // Create timezone based on CA's reported TZ.
    if (template.hasValue(DublinCores.OC_PROPERTY_AGENT_TIMEZONE)) {
      tz = TimeZone.getTimeZone(template.getFirst(DublinCores.OC_PROPERTY_AGENT_TIMEZONE));
    } else { // No timezone was present, assume the serve's local timezone.
      tz = TimeZone.getDefault();
    }

    Recur recur = new RRule(template.getFirst(DublinCores.OC_PROPERTY_RECURRENCE)).getRecur();
    DateTime seed = new DateTime(true);
    DateTime period = new DateTime(true);
    if (tz.inDaylightTime(start) && !tz.inDaylightTime(end)) {
      seed.setTime(start.getTime() + 3600000);
      period.setTime(end.getTime());
      duration = (end.getTime() - (start.getTime() + 3600000)) % (24 * 60 * 60 * 1000);
    } else if (!tz.inDaylightTime(start) && tz.inDaylightTime(end)) {
      seed.setTime(start.getTime());
      period.setTime(end.getTime() + 3600000);
      duration = ((end.getTime() + 3600000) - start.getTime()) % (24 * 60 * 60 * 1000);
    } else {
      seed.setTime(start.getTime());
      period.setTime(end.getTime());
      duration = (end.getTime() - start.getTime()) % (24 * 60 * 60 * 1000);
    }
    DateList dates = recur.getDates(seed, period, Value.DATE_TIME);
    logger.debug("DateList: {}", dates);

    List<DublinCoreCatalog> events = new LinkedList<DublinCoreCatalog>();
    int i = 1;
    int length = Integer.toString(dates.size()).length();
    for (Object date : dates) {
      Date d = (Date) date;
      // Adjust for DST, if start of event
      if (tz.inDaylightTime(seed)) { // Event starts in DST
        if (!tz.inDaylightTime(d)) { // Date not in DST?
          d.setTime(d.getTime() + tz.getDSTSavings()); // Adjust for Fall back one hour
        }
      } else { // Event doesn't start in DST
        if (tz.inDaylightTime(d)) {
          d.setTime(d.getTime() - tz.getDSTSavings()); // Adjust for Spring forward one hour
        }
      }
      DublinCoreCatalog event = (DublinCoreCatalog) template.clone();
      int numZeros = length - Integer.toString(i).length();
      StringBuilder sb = new StringBuilder();
      for (int j = 0; j < numZeros; j++) {
        sb.append(0);
      }
      sb.append(i);
      event.set(DublinCore.PROPERTY_TITLE, template.getFirst(DublinCore.PROPERTY_TITLE) + " " + sb.toString());

      DublinCoreValue eventTime = EncodingSchemeUtils.encodePeriod(new DCMIPeriod(d, new Date(d.getTime() + duration)),
              Precision.Second);
      event.set(DublinCore.PROPERTY_TEMPORAL, eventTime);
      events.add(event);
      i++;
    }
    return events;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.scheduler.api.SchedulerService#removeEvent(long)
   */
  @Override
  public void removeEvent(final long eventId) throws SchedulerException, NotFoundException, UnauthorizedException {
    try {
      try {
        stopWorkflowInstance(eventId);
      } catch (NotFoundException e) {
        // There is no workflow to stop, continue deleting the event
      }
      String mediaPackageId = persistence.getMediaPackageId(eventId);
      persistence.deleteEvent(eventId);
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.delete(mediaPackageId));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not remove event '{}' from persistent storage: {}", eventId, e);
      throw new SchedulerException(e);
    }

    if (agentService != null) {
      try {
        agentService.removeRecording(Long.toString(eventId));
      } catch (NotFoundException e) {
        logger.info("Agent recording '{}' already removed", eventId);
      } catch (Exception e) {
        logger.warn("Unable to remove agent recording '{}': {}", eventId, ExceptionUtils.getMessage(e));
      }
    }

    try {
      index.delete(eventId);
    } catch (Exception e) {
      logger.warn("Unable to delete event '{}' from index: {}", eventId, e);
      throw new SchedulerException(e);
    }
  }

  /**
   * Removes an event but doesn't fail if the event's workflow or persistence record has already been deleted.
   *
   * @param eventId
   *          The id of the event to remove.
   */
  private void removeEventTolerantOfNotFound(final long eventId) throws SchedulerException, NotFoundException,
          UnauthorizedException {
    try {
      stopWorkflowInstance(eventId);
    } catch (NotFoundException e) {
      logger.info(
              "Skipping removing workflow instance with id {} because it wasn't found, it must have been removed already.",
              eventId);
    }

    try {
      String mediaPackageId = persistence.getMediaPackageId(eventId);
      persistence.deleteEvent(eventId);
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.delete(mediaPackageId));
    } catch (NotFoundException e) {
      logger.info(
              "Skipping removing scheduled event from persistence with id {} because it wasn't found, it must have been removed already.",
              eventId);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not remove event '{}' from persistent storage: {}", eventId, e);
      throw new SchedulerException(e);
    }

    if (agentService != null) {
      try {
        agentService.removeRecording(Long.toString(eventId));
      } catch (NotFoundException e) {
        logger.info("Agent recording '{}' already removed", eventId);
      } catch (Exception e) {
        logger.warn("Unable to remove agent recording '{}': {}", eventId, ExceptionUtils.getMessage(e));
      }
    }

    try {
      index.delete(eventId);
    } catch (Exception e) {
      logger.warn("Unable to delete event '{}' from index: {}", eventId, e);
      throw new SchedulerException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.scheduler.api.SchedulerService#getEventDublinCore(long)
   */
  @Override
  public DublinCoreCatalog getEventDublinCore(long eventId) throws NotFoundException, SchedulerException {
    try {
      return index.getDublinCore(eventId);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not retrieve Dublin Core for event with ID {}", eventId);
      throw new SchedulerException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.scheduler.api.SchedulerService#getEventCaptureAgentConfiguration(long)
   */
  @Override
  public Properties getEventCaptureAgentConfiguration(long eventId) throws NotFoundException, SchedulerException {
    try {
      return index.getCaptureAgentProperties(eventId);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not retrieve Capture Agent properties for event with ID {}", eventId);
      throw new SchedulerException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.scheduler.api.SchedulerService#search(org.opencastproject.scheduler.api.SchedulerQuery)
   */
  @Override
  public DublinCoreCatalogList search(SchedulerQuery query) throws SchedulerException {
    try {
      return index.search(query);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not execute query: {}", e.getMessage());
      throw new SchedulerException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.scheduler.api.SchedulerService#findConflictingEvents(java.lang.String, java.util.Date,
   * java.util.Date)
   */
  @Override
  public DublinCoreCatalogList findConflictingEvents(String captureDeviceID, Date startDate, Date endDate)
          throws SchedulerException {
    SchedulerQuery q = new SchedulerQuery().setSpatial(captureDeviceID).setEndsFrom(startDate).setStartsTo(endDate);
    try {
      return index.search(q);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not complete search after conflicting events for device '{}': {}", captureDeviceID,
              e.getMessage());
      throw new SchedulerException(e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.scheduler.api.SchedulerService#findConflictingEvents(java.lang.String, java.lang.String,
   * java.util.Date, java.util.Date, long)
   */
  @Override
  public DublinCoreCatalogList findConflictingEvents(String captureDeviceID, String rrule, Date startDate,
          Date endDate, long duration, String timezone) throws SchedulerException {
    RRule rule;
    try {
      rule = new RRule(rrule);
      rule.validate();
    } catch (Exception e) {
      logger.error("Could not create rule for finding conflicting events: {}", e.getMessage());
      throw new SchedulerException(e);
    }
    Recur recur = rule.getRecur();
    TimeZone tz = TimeZone.getTimeZone(timezone);
    DateTime seed = new DateTime(true);
    DateTime period = new DateTime(true);
    if (tz.inDaylightTime(startDate) && !tz.inDaylightTime(endDate)) {
      seed.setTime(startDate.getTime() + 3600000);
      period.setTime(endDate.getTime());
    } else if (!tz.inDaylightTime(startDate) && tz.inDaylightTime(endDate)) {
      seed.setTime(startDate.getTime());
      period.setTime(endDate.getTime() + 3600000);
    } else {
      seed.setTime(startDate.getTime());
      period.setTime(endDate.getTime());
    }
    DateList dates = recur.getDates(seed, period, Value.DATE_TIME);
    List<DublinCoreCatalog> events = new ArrayList<DublinCoreCatalog>();

    for (Object date : dates) {
      // Date filterStart = (Date) d;
      Date d = (Date) date;
      // Adjust for DST, if start of event
      if (tz.inDaylightTime(seed)) { // Event starts in DST
        if (!tz.inDaylightTime(d)) { // Date not in DST?
          d.setTime(d.getTime() + tz.getDSTSavings()); // Adjust for Fall back one hour
        }
      } else { // Event doesn't start in DST
        if (tz.inDaylightTime(d)) {
          d.setTime(d.getTime() - tz.getDSTSavings()); // Adjust for Spring forward one hour
        }
      }
      // TODO optimize: create only one query and execute it
      List<DublinCoreCatalog> filterEvents = findConflictingEvents(captureDeviceID, d, new Date(d.getTime() + duration))
              .getCatalogList();
      events.addAll(filterEvents);
    }

    return new DublinCoreCatalogList(events, events.size());
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.scheduler.api.SchedulerService#getCalendarForCaptureAgent(java.lang.String)
   */
  @Override
  public String getCalendar(SchedulerQuery filter) throws SchedulerException {
    List<Tuple<String, String>> eventList;
    try {
      eventList = index.calendarSearch(filter.setOptOut(false).setBlacklisted(false));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to retrieve events for capture agent '{}'", filter);
      throw new SchedulerException(e);
    }

    CalendarGenerator cal = new CalendarGenerator(seriesService);
    for (Tuple<String, String> event : eventList) {
      // If the even properties are empty, skip the event
      if (event.getB() == null) {
        logger.warn("Properties for event '{}' can't be found, event is not recorded", event.getA());
        continue;
      }

      DublinCoreCatalog catalog;
      try {
        catalog = dcService.load(new ByteArrayInputStream(event.getA().getBytes("UTF-8")));
      } catch (IOException e) {
        logger.error("Unable to parse dublinc core of event '{}': {}.", event.getA(), ExceptionUtils.getStackTrace(e));
        continue;
      }

      // Add the entry to the calendar, skip it with a warning if adding fails
      try {
        cal.addEvent(catalog, event.getA(), event.getB());
      } catch (Exception e) {
        logger.warn("Error adding event '{}' to calendar: {}. Event is not recorded", getEventIdentifier(catalog),
                ExceptionUtils.getStackTrace(e));
        continue;
      }
    }

    // Only validate calendars with events. Without any events, the iCalendar won't validate
    if (cal.getCalendar().getComponents().size() > 0) {
      try {
        cal.getCalendar().validate();
      } catch (ValidationException e) {
        logger.warn("Recording calendar '{}' could not be validated (returning it anyways): {}", filter,
                ExceptionUtils.getStackTrace(e));
      }
    }

    return cal.getCalendar().toString(); // CalendarOutputter performance sucks (jmh)
  }

  @Override
  public String getScheduleLastModified(String agentId) throws SchedulerException {
    String lastModified = lastModifiedCache.getIfPresent(agentId);
    if (lastModified != null)
      return lastModified;

    populateLastModifiedCache();

    lastModified = lastModifiedCache.getIfPresent(agentId);

    // If still null set the empty calendar ETag
    if (lastModified == null) {
      lastModified = EMPTY_CALENDAR_ETAG;
      lastModifiedCache.put(agentId, lastModified);
    }
    return lastModified;
  }

  private void populateLastModifiedCache() throws SchedulerException {
    try {
      Map<String, Date> lastModifiedDates = index.getLastModifiedDate(new SchedulerQuery());
      for (Entry<String, Date> entry : lastModifiedDates.entrySet()) {
        Date lastModifiedDate = entry.getValue() != null ? entry.getValue() : new Date();
        lastModifiedCache.put(entry.getKey(), generateLastModifiedHash(lastModifiedDate));
      }
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to retrieve last modified for CA: {}", ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  private String generateLastModifiedHash(Date lastModifiedDate) {
    return "mod" + Long.toString(lastModifiedDate.getTime());
  }

  /**
   * Determine the cutoff date to remove recordings in GMT
   *
   * @param buffer
   *          The number of seconds before now to start the cutoff date
   * @return A date that is the number of seconds in buffer before now in GMT.
   */
  public static org.joda.time.DateTime getCutoffDate(long buffer) {
    return getCutoffDate(buffer, new org.joda.time.DateTime(DateTimeZone.getDefault()));
  }

  /**
   * Determine the cutoff date to remove recordings in GMT
   *
   * @param buffer
   *          The number of seconds before now to start the cutoff date
   * @param dateTimeZone
   *          The time zone to use to get the current time.
   * @return A date that is the number of seconds in buffer before now in GMT.
   */
  public static org.joda.time.DateTime getCutoffDate(long buffer, org.joda.time.DateTime dateTime) {
    org.joda.time.DateTime dt = dateTime.toDateTime(DateTimeZone.UTC).minus(buffer * 1000);
    return dt;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.opencastproject.scheduler.api.SchedulerService#removeScheduledRecordingsBeforeBuffer(long)
   */
  @Override
  public void removeScheduledRecordingsBeforeBuffer(long buffer) throws SchedulerException {
    int recordingsRemoved = 0;
    DublinCoreCatalogList finishedEvents;
    DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    org.joda.time.DateTime end = getCutoffDate(buffer);

    org.joda.time.DateTime start = new org.joda.time.DateTime();
    start.withYear(1999);
    start.withDayOfYear(1);

    logger.info("Starting to look for scheduled recordings that have finished "
            + Log.getHumanReadableTimeString(buffer) + " ago from " + dateTimeFormatter.print(start) + " to "
            + dateTimeFormatter.print(end) + ".");
    SchedulerQuery q = new SchedulerQuery();
    long id = -1;
    try {
      q.setEndsFrom(SolrUtils.parseDate(dateTimeFormatter.print(start)));
      q.setEndsTo(SolrUtils.parseDate(dateTimeFormatter.print(end)));
    } catch (ParseException e) {
      logger.error("Unable to parse the date " + end + " as a cut off to remove finished scheduled recordings.");
      throw new SchedulerException(e);
    }

    try {
      finishedEvents = search(q);
    } catch (SchedulerException e) {
      logger.error("Unable to search for finished events: ", e);
      throw new SchedulerException(e);
    }

    logger.debug("Found {} events from search.", finishedEvents.getTotalCount());
    for (DublinCoreCatalog catalog : finishedEvents.getCatalogList()) {
      try {
        String idText = catalog.getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER);
        id = Long.parseLong(StringUtils.trimToNull(idText));
        removeEventTolerantOfNotFound(id);
        logger.debug("Sucessfully removed scheduled event with id " + id);
        recordingsRemoved++;
      } catch (NotFoundException e) {
        logger.debug("Skipping event with id {} because it is not found", id);
      } catch (Exception e) {
        logger.warn("Unable to delete event with id '" + id + "':", e);
      }
    }
    logger.info("Found " + recordingsRemoved + " to remove that ended " + Log.getHumanReadableTimeString(buffer)
            + " ago from " + dateTimeFormatter.print(start) + " to " + dateTimeFormatter.print(end) + ".");
  }

  /**
   * Verifies if existing event is found and has not already ended
   *
   * @param eventId
   * @throws SchedulerException
   *           if event has ended, or NotFoundException if event it not found
   */
  private void verifyActive(Long eventId) throws SchedulerException {
    DublinCoreCatalog dcc;
    try {
      dcc = getEventDublinCore(eventId);
      final DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(dcc.getFirst(DublinCore.PROPERTY_TEMPORAL));
      if (!period.hasEnd() || !period.hasStart()) {
        // Unlikely to get this error since catalog was already in index
        throw new IllegalArgumentException("Dublin core field dc:temporal for event ID " + eventId
                + " does not contain information about start and end of event");
      }
      final Date endDate = period.getEnd();
      // TODO: Assumption of no TimeZone adjustment because catalog temporal is local to server
      if (getCurrentDate().after(endDate)) {
        String msg = "Event ID " + eventId + " has already ended";
        logger.info(msg);
        throw new SchedulerException(msg);
      }
    } catch (NotFoundException e) {
      logger.info("Event ID {} is not found", eventId);
      throw new SchedulerException(e);
    }
  }

  /**
   * Returns current system Date. Enables date to be mocked to simulate the future and the past for testing. Reference:
   * http://neovibrant.com/2011/08/05/junit-with-new-date/
   *
   * @return current system date
   */
  public Date getCurrentDate() {
    return new Date();
  }

  @Override
  public void updateAccessControlList(long eventId, AccessControlList accessControlList) throws NotFoundException,
          SchedulerException {
    try {
      persistence.updateEventAccessControlList(eventId, accessControlList);
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateAcl(persistence.getMediaPackageId(eventId), accessControlList));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to update access control list of event with id '{}': {}", eventId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public AccessControlList getAccessControlList(long eventId) throws NotFoundException, SchedulerException {
    try {
      return persistence.getAccessControlList(eventId);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to get access control list of event '{}': {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public String getMediaPackageId(long eventId) throws NotFoundException, SchedulerException {
    try {
      return persistence.getMediaPackageId(eventId);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to get mediapackage id of event '{}': {}", eventId, ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public Long getEventId(String mediaPackageId) throws NotFoundException, SchedulerException {
    try {
      return persistence.getEventId(mediaPackageId);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to get event id from mediapackage id '{}': {}", mediaPackageId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public void updateOptOutStatus(String mediapackageId, boolean optOut) throws NotFoundException, SchedulerException {
    try {
      persistence.updateEventOptOutStatus(mediapackageId, optOut);
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateOptOut(mediapackageId, optOut));
      index.indexOptOut(persistence.getEventId(mediapackageId), optOut);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to update opt out status of event with mediapackage '{}': {}", mediapackageId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public void updateReviewStatus(String mediapackageId, ReviewStatus reviewStatus) throws NotFoundException,
          SchedulerException {
    try {
      Date now = new Date();
      persistence.updateEventReviewStatus(mediapackageId, reviewStatus, now);
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateReviewStatus(mediapackageId, reviewStatus, now));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to update review status of event with mediapackage '{}': {}", mediapackageId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public ReviewStatus getReviewStatus(String mediapackageId) throws NotFoundException, SchedulerException {
    try {
      return persistence.getReviewStatus(mediapackageId);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to get review status of event with mediapackage '{}': {}", mediapackageId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public boolean isOptOut(String mediapackageId) throws NotFoundException, SchedulerException {
    try {
      return index.isOptOut(persistence.getEventId(mediapackageId));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to get opt out status of event with mediapackage '{}': {}", mediapackageId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public void updateWorkflowConfig(String mediapackageId, Map<String, String> properties) throws NotFoundException,
          SchedulerException {
    try {
      Long eventId = persistence.getEventId(mediapackageId);
      Properties eventMetadata = persistence.getEventMetadata(eventId);
      for (Entry<String, String> entry : properties.entrySet()) {
        eventMetadata.put(WORKFLOW_CONFIG_PREFIX.concat(entry.getKey()), entry.getValue());
      }
      persistence.updateEventWithMetadata(eventId, eventMetadata);
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateProperties(mediapackageId, eventMetadata));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to add workflow configs to the event with mediapackage '{}': {}", mediapackageId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public boolean isBlacklisted(String mediapackageId) throws NotFoundException, SchedulerException {
    try {
      return index.isBlacklisted(persistence.getEventId(mediapackageId));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to get blacklist status of event with mediapackage '{}': {}", mediapackageId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public void updateBlacklistStatus(String mediapackageId, boolean blacklisted) throws NotFoundException,
          SchedulerException {
    try {
      persistence.updateEventBlacklistStatus(mediapackageId, blacklisted);
      messageSender.sendObjectMessage(SchedulerItem.SCHEDULER_QUEUE, MessageSender.DestinationType.Queue,
              SchedulerItem.updateBlacklist(mediapackageId, blacklisted));
      index.indexBlacklisted(persistence.getEventId(mediapackageId), blacklisted);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to update opt out status of event with mediapackage '{}': {}", mediapackageId,
              ExceptionUtils.getStackTrace(e));
      throw new SchedulerException(e);
    }
  }

  @Override
  public void repopulate(final String indexName) {
    final String destinationId = SchedulerItem.SCHEDULER_QUEUE_PREFIX + WordUtils.capitalize(indexName);
    Organization organization = new DefaultOrganization();
    SecurityUtil.runAs(securityService, organization, SecurityUtil.createSystemUser(cc, organization), new Effect0() {
      @Override
      protected void run() {
        int total = 0;
        int current = 1;
        try {
          total = persistence.countEvents();
          logger.info(
                  "Re-populating '{}' index with scheduled events. There are {} scheduled events to add to the index.",
                  indexName, total);
          DublinCoreCatalog[] catalogs = persistence.getAllEvents();
          for (DublinCoreCatalog event : catalogs) {
            final long id = getEventIdentifier(event);
            Properties properties = persistence.getEventMetadata(id);
            String eventId = persistence.getMediaPackageId(id);
            ReviewStatus reviewStatus = persistence.getReviewStatus(eventId);
            Date reviewDate = persistence.getReviewDate(eventId);
            AccessControlList accessControlList = persistence.getAccessControlList(id);
            messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                    SchedulerItem.updateCatalog(eventId, event));
            messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                    SchedulerItem.updateProperties(eventId, properties));
            if (accessControlList != null)
              messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                      SchedulerItem.updateAcl(eventId, accessControlList));
            messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                    SchedulerItem.updateOptOut(eventId, persistence.isOptOut(id)));
            messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                    SchedulerItem.updateBlacklist(eventId, persistence.isBlacklisted(id)));
            messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                    SchedulerItem.updateReviewStatus(eventId, reviewStatus, reviewDate));
            messageSender.sendObjectMessage(IndexProducer.RESPONSE_QUEUE, MessageSender.DestinationType.Queue,
                    IndexRecreateObject.update(indexName, IndexRecreateObject.Service.Scheduler, total, current));
            current++;
          }
        } catch (Exception e) {
          logger.warn("Unable to index scheduled instances: {}", e);
          throw new ServiceException(e.getMessage());
        }
        messageSender.sendObjectMessage(destinationId, MessageSender.DestinationType.Queue,
                IndexRecreateObject.end(indexName, IndexRecreateObject.Service.Scheduler));
      }
    });
  }

  @Override
  public MessageReceiver getMessageReceiver() {
    return messageReceiver;
  }

  @Override
  public Service getService() {
    return Service.Scheduler;
  }

  @Override
  public String getClassName() {
    return SchedulerServiceImpl.class.getName();
  }

}
