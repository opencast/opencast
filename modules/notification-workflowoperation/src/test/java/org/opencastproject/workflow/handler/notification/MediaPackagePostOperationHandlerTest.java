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


package org.opencastproject.workflow.handler.notification;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MediaPackagePostOperationHandlerTest {

  /** the logging facility provided by log4j */
  private static final Logger logger
      = LoggerFactory.getLogger(MediaPackagePostOperationHandlerTest.class.getName());

  /** Represents a tuple of handler and instance, useful for return types */
  private static final class InstanceAndHandler {

    private WorkflowInstance workflowInstance;
    private WorkflowOperationHandler workflowHandler;

    private InstanceAndHandler(WorkflowInstance i,
        WorkflowOperationHandler h) {
      this.workflowInstance = i;
      this.workflowHandler = h;
    }

  }

  /**
   * Creates a new workflow and readies the engine for processing
   */
  private InstanceAndHandler createWorkflow(String url, String format) {
    WorkflowOperationHandler handler = new MediaPackagePostOperationHandler();

    WorkflowInstance workflowInstance = new WorkflowInstance();
    workflowInstance.setId(1);
    workflowInstance.setState(WorkflowState.RUNNING);
    WorkflowOperationInstance operation
        = new WorkflowOperationInstance("op", OperationState.RUNNING);
    List<WorkflowOperationInstance> operationsList
        = new ArrayList<WorkflowOperationInstance>();
    operationsList.add(operation);
    workflowInstance.setOperations(operationsList);

    operation.setConfiguration("url", url);
    operation.setConfiguration("format", format);
    operation.setConfiguration("mediapackage.type", "workflow");
    return new InstanceAndHandler(workflowInstance, handler);
  }

  @Test
  public void testHTTPPostXML() throws Exception {
    // create a dummy mediapackage
    MediaPackageBuilderFactory factory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder builder = factory.newMediaPackageBuilder();
    MediaPackage mp = builder.createNew(new IdImpl("xyz"));
    mp.setTitle("test");
    mp.addContributor("lkiesow");
    mp.addContributor("lkiesow");

    /* Sending stuff to port 9 shound never return anything as the Discard
     * Protocol uses port 9 */
    InstanceAndHandler tuple = createWorkflow("http://127.0.0.1:9", "xml");
    tuple.workflowInstance.setMediaPackage(mp);

    try {
      tuple.workflowHandler.start(tuple.workflowInstance, null);
      /* This should raise an exception. Something is wrong if not. */
      Assert.fail();
    } catch (WorkflowOperationException e) {
      logger.info(e.toString());
    }

  }

  @Test
  public void testHTTPPostJSON() throws Exception {
    // create a dummy mediapackage
    MediaPackageBuilderFactory factory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder builder = factory.newMediaPackageBuilder();
    MediaPackage mp = builder.createNew(new IdImpl("xyz"));
    mp.setTitle("test");
    mp.addContributor("lkiesow");
    mp.addContributor("lkiesow");

    /* Sending stuff to port 9 shound never return anything as the Discard
     * Protocol uses port 9 */
    InstanceAndHandler tuple = createWorkflow("http://127.0.0.1:9", "json");
    tuple.workflowInstance.setMediaPackage(mp);

    try {
      tuple.workflowHandler.start(tuple.workflowInstance, null);
      /* This should raise an exception. Something is wrong if not. */
      Assert.fail();
    } catch (WorkflowOperationException e) {
      logger.info(e.toString());
    }

  }

}
