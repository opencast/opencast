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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Product type of a workflow definition and its parameters. */
public class ConfiguredWorkflow {
  private final WorkflowDefinition workflowDefinition;
  private final Map<String, String> parameters;

  private static final Map<String, String> noparams = Collections.unmodifiableMap(new HashMap<String, String>());

  /** Constructor. */
  public ConfiguredWorkflow(WorkflowDefinition workflowDefinition, Map<String, String> parameters) {
    this.workflowDefinition = workflowDefinition;
    this.parameters = parameters;
  }

  /** Create a workflow with parameters. */
  public static ConfiguredWorkflow workflow(WorkflowDefinition workflowDefinition, Map<String, String> parameters) {
    return new ConfiguredWorkflow(workflowDefinition, parameters);
  }

  /** Create a parameterless workflow. */
  public static ConfiguredWorkflow workflow(WorkflowDefinition workflowDefinition) {
    return new ConfiguredWorkflow(workflowDefinition, noparams);
  }

  /** Get the workflow definition. */
  public WorkflowDefinition getWorkflowDefinition() {
    return workflowDefinition;
  }

  /** Get the workflow's parameter map. */
  public Map<String, String> getParameters() {
    return parameters;
  }
}
