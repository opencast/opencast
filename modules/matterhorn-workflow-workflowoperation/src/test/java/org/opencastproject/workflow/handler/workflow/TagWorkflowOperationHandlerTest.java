/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for {@link TagWorkflowOperationHandler}
 */
public class TagWorkflowOperationHandlerTest {

  private TagWorkflowOperationHandler operationHandler;
  private MediaPackage mp;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mp = builder.loadFromXml(this.getClass().getResourceAsStream("/archive_mediapackage.xml"));

    // set up the handler
    operationHandler = new TagWorkflowOperationHandler();
  }

  @Test
  public void testAllTagsFlavors() throws Exception {
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(mp);

    operation.setConfiguration(TagWorkflowOperationHandler.SOURCE_FLAVORS_PROPERTY, "presenter/source");
    operation.setConfiguration(TagWorkflowOperationHandler.TARGET_FLAVOR_PROPERTY, "presenter/test");
    operation.setConfiguration(TagWorkflowOperationHandler.TARGET_TAGS_PROPERTY, "tag1,tag2");
    operation.setConfiguration(TagWorkflowOperationHandler.COPY_PROPERTY, "false");

    WorkflowOperationResult result = operationHandler.start(instance, null);
    MediaPackage resultingMediapackage = result.getMediaPackage();

    Track track = resultingMediapackage.getTrack("track-2");
    Assert.assertEquals("presenter/test", track.getFlavor().toString());
    Assert.assertEquals("tag1", track.getTags()[0]);
    Assert.assertEquals("tag2", track.getTags()[1]);

    instance = new WorkflowInstanceImpl();
    ops = new ArrayList<WorkflowOperationInstance>();
    operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(resultingMediapackage);

    operation.setConfiguration(TagWorkflowOperationHandler.SOURCE_TAGS_PROPERTY, "tag1");
    operation.setConfiguration(TagWorkflowOperationHandler.TARGET_FLAVOR_PROPERTY, "presenter/source");
    operation.setConfiguration(TagWorkflowOperationHandler.TARGET_TAGS_PROPERTY, "-tag1,+tag3");
    operation.setConfiguration(TagWorkflowOperationHandler.COPY_PROPERTY, "false");

    result = operationHandler.start(instance, null);
    resultingMediapackage = result.getMediaPackage();

    track = resultingMediapackage.getTrack("track-2");
    Assert.assertEquals("presenter/source", track.getFlavor().toString());
    Assert.assertEquals("tag2", track.getTags()[0]);
    Assert.assertEquals("tag3", track.getTags()[1]);
  }

}
