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

package org.opencastproject.videosegmenter.ffmpeg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.VideoStreamImpl;
import org.opencastproject.metadata.mpeg7.MediaLocator;
import org.opencastproject.metadata.mpeg7.MediaLocatorImpl;
import org.opencastproject.metadata.mpeg7.MediaRelTimeImpl;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.MultimediaContentType;
import org.opencastproject.metadata.mpeg7.Segment;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Test class for video segmentation.
 */
public class VideoSegmenterTest {

  /** Video file to test. Contains a new scene at 00:12 */
  protected static final String mediaResource = "/scene-change.mov";

  /** Video file to test the optimization */
  protected static final String mediaResource1 = "/test-optimization.mp4";

  /** Duration of whole movie */
  protected static final long mediaDuration = 20000L;
  protected static final long mediaDuration1 = 30000L;

  /** Duration of the first segment */
  protected static final long firstSegmentDuration = 12000L;

  /** Duration of the seconds segment */
  protected static final long secondSegmentDuration = mediaDuration - firstSegmentDuration;

  /** The in-memory service registration */
  protected ServiceRegistry serviceRegistry = null;
  protected ServiceRegistry serviceRegistry1 = null;

  /** The video segmenter */
  protected VideoSegmenterServiceImpl vsegmenter = null;
  protected VideoSegmenterServiceImpl vsegmenter1 = null;

  protected Mpeg7CatalogService mpeg7Service = null;
  protected Mpeg7CatalogService mpeg7Service1 = null;

  /** The media url */
  protected static TrackImpl track = null;
  protected static TrackImpl track1 = null;

  /** Temp file */
  protected File tempFile = null;
  protected File tempFile1 = null;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  /**
   * Copies test files to the local file system, since jmf is not able to access movies from the resource section of a
   * bundle.
   *
   * @throws Exception
   *           if setup fails
   */
  @BeforeClass
  public static void setUpClass() throws Exception {
    track = TrackImpl.fromURI(VideoSegmenterTest.class.getResource(mediaResource).toURI());
    track.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    track.setMimeType(MimeTypes.MJPEG);
    track.addStream(new VideoStreamImpl());
    track.setDuration(new Long(20000));

    track1 = TrackImpl.fromURI(VideoSegmenterTest.class.getResource(mediaResource1).toURI());
    track1.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    track1.setMimeType(MimeTypes.MJPEG);
    track1.addStream(new VideoStreamImpl());
    track1.setDuration(mediaDuration1);
  }

  /**
   * Setup for the video segmenter service, including creation of a mock workspace.
   *
   * @throws Exception
   *           if setup fails
   */
  @Before
  public void setUp() throws Exception {
    mpeg7Service = new Mpeg7CatalogService();
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(new File(track.getURI()));
    tempFile = testFolder.newFile(getClass().getName() + ".xml");
    EasyMock.expect(
        workspace.putInCollection(
            (String) EasyMock.anyObject(),
            (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())
    ).andAnswer(new IAnswer<URI>() {
      @Override
      public URI answer() throws Throwable {
        InputStream in = (InputStream) EasyMock.getCurrentArguments()[2];
        IOUtils.copy(in, new FileOutputStream(tempFile));
        return tempFile.toURI();
      }
    });
    EasyMock.replay(workspace);

    mpeg7Service1 = new Mpeg7CatalogService();
    Workspace workspace1 = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace1.get((URI) EasyMock.anyObject())).andReturn(new File(track1.getURI()));
    tempFile1 = testFolder.newFile(getClass().getName() + "-1.xml");
    EasyMock.expect(
        workspace1.putInCollection(
            (String) EasyMock.anyObject(),
            (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject())
    ).andAnswer(new IAnswer<URI>() {
      @Override
      public URI answer() throws Throwable {
        InputStream in = (InputStream) EasyMock.getCurrentArguments()[2];
        IOUtils.copy(in, new FileOutputStream(tempFile1));
        return tempFile1.toURI();
      }
    });
    EasyMock.replay(workspace1);

    User anonymous = new JaxbUser("anonymous", "test", new DefaultOrganization(), new JaxbRole(
            DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, new DefaultOrganization()));
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);

    vsegmenter = new VideoSegmenterServiceImpl();
    serviceRegistry = new ServiceRegistryInMemoryImpl(vsegmenter, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));
    vsegmenter.setServiceRegistry(serviceRegistry);
    vsegmenter.setMpeg7CatalogService(mpeg7Service);
    vsegmenter.setWorkspace(workspace);
    vsegmenter.setSecurityService(securityService);
    vsegmenter.setUserDirectoryService(userDirectoryService);
    vsegmenter.setOrganizationDirectoryService(organizationDirectoryService);

