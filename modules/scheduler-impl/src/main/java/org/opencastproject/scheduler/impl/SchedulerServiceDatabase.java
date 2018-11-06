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

import org.opencastproject.util.NotFoundException;

import java.util.Date;
import java.util.Map;

/**
 * Permanent storage for events. Does not support searching.
 */
public interface SchedulerServiceDatabase {

  /**
   * Touches the most recent entry by updating its last modification date.
   *
   * @param agentId
   *          the capture agent identifier
   * @throws SchedulerServiceDatabaseException
   *           if updating of the last modified value fails
   */
  void touchLastEntry(String agentId) throws SchedulerServiceDatabaseException;

  /**
   * Get the last modification date by an agent identifier
   *
   * @param agentId
   *          the capture agent identifier
   * @return the last modification date
   * @throws NotFoundException
   *           if the agent could not be found
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  Date getLastModified(String agentId) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Get a {@link Map} of last modification dates of all existing capture agents.
   *
   * @return the last modified map
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  Map<String, Date> getLastModifiedDates() throws SchedulerServiceDatabaseException;


  /**
   * Removes the extended event from persistent storage.
   *
   * @param mediapackageId
   *          ID of event to be removed
   * @throws NotFoundException
   *           if there is no element with specified ID
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void deleteEvent(String mediapackageId) throws NotFoundException, SchedulerServiceDatabaseException;

}
