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
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Ordering;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


public class StatisticsUtilTest {

  private static final Logger logger = LoggerFactory.getLogger(StatisticsUtilTest.class);
  private static final int repetitions = 50;
  private static Random random = new Random(Instant.now().toEpochMilli());

  private static Instant randomInstant() {
    long randomEpochSec = random.nextInt((int) Instant.now().getEpochSecond());
    return Instant.ofEpochSecond(randomEpochSec);
  }

  @Test
  public void testStartAfterEnd() {
    // Test start after end
    final Instant wrongFrom = Instant.parse("2019-04-01T00:00:00.000Z");
    final Instant wrongTo = Instant.parse("2019-03-30T17:00:00.000Z");
    Arrays.stream(DataResolution.values()).forEach(dr -> {
      assertTrue(StatisticsUtil.getBuckets(wrongFrom, wrongTo, dr, ZoneId.of("Z")).isEmpty());
    });
  }

  @Test
  public void testGetBucketsHourly() {
    // Tests with pre-picked valid data
    new HashMap<Instant, Instant>() {{
      put(Instant.parse("2019-10-28T17:00:00.000Z"), Instant.parse("2020-01-02T11:30:00.000Z"));
      put(Instant.parse("2019-03-30T17:00:00.000Z"), Instant.parse("2019-04-01T00:00:00.000Z"));
      put(Instant.parse("2019-10-27T00:00:00.000Z"), Instant.parse("2019-10-28T00:00:00.000Z"));
    }}.forEach(this::testGetBucketsHourly);

    // Tests with random input
    for (int i = 0; i < repetitions / 24; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      testGetBucketsHourly(from, to);
    }
  }

