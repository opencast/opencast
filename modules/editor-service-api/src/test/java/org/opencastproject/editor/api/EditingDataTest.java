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
package org.opencastproject.editor.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

public class EditingDataTest {
  public static final String TITLE = "title";
  public static final String DATE = "date";
  public static final String SERIES = "series";
  public static final String DURATION = "duration";
  public static final String SEGMENTS = "segments";
  public static final String TRACKS = "tracks";
  public static final String WORKFLOWS = "workflows";
  public static final String SERIES_ID = "id";
  public static final String SERIES_NAME = "title";

  protected String loadResource(String resourceName) throws Exception {
    URI postUri = EditingDataTest.class.getResource(resourceName).toURI();
    String data = IOUtils.toString(postUri, Charset.defaultCharset());
    assertNotNull(data);
    return data;
  }

  @Test
  public void testEditingInfoParse() throws Exception {
    EditingData editingInfo = EditingData.parse(loadResource("/edit.json"));

    final List<SegmentData> segments = editingInfo.getSegments();
    assertEquals(2, segments.size());
    assertTrue(segments.contains(new SegmentData(0L, 20000L, false)));
    assertTrue(segments.contains(new SegmentData(20000L, 30000L, true)));

    final List<TrackData> tracks = editingInfo.getTracks();
    assertEquals(2, tracks.size());

    assertEquals("placeholdervideo", editingInfo.getPostProcessingWorkflow());
  }

  @Test
  public void testEditingInfoParseNoWorkflow() throws Exception {
    EditingData editingInfo = EditingData.parse(loadResource("/editWithoutWorkflow.json"));

    final List<SegmentData> segments = editingInfo.getSegments();
    assertEquals(2, segments.size());
    assertTrue(segments.contains(new SegmentData(0L, 20000L, false)));
    assertTrue(segments.contains(new SegmentData(20000L, 30000L, true)));

    final List<TrackData> tracks = editingInfo.getTracks();
    assertEquals(2, tracks.size());

    assertEquals(null, editingInfo.getPostProcessingWorkflow());

    assertNotNull(editingInfo.toString());
  }

  @Test
  public void testEditingInfoEncode() throws Exception {
    JSONParser parser = new JSONParser();
    String originalJson = loadResource("/edit.json");

    EditingData editingInfo = EditingData.parse(originalJson);
    JSONObject origJson = (JSONObject) parser.parse(originalJson);
    JSONObject resultingJson = (JSONObject) parser.parse(editingInfo.toString());

    assertEquals(origJson.get(TITLE), resultingJson.get(TITLE));
    assertEquals(origJson.get(DURATION), resultingJson.get(DURATION));
    assertEquals(origJson.get(DATE), resultingJson.get(DATE));
    JSONObject origSeries = (JSONObject) origJson.get(SERIES);
    JSONObject resultSeries = (JSONObject) resultingJson.get(SERIES);
    assertEquals(origSeries.get(SERIES_ID), resultSeries.get(SERIES_ID));
    assertEquals(origSeries.get(SERIES_NAME), resultSeries.get(SERIES_NAME));
  }

  @Test
  public void testInvalidEncoding() throws Exception {
    try {
      EditingData.parse("bla");
      fail();
    } catch (Exception e) {
    }
  }

}
