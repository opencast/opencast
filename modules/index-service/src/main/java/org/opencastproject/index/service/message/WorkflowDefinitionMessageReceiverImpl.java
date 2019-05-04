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

package org.opencastproject.index.service.message;

import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.workflow.SerializableWorkflowStateMapping;
import org.opencastproject.message.broker.api.workflow.WorkflowDefinitionItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;


public class WorkflowDefinitionMessageReceiverImpl extends BaseMessageReceiverImpl<WorkflowDefinitionItem> {

  private static final Logger logger = LoggerFactory.getLogger(WorkflowDefinitionMessageReceiverImpl.class);

  /**
   * Creates a new message receiver that is listening to the admin ui destination of the workflow queue.
   */
  public WorkflowDefinitionMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(WorkflowDefinitionItem workflowDefintionItem) {
    logger.debug("Received workflow definition item for definition id '{}'", workflowDefintionItem.getWorkflowDefinitionId());

    if (workflowDefintionItem.getStateMappings().length > 0) {
      final Map<String, String> mappings = Arrays
          .stream(workflowDefintionItem.getStateMappings())
          .collect(Collectors.toMap(SerializableWorkflowStateMapping::getState, SerializableWorkflowStateMapping::getValue));
      Event.customWorkflowStatusMapping.put(workflowDefintionItem.getWorkflowDefinitionId(), mappings);
    }

  }
}
