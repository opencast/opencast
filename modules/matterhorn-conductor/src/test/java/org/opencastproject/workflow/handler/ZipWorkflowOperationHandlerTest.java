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
package org.opencastproject.workflow.handler;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationException;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for {@link ZipWorkflowOperationHandler}
 */
public class ZipWorkflowOperationHandlerTest {

  private ZipWorkflowOperationHandler operationHandler;
  private MediaPackage mp;

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    mp = builder.loadFromXml(this.getClass().getResourceAsStream("/archive_mediapackage.xml"));

    // set up the handler
    operationHandler = new ZipWorkflowOperationHandler();
  }

  /*
   * MH-9757
   */
  @Test
  public void testInvalidWorkflow() throws Exception {
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    ops.add(operation);
    instance.setOperations(ops);

    operation.setConfiguration(ZipWorkflowOperationHandler.ZIP_COLLECTION_PROPERTY, "failed-zips");
    operation.setConfiguration(ZipWorkflowOperationHandler.INCLUDE_FLAVORS_PROPERTY, "*/source,dublincore/*");
    operation.setConfiguration(ZipWorkflowOperationHandler.TARGET_FLAVOR_PROPERTY, "archive/zip");
    operation.setConfiguration(ZipWorkflowOperationHandler.COMPRESS_PROPERTY, "false");

    try {
      WorkflowOperationResult result = operationHandler.start(null, null);
      Assert.fail("A null workflow is passed so an exception should be thrown");
    } catch (WorkflowOperationException e) {
      // expecting exception
    }

  }
  
  /*
   * MH-9759
   */
  @Test
  public void testInvalidMediaPackage() throws Exception {
    WorkflowInstanceImpl instance = new WorkflowInstanceImpl();
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    WorkflowOperationInstanceImpl operation = new WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED);
    ops.add(operation);
    instance.setOperations(ops);
    instance.setMediaPackage(null);

    operation.setConfiguration(ZipWorkflowOperationHandler.ZIP_COLLECTION_PROPERTY, "failed-zips");
    operation.setConfiguration(ZipWorkflowOperationHandler.INCLUDE_FLAVORS_PROPERTY, "*/source,dublincore/*");
    operation.setConfiguration(ZipWorkflowOperationHandler.TARGET_FLAVOR_PROPERTY, "archive/zip");
    operation.setConfiguration(ZipWorkflowOperationHandler.COMPRESS_PROPERTY, "false");

    try {
      WorkflowOperationResult result = operationHandler.start(instance, null);
      Assert.fail("A null mediapackage is passed so an exception should be thrown");
    } catch (WorkflowOperationException e) {
      // expecting exception
    }
  }
}
