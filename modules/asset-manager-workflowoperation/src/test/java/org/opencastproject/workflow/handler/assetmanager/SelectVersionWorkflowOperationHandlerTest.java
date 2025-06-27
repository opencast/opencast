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

package org.opencastproject.workflow.handler.assetmanager;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SelectVersionWorkflowOperationHandlerTest {
  private static final String SOURCE_FLAVORS = "presenter/delivery,presentation/delivery";
  private static final String NO_TAGS = "hls-full-res-presenter-mp4,hls-full-res-presentation-mp4";

  private SelectVersionWorkflowOperationHandler operationHandler;
  private WorkflowOperationInstance operation;
  private WorkflowInstance wfInstance;
  private MediaPackageBuilder builder;
  private AssetManager assetManager;

  @Before
  public void setUp() throws Exception {
    builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    wfInstance = new WorkflowInstance();
    wfInstance.setId(1);
    wfInstance.setState(WorkflowState.RUNNING);

    operation = new WorkflowOperationInstance("select-version", OperationState.RUNNING);

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    wfInstance.setOperations(operationsList);
    URI uri = SelectVersionWorkflowOperationHandlerTest.class.getResource("/mediapackage-1.xml").toURI();
    wfInstance.setMediaPackage(builder.loadFromXml(uri.toURL().openStream()));

    operationHandler = new SelectVersionWorkflowOperationHandler();
  }

  private void mockAssetManager(int versions) throws Exception {
    List<Snapshot> snapshots = new ArrayList<>();
    for (int version = versions - 1, index = 0; version >= 0; version--, index++) {
      URI uri = SelectVersionWorkflowOperationHandlerTest.class
          .getResource("/mediapackage-" + version + ".xml").toURI();
      Snapshot snapshot = EasyMock.createNiceMock(Snapshot.class);
      EasyMock.expect(snapshot.getMediaPackage()).andReturn(builder.loadFromXml(uri.toURL().openStream()));
      snapshots.add(snapshot);
      EasyMock.replay(snapshot);
    }

    assetManager = EasyMock.createNiceMock(AssetManager.class);

    EasyMock.expect(assetManager.getSnapshotsByIdAndVersion(EasyMock.anyObject(), EasyMock.anyObject()))
        .andReturn(snapshots);

    EasyMock.expect(assetManager.getSnapshotsByIdOrderedByVersion(EasyMock.anyObject(), EasyMock.anyBoolean()))
        .andReturn(snapshots);

    Version version = EasyMock.createNiceMock(Version.class);
    Optional<Version> optV = Optional.of(version);
    EasyMock.expect(assetManager.toVersion(EasyMock.anyObject(String.class))).andReturn(optV);

    EasyMock.replay(assetManager, version);
  }

  @Test
  public void testSelectByNoTags() throws Exception {
    mockAssetManager(3);
    operationHandler.setAssetManager(assetManager);
    operation.setConfiguration(SelectVersionWorkflowOperationHandler.OPT_SOURCE_FLAVORS, SOURCE_FLAVORS);
    operation.setConfiguration(SelectVersionWorkflowOperationHandler.OPT_NO_TAGS, NO_TAGS);
    WorkflowOperationResult result = operationHandler.start(wfInstance, null);

    MediaPackage resultMp = result.getMediaPackage();

    // Make sure no presenter/delivery, presentation/delivery have "hls" tags
    SimpleElementSelector elementSelector = new SimpleElementSelector();
    elementSelector.addFlavor("presenter/delivery");
    elementSelector.addFlavor("presentation/delivery");

    for (MediaPackageElement el : elementSelector.select(resultMp, false)) {
      for (String tag : el.getTags()) {
        Assert.assertTrue(tag.indexOf("hls") == -1);
      }
    }
  }

  @Test
  public void testSelectByVersion() throws Exception {
    mockAssetManager(1);
    operationHandler.setAssetManager(assetManager);
    operation.setConfiguration(SelectVersionWorkflowOperationHandler.OPT_VERSION, "0");
    WorkflowOperationResult result = operationHandler.start(wfInstance, null);

    MediaPackage resultMp = result.getMediaPackage();

    // Make sure no presenter/delivery, presentation/delivery have "hls" tags
    SimpleElementSelector elementSelector = new SimpleElementSelector();
    elementSelector.addFlavor("presenter/delivery");
    elementSelector.addFlavor("presentation/delivery");

    for (MediaPackageElement el : elementSelector.select(resultMp, false)) {
      for (String tag : el.getTags()) {
        Assert.assertTrue(tag.indexOf("hls") == -1);
      }
    }
  }

  @Test(expected = WorkflowOperationException.class)
  public void testSelectInvalidVersion() throws Exception {
    mockAssetManager(1);
    operationHandler.setAssetManager(assetManager);
    operation.setConfiguration(SelectVersionWorkflowOperationHandler.OPT_VERSION, "invalid");
    operationHandler.start(wfInstance, null);
  }

  @Test(expected = WorkflowOperationException.class)
  public void testSelectWithNoConfiguration() throws Exception {
    mockAssetManager(2);
    operationHandler.setAssetManager(assetManager);
    operationHandler.start(wfInstance, null);
  }

  @Test(expected = WorkflowOperationException.class)
  public void testSelectWithBothConfiguration() throws Exception {
    mockAssetManager(1);
    operationHandler.setAssetManager(assetManager);
    operation.setConfiguration(SelectVersionWorkflowOperationHandler.OPT_VERSION, "0");
    operation.setConfiguration(SelectVersionWorkflowOperationHandler.OPT_NO_TAGS, "hls");
    operation.setConfiguration(SelectVersionWorkflowOperationHandler.OPT_SOURCE_FLAVORS, "presenter/delivery");
    operationHandler.start(wfInstance, null);
  }

}
