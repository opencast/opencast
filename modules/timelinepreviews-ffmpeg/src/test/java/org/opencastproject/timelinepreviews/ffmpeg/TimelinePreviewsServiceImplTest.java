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
package org.opencastproject.timelinepreviews.ffmpeg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Test class for TimelinePreviewsServiceImpl.
 */
public class TimelinePreviewsServiceImplTest {
  /** Video file to test the optimization */
  protected static final String mediaResource = "/test-optimization.mp4";

  /** Duration of whole movie */
  protected static final long mediaDuration = 30000L;

  /** The media url */
  protected static TrackImpl track = null;

  @BeforeClass
  public static void setUpClass() throws Exception {
    track = TrackImpl.fromURI(TimelinePreviewsServiceImplTest.class.getResource(mediaResource).toURI());
    track.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    track.setMimeType(MimeTypes.MJPEG);
    track.addStream(new VideoStreamImpl());
    track.setDuration(mediaDuration);
    track.setIdentifier(IdBuilderFactory.newInstance().newIdBuilder().createNew().compact());
  }

  /**
   * Test of updated method of class TimelinePreviewsServiceImpl.
   * @throws java.lang.Exception
   */
  @Test
  public void testUpdated() throws Exception {
    Dictionary<String, String> properties = new Hashtable<>();
    properties.put(TimelinePreviewsServiceImpl.OPT_RESOLUTION_X, "200");
    properties.put(TimelinePreviewsServiceImpl.OPT_RESOLUTION_Y, "90");
    properties.put(TimelinePreviewsServiceImpl.OPT_OUTPUT_FORMAT, ".jpg");
    properties.put(TimelinePreviewsServiceImpl.OPT_MIMETYPE, "image/jpg");

    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getHostRegistrations()).andReturn(new ArrayList());
    EasyMock.replay(serviceRegistry);

    TimelinePreviewsServiceImpl instance = new TimelinePreviewsServiceImpl();
    instance.setServiceRegistry(serviceRegistry);
    try {
      instance.updated(properties);
      // we cannot check private fields but it should not throw any exception
      assertEquals(200, instance.resolutionX);
      assertEquals(90, instance.resolutionY);
      assertEquals(".jpg", instance.outputFormat);
      assertEquals("image/jpg", instance.mimetype);
    } catch (Exception e) {
      fail("updated method should not throw any exceptions but has thrown: " + ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * Test of createTimelinePreviewImages method of class TimelinePreviewsServiceImpl.
   * @throws java.lang.Exception
   */
  @Test
  public void testCreateTimelinePreviewImages() throws Exception {
    Job expectedJob = new JobImpl(1);
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.createJob(
            EasyMock.eq(TimelinePreviewsServiceImpl.JOB_TYPE),
            EasyMock.eq(TimelinePreviewsServiceImpl.Operation.TimelinePreview.toString()),
            (List<String>) EasyMock.anyObject(), EasyMock.anyFloat()))
            .andReturn(expectedJob);
    EasyMock.replay(serviceRegistry);

    TimelinePreviewsServiceImpl instance = new TimelinePreviewsServiceImpl();
    instance.setServiceRegistry(serviceRegistry);
    Job job = instance.createTimelinePreviewImages(track, 10);
    assertEquals(expectedJob, job);
  }

  /**
   * Test of process method of class TimelinePreviewsServiceImpl.
   * @throws java.lang.Exception
   */
  @Test
  public void testProcess() throws Exception {
    File file = new File(track.getURI());
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject()))
            .andReturn(file);
    Capture<String> filenameCapture = Capture.newInstance();
    EasyMock.expect(workspace.putInCollection(
            EasyMock.anyString(), EasyMock.capture(filenameCapture), EasyMock.anyObject()))
            .andReturn(new URI("timelinepreviews.png"));
    EasyMock.replay(workspace);

    TimelinePreviewsServiceImpl instance = new TimelinePreviewsServiceImpl();
    instance.setWorkspace(workspace);

    String trackXml = MediaPackageElementParser.getAsXml(track);
    Job job = new JobImpl(1);
    job.setJobType(TimelinePreviewsServiceImpl.JOB_TYPE);
    job.setOperation(TimelinePreviewsServiceImpl.Operation.TimelinePreview.toString());
    job.setArguments(Arrays.asList(trackXml, String.valueOf(10)));
    String result = instance.process(job);
    assertNotNull(result);

    MediaPackageElement timelinepreviewsAttachment = MediaPackageElementParser.getFromXml(result);
    assertEquals(new URI("timelinepreviews.png"), timelinepreviewsAttachment.getURI());
    assertTrue(filenameCapture.hasCaptured());
  }
}
