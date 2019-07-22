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

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AddCatalogWorkflowOperationHandlerTest {

  private AddCatalogWorkflowOperationHandler operationHandler;
  private WorkflowInstanceImpl instance;
  private WorkflowOperationInstanceImpl operation;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    operationHandler = new AddCatalogWorkflowOperationHandler();

    instance = new WorkflowInstanceImpl();

    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    operation = new WorkflowOperationInstanceImpl("test", OperationState.RUNNING);
    ops.add(operation);
    instance.setOperations(ops);
    String catalogName = "test-catalog";
    operation.setConfiguration("catalog-name", catalogName);
    operation.setConfiguration("catalog-path", getClass().getResource("/dublincore.xml").getPath());

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    instance.setMediaPackage(mp);

    Workspace workspace = EasyMock.createMock(Workspace.class);
    final Capture<InputStream> inStream = EasyMock.newCapture();
    EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.eq(catalogName),
                                  EasyMock.capture(inStream))).andAnswer(() -> {
                                      final File file = temporaryFolder.newFile();
                                      FileUtils.copyInputStreamToFile(inStream.getValue(), file);
                                      return file.toURI();
                                    }).anyTimes();
    EasyMock.replay(workspace);

    operationHandler.setWorkspace(workspace);
  }

  @Test
  public void testBasic() throws WorkflowOperationException {
    // setup
    operation.setConfiguration("catalog-flavor", "flavor/test");
    operation.setConfiguration("catalog-type-collision-behavior", "keep");

    operation.setConfiguration("catalog-tags", "tag1,tag2");

    // execution
    WorkflowOperationResult result = operationHandler.start(instance, null);

    // checks
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    MediaPackage mp = result.getMediaPackage();

    Assert.assertEquals(mp.getCatalogs().length, 1);

    Assert.assertEquals(mp.getCatalogs()[0].getTags().length, 2);
    Assert.assertEquals(mp.getCatalogs()[0].getTags()[0], "tag1");
    Assert.assertEquals(mp.getCatalogs()[0].getTags()[1], "tag2");

    Assert.assertEquals(mp.getCatalogs()[0].getFlavor(),
                        MediaPackageElementFlavor.parseFlavor("flavor/test"));
  }

  @Test
  public void testNoTags() throws WorkflowOperationException {
    // setup
    operation.setConfiguration("catalog-flavor", "flavor/test");
    operation.setConfiguration("catalog-type-collision-behavior", "keep");

    // execution
    WorkflowOperationResult result = operationHandler.start(instance, null);

    // checks
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    MediaPackage mp = result.getMediaPackage();

    Assert.assertEquals(mp.getCatalogs().length, 1);

    Assert.assertEquals(mp.getCatalogs()[0].getTags().length, 0);

    Assert.assertEquals(mp.getCatalogs()[0].getFlavor(),
                        MediaPackageElementFlavor.parseFlavor("flavor/test"));
  }

  @Test
  public void testNoFlavorFail() throws WorkflowOperationException {
    // setup
    operation.setConfiguration("catalog-flavor", "");
    operation.setConfiguration("catalog-type-collision-behavior", "keep");

    // execution
    expectedException.expect(WorkflowOperationException.class);
    operationHandler.start(instance, null);
  }

  @Test
  public void testKeep() throws WorkflowOperationException {
    // setup
    operation.setConfiguration("catalog-flavor", "flavor/test");
    operation.setConfiguration("catalog-type-collision-behavior", "keep");

    // execution
    operationHandler.start(instance, null);
    WorkflowOperationResult result = operationHandler.start(instance, null);

    // checks
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    MediaPackage mp = result.getMediaPackage();

    Assert.assertEquals(mp.getCatalogs().length, 2);
  }

  @Test
  public void testSkip() throws WorkflowOperationException {
    // setup
    operation.setConfiguration("catalog-flavor", "flavor/test");
    operation.setConfiguration("catalog-type-collision-behavior", "skip");

    // execution
    operationHandler.start(instance, null);
    WorkflowOperationResult result = operationHandler.start(instance, null);

    // checks
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    MediaPackage mp = result.getMediaPackage();

    Assert.assertEquals(mp.getCatalogs().length, 1);
  }

  @Test
  public void testFail() throws WorkflowOperationException {
    // setup
    operation.setConfiguration("catalog-flavor", "flavor/test");
    operation.setConfiguration("catalog-type-collision-behavior", "fail");

    // execution
    operationHandler.start(instance, null);
    expectedException.expect(WorkflowOperationException.class);
    operationHandler.start(instance, null);
  }
}
