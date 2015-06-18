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

package org.opencastproject.silencedetection.api;

import java.util.LinkedList;
import java.util.List;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author wsmirnow
 */
public class MediaSegmentsTest {

  private MediaSegments mediaSegments;

  @Before
  public void setUp() {
    List<MediaSegment> segments = new LinkedList<MediaSegment>();
    segments.add(new MediaSegment(0L, 10L));
    segments.add(new MediaSegment(25L, 50L));
    segments.add(new MediaSegment(60L, 120L));
    mediaSegments = new MediaSegments("track-1", "foo", segments);
  }

  /**
   * Test of getMediaSegments method, of class MediaSegments.
   */
  @Test
  public void testGetMediaSegments() {
    assertNotNull(mediaSegments);
    assertNotNull(mediaSegments.getMediaSegments());
    assertTrue(mediaSegments.getMediaSegments().size() == 3);
  }

  /**
   * Test of getTrackId method, of class MediaSegments.
   */
  @Test
  public void testGetTrackId() {
    assertNotNull(mediaSegments);
    assertEquals("track-1", mediaSegments.getTrackId());
  }

  /**
   * Test of getFilePath method, of class MediaSegments.
   */
  @Test
  public void testGetFilePath() {
    assertNotNull(mediaSegments);
    assertEquals("foo", mediaSegments.getFilePath());
  }

  /**
   * Test of toXml method, of class MediaSegments.
   */
  @Test
  public void testToXml() throws Exception {
    assertNotNull(mediaSegments);
    String xml = mediaSegments.toXml();
    assertNotNull(xml);
    assertFalse(xml.isEmpty());
  }

  /**
   * Test of fromXml method, of class MediaSegments.
   */
  @Test
  public void testFromXml() throws Exception {
    assertNotNull(mediaSegments);
    String xml = mediaSegments.toXml();
    MediaSegments mediaSegments = MediaSegments.fromXml(xml);
    assertNotNull(mediaSegments);
    assertNotNull(mediaSegments.getMediaSegments());
    assertTrue(mediaSegments.getMediaSegments().size() == 3);
  }
}
