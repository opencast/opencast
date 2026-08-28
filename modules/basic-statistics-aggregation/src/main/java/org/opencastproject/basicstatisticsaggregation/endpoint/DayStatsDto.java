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

package org.opencastproject.basicstatisticsaggregation.endpoint;

public class DayStatsDto {

  private String day;
  private int[] views;
  private long[] watchtime;

  public DayStatsDto() {
  }

  public DayStatsDto(String day, int[] views, long[] watchtime) {
    this.day = day;
    this.views = views;
    this.watchtime = watchtime;
  }

  public String getDay() {
    return day;
  }

  public void setDay(String day) {
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
