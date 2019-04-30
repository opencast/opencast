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

package org.opencastproject.statistics.api;

import com.google.common.collect.Ordering;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public final class StatisticsUtil {

  private StatisticsUtil() {
  }

  public static List<Instant> getBuckets(final Instant from, final Instant to, final DataResolution resolution, final ZoneId zoneId) {
    final List<Instant> result = new ArrayList<>();
    LocalDateTime localStart = LocalDateTime.ofInstant(from, zoneId);
    final LocalDateTime localEnd = LocalDateTime.ofInstant(to, zoneId);
    while (!localStart.isAfter(localEnd)) {
      switch (resolution) {
        case MONTHLY:
          final Instant currentMonthStart = YearMonth.of(localStart.getYear(), localStart.getMonth())
                  .atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
          result.add(Ordering.natural().min(to, Ordering.natural().max(from, currentMonthStart)));
          localStart = localStart.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
          break;
        case YEARLY:
          final Instant currentYearStart = YearMonth.of(localStart.getYear(), 1)
                  .atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
          result.add(Ordering.natural().min(to, Ordering.natural().max(from, currentYearStart)));
          localStart = localStart.plusYears(1).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
          break;
        case DAILY:
          final Instant currentDayStart = localStart.withHour(0).withMinute(0).withSecond(0).withNano(0)
                  .toInstant(ZoneOffset.UTC);
          result.add(Ordering.natural().min(to, Ordering.natural().max(from, currentDayStart)));
          localStart = localStart.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
          break;
        case WEEKLY:
          final int daysOff = 1 - localStart.getDayOfWeek().getValue();
          final Instant currentWeekStart = localStart.plusDays(daysOff).withHour(0).withMinute(0).withSecond(0)
                  .withNano(0).toInstant(ZoneOffset.UTC);
          result.add(currentWeekStart); // No min, max here, because influx uses previous monday as bucket label
          localStart = localStart.plusDays(7).plusDays(daysOff).withHour(0).withMinute(0).withSecond(0).withNano(0);
          break;
        case HOURLY:
          final Instant currentHourStart = ZonedDateTime.of(localStart.withMinute(0).withSecond(0).withNano(0), zoneId)
              .toInstant();
          result.add(Ordering.natural().min(to, Ordering.natural().max(from, currentHourStart)));
          localStart = localStart.plusHours(1).withMinute(0).withSecond(0).withNano(0);
          break;
        default:
      }
    }
    return result;
  }
}
