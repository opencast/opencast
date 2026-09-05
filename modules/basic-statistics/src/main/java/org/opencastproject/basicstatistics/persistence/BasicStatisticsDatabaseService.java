/*
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
package org.opencastproject.basicstatistics.persistence;

import org.opencastproject.basicstatistics.RawEvent;
import org.opencastproject.util.requests.SortCriterion;

import java.time.Instant;
import java.util.List;

/**
 * API that defines persistent storage of basic statistics
 */
public interface BasicStatisticsDatabaseService {

  /**
   * Get several raw events based on their order in the database
   * @param limit Maximum amount of raw events to return
   * @param offset The index of the first result to return
   * @return a list of {@link RawEvent}s
   * @throws BasicStatisticsDatabaseException if there is a problem communicating with the underlying data store
   */
  List<RawEvent> getRawEvents(int limit, int offset, SortCriterion sortCriterion)
          throws BasicStatisticsDatabaseException;

  /**
   * Creates a list of new raw events
   * @param events The new raw events
   * @throws BasicStatisticsDatabaseException
   */
  void createRawEvents(List<RawEvent> events)
          throws BasicStatisticsDatabaseException;

  /**
   * Record that {@code StatisticsVersion.CURRENT_VERSION} is now active, unless the most recently recorded
   * version already matches it.
   */
  void ensureVersionRecorded() throws BasicStatisticsDatabaseException;

  /**
   * @return when statistics version 1 first became active, or {@code null} if it has never been recorded
   */
  Instant getVersion1Timestamp() throws BasicStatisticsDatabaseException;
}
