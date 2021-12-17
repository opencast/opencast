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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A workflow operation belonging to a workflow instance.
 */
@Entity(name = "WorkflowOperationInstance")
@Access(AccessType.FIELD)
@Table(name = "oc_workflow_operation_instance")
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
  protected String description;

  @OneToMany(
          mappedBy = "operationInstance",
          cascade = CascadeType.ALL,
          orphanRemoval = true,
          fetch = FetchType.LAZY
  )
  protected Set<WorkflowConfigurationForOperationInstance> configurations;

  @Column(name = "holdurl")
  protected String holdStateUserInterfaceUrl;

  @Column(name = "holdActionTitle")
  protected String holdActionTitle;

  @Column(name = "failOnError")
  protected boolean failWorkflowOnException;

  @Column(name = "if_condition")
  protected String executeCondition;

  @Column(name = "unless_condition")
  protected String skipCondition;

  @Column(name = "exceptionHandlerWorkflow")
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

  @Column(name = "timeInQueue")
  protected Long timeInQueue;

  @Column(name = "maxAttempts")
  protected int maxAttempts;

  @Column(name = "failedAttempts")
  protected int failedAttempts;

  @Column(name = "executionHost")
  protected String executionHost;

  @ElementCollection
  @CollectionTable(
          name = "executionHistory",
          joinColumns = @JoinColumn(name = "operationId"))
  @Column(name = "executionHistoryEntry")
  protected List<Long> executionHistory = new ArrayList<Long>();

  @Column(name = "retryStrategy", length = 128)
  protected RetryStrategy retryStrategy;

  @ManyToOne(fetch = FetchType.LAZY)
  private WorkflowInstance instance;

  /** The position of this operation in the workflow instance */
  protected int position;

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
   * @param position
   *          the operation's position within the workflow
   */
  public WorkflowOperationInstance(WorkflowOperationDefinition def, int position) {
    this();
    this.position = position;
    setTemplate(def.getId());
    setState(OperationState.INSTANTIATED);
    setDescription(def.getDescription());
    setMaxAttempts(def.getMaxAttempts());
    setFailWorkflowOnException(def.isFailWorkflowOnException());
    setExceptionHandlingWorkflow(def.getExceptionHandlingWorkflow());
    setExecutionCondition(def.getExecutionCondition());
    setSkipCondition(def.getSkipCondition());
    setRetryStrategy(def.getRetryStrategy());
    Set<String> defConfigs = def.getConfigurationKeys();
    this.configurations = new TreeSet<WorkflowConfigurationForOperationInstance>();
    if (defConfigs != null) {
      for (String key : defConfigs) {
        addConfiguration(key, def.getConfiguration(key));
//        configurations.add(new WorkflowConfigurationForOperationInstance(key, def.getConfiguration(key)));
      }
    }

    switch (retryStrategy) {
      case RETRY:
        if (maxAttempts < 2)
          maxAttempts = 2;
        break;
      case HOLD:
        // Can hold and retry until max-attempts
        if (maxAttempts < 2)
          maxAttempts = 2;
        break;
      default:
        // Nothing to do
        break;
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
          long id,
          String template,
          Long jobId,
          OperationState state,
          String description,
          Set<WorkflowConfigurationForOperationInstance> configurations,
          String holdStateUserInterfaceUrl,
          String holdActionTitle,
          boolean failWorkflowOnException,
          String executeCondition,
          String skipCondition,
          String exceptionHandlingWorkflow,
          Boolean abortable,
          Boolean continuable,
          Date dateStarted,
          Date dateCompleted,
          Long timeInQueue,
          int maxAttempts,
          int failedAttempts,
          String executionHost,
          List<Long> executionHistory,
          RetryStrategy retryStrategy) {
    this.id = id;
    this.template = template;
    this.jobId = jobId;
    this.state = state;
    this.description = description;
    this.configurations = configurations;
    this.holdStateUserInterfaceUrl = holdStateUserInterfaceUrl;
    this.holdActionTitle = holdActionTitle;
    this.failWorkflowOnException = failWorkflowOnException;
    this.executeCondition = executeCondition;
    this.skipCondition = skipCondition;
    this.exceptionHandlingWorkflow = exceptionHandlingWorkflow;
    this.abortable = abortable;
    this.continuable = continuable;
    this.dateStarted = dateStarted;
    this.dateCompleted = dateCompleted;
    this.timeInQueue = timeInQueue;
    this.maxAttempts = maxAttempts;
    this.failedAttempts = failedAttempts;
    this.executionHost = executionHost;
    this.executionHistory = executionHistory;
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

  private void setDescription(String description) {
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
    } else if (OperationState.FAILED.equals(state)) {
      this.dateCompleted = now;
    } else if (OperationState.SUCCEEDED.equals(state)) {
      this.dateCompleted = now;
    }
    this.state = state;
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
    for (WorkflowConfiguration config : configurations) {
      if (config.getKey().equals(key))
        return config.getValue();
    }
    return null;
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
    for (Iterator<WorkflowConfigurationForOperationInstance> configIter = configurations.iterator(); configIter.hasNext();) {
      WorkflowConfigurationForOperationInstance config = configIter.next();
      if (config.getKey().equals(key)) {
        config.setWorkflowOperationInstance(null);
        configIter.remove();
        return;
      }
    }
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
      configurations = new TreeSet<WorkflowConfigurationForOperationInstance>();

    for (WorkflowConfigurationForOperationInstance config : configurations) {
      if (config.getKey().equals(key)) {
        ((WorkflowConfigurationForOperationInstance) config).setValue(value);
        return;
      }
    }
    // No configurations were found, so add a new one
    addConfiguration(key, value);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getConfigurationKeys()
   */
  @Override
  public Set<String> getConfigurationKeys() {
    Set<String> keys = new TreeSet<String>();
    if (configurations != null && !configurations.isEmpty()) {
      for (WorkflowConfigurationForOperationInstance config : configurations) {
        keys.add(config.getKey());
      }
    }
    return keys;
  }

  private void addConfiguration(String key, String value) {
    WorkflowConfigurationForOperationInstance newConfig = new WorkflowConfigurationForOperationInstance(key, value);
    newConfig.setWorkflowOperationInstance(this);
    configurations.add(newConfig);
  }

  /**
   * Gets the URL for the hold state.
   *
   * @return the URL of the hold state, if any, for this operation
   */
  public String getHoldStateUserInterfaceUrl() {
    return holdStateUserInterfaceUrl;
  }

  public void setHoldStateUserInterfaceUrl(String holdStateUserInterfaceUrl) {
    this.holdStateUserInterfaceUrl = holdStateUserInterfaceUrl;
  }

  /**
   * Returns the title for the link to this operations hold state UI, a default String if no title is set.
   *
   * @return title to be displayed
   */
  public String getHoldActionTitle() {
    return holdActionTitle;
  }

  /**
   * Set the title for the link to this operations hold state UI, a default String if no title is set.
   */
  public void setHoldActionTitle(String title) {
    this.holdActionTitle = title;
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
  public boolean isFailWorkflowOnException() {
    return failWorkflowOnException;
  }

  public void setFailWorkflowOnException(boolean failWorkflowOnException) {
    this.failWorkflowOnException = failWorkflowOnException;
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

  /** The position of this workflow operation in the workflow instance */
  public int getPosition() {
    return position;
  }

  /**
   * Sets the workflow operation's position within the workflow.
   *
   * @param position
   *          the position
   */
  public void setPosition(int position) {
    this.position = position;
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
   * @return the excecution condition.
   */
  public String getExecutionCondition() {
    return executeCondition;
  }

  public void setExecutionCondition(String condition) {
    this.executeCondition = condition;
  }

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
  public String getSkipCondition() {
    return skipCondition;
  }

  private void setSkipCondition(String condition) {
    this.skipCondition = condition;
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

  /**
   * Adds a failed job to the execution history
   *
   * @param jobId
   *          the failed job id
   */
  public void addToExecutionHistory(long jobId) {
    executionHistory.add(jobId);
  }

  /**
   * Returns a list of failed job executions
   *
   * @return the execution history
   */
  public List<Long> getExecutionHistory() {
    return executionHistory;
  }

  public WorkflowInstance getWorkflowInstance() {
    return instance;
  }

  public void setWorkflowInstance(WorkflowInstance instance) {
    this.instance = instance;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return position;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o instanceof WorkflowOperationInstance) {
      WorkflowOperationInstance other = (WorkflowOperationInstance) o;
      return other.getTemplate().equals(this.getTemplate()) && other.getPosition() == this.position;
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "operation:'" + template + "', position:" + position + ", state:'" + this.state + "'";
  }
}
