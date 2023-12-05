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

package org.opencastproject.workflow.handler.composer;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class ImageConvertWorkflowOperationHandlerTest {

  private MediaPackage mp = null;

  @Before
  public void setUp() throws URISyntaxException, MalformedURLException, IOException, MediaPackageException {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mp = builder.loadFromXml(getClass().getResource("/image_convert_mediapackage.xml").toURI().toURL().openStream());
  }

  @Test
  public void testImageConvert() throws WorkflowOperationException, NotFoundException, IOException, URISyntaxException,
          EncoderException, MediaPackageException, ServiceRegistryException {
    // create workflow operation configuration
    Map<String, String> config = new HashMap<>();
    config.put("source-flavor", "image/intro");
    config.put("target-flavor", "image/converted");
    config.put("target-tags", "convert");
    config.put("encoding-profile", "image.convert");
    // mock workspace
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    URI targetElementUri = new URI("/converted.jpg");
    EasyMock.expect(workspace.moveTo(EasyMock.anyObject(URI.class), EasyMock.anyString(), EasyMock.anyString(),
            EasyMock.anyString())).andReturn(targetElementUri).anyTimes();
    // mock job to be created
    Job job = EasyMock.createNiceMock(Job.class);
    String jobPayloadAttachment = IOUtils.resourceToString("/image_convert_attachment.xml", Charset.forName("UTF-8"));
    EasyMock.expect(job.getPayload()).andReturn(jobPayloadAttachment).anyTimes();
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED);
    // mock service registry
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job);
    // mock composer service
    ComposerService composerService = EasyMock.createNiceMock(ComposerService.class);
    Capture<Attachment> sourceImageAttachmentCapture = EasyMock.newCapture();
    Capture<String[]> encodingProfilesCapture = EasyMock.newCapture();
    EasyMock.expect(composerService.convertImage(EasyMock.capture(sourceImageAttachmentCapture),
            EasyMock.capture(encodingProfilesCapture))).andReturn(job);
    EncodingProfile encodingProfile = EasyMock.createNiceMock(EncodingProfile.class);
    EasyMock.expect(composerService.getProfile("image.convert")).andReturn(encodingProfile);

    EasyMock.replay(workspace, composerService, job, encodingProfile, serviceRegistry);

    WorkflowInstance workflowInstance = mockWorkflowInstance(config);
    // initialize WOH
    ImageConvertWorkflowOperationHandler imageConvertWOH = new ImageConvertWorkflowOperationHandler();
    imageConvertWOH.setServiceRegistry(serviceRegistry);
    imageConvertWOH.setWorkspace(workspace);
    imageConvertWOH.setComposerService(composerService);
    // run test
    WorkflowOperationResult result = imageConvertWOH.start(workflowInstance, null);
    // check result
    MediaPackage resultMP = result.getMediaPackage();
    Attachment[] resultElements = resultMP.getAttachments(MediaPackageElementFlavor.parseFlavor("image/converted"));
    Assert.assertNotNull(resultElements);
    Assert.assertEquals(1, resultElements.length);
    Attachment convertedImageAttachment = resultElements[0];
    Assert.assertTrue(convertedImageAttachment.containsTag("convert"));

    // check captures
    Assert.assertTrue(sourceImageAttachmentCapture.hasCaptured());
    Assert.assertEquals("image/intro", sourceImageAttachmentCapture.getValue().getFlavor().toString());

    Assert.assertTrue(encodingProfilesCapture.hasCaptured());
    Assert.assertEquals("image.convert", encodingProfilesCapture.getValue());
  }

  private WorkflowInstance mockWorkflowInstance(Map<String, String> configurations) {
    WorkflowOperationInstance operation = EasyMock.createNiceMock(WorkflowOperationInstance.class);
    EasyMock.expect(operation.getConfiguration(EasyMock.anyString())).andAnswer(() -> {
      String key = (String) EasyMock.getCurrentArguments()[0];
      return configurations.get(key);
    }).anyTimes();

    WorkflowInstance workflowInstance = EasyMock.createNiceMock(WorkflowInstance.class);
    EasyMock.expect(workflowInstance.getMediaPackage()).andReturn(mp).anyTimes();
    EasyMock.expect(workflowInstance.getCurrentOperation()).andReturn(operation).anyTimes();
    EasyMock.replay(operation, workflowInstance);
    return workflowInstance;
  }
}
