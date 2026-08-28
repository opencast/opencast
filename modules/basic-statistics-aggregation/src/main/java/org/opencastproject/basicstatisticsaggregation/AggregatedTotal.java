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

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * The all-time view/watchtime total for one item, maintained alongside the daily buckets in
 * {@link AggregatedEvent} so that retrieving the total is a single-row lookup rather than a sum over
 * every day of the item's history.
 */
@Entity(name = "AggregatedTotal")
@Table(name = "oc_basic_statistics_aggregation_total", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "organization", "item_type", "item_id" })
})
@NamedQuery(
    name = "AggregatedTotal.find",
    query = "SELECT t FROM AggregatedTotal t WHERE t.organization = :organization AND t.itemType = :itemType "
        + "AND t.itemId = :itemId"
)
public class AggregatedTotal {

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

  @Column(name = "total_views", nullable = false)
  private long totalViews;

  @Column(name = "total_watchtime", nullable = false)
  private long totalWatchtime;

  public AggregatedTotal() {
  }

  public AggregatedTotal(String organization, ItemType itemType, String itemId) {
    this.organization = organization;
    this.itemType = itemType;
    this.itemId = itemId;
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

  public long getTotalViews() {
    return totalViews;
  }

  public void setTotalViews(long totalViews) {
    this.totalViews = totalViews;
  }

  public long getTotalWatchtime() {
    return totalWatchtime;
  }

  public void setTotalWatchtime(long totalWatchtime) {
    this.totalWatchtime = totalWatchtime;
  }
}
