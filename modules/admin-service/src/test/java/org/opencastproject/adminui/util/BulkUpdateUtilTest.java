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

package org.opencastproject.adminui.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import org.opencastproject.elasticsearch.index.objects.event.Event;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.easymock.EasyMock;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

public class BulkUpdateUtilTest {

  @Test
  public void testAddSchedulingDates() {
    final Event event = EasyMock.mock(Event.class);
    EasyMock.expect(event.getRecordingStartDate()).andReturn("2018-05-16T13:22:23Z").anyTimes();
    EasyMock.expect(event.getRecordingEndDate()).andReturn("2018-05-16T17:12:13Z").anyTimes();
    EasyMock.replay(event);
    testAddSchedulingDates("scheduling1", event); // Change start and end time
    testAddSchedulingDates("scheduling2", event); // Change end time to be before start time
    testAddSchedulingDates("scheduling3", event); // Change weekday
    testAddSchedulingDates("scheduling4", event); // Change duration
    testAddSchedulingDates("scheduling5", event); // Change duration and end time
  }

  @Test
  public void testToNonTechnicalMetadataJson() {
    final JsonObject scheduling = loadJsonObject("metadata.json");
    final JsonObject expected = loadJsonObject("metadata-expected.json");
    final JsonObject actual = BulkUpdateUtil.toNonTechnicalMetadataJson(scheduling);
    assertThat(actual.toString(), SameJSONAs.sameJSONAs(expected.toString()));
  }

  @Test
  public void testMergeMetadataFields() {
    final JsonObject metadata1 = loadJsonObject("merge1.json");
    final JsonObject metadata2 = loadJsonObject("merge2.json");
    final JsonObject expected = loadJsonObject("metadata-expected.json");
    final JsonObject actual = BulkUpdateUtil.mergeMetadataFields(metadata1, metadata2);
    assertThat(actual.toString(), SameJSONAs.sameJSONAs(expected.toString()));
  }

  @Test
  public void testMergeMetadataFieldsFirstNull() {
    final JsonObject metadata1 = null;
    final JsonObject metadata2 = loadJsonObject("merge2.json");
    final JsonObject expected = loadJsonObject("merge2.json");
    final JsonObject actual = BulkUpdateUtil.mergeMetadataFields(metadata1, metadata2);
    assertThat(actual.toString(), SameJSONAs.sameJSONAs(expected.toString()));
  }

  @Test
  public void testMergeMetadataFieldsSecondNull() {
    final JsonObject metadata1 = loadJsonObject("merge1.json");
    final JsonObject metadata2 = null;
    final JsonObject expected = loadJsonObject("merge1.json");
    final JsonObject actual = BulkUpdateUtil.mergeMetadataFields(metadata1, metadata2);
    assertThat(actual.toString(), SameJSONAs.sameJSONAs(expected.toString()));
  }

  @Test
  public void testBulkUpdateInstructions() {
    final JsonArray jsonArray = loadJsonArray("instructions.json");
    assertEquals(jsonArray.size(), 1);
    final JsonObject json = jsonArray.get(0).getAsJsonObject();
    final BulkUpdateUtil.BulkUpdateInstructions actual = new BulkUpdateUtil.BulkUpdateInstructions(
        jsonArray.toString());
    final List<String> expectedIds = new ArrayList<>();
    json.getAsJsonArray("events").forEach(id -> expectedIds.add(id.getAsString()));
    final JsonObject expectedScheduling = json.getAsJsonObject("scheduling");
    final JsonObject expectedMetadata = json.getAsJsonObject("metadata");
    final BulkUpdateUtil.BulkUpdateInstructionGroup firstGroup = actual.getGroups().get(0);
    assertThat(firstGroup.getMetadata().toString(), SameJSONAs.sameJSONAs(expectedMetadata.toString()));
    assertThat(firstGroup.getScheduling().toString(), SameJSONAs.sameJSONAs(expectedScheduling.toString()));
    assertEquals(expectedIds, firstGroup.getEventIds());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBulkUpdateInstructionsParseError() {
    new BulkUpdateUtil.BulkUpdateInstructions("hello");
  }

  private void testAddSchedulingDates(final String filename, final Event event) {
    final JsonObject scheduling = loadJsonObject(filename + ".json");
    final JsonObject expected = loadJsonObject(filename + "-expected.json");
    final JsonObject actual = BulkUpdateUtil.addSchedulingDates(event, scheduling);
    assertThat(actual.toString(), SameJSONAs.sameJSONAs(expected.toString()));
  }

  private static JsonElement loadJson(String filename) {
    final String fullName = "/bulkupdate/" +  filename;
    try (InputStream in = BulkUpdateUtil.class.getResourceAsStream(fullName)) {
      return JsonParser.parseReader(new InputStreamReader(in));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static JsonObject loadJsonObject(String filename) {
    return loadJson(filename).getAsJsonObject();
  }

  private static JsonArray loadJsonArray(String filename) {
    return loadJson(filename).getAsJsonArray();
  }
}
