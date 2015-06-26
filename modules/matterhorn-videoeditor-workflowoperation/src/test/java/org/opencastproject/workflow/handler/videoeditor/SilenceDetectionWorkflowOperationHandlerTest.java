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

package org.opencastproject.workflow.handler.videoeditor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.silencedetection.api.SilenceDetectionFailedException;
import org.opencastproject.silencedetection.api.SilenceDetectionService;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.impl.SmilServiceImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;
import org.xml.sax.SAXException;

/**
 * Test class for {@link SilenceDetectionWorkflowOperationHandler}
 */
public class SilenceDetectionWorkflowOperationHandlerTest {

  private SilenceDetectionWorkflowOperationHandler silenceDetectionOperationHandler;
  private SilenceDetectionService silenceDetectionServiceMock = null;
  private SmilService smilService = null;
  private Workspace workspaceMock = null;

  private URI mpURI;
  private MediaPackage mp;
  private URI smilURI;

  @Before
  public void setUp() throws URISyntaxException, MediaPackageException,
          MalformedURLException, IOException {

    MediaPackageBuilder mpBuilder = MediaPackageBuilderFactory.newInstance()
            .newMediaPackageBuilder();

    mpURI = SilenceDetectionWorkflowOperationHandlerTest.class
            .getResource("/silencedetection_mediapackage.xml").toURI();
    mp = mpBuilder.loadFromXml(mpURI.toURL().openStream());
    smilURI = SilenceDetectionWorkflowOperationHandlerTest.class
            .getResource("/silencedetection_smil_filled.smil").toURI();
    smilService = new SmilServiceImpl();
    // create service mocks
    silenceDetectionServiceMock = EasyMock.createNiceMock(SilenceDetectionService.class);
    workspaceMock = EasyMock.createNiceMock(Workspace.class);
    // setup SilenceDetectionWorkflowOperationHandler
    silenceDetectionOperationHandler = new SilenceDetectionWorkflowOperationHandler();
    silenceDetectionOperationHandler.setDetectionService(silenceDetectionServiceMock);
    silenceDetectionOperationHandler.setSmilService(smilService);
    silenceDetectionOperationHandler.setWorkspace(workspaceMock);
  }

  private static Map<String, String> getDefaultConfiguration() {
    Map<String, String> configuration = new HashMap<String, String>();
    configuration.put("source-flavors", "*/audio");
    configuration.put("smil-flavor-subtype", "smil");
    configuration.put("reference-tracks-flavor", "*/preview");
    return configuration;
  }

  private WorkflowInstanceImpl getWorkflowInstance(MediaPackage mp, Map<String, String> configurations) {
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowInstance.WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", WorkflowOperationInstance.OperationState.RUNNING);
    operation.setTemplate("silence");
    operation.setState(WorkflowOperationInstance.OperationState.RUNNING);
    for (String key : configurations.keySet()) {
      operation.setConfiguration(key, configurations.get(key));
    }
    List<WorkflowOperationInstance> operations = new ArrayList<WorkflowOperationInstance>(1);
    operations.add(operation);
    workflowInstance.setOperations(operations);
    return workflowInstance;
  }


  @Test
  public void testStartOperation() throws WorkflowOperationException, SilenceDetectionFailedException,
          NotFoundException, ServiceRegistryException, MediaPackageException, SmilException, MalformedURLException, JAXBException, SAXException, IOException {

    Smil smil = smilService.fromXml(new File(smilURI.toURL().getFile())).getSmil();
    Job job = EasyMock.createNiceMock(Job.class);
    EasyMock.expect(job.getPayload()).andReturn(smil.toXML()).anyTimes();
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED);
    EasyMock.expect(silenceDetectionServiceMock.detect(
            (Track) EasyMock.anyObject(),
            (Track[]) EasyMock.anyObject()))
            .andReturn(job);
    EasyMock.expect(workspaceMock.put(
            (String) EasyMock.anyObject(),
            (String) EasyMock.anyObject(),
            (String) EasyMock.anyObject(),
            (InputStream) EasyMock.anyObject()))
            .andReturn(smilURI);
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    silenceDetectionOperationHandler.setServiceRegistry(serviceRegistry);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job);
    EasyMock.replay(job, serviceRegistry, silenceDetectionServiceMock, workspaceMock);
    WorkflowInstanceImpl workflowInstance = getWorkflowInstance(mp, getDefaultConfiguration());
    WorkflowOperationResult result = silenceDetectionOperationHandler.start(workflowInstance, null);
    Assert.assertNotNull("SilenceDetectionWorkflowOperationHandler workflow operation returns null "
            + "but should be an instantiated WorkflowOperationResult", result);
    EasyMock.verify(silenceDetectionServiceMock, workspaceMock);

    WorkflowOperationInstance worflowOperationInstance = workflowInstance.getCurrentOperation();
    String smilFlavorSubtypeProperty = worflowOperationInstance.getConfiguration("smil-flavor-subtype");

    // test media package contains new smil catalog
    MediaPackageElementFlavor smilPartialFlavor = new MediaPackageElementFlavor("*", smilFlavorSubtypeProperty);
    Catalog[] smilCatalogs = mp.getCatalogs(smilPartialFlavor);
    Assert.assertTrue("Media package should contain a smil catalog",
            smilCatalogs != null && smilCatalogs.length > 0);
  }
}
