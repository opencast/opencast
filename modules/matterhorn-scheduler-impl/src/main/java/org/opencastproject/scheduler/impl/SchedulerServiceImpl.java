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

import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
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
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowService;

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

import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

  /** The workflow service */
  protected WorkflowService workflowService;

  /** Persistent storage for events */
  protected SchedulerServiceDatabase persistence;

  /** Solr index for events */
  protected SchedulerServiceIndex index;

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
            String id = event.getFirst(DublinCore.PROPERTY_IDENTIFIER);
            Properties properties = persistence.getEventMetadata(Long.parseLong(id));
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
   * @return {@link WorkflowInstance} of started worflow
   * @throws WorkflowException
   *           if exception occurred while starting worflow
   * @throws MediaPackageException
   *           if media package cannot be created
   */
  protected WorkflowInstance startWorkflowInstance(DublinCoreCatalog event, Date startDate, Date endDate)
          throws WorkflowException, MediaPackageException {
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
    Map<String, String> properties = new HashMap<String, String>();
    properties.put(WORKFLOW_OPERATION_KEY_SCHEDULE_START, Long.toString(startDate.getTime()));
    properties.put(WORKFLOW_OPERATION_KEY_SCHEDULE_STOP, Long.toString(endDate.getTime()));
    properties.put(WORKFLOW_OPERATION_KEY_SCHEDULE_LOCATION, event.getFirst(DublinCore.PROPERTY_SPATIAL));

    // Start the workflow
    return workflowService.start(getPreprocessingWorkflowDefinition(), mediapackage, properties);
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
   * @throws SchedulerException
   *           if workflow is not in paused state or current operation is no longer 'schedule'
   * @throws NotFoundException
   *           if workflow with ID from DublinCore cannot be found
   * @throws WorkflowException
   *           if update fails
   * @throws UnauthorizedException
   *           if the current user is not authorized to update the workflow
   */
  protected void updateWorkflow(DublinCoreCatalog event, Date startDate, Date endDate) throws SchedulerException,
          NotFoundException, WorkflowException, UnauthorizedException {
    WorkflowInstance workflow = workflowService.getWorkflowById(Long.parseLong(event
            .getFirst(DublinCore.PROPERTY_IDENTIFIER)));
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

    // update the workflow
    workflowService.update(workflow);
  }

  /**
   * Populates MediaPackage with standard values from DublinCore such as: title, language, license, series id, creators,
   * contributors and subjects.
   * 
   * @param mediapackage
   *          {@link MediaPackage} to be updated
   * @param catalog
   *          {@link DublinCoreCatalog} for event
   */
  private void populateMediapackageWithStandardDCFields(MediaPackage mediapackage, DublinCoreCatalog catalog) throws Exception {
    mediapackage.setTitle(catalog.getFirst(DublinCore.PROPERTY_TITLE));
    mediapackage.setLanguage(catalog.getFirst(DublinCore.PROPERTY_LANGUAGE));
    mediapackage.setLicense(catalog.getFirst(DublinCore.PROPERTY_LICENSE));
    mediapackage.setSeries(catalog.getFirst(DublinCore.PROPERTY_IS_PART_OF));
    
    if (StringUtils.isNotEmpty(mediapackage.getSeries())) {
      try {
        DublinCoreCatalog s = seriesService.getSeries(catalog.getFirst(DublinCore.PROPERTY_IS_PART_OF));
        mediapackage.setSeriesTitle(s.getFirst(DublinCore.PROPERTY_TITLE));
      } catch (Exception e) {
        logger.error("Unable to find series: " + catalog.getFirst(DublinCore.PROPERTY_IS_PART_OF), e);
        throw e;
      }
    }
    
    for (DublinCoreValue value : catalog.get(DublinCore.PROPERTY_CREATOR)) {
      mediapackage.addCreator(value.getValue());
    }
    for (DublinCoreValue value : catalog.get(DublinCore.PROPERTY_CONTRIBUTOR)) {
      mediapackage.addContributor(value.getValue());
    }
    for (DublinCoreValue value : catalog.get(DublinCore.PROPERTY_SUBJECT)) {
      mediapackage.addSubject(value.getValue());
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
      workflowService.stop(eventID);
    } catch (WorkflowException e) {
      logger.warn("Can not stop workflow {}: {}", eventID, e.getMessage());
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
  public Long addEvent(final DublinCoreCatalog eventCatalog) throws SchedulerException, UnauthorizedException {
    DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(eventCatalog.getFirst(DublinCore.PROPERTY_TEMPORAL));
    if (!period.hasEnd() || !period.hasStart()) {
      throw new IllegalArgumentException(
              "Dublin core field dc:temporal does not contain information about start and end of event");
    }
    Date startDate = period.getStart();
    Date endDate = period.getEnd();

    final WorkflowInstance workflow;
    try {
      workflow = startWorkflowInstance(eventCatalog, startDate, endDate);
    } catch (WorkflowException e) {
      logger.error("Could not start workflow: {}", e.getMessage());
      throw new SchedulerException(e);
    } catch (MediaPackageException e) {
      logger.error("Could not create media package: {}", e.getMessage());
      throw new SchedulerException(e);
    }

    eventCatalog.set(DublinCore.PROPERTY_IDENTIFIER, Long.toString(workflow.getId()));

    // store event asociated
    try {
      persistence.storeEvents(eventCatalog);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not store event to persistent storage: {}", e);
      logger.info("Canceling workflow associated with event");
      try {
        stopWorkflowInstance(workflow.getId());
      } catch (NotFoundException e1) {
        // should not happen
      }
      throw new SchedulerException(e);
    }

    try {
      index.index(eventCatalog);
    } catch (Exception e) {
      logger.warn("Unable to index event with ID '{}': {}", workflow.getId(), e.getMessage());
      throw new SchedulerException(e);
    }

    // update with default CA properties
    Properties caProperties = new Properties();
    updateCAProperties(caProperties, eventCatalog);
    try {
      updateCaptureAgentMetadata(workflow.getId(), caProperties);
    } catch (NotFoundException e) {
      // should not happen
      throw new IllegalStateException(e);
    }

    return workflow.getId();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.api.SchedulerService#addReccuringEvent(org.opencastproject.metadata.dublincore.
   * DublinCoreCatalog, java.lang.String, java.util.Date, java.util.Date, long)
   */
  @Override
  public Long[] addReccuringEvent(DublinCoreCatalog templateCatalog) throws SchedulerException, UnauthorizedException {
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
        instance = startWorkflowInstance(event, startDate, endDate);
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
      event.set(DublinCore.PROPERTY_IDENTIFIER, Long.toString(instance.getId()));
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
        logger.warn("Unable to index event {}: {}", event.getFirst(DublinCore.PROPERTY_IDENTIFIER), e.getMessage());
        throw new SchedulerException(e);
      }
    }

    // update with CA properties with defaults
    for (DublinCoreCatalog event : eventList) {
      Properties caProperties = new Properties();
      updateCAProperties(caProperties, event);
      try {
        updateCaptureAgentMetadata(Long.parseLong(event.getFirst(DublinCore.PROPERTY_IDENTIFIER)), caProperties);
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
  public void updateCaptureAgentMetadata(final Properties configuration, Long... eventIDs) throws NotFoundException,
          SchedulerException {
    for (long eventID : eventIDs) {
      // create clone and update with matching values from DC
      Properties properties = (Properties) configuration.clone();
      DublinCoreCatalog catalog = getEventDublinCore(eventID);
      updateCAProperties(properties, catalog);
      // store
      updateCaptureAgentMetadata(eventID, properties);
    }
  }

  /**
   * Stores capture agent properties.
   * 
   * @param id
   *          of event for which properties should be stored
   * @param properties
   *          CA properties
   * @throws NotFoundException
   *           if event with given index does not exist
   * @throws SchedulerException
   *           if exception occurred
   */
  protected void updateCaptureAgentMetadata(long id, Properties properties) throws NotFoundException,
          SchedulerException {
    try {
      persistence.updateEventWithMetadata(id, properties);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to update capture agent configuration for event '{}': {}", id, e.getMessage());
      throw new SchedulerException(e);
    }

    try {
      index.index(Long.toString(id), properties);
    } catch (Exception e) {
      logger.warn("Unable to update capture agent properties for event with ID '{}': {}", id, e.getMessage());
      throw new SchedulerException(e);
    }
  }

  /**
   * Updates CA properties so that they match values from DC. Values that are updated this way are:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from dc:is_part_of)</li>
   * <li>event.location (mapped from dc:spatial)</li>
   * </ul>
   * 
   * @param properties
   *          properties to be updated
   * @param catalog
   *          matching {@link DublinCoreCatalog}
   */
  protected void updateCAProperties(Properties properties, DublinCoreCatalog catalog) {
    properties.put("event.title", catalog.getFirst(DublinCore.PROPERTY_TITLE));
    if (StringUtils.isNotBlank(catalog.getFirst(DublinCore.PROPERTY_IS_PART_OF))) {
      properties.put("event.series", catalog.getFirst(DublinCore.PROPERTY_IS_PART_OF));
    }
    if (StringUtils.isNotBlank(catalog.getFirst(DublinCore.PROPERTY_SPATIAL))) {
      properties.put("event.location", catalog.getFirst(DublinCore.PROPERTY_SPATIAL));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.api.SchedulerService#updateEvent(org.opencastproject.metadata.dublincore.
   * DublinCoreCatalog)
   */
  @Override
  public void updateEvent(final DublinCoreCatalog eventCatalog) throws NotFoundException, SchedulerException,
          UnauthorizedException {
    if (eventCatalog == null) {
      logger.warn("Cannot update <null> event.");
      return;
    }

    if (StringUtils.isBlank(eventCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER))) {
      logger.error("Dublin core does not contain identifier, so event update is not possible.");
      throw new SchedulerException("Missing event identifier");
    }

    final long eventID = Long.parseLong(eventCatalog.getFirst(DublinCore.PROPERTY_IDENTIFIER));

    DCMIPeriod period = EncodingSchemeUtils.decodeMandatoryPeriod(eventCatalog.getFirst(DublinCore.PROPERTY_TEMPORAL));
    if (!period.hasEnd() || !period.hasStart()) {
      throw new IllegalArgumentException(
              "Dublin core field dc:temporal does not contain information about start and end of event");
    }
    Date startDate = period.getStart();
    Date endDate = period.getEnd();

    try {
      persistence.updateEvent(eventCatalog);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not update event {} in persistent storage: {}", eventID, e.getMessage());
      throw new SchedulerException(e);
    }

    try {
      index.index(eventCatalog);
    } catch (Exception e) {
      logger.warn("Unable to index event with ID '{}': {}", eventID, e.getMessage());
      throw new SchedulerException(e);
    }

    // update workflow
    try {
      updateWorkflow(eventCatalog, startDate, endDate);
    } catch (WorkflowException e) {
      logger.error("Could not update workflow for event with ID '{}': {}", eventID, e.getMessage());
      throw new SchedulerException(e);
    }
  }
  
  /**
   * TODO: Update this function so that it uses a new service function with a single transaction so that it is not slow.
   */
  public void updateEvents(List<String> idList, final DublinCoreCatalog eventCatalog)  throws NotFoundException, SchedulerException, UnauthorizedException {
    SchedulerQuery q = new SchedulerQuery();
    q.withIdInList(idList);
    q.withSort(Sort.EVENT_START);
    List<DublinCoreCatalog> catalogs = search(q).getCatalogList();
    for (int i = 0; i < catalogs.size(); i++) {
      DublinCoreCatalog cat = catalogs.get(i);
      for (EName prop : eventCatalog.getProperties()) {
        if (!eventCatalog.get(prop).isEmpty()) {
          List<DublinCoreValue> vals = eventCatalog.get(prop);
          if (DublinCore.PROPERTY_TITLE.equals(prop)) {
            List<DublinCoreValue> incrementedVals = new ArrayList<DublinCoreValue>();
            for (DublinCoreValue v : vals) {
              incrementedVals.add(new DublinCoreValue(v.getValue().concat(" " + String.valueOf(i + 1)), v.getLanguage(), v.getEncodingScheme()));
            }
            cat.set(prop, incrementedVals);
          } else {
            cat.set(prop, vals);
          }
        }
      }
      updateEvent(cat);
    }
  }

  /**
   * Given recurrence pattern and template DublinCore, DublinCores for multiple events are generated. Each event will
   * have template's title plus sequential number. Spatial property of DublinCore is set to represent time period in
   * which event will take place.
   * 
   * @param template
   *          {@link DublinCoreCatalog} used as template
   * @param rrule
   *          recurrence pattern
   * @param start
   *          date when series of event will start
   * @param end
   *          date when series of event will end
   * @param duration
   *          duration of each even in milliseconds
   * @param timeZone
   *          time zone in which event will take place
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
    Date end   = temporal.getEnd();
    Long duration = (end.getTime() % (60 * 60 * 1000)) - (start.getTime() % (60 * 60 * 1000));
    
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
    } else if (!tz.inDaylightTime(start) && tz.inDaylightTime(end)) {
      seed.setTime(start.getTime());
      period.setTime(end.getTime() + 3600000);
    } else {
      seed.setTime(start.getTime());
      period.setTime(end.getTime());
    }
    DateList dates = recur.getDates(seed, period, Value.DATE_TIME);
    logger.debug("DateList: {}", dates);
    
    List<DublinCoreCatalog> events = new LinkedList<DublinCoreCatalog>();
    int i = 1;
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
      event.set(DublinCore.PROPERTY_TITLE, template.getFirst(DublinCore.PROPERTY_TITLE) + " " + i);
      
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
  public void removeEvent(final long eventID) throws SchedulerException, NotFoundException, UnauthorizedException {
    try {
      stopWorkflowInstance(eventID);
      persistence.deleteEvent(eventID);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not remove event '{}' from persistent storage: {}", eventID, e);
      throw new SchedulerException(e);
    }

    try {
      index.delete(Long.toString(eventID));
    } catch (Exception e) {
      logger.warn("Unable to delete event '{}' from index: {}", eventID, e);
      throw new SchedulerException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.api.SchedulerService#getEventDublinCore(long)
   */
  @Override
  public DublinCoreCatalog getEventDublinCore(long eventID) throws NotFoundException, SchedulerException {
    try {
      return index.getDublinCore(Long.toString(eventID));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not retrieve Dublin Core for event with ID {}", eventID);
      throw new SchedulerException(e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.opencastproject.scheduler.api.SchedulerService#getEventCaptureAgentConfiguration(long)
   */
  @Override
  public Properties getEventCaptureAgentConfiguration(long eventID) throws NotFoundException, SchedulerException {
    try {
      return index.getCaptureAgentProperties(Long.toString(eventID));
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Could not retrieve Capture Agent properties for event with ID {}", eventID);
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
      List<DublinCoreCatalog> resultList = index.search(query);
      DublinCoreCatalogList dcList = new DublinCoreCatalogList();
      dcList.setCatalogList(resultList);
      return dcList;
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
      List<DublinCoreCatalog> result = index.search(q);
      DublinCoreCatalogList dcList = new DublinCoreCatalogList();
      dcList.setCatalogList(result);
      return dcList;
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
          Date endDate, long duration) throws SchedulerException {
    RRule rule;
    try {
      rule = new RRule(rrule);
      rule.validate();
    } catch (Exception e) {
      logger.error("Could not create rule for finding conflicting events: {}", e.getMessage());
      throw new SchedulerException(e);
    }
    Recur recur = rule.getRecur();
    DateTime start = new DateTime(startDate.getTime());
    start.setUtc(true);
    DateTime end = new DateTime(endDate.getTime());
    end.setUtc(true);
    DateList dates = recur.getDates(start, end, Value.DATE_TIME);
    List<DublinCoreCatalog> events = new ArrayList<DublinCoreCatalog>();

    for (Object d : dates) {
      Date filterStart = (Date) d;
      // TODO optimize: create only one query and execute it
      List<DublinCoreCatalog> filterEvents = findConflictingEvents(captureDeviceID, filterStart,
              new Date(filterStart.getTime() + duration)).getCatalogList();
      events.addAll(filterEvents);
    }

    DublinCoreCatalogList dcList = new DublinCoreCatalogList();
    dcList.setCatalogList(events);
    return dcList;
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
      eventList = index.search(filter);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to retrieve events for capture agent '{}'", filter);
      throw new SchedulerException(e);
    }

    CalendarGenerator cal = new CalendarGenerator(seriesService);
    for (DublinCoreCatalog event : eventList) {
      String id = event.getFirst(DublinCore.PROPERTY_IDENTIFIER);
      Properties prop;
      try {
        prop = getEventCaptureAgentConfiguration(Long.parseLong(id));
      } catch (NotFoundException e) {
        // should not happen
        throw new IllegalStateException(e);
      }
      cal.addEvent(event, prop);
    }

    // Only validate calendars with events. Without any events, the icalendar won't validate
    if (eventList.size() > 0) {
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
      return index.getLastModifiedDate(filter);
    } catch (SchedulerServiceDatabaseException e) {
      logger.error("Failed to retrieve last modified for CA {}: {}", filter, e.getMessage());
      throw new SchedulerException(e);
    }
  }
}
