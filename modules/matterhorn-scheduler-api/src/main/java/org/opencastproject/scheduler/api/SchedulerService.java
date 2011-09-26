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
package org.opencastproject.scheduler.api;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import java.util.Date;
import java.util.Properties;

/**
 * Scheduler service manages events (creates new, updates already existing and removes events). It enables searches over
 * existing events, retrieving DublinCore or capture agent properties for specific event, search for conflicting events
 * and generating calendar for capture agent.
 */
public interface SchedulerService {

  /**
   * Creates new event using specified DublinCore. Default capture agent properties are created from DublinCore.
   * Following values are generated:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from dc:is_part_of)</li>
   * <li>event.location (mapped from dc:spatial)</li>
   * </ul>
   * 
   * @param eventCatalog
   *          {@link DublinCoreCatalog} used for creating event
   * @return ID of created event
   * @throws SchedulerException
   *           if creating new events failed
   * @throws UnauthorizedException
   *           if the caller is not authorized to take this action
   */
  Long addEvent(DublinCoreCatalog eventCatalog) throws SchedulerException, UnauthorizedException;

  /**
   * Creates series of events using DublinCore as template and recurrence pattern. For each event default capture agent
   * properties are created from DublinCore. Following values are generated:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from dc:is_part_of)</li>
   * <li>event.location (mapped from dc:spatial)</li>
   * </ul>
   * 
   * @param eventCatalog
   *          template {@link DublinCoreCatalog} used to create events
   * @param recPattern
   *          pattern of recurrence
   * @param beginning
   *          start date of event series
   * @param end
   *          end date of event series
   * @param duration
   *          duration of each event in milliseconds
   * @param timeZone
   *          time zone in which event will take place or null if local time zone should be used
   * @return array of events IDs that were created
   * @throws SchedulerException
   *           if events cannot be created
   * @throws UnauthorizedException
   *           if the caller is not authorized to take this action
   */
  Long[] addReccuringEvent(DublinCoreCatalog eventCatalog, String recPattern, Date beginning, Date end, long duration,
          String timeZone) throws SchedulerException, UnauthorizedException;

  /**
   * Updates existing events with capture agent metadata. Configuration will be updated from event's DublinCore:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from dc:is_part_of)</li>
   * <li>event.location (mapped from dc:spatial)</li>
   * </ul>
   * 
   * @param eventIDs
   *          array of events that should be updated
   * @param configuration
   *          Properties for capture agent
   * @throws NotFoundException
   *           there is event that does not exist
   * @throws SchedulerException
   *           if update fails
   */
  void updateCaptureAgentMetadata(Properties configuration, Long... eventIDs) throws NotFoundException,
          SchedulerException;

  /**
   * Update event's DublinCore. Capture agent metadata will also be updated:
   * <ul>
   * <li>event.title (mapped from dc:title)</li>
   * <li>event.series (mapped from dc:is_part_of)</li>
   * <li>event.location (mapped from dc:spatial)</li>
   * </ul>
   * 
   * @param eventCatalog
   *          updated {@link DublinCoreCatalog}
   * @throws NotFoundException
   *           if events with specified DublinCore ID cannot be found
   * @throws SchedulerException
   *           if update fails
   * @throws UnauthorizedException
   *           if the caller is not authorized to take this action
   */
  void updateEvent(DublinCoreCatalog eventCatalog) throws NotFoundException, SchedulerException, UnauthorizedException;

  /**
   * Removes event with specified ID.
   * 
   * @param eventID
   *          event to be removed
   * @throws SchedulerException
   *           if exception occurred
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   */
  void removeEvent(long eventID) throws SchedulerException, NotFoundException;

  /**
   * Retrieves DublinCore associated with specified event ID.
   * 
   * @param eventID
   *          ID of event for which DublinCore will be retrieved
   * @return {@link DublinCoreCatalog} for specified event
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  DublinCoreCatalog getEventDublinCore(long eventID) throws NotFoundException, SchedulerException;

  /**
   * Retrieves capture agent configuration for specified event.
   * 
   * @param eventID
   *          ID of event for which capture agent configuration should be retrieved
   * @return Properties for capture agent
   * @throws NotFoundException
   *           if event with specified ID cannot be found
   * @throws SchedulerException
   *           if exception occurred
   */
  Properties getEventCaptureAgentConfiguration(long eventID) throws NotFoundException, SchedulerException;

  /**
   * Retrieves all events matching given query object.
   * 
   * @param query
   *          {@link SchedulerQuery} representing query
   * @return {@link DublinCoreCatalogList} with results matching given query
   * @throws SchedulerException
   *           if exception occurred
   */
  DublinCoreCatalogList search(SchedulerQuery query) throws SchedulerException;

  /**
   * Returns list of all conflicting events, i.e. all events that ends after start date and begins before end date.
   * 
   * @param captureDeviceID
   *          capture device ID for which conflicting events are searched for
   * @param startDate
   *          start date of of conflicting period
   * @param endDate
   *          end date of conflicting period
   * @return list of events that are in conflict with specified period
   * @throws SchedulerException
   *           if exception occurred
   */
  DublinCoreCatalogList findConflictingEvents(String captureDeviceID, Date startDate, Date endDate)
          throws SchedulerException;

  /**
   * Returns list of all conflicting events. Conflicting periods are calculated based on recurrence rule, start date,
   * end date and duration of each conflicting period.
   * 
   * @param captureDeviceID
   *          capture device ID for which conflicting events are searched for
   * @param rrule
   *          recurrence rule
   * @param startDate
   *          beginning of period
   * @param endDate
   *          ending of period
   * @param duration
   *          duration of each period
   * @return list of all conflicting events
   * @throws SchedulerException
   *           if exception occurred
   */
  DublinCoreCatalogList findConflictingEvents(String captureDeviceID, String rrule, Date startDate, Date endDate,
          long duration) throws SchedulerException;

  /**
   * Generates calendar for specified capture agent.
   * 
   * @param filter
   *          filter on which calendar will be generated
   * @return generated calendar
   * @throws SchedulerException
   *           if exception occurred
   */
  String getCalendar(SchedulerQuery filter) throws SchedulerException;

  /**
   * Returns date of last modification of event belonging to specified capture agent.
   * 
   * @param filter
   *          filter for events to be checked
   * @return last modification date
   * @throws SchedulerException
   *           if exception occurred
   */
  Date getScheduleLastModified(SchedulerQuery filter) throws SchedulerException;
}
