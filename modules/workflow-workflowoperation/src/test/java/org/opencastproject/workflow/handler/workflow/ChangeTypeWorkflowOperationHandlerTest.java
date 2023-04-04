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

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeTypeWorkflowOperationHandlerTest {

  private ChangeTypeWorkflowOperationHandler operationHandler;

  // local resources
  private MediaPackage mp;

  private MediaPackageElementFlavor sourceFlavor;

//  // mock services and objects
//  private Workspace workspace = null;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();

    // test resources
    sourceFlavor = MediaPackageElementFlavor.parseFlavor("presentation/source");

    mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();

    URI vttURI = ChangeTypeWorkflowOperationHandler.class.getResource("/vtt.xml").toURI();
    String vttXml = FileUtils.readFileToString(new File(vttURI));
    Attachment captionVtt = (Attachment) MediaPackageElementParser.getFromXml(vttXml);
    captionVtt.setFlavor(sourceFlavor);
    captionVtt.addTag("first");

    mp.add(captionVtt);

    // set up service
    operationHandler = new ChangeTypeWorkflowOperationHandler();
  }

  @Test
  public void testAttachmentToTrack() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(ChangeTypeWorkflowOperationHandler.OPT_SOURCE_FLAVOR, sourceFlavor.toString());
    configurations.put(ChangeTypeWorkflowOperationHandler.OPT_TARGET_FLAVOR, "presentation/source");
    configurations.put(ChangeTypeWorkflowOperationHandler.TARGET_TYPE, "track");

    MediaPackageElementFlavor newFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavor.toString());
    Attachment[] attachments = mp.getAttachments(newFlavor);
    Assert.assertTrue(attachments.length == 1);
    Track[] tracks = mp.getTracks(newFlavor);
    Assert.assertTrue(tracks.length == 0);

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Assert.assertTrue(result.getMediaPackage().getElementsByFlavor(newFlavor).length == 1);
    attachments = mp.getAttachments(newFlavor);
    Assert.assertTrue(attachments.length == 0);
    tracks = result.getMediaPackage().getTracks(newFlavor);
    Assert.assertTrue(tracks.length == 1);
  }

  @Test
  public void testAttachmentToAttachment() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(ChangeTypeWorkflowOperationHandler.OPT_SOURCE_FLAVOR, "*/source");
    configurations.put(ChangeTypeWorkflowOperationHandler.OPT_TARGET_FLAVOR, "*/target");
    configurations.put(ChangeTypeWorkflowOperationHandler.TARGET_TYPE, "attachment");

    MediaPackageElementFlavor newFlavor = MediaPackageElementFlavor.parseFlavor("*/source");
    Attachment[] attachments = mp.getAttachments(newFlavor);
    Assert.assertTrue(attachments.length == 1);
    Track[] tracks = mp.getTracks(newFlavor);
    Assert.assertTrue(tracks.length == 0);

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());

    newFlavor = MediaPackageElementFlavor.parseFlavor("*/target");
    Assert.assertTrue(result.getMediaPackage().getElementsByFlavor(newFlavor).length == 1);
    attachments = mp.getAttachments(newFlavor);
    Assert.assertTrue(attachments.length == 1);
    tracks = result.getMediaPackage().getTracks(newFlavor);
    Assert.assertTrue(tracks.length == 0);
  }

  @Test
  public void testTagsAsSourceSelector() throws Exception {
    // operation configuration
    Map<String, String> configurations = new HashMap<String, String>();
    configurations.put(ChangeTypeWorkflowOperationHandler.OPT_SOURCE_TAGS, "first");
    configurations.put(ChangeTypeWorkflowOperationHandler.OPT_TARGET_FLAVOR, "*/source");
    configurations.put(ChangeTypeWorkflowOperationHandler.TARGET_TYPE, "attachment");

    MediaPackageElementFlavor newFlavor = MediaPackageElementFlavor.parseFlavor(sourceFlavor.toString());
    Attachment[] attachments = mp.getAttachments(newFlavor);
    Assert.assertTrue(attachments.length == 1);
    Track[] tracks = mp.getTracks(newFlavor);
    Assert.assertTrue(tracks.length == 0);

    // run the operation handler
    WorkflowOperationResult result = getWorkflowOperationResult(mp, configurations);

    Assert.assertEquals(Action.CONTINUE, result.getAction());

    Assert.assertTrue(result.getMediaPackage().getElementsByFlavor(newFlavor).length == 1);
    attachments = mp.getAttachments(newFlavor);
    Assert.assertTrue(attachments.length == 1);
    tracks = result.getMediaPackage().getTracks(newFlavor);
    Assert.assertTrue(tracks.length == 0);
  }

  private WorkflowOperationResult getWorkflowOperationResult(MediaPackage mp, Map<String, String> configurations)
          throws WorkflowOperationException {
    // Add the mediapackage to a workflow instance
    WorkflowInstance workflowInstance = new WorkflowInstance();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    workflowInstance.setMediaPackage(mp);
    WorkflowOperationInstance operation = new WorkflowOperationInstance("op", OperationState.RUNNING);
    operation.setTemplate("changetype");
    operation.setState(OperationState.RUNNING);
    for (String key : configurations.keySet()) {
      operation.setConfiguration(key, configurations.get(key));
    }

    List<WorkflowOperationInstance> operationsList = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    // Run the media package through the operation handler, ensuring that metadata gets added
    return operationHandler.start(workflowInstance, null);
  }

}
