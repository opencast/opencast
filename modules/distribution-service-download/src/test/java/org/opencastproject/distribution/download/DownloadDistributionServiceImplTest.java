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

package org.opencastproject.distribution.download;

import static org.opencastproject.systems.OpencastConstants.DIGEST_USER_PROPERTY;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

public class DownloadDistributionServiceImplTest {

  private final Logger logger = LoggerFactory.getLogger(DownloadDistributionServiceImplTest.class);
  private DownloadDistributionServiceImpl service = null;
  private MediaPackage mp = null;
  private File distributionRoot = null;
  private ServiceRegistry serviceRegistry = null;
  private DefaultOrganization defaultOrganization;

  @Before
  public void setUp() throws Exception {
    final File mediaPackageRoot = new File(getClass().getResource("/mediapackage.xml").toURI()).getParentFile();
    mp = MediaPackageParser.getFromXml(IOUtils.toString(getClass().getResourceAsStream("/mediapackage.xml"), "UTF-8"));

    distributionRoot = new File(mediaPackageRoot, "static");
    service = new DownloadDistributionServiceImpl();

    StatusLine statusLine = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(statusLine.getStatusCode()).andReturn(HttpServletResponse.SC_OK).anyTimes();
    EasyMock.replay(statusLine);

    HttpResponse response = EasyMock.createNiceMock(HttpResponse.class);
    EasyMock.expect(response.getStatusLine()).andReturn(statusLine).anyTimes();
    EasyMock.replay(response);

    final TrustedHttpClient httpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    EasyMock.expect(httpClient.execute((HttpUriRequest) EasyMock.anyObject())).andReturn(response).anyTimes();
    EasyMock.replay(httpClient);

    defaultOrganization = new DefaultOrganization();
    User anonymous = new JaxbUser("anonymous", "test", defaultOrganization,
            new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, defaultOrganization));
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);
    service.setUserDirectoryService(userDirectoryService);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject())).andReturn(organization)
            .anyTimes();
    EasyMock.replay(organizationDirectoryService);
    service.setOrganizationDirectoryService(organizationDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);
    service.setSecurityService(securityService);

    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));
    service.setServiceRegistry(serviceRegistry);
    service.setTrustedHttpClient(httpClient);

    final Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    service.setWorkspace(workspace);

    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        final URI uri = (URI) EasyMock.getCurrentArguments()[0];
        final String[] pathElems = uri.getPath().split("/");
        final String file = pathElems[pathElems.length - 1];
        return new File(mediaPackageRoot, file);
      }
    }).anyTimes();
    final Capture<String> myId1 = Capture.newInstance();
    final Capture<String> myId2 = Capture.newInstance();
    final Capture<String> name = Capture.newInstance();
    final Capture<InputStream> in = Capture.newInstance();
    EasyMock.expect(workspace.put(EasyMock.capture(myId1), EasyMock.capture(myId2), EasyMock.capture(name),
            EasyMock.capture(in))).andAnswer(new IAnswer<URI>() {
              @Override
              public URI answer() throws Throwable {
                File file = new File(distributionRoot,
                        myId1.getValue() + "/" + myId2.getValue() + "/" + name.getValue());
                FileUtils.copyInputStreamToFile(in.getValue(), file);
                return file.toURI();
              }
            }).anyTimes();

    EasyMock.replay(workspace);

    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty("org.opencastproject.download.directory"))
        .andReturn(distributionRoot.toString()).anyTimes();
    EasyMock.expect(bc.getProperty("org.opencastproject.download.url"))
        .andReturn(UrlSupport.DEFAULT_BASE_URL).anyTimes();
    EasyMock.expect(bc.getProperty(DIGEST_USER_PROPERTY))
        .andReturn("opencast_system_account").anyTimes();
    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    Dictionary<String, Object> p = new Hashtable<String, Object>();
    p.put(DistributionService.CONFIG_KEY_STORE_TYPE, "download");
    EasyMock.expect(cc.getProperties()).andReturn(p).anyTimes();
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(bc, cc);
    service.activate(cc);
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(distributionRoot);
    ((ServiceRegistryInMemoryImpl) serviceRegistry).dispose();
  }

  @Test
  public void testPartialDistribution() throws Exception {
    // Distribute only some of the elements in the mediapackage
    Job job1 = service.distribute("engage-player", mp, "track-1");
    Job job2 = service.distribute("engage-player", mp, "catalog-1");
    Job job3 = service.distribute("oai-pmh", mp, "catalog-1");
    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 500, job1, job2, job3);
    jobBarrier.waitForJobs();

    File mpDir = new File(distributionRoot,
            PathSupport.path(defaultOrganization.getId(), "engage-player", mp.getIdentifier().toString()));
    Assert.assertTrue(mpDir.exists());
    File mediaDir = new File(mpDir, "track-1");
    File metadataDir = new File(mpDir, "catalog-1");
    File attachmentsDir = new File(mpDir, "notes");
    Assert.assertTrue(mediaDir.exists());
    Assert.assertTrue(metadataDir.exists());
    Assert.assertFalse(attachmentsDir.exists());
    Assert.assertTrue(new File(mediaDir, "media.mov").exists()); // the filenames are changed to reflect the element ID
    Assert.assertTrue(new File(metadataDir, "dublincore.xml").exists());
    Assert.assertFalse(new File(metadataDir, "mpeg7.xml").exists());
    Assert.assertFalse(new File(attachmentsDir, "attachment.txt").exists());
  }

  @Test
  public void testRetractByOriginal() throws Exception {
    // Distribute the mediapackage and all of its elements
    Job job1 = service.distribute("engage-player", mp, "track-1");
    Job job2 = service.distribute("engage-player", mp, "catalog-1");
    Job job3 = service.distribute("engage-player", mp, "catalog-2");
    Job job4 = service.distribute("engage-player", mp, "notes");
    Job job5 = service.distribute("oai-pmh", mp, "track-1");
    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 500, job1, job2, job3, job4, job5);
    jobBarrier.waitForJobs();

    // Add the new elements to the mediapackage
    mp.add(MediaPackageElementParser.getFromXml(job1.getPayload()));
    mp.add(MediaPackageElementParser.getFromXml(job2.getPayload()));
    mp.add(MediaPackageElementParser.getFromXml(job3.getPayload()));
    mp.add(MediaPackageElementParser.getFromXml(job4.getPayload()));
    mp.add(MediaPackageElementParser.getFromXml(job5.getPayload()));

    File mpDir = new File(distributionRoot,
            PathSupport.path(defaultOrganization.getId(), "engage-player", mp.getIdentifier().toString()));
    File mediaDir = new File(mpDir, "track-1");
    File metadata1Dir = new File(mpDir, "catalog-1");
    File metadata2Dir = new File(mpDir, "catalog-2");
    File attachmentsDir = new File(mpDir, "notes");
    Assert.assertTrue(mediaDir.exists());
    Assert.assertTrue(metadata1Dir.exists());
    Assert.assertTrue(metadata2Dir.exists());
    Assert.assertTrue(attachmentsDir.exists());
    Assert.assertTrue(new File(mediaDir, "media.mov").exists()); // the filenames are changed to reflect the element ID
    Assert.assertTrue(new File(metadata1Dir, "dublincore.xml").exists());
    Assert.assertTrue(new File(metadata2Dir, "mpeg7.xml").exists());
    Assert.assertTrue(new File(attachmentsDir, "attachment.txt").exists());

    // Now retract the mediapackage and ensure that the distributed files have been removed
    Job job6 = service.retract("engage-player", mp, "track-1");
    Job job7 = service.retract("engage-player", mp, "catalog-1");
    Job job8 = service.retract("engage-player", mp, "catalog-2");
    Job job9 = service.retract("engage-player", mp, "notes");
    jobBarrier = new JobBarrier(null, serviceRegistry, 500, job6, job7, job8, job9);
    jobBarrier.waitForJobs();

    Assert.assertFalse(service.getDistributionFile("engage-player", mp, mp.getElementById("track-1")).isFile());
    Assert.assertFalse(service.getDistributionFile("engage-player", mp, mp.getElementById("catalog-1")).isFile());
    Assert.assertFalse(service.getDistributionFile("engage-player", mp, mp.getElementById("catalog-2")).isFile());
    Assert.assertFalse(service.getDistributionFile("engage-player", mp, mp.getElementById("notes")).isFile());
    Assert.assertTrue(service.getDistributionFile("oai-pmh", mp, mp.getElementById("track-1")).isFile());
  }

  @Test
  public void testRetractByDistributed() throws Exception {
    int elementCount = mp.getElements().length;

    // Distribute the mediapackage and all of its elements
    Job job1 = service.distribute("engage-player", mp, "track-1");
    Job job2 = service.distribute("engage-player", mp, "catalog-1");
    Job job3 = service.distribute("engage-player", mp, "catalog-2");
    Job job4 = service.distribute("engage-player", mp, "notes");
    Job job5 = service.distribute("oai-pmh", mp, "notes");
    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 500, job1, job2, job3, job4, job5);
    jobBarrier.waitForJobs();

    // Add the new elements to the mediapackage
    MediaPackageElement job1Element = MediaPackageElementParser.getFromXml(job1.getPayload());
    mp.add(job1Element);
    MediaPackageElement job2Element = MediaPackageElementParser.getFromXml(job2.getPayload());
    mp.add(job2Element);
    MediaPackageElement job3Element = MediaPackageElementParser.getFromXml(job3.getPayload());
    mp.add(job3Element);
    MediaPackageElement job4Element = MediaPackageElementParser.getFromXml(job4.getPayload());
    mp.add(job4Element);
    MediaPackageElement job5Element = MediaPackageElementParser.getFromXml(job5.getPayload());
    mp.add(job5Element);

    File mpDir = new File(distributionRoot,
            PathSupport.path(defaultOrganization.getId(), "engage-player", mp.getIdentifier().toString()));
    File mediaDir = new File(mpDir, "track-1");
    File metadata1Dir = new File(mpDir, "catalog-1");
    File metadata2Dir = new File(mpDir, "catalog-2");
    File attachmentsDir = new File(mpDir, "notes");
    Assert.assertTrue(mediaDir.exists());
    Assert.assertTrue(metadata1Dir.exists());
    Assert.assertTrue(metadata2Dir.exists());
    Assert.assertTrue(attachmentsDir.exists());
    Assert.assertTrue(new File(mediaDir, "media.mov").exists()); // the filenames are changed to reflect the element ID
    Assert.assertTrue(new File(metadata1Dir, "dublincore.xml").exists());
    Assert.assertTrue(new File(metadata2Dir, "mpeg7.xml").exists());
    Assert.assertTrue(new File(attachmentsDir, "attachment.txt").exists());

    // Now retract the mediapackage and ensure that the distributed files have been removed
    Job job6 = service.retract("engage-player", mp, job1Element.getIdentifier());
    Job job7 = service.retract("engage-player", mp, job2Element.getIdentifier());
    Job job8 = service.retract("engage-player", mp, job3Element.getIdentifier());
    Job job9 = service.retract("engage-player", mp, job4Element.getIdentifier());
    jobBarrier = new JobBarrier(null, serviceRegistry, 500, job6, job7, job8, job9);
    jobBarrier.waitForJobs();

    // Remove the distributed elements from the mediapackage
    mp.remove(MediaPackageElementParser.getFromXml(job6.getPayload()));
    mp.remove(MediaPackageElementParser.getFromXml(job7.getPayload()));
    mp.remove(MediaPackageElementParser.getFromXml(job8.getPayload()));
    mp.remove(MediaPackageElementParser.getFromXml(job9.getPayload()));

    // +1 since the oai-pmh distribution has not been retracted
    Assert.assertEquals(elementCount + 1, mp.getElements().length);
    Assert.assertNotNull(mp.getElementById("track-1"));
    Assert.assertNotNull(mp.getElementById("catalog-1"));
    Assert.assertNotNull(mp.getElementById("catalog-2"));
    Assert.assertNotNull(mp.getElementById("notes"));

    Assert.assertFalse(service.getDistributionFile("engage-player", mp, mp.getElementById("track-1")).isFile());
    Assert.assertFalse(service.getDistributionFile("engage-player", mp, mp.getElementById("catalog-1")).isFile());
    Assert.assertFalse(service.getDistributionFile("engage-player", mp, mp.getElementById("catalog-2")).isFile());
    Assert.assertFalse(service.getDistributionFile("engage-player", mp, mp.getElementById("notes")).isFile());
    Assert.assertTrue(service.getDistributionFile("oai-pmh", mp, mp.getElementById("notes")).isFile());
  }

  @Test
  public void testHLSDistribution() throws Exception {
    // Distribute only some of the elements in the mediapackage
    mp = MediaPackageParser
            .getFromXml(IOUtils.toString(getClass().getResourceAsStream("/hls_1_var_mediapackage.xml"), "UTF-8"));

    String[] elementIds = { "master", "variant", "segments", "dublincore", "mpeg7" };
    Job job = service.distribute("engage-player", mp, Arrays.stream(elementIds).collect(Collectors.toSet()), true);
    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 500, job);
    jobBarrier.waitForJobs();

    File mpDir = new File(distributionRoot,
            PathSupport.path(defaultOrganization.getId(), "engage-player", mp.getIdentifier().toString()));
    Assert.assertTrue(mpDir.exists());
    File mediaDir = new File(mpDir, "master");
    File variantDir = new File(mpDir, "variant");
    File segmentDir = new File(mpDir, "segments");
    File metadataDir = new File(mpDir, "dublincore");
    File attachmentsDir = new File(mpDir, "mpeg7");
    Assert.assertTrue(mediaDir.exists());
    Assert.assertTrue(variantDir.exists());
    Assert.assertTrue(segmentDir.exists());
    Assert.assertTrue(metadataDir.exists());
    Assert.assertTrue(attachmentsDir.exists());
    Assert.assertTrue(new File(mediaDir, "master.m3u8").exists()); // the filenames are changed to reflect the element
                                                                   // ID
    Assert.assertTrue(new File(variantDir, "variant_0.m3u8").exists()); // the filenames are changed to reflect the
                                                                      // element ID
    Assert.assertTrue(new File(segmentDir, "file_0.mp4").exists()); // the filenames are changed to reflect the element
                                                                    // ID
    Assert.assertTrue(new File(metadataDir, "dublincore.xml").exists());
    Assert.assertTrue(new File(attachmentsDir, "mpeg7.xml").exists()); // the filenames are changed to reflect the
                                                                       // element ID
    Assert.assertFalse(new File(metadataDir, "mpeg7.xml").exists());
    Assert.assertFalse(new File(attachmentsDir, "attachment.txt").exists());
    File playlist = new File(mediaDir, "master.m3u8");
    logger.info("Reading playlist " + playlist);
    Optional<String> hasRelativeLink = Files.lines(playlist.toPath())
            .filter(s -> s.contains("../variant/variant_0.m3u8"))
            .findFirst();
    Assert.assertTrue(hasRelativeLink.isPresent());
    playlist = new File(variantDir, "variant_0.m3u8");
    hasRelativeLink = Files.lines(playlist.toPath()).filter(s -> s.contains("../segments/file_0.mp4"))
            .findFirst();
    Assert.assertTrue(hasRelativeLink.isPresent());
  }

  /**
   * An element whose publication URI refers to the channel and media package of the current
   * job resolves to the file in that channel's subtree
   */
  @Test
  public void testGetDistributionFileOfCurrentJob() throws Exception {
    String orgId = defaultOrganization.getId();
    String mpId = mp.getIdentifier().toString();

    // Five-segment layout: .../{orgId}/{channelId}/{mpId}/{elementId}/{fileName}
    MediaPackageElement element = trackElement("track-1",
            UrlSupport.concat(UrlSupport.DEFAULT_BASE_URL, orgId, "engage-player", mpId, "track-1", "media.mov"));
    File file = service.getDistributionFile("engage-player", mp, element);
    File expected = new File(distributionRoot,
            Paths.get(orgId, "engage-player", mpId, "track-1", "media.mov").toString());
    Assert.assertEquals(expected, file);

    // The element segment of the URI is not compared against the element identifier: a
    // distributed element is a copy of the original element whose publication URI references
    // the original element's identifier, and adding the copy to a media package that still
    // contains the original element generates a new identifier for the copy. Retracting such
    // a renamed element must still find its file.
    MediaPackageElement renamedElement = trackElement("renamed-id",
            UrlSupport.concat(UrlSupport.DEFAULT_BASE_URL, orgId, "engage-player", mpId, "track-1", "media.mov"));
    Assert.assertEquals(expected, service.getDistributionFile("engage-player", mp, renamedElement));

    // Legacy layout without channel level: .../{orgId}/{mpId}/{elementId}/{fileName}
    MediaPackageElement legacyElement = trackElement("track-1",
            UrlSupport.concat(UrlSupport.DEFAULT_BASE_URL, orgId, mpId, "track-1", "media.mov"));
    File legacyFile = service.getDistributionFile("engage-player", mp, legacyElement);
    File legacyExpected = new File(distributionRoot,
            Paths.get(orgId, mpId, "track-1", "media.mov").toString());
    Assert.assertEquals(legacyExpected, legacyFile);
  }

  /**
   * An element whose publication URI refers to another channel or media package must not be
   * able to address that channel's or media package's files, nor may the URI use unsafe path
   * segments. A URI that stays within the channel and media package of the current job but
   * names a different element is deliberately accepted (see
   * {@link #testGetDistributionFileOfCurrentJob()})
   */
  @Test
  public void testGetDistributionFileRejectsForeignUri() throws Exception {
    String orgId = defaultOrganization.getId();
    String mpId = mp.getIdentifier().toString();

    // Five-segment layout with a foreign channel or media package
    assertIllegalState(
            () -> service.getDistributionFile("engage-player", mp,
                    trackElement("track-1", UrlSupport.concat(UrlSupport.DEFAULT_BASE_URL, orgId, "oai-pmh", mpId,
                            "track-1", "media.mov"))),
            "URI of another channel");
    assertIllegalState(
            () -> service.getDistributionFile("engage-player", mp,
                    trackElement("track-1", UrlSupport.concat(UrlSupport.DEFAULT_BASE_URL, orgId,
                            "engage-player", "other-mp", "track-1", "media.mov"))),
            "URI of another media package");

    // Legacy layout with a foreign media package
    assertIllegalState(
            () -> service.getDistributionFile("engage-player", mp,
                    trackElement("track-1",
                            UrlSupport.concat(UrlSupport.DEFAULT_BASE_URL, orgId, "other-mp", "track-1",
                                    "media.mov"))),
            "legacy URI of another media package");

    // Unsafe path segments
    assertIllegalState(
            () -> service.getDistributionFile("engage-player", mp,
                    trackElement("track-1", UrlSupport.concat(UrlSupport.DEFAULT_BASE_URL, orgId,
                            "engage-player", mpId, "..", "media.mov"))),
            "traversal segment in URI");
    assertIllegalState(
            () -> service.getDistributionFile("engage-player", mp,
                    trackElement("track-1", UrlSupport.DEFAULT_BASE_URL + "/" + orgId + "//engage-player/" + mpId
                            + "/track-1/media.mov")),
            "empty segment in URI");
  }

  /**
   * The channel identifier is joined into the distribution path and determines the directory
   * the containment check is anchored at, so a traversal channel identifier would move that
   * directory along with the path. It must be rejected outright.
   */
  @Test
  public void testUnsafeChannelIdIsRejected() throws Exception {
    final MediaPackageElement element = trackElement("track-1", "http://wfr.example/mediapackage/mp/track-1/media.mov");
    for (String channelId : new String[] { "", ".", "..", "../..", "../../../..", "a/b", "a\\b" }) {
      assertIllegalState(() -> service.getDistributionFile(channelId, mp, element),
              "distribution file for channel '" + channelId + "'");
      assertIllegalState(() -> service.getMediaPackageDirectory(channelId, mp),
              "media package directory for channel '" + channelId + "'");
    }
  }

  /**
   * A publication URI in the legacy layout without channel level is only compared with the
   * media package of the job. If that media package is named like a channel directory, such
   * a URI can address the element directory of another media package in that channel. A
   * retraction must not delete it (nor its parent, the other media package's directory).
   */
  @Test
  public void testRetractLegacyUriDoesNotDeleteForeignElementDirectory() throws Exception {
    String orgId = defaultOrganization.getId();
    File victimFile = new File(distributionRoot,
            Paths.get(orgId, "engage-player", "victim-mp", "victim-element", "media.mov").toString());
    FileUtils.forceMkdir(victimFile.getParentFile());
    FileUtils.writeStringToFile(victimFile, "must not be deleted", "UTF-8");

    MediaPackage channelNamedMp = MediaPackageParser.getFromXml(
            "<mediapackage xmlns=\"http://mediapackage.opencastproject.org\" id=\"engage-player\"/>");
    MediaPackageElement element = trackElement("track-1",
            UrlSupport.concat(UrlSupport.DEFAULT_BASE_URL, orgId, "engage-player", "victim-mp", "victim-element"));
    try {
      service.retractElement("internal", channelNamedMp, element);
      Assert.fail("Expected the retraction of a directory to be rejected");
    } catch (DistributionException e) {
      // intended
      Assert.assertTrue("The element of another media package must not be deleted", victimFile.isFile());
    } finally {
      FileUtils.deleteQuietly(victimFile.getParentFile().getParentFile());
    }
  }

  private static MediaPackageElement trackElement(String id, String url) throws Exception {
    String xml = "<track xmlns=\"http://mediapackage.opencastproject.org\" id=\"" + id
            + "\" type=\"presenter/source\"><url>" + url + "</url></track>";
    return MediaPackageElementParser.getFromXml(xml);
  }

  private static void assertIllegalState(ThrowingRunnable runnable, String description) {
    try {
      runnable.run();
      Assert.fail(description + " must be rejected");
    } catch (IllegalStateException e) {
      // intended
    } catch (Exception e) {
      Assert.fail(description + " was rejected with " + e + " instead of IllegalStateException");
    }
  }

  private interface ThrowingRunnable {
    void run() throws Exception;
  }
}
