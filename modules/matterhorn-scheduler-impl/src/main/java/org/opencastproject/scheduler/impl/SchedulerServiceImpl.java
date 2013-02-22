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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.opencastproject.scheduler.impl.Util.getEventIdentifier;
import static org.opencastproject.scheduler.impl.Util.setEventIdentifierImmutable;
import static org.opencastproject.scheduler.impl.Util.setEventIdentifierMutable;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.scheduler.api.SchedulerQuery.Sort;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.ServiceException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
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

/**
 * Implementation of {@link SchedulerService}.
 * 
 */
public class SchedulerServiceImpl implements SchedulerService, ManagedService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

  /** The metadata key used to store the workflow identifier in an event's metadata */
  public static final String WORKFLOW_INSTANCE_ID_KEY = "org.opencastproject.workflow.id";

  /** The metadata key used to store the workflow definition in an event's metadata */
  public static final String WORKFLOW_DEFINITION_ID_KEY = "org.opencastproject.workflow.definition";

  /** The schedule workflow operation identifier */
  public static final String SCHEDULE_OPERATION_ID = "schedule";

  /** The workflow operation property that stores the event start time, as milliseconds since 1970 */
  public static final String WORKFLOW_OPERATION_KEY_SCHEDULE_START = "schedule.start";

  /** The workflow operation property that stores the event stop time, as milliseconds since 1970 */
  public static final String WORKFLOW_OPERATION_KEY_SCHEDULE_STOP = "schedule.stop";

  /** The workflow operation property that stores the event location */
  public static final String WORKFLOW_OPERATION_KEY_SCHEDULE_LOCATION = "schedule.location";

  /** The series service */
  protected SeriesService seriesService;

  /** The ingest service */
  protected IngestService ingestService;

  /** The workflow service */
  protected WorkflowService workflowService;

  /** Persistent storage for events */
  protected SchedulerServiceDatabase persistence;

  /** Solr index for events */
  protected SchedulerServiceIndex index;

  /** Workspace */
  protected Workspace workspace;

  /**
   * Properties that are updated by ManagedService updated method
   */
  @SuppressWarnings("rawtypes")
  protected Dictionary properties;

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

  /** OSGi callback */
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
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
          logger.info("The event index is empty. Populating it now with {} events", Integer.valueOf(events.length));
          for (DublinCoreCatalog event : events) {
            index.index(event);
            final long id = getEventIdentifier(event);
            Properties properties = persistence.getEventMetadata(id);
            if (properties != null) {
              index.index(id, properties);
            }
          }
          logger.info("Finished populating event search index");
        }
      } catch (Exception e) {
        logger.warn("Unable to index event instances: {}", e.getMessage());
        throw new ServiceException(e.getMessage());
      }
    }
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
   * Updates workflow for event, which values where updated.
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
   *           if workflow is not in paused state or current operation is no longer 'schedule'
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
    WorkflowOperationInstance scheduleOperation = workflow.getCurrentOperation();

    // if the workflow is not in the hold state with 'schedule' as the current operation, we can't update the event
    if (!WorkflowInstance.WorkflowState.PAUSED.equals(workflow.getState())) {
      throw new SchedulerException("The workflow is not in the paused state, so it can not be updated");
    }
    if (!SCHEDULE_OPERATION_ID.equals(scheduleOperation.getTemplate())) {
      throw new SchedulerException("The current worflow operation is not 'schedule', so it can not be updated");
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
    scheduleOperation.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_START, Long.toString(startDate.getTime()));
    scheduleOperation.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_STOP, Long.toString(endDate.getTime()));
    scheduleOperation.setConfiguration(WORKFLOW_OPERATION_KEY_SCHEDULE_LOCATION,
            event.getFirst(DublinCore.PROPERTY_SPATIAL));
    // Set the location in the workflow as well, so that it shows up in the UI properly.
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
    final String seriesId = dc.getFirst(DublinCore.PROPERTY_IS_PART_OF);
    mp.setTitle(dc.getFirst(DublinCore.PROPERTY_TITLE));
    mp.setLanguage(dc.getFirst(DublinCore.PROPERTY_LANGUAGE));
    mp.setLicense(dc.getFirst(DublinCore.PROPERTY_LICENSE));

    if (isBlank(mp.getSeries()) && isNotBlank(seriesId)) {
      // add series dc to mp
      // add the episode catalog
      DublinCoreCatalog seriesCatalog = seriesService.getSeries(seriesId);
      ingestService.addCatalog(IOUtils.toInputStream(seriesCatalog.toXmlString(), "UTF-8"), "dublincore.xml",
              MediaPackageElements.SERIES, mp);
    } else if (isNotBlank(mp.getSeries()) && !mp.getSeries().equals(seriesId)) {
      // switch to new series dc and remove old
      for (Catalog c : mp.getCatalogs(MediaPackageElements.SERIES)) {
        logger.debug("Deleting existing series dublin core {} from media package {}", c.getIdentifier(), mp
                .getIdentifier().toString());
        mp.remove(c);
      }
      DublinCoreCatalog seriesCatalog = seriesService.getSeries(seriesId);
      ingestService.addCatalog(IOUtils.toInputStream(seriesCatalog.toXmlString(), "UTF-8"), "dublincore.xml",
              MediaPackageElements.SERIES, mp);
    } else if (isNotBlank(mp.getSeries()) && isBlank(seriesId)) {
      // remove series dc
      for (Catalog c : mp.getCatalogs(MediaPackageElements.SERIES)) {
        logger.debug("Deleting existing series dublin core {} from media package {}", c.getIdentifier(), mp
                .getIdentifier().toString());
        mp.remove(c);
      }
    }

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

    // set series id
    mp.setSeries(seriesId);

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
    ingestService.addCatalog(IOUtils.toInputStream(dc.toXmlString(), "UTF-8"), "dublincore.xml",
            MediaPackageElements.EPISODE, mp);
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

  /*
   * (non-Javadoc)
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @Override
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    this.properties = properties;
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
    Date startDate = period.getStart();
    Date endDate = period.getEnd();

    final long eventId;
    try {
      eventId = startWorkflowInstance(eventCatalog, startDate, endDate, wfProperties).getId();
    } catch (WorkflowException e) {
      logger.error("Could not start workflow: {}", e.getMessage());
      throw new SchedulerException(e);
    } catch (MediaPackageException e) {
      logger.error("Could not create media package: {}", e.getMessage());
      throw new SchedulerException(e);
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
      DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(event.getFirst(DublinCore.PROPERTY_TEMPORAL));
      if (!period.hasEnd() || !period.hasStart()) {
        throw new IllegalArgumentException(
                "Dublin core field dc:temporal does not contain information about start and end of event");
      }
      Date startDate = period.getStart();
      Date endDate = period.getEnd();

      WorkflowInstance instance;
      try {
        instance = startWorkflowInstance(event, startDate, endDate, wfProperties);
      } catch (Exception e) {
        logger.error("Failed to start workflow for event: {}", e.getMessage());
        if (!eventsIDs.isEmpty()) {
          logger.info("Stoping workflows that were already started...");
          for (long id : eventsIDs) {
            try {
              stopWorkflowInstance(id);
            } catch (NotFoundException e1) {
              // should not happen
              throw new IllegalStateException(e1);
            }
          }
        }
        throw new SchedulerException(e);
      }
      setEventIdentifierMutable(instance.getId(), event);
      eventsIDs.add(instance.getId());
    }

    try {
      persistence.storeEvents(eventList.toArray(new DublinCoreCatalog[eventList.size()]));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not persist events: {}", e.getMessage());
      if (!eventsIDs.isEmpty()) {
        logger.info("Stopping workflows associated with events...");
        for (long id : eventsIDs) {
          try {
            stopWorkflowInstance(id);
          } catch (NotFoundException e1) {
            // should not happen
          }
        }
      }
    }

    // index events

    for (DublinCoreCatalog event : eventList) {
      try {
        index.index(event);
      } catch (Exception e) {
        logger.warn("Unable to index event {}: {}", getEventIdentifier(event), e.getMessage());
        throw new SchedulerException(e);
      }
    }

    // update with CA properties with defaults
    for (DublinCoreCatalog event : eventList) {
      try {
        updateCaptureAgentMetadata(new Properties(), tuple(getEventIdentifier(event), event));
      } catch (NotFoundException e) {
        // should not happen
        throw new IllegalStateException(e);
      }
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
      persistence.updateEvent(event);
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
              incrementedVals.add(new DublinCoreValue(v.getValue().concat(" " + String.valueOf(i + 1)),
                      v.getLanguage(), v.getEncodingScheme()));
            }
            cat.set(prop, incrementedVals);
          } else {
            cat.set(prop, vals);
          }
        }
      }
      updateEvent(getEventIdentifier(cat), cat, new HashMap<String, String>());
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

    if (!template.hasValue(DublinCoreCatalogImpl.PROPERTY_RECURRENCE)) {
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
    if (template.hasValue(DublinCoreCatalogImpl.PROPERTY_AGENT_TIMEZONE)) {
      tz = TimeZone.getTimeZone(template.getFirst(DublinCoreCatalogImpl.PROPERTY_AGENT_TIMEZONE));
    } else { // No timezone was present, assume the serve's local timezone.
      tz = TimeZone.getDefault();
    }

    Recur recur = new RRule(template.getFirst(DublinCoreCatalogImpl.PROPERTY_RECURRENCE)).getRecur();
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
          d.setTime(d.getTime() + tz.getDSTSavings()); // Ajust for Fall back one hour
        }
      } else { // Event doesn't start in DST
        if (tz.inDaylightTime(d)) {
          d.setTime(d.getTime() - tz.getDSTSavings()); // Adjust for Spring forward one hour
        }
      }
      DublinCoreCatalog event = (DublinCoreCatalog) ((DublinCoreCatalogImpl) template).clone();
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
      stopWorkflowInstance(eventId);
      persistence.deleteEvent(eventId);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not remove event '{}' from persistent storage: {}", eventId, e);
      throw new SchedulerException(e);
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
          d.setTime(d.getTime() + tz.getDSTSavings()); // Ajust for Fall back one hour
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

    List<DublinCoreCatalog> eventList;
    try {
      eventList = index.search(filter).getCatalogList();
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to retrieve events for capture agent '{}'", filter);
      throw new SchedulerException(e);
    }

    CalendarGenerator cal = new CalendarGenerator(seriesService);
    for (DublinCoreCatalog event : eventList) {
      final long id = getEventIdentifier(event);
      Properties prop;
      try {
        prop = getEventCaptureAgentConfiguration(id);
      } catch (NotFoundException e) {
        // should not happen
        throw new IllegalStateException(e);
      }
      cal.addEvent(event, prop);
    }

    // Only validate calendars with events. Without any events, the icalendar won't validate
    if (cal.getCalendar().getComponents().size() > 0) {
      try {
        cal.getCalendar().validate();
      } catch (ValidationException e1) {
        logger.warn("Could not validate Calendar: {}", e1.getMessage());
      }
    }

    return cal.getCalendar().toString(); // CalendarOutputter performance sucks (jmh)
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.api.SchedulerService#getScheduleLastModified(java.lang.String)
   */
  @Override
  public Date getScheduleLastModified(SchedulerQuery filter) throws SchedulerException {
    try {
      Date lastModified = index.getLastModifiedDate(filter);
      return lastModified != null ? lastModified : new Date();
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to retrieve last modified for CA {}: {}", filter, e.getMessage());
      throw new SchedulerException(e);
    }
  }
}
