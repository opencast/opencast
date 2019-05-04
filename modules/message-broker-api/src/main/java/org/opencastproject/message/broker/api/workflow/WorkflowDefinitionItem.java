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

import org.opencastproject.message.broker.api.MessageItem;

import java.io.Serializable;
import java.util.UUID;

/**
 * Serializable message which is sent whenever a workflow definition is updated.
 */
public class WorkflowDefinitionItem implements MessageItem, Serializable {

  private static final long serialVersionUID = 7484316173240443903L;

  public static final String WORKFLOW_DEFINITION_QUEUE_PREFIX = "WORKFLOW_DEFINITION.";

  public static final String WORKFLOW_DEFINITION_QUEUE = WORKFLOW_DEFINITION_QUEUE_PREFIX + "QUEUE";

  private final String id;
  private final String workflowDefinitionId;
  private final SerializableWorkflowStateMapping[] stateMappings;

  public WorkflowDefinitionItem(String workflowDefinitionId, SerializableWorkflowStateMapping[] stateMappings) {
    this.id = UUID.randomUUID().toString();
    this.workflowDefinitionId = workflowDefinitionId;
    this.stateMappings = stateMappings;
  }

  @Override
  public String getId() {
    return id;
  }

  public String getWorkflowDefinitionId() {
    return workflowDefinitionId;
  }

  public SerializableWorkflowStateMapping[] getStateMappings() {
    return stateMappings;
  }
}
