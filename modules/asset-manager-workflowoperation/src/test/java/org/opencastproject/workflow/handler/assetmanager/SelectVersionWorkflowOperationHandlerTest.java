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
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Order;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.api.query.VersionField;
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
import java.util.Arrays;
import java.util.LinkedHashSet;
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
    // Mock asset manager
    ARecord[] aRecs = new ARecord[versions];
    for (int version = versions - 1, index = 0; version >= 0; version--, index++) {
      URI uri = SelectVersionWorkflowOperationHandlerTest.class
              .getResource("/mediapackage-" + version + ".xml").toURI();
      Snapshot snapshot = EasyMock.createNiceMock(Snapshot.class);
      EasyMock.expect(snapshot.getMediaPackage()).andReturn(builder.loadFromXml(uri.toURL().openStream()));
      aRecs[index] = EasyMock.createNiceMock(ARecord.class);
      EasyMock.expect(aRecs[index].getSnapshot()).andReturn(Optional.of(snapshot));
      EasyMock.replay(snapshot, aRecs[index]);
    }

    assetManager = EasyMock.createNiceMock(AssetManager.class);
    // Mocks for query, result, etc
    LinkedHashSet<ARecord> recStream = new LinkedHashSet<>(Arrays.asList(aRecs));
    Predicate p = EasyMock.createNiceMock(Predicate.class);
    AResult r = EasyMock.createNiceMock(AResult.class);
    EasyMock.expect(r.getSize()).andReturn(new Long(versions));
    EasyMock.expect(r.getRecords()).andReturn(recStream);
    Target t = EasyMock.createNiceMock(Target.class);
    ASelectQuery selectQuery = EasyMock.createNiceMock(ASelectQuery.class);

    EasyMock.expect(selectQuery.where(EasyMock.anyObject(Predicate.class))).andReturn(selectQuery);
    EasyMock.expect(selectQuery.orderBy(EasyMock.anyObject(Order.class))).andReturn(selectQuery);
    EasyMock.expect(selectQuery.run()).andReturn(r);

    AQueryBuilder query = EasyMock.createNiceMock(AQueryBuilder.class);
    EasyMock.expect(query.mediaPackageId(EasyMock.anyObject(String.class))).andReturn(p);
    EasyMock.expect(p.and(EasyMock.anyObject(Predicate.class))).andReturn(p);
    EasyMock.expect(query.snapshot()).andReturn(t);
    EasyMock.expect(query.select(EasyMock.anyObject(Target.class))).andReturn(selectQuery);
    VersionField v = EasyMock.createNiceMock(VersionField.class);
    EasyMock.expect(v.eq(EasyMock.anyObject(Version.class))).andReturn(p);

    Order order = EasyMock.createNiceMock(Order.class);
    EasyMock.expect(v.desc()).andReturn(order);
    EasyMock.expect(query.version()).andReturn(v);
    EasyMock.expect(assetManager.createQuery()).andReturn(query);
    Version version = EasyMock.createNiceMock(Version.class);
    Optional<Version> optV = Optional.of(version);
    EasyMock.expect(assetManager.toVersion(EasyMock.anyObject(String.class))).andReturn(optV);

    EasyMock.replay(assetManager, p, r, t, selectQuery, order, query, v, version);
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
