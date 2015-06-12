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

import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.opencastproject.util.EqualsUtil.eqMap;
import static org.opencastproject.util.EqualsUtil.eqObj;
import static org.opencastproject.util.EqualsUtil.hash;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.chuck;

/** Product type of a workflow reference and its parameters. */
public final class ConfiguredWorkflowRef {
  private final String workflowId;
  private final Map<String, String> parameters;

  private static final Map<String, String> noparams = Collections.unmodifiableMap(new HashMap<String, String>());

  /** Constructor. */
  public ConfiguredWorkflowRef(String workflowId, Map<String, String> parameters) {
    this.workflowId = workflowId;
    this.parameters = parameters;
  }

  /** Create a workflow with parameters. */
  public static ConfiguredWorkflowRef workflow(String workflowId, Map<String, String> parameters) {
    return new ConfiguredWorkflowRef(workflowId, parameters);
  }

  /** Create a parameterless workflow. */
  public static ConfiguredWorkflowRef workflow(String workflowId) {
    return new ConfiguredWorkflowRef(workflowId, noparams);
  }

  /** Get the workflow id (reference). */
  public String getWorkflowId() {
    return workflowId;
  }

  /** Get the workflow's parameter map. */
  public Map<String, String> getParameters() {
    return parameters;
  }

  @Override
  public boolean equals(Object that) {
    return (this == that) || (that instanceof ConfiguredWorkflowRef && eqFields((ConfiguredWorkflowRef) that));
  }

  private boolean eqFields(ConfiguredWorkflowRef that) {
    return eqObj(getWorkflowId(), that.getWorkflowId())
            && eqMap(getParameters(), that.getParameters());
  }

  @Override
  public int hashCode() {
    return hash(workflowId, parameters);
  }

  @Override public String toString() {
    return "[ConfiguredWorkflowRef workflowId=" + workflowId
            + (!parameters.isEmpty() ? " with parameters" : "")
            + "]";
  }

  /**
   * Convert into a {@link ConfiguredWorkflow}.
   *
   * @return none, if the workflow id cannot be resolved
   */
  public static Option<ConfiguredWorkflow> toConfiguredWorkflow(WorkflowService ws, ConfiguredWorkflowRef ref) {
    try {
      return some(ConfiguredWorkflow.workflow(ws.getWorkflowDefinitionById(ref.getWorkflowId()), ref.getParameters()));
    } catch (WorkflowDatabaseException e) {
      return chuck(e);
    } catch (NotFoundException e) {
      return none();
    }
  }

  /** {@link ConfiguredWorkflowRef#toConfiguredWorkflow(WorkflowService, ConfiguredWorkflowRef)} as a function. */
  public static Function<ConfiguredWorkflowRef, Option<ConfiguredWorkflow>> toConfiguredWorkflow(final WorkflowService ws) {
    return new Function<ConfiguredWorkflowRef, Option<ConfiguredWorkflow>>() {
      @Override public Option<ConfiguredWorkflow> apply(ConfiguredWorkflowRef ref) {
        return toConfiguredWorkflow(ws, ref);
      }
    };
  }

  /** {@link ConfiguredWorkflowRef#toConfiguredWorkflow(WorkflowService, ConfiguredWorkflowRef)} as a function. */
  public static final Function<WorkflowService, Function<ConfiguredWorkflowRef, Option<ConfiguredWorkflow>>> toConfiguredWorkflow =
          new Function<WorkflowService, Function<ConfiguredWorkflowRef, Option<ConfiguredWorkflow>>>() {
            @Override
            public Function<ConfiguredWorkflowRef, Option<ConfiguredWorkflow>> apply(WorkflowService ws) {
              return toConfiguredWorkflow(ws);
            }
          };
}
