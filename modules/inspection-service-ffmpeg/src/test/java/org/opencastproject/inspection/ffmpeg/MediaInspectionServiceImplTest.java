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

package org.opencastproject.inspection.ffmpeg;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.MimeType.mimeType;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.inspection.api.util.Options;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class MediaInspectionServiceImplTest {
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceImplTest.class);

  /** True to run the tests */
  private static Option<String> ffprobePath;

  @BeforeClass
  public static void setupClass() {
    try {
      Process p = new ProcessBuilder(FFmpegAnalyzer.FFPROBE_BINARY_DEFAULT, "-version").start();
      if (p.waitFor() != 0)
        throw new IllegalStateException();
      ffprobePath = some(FFmpegAnalyzer.FFPROBE_BINARY_DEFAULT);
    } catch (Throwable t) {
      logger.warn("Skipping composer tests due to missing ffprobe binary");
      ffprobePath = none();
    }
  }

  /** Setup test. */
  @Ignore
  private Option<MediaInspector> init(URI resource) throws Exception {
    for (String binary : ffprobePath) {
      final File f = new File(resource);
      Workspace workspace = EasyMock.createNiceMock(Workspace.class);
      EasyMock.expect(workspace.get(resource)).andReturn(f);
      EasyMock.expect(workspace.get(resource)).andReturn(f);
      EasyMock.expect(workspace.get(resource)).andReturn(f);
      EasyMock.replay(workspace);
      return some(new MediaInspector(workspace, binary));
    }
    return none();
  }

  private URI getResource(String resource) {
    try {
      return MediaInspectionServiceImpl.class.getResource(resource).toURI();
    } catch (URISyntaxException e) {
      return chuck(e);
    }
  }

  @Test
  public void testInspection() throws Exception {
    final URI trackUri = getResource("/test.mp4");
    for (MediaInspector mi : init(trackUri)) {
      Track track = mi.inspectTrack(trackUri, Options.NO_OPTION);
      // test the returned values
      Checksum cs = Checksum.create(ChecksumType.fromString("md5"), "cc72b7a4f1a68b84fba6f0fb895da395");
      assertEquals(cs, track.getChecksum());
      assertEquals("video", track.getMimeType().getType());
      assertEquals("mp4", track.getMimeType().getSubtype());
      assertNotNull(track.getDuration());
      assertTrue(track.getDuration() > 0);
    }
  }

  @Test
  public void testInspectionEmptyContainer() throws Exception {
    final URI trackUri = getResource("/nostreams.mp4");
    for (MediaInspector mi : init(trackUri)) {
      final Track track = mi.inspectTrack(trackUri, Options.NO_OPTION);
      assertEquals(0, track.getStreams().length);
      assertEquals("mp4", track.getMimeType().getSubtype());
      assertEquals(null, track.getDuration());
    }
  }

  @Test
  public void testEnrichment() throws Exception {
    final URI trackUri = getResource("/test.mp4");
    for (MediaInspector mi : init(trackUri)) {
      Track track = mi.inspectTrack(trackUri, Options.NO_OPTION);
      // make changes to metadata
      Checksum cs = track.getChecksum();
      track.setChecksum(null);
      MimeType mt = mimeType("video", "flash");
      track.setMimeType(mt);
      // test the enrich scenario
      Track newTrack = (Track) mi.enrich(track, false, Options.NO_OPTION);

      VideoStream[] videoStreams = TrackSupport.byType(newTrack.getStreams(), VideoStream.class);
      assertTrue(videoStreams[0].getFrameCount() > 0);
      AudioStream[] audioStreams = TrackSupport.byType(newTrack.getStreams(), AudioStream.class);
      assertTrue(audioStreams[0].getFrameCount() > 0);
      assertEquals(newTrack.getChecksum(), cs);
      assertEquals(newTrack.getMimeType(), mt);
      assertNotNull(newTrack.getDuration());
      assertTrue(newTrack.getDuration() > 0);
      // test the override scenario
      newTrack = (Track) mi.enrich(track, true, Options.NO_OPTION);
      assertEquals(newTrack.getChecksum(), cs);
      assertNotSame(newTrack.getMimeType(), mt);
      assertTrue(newTrack.getDuration() > 0);
    }

    for (MediaInspector mi : init(trackUri)) {
      Track track = mi.inspectTrack(trackUri, Options.NO_OPTION);
      // make changes to metadata
      Checksum cs = track.getChecksum();
      track.setChecksum(null);
      MimeType mt = mimeType("video", "flash");
      track.setMimeType(mt);
      // test the enrich scenario
      Track newTrack = (Track) mi.enrich(track, false, Options.NO_OPTION);

      VideoStream[] videoStreams = TrackSupport.byType(newTrack.getStreams(), VideoStream.class);
      assertTrue(videoStreams[0].getFrameCount() > 0);
      AudioStream[] audioStreams = TrackSupport.byType(newTrack.getStreams(), AudioStream.class);
      assertTrue(audioStreams[0].getFrameCount() > 0);
      assertEquals(newTrack.getChecksum(), cs);
      assertEquals(newTrack.getMimeType(), mt);
      assertNotNull(newTrack.getDuration());
      assertTrue(newTrack.getDuration() > 0);
      // test the override scenario
      newTrack = (Track) mi.enrich(track, true, Options.NO_OPTION);
      assertEquals(newTrack.getChecksum(), cs);
      assertNotSame(newTrack.getMimeType(), mt);
      assertTrue(newTrack.getDuration() > 0);
    }
  }

  @Test
  public void testEnrichmentEmptyContainer() throws Exception {
    final URI trackUri = getResource("/nostreams.mp4");
    for (MediaInspector mi : init(trackUri)) {
      Track track = mi.inspectTrack(trackUri, Options.NO_OPTION);
      // make changes to metadata
      Checksum cs = track.getChecksum();
      track.setChecksum(null);
      MimeType mt = mimeType("video", "flash");
      track.setMimeType(mt);
      // test the enrich scenario
      Track newTrack = (Track) mi.enrich(track, false, Options.NO_OPTION);
      assertEquals(newTrack.getChecksum(), cs);
      assertEquals(newTrack.getMimeType(), mt);
      assertNull(newTrack.getDuration());
      // test the override scenario
      newTrack = (Track) mi.enrich(track, true, Options.NO_OPTION);
      assertEquals(newTrack.getChecksum(), cs);
      assertNotSame(newTrack.getMimeType(), mt);
      assertNull(newTrack.getDuration());
    }
  }

  @Test
  public void testHLSContainer() throws Exception {
    final URI trackUri = getResource("/master.m3u8");
    for (MediaInspector mi : init(trackUri)) {
      Track track = mi.inspectTrack(trackUri, Options.NO_OPTION);
      // test the returned values
      Checksum cs = Checksum.create(ChecksumType.fromString("md5"), "66ed40c8ea9c8419f47a254668540d77");
      assertEquals(cs, track.getChecksum());
      assertEquals("application", track.getMimeType().getType());
      assertEquals("x-mpegURL", track.getMimeType().getSubtype());
      assertNotNull(track.getDuration());
      assertTrue(track.getDuration() > 0);
      // make changes to metadata
    }
  }

}
