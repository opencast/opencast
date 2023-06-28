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
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A workflow operation belonging to a workflow instance.
 */
@Entity(name = "WorkflowOperationInstance")
@Access(AccessType.FIELD)
@Table(name = "oc_workflow_operation", indexes = {
    @Index(name = "IX_oc_workflow_operation_workflow_id", columnList = ("workflow_id"))})
public class WorkflowOperationInstance implements Configurable {
  public enum OperationState {
    INSTANTIATED, RUNNING, PAUSED, SUCCEEDED, FAILED, SKIPPED, RETRY
  }

  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "template")
  protected String template;

  @Column(name = "job")
  protected Long jobId;

  @Column(name = "state")
  protected OperationState state;

  @Column(name = "description")
  @Lob
  protected String description;

  @ElementCollection
  @CollectionTable(
      name = "oc_workflow_operation_configuration",
      joinColumns = @JoinColumn(name = "workflow_operation_id"),
      indexes = {
          @Index(name = "IX_oc_workflow_operation_configuration_workflow_operation_id", columnList = ("workflow_operation_id")),
      }
  )
  @MapKeyColumn(name = "configuration_key", nullable = false)
  @Lob
  @Column(name = "configuration_value")
  protected Map<String, String> configurations;

  @Column(name = "fail_on_error")
  protected boolean failOnError;

  @Column(name = "if_condition")
  @Lob
  protected String executeCondition;

  @Column(name = "exception_handler_workflow")
  protected String exceptionHandlingWorkflow;

  @Column(name = "abortable")
  protected Boolean abortable;

  @Column(name = "continuable")
  protected Boolean continuable;

  @Column(name = "started")
  @Temporal(TemporalType.TIMESTAMP)
  protected Date dateStarted;

  @Column(name = "completed")
  @Temporal(TemporalType.TIMESTAMP)
  protected Date dateCompleted;

  @Column(name = "time_in_queue")
  protected Long timeInQueue;

  @Column(name = "max_attempts")
  protected int maxAttempts;

  @Column(name = "failed_attempts")
  protected int failedAttempts;

  @Column(name = "execution_host")
  protected String executionHost;

  @Column(name = "retry_strategy", length = 128)
  protected RetryStrategy retryStrategy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workflow_id", nullable = false)
  private WorkflowInstance instance;

  /**
   * No-arg constructor needed for JAXB serialization
   */
  public WorkflowOperationInstance() {
    this.maxAttempts = 1;
    this.retryStrategy = RetryStrategy.NONE;
  }

  /**
   * Builds a new workflow operation instance based on another workflow operation.
   *
   * @param def
   *          the workflow definition
   */
  public WorkflowOperationInstance(WorkflowOperationDefinition def) {
    this();
    setTemplate(def.getId());
    setState(OperationState.INSTANTIATED);
    setDescription(def.getDescription());
    setMaxAttempts(def.getMaxAttempts());
    setFailOnError(def.isFailWorkflowOnException());
    setExceptionHandlingWorkflow(def.getExceptionHandlingWorkflow());
    setExecutionCondition(def.getExecutionCondition());
    setRetryStrategy(def.getRetryStrategy());
    Set<String> defConfigs = def.getConfigurationKeys();
    this.configurations = new TreeMap<>();
    if (defConfigs != null) {
      for (String key : defConfigs) {
        configurations.put(key, def.getConfiguration(key));
      }
    }

    if ((retryStrategy == RetryStrategy.RETRY || retryStrategy == RetryStrategy.HOLD) && maxAttempts < 2) {
      maxAttempts = 2;
    }
  }

  /**
   * Constructs a new operation instance with the given id and initial state.
   *
   * @param id
   *          the operation id
   * @param state
   *          the state
   */
  public WorkflowOperationInstance(String id, OperationState state) {
    this();
    setTemplate(id);
    setState(state);
  }

  public WorkflowOperationInstance(
          String template,
          Long jobId,
          OperationState state,
          String description,
          Map<String, String> configurations,
          boolean failOnError,
          String executeCondition,
          String exceptionHandlingWorkflow,
          Boolean abortable,
          Boolean continuable,
          Date dateStarted,
          Date dateCompleted,
          Long timeInQueue,
          int maxAttempts,
          int failedAttempts,
          String executionHost,
          RetryStrategy retryStrategy) {
    this.template = template;
    this.jobId = jobId;
    this.state = state;
    this.description = description;
    this.configurations = configurations;
    this.failOnError = failOnError;
    this.executeCondition = executeCondition;
    this.exceptionHandlingWorkflow = exceptionHandlingWorkflow;
    this.abortable = abortable;
    this.continuable = continuable;
    this.dateStarted = dateStarted;
    this.dateCompleted = dateCompleted;
    this.timeInQueue = timeInQueue;
    this.maxAttempts = maxAttempts;
    this.failedAttempts = failedAttempts;
    this.executionHost = executionHost;
    this.retryStrategy = retryStrategy;
  }

  /**
   * Sets the template
   *
   * @param template
   *          the template
   */
  public void setTemplate(String template) {
    this.template = template;
  }

  /**
   * Gets the operation type.
   *
   * @return the operation type
   */
  public String getTemplate() {
    return template;
  }

  /**
   * Gets the unique identifier for this operation, or null.
   *
   * @return the identifier, or null if this operation has not yet run
   */
  public Long getId() {
    return jobId;
  }

  /**
   * Sets the unique identifier for this operation.
   *
   * @param jobId
   *          the identifier
   */
  public void setId(Long jobId) {
    this.jobId = jobId;
  }

  /**
   * Gets the operation description
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set the operation description.
   *
   * @param description The new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * The state of this operation.
   */
  public OperationState getState() {
    return state;
  }

  /**
   * Sets the state of this operation
   *
   * @param state
   *          the state to set
   */
  public void setState(OperationState state) {
    Date now = new Date();
    if (OperationState.RUNNING.equals(state)) {
      this.dateStarted = now;
    } else if (OperationState.FAILED.equals(state) || OperationState.SUCCEEDED.equals(state)) {
      this.dateCompleted = now;
    }
    this.state = state;
  }

  /**
   * Return configuration of this workflow operation as Map.
   * Guaranteed to be not null
   *
   * @return Configuration map
   */
  public Map<String, String> getConfigurations() {
    return configurations;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#getConfiguration(java.lang.String)
   */
  @Override
  public String getConfiguration(String key) {
    if (key == null || configurations == null)
      return null;
    return configurations.get(key);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#removeConfiguration(java.lang.String)
   */
  @Override
  public void removeConfiguration(String key) {
    if (key == null || configurations == null)
      return;
    configurations.remove(key);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#setConfiguration(java.lang.String, java.lang.String)
   */
  @Override
  public void setConfiguration(String key, String value) {
    if (key == null)
      return;
    if (configurations == null)
      configurations = new TreeMap<>();

    configurations.put(key, value);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getConfigurationKeys()
   */
  @Override
  public Set<String> getConfigurationKeys() {
    if (configurations == null) {
      return Collections.emptySet();
    }
    return configurations.keySet();
  }

  /** The workflow to run if an exception is thrown while this operation is running. */
  public String getExceptionHandlingWorkflow() {
    return exceptionHandlingWorkflow;
  }

  public void setExceptionHandlingWorkflow(String exceptionHandlingWorkflow) {
    this.exceptionHandlingWorkflow = exceptionHandlingWorkflow;
  }

  /**
   * If true, this workflow will be put into a failed (or failing, if getExceptionHandlingWorkflow() is not null) state
   * when exceptions are thrown during an operation.
   */
  public boolean isFailOnError() {
    return failOnError;
  }

  public void setFailOnError(boolean failOnError) {
    this.failOnError = failOnError;
  }

  /**
   * The timestamp this operation started. If the job was queued, this can be significantly later than the date created.
   */
  public Date getDateStarted() {
    return dateStarted;
  }

  /** The number of milliseconds this operation waited in a service queue */
  public Long getTimeInQueue() {
    return timeInQueue;
  }

  public void setTimeInQueue(long timeInQueue) {
    this.timeInQueue = timeInQueue;
  }

  /** The timestamp this operation completed */
  public Date getDateCompleted() {
    return dateCompleted;
  }

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
   * @return the execution condition.
   */
  public String getExecutionCondition() {
    return executeCondition;
  }

  public void setExecutionCondition(String condition) {
    this.executeCondition = condition;
  }

  /**
   * Returns <code>true</code> if this operation can be continued by the user from an optional hold state. A return
   * value of <code>null</code> indicates that this operation instance does not have a hold state.
   *
   * @return <code>true</code> if this operation instance is continuable
   */
  public Boolean isContinuable() {
    return continuable;
  }

  /**
   * Defines whether this operation instance should be continuable from a hold state or whether it is resumed
   * automatically.
   *
   * @param continuable
   *          <code>true</code> to allow the user to resume the operation
   */
  public void setContinuable(Boolean continuable) {
    this.continuable = continuable;
  }

  /**
   * Returns <code>true</code> if this operation can be aborted by the user from an optional hold state. If a resumable
   * operation is aborted from its hold state, the workflow is put into
   * {@link org.opencastproject.workflow.api.WorkflowInstance.WorkflowState#STOPPED}. A return value of
   * <code>null</code> indicates that this operation instance does not have a hold state.
   *
   * @return <code>true</code> if this operation instance is abortable
   */
  public Boolean isAbortable() {
    return abortable;
  }

  /**
   * Defines whether this operation instance should be abortable from a hold state.
   *
   * @param abortable
   *          <code>true</code> to allow the user to cancel the operation
   */
  public void setAbortable(Boolean abortable) {
    this.abortable = abortable;
  }

  /**
   * Return the strategy to use in case of operation failure
   *
   * @return a strategy from {@link org.opencastproject.workflow.api.RetryStrategy}.
   */
  public RetryStrategy getRetryStrategy() {
    return retryStrategy;
  }

  private void setRetryStrategy(RetryStrategy retryStrategy) {
    this.retryStrategy = retryStrategy;
  }

  /**
   * Returns the number of attempts the workflow service will make to execute this operation.
   *
   * @return the maximum number of retries before failing
   */
  public int getMaxAttempts() {
    return maxAttempts;
  }

  /**
   * @param maxAttempts
   *          the maxAttempts to set
   * @throws IllegalArgumentException
   *           if maxAttempts is less than one.
   */
  private void setMaxAttempts(int maxAttempts) {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be >=1");
    }
    this.maxAttempts = maxAttempts;
  }

  /**
   * Returns the number of failed executions that have previously been attempted.
   *
   * @return the number of previous attempts
   */
  public int getFailedAttempts() {
    return failedAttempts;
  }

  public void setFailedAttempts(int failedAttempts) {
    this.failedAttempts = failedAttempts;
  }

  /**
   * Returns the current execution host
   *
   * @return the execution host
   */
  public String getExecutionHost() {
    return executionHost;
  }

  /**
   * Sets the current execution host
   *
   * @param executionHost
   *          the execution host
   */
  public void setExecutionHost(String executionHost) {
    this.executionHost = executionHost;
  }

  public WorkflowInstance getWorkflowInstance() {
    return instance;
  }

  public void setWorkflowInstance(WorkflowInstance instance) {
    this.instance = instance;
  }

  @Override
  public int hashCode() {
    return Long.valueOf(id).hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof WorkflowOperationInstance) {
      WorkflowOperationInstance other = (WorkflowOperationInstance) o;
      return other.getTemplate().equals(this.getTemplate()) && Objects.equals(other.id, this.id);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "operation:'" + template +  ", state:'" + this.state + "'";
  }
}
