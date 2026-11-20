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

package org.opencastproject.workflow.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests that the workflow operation worker leaves the values of operation configuration keys declared as templates by
 * the handler untouched, and substitutes all other values as before.
 */
public class WorkflowOperationWorkerTemplatedConfigurationTest {

  /** A workflow operation handler that declares which of its configuration keys are templates */
  private static class TemplatedConfigurationHandler extends AbstractWorkflowOperationHandler {
    private final Set<String> templatedKeys;

    TemplatedConfigurationHandler(Set<String> templatedKeys) {
      super.id = "templated-configuration-test";
      this.templatedKeys = templatedKeys;
    }

    @Override
    public Set<String> getTemplatedConfigurationKeys() {
      return templatedKeys;
    }

    @Override
    public WorkflowOperationResult start(WorkflowInstance workflowInstance, JobContext context) {
      return createResult(Action.CONTINUE);
    }
  }

  /**
   * Builds a workflow instance with the given operation configurations and a single instantiable operation.
   */
  private WorkflowInstance buildWorkflow(Map<String, String> operationConfigurations) {
    WorkflowInstance workflow = new WorkflowInstance();
    workflow.setId(1);
    workflow.setConfiguration("event_id", "my-event");
    WorkflowOperationInstance operation = new WorkflowOperationInstance("operation", OperationState.INSTANTIATED);
    operationConfigurations.forEach(operation::setConfiguration);
    workflow.setOperations(List.of(operation));
    return workflow;
  }

  /**
   * Builds a worker that runs the given handler without persisting the workflow.
   */
  private WorkflowOperationWorker buildWorker(WorkflowOperationHandler handler, WorkflowInstance workflow) {
    WorkflowServiceImpl service = new WorkflowServiceImpl() {
      @Override
      public void update(WorkflowInstance workflowInstance) {
        // no-op: do not persist
      }
    };
    service.setSecurityService(new SecurityServiceStub());
    return new WorkflowOperationWorker(handler, workflow, null, service);
  }

  @Test
  public void templatedConfigurationValuesAreNotSubstituted() throws Exception {
    Map<String, String> operationConfigurations = new HashMap<>();
    operationConfigurations.put("templated", "${event_id}");
    operationConfigurations.put("plain", "${event_id}");
    operationConfigurations.put("org", "${org_logo_small}");
    WorkflowInstance workflow = buildWorkflow(operationConfigurations);
    WorkflowOperationInstance operation = workflow.getCurrentOperation();

    WorkflowOperationWorker worker =
        buildWorker(new TemplatedConfigurationHandler(Set.of("templated")), workflow);
    worker.start();

    // The value of the templated key is left untouched for the handler to render
    assertEquals("${event_id}", operation.getConfiguration("templated"));
    // All other values are substituted as before
    assertEquals("my-event", operation.getConfiguration("plain"));
    assertEquals(
        DefaultOrganization.DEFAULT_PROPERTIES.get("logo_small"),
        operation.getConfiguration("org"));
  }

  @Test
  public void aNullTemplatedConfigurationKeySetIsTreatedAsEmpty() throws Exception {
    Map<String, String> operationConfigurations = new HashMap<>();
    operationConfigurations.put("templated", "${event_id}");
    WorkflowInstance workflow = buildWorkflow(operationConfigurations);
    WorkflowOperationInstance operation = workflow.getCurrentOperation();

    WorkflowOperationWorker worker = buildWorker(new TemplatedConfigurationHandler(null), workflow);
    worker.start();

    assertEquals("my-event", operation.getConfiguration("templated"));
  }

  @Test
  public void allConfigurationValuesAreSubstitutedByDefault() throws Exception {
    Map<String, String> operationConfigurations = new HashMap<>();
    operationConfigurations.put("templated", "${event_id}");
    operationConfigurations.put("plain", "${event_id}");
    WorkflowInstance workflow = buildWorkflow(operationConfigurations);
    WorkflowOperationInstance operation = workflow.getCurrentOperation();

    WorkflowOperationWorker worker =
        buildWorker(new TemplatedConfigurationHandler(Set.of()), workflow);
    worker.start();

    assertEquals("my-event", operation.getConfiguration("templated"));
    assertEquals("my-event", operation.getConfiguration("plain"));
  }
}
