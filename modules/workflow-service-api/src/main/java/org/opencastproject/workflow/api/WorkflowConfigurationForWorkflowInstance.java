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

package org.opencastproject.workflow.api;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A configuration value for workflow operations.
 */
@Entity(name = "WorkflowConfigurationForWorkflowInstance")
@Access(AccessType.FIELD)
@Table(name = "oc_workflow_instance_configuration")
public class WorkflowConfigurationForWorkflowInstance extends WorkflowConfiguration {


  @ManyToOne(fetch = FetchType.LAZY)
  private WorkflowInstance instance;

  public WorkflowConfigurationForWorkflowInstance() {
  }

  public WorkflowConfigurationForWorkflowInstance(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public void setWorkflowInstance(WorkflowInstance instance) {
    this.instance = instance;
  }
}
