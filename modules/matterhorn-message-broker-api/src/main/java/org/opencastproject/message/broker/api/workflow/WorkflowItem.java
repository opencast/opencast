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

package org.opencastproject.message.broker.api.workflow;

import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowParser;
import org.opencastproject.workflow.api.WorkflowParsingException;

import java.io.Serializable;

/**
 * {@link Serializable} class that represents all of the possible messages sent through a WorkflowService queue.
 */
public class WorkflowItem implements Serializable {

  private static final long serialVersionUID = -202811055899495045L;

  public static final String WORKFLOW_QUEUE_PREFIX = "WORKFLOW.";

  public static final String WORKFLOW_QUEUE = WORKFLOW_QUEUE_PREFIX + "QUEUE";

  private final String workflowDefinitionId;
  private final String workflowDefinition;

  private final long workflowInstanceId;
  private final String workflowInstance;

  private final Type type;

  public enum Type {
    AddDefinition, DeleteDefinition, UpdateInstance, DeleteInstance
  };

  /**
   * @param workflowDefinition
   *          The workflow definition to add.
   * @return Builds a {@link WorkflowItem} for adding a workflow definition.
   */
  public static WorkflowItem addDefinition(WorkflowDefinition workflowDefinition) {
    return new WorkflowItem(workflowDefinition);
  }

  /**
   * @param workflowInstance
   *          The workflow instance to update.
   * @return Builds {@link WorkflowItem} for updating a workflow instance.
   */
  public static WorkflowItem updateInstance(WorkflowInstance workflowInstance) {
    return new WorkflowItem(workflowInstance);
  }

  /**
   * @param workflowInstanceId
   *          The unique id of the workflow instance to delete.
   * @param workflowInstance
   *          The workflow instance to delete.
   * @return Builds {@link WorkflowItem} for deleting a workflow instance.
   */
  public static WorkflowItem deleteInstance(long workflowInstanceId, WorkflowInstance workflowInstance) {
    return new WorkflowItem(workflowInstanceId, workflowInstance);
  }

  /**
   * @param workflowDefinitionId
   *          The unique id of the workflow definition to delete.
   * @return Builds {@link WorkflowItem} for deleting a workflow definition.
   */
  public static WorkflowItem deleteDefinition(String workflowDefinitionId) {
    return new WorkflowItem(workflowDefinitionId);
  }

  /**
   * Constructor to build an add workflow definition {@link WorkflowItem}.
   *
   * @param workflowDefinition
   *          The workflow definition to add.
   */
  public WorkflowItem(WorkflowDefinition workflowDefinition) {
    this.workflowDefinitionId = null;
    try {
      this.workflowDefinition = WorkflowParser.toXml(workflowDefinition);
    } catch (WorkflowParsingException e) {
      throw new IllegalStateException(String.format("Not able to serialize the given workflow definition %s.",
              workflowDefinition), e);
    }
    this.workflowInstanceId = -1;
    this.workflowInstance = null;
    this.type = Type.AddDefinition;
  }

  /**
   * Constructor to build an update workflow instance {@link WorkflowItem}.
   *
   * @param workflowInstance
   *          The workflow instance to update.
   */
  public WorkflowItem(WorkflowInstance workflowInstance) {
    this.workflowDefinitionId = null;
    this.workflowDefinition = null;
    this.workflowInstanceId = -1;
    try {
      this.workflowInstance = WorkflowParser.toXml(workflowInstance);
    } catch (WorkflowParsingException e) {
      throw new IllegalStateException(String.format("Not able to serialize the given workflow instance %s.",
              workflowInstance), e);
    }
    this.type = Type.UpdateInstance;
  }

  /**
   * Constructor to build a delete workflow {@link WorkflowItem}.
   *
   * @param workflowDefinitionId
   *          The id of the workflow definition to delete.
   */
  public WorkflowItem(String workflowDefinitionId) {
    this.workflowDefinitionId = workflowDefinitionId;
    this.workflowDefinition = null;
    this.workflowInstanceId = -1;
    this.workflowInstance = null;
    this.type = Type.DeleteDefinition;
  }

  /**
   * Constructor to build a delete workflow {@link WorkflowItem}.
   *
   * @param workflowDefinitionId
   *          The id of the workflow instance to delete.
   * @param workflowInstance
   *          The workflow instance to update.
   */
  public WorkflowItem(long workflowInstanceId, WorkflowInstance workflowInstance) {
    this.workflowDefinitionId = null;
    this.workflowDefinition = null;
    this.workflowInstanceId = workflowInstanceId;
    try {
      this.workflowInstance = WorkflowParser.toXml(workflowInstance);
    } catch (WorkflowParsingException e) {
      throw new IllegalStateException(String.format("Not able to serialize the given workflow instance %s.",
              workflowInstance), e);
    }
    this.type = Type.DeleteInstance;
  }

  public String getWorkflowDefinitionId() {
    return workflowDefinitionId;
  }

  public WorkflowDefinition getWorkflowDefinition() {
    try {
      return WorkflowParser.parseWorkflowDefinition(workflowDefinition);
    } catch (WorkflowParsingException e) {
      throw new IllegalStateException(String.format("Not able to serialize the workflow definition %s.",
              workflowDefinition), e);
    }
  }

  public long getWorkflowInstanceId() {
    return workflowInstanceId;
  }

  public WorkflowInstance getWorkflowInstance() {
    try {
      return workflowInstance == null ? null : WorkflowParser.parseWorkflowInstance(workflowInstance);
    } catch (WorkflowParsingException e) {
      throw new IllegalStateException("Not able to parse the workflow instance.", e);
    }
  }

  public Type getType() {
    return type;
  }

}
