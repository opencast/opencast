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
package org.opencastproject.basicstatisticsaggregation;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Marks how far the aggregation job has scanned the raw event log for new activity. There is only ever one row:
 * this is not a correctness boundary (raw events are re-scanned in full per affected item/day), just a cheap way
 * to avoid re-scanning the entire raw event log on every tick.
 */
@Entity(name = "AggregationCursor")
@Table(name = "oc_basic_statistics_aggregation_cursor")
public class AggregationCursor {

  public static final long SINGLETON_ID = 1L;

  @Id
  @Column(name = "id")
  private long id = SINGLETON_ID;

  @Column(name = "scanned_until", nullable = false)
  private Instant scannedUntil = Instant.EPOCH;

  public long getId() {
    return id;
  }

  public Instant getScannedUntil() {
    return scannedUntil;
  }

  public void setScannedUntil(Instant scannedUntil) {
    this.scannedUntil = scannedUntil;
  }
}
