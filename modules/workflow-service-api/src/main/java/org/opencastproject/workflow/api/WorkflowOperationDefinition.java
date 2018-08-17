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

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Describes an operation or action to be performed as part of a workflow.
 */
@XmlJavaTypeAdapter(WorkflowOperationDefinitionImpl.Adapter.class)
public interface WorkflowOperationDefinition extends Configurable {

  String getId();

  String getDescription();

  /** The workflow to run if an exception is thrown while this operation is running. */
  String getExceptionHandlingWorkflow();

  /**
   * If true, this workflow will be put into a failed (or failing, if getExceptionHandlingWorkflow() is not null) state
   * when exceptions are thrown during an operation.
   */
  boolean isFailWorkflowOnException();

  /**
   * Returns either <code>null</code> or <code>true</code> to have the operation executed. Any other value is
   * interpreted as <code>false</code> and will skip the operation.
   * <p>
   * Usually, this will be a variable name such as <code>${foo}</code>, which will be replaced with its acutal value
   * once the workflow is executed.
   * <p>
   * If both <code>getExecuteCondition()</code> and <code>getSkipCondition</code> return a non-null value, the execute
   * condition takes precedence.
   *
   * @return the excecution condition.
   */
  String getExecutionCondition();

  /**
   * Returns either <code>null</code> or <code>true</code> to have the operation skipped. Any other value is interpreted
   * as <code>false</code> and will execute the operation.
   * <p>
   * Usually, this will be a variable name such as <code>${foo}</code>, which will be replaced with its actual value
   * once the workflow is executed.
   * <p>
   * If both <code>getExecuteCondition()</code> and <code>getSkipCondition</code> return a non-null value, the execute
   * condition takes precedence.
   *
   * @return the excecution condition.
   */
  String getSkipCondition();

  /**
   * Return the retry strategy
   *
   * @return the retry strategy
   */
  RetryStrategy getRetryStrategy();

  /**
   * Returns the number of attempts the workflow service will make to execute this operation.
   *
   * @return the maximum number of retries before failing
   */
  int getMaxAttempts();
}
