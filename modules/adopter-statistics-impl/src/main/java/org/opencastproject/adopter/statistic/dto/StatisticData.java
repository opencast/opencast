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

package org.opencastproject.adopter.statistic.dto;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO that contains anonymous statistic data of an adopter.
 */
public class StatisticData {

  /** JSON parser */
  private static final Gson gson = new Gson();

  //================================================================================
  // Properties
  //================================================================================

  /**
   * A key that's unique for every adopter.
   *
   * Every adopter has his own statistic key, so when the data of different adopters will be
   * collected, we can use this as an ID and identify every statistic data entry
   * later on to update existing entries in the database.
   * We don't use the adopter key from {@link org.opencastproject.adopter.statistic.dto.GeneralData} at this point,
   * because we are not allowed to associate the statistic data with the adopter.
   */
  @SerializedName("statistic_key")
  private String statisticKey;

  /** The total number of jobs. */
  @SerializedName("job_count")
  private long jobCount;

  /** The total number of events. */
  @SerializedName("event_count")
  private long eventCount;

  /** The total number of series. */
  @SerializedName("series_count")
  private int seriesCount;

  /** The total number of users. */
  @SerializedName("user_count")
  private long userCount;

  /** The hosts of an adopter.*/
  private List<Host> hosts;


  //================================================================================
  // Methods
  //================================================================================

  /**
   * A StatisticData instance should always have a unique key.
   * @param statisticKey The unique key that identifies a statistic entry.
   */
  public StatisticData(String statisticKey) {
    this.statisticKey = statisticKey;
  }

  /**
   * Parses an instance of this class to a JSON string.
   * @return The instance as JSON string.
   */
  public String jsonify() {
    return gson.toJson(this);
  }


  //================================================================================
  // Getter and Setter
  //================================================================================

  public String getStatisticKey() {
    return statisticKey;
  }

  public void setStatisticKey(String statisticKey) {
    this.statisticKey = statisticKey;
  }

  public long getJobCount() {
    return jobCount;
  }

  public void setJobCount(long jobCount) {
    this.jobCount = jobCount;
  }

  public long getEventCount() {
    return eventCount;
  }

  public void setEventCount(long eventCount) {
    this.eventCount = eventCount;
  }

  public int getSeriesCount() {
    return seriesCount;
  }

  public void setSeriesCount(int seriesCount) {
    this.seriesCount = seriesCount;
  }

  public long getUserCount() {
    return userCount;
  }

  public void setUserCount(long userCount) {
    this.userCount = userCount;
  }

  public List<Host> getHosts() {
    return hosts;
  }

  public void setHosts(List<Host> hosts) {
    this.hosts = hosts;
  }

  public void addHost(Host host) {
    if (this.hosts == null) {
      this.hosts = new ArrayList<>();
    }
    this.hosts.add(host);
  }

}
