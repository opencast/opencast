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

package org.opencastproject.silencedetection.ffmpeg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.opencastproject.mediapackage.Track;
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException;
import org.opencastproject.silencedetection.impl.SilenceDetectionProperties;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class SilenceDetectorTest {
  private static final Logger logger = LoggerFactory.getLogger(SilenceDetectorTest.class);


  /** True to run the tests */
  private static boolean ffmpegInstalled = true;


  @BeforeClass
  public static void setupClass() {
    try {
      Process p = new ProcessBuilder(FFmpegSilenceDetector.FFMPEG_BINARY_DEFAULT, "-version").start();
      if (p.waitFor() != 0) {
        throw new IllegalStateException();
      }
    } catch (Throwable t) {
      logger.warn("Skipping composer tests due to missing ffmpeg");
      ffmpegInstalled = false;
    }
  }

  @Before
  public void setUp() throws Exception {
    // Skip tests if FFmpeg is not installed
    Assume.assumeTrue(ffmpegInstalled);
  }

  /** Setup test. */
  private FFmpegSilenceDetector init(URI resource, Boolean hasAudio, Properties props) throws Exception {
    final File f = new File(resource);
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(resource)).andReturn(f);
    EasyMock.replay(workspace);
    Track track = EasyMock.createNiceMock(Track.class);
    EasyMock.expect(track.getURI()).andReturn(resource);
    EasyMock.expect(track.getIdentifier()).andReturn("123");
    EasyMock.expect(track.getDuration()).andStubReturn(60000L);
    EasyMock.expect(track.hasAudio()).andReturn(hasAudio);
    EasyMock.replay(track);
    return new FFmpegSilenceDetector(props, track, workspace);
  }


  /** Setup test. */
  private FFmpegSilenceDetector init(URI resource, Boolean hasAudio) throws Exception {
    Properties props = new Properties();
    props.setProperty(SilenceDetectionProperties.VOICE_MIN_LENGTH, "4000");
    return init(resource, hasAudio, props);
  }

  private URI getResource(String resource) throws URISyntaxException {
    return FFmpegSilenceDetector.class.getResource(resource).toURI();
  }


  @Test
  public void testSilenceDetection() throws Exception {
    final URI trackUri = getResource("/testspeech.mp4");
    FFmpegSilenceDetector sd = init(trackUri, true);
    assertNotNull(sd.getMediaSegments());
    assertEquals(2, sd.getMediaSegments().getMediaSegments().size());
  }


  @Test
  public void testSilenceDetectionLongVoice() throws Exception {
    final URI trackUri = getResource("/testspeech.mp4");
    Properties props = new Properties();
    // Set minimum voice length to something longer than the actual recording
    props.setProperty(SilenceDetectionProperties.VOICE_MIN_LENGTH, "600000");
    FFmpegSilenceDetector sd = init(trackUri, true, props);
    assertNotNull(sd.getMediaSegments());
    assertEquals(0, sd.getMediaSegments().getMediaSegments().size());
  }


  @Test
  public void testSilenceDetectionOnSilence() throws Exception {
    final URI trackUri = getResource("/silent.mp4");
    FFmpegSilenceDetector sd = init(trackUri, true);
    assertNotNull(sd.getMediaSegments());
    assertEquals(0, sd.getMediaSegments().getMediaSegments().size());
  }


  @Test
  public void testMisconfiguration() throws Exception {
    final URI trackUri = getResource("/nostreams.mp4");
    Properties props = new Properties();
    props.setProperty(SilenceDetectionProperties.SILENCE_PRE_LENGTH, "6000");
    props.setProperty(SilenceDetectionProperties.SILENCE_MIN_LENGTH, "4000");
    try {
      init(trackUri, true, props);
      fail("Silence detection of media without audio should fail");
    } catch (SilenceDetectionFailedException e) {
      // we expect an exception
    }
  }


  @Test
  public void testNoAudio() throws Exception {
    final URI trackUri = getResource("/nostreams.mp4");
    try {
      init(trackUri, false);
      fail("Silence detection of media without audio should fail");
    } catch (SilenceDetectionFailedException e) {
      // we expect an exception
    }
  }

}
