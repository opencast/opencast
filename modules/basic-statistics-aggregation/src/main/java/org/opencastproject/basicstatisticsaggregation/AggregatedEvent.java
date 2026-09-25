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

import org.opencastproject.basicstatistics.ItemType;
import org.opencastproject.basicstatistics.persistence.ItemTypeConverter;
import org.opencastproject.basicstatisticsaggregation.persistence.IntArrayConverter;
import org.opencastproject.basicstatisticsaggregation.persistence.LongArrayConverter;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A single day's worth of hourly view/watchtime buckets for one item. Only days with at least one view are stored.
 *
 * {@link #day} and the hour buckets in {@link #views}/{@link #watchtime} are all in UTC (see
 * {@code ViewCorrelator.hourOf}). A caller that wants stats for a non-UTC calendar day will generally need to read
 * two adjacent rows and splice the hour arrays by the relevant offset; this isn't exact for non-whole-hour offsets
 * or across DST transitions.
 */
@Entity(name = "AggregatedEvent")
@Table(name = "oc_basic_statistics_aggregation", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "organization", "item_type", "item_id", "stat_day" })
})
@NamedQueries({
    @NamedQuery(
        name = "AggregatedEvent.findByDay",
        query = "SELECT e FROM AggregatedEvent e WHERE e.organization = :organization AND e.itemType = :itemType "
            + "AND e.itemId = :itemId AND e.day = :day"
    ),
    @NamedQuery(
        name = "AggregatedEvent.findByRange",
        query = "SELECT e FROM AggregatedEvent e WHERE e.organization = :organization AND e.itemType = :itemType "
            + "AND e.itemId = :itemId AND e.day >= :from AND e.day <= :to ORDER BY e.day ASC"
    ),
})
public class AggregatedEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  private long id;

  @Column(name = "organization", nullable = false, length = 128)
  private String organization;

  @Convert(converter = ItemTypeConverter.class)
  @Column(name = "item_type", nullable = false)
  private ItemType itemType;

  @Column(name = "item_id", nullable = false)
  private String itemId;

  /** the calendar day these hourly buckets cover.
   * Called "stat_day" instead of "day" — because DAY(...) function is reserved in H2. */
  @Column(name = "stat_day", nullable = false)
  private LocalDate day;

  /** views[h] = number of views that started in hour h (0-23) of {@link #day} */
  @Convert(converter = IntArrayConverter.class)
  @Column(name = "views", nullable = false, columnDefinition = "TEXT")
  private int[] views = new int[24];

  /** watchtime[h] = seconds watched in hour h (0-23) of {@link #day} */
  @Convert(converter = LongArrayConverter.class)
  @Column(name = "watchtime", nullable = false, columnDefinition = "TEXT")
  private long[] watchtime = new long[24];

  public AggregatedEvent() {
  }

  public AggregatedEvent(String organization, ItemType itemType, String itemId, LocalDate day) {
    this.organization = organization;
    this.itemType = itemType;
    this.itemId = itemId;
    this.day = day;
  }

  public long getId() {
    return id;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public ItemType getItemType() {
    return itemType;
  }

  public void setItemType(ItemType itemType) {
    this.itemType = itemType;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public LocalDate getDay() {
    return day;
  }

  public void setDay(LocalDate day) {
    this.day = day;
  }

  public int[] getViews() {
    return views;
  }

  public void setViews(int[] views) {
    this.views = views;
  }

  public long[] getWatchtime() {
    return watchtime;
  }

  public void setWatchtime(long[] watchtime) {
    this.watchtime = watchtime;
  }
}
