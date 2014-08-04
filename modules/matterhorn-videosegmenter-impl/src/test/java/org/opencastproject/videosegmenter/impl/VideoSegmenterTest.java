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
package org.opencastproject.videosegmenter.impl;

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
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.Mpeg7Catalog;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.MultimediaContentType;
import org.opencastproject.metadata.mpeg7.Segment;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
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
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

/**
 * Test class for video segmentation.
 */
public class VideoSegmenterTest {

  /** Video file to test. Contains a new scene at 00:12 */
  protected static final String mediaResource = "/scene-change.mov";

  /** Duration of whole movie */
  protected static final long mediaDuration = 20000L;

  /** Duration of the first segment */
  protected static final long firstSegmentDuration = 11000L;

  /** Duration of the seconds segment */
  protected static final long secondSegmentDuration = mediaDuration - firstSegmentDuration;

  /** The in-memory service registration */
  protected ServiceRegistry serviceRegistry = null;

  /** The video segmenter */
  protected VideoSegmenterServiceImpl vsegmenter = null;

  protected Mpeg7CatalogService mpeg7Service = null;

  /** The media url */
  protected static TrackImpl track = null;

  /** Temp file */
  protected File tempFile = null;

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
    System.setProperty("java.awt.headless", "true");
    System.setProperty("awt.toolkit", "sun.awt.HeadlessToolkit");
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
    tempFile = File.createTempFile(getClass().getName(), "xml");
    EasyMock.expect(
            workspace.putInCollection((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (InputStream) EasyMock.anyObject())).andAnswer(new IAnswer<URI>() {
      public URI answer() throws Throwable {
        InputStream in = (InputStream) EasyMock.getCurrentArguments()[2];
        IOUtils.copy(in, new FileOutputStream(tempFile));
        return tempFile.toURI();
      }
    });
    EasyMock.replay(workspace);

    User anonymous = new JaxbUser("anonymous", new DefaultOrganization(), new JaxbRole(
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
            organizationDirectoryService);
    vsegmenter.setServiceRegistry(serviceRegistry);
    vsegmenter.setMpeg7CatalogService(mpeg7Service);
    vsegmenter.setWorkspace(workspace);
    vsegmenter.setSecurityService(securityService);
    vsegmenter.setUserDirectoryService(userDirectoryService);
    vsegmenter.setOrganizationDirectoryService(organizationDirectoryService);
  }

  /**
   * @throws java.io.File.IOException
   */
  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(tempFile);
    ((ServiceRegistryInMemoryImpl) serviceRegistry).dispose();
  }

  @Test
  public void testAnalyze() throws Exception {
    Job receipt = vsegmenter.segment(track);
    JobBarrier jobBarrier = new JobBarrier(serviceRegistry, 1000, receipt);
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
    Segment firstSegment = si.next();
    MediaTime firstSegmentMediaTime = firstSegment.getMediaTime();
    long startTime = firstSegmentMediaTime.getMediaTimePoint().getTimeInMilliseconds();
    long duration = firstSegmentMediaTime.getMediaDuration().getDurationInMilliseconds();
    assertEquals("Unexepcted start time of second segment", 0, startTime);
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

}
