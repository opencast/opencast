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


import static org.junit.Assert.assertEquals;

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


public class StatisticsUtilTest {

  private static final Logger logger = LoggerFactory.getLogger(StatisticsUtilTest.class);
  private static final int repetitions = 1000;
  private static Random random = new Random(Instant.now().toEpochMilli());

  private static Instant randomInstant() {
    long randomEpochSec = random.nextInt((int) Instant.now().getEpochSecond());
    return Instant.ofEpochSecond(randomEpochSec);
  }

  @Test
  public void testGetBucketsHourly() {
    for (int i = 0; i < repetitions / 24; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      logger.info("from {} to {} {}", from, to, DataResolution.HOURLY.name());
      final List<Instant> buckets = StatisticsUtil.getBuckets(from, to, DataResolution.HOURLY, ZoneId.of("Z"));
      assertEquals(from, buckets.get(0));
      if (buckets.size() == 1) {
        continue;
      }
      assertEquals(
          LocalDateTime.ofInstant(
              to,
              ZoneOffset.UTC)
              .withMinute(0)
              .withSecond(0)
              .withNano(0)
              .toInstant(ZoneOffset.UTC),
          buckets.get(buckets.size() - 1));
    }
  }

  @Test
  public void testGetBucketsDaily() {
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      logger.info("from {} to {} {}", from, to, DataResolution.DAILY.name());
      final List<Instant> buckets = StatisticsUtil.getBuckets(from, to, DataResolution.DAILY, ZoneId.of("Z"));
      assertEquals(from, buckets.get(0));
      if (buckets.size() == 1) {
        continue;
      }
      assertEquals(
          LocalDateTime.ofInstant(
              to,
              ZoneOffset.UTC)
              .withHour(0)
              .withMinute(0)
              .withSecond(0)
              .withNano(0)
              .toInstant(ZoneOffset.UTC),
          buckets.get(buckets.size() - 1));
    }
  }

  @Test
  public void testGetBucketsWeekly() {
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      logger.info("from {} to {} {}", from, to, DataResolution.WEEKLY.name());
      final int fromOffMonday = 1 - LocalDateTime.ofInstant(from, ZoneOffset.UTC).getDayOfWeek().getValue();
      final int toOffMonday = 1 - LocalDateTime.ofInstant(to, ZoneOffset.UTC).getDayOfWeek().getValue();
      final List<Instant> buckets = StatisticsUtil.getBuckets(from, to, DataResolution.WEEKLY, ZoneId.of("Z"));
      assertEquals(LocalDateTime.ofInstant(
          from,
          ZoneOffset.UTC)
              .plusDays(fromOffMonday)
              .withHour(0)
              .withMinute(0)
              .withSecond(0)
              .withNano(0)
              .toInstant(ZoneOffset.UTC),
          buckets.get(0));
      if (buckets.size() == 1) {
        continue;
      }
      assertEquals(
          LocalDateTime.ofInstant(
              to,
              ZoneOffset.UTC)
              .plusDays(toOffMonday)
              .withHour(0)
              .withMinute(0)
              .withSecond(0)
              .withNano(0)
              .toInstant(ZoneOffset.UTC),
          buckets.get(buckets.size() - 1));
    }
  }

  @Test
  public void testGetBucketsMonthly() {
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      logger.info("from {} to {} {}", from, to, DataResolution.MONTHLY.name());
      final List<Instant> buckets = StatisticsUtil.getBuckets(from, to, DataResolution.MONTHLY, ZoneId.of("Z"));
      assertEquals(from, buckets.get(0));
      if (buckets.size() == 1) {
        continue;
      }
      assertEquals(
          LocalDateTime.ofInstant(
              to,
              ZoneOffset.UTC)
              .withDayOfMonth(1)
              .withHour(0)
              .withMinute(0)
              .withSecond(0)
              .withNano(0)
              .toInstant(ZoneOffset.UTC),
          buckets.get(buckets.size() - 1));
    }
  }

  @Test
  public void testGetBucketsYearly() {
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      logger.info("from {} to {} {}", from, to, DataResolution.YEARLY.name());
      final List<Instant> buckets = StatisticsUtil.getBuckets(from, to, DataResolution.YEARLY, ZoneId.of("Z"));
      assertEquals(from, buckets.get(0));
      if (buckets.size() == 1) {
        continue;
      }
      assertEquals(
          LocalDateTime.ofInstant(
              to,
              ZoneOffset.UTC)
              .withMonth(1)
              .withDayOfMonth(1)
              .withHour(0)
              .withMinute(0)
              .withSecond(0)
              .withNano(0)
              .toInstant(ZoneOffset.UTC),
          buckets.get(buckets.size() - 1));
    }
  }

}
