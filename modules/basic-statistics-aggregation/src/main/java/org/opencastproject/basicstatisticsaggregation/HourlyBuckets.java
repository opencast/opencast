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

/**
 * The result of correlating one item's raw events for one day: views and watched seconds per hour (0-23).
 */
public final class HourlyBuckets {

  private final int[] views;
  private final long[] watchtime;

  public HourlyBuckets(int[] views, long[] watchtime) {
    this.views = views;
    this.watchtime = watchtime;
  }

  public int[] getViews() {
    return views;
  }

  public long[] getWatchtime() {
    return watchtime;
  }
}
