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
package org.opencastproject.distribution.streaming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.UrlSupport.concat;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;

public class StreamingDistributionServiceTest {

  private static final Logger logger = LoggerFactory.getLogger(StreamingDistributionServiceTest.class);
  private StreamingDistributionServiceImpl service = null;
  private MediaPackage mp = null;
  private File distributionRoot = null;
  private ServiceRegistry serviceRegistry = null;
  private DefaultOrganization defaultOrganization;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    final File mediaPackageRoot = new File(getClass().getResource("/mediapackage.xml").toURI()).getParentFile();
    mp = MediaPackageParser.getFromXml(IOUtils.toString(getClass().getResourceAsStream("/mediapackage.xml"), "UTF-8"));

    distributionRoot = new File(mediaPackageRoot, "static");
    service = new StreamingDistributionServiceImpl();

    defaultOrganization = new DefaultOrganization();
    User anonymous = new JaxbUser("anonymous", "test", defaultOrganization,
            new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, defaultOrganization));
    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser((String) EasyMock.anyObject())).andReturn(anonymous).anyTimes();
    EasyMock.replay(userDirectoryService);
    service.setUserDirectoryService(userDirectoryService);

    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(defaultOrganization).anyTimes();
    EasyMock.replay(organizationDirectoryService);
    service.setOrganizationDirectoryService(organizationDirectoryService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(defaultOrganization).anyTimes();
    EasyMock.replay(securityService);
    service.setSecurityService(securityService);

    serviceRegistry = new ServiceRegistryInMemoryImpl(service, securityService, userDirectoryService,
            organizationDirectoryService, EasyMock.createNiceMock(IncidentService.class));
    service.setServiceRegistry(serviceRegistry);

    final Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        final URI uri = (URI) EasyMock.getCurrentArguments()[0];
        final String[] pathElems = uri.getPath().split("/");
        final String file = pathElems[pathElems.length - 1];
        return new File(mediaPackageRoot, file);
      }
    }).anyTimes();
    EasyMock.replay(workspace);
    service.setWorkspace(workspace);

    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty("org.opencastproject.streaming.url")).andReturn("rtmp://localhost/").anyTimes();
    EasyMock.expect(bc.getProperty("org.opencastproject.streaming.directory")).andReturn(distributionRoot.getPath())
            .anyTimes();
    EasyMock.replay(bc);

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();
    EasyMock.replay(cc);

    service.activate(cc);
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteDirectory(distributionRoot);
    ((ServiceRegistryInMemoryImpl) serviceRegistry).dispose();
  }

  @Test
  public void testUriFileConversionFlvWorkspace() throws Exception {
    final StreamingDistributionServiceImpl.Locations loc = new StreamingDistributionServiceImpl.Locations(
            URI.create("rtmp://localhost/matterhorn-engage"), testFolder.newFolder(), false);
    final String channelId = "engage-player";
    final String mpId = "9f411edb-edf5-4308-8df5-f9b111d9d346";
    final String mpeId = "bed1cdba-2d42-49b1-b78f-6c6745fb064a";
    final URI mpeUri = URI
            .create(concat("http://localhost:8080/files/mediapackage/", mpId, mpeId, "hans_arp_1m10s.flv"));
    //
    final URI distUri = loc.createDistributionUri(defaultOrganization.getId(), channelId, mpId, mpeId, mpeUri);
    logger.info(distUri.toString());
    assertTrue("original URI and distribution URI are not equal", !distUri.equals(mpeUri));
    final File distFile = loc.createDistributionFile(defaultOrganization.getId(), channelId, mpId, mpeId, mpeUri);
    logger.info(distFile.toString());
    final Option<File> retrievedFile = loc.getDistributionFileFrom(distUri);
    logger.info(retrievedFile.toString());
    assertTrue("file could be retrieved from distribution URI", retrievedFile.isSome());
    assertEquals("file retrieved from distribution URI and distribution file match", distFile, retrievedFile.get());
  }

  @Test
  public void testUriFileConversionFlvDistribution() throws Exception {
    final StreamingDistributionServiceImpl.Locations loc = new StreamingDistributionServiceImpl.Locations(
            URI.create("rtmp://localhost/matterhorn-engage"), testFolder.newFolder(), false);
    final String channelId = "engage-player";
    final String mpId = "9f411edb-edf5-4308-8df5-f9b111d9d346";
    final String mpeId = "bed1cdba-2d42-49b1-b78f-6c6745fb064a";
    final URI mpeUri = URI.create(
            concat("rtmp://localhost/matterhorn-engage/mh_default_org/", channelId, mpId, mpeId, "Hans_Arp_1m10s"));
    //
    final URI distUri = loc.createDistributionUri(defaultOrganization.getId(), channelId, mpId, mpeId, mpeUri);
    logger.info(distUri.toString());
    assertEquals("original URI and distribution URI are equal", distUri, mpeUri);
    final File distFile = loc.createDistributionFile(defaultOrganization.getId(), channelId, mpId, mpeId, mpeUri);
    logger.info(distFile.toString());
    final Option<File> retrievedFile = loc.getDistributionFileFrom(distUri);
    logger.info(retrievedFile.toString());
    assertTrue("file could be retrieved from distribution URI", retrievedFile.isSome());
    assertEquals("file retrieved from distribution URI and distribution file match", distFile, retrievedFile.get());
  }

  @Test
  public void testUriFileConversionMp4Workspace() throws Exception {
    final StreamingDistributionServiceImpl.Locations loc = new StreamingDistributionServiceImpl.Locations(
            URI.create("rtmp://localhost/matterhorn-engage"), testFolder.newFolder(), false);
    final String channelId = "engage-player";
    final String mpId = "9f411edb-edf5-4308-8df5-f9b111d9d346";
    final String mpeId = "bed1cdba-2d42-49b1-b78f-6c6745fb064a";
    final URI mpeUri = URI
            .create(concat("http://localhost:8080/files/mediapackage/", mpId, mpeId, "hans_arp_1m10s.mp4"));
    //
    final URI distUri = loc.createDistributionUri(defaultOrganization.getId(), channelId, mpId, mpeId, mpeUri);
    logger.info(distUri.toString());
    assertTrue("original URI and distribution URI are not equal", !distUri.equals(mpeUri));
    final File distFile = loc.createDistributionFile(defaultOrganization.getId(), channelId, mpId, mpeId, mpeUri);
    logger.info(distFile.toString());
    final Option<File> retrievedFile = loc.getDistributionFileFrom(distUri);
    logger.info(retrievedFile.toString());
    assertTrue("file could be retrieved from distribution URI", retrievedFile.isSome());
    assertEquals("file retrieved from distribution URI and distribution file match", distFile, retrievedFile.get());
  }

  @Test
  public void testUriFileConversionMp4Distribution() throws Exception {
    final StreamingDistributionServiceImpl.Locations loc = new StreamingDistributionServiceImpl.Locations(
            URI.create("rtmp://localhost/matterhorn-engage"), testFolder.newFolder(), false);
    final String channelId = "engage-player";
    final String mpId = "9f411edb-edf5-4308-8df5-f9b111d9d346";
    final String mpeId = "bed1cdba-2d42-49b1-b78f-6c6745fb064a";
    final URI mpeUri = URI.create(
            concat("rtmp://localhost/matterhorn-engage/mp4:mh_default_org/", channelId, mpId, mpeId, "Hans_Arp_1m10s"));
    //
    final URI distUri = loc.createDistributionUri(defaultOrganization.getId(), channelId, mpId, mpeId, mpeUri);
    logger.info(distUri.toString());
    assertEquals("original URI and distribution URI are equal", distUri, mpeUri);
    final File distFile = loc.createDistributionFile(defaultOrganization.getId(), channelId, mpId, mpeId, mpeUri);
    logger.info(distFile.toString());
    final Option<File> retrievedFile = loc.getDistributionFileFrom(distUri);
    logger.info(retrievedFile.toString());
    assertTrue("file could be retrieved from distribution URI", retrievedFile.isSome());
    assertEquals("file retrieved from distribution URI and distribution file match", distFile, retrievedFile.get());
  }

  @Test
  public void testUriFileRetrieval() throws Exception {
    File testDir = testFolder.newFolder();
    final StreamingDistributionServiceImpl.Locations loc1 = new StreamingDistributionServiceImpl.Locations(
            URI.create("rtmp://localhost/matterhorn-engage"), testDir, false);
    final StreamingDistributionServiceImpl.Locations loc2 = new StreamingDistributionServiceImpl.Locations(
            URI.create("rtmp://localhost/matterhorn-engage/"), testDir, false);
    final String channelId = "engage-player";
    final String mpId = "9f411edb-edf5-4308-8df5-f9b111d9d346";
    final String mpeId = "bed1cdba-2d42-49b1-b78f-6c6745fb064a";
    final URI distUri = URI.create(
            concat("rtmp://localhost/matterhorn-engage/mp4:mh_default_org/", channelId, mpId, mpeId, "Hans_Arp_1m10s"));
    //
    final Option<File> retrievedFile1 = loc1.getDistributionFileFrom(distUri);
    final Option<File> retrievedFile2 = loc2.getDistributionFileFrom(distUri);
    assertTrue("file could be retrieved from distribution URI", retrievedFile1.isSome());
    assertTrue("file could be retrieved from distribution URI", retrievedFile2.isSome());
    assertEquals(retrievedFile1, retrievedFile2);
  }

  @Test
  public void testDistribution() throws Exception {
    // Distribute the mediapackage and all of its elements
    Job job1 = service.distribute("engage-player", mp, "track-1");
    Job job2 = service.distribute("oai-pmh", mp, "track-1");
    JobBarrier jobBarrier = new JobBarrier(null, serviceRegistry, 500, job1, job2);
    jobBarrier.waitForJobs();

    // Add the new elements to the mediapackage
    mp.add(MediaPackageElementParser.getFromXml(job1.getPayload()));
    mp.add(MediaPackageElementParser.getFromXml(job2.getPayload()));

    File mpDir = new File(distributionRoot,
            PathSupport.path(defaultOrganization.getId(), "engage-player", mp.getIdentifier().compact()));
    File mediaDir = new File(mpDir, "track-1");
    Assert.assertTrue(mediaDir.exists());
    Assert.assertTrue(new File(mediaDir, "media.mov").exists()); // the filenames are changed to reflect the element ID
  }

}
