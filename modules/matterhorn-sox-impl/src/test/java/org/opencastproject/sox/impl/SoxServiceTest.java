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
package org.opencastproject.sox.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl;
import org.opencastproject.sox.api.SoxException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.StreamHelper;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the {@link SoxServiceImpl}.
 */
public class SoxServiceTest {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SoxServiceTest.class);

  private static final String SOX_BINARY = "sox";

  /** The source file to test with */
  private File source = null;

  /** The SoX service to test */
  private SoxServiceImpl soxService = null;

  /** The service registry for job dispatching */
  private ServiceRegistry serviceRegistry = null;

  /** True to run the tests */
  private static boolean soxInstalled = true;

  @BeforeClass
  public static void testForSox() throws SoxException {
    StreamHelper stdout = null;
    StreamHelper stderr = null;
    Process p = null;
    try {
      ArrayList<String> command = new ArrayList<String>();
      command.add(SOX_BINARY);
      command.add("--version");
      p = new ProcessBuilder(command).start();
      stdout = new StreamHelper(p.getInputStream());
      stderr = new StreamHelper(p.getErrorStream());
      if (p.waitFor() != 0)
        throw new IllegalStateException();
    } catch (Throwable t) {
      logger.warn("Skipping sox audio processing service tests due to unsatisifed or erroneus sox installation");
      soxInstalled = false;
    } finally {
      IoSupport.closeQuietly(stdout);
      IoSupport.closeQuietly(stderr);
      IoSupport.closeQuietly(p);
    }
  }

  @Before
  public void setUp() throws Exception {
    if (!soxInstalled)
      return;

    // Copy an existing media file to a temp file
    File f = new File("src/test/resources/audio-test.mp3");
    source = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".mp3");
    FileUtils.copyFile(f, source);
    f = null;

    JaxbOrganization org = new DefaultOrganization();
    User user = new JaxbUser("admin", org, new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, org));
    OrganizationDirectoryService orgDirectory = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDirectory.getOrganization((String) EasyMock.anyObject())).andReturn(org).anyTimes();

    UserDirectoryService userDirectory = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectory.loadUser("admin")).andReturn(user).anyTimes();

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();

    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andReturn(source).anyTimes();

    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty(SoxServiceImpl.CONFIG_SOX_PATH)).andReturn(SOX_BINARY).anyTimes();

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc).anyTimes();

    // Finish setting up the mocks
    EasyMock.replay(bc, cc, orgDirectory, userDirectory, securityService, workspace);

    // Create and populate the composer service
    soxService = new SoxServiceImpl();
    serviceRegistry = new ServiceRegistryInMemoryImpl(soxService, securityService, userDirectory, orgDirectory);
    soxService.setOrganizationDirectoryService(orgDirectory);
    soxService.setSecurityService(securityService);
    soxService.setServiceRegistry(serviceRegistry);
    soxService.setUserDirectoryService(userDirectory);
    soxService.setWorkspace(workspace);
    soxService.activate(cc);
  }

  @After
  public void tearDown() throws Exception {
    FileUtils.deleteQuietly(source);
  }

  @Test
  public void testAnalyzeAudio() throws Exception {
    if (!soxInstalled)
      return;

    assertTrue(source.isFile());
    String sourceTrackXml = "<track id=\"track-1\" type=\"presentation/source\"><mimetype>audio/mp3</mimetype>"
            + "<url>http://localhost:8080/workflow/samples/camera.mpg</url>"
            + "<checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum><duration>14546</duration>"
            + "<audio><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
            + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><channels>2</channels>"
            + "<bitdepth>16</bitdepth><samplingrate>44100</samplingrate></audio></track>";
    Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);
    List<Job> jobs = new ArrayList<Job>();
    for (int i = 0; i < 10; i++) {
      jobs.add(soxService.analyze(sourceTrack));
    }
    boolean success = new JobBarrier(serviceRegistry, jobs.toArray(new Job[jobs.size()])).waitForJobs().isSuccess();
    assertTrue(success);
    for (Job j : jobs) {
      // Always check the service registry for the latest version of the job
      Job job = serviceRegistry.getJob(j.getId());
      TrackImpl track = (TrackImpl) MediaPackageElementParser.getFromXml(job.getPayload());
      AudioStream audioStream = track.getAudio().get(0);
      assertEquals(-0.34f, audioStream.getPkLevDb().floatValue(), 0.0002);
      assertEquals(-24.78f, audioStream.getRmsLevDb().floatValue(), 0.0002);
      assertEquals(-15.00f, audioStream.getRmsPkDb().floatValue(), 0.0002);
      assertEquals(Job.Status.FINISHED, job.getStatus());
    }
  }

  @Test
  public void testNormalizeIncreaseAudio() throws Exception {
    if (!soxInstalled)
      return;

    assertTrue(source.isFile());
    String sourceTrackXml = "<track id=\"track-1\" type=\"presentation/source\"><mimetype>audio/mp3</mimetype>"
            + "<url>http://localhost:8080/workflow/samples/camera.mpg</url>"
            + "<checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum><duration>14546</duration>"
            + "<audio><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
            + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><channels>2</channels>"
            + "<bitdepth>16</bitdepth><rmsleveldb>-24.78</rmsleveldb><samplingrate>44100</samplingrate></audio></track>";
    Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);
    List<Job> jobs = new ArrayList<Job>();
    for (int i = 0; i < 10; i++) {
      jobs.add(soxService.normalize(sourceTrack, -20f));
    }
    boolean success = new JobBarrier(serviceRegistry, jobs.toArray(new Job[jobs.size()])).waitForJobs().isSuccess();
    assertTrue(success);
    for (Job j : jobs) {
      // Always check the service registry for the latest version of the job
      Job job = serviceRegistry.getJob(j.getId());
      TrackImpl track = (TrackImpl) MediaPackageElementParser.getFromXml(job.getPayload());
      AudioStream audioStream = track.getAudio().get(0);
      assertEquals(-20f, audioStream.getRmsLevDb().floatValue(), 0.9);
      assertEquals(Job.Status.FINISHED, job.getStatus());
    }
  }

  @Test
  public void testNormalizeDecreaseAudio() throws Exception {
    if (!soxInstalled)
      return;

    assertTrue(source.isFile());
    String sourceTrackXml = "<track id=\"track-1\" type=\"presentation/source\"><mimetype>audio/mp3</mimetype>"
            + "<url>http://localhost:8080/workflow/samples/camera.mpg</url>"
            + "<checksum type=\"md5\">43b7d843b02c4a429b2f547a4f230d31</checksum><duration>14546</duration>"
            + "<audio><device type=\"UFG03\" version=\"30112007\" vendor=\"Unigraf\" />"
            + "<encoder type=\"H.264\" version=\"7.4\" vendor=\"Apple Inc\" /><channels>2</channels>"
            + "<bitdepth>16</bitdepth><rmsleveldb>-24.78</rmsleveldb><samplingrate>44100</samplingrate></audio></track>";
    Track sourceTrack = (Track) MediaPackageElementParser.getFromXml(sourceTrackXml);
    List<Job> jobs = new ArrayList<Job>();
    for (int i = 0; i < 10; i++) {
      jobs.add(soxService.normalize(sourceTrack, -30f));
    }
    boolean success = new JobBarrier(serviceRegistry, jobs.toArray(new Job[jobs.size()])).waitForJobs().isSuccess();
    assertTrue(success);
    for (Job j : jobs) {
      // Always check the service registry for the latest version of the job
      Job job = serviceRegistry.getJob(j.getId());
      TrackImpl track = (TrackImpl) MediaPackageElementParser.getFromXml(job.getPayload());
      AudioStream audioStream = track.getAudio().get(0);
      assertEquals(-30f, audioStream.getRmsLevDb().floatValue(), 0.1);
      assertEquals(Job.Status.FINISHED, job.getStatus());
    }
  }

}
