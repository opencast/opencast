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

import static org.junit.Assert.assertEquals;

import org.opencastproject.statistics.api.DataResolution;
import org.opencastproject.util.data.Tuple;

import com.google.common.collect.Ordering;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;


public class InfluxStatisticsProviderTest {

  private static final Logger logger = LoggerFactory.getLogger(InfluxStatisticsProviderTest.class);
  private static final int repetitions = 1000;
  private static Random random = new Random(Instant.now().toEpochMilli());

  private static Instant randomInstant() {
    long randomEpochSec = random.nextInt((int) Instant.now().getEpochSecond());
    return Instant.ofEpochSecond(randomEpochSec);
  }


  @Test
  public void testGetPeriodsDaily() {
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      logger.info("from {} to {} {}", from, to, DataResolution.DAILY.name());
      final List<Tuple<Instant, Instant>> periods = InfluxStatisticsProvider.getPeriods(from, to, DataResolution.DAILY,
          ZoneId.of("Z"));
      assertEquals(1, periods.size());
      assertEquals(from, periods.get(0).getA());
      assertEquals(to, periods.get(0).getB());
    }
  }

  @Test
  public void testGetPeriodsWeekly() {
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      logger.info("from {} to {} {}", from, to, DataResolution.WEEKLY.name());
      final List<Tuple<Instant, Instant>> periods = InfluxStatisticsProvider.getPeriods(from, to, DataResolution.WEEKLY,
          ZoneId.of("Z"));
      assertEquals(1, periods.size());
      assertEquals(from, periods.get(0).getA());
      assertEquals(to, periods.get(0).getB());
    }
  }


  @Test
  public void testGetPeriodsMonthly() {
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      logger.info("from {} to {} {}", from, to, DataResolution.MONTHLY.name());
      final List<Tuple<Instant, Instant>> periods = InfluxStatisticsProvider.getPeriods(from, to, DataResolution.MONTHLY,
          ZoneId.of("Z"));
      assertEquals(from, periods.get(0).getA());
      if (periods.size() > 1) {
        assertEquals(
            LocalDateTime
                .ofInstant(from, ZoneOffset.UTC)
                .plusMonths(1)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .minusNanos(1)
                .toInstant(ZoneOffset.UTC),
            periods.get(0).getB());
      } else {
        assertEquals(to, periods.get(0).getB());
        continue;
      }
      assertEquals(to, periods.get(periods.size() - 1).getB());
      assertEquals(LocalDateTime
              .ofInstant(to, ZoneOffset.UTC)
              .withDayOfMonth(1)
              .withHour(0)
              .withMinute(0)
              .withSecond(0)
              .withNano(0)
              .toInstant(ZoneOffset.UTC),
          periods.get(periods.size() - 1).getA());
    }
  }

  @Test
  public void testGetPeriodsYearly() {
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      logger.info("from {} to {} {}", from, to, DataResolution.YEARLY.name());
      final List<Tuple<Instant, Instant>> periods = InfluxStatisticsProvider.getPeriods(from, to, DataResolution.YEARLY,
          ZoneId.of("Z"));
      assertEquals(from, periods.get(0).getA());
      if (periods.size() > 1) {
        assertEquals(
            LocalDateTime
                .ofInstant(from, ZoneOffset.UTC)
                .plusYears(1)
                .withMonth(1)
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .minusNanos(1)
                .toInstant(ZoneOffset.UTC),
            periods.get(0).getB());
      } else {
        assertEquals(to, periods.get(0).getB());
        continue;
      }
      assertEquals(to, periods.get(periods.size() - 1).getB());
      assertEquals(LocalDateTime
              .ofInstant(to, ZoneOffset.UTC)
              .withMonth(1)
              .withDayOfMonth(1)
              .withHour(0)
              .withMinute(0)
              .withSecond(0)
              .withNano(0)
              .toInstant(ZoneOffset.UTC),
          periods.get(periods.size() - 1).getA());
    }
  }

}
