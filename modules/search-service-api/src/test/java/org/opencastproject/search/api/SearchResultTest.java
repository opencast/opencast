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

package org.opencastproject.search.api;

import static org.junit.Assert.assertEquals;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCores;

import org.junit.Test;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public class SearchResultTest {

  /**
   * Expected values are what OpenSearch's {@code DateFieldMapper.DEFAULT_DATE_TIME_FORMATTER} produced before the
   * search service stopped depending on the OpenSearch server library.
   */
  private static final Map<String, String> CREATED_DATES = Map.ofEntries(
      Map.entry("2024-01-01T10:00:00Z", "2024-01-01T10:00:00.000Z"),
      Map.entry("2024-01-01T10:00:00.123+02:00", "2024-01-01T08:00:00.123Z"),
      Map.entry("2024-01-01", "2024-01-01T00:00:00.000Z"),
      Map.entry("2024-05", "2024-05-01T00:00:00.000Z"),
      Map.entry("2024", "2024-01-01T00:00:00.000Z"),
      Map.entry("2024-05-17T10", "2024-05-17T10:00:00.000Z"),
      Map.entry("2024-01-01T10:00Z", "2024-01-01T10:00:00.000Z"),
      Map.entry("2024-01-01T10:00:00", "2024-01-01T10:00:00.000Z"),
      Map.entry("1700000000000", "2023-11-14T22:13:20.000Z"),
      Map.entry("20240517", "1970-01-01T05:37:20.517Z"),
      Map.entry("24", "1970-01-01T00:00:00.024Z"),
      Map.entry("2024-06-30T23:59:59.999999Z", "2024-06-30T23:59:59.999Z"),
      Map.entry("2024-01-01T10:00:00+0100", "2024-01-01T09:00:00.000Z"));

  @Test
  public void testCreatedDateIsNormalizedForIndex() {
    for (Map.Entry<String, String> date : CREATED_DATES.entrySet()) {
      final var dc = DublinCores.mkOpencastEpisode().getCatalog();
      dc.set(DublinCore.PROPERTY_CREATED, date.getKey());
      assertEquals(date.getKey(), List.of(date.getValue()), SearchResult.dehydrateDC(dc).get("created"));
    }
  }

  @Test
  public void testParseDate() {
    assertEquals(Instant.parse("2024-01-01T08:00:00.123Z"), SearchResult.parseDate("2024-01-01T10:00:00.123+02:00"));
    assertEquals(Instant.parse("2024-01-01T00:00:00Z"), SearchResult.parseDate("2024-01-01"));
    assertEquals(Instant.ofEpochMilli(1700000000000L), SearchResult.parseDate("1700000000000"));
  }

  @Test(expected = DateTimeParseException.class)
  public void testParseInvalidDate() {
    SearchResult.parseDate("2024-5");
  }
}