    vsegmenter1 = new VideoSegmenterServiceImpl();
    serviceRegistry1 = new ServiceRegistryInMemoryImpl(vsegmenter1, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));
    vsegmenter1.setServiceRegistry(serviceRegistry1);
    vsegmenter1.setMpeg7CatalogService(mpeg7Service1);
    vsegmenter1.setWorkspace(workspace1);
    vsegmenter1.setSecurityService(securityService);
    vsegmenter1.setUserDirectoryService(userDirectoryService);
    vsegmenter1.setOrganizationDirectoryService(organizationDirectoryService);

    // set parameters for segmentation because the default parameters are not suitable for too short videos
    vsegmenter.prefNumber = 2;
    vsegmenter.stabilityThreshold = 2;
    vsegmenter.absoluteMin = 1;

    vsegmenter1.stabilityThreshold = 2;
    vsegmenter1.changesThreshold = 0.025f;
    vsegmenter1.prefNumber = 5;
    vsegmenter1.maxCycles = 5;
    vsegmenter1.maxError = 0.2f;
    vsegmenter1.absoluteMin = 1;
  }

  /**
   * @throws java.io.File.IOException
   */
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(tempFile);
    FileUtils.deleteQuietly(tempFile1);
    ((ServiceRegistryInMemoryImpl) serviceRegistry).dispose();
    ((ServiceRegistryInMemoryImpl) serviceRegistry1).dispose();
  }

  @Test
  public void testAnalyze() throws Exception {
    Job receipt = vsegmenter.segment(track);
    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 1000, receipt);
    jobBarrier.waitForJobs();

    Catalog catalog = (Catalog) MediaPackageElementParser.getFromXml(receipt.getPayload());

    Mpeg7Catalog mpeg7 = new Mpeg7CatalogImpl(catalog.getURI().toURL().openStream());

    // Is there multimedia content in the mpeg7?
    assertTrue("Audiovisual content was expected", mpeg7.hasVideoContent());

    MultimediaContentType contentType = mpeg7.multimediaContent().next().elements().next();

    // Is there at least one segment?
    TemporalDecomposition<? extends Segment> segments = contentType.getTemporalDecomposition();
    Iterator<? extends Segment> si = segments.segments();
    assertTrue(si.hasNext());
    Segment firstSegment = si.next();
    MediaTime firstSegmentMediaTime = firstSegment.getMediaTime();
    long startTime = firstSegmentMediaTime.getMediaTimePoint().getTimeInMilliseconds();
    long duration = firstSegmentMediaTime.getMediaDuration().getDurationInMilliseconds();
    assertEquals("Unexpected start time of first segment", 0, startTime);
    assertEquals("Unexpected duration of first segment", firstSegmentDuration, duration);

    // What about the second one?
    assertTrue("Video is expected to have more than one segment", si.hasNext());

    Segment secondSegment = si.next();
    MediaTime secondSegmentMediaTime = secondSegment.getMediaTime();
    startTime = secondSegmentMediaTime.getMediaTimePoint().getTimeInMilliseconds();
    duration = secondSegmentMediaTime.getMediaDuration().getDurationInMilliseconds();
    assertEquals("Unexpected start time of second segment", firstSegmentDuration, startTime);
    assertEquals("Unexpected duration of second segment", secondSegmentDuration, duration);

    // There should be no third segment
    assertFalse("Found an unexpected third video segment", si.hasNext());
  }

  @Test
  public void testAnalyzeOptimization() throws Exception {
    Job receipt = vsegmenter1.segment(track1);
    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry1, 1000, receipt);
    jobBarrier.waitForJobs();

    Catalog catalog = (Catalog) MediaPackageElementParser.getFromXml(receipt.getPayload());

    Mpeg7Catalog mpeg7 = new Mpeg7CatalogImpl(catalog.getURI().toURL().openStream());

    // Is there multimedia content in the mpeg7?
    assertTrue("Audiovisual content was expected", mpeg7.hasVideoContent());
    assertNotNull("Audiovisual content expected", mpeg7.multimediaContent().next().elements().hasNext());

    MultimediaContentType contentType = mpeg7.multimediaContent().next().elements().next();

    // Is there at least one segment?
    TemporalDecomposition<? extends Segment> segments = contentType.getTemporalDecomposition();
    Iterator<? extends Segment> si = segments.segments();
    assertTrue(si.hasNext());

    // Is the error of optimization small enough?
    int segmentCounter = 0;
    for ( ; si.hasNext(); ++segmentCounter) {
      si.next();
    }
    float error = Math.abs((segmentCounter - vsegmenter1.prefNumber) / (float)vsegmenter1.prefNumber);
    assertTrue("Error of Optimization is too big", error <= vsegmenter1.maxError);
  }

  @Test
  public void testAnalyzeOptimizedList() throws Exception {
    Job receipt = vsegmenter.segment(track);
    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 1000, receipt);
    jobBarrier.waitForJobs();

    Catalog catalog = (Catalog) MediaPackageElementParser.getFromXml(receipt.getPayload());
    Mpeg7Catalog mpeg7 = new Mpeg7CatalogImpl(catalog.getURI().toURL().openStream());

    List<OptimizationStep> optimizedList = new LinkedList<OptimizationStep>();
    OptimizationStep firstStep  = new OptimizationStep(0.015f, 46, 41, mpeg7, null);
    OptimizationStep secondStep = new OptimizationStep(0.167f, 34, 41, mpeg7, null);
    OptimizationStep thirdStep  = new OptimizationStep(0.011f, 44, 41, mpeg7, null);
    OptimizationStep fourthStep = new OptimizationStep(0.200f, 23, 41, mpeg7, null);

    float error1 = (46 - 41) / (float)41; // ~  0.122
    float error2 = (34 - 41) / (float)41; // ~ -0.171
    float error3 = (44 - 41) / (float)41; // ~  0.073
    float error4 = (23 - 41) / (float)41; // ~ -0.439

    optimizedList.add(firstStep);
    optimizedList.add(secondStep);
    optimizedList.add(thirdStep);
    optimizedList.add(fourthStep);
    Collections.sort(optimizedList);

    // check if the errors were calculated correctly and  whether the elements are in the correct order
    assertEquals("first element of optimized list incorrect",  error3, optimizedList.get(0).getError(), 0.0001f);
    assertEquals("second element of optimized list incorrect", error1, optimizedList.get(1).getError(), 0.0001f);
    assertEquals("third element of optimized list incorrect",  error4, optimizedList.get(2).getError(), 0.0001f);
    assertEquals("fourth element of optimized list incorrect", error2, optimizedList.get(3).getError(), 0.0001f);
    assertTrue("first error in optimized list is not positive", optimizedList.get(0).getError() >= 0);
    assertTrue("second error in optimized list is not bigger than first",
            optimizedList.get(1).getError() > optimizedList.get(0).getError());
    assertTrue("third error in optimized list is not negative", optimizedList.get(2).getError() < 0);
    assertTrue("fourth error in optimized list is smaller than third",
            optimizedList.get(3).getError() > optimizedList.get(2).getError());
  }

  @Test
  public void testAnalyzeSegmentMerging() {
    Mpeg7CatalogService mpeg7catalogService = vsegmenter.mpeg7CatalogService;
    MediaTime contentTime = new MediaRelTimeImpl(0, track.getDuration());
    MediaLocator contentLocator = new MediaLocatorImpl(track.getURI());
    Mpeg7Catalog mpeg7 = mpeg7catalogService.newInstance();
    Video videoContent = mpeg7.addVideoContent("videosegment", contentTime, contentLocator);
    LinkedList<Segment> segments;
    LinkedList<Segment> result;
    int segmentcount = 1;
    track.setDuration(47000L);

    // list of segment durations (starttimes can be calculated from those)
    int[] segmentArray1 = {3000, 2000, 8000, 3000, 1000, 6000, 3000, 2000, 4000, 11000, 2000, 2000};
    int[] segmentArray2 = {1000, 2000, 8000, 3000, 1000, 6000, 3000, 2000, 4000, 11000, 2000, 4000};
    int[] segmentArray3 = {1000, 2000, 4000, 3000, 1000, 2000, 3000, 2000, 4000, 1000, 2000, 4000};
    int[] segmentArray4 = {6000, 7000, 13000, 9000, 8000, 11000, 5000, 16000};

    // predicted outcome of filtering the segmentation
    int[] prediction1 = {5000, 10000, 8000, 9000, 15000};
    int[] prediction2 = {13000, 8000, 9000, 11000, 6000};
    int[] prediction3 = {29000};
    int[] prediction4 = {6000, 7000, 13000, 9000, 8000, 11000, 5000, 16000};

    // total duration of respective segment arrays
    long duration1 = 47000L;
    long duration2 = 47000L;
    long duration3 = 29000L;
    long duration4 = 75000L;

    int[][] segmentArray = {segmentArray1, segmentArray2, segmentArray3, segmentArray4};
    int[][] prediction = {prediction1, prediction2, prediction3, prediction4};
    long[] durations = {duration1, duration2, duration3, duration4};

    // check for all test segmentations if "filterSegmentation" yields the expected result
    for (int k = 0; k < segmentArray.length; k++) {

      segments = new LinkedList<Segment>();
      result = new LinkedList<Segment>();
      track.setDuration(durations[k]);
      int previous = 0;

      for (int i = 0; i < segmentArray[k].length; i++) {
        Segment s = videoContent.getTemporalDecomposition().createSegment("segment-" + segmentcount++);
        s.setMediaTime(new MediaRelTimeImpl(previous, segmentArray[k][i]));
        segments.add(s);

        previous += segmentArray[k][i];
      }

      vsegmenter.filterSegmentation(segments, track, result, 5000);

      assertEquals("segment merging yields wrong number of segments", prediction[k].length, result.size());

      previous = 0;
      for (int i = 0; i < prediction[k].length; i++) {
        String message = "segment " + i + " in set " + k + " has the wrong start time.";
        String message1 = "segment " + i + " in set " + k + " has the wrong duration.";
        assertEquals(message, previous, result.get(i).getMediaTime().getMediaTimePoint().getTimeInMilliseconds());
        assertEquals(message1, prediction[k][i], result.get(i).getMediaTime().getMediaDuration()
            .getDurationInMilliseconds());
        previous += prediction[k][i];
      }
    }

  }

}