  private void testGetBucketsHourly(Instant from, Instant to) {
    logger.info("from {} to {} {}", from, to, DataResolution.HOURLY.name());
    final List<Instant> buckets = StatisticsUtil.getBuckets(from, to, DataResolution.HOURLY, ZoneId.of("Z"));
    assertEquals("from: " + from + " to: " + to, from, buckets.get(0));
    if (buckets.size() == 1) {
      return;
    }
    for (int bucket = 1; bucket < buckets.size() - 1; bucket++) {
      assertTrue("from: " + from + " to: " + to, buckets.get(bucket).isAfter(from));
      assertTrue("from: " + from + " to: " + to, buckets.get(bucket).isBefore(to));
      final LocalDateTime ldt = LocalDateTime.ofInstant(buckets.get(bucket), ZoneOffset.UTC);
      assertEquals("from: " + from + " to: " + to, 0, ldt.getMinute());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getSecond());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getNano());
    }
    assertEquals("from: " + from + " to: " + to,
        LocalDateTime.ofInstant(
            to,
            ZoneOffset.UTC)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toInstant(ZoneOffset.UTC),
        buckets.get(buckets.size() - 1));
  }

  @Test
  public void testGetBucketsDaily() {
    // Tests with pre-picked valid data
    new HashMap<Instant, Instant>() {{
      put(Instant.parse("2019-10-28T17:00:00.000Z"), Instant.parse("2020-01-02T11:30:00.000Z"));
      put(Instant.parse("2019-03-30T17:00:00.000Z"), Instant.parse("2019-04-01T00:00:00.000Z"));
      put(Instant.parse("2019-10-27T00:00:00.000Z"), Instant.parse("2019-10-28T00:00:00.000Z"));
    }}.forEach(this::testGetBucketsDaily);

    // Tests with random input
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      testGetBucketsDaily(from, to);
    }
  }

  private void testGetBucketsDaily(Instant from, Instant to) {
    logger.info("from {} to {} {}", from, to, DataResolution.DAILY.name());
    final List<Instant> buckets = StatisticsUtil.getBuckets(from, to, DataResolution.DAILY, ZoneId.of("Z"));
    assertEquals("from: " + from + " to: " + to, from, buckets.get(0));
    if (buckets.size() == 1) {
      return;
    }
    for (int bucket = 1; bucket < buckets.size() - 1; bucket++) {
      assertTrue("from: " + from + " to: " + to, buckets.get(bucket).isAfter(from));
      assertTrue("from: " + from + " to: " + to, buckets.get(bucket).isBefore(to));
      final LocalDateTime ldt = LocalDateTime.ofInstant(buckets.get(bucket), ZoneOffset.UTC);
      assertEquals("from: " + from + " to: " + to, 0, ldt.getHour());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getMinute());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getSecond());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getNano());
    }
    assertEquals("from: " + from + " to: " + to,
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

  @Test
  public void testGetBucketsWeekly() {
    // Tests with pre-picked valid data
    new HashMap<Instant, Instant>() {{
      put(Instant.parse("2019-10-28T17:00:00.000Z"), Instant.parse("2020-01-02T11:30:00.000Z"));
      put(Instant.parse("2019-03-30T17:00:00.000Z"), Instant.parse("2019-04-01T00:00:00.000Z"));
      put(Instant.parse("2019-01-01T00:00:00.000Z"), Instant.parse("2019-12-31T23:59:59.999Z"));
    }}.forEach(this::testGetBucketsWeekly);

    // Tests with random input
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      testGetBucketsWeekly(from, to);
    }
  }

  private void testGetBucketsWeekly(Instant from, Instant to) {
    logger.info("from {} to {} {}", from, to, DataResolution.WEEKLY.name());
    final int fromOffMonday = 1 - LocalDateTime.ofInstant(from, ZoneOffset.UTC).getDayOfWeek().getValue();
    final int toOffMonday = 1 - LocalDateTime.ofInstant(to, ZoneOffset.UTC).getDayOfWeek().getValue();
    final List<Instant> buckets = StatisticsUtil.getBuckets(from, to, DataResolution.WEEKLY, ZoneId.of("Z"));
    assertEquals("from: " + from + " to: " + to, LocalDateTime.ofInstant(
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
      return;
    }
    for (int bucket = 1; bucket < buckets.size() - 1; bucket++) {
      assertTrue("from: " + from + " to: " + to, buckets.get(bucket).isAfter(from));
      assertTrue("from: " + from + " to: " + to, buckets.get(bucket).isBefore(to));
      final LocalDateTime ldt = LocalDateTime.ofInstant(buckets.get(bucket), ZoneOffset.UTC);
      assertEquals("from: " + from + " to: " + to, DayOfWeek.MONDAY, ldt.getDayOfWeek());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getHour());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getMinute());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getSecond());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getNano());
    }
    assertEquals("from: " + from + " to: " + to,
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

  @Test
  public void testGetBucketsMonthly() {
    // Tests with pre-picked valid data
    new HashMap<Instant, Instant>() {{
      put(Instant.parse("2019-05-28T17:00:00.000Z"), Instant.parse("2020-01-02T11:30:00.000Z"));
      put(Instant.parse("2019-02-01T00:00:00.000Z"), Instant.parse("2019-04-30T23:59:59.999Z"));
      put(Instant.parse("2019-10-27T00:00:00.000Z"), Instant.parse("2019-10-28T00:00:00.000Z"));
    }}.forEach(this::testGetBucketsMonthly);

    // Tests with random input
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      testGetBucketsMonthly(from, to);
    }
  }

  private void testGetBucketsMonthly(Instant from, Instant to) {
    logger.info("from {} to {} {}", from, to, DataResolution.MONTHLY.name());
    final List<Instant> buckets = StatisticsUtil.getBuckets(from, to, DataResolution.MONTHLY, ZoneId.of("Z"));
    assertEquals("from: " + from + " to: " + to, from, buckets.get(0));
    if (buckets.size() == 1) {
      return;
    }
    for (int bucket = 1; bucket < buckets.size() - 1; bucket++) {
      assertTrue("from: " + from + " to: " + to, buckets.get(bucket).isAfter(from));
      assertTrue("from: " + from + " to: " + to, buckets.get(bucket).isBefore(to));
      final LocalDateTime ldt = LocalDateTime.ofInstant(buckets.get(bucket), ZoneOffset.UTC);
      assertEquals("from: " + from + " to: " + to, 1, ldt.getDayOfMonth());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getHour());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getMinute());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getSecond());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getNano());
    }
    assertEquals("from: " + from + " to: " + to,
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

  @Test
  public void testGetBucketsYearly() {
    // Tests with pre-picked valid data
    new HashMap<Instant, Instant>() {{
      put(Instant.parse("2018-10-28T17:00:00.000Z"), Instant.parse("2020-01-02T11:30:00.000Z"));
      put(Instant.parse("2019-01-01T00:00:00.000Z"), Instant.parse("2019-12-31T23:59:59.999Z"));
      put(Instant.parse("2019-10-27T00:00:00.000Z"), Instant.parse("2029-10-28T00:00:00.000Z"));
    }}.forEach(this::testGetBucketsYearly);

    // Tests with random input
    for (int i = 0; i < repetitions; i++) {
      final Instant a = randomInstant();
      final Instant b = randomInstant();
      final Instant from = Ordering.natural().min(a, b);
      final Instant to = Ordering.natural().max(a, b);
      testGetBucketsYearly(from, to);
    }
  }

  private void testGetBucketsYearly(Instant from, Instant to) {
    logger.info("from {} to {} {}", from, to, DataResolution.YEARLY.name());
    final List<Instant> buckets = StatisticsUtil.getBuckets(from, to, DataResolution.YEARLY, ZoneId.of("Z"));
    assertEquals("from: " + from + " to: " + to, from, buckets.get(0));
    if (buckets.size() == 1) {
      return;
    }
    for (int bucket = 1; bucket < buckets.size() - 1; bucket++) {
      assertTrue("from: " + from + " to: " + to, buckets.get(bucket).isAfter(from));
      assertTrue("from: " + from + " to: " + to, buckets.get(bucket).isBefore(to));
      final LocalDateTime ldt = LocalDateTime.ofInstant(buckets.get(bucket), ZoneOffset.UTC);
      assertEquals("from: " + from + " to: " + to, Month.JANUARY, ldt.getMonth());
      assertEquals("from: " + from + " to: " + to, 1, ldt.getDayOfMonth());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getHour());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getMinute());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getSecond());
      assertEquals("from: " + from + " to: " + to, 0, ldt.getNano());
    }
    assertEquals("from: " + from + " to: " + to,
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
