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

package org.opencastproject.ingestdownloadservice.impl;

import static org.easymock.EasyMock.capture;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * Test class for IngestDonwloadService
 */
public class IngestDownloadServiceTest {

  private static final Logger logger = LoggerFactory.getLogger(IngestDownloadServiceTest.class);

  private IngestDownloadServiceImpl service;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  /**
   * Setup for the Service
   */
  @Before
  public void setUp()
          throws IOException, ServiceRegistryException, MediaPackageException, URISyntaxException, NotFoundException {
    service = new IngestDownloadServiceImpl();

    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.getBaseUri()).andReturn(new URI("http://localhost/")).anyTimes();
    EasyMock.expect(workspace.get(EasyMock.anyObject())).andAnswer(() -> testFolder.newFile()).anyTimes();
    EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andReturn(new URI("http://local.opencast/xy")).anyTimes();
    workspace.delete(EasyMock.anyObject());
    EasyMock.expectLastCall().anyTimes();

    // Finish setting up the mocks
    EasyMock.replay(workspace);

    ServiceRegistry serviceRegistry = EasyMock.createMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getServiceRegistrationsByType("org.opencastproject.files"))
            .andReturn(Collections.emptyList()).once();
    final Capture<String> type = EasyMock.newCapture();
    final Capture<String> operation = EasyMock.newCapture();
    final Capture<List<String>> args = EasyMock.newCapture();
    EasyMock.expect(serviceRegistry.createJob(capture(type), capture(operation), capture(args)))
            .andAnswer(() -> {
              // you could do work here to return something different if you needed.
              Job job = new JobImpl(0);
              job.setJobType(type.getValue());
              job.setOperation(operation.getValue());
              job.setArguments(args.getValue());
              job.setPayload(service.process(job));
              return job;
            }).anyTimes();
    EasyMock.replay(serviceRegistry);

    service.setServiceRegistry(serviceRegistry);
    service.setWorkspace(workspace);
  }

  @Test
  public void testProcessEmptyMediaPackage() throws Exception {
    MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    service.ingestDownload(mediaPackage, "*/*", "", true, false);
  }

  @Test
  public void testDownloadToWorkspace() throws Exception {
    MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    TrackImpl track = new TrackImpl();
    track.setURI(new URI("http://localhost:9/a.mp4"));
    track.setFlavor(MediaPackageElementFlavor.flavor("a", "b"));
    mediaPackage.add(track);
    Job job = service.ingestDownload(mediaPackage, "*/*", "a", true, false);
    mediaPackage = MediaPackageParser.getFromXml(job.getPayload());
    for (MediaPackageElement element: mediaPackage.getElements()) {
      Assert.assertEquals("http://local.opencast/xy", element.getURI().toString());
    }
  }
}
