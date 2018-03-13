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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler
    .COPY_NUMBER_PREFIX_PROPERTY;
import static org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler
    .MAX_NUMBER_PROPERTY;
import static org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler
    .NUMBER_PROPERTY;
import static org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler
    .PROPERTY_NAMESPACES_PROPERTY;
import static org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler
    .SOURCE_FLAVORS_PROPERTY;
import static org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler
    .SOURCE_TAGS_PROPERTY;
import static org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler
    .TARGET_TAGS_PROPERTY;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Stream;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuplicateEventWorkflowOperationHandlerTest {

  private DuplicateEventWorkflowOperationHandler operationHandler;

  // local resources
  private MediaPackage mp;

  // mock workspace
  private Workspace workspace = null;

  // mock asset manager
  private AssetManager assetManager = null;

  // mock service registry
  private ServiceRegistry serviceRegistry = null;

  // mock distribution service
  private DistributionService distributionService = null;

  private Capture<MediaPackage> clonedMediaPackages = null;

  @Before
  public void setUp() throws Exception {
    operationHandler = new DuplicateEventWorkflowOperationHandler();

    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    URI uriMP = getClass().getResource("/duplicate-event_mediapackage.xml").toURI();
    mp = builder.loadFromXml(uriMP.toURL().openStream());

    workspace = createNiceMock(Workspace.class);
    assetManager = createNiceMock(AssetManager.class);
    distributionService = createNiceMock(DistributionService.class);
    serviceRegistry = createNiceMock(ServiceRegistry.class);

    operationHandler.setWorkspace(workspace);
    operationHandler.setAssetManager(assetManager);
    operationHandler.setDistributionService(distributionService);
    operationHandler.setServiceRegistry(serviceRegistry);
  }

  @Test
  public void testSuccessfulCreate() throws Exception {

    final int numCopies = 2;

    mockDependencies(numCopies);

    // operation configuration
    Map<String, String> configurations = new HashMap<>();
    configurations.put(SOURCE_FLAVORS_PROPERTY, "*/*");
    configurations.put(SOURCE_TAGS_PROPERTY, "archive");
    configurations.put(TARGET_TAGS_PROPERTY, "");
    configurations.put(NUMBER_PROPERTY, "" + numCopies);
    configurations.put(MAX_NUMBER_PROPERTY, "" + 10);
    configurations.put(PROPERTY_NAMESPACES_PROPERTY, "org.opencastproject.assetmanager.security");
    configurations.put(COPY_NUMBER_PREFIX_PROPERTY, "copy");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());
    Assert.assertEquals(numCopies, clonedMediaPackages.getValues().size());
    for (int i = 1; i <= numCopies; i++) {
      final String expectedTitle = mp.getTitle()
          + " (" + configurations.get(COPY_NUMBER_PREFIX_PROPERTY) + " " + i + ")";
      Assert.assertEquals(expectedTitle, clonedMediaPackages.getValues().get(i - 1).getTitle());
    }
  }

  @Test
  public void testOverrideTags() throws Exception {

    mockDependencies(1);

    // operation configuration
    Map<String, String> configurations = new HashMap<>();
    configurations.put(SOURCE_FLAVORS_PROPERTY, "presenter/source");
    configurations.put(SOURCE_TAGS_PROPERTY, "archive");
    configurations.put(TARGET_TAGS_PROPERTY, "tag1,tag2");
    configurations.put(NUMBER_PROPERTY, "" + 1);
    configurations.put(MAX_NUMBER_PROPERTY, "" + 10);
    configurations.put(PROPERTY_NAMESPACES_PROPERTY, "org.opencastproject.assetmanager.security");
    configurations.put(COPY_NUMBER_PREFIX_PROPERTY, "copy");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Track track = clonedMediaPackages.getValue().getTracksByTag("tag1")[0];
    Assert.assertEquals("tag1", track.getTags()[0]);
    Assert.assertEquals("tag2", track.getTags()[1]);
  }

  @Test
  public void testRemoveAndAddTags() throws Exception {
    mockDependencies(1);
    Map<String, String> configurations = new HashMap<>();
    configurations.put(SOURCE_FLAVORS_PROPERTY, "*/*");
    configurations.put(SOURCE_TAGS_PROPERTY, "part1");
    configurations.put(TARGET_TAGS_PROPERTY, "-part1,+tag3");
    configurations.put(NUMBER_PROPERTY, "" + 1);
    configurations.put(MAX_NUMBER_PROPERTY, "" + 10);
    configurations.put(PROPERTY_NAMESPACES_PROPERTY, "org.opencastproject.assetmanager.security");
    configurations.put(COPY_NUMBER_PREFIX_PROPERTY, "copy");

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);
    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Track track = clonedMediaPackages.getValue().getTracksByTag("tag3")[0];
    final List<String> newTags = Arrays.asList(track.getTags());
    final List<String> originalTags = Arrays.asList(mp.getTracksByTag("part1")[0].getTags());
    Assert.assertEquals(originalTags.size(), newTags.size());
    Assert.assertTrue(newTags.contains("tag3"));
    Assert.assertFalse(newTags.contains("part1"));
  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, Map<String, String> configurations)
      throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstanceImpl workflowInstance = new WorkflowInstanceImpl();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("op", OperationState.RUNNING);
    operation.setTemplate("create-event");
    operation.setState(OperationState.RUNNING);
    for (String key : configurations.keySet()) {
      operation.setConfiguration(key, configurations.get(key));
    }

    List<WorkflowOperationInstance> operationsList = new ArrayList<>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    // Run the media package through the operation handler
    return operationHandler.start(workflowInstance, null);
  }

  private void mockDependencies(int numberOfCopies) throws Exception {
    clonedMediaPackages = Capture.newInstance(CaptureType.ALL);
    reset(workspace, assetManager, distributionService);

    URI uriDc = getClass().getResource("/dublincore.xml").toURI();
    for (int i = 0; i < numberOfCopies; i++) {
      expect(workspace.read(eq(URI.create("dublincore.xml")))).andReturn(new FileInputStream(new File(uriDc)))
              .times(1);
    }
    expect(workspace.get(anyObject())).andReturn(new File(getClass().getResource("/av.mov").toURI())).anyTimes();
    expect(workspace.put(anyString(), anyString(), eq("dublincore.xml"), anyObject()))
        .andReturn(uriDc).times(numberOfCopies);
    replay(workspace);

    final AResult qResult = createNiceMock(AResult.class);
    expect(qResult.getRecords()).andReturn(Stream.empty()).anyTimes();
    replay(qResult);
    final ASelectQuery qSelect = createNiceMock(ASelectQuery.class);
    expect(qSelect.where(anyObject())).andReturn(qSelect).anyTimes();
    expect(qSelect.run()).andReturn(qResult).anyTimes();
    replay(qSelect);
    final AQueryBuilder qBuilder = createNiceMock(AQueryBuilder.class);
    expect(qBuilder.select(anyObject())).andReturn(qSelect).anyTimes();
    replay(qBuilder);
    expect(assetManager.createQuery()).andReturn(qBuilder).anyTimes();
    expect(assetManager.takeSnapshot(eq(AssetManager.DEFAULT_OWNER), capture(clonedMediaPackages)))
        .andReturn(createNiceMock(Snapshot.class)).times(numberOfCopies);
    replay(assetManager);

    final Job distributionJob = createNiceMock(Job.class);
    final Publication internalPub = (Publication) mp.getElementById("pub-int");
    final List<MediaPackageElement> internalPubElements = new ArrayList<>();
    Collections.addAll(internalPubElements, (internalPub.getAttachments()));
    Collections.addAll(internalPubElements, (internalPub.getCatalogs()));
    Collections.addAll(internalPubElements, (internalPub.getTracks()));
    expect(distributionJob.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    for (MediaPackageElement e : internalPubElements) {
      expect(distributionJob.getPayload()).andReturn(MediaPackageElementParser.getAsXml(e)).times(numberOfCopies);
    }
    replay(distributionJob);
    expect(distributionService.distribute(eq(InternalPublicationChannel.CHANNEL_ID), anyObject(), anyString()))
        .andReturn(distributionJob).anyTimes();
    replay(distributionService);
  }

  @Test(expected = WorkflowOperationException.class)
  public void testCreateMoreThanMaximum() throws Exception {

    final int numCopies = 2;
    final int maxCopies = 1;

    mockDependencies(numCopies);

    // operation configuration
    Map<String, String> configurations = new HashMap<>();
    configurations.put(SOURCE_FLAVORS_PROPERTY, "*/*");
    configurations.put(SOURCE_TAGS_PROPERTY, "archive");
    configurations.put(TARGET_TAGS_PROPERTY, "");
    configurations.put(NUMBER_PROPERTY, "" + numCopies);
    configurations.put(MAX_NUMBER_PROPERTY, "" + maxCopies);
    configurations.put(PROPERTY_NAMESPACES_PROPERTY, "org.opencastproject.assetmanager.security");
    configurations.put(COPY_NUMBER_PREFIX_PROPERTY, "copy");

    // run the operation handler
    getWorkflowOperationResult(mp, configurations);
  }

}
