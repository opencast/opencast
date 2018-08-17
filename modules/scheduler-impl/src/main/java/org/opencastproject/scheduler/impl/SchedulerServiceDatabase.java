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

import org.opencastproject.scheduler.api.Blacklist;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import java.util.Date;
import java.util.List;
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
   * Get a {@link List} of active transaction identifiers.
   *
   * @return a list of active transaction identifiers
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  List<String> getTransactions() throws SchedulerServiceDatabaseException;

  /**
   * Get the transaction identifier by the source
   *
   * @param source
   *          the source
   * @return the transaction identifier
   * @throws NotFoundException
   *           if the transaction could not be found
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  String getTransactionId(String source) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Get the transaction source by the identifier
   *
   * @param id
   *          the transaction identifier
   * @return the source
   * @throws NotFoundException
   *           if the transaction could not be found
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  String getTransactionSource(String id) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Get the transaction last modified date by the transaction identifier
   *
   * @param id
   *          the transaction identifier
   * @return the transaction last modified date
   * @throws NotFoundException
   *           if the transaction could not be found
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  Date getTransactionLastModified(String id) throws NotFoundException, SchedulerServiceDatabaseException;

  /**
   * Returns whether the source has an active transaction or not
   *
   * @param source
   *          the source
   * @return whether the source has an active transaction <code>true</code> or not <code>false</code>
   */
  boolean hasTransaction(String source) throws SchedulerServiceDatabaseException;

  /**
   * Stores the given transaction
   *
   * @param id
   *          the transaction identifier
   * @param source
   *          the transaction source
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void storeTransaction(String id, String source) throws SchedulerServiceDatabaseException;

  /**
   * Delete the transaction by the given identifier
   *
   * @param id
   *          the transaction identifier
   * @throws NotFoundException
   *           if the transaction could not be found
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  void deleteTransaction(String id) throws NotFoundException, SchedulerServiceDatabaseException;

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

  // TODO
  List<Tuple<String, Boolean>> updateBlacklist(Blacklist blacklist) throws SchedulerServiceDatabaseException;

  /**
   * Returns the blacklist status of the agent with the given ID
   *
   * @param agentId
   *          the agent identifier
   * @param start
   *          the start date
   * @param end
   *          the end date
   * @return the blacklist status
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  boolean isBlacklisted(String agentId, Date start, Date end) throws SchedulerServiceDatabaseException;

  /**
   * Returns the blacklist status of the given presenters
   *
   * @param presenters
   *          the list of presenters
   * @param start
   *          the start date
   * @param end
   *          the end date
   * @return the blacklist status
   * @throws SchedulerServiceDatabaseException
   *           if exception occurred
   */
  boolean isBlacklisted(List<String> presenters, Date start, Date end) throws SchedulerServiceDatabaseException;

}
