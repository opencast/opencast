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

package org.opencastproject.workflow.handler.hello;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * The <code>HelloWorldWorkflowOperationHandler</code> provides a very simple example of how a workflow operation works
 * and can be a starting point for new developments.
 *
 * Like the other hello-world modules, this is intentionally not included in the Opencast distributions and thus not
 * listed in the documentation since people cannot actually use it.
 */
@Component(
    property = {
        "service.description=Hello-World Workflow Operation Handler",
        "workflow.operation=hello-world"
    },
    immediate = true,
    service = WorkflowOperationHandler.class
)
public class HelloWorldWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(HelloWorldWorkflowOperationHandler.class);

  @Override
  public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context) {

    // Get reference to current operation to get configuration, …
    final WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    // Read configuration key `message`, falling back to "Hello world!".
    // Configuration for this operation could look like this:
    //
    //   <operation id="hello-world">
    //     <configurations>
    //       <configuration key="message">A configured message</configuration>
    //     </configurations>
    //   </operation>
    String message = Objects.toString(
        operation.getConfiguration("message"),
        "Hello World!");
    logger.info("Message: {}", message);


    // Most of the time, you would do something with the media package and its content
    // We could also modify the media package, add new content or remove content
    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    logger.info("The media package contains {} tracks", mediaPackage.getTracks().length);

    // Continue the workflow, passing the possibly modified media package to the next operation
    return createResult(mediaPackage, Action.CONTINUE);
  }

}
