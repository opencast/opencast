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
package org.opencastproject.workflow.handler.composer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.limit;
import static org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.toSeconds;
import static org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.validateTargetBaseNameFormat;

import org.opencastproject.mediapackage.Track;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.Cfg;
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.MediaPosition;
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.MediaPositionParser;
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.PositionType;

import com.entwinemedia.fn.parser.Result;

import org.easymock.EasyMock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImageWorkflowOperationHandlerTest {

  @Test
  public void testMediaPositionParserSuccess() {
    test("75900000000001", sec(75900000000001d));
    test("10, 20.3", sec(10), sec(20.3));
    test("10 20.3", sec(10), sec(20.3));
    test("1020.3", sec(1020.3));
    test("10 , 20.3", sec(10), sec(20.3));
    test("10    20.3", sec(10), sec(20.3));
    test("15", sec(15));
    test("11%", percent(11));
    test(" 19.70% , 20.3", percent(19.7), sec(20.3));
    test(" 10.62% 20.3 14.2     ", percent(10.62), sec(20.3), sec(14.2));
    test("10.62% 20.3 ,14.213%", percent(10.62), sec(20.3), percent(14.213));
  }

  @Test
  public void testMediaPositionParserFailure() {
    assertFalse(testSuccess("  ") || testSuccess(" ") || testSuccess(",, ,") || testSuccess("")
            || testSuccess("10a,10b"));
  }

  @Test
  public void testFileNameGeneration() {
    final ImageWorkflowOperationHandler dummy = new ImageWorkflowOperationHandler();
    assertEquals("thumbnail_12.5p_small.jpg",
        dummy.createFileName("_small.jpg", uri("http://localhost/path/filename.mp4"),
            new MediaPosition(PositionType.Percentage, 12.5), cfg(Optional.empty(), Optional.of("thumbnail_%.1fp%s")))
    );
    assertEquals("thumbnail_0p.jpg",
        dummy.createFileName(".jpg", uri("http://localhost/path/filename.mp4"),
            new MediaPosition(PositionType.Percentage, 0), cfg(Optional.empty(), Optional.of("thumbnail_%.0fp%s")))
    );
    assertEquals("video_14.200s.jpg",
        dummy.createFileName(".jpg", uri("http://localhost/path/video.mp4"),
            new MediaPosition(PositionType.Seconds, 14.2), cfg(Optional.empty(), Optional.empty()))
    );
    assertEquals("video_15.110s_medium.jpg",
        dummy.createFileName("_medium.jpg", uri("http://localhost/path/video.mp4"),
            new MediaPosition(PositionType.Seconds, 15.1099), cfg(Optional.empty(), Optional.empty()))
    );
    assertEquals("thumbnail_15.110s_large.jpg",
        dummy.createFileName("_large.jpg", uri("http://localhost/path/video.mp4"),
            new MediaPosition(PositionType.Seconds, 15.1099), cfg(Optional.of("thumbnail_%.3fs%s"), Optional.empty()))
    );
    assertEquals("thumbnail",
        dummy.createFileName("_large.jpg", uri("http://localhost/path/video.mp4"),
            new MediaPosition(PositionType.Seconds, 15.1099), cfg(Optional.of("thumbnail"), Optional.empty()))
    );
    assertEquals("thumbnail_large.jpg",
        dummy.createFileName("_large.jpg", uri("http://localhost/path/video.mp4"),
            new MediaPosition(PositionType.Seconds, 15.1099), cfg(Optional.of("thumbnail%2$s"), Optional.empty()))
    );
  }

  @Test
  public void testValidateTargetBaseNameFormat() {
    validateTargetBaseNameFormat("format-a").apply("thumbnail_%.1fs%s");
    validateTargetBaseNameFormat("format-a").apply("thumbnail_%1$.1fs%2$s");
    validateTargetBaseNameFormat("format-a").apply("%2$s_thumbnail_%1$.1fs%1$.1fs%2$s");
    try {
      validateTargetBaseNameFormat("format-a").apply("thumbnail_%.1fs%.3f");
      fail("Invalid format passed check. Suffix format %s is missing.");
    } catch (Exception ignore) {
    }
    try {
      validateTargetBaseNameFormat("format-a").apply("thumbnail_%.3f");
      fail("Invalid format passed check. Suffix format %s is missing.");
    } catch (Exception ignore) {
    }
    try {
      validateTargetBaseNameFormat("format-a").apply("thumbnail_%s");
      fail("Invalid format passed check. Suffix is missing since %s does not have a positional parameter.");
    } catch (Exception ignore) {
    }
    // omitting the position should pass
    validateTargetBaseNameFormat("format-a").apply("thumbnail_%2$s");
  }

  @Test
  public void testLimit() {
    assertTrue(limit(track(1511), List.of(sec(-1), sec(1511), sec(1512))).isEmpty());
    assertTrue(limit(track(1511), List.of(percent(-0.2), percent(101))).isEmpty());
    assertEquals(4, limit(track(1511), List.of(percent(0), percent(100), sec(0), sec(1510))).size());
    assertEquals(4, limit(track(1511), List.of(percent(0), percent(10), sec(10), sec(1500), percent(200))).size());
  }

  @Test
  public void testToSeconds() {
    assertEquals(0.985, toSeconds(track(1970), percent(50), 0), 0);
    assertEquals(0, toSeconds(track(1970), percent(0), 0), 0);
    assertEquals(1.87, toSeconds(track(1970), percent(100), 100), 0);
    assertEquals(0, toSeconds(track(100), percent(100), 200), 0);
    assertEquals(1.77, toSeconds(track(1970), sec(1.970), 200), 0);
    assertEquals(1.77, toSeconds(track(1970), sec(1.870), 200), 0);
    assertEquals(1.77, toSeconds(track(1970), sec(1.770), 200), 0);
    assertEquals(1.769, toSeconds(track(1970), sec(1.769), 200), 0);
  }

  // ** ** **

  private Cfg cfg(Optional<String> targetBaseNamePatternSecond, Optional<String> targetBaseNamePatternPercent) {
    return new Cfg(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
            new ArrayList<>(), new ConfiguredTagsAndFlavors.TargetTags(), targetBaseNamePatternSecond,
            targetBaseNamePatternPercent, 0);
  }

  private MediaPosition sec(double a) {
    return new MediaPosition(PositionType.Seconds, a);
  }

  private MediaPosition percent(double a) {
    return new MediaPosition(PositionType.Percentage, a);
  }

  private Track track(long duration) {
    final Track t = EasyMock.createNiceMock(Track.class);
    EasyMock.expect(t.getDuration()).andReturn(duration).anyTimes();
    EasyMock.replay(t);
    return t;
  }

  private void test(String expr, MediaPosition... expected) {
    final Result<List<MediaPosition>> r = MediaPositionParser.positions.parse(expr);
    assertTrue(r.isDefined());
    assertTrue("Rest:\"" + r.getRest() + "\"", r.getRest().isEmpty());
    assertEquals(List.of(expected), r.getResult());
  }

  private boolean testSuccess(String expr) {
    final Result<List<MediaPosition>> r = MediaPositionParser.positions.parse(expr);
    return r.isDefined() && r.getRest().isEmpty();
  }
}
