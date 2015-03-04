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

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Index for scheduled events. Supports all kinds of advanced searching over stored events.
 *
 */
public interface SchedulerServiceIndex {

  /**
   * Allocates any necessary resources and activates index.
   */
  void activate();

  /**
   * Deactivates index and performs cleanup.
   */
  void deactivate();

  /**
   * Returns number of instances currently in index.
   *
   * @return number of instances in solr index
   * @throws SchedulerServiceDatabaseException
   *           if query cannot be completed
   */
  long count() throws SchedulerServiceDatabaseException;

  /**
   * Index event catalog.
   *
   * @param dc
   *          {@link DublinCoreCatalog} describing event
   * @throws SchedulerServiceDatabaseException
   *           if indexing failed
   */
  void index(DublinCoreCatalog dc) throws SchedulerServiceDatabaseException;

  /**
   * Index event catalog.
   *
   * @param dc
   *          {@link DublinCoreCatalog} describing event
   * @param captureAgentProperties
   *          properties for capture agent
   * @throws SchedulerServiceDatabaseException
   *           if indexing failed
   */
  void index(DublinCoreCatalog dc, Properties captureAgentProperties) throws SchedulerServiceDatabaseException;

  /**
   * Index CA properties for existing event.
   *
   * @param eventId
   *          ID of event to which properties will be added
   * @param captureAgentProperties
   *          properties for capture agent
   * @throws NotFoundException
   *           if there is no event with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if indexing failed
   */
  void index(long eventId, Properties captureAgentProperties) throws NotFoundException,
          SchedulerServiceDatabaseException;

  /**
   * Index opt out status for existing event.
   *
   * @param eventId
   *          ID of event to which properties will be added
   * @param optOut
   *          the opt out status
   * @throws NotFoundException
   *           if there is no event with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if indexing failed
   */
  void indexOptOut(long eventId, boolean optOut) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Index blacklist status for existing event.
   *
   * @param eventId
   *          ID of event to which properties will be added
   * @param blacklisted
   *          the blacklist status
   * @throws NotFoundException
   *           if there is no event with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if indexing failed
   */
  void indexBlacklisted(long eventId, boolean blacklisted) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Search over indexed events. Search parameters are specified with {@link SchedulerQuery} object.
   *
   * @param query
   *          {@link SchedulerQuery} object representing query parameters
   * @return list of all matching events
   * @throws SchedulerServiceDatabaseException
   *           if query cannot be performed
   */
  DublinCoreCatalogList search(SchedulerQuery query) throws SchedulerServiceDatabaseException;

  /**
   * Search over indexed events. Search parameters are specified with {@link SchedulerQuery} object.
   *
   * @param query
   *          {@link SchedulerQuery} object representing query parameters
   * @return list of all matching events as a {@link Tuple} of the serialized dublincore and CA properties
   * @throws SchedulerServiceDatabaseException
   *           if query cannot be performed
   */
  List<Tuple<String, String>> calendarSearch(SchedulerQuery query) throws SchedulerServiceDatabaseException;

  /**
   * Removes event from index.
   *
   * @param id
   *          ID of event to be removed
   * @throws SchedulerServiceDatabaseException
   *           if removal failed
   */
  void delete(long id) throws SchedulerServiceDatabaseException;

  /**
   * Retrieves Dublin core of event with specified ID.
   *
   * @param eventId
   *          Dublin core to be retrieved
   * @return {@link DublinCoreCatalog} of event
   * @throws NotFoundException
   *           if there is no event with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  DublinCoreCatalog getDublinCore(long eventId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Retrieves capture agent properties for specified ID
   *
   * @param eventId
   *          ID of event for which properties should be retrieved
   * @return capture agent properties
   * @throws NotFoundException
   *           if there is no event with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  Properties getCaptureAgentProperties(long eventId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Returns date of last modification of event belonging to specified scheduler query.
   *
   * @param filter
   *          filter of events
   * @return the dates of last modification
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  Map<String, Date> getLastModifiedDate(SchedulerQuery filter) throws SchedulerServiceDatabaseException;

  /**
   * Returns the opt out status of an event with the given event id
   *
   * @param eventId
   *          the event id
   * @return the opt out status
   * @throws NotFoundException
   *           if there is no event with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  boolean isOptOut(long eventId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Returns the blacklist status of an event with the given event id
   *
   * @param eventId
   *          the event id
   * @return the blacklist status
   * @throws NotFoundException
   *           if there is no event with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  boolean isBlacklisted(long eventId) throws NotFoundException, SchedulerServiceDatabaseException;

}
