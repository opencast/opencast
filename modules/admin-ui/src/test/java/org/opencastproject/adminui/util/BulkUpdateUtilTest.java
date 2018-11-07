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

package org.opencastproject.adminui.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.opencastproject.index.service.impl.index.event.Event;

import com.google.common.io.Files;

import org.easymock.EasyMock;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.io.File;
import java.io.Reader;
import java.nio.charset.Charset;
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
    final JSONObject scheduling = loadJsonObject("metadata.json");
    final JSONObject expected = loadJsonObject("metadata-expected.json");
    final JSONObject actual = BulkUpdateUtil.toNonTechnicalMetadataJson(scheduling);
    assertThat(actual.toJSONString(), SameJSONAs.sameJSONAs(expected.toJSONString()));
  }

  @Test
  public void testMergeMetadataFields() {
    final JSONObject metadata1 = loadJsonObject("merge1.json");
    final JSONObject metadata2 = loadJsonObject("merge2.json");
    final JSONObject expected = loadJsonObject("metadata-expected.json");
    final JSONObject actual = BulkUpdateUtil.mergeMetadataFields(metadata1, metadata2);
    assertThat(actual.toJSONString(), SameJSONAs.sameJSONAs(expected.toJSONString()));
  }

  @Test
  public void testMergeMetadataFieldsFirstNull() {
    final JSONObject metadata1 = null;
    final JSONObject metadata2 = loadJsonObject("merge2.json");
    final JSONObject expected = loadJsonObject("merge2.json");
    final JSONObject actual = BulkUpdateUtil.mergeMetadataFields(metadata1, metadata2);
    assertThat(actual.toJSONString(), SameJSONAs.sameJSONAs(expected.toJSONString()));
  }

  @Test
  public void testMergeMetadataFieldsSecondNull() {
    final JSONObject metadata1 = loadJsonObject("merge1.json");
    final JSONObject metadata2 = null;
    final JSONObject expected = loadJsonObject("merge1.json");
    final JSONObject actual = BulkUpdateUtil.mergeMetadataFields(metadata1, metadata2);
    assertThat(actual.toJSONString(), SameJSONAs.sameJSONAs(expected.toJSONString()));
  }

  @Test
  public void testBulkUpdateInstructions() {
    final JSONArray jsonArray = loadJsonArray("instructions.json");
    assertEquals(jsonArray.size(), 1);
    final JSONObject json = (JSONObject) jsonArray.get(0);
    final BulkUpdateUtil.BulkUpdateInstructions actual = new BulkUpdateUtil.BulkUpdateInstructions(jsonArray.toJSONString());
    final List<String> expectedIds = (JSONArray) json.get("events");
    final JSONObject expectedScheduling = (JSONObject) json.get("scheduling");
    final JSONObject expectedMetadata = (JSONObject) json.get("metadata");
    final BulkUpdateUtil.BulkUpdateInstructionGroup firstGroup = actual.getGroups().get(0);
    assertThat(firstGroup.getMetadata().toJSONString(), SameJSONAs.sameJSONAs(expectedMetadata.toJSONString()));
    assertThat(firstGroup.getScheduling().toJSONString(), SameJSONAs.sameJSONAs(expectedScheduling.toJSONString()));
    assertEquals(expectedIds, firstGroup.getEventIds());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBulkUpdateInstructionsParseError() {
    new BulkUpdateUtil.BulkUpdateInstructions("hello");
  }

  private void testAddSchedulingDates(final String filename, final Event event) {
    final JSONObject scheduling = loadJsonObject(filename + ".json");
    final JSONObject expected = loadJsonObject(filename + "-expected.json");
    final JSONObject actual = BulkUpdateUtil.addSchedulingDates(event, scheduling);
    assertThat(actual.toJSONString(), SameJSONAs.sameJSONAs(expected.toJSONString()));
  }

  private static Object loadJson(String filename) {
    final Charset utf8 = Charset.forName("utf-8");
    final String fullName = "/bulkupdate/" +  filename;
    try (Reader reader = Files.newReader(new File(BulkUpdateUtil.class.getResource(fullName).toURI()), utf8)) {
      return new JSONParser().parse(reader);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static JSONObject loadJsonObject(String filename) {
    return (JSONObject)loadJson(filename);
  }

  private static JSONArray loadJsonArray(String filename) {
    return (JSONArray)loadJson(filename);
  }
}
