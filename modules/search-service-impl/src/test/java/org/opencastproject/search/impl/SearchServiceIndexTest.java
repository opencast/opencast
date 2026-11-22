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

package org.opencastproject.search.impl;

import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AccessControlList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Regression test for series deletion timestamp format (Issue #8326).
 */
public class SearchServiceIndexTest {

  private SearchServiceIndex searchServiceIndex;

  @Before
  public void setUp() {
    searchServiceIndex = new SearchServiceIndex();
  }

  @Test
  public void testBuildSeriesDeletionMapStoresIsoStringTimestamp() {
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    Map<String, Object> deletionMap = searchServiceIndex.buildSeriesDeletionMap(now);

    Assert.assertNotNull("Deletion map must not be null", deletionMap);
    Assert.assertTrue("Deletion map must contain DELETED_DATE key",
        deletionMap.containsKey(SearchResult.DELETED_DATE));
    Assert.assertTrue("Deletion map must contain MODIFIED_DATE key",
        deletionMap.containsKey(SearchResult.MODIFIED_DATE));

    Object deletedVal = deletionMap.get(SearchResult.DELETED_DATE);
    Object modifiedVal = deletionMap.get(SearchResult.MODIFIED_DATE);

    // Verify both are String instances (ISO-8601), NOT Numbers (epoch seconds)
    Assert.assertTrue("DELETED_DATE must be stored as a String", deletedVal instanceof String);
    Assert.assertTrue("MODIFIED_DATE must be stored as a String", modifiedVal instanceof String);
    Assert.assertFalse("DELETED_DATE must not be a Number", deletedVal instanceof Number);
    Assert.assertFalse("MODIFIED_DATE must not be a Number", modifiedVal instanceof Number);

    String deletedString = (String) deletedVal;
    String modifiedString = (String) modifiedVal;

    // Both timestamps must be identical for the same deletion operation
    Assert.assertEquals("DELETED_DATE and MODIFIED_DATE must be identical", deletedString, modifiedString);

    // Verify parsing with Instant.parse succeeds
    Instant parsedDeleted = Instant.parse(deletedString);
    Instant parsedModified = Instant.parse(modifiedString);

    Assert.assertEquals("Parsed deleted Instant must equal original", now, parsedDeleted);
    Assert.assertEquals("Parsed modified Instant must equal original", now, parsedModified);
  }

  @Test
  public void testSeriesRehydrationWithIsoStringDeletionDate() throws Exception {
    Instant deletionInstant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    Map<String, Object> deletionMap = searchServiceIndex.buildSeriesDeletionMap(deletionInstant);

    Map<String, Object> seriesData = createMinimalSeriesIndexData();
    seriesData.put(SearchResult.DELETED_DATE, deletionMap.get(SearchResult.DELETED_DATE));
    seriesData.put(SearchResult.MODIFIED_DATE, deletionMap.get(SearchResult.MODIFIED_DATE));

    // Rehydration must succeed without ClassCastException
    SearchResult result = SearchResult.rehydrate(seriesData);

    Assert.assertNotNull("Rehydrated SearchResult must not be null", result);
    Assert.assertEquals("series-1", result.getId());
    Assert.assertEquals("Deletion date must match", new Date(deletionInstant.toEpochMilli()), result.getDeletionDate());
    Assert.assertEquals("Modified date must match", new Date(deletionInstant.toEpochMilli()), result.getModifiedDate());
  }

  private Map<String, Object> createMinimalSeriesIndexData() throws Exception {
    Map<String, Object> data = new HashMap<>();
    data.put(SearchResult.TYPE, SearchService.IndexEntryType.Series.name());
    data.put(SearchResult.ORG, "mh_default_org");

    Map<String, Object> dcMap = new HashMap<>();
    dcMap.put("identifier", java.util.List.of("series-1"));
    data.put(SearchResult.DUBLINCORE, dcMap);

    data.put(SearchResult.INDEX_ACL, SearchResult.dehydrateAclForIndex(new AccessControlList()));
    return data;
  }
}
