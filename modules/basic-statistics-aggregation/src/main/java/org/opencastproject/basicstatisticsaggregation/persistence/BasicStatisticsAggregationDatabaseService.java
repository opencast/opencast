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
package org.opencastproject.basicstatisticsaggregation.persistence;

import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatisticsaggregation.AggregatedEvent;
import org.opencastproject.basicstatisticsaggregation.AggregatedTotal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * API that defines persistent storage of aggregated basic statistics.
 */
public interface BasicStatisticsAggregationDatabaseService {

  /**
   * Get the hourly view/watchtime buckets for a single day, if any events were recorded for it.
   *
   * @return the matching {@link AggregatedEvent}, or {@code null} if no events were recorded that day
   */
  AggregatedEvent getAggregatedEvent(String organization, ItemType itemType, String itemId, LocalDate day)
          throws BasicStatisticsAggregationDatabaseException;

  /**
   * Get the hourly view/watchtime buckets for every day in the given (inclusive) range that had at least one event.
   */
  List<AggregatedEvent> getAggregatedEvents(String organization, ItemType itemType, String itemId,
      LocalDate from, LocalDate to) throws BasicStatisticsAggregationDatabaseException;

  /**
   * Create or overwrite the hourly view/watchtime buckets for one day.
   */
  void updateAggregatedEvent(AggregatedEvent event) throws BasicStatisticsAggregationDatabaseException;

  /**
   * Get the all-time view/watchtime total for an item. Returns a zeroed-out total if none has been recorded yet.
   */
  AggregatedTotal getTotal(String organization, ItemType itemType, String itemId)
          throws BasicStatisticsAggregationDatabaseException;

  /**
   * Add to the all-time view/watchtime total for an item, creating it first if it doesn't exist yet.
   */
  void addToTotal(String organization, ItemType itemType, String itemId, long viewsDelta, long watchtimeDelta)
          throws BasicStatisticsAggregationDatabaseException;

  /**
   * Get the timestamp up to which the raw event log has already been scanned for new activity.
   * Returns {@link Instant} if the job has never run before.
   */
  Instant getCursor() throws BasicStatisticsAggregationDatabaseException;

  /**
   * Advance the cursor to the given timestamp.
   */
  void setCursor(Instant scannedUntil) throws BasicStatisticsAggregationDatabaseException;
}
