/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.inspection.impl;

import org.apache.tika.parser.audio.AudioParser;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.StreamHelper;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.MimeType.mimeType;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;

public class MediaInspectionServiceImplTest {
  private static final Logger logger = LoggerFactory.getLogger(MediaInspectionServiceImplTest.class);

  /** True to run the tests */
  private static Option<String> mediainfoPath;

  @BeforeClass
  public static void setupClass() {
    StreamHelper stdout = null;
    StreamHelper stderr = null;
    StringBuffer errorBuffer = new StringBuffer();
    Process p = null;
    try {
      // Mediainfo requires a track in order to return a status code of 0, indicating that it is working as expected
      URI uriTrack = MediaInspectionServiceImpl.class.getResource("/av.mov").toURI();
      File f = new File(uriTrack);
      p = new ProcessBuilder(MediaInfoAnalyzer.MEDIAINFO_BINARY_DEFAULT, f.getAbsolutePath()).start();
      stdout = new StreamHelper(p.getInputStream());
      stderr = new StreamHelper(p.getErrorStream(), errorBuffer);
      int exitCode = p.waitFor();
      stdout.stopReading();
      stderr.stopReading();
      if (exitCode != 0 && exitCode != 141)
        throw new IllegalStateException("process returned " + exitCode);
      mediainfoPath = some(MediaInfoAnalyzer.MEDIAINFO_BINARY_DEFAULT);
    } catch (Throwable t) {
      logger.warn("Skipping media inspection tests due to unsatisfied mediainfo installation: " + t.getMessage());
      logger.warn(errorBuffer.toString());
      mediainfoPath = none();
    } finally {
      IoSupport.closeQuietly(stdout);
      IoSupport.closeQuietly(stderr);
      IoSupport.closeQuietly(p);
    }
  }

  /** Setup test. */
  @Ignore
  private Option<MediaInspector> init(URI resource) throws Exception {
    for (String binary : mediainfoPath) {
      final File f = new File(resource);
      Workspace workspace = EasyMock.createNiceMock(Workspace.class);
      EasyMock.expect(workspace.get(resource)).andReturn(f);
      EasyMock.expect(workspace.get(resource)).andReturn(f);
      EasyMock.expect(workspace.get(resource)).andReturn(f);
      EasyMock.replay(workspace);
      return some(new MediaInspector(workspace, new AudioParser(), binary));
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
    final URI trackUri = getResource("/av.mov");
    for (MediaInspector mi : init(trackUri)) {
      Track track = mi.inspectTrack(trackUri);
      // test the returned values
      Checksum cs = Checksum.create(ChecksumType.fromString("md5"), "9d3523e464f18ad51f59564acde4b95a");
      assertEquals(cs, track.getChecksum());
      assertEquals("video", track.getMimeType().getType());
      assertEquals("quicktime", track.getMimeType().getSubtype());
      assertNotNull(track.getDuration());
      assertTrue(track.getDuration() > 0);
    }
  }

  @Test
  public void testInspectionEmptyContainer() throws Exception {
    final URI trackUri = getResource("/nostreams.mp4");
    for (MediaInspector mi : init(trackUri)) {
      final Track track = mi.inspectTrack(trackUri);
      assertEquals(0, track.getStreams().length);
      assertEquals("mp4", track.getMimeType().getSubtype());
      assertEquals(null, track.getDuration());
    }
  }

  @Test
  public void testEnrichment() throws Exception {
    final URI trackUri = getResource("/av.mov");
    for (MediaInspector mi : init(trackUri)) {
      Track track = mi.inspectTrack(trackUri);
      // make changes to metadata
      Checksum cs = track.getChecksum();
      track.setChecksum(null);
      MimeType mt = mimeType("video", "flash");
      track.setMimeType(mt);
      // test the enrich scenario
      Track newTrack = (Track) mi.enrich(track, false);
      assertEquals(newTrack.getChecksum(), cs);
      assertEquals(newTrack.getMimeType(), mt);
      assertNotNull(newTrack.getDuration());
      assertTrue(newTrack.getDuration() > 0);
      // test the override scenario
      newTrack = (Track) mi.enrich(track, true);
      assertEquals(newTrack.getChecksum(), cs);
      assertNotSame(newTrack.getMimeType(), mt);
      assertTrue(newTrack.getDuration() > 0);
    }
  }

  @Test
  public void testEnrichmentEmptyContainer() throws Exception {
    final URI trackUri = getResource("/nostreams.mp4");
    for (MediaInspector mi : init(trackUri)) {
      Track track = mi.inspectTrack(trackUri);
      // make changes to metadata
      Checksum cs = track.getChecksum();
      track.setChecksum(null);
      MimeType mt = mimeType("video", "flash");
      track.setMimeType(mt);
      // test the enrich scenario
      Track newTrack = (Track) mi.enrich(track, false);
      assertEquals(newTrack.getChecksum(), cs);
      assertEquals(newTrack.getMimeType(), mt);
      assertNull(newTrack.getDuration());
      // test the override scenario
      newTrack = (Track) mi.enrich(track, true);
      assertEquals(newTrack.getChecksum(), cs);
      assertNotSame(newTrack.getMimeType(), mt);
      assertNull(newTrack.getDuration());
    }
  }
}
