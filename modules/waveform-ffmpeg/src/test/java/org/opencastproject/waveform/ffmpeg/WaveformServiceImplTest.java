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
package org.opencastproject.waveform.ffmpeg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.IoSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Test class for WaveformServiceImpl.
 */
public class WaveformServiceImplTest {
  private static final Logger logger = LoggerFactory.getLogger(WaveformServiceImplTest.class);

  private static Track audioTrack = null;
  private static Track dummyTrack = null;

  @BeforeClass
  public static void setUpClass() throws Exception {
    audioTrack = readTrackFromResource("/audio-track.xml");
    audioTrack.setURI(WaveformServiceImplTest.class.getResource("/test.mp3").toURI());
    dummyTrack = readTrackFromResource("/dummy-track.xml");
  }

  private static Track readTrackFromResource(String resourceName) throws IOException, MediaPackageException {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(
              WaveformServiceImplTest.class.getResourceAsStream(resourceName)));
      String line = reader.readLine();
      StringBuilder trackBuilder = new StringBuilder();
      while (line != null) {
        trackBuilder.append(line);
        line = reader.readLine();
      }

      return (Track) MediaPackageElementParser.getFromXml(trackBuilder.toString());

    } finally {
      IoSupport.closeQuietly(reader);
    }
  }

  /**
   * Test of updated method of class WaveformServiceImpl.
   */
  @Test
  public void testUpdated() throws Exception {
    Dictionary<String, String> properties = new Hashtable<>();
    properties.put(WaveformServiceImpl.WAVEFORM_COLOR_CONFIG_KEY, "blue green 0x2A2A2A 323232CC");
    properties.put(WaveformServiceImpl.WAVEFORM_SPLIT_CHANNELS_CONFIG_KEY, "false");
    properties.put(WaveformServiceImpl.WAVEFORM_SCALE_CONFIG_KEY, "lin");

    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getHostRegistrations()).andReturn(new ArrayList());
    EasyMock.replay(serviceRegistry);

    WaveformServiceImpl instance = new WaveformServiceImpl();
    instance.setServiceRegistry(serviceRegistry);
    try {
      instance.updated(properties);
      // we can not check private fields but it should not throw any exception
    } catch (Exception e) {
      fail("updated method should not throw any exceptions but has thrown: " + ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * Test of createWaveformImage method of class WaveformServiceImpl.
   */
  @Test
  public void testGenerateWaveformImage() throws Exception {
    Job expectedJob = new JobImpl(1);
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.createJob(
            EasyMock.eq(WaveformServiceImpl.JOB_TYPE),
            EasyMock.eq(WaveformServiceImpl.Operation.Waveform.toString()),
            (List<String>) EasyMock.anyObject(), EasyMock.anyFloat()))
            .andReturn(expectedJob);
    EasyMock.replay(serviceRegistry);

    WaveformServiceImpl instance = new WaveformServiceImpl();
    instance.setServiceRegistry(serviceRegistry);
    Job job = instance.createWaveformImage(dummyTrack, 200, 5000, 20000, 500, "black");
    assertEquals(expectedJob, job);
  }

  /**
   * Test of process method of class WaveformServiceImpl.
   */
  @Test
  public void testProcess() throws Exception {
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject()))
            .andReturn(new File(audioTrack.getURI()));
    Capture<String> filenameCapture = Capture.newInstance();
    EasyMock.expect(workspace.putInCollection(
            EasyMock.anyString(), EasyMock.capture(filenameCapture), EasyMock.anyObject()))
            .andReturn(new URI("waveform.png"));
    EasyMock.replay(workspace);

    WaveformServiceImpl instance = new WaveformServiceImpl();
    instance.setWorkspace(workspace);

    String audioTrackXml = MediaPackageElementParser.getAsXml(audioTrack);
    Job job = new JobImpl(1);
    job.setJobType(WaveformServiceImpl.JOB_TYPE);
    job.setOperation(WaveformServiceImpl.Operation.Waveform.toString());
    job.setArguments(Arrays.asList(audioTrackXml, "200", "5000", "20000", "500", "black"));
    String result = instance.process(job);
    assertNotNull(result);

    MediaPackageElement waveformAttachment = MediaPackageElementParser.getFromXml(result);
    assertEquals(new URI("waveform.png"), waveformAttachment.getURI());
    assertTrue(filenameCapture.hasCaptured());
  }
}
