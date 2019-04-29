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

package org.opencastproject.statistics.provider.influx.provider;

import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.statistics.api.ResourceType;
import org.opencastproject.statistics.api.StatisticsProvider;
import org.opencastproject.statistics.provider.influx.StatisticsProviderInfluxService;
import org.opencastproject.util.data.Tuple;

import com.google.common.collect.Ordering;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public abstract class InfluxStatisticsProvider implements StatisticsProvider {


  protected StatisticsProviderInfluxService service;
  private String id;
  private ResourceType resourceType;
  private Set<DataResolution> dataResolutions;
  private String title;
  private String description;


  public InfluxStatisticsProvider(
      StatisticsProviderInfluxService service,
      String id,
      ResourceType resourceType,
      Set<DataResolution> dataResolutions,
      String title,
      String description
  ) {
    this.service = service;
    this.id = id;
    this.resourceType = resourceType;
    this.dataResolutions = dataResolutions;
    this.title = title;
    this.description = description;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public ResourceType getResourceType() {
    return resourceType;
  }

  public Set<DataResolution> getDataResolutions() {
    return dataResolutions;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public String getDescription() {
    return description;
  }

  protected static String dataResolutionToInfluxGrouping(DataResolution dataResolution) {
    switch (dataResolution) {
      case DAILY:
        return " GROUP BY time(1d)";
      case WEEKLY:
        return " GROUP BY time(1w, -3d)";  // -3d because otherwise, influx starts weeks on Thursdays
      case MONTHLY:
        return ""; // not available in influx -> we have to do multiple queries with different periods
      case YEARLY:
        return ""; // not available in influx -> we have to do multiple queries with different periods
      default:
        throw new IllegalArgumentException("unmapped DataResolution: " + dataResolution.name());
    }
  }

  protected static List<Tuple<Instant, Instant>> getPeriods(final Instant from, final Instant to, DataResolution resolution, ZoneId zoneId) {
    switch (resolution) {
      case MONTHLY:
        return getMonthPeriods(getMonths(from, to, zoneId), from, to);
      case YEARLY:
        return getYearPeriods(getYears(from, to, zoneId), from, to);
      case DAILY: // Will be handled by influx grouping. No need to divide into periods.
      case WEEKLY: // Will be handled by influx grouping. No need to divide into periods.
      default:
        return Collections.singletonList(new Tuple<>(from, to));
    }
  }

  private static List<Tuple<Instant, Instant>> getMonthPeriods(List<YearMonth> months, Instant from, Instant to) {
    final List<Tuple<Instant, Instant>> result = new ArrayList<>();
    for (YearMonth month : months) {
      final Instant start = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
      final Instant end = month.atEndOfMonth().atTime(23, 59, 59, 1_000_000_000 - 1).toInstant(ZoneOffset.UTC);
      result.add(new Tuple<>(Ordering.natural().max(start, from), Ordering.natural().min(end, to)));
    }
    return result;
  }

  private static List<Tuple<Instant, Instant>> getYearPeriods(List<Year> years, Instant from, Instant to) {
    final List<Tuple<Instant, Instant>> result = new ArrayList<>();
    for (Year year : years) {
      final Instant start = year.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
      final Instant end = year.atMonthDay(MonthDay.of(12, 31)).atTime(23, 59, 59, 1_000_000_000 - 1).toInstant(ZoneOffset.UTC);
      result.add(new Tuple<>(Ordering.natural().max(start, from), Ordering.natural().min(end, to)));
    }
    return result;
  }

  private static List<YearMonth> getMonths(final Instant from, final Instant to, ZoneId zoneId) {
    LocalDateTime localStart = LocalDateTime.ofInstant(from, zoneId);
    final LocalDateTime localEnd = LocalDateTime.ofInstant(to, zoneId);
    final List<YearMonth> months = new ArrayList<>();
    while (!localStart.isAfter(localEnd)) {
      months.add(YearMonth.from(localStart));
      localStart = localStart.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
    return months;
  }

  private static List<Year> getYears(final Instant from, final Instant to, ZoneId zoneId) {
    LocalDateTime localStart = LocalDateTime.ofInstant(from, zoneId);
    final LocalDateTime localEnd = LocalDateTime.ofInstant(to, zoneId);
    final List<Year> years = new ArrayList<>();
    while (!localStart.isAfter(localEnd)) {
      years.add(Year.from(localStart));
      localStart = localStart.plusYears(1).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
    return years;
  }

  @Override
  public int hashCode() {
    return this.getId().hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof InfluxStatisticsProvider)) {
      return false;
    }
    final StatisticsProvider other = (StatisticsProvider) o;
    return this.getId().equals(other.getId());
  }
}
