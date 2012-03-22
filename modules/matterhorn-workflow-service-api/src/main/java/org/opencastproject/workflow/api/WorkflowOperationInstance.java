/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.workflow.api;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A workflow operation belonging to a workflow instance.
 */
@XmlJavaTypeAdapter(WorkflowOperationInstanceImpl.Adapter.class)
public interface WorkflowOperationInstance extends Configurable {
  public enum OperationState {
    INSTANTIATED, RUNNING, PAUSED, SUCCEEDED, FAILED, SKIPPED, RETRY
  }

  /**
   * Gets the operation type.
   * 
   * @return the operation type
   */
  String getTemplate();

  /**
   * Gets the unique identifier for this operation, or null.
   * 
   * @return the identifier, or null if this operation has not yet run
   */
  Long getId();

  /**
   * Sets the unique identifier for this operation.
   * 
   * @param id
   *          the identifier
   */
  void setId(Long id);

  /**
   * Gets the operation description
   * 
   * @return the description
   */
  String getDescription();

  /**
   * The state of this operation.
   */
  OperationState getState();

  /**
   * Sets the state of this operation
   * 
   * @param state
   *          the state to set
   */
  void setState(OperationState state);

  /**
   * Gets the URL for the hold state.
   * 
   * @return the URL of the hold state, if any, for this operation
   */
  String getHoldStateUserInterfaceUrl();

  /**
   * Returns the title for the link to this operations hold state UI, a default String if no title is set.
   * 
   * @return title to be displayed
   */
  String getHoldActionTitle();

  /** The workflow to run if an exception is thrown while this operation is running. */
  String getExceptionHandlingWorkflow();

  /**
   * If true, this workflow will be put into a failed (or failing, if getExceptionHandlingWorkflow() is not null) state
   * when exceptions are thrown during an operation.
   */
  boolean isFailWorkflowOnException();

  /**
   * The timestamp this operation started. If the job was queued, this can be significantly later than the date created.
   */
  Date getDateStarted();

  /** The number of milliseconds this operation waited in a service queue */
  long getTimeInQueue();

  /** The timestamp this operation completed */
  Date getDateCompleted();

  /** The position of this workflow operation in the workflow instance */
  int getPosition();

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
   * Returns <code>true</code> if this operation can be continued by the user from an optional hold state. A return
   * value of <code>null</code> indicates that this operation instance does not have a hold state.
   * 
   * @return <code>true</code> if this operation instance is continuable
   */
  Boolean isContinuable();

  /**
   * Returns <code>true</code> if this operation can be aborted by the user from an optional hold state. If a resumable
   * operation is aborted from its hold state, the workflow is put into
   * {@link org.opencastproject.workflow.api.WorkflowInstance.WorkflowState#STOPPED}. A return value of
   * <code>null</code> indicates that this operation instance does not have a hold state.
   * 
   * @return <code>true</code> if this operation instance is abortable
   */
  Boolean isAbortable();

  /**
   * Return the strategy to use in case of operation failure
   * 
   * @return a strategy from {@link org.opencastproject.workflow.api.RetryStrategy}.
   */
  RetryStrategy getRetryStrategy();

  /**
   * Returns the number of attempts the workflow service will make to execute this operation.
   * 
   * @return the maximum number of retries before failing
   */
  int getMaxAttempts();

  /**
   * Returns the number of failed executions that have previously been attempted.
   * 
   * @return the number of previous attempts
   */
  int getFailedAttempts();

  /**
   * Sets the current execution host
   * 
   * @param executionHost
   *          the execution host
   */
  void setExecutionHost(String executionHost);

  /**
   * Returns the current execution host
   * 
   * @return the execution host
   */
  String getExecutionHost();

  /**
   * Adds a failed job to the execution history
   * 
   * @param jobId
   *          the failed job id
   */
  void addToExecutionHistory(long jobId);

  /**
   * Returns a list of failed job executions
   * 
   * @return the execution history
   */
  List<Long> getExecutionHistory();

}
