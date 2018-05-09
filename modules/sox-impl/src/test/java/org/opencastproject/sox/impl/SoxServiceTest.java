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

package org.opencastproject.sox.impl;

import static org.easymock.EasyMock.capture;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.track.AudioStreamImpl;
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
import org.opencastproject.sox.api.SoxException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.easymock.Capture;
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

  /** True to run the tests */
  private static boolean soxInstalled = true;

  private TrackImpl sourceTrack;

  @BeforeClass
  public static void testForSox() throws SoxException {
    Process p = null;
    try {
      ArrayList<String> command = new ArrayList<>();
      command.add(SOX_BINARY);
      command.add("--version");
      p = new ProcessBuilder(command).start();
      if (p.waitFor() != 0) {
        throw new IllegalStateException();
      }
    } catch (Throwable t) {
      logger.warn("Skipping sox audio processing service tests due to unsatisfied or erroneous sox installation");
      soxInstalled = false;
    } finally {
      IoSupport.closeQuietly(p);
    }
  }

  @Before
  public void setUp() throws Exception {
    if (!soxInstalled)
      return;

    // Copy an existing media file to a temp file
    File f = new File(getClass().getResource("/audio-test.wav").getFile());
    source = File.createTempFile(FilenameUtils.getBaseName(f.getName()), ".wav");
    FileUtils.copyFile(f, source);

    JaxbOrganization org = new DefaultOrganization();
    User user = new JaxbUser("admin", "test", org, new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, org));
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

    ServiceRegistry serviceRegistry = EasyMock.createMock(ServiceRegistry.class);
    final Capture<String> type = EasyMock.newCapture();
    final Capture<String> operation = EasyMock.newCapture();
    final Capture<List<String>> args = EasyMock.newCapture();
    EasyMock.expect(serviceRegistry.createJob(capture(type), capture(operation), capture(args), EasyMock.anyFloat()))
            .andAnswer(() -> {
              // you could do work here to return something different if you needed.
              Job job = new JobImpl(0);
              job.setJobType(type.getValue());
              job.setOperation(operation.getValue());
              job.setArguments(args.getValue());
              job.setPayload(soxService.process(job));
              return job;
            }).anyTimes();

    // Finish setting up the mocks
    EasyMock.replay(bc, cc, orgDirectory, userDirectory, securityService, workspace, serviceRegistry);

    // Create and populate the composer service
    soxService = new SoxServiceImpl();
    soxService.setOrganizationDirectoryService(orgDirectory);
    soxService.setSecurityService(securityService);
    soxService.setServiceRegistry(serviceRegistry);
    soxService.setUserDirectoryService(userDirectory);
    soxService.setWorkspace(workspace);
    soxService.activate(cc);

    // Initialize track
    sourceTrack = new TrackImpl();
    AudioStreamImpl audioStream = new AudioStreamImpl();
    audioStream.setBitDepth(16);
    audioStream.setSamplingRate(8000);
    audioStream.setChannels(1);
    audioStream.setRmsLevDb(-20.409999f);
    sourceTrack.addStream(audioStream);
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
    Job job = soxService.analyze(sourceTrack);
    TrackImpl track = (TrackImpl) MediaPackageElementParser.getFromXml(job.getPayload());
    AudioStream audioStream = track.getAudio().get(0);
    assertEquals(-1.159999966621399f, audioStream.getPkLevDb(), 0.0002);
    assertEquals(-20.40999984741211f, audioStream.getRmsLevDb(), 0.0002);
    assertEquals(-13.779999732971191f, audioStream.getRmsPkDb(), 0.0002);
  }

  @Test
  public void testNormalizeIncreaseAudio() throws Exception {
    if (!soxInstalled)
      return;

    assertTrue(source.isFile());
    Job job = soxService.normalize(sourceTrack, -25f);
    TrackImpl track = (TrackImpl) MediaPackageElementParser.getFromXml(job.getPayload());
    AudioStream audioStream = track.getAudio().get(0);
    assertEquals(-25f, audioStream.getRmsLevDb(), 0.9);
  }

  @Test
  public void testNormalizeDecreaseAudio() throws Exception {
    if (!soxInstalled)
      return;

    assertTrue(source.isFile());
    Job job = soxService.normalize(sourceTrack, -30f);
    TrackImpl track = (TrackImpl) MediaPackageElementParser.getFromXml(job.getPayload());
    AudioStream audioStream = track.getAudio().get(0);
    assertEquals(-30f, audioStream.getRmsLevDb(), 0.1);
  }

}
