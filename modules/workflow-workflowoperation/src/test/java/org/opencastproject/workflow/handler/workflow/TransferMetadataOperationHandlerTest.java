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

package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.CatalogImpl;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class TransferMetadataOperationHandlerTest {

  private TransferMetadataWorkflowOperationHandler handler;
  private WorkflowInstanceImpl instance;
  private WorkflowOperationInstanceImpl operation;
  private DublinCoreCatalog resultCatalog = null;

  @Before
  public void setUp() throws Exception {
    handler = new TransferMetadataWorkflowOperationHandler();

    operation = new WorkflowOperationInstanceImpl("test", WorkflowOperationInstance.OperationState.RUNNING);

    String dc = IOUtils.toString(getClass().getResourceAsStream("/dublincore.xml"), StandardCharsets.UTF_8);
    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.read(EasyMock.anyObject()))
            .andAnswer(() -> getClass().getResourceAsStream("/dublincore.xml")).anyTimes();
    EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyObject()))
            .andAnswer(() -> {
              resultCatalog = DublinCores.read((InputStream) EasyMock.getCurrentArguments()[3]);
              return new URI("http://example.opencast.org/xy");
            }).once();
    EasyMock.replay(workspace);
    handler.setWorkspace(workspace);

    MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    Catalog catalog = CatalogImpl.fromURI(new URI("http://example.com"));
    catalog.setFlavor(MediaPackageElementFlavor.parseFlavor("dublincore/episode"));
    mediaPackage.add(catalog);
    instance = EasyMock.createMock(WorkflowInstanceImpl.class);
    EasyMock.expect(instance.getCurrentOperation()).andReturn(operation).anyTimes();
    EasyMock.expect(instance.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.replay(instance);
  }

  @Test
  public void testNoConfiguration() throws WorkflowOperationException {
    try {
      handler.start(instance, null);
      Assert.fail("Operation should fail");
    } catch (IllegalArgumentException e) {
      // this is expected
    }

    operation.setConfiguration("source-flavor", "dublincore/episode");
    operation.setConfiguration("target-flavor", "lk/episode");

    try {
      handler.start(instance, null);
      Assert.fail("Operation should fail");
    } catch (IllegalArgumentException e) {
      // this is expected
    }
  }

  @Test
  public void testExistingElement() throws WorkflowOperationException {
    // setup
    operation.setConfiguration("source-flavor", "dublincore/episode");
    operation.setConfiguration("target-flavor", "dublincore/episode");
    operation.setConfiguration("source-element", "{http://purl.org/dc/terms/}title");
    operation.setConfiguration("target-element", "{http://purl.org/dc/terms/}description");

    // execution
    try {
      handler.start(instance, null);
      Assert.fail("Operation should have failed");
    } catch (WorkflowOperationException e) {
      // expected
    }

    // force
    operation.setConfiguration("force", "true");
    WorkflowOperationResult result = handler.start(instance, null);

    // checks
    Assert.assertEquals(WorkflowOperationResult.Action.CONTINUE, result.getAction());
    Assert.assertNotNull(resultCatalog);
    EName src = EName.mk("http://purl.org/dc/terms/", "title");
    EName dest = EName.mk("http://purl.org/dc/terms/", "description");
    Assert.assertEquals(resultCatalog.get(src).size(), resultCatalog.get(dest).size());
    Assert.assertEquals(resultCatalog.getFirst(src), resultCatalog.getFirst(dest));
  }

  @Test
  public void testSkip() throws WorkflowOperationException {
    // setup
    operation.setConfiguration("source-flavor", "does-not/exist");
    operation.setConfiguration("target-flavor", "dublincore/episode");
    operation.setConfiguration("source-element", "does-not-exist");
    operation.setConfiguration("target-element", "{http://purl.org/dc/terms/}description");

    // skip since source catalog does not exists
    WorkflowOperationResult result = handler.start(instance, null);
    Assert.assertEquals(WorkflowOperationResult.Action.SKIP, result.getAction());
  }

}
