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

package org.opencastproject.videogrid.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workspace.api.Workspace;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoGridServiceImplTest {

  private VideoGridServiceImpl videoGridService;
  private ServiceRegistry serviceRegistry;

  private Track sourceVideoTrack;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    videoGridService = new VideoGridServiceImpl();

    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    URI uri = new URI("/uri");
    EasyMock.expect(workspace.putInCollection(anyString(),
            anyString(), anyObject())).andReturn(uri).anyTimes();
    final String directory = testFolder.newFolder().getAbsolutePath();
    EasyMock.expect(workspace.rootDirectory()).andReturn(directory).anyTimes();

    sourceVideoTrack = (Track) MediaPackageElementParser.getFromXml(IOUtils.toString(
            VideoGridServiceImplTest.class.getResourceAsStream("/composer_test_source_track_video.xml"), Charset.defaultCharset()));

    serviceRegistry = EasyMock.createMock(ServiceRegistry.class);
    final Capture<String> type = EasyMock.newCapture();
    final Capture<String> operation = EasyMock.newCapture();
    final Capture<List<String>> args = EasyMock.newCapture();
    EasyMock.expect(serviceRegistry.createJob(capture(type), capture(operation), capture(args), EasyMock.anyFloat()))
            .andAnswer(() -> {
              Job job = new JobImpl(0);
              job.setJobType(type.getValue());
              job.setOperation(operation.getValue());
              job.setArguments(args.getValue());
              job.setPayload(videoGridService.process(job));
              return job;
            }).anyTimes();

    EasyMock.replay(workspace, serviceRegistry);

    videoGridService.setWorkspace(workspace);
    videoGridService.setServiceRegistry(serviceRegistry);
  }

  @Test
  public void testCreatePartialTracks() throws Exception {

    List<String> command = Arrays.asList("ffmpeg", "-y", "-v", "warning", "-nostats", "-max_error_rate", "1.0",
            "-filter_complex", "color=c=0xFF00FF:s=320x180:r=24,trim=end=3.14", "-an", "-codec", "h264", "-q:v", "2",
            "-g", "240", "-pix_fmt", "yuv420p", "-r", "24");

    List<List<String>> commands = new ArrayList<>();
    commands.add(command);
    commands.add(command);

    Job job = videoGridService.createPartialTracks(
            commands,
            sourceVideoTrack);
    Gson gson = new Gson();
    List<URI> uris = gson.fromJson(job.getPayload(), new TypeToken<List<URI>>() { }.getType());
    Assert.assertEquals(2, uris.size());
  }
}
