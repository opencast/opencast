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
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 * @author wsmirnow
 */
public class MediaSegmentTest {

  private List<MediaSegment> segments = new LinkedList<MediaSegment>();

  @Before
  public void setUp() {
    List<MediaSegment> segments = new LinkedList<MediaSegment>();
    segments.add(new MediaSegment(0L, 10L));
    segments.add(new MediaSegment(25L, 50L));
    segments.add(new MediaSegment(60L, 120L));
  }

  /**
   * Test of getSegmentStart method, of class MediaSegment.
   */
  @Test
  public void testGetSegmentStart() {
    assertNotNull(segments);
    for (MediaSegment segment : segments) {
      assertNotNull(segment);
      if (segment.getSegmentStart() != 0
              || segment.getSegmentStart() != 25
              || segment.getSegmentStart() != 60) {
        fail("MediaSegment returned invalid start position!");
      }
    }
  }

  /**
   * Test of getSegmentStop method, of class MediaSegment.
   */
  @Test
  public void testGetSegmentStop() {
    assertNotNull(segments);
    for (MediaSegment segment : segments) {
      assertNotNull(segment);
      if (segment.getSegmentStop() != 10
              || segment.getSegmentStop() != 50
              || segment.getSegmentStop() != 120) {
        fail("MediaSegment returned invalid stop position!");
      }
    }
  }
}
