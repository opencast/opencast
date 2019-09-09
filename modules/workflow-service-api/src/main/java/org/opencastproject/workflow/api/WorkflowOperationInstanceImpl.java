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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * A JAXB-annotated implementation of {@link WorkflowOperationInstance}
 */
@XmlType(name = "operation-instance", namespace = "http://workflow.opencastproject.org")
@XmlRootElement(name = "operation-instance", namespace = "http://workflow.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
public class WorkflowOperationInstanceImpl implements WorkflowOperationInstance {

  static class DateAdapter extends XmlAdapter<Long, Date> {
    /**
     * {@inheritDoc}
     *
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
     */
    @Override
    public Long marshal(Date v) throws Exception {
      return v == null ? null : v.getTime();
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
     */
    @Override
    public Date unmarshal(Long v) throws Exception {
      return v == null ? null : new Date(v);
    }
  }

  @XmlAttribute(name = "id")
  protected String template;

  @XmlAttribute(name = "job")
  protected Long jobId;

  @XmlAttribute(name = "state")
  protected OperationState state;

  @XmlAttribute(name = "description")
  protected String description;

  @XmlElement(name = "configuration")
  @XmlElementWrapper(name = "configurations")
  protected Set<WorkflowConfiguration> configurations;

  @XmlElement(name = "holdurl")
  protected String holdStateUserInterfaceUrl;

  @XmlElement(name = "hold-action-title")
  protected String holdActionTitle;

  @XmlAttribute(name = "fail-on-error")
  protected boolean failWorkflowOnException;

  @XmlAttribute(name = "if")
  protected String executeCondition;

  @XmlAttribute(name = "unless")
  protected String skipCondition;

  @XmlAttribute(name = "exception-handler-workflow")
  protected String exceptionHandlingWorkflow;

  @XmlAttribute(name = "abortable")
  protected Boolean abortable;

  @XmlAttribute(name = "continuable")
  protected Boolean continuable;

  @XmlJavaTypeAdapter(WorkflowOperationInstanceImpl.DateAdapter.class)
  @XmlElement(name = "started")
  protected Date dateStarted;

  @XmlJavaTypeAdapter(WorkflowOperationInstanceImpl.DateAdapter.class)
  @XmlElement(name = "completed")
  protected Date dateCompleted;

  @XmlElement(name = "time-in-queue")
  protected Long timeInQueue;

  @XmlAttribute(name = "max-attempts")
  protected int maxAttempts;

  @XmlAttribute(name = "failed-attempts")
  protected int failedAttempts;

  @XmlAttribute(name = "execution-host")
  protected String executionHost;

  @XmlElementWrapper(name = "execution-history")
  @XmlElement(name = "execution-history-entry")
  protected List<Long> executionHistory = new ArrayList<Long>();

  @XmlJavaTypeAdapter(RetryStrategy.Adapter.class)
  @XmlAttribute(name = "retry-strategy")
  protected RetryStrategy retryStrategy;

  /** The position of this operation in the workflow instance */
  protected int position;

  /**
   * No-arg constructor needed for JAXB serialization
   */
  public WorkflowOperationInstanceImpl() {
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
  public WorkflowOperationInstanceImpl(WorkflowOperationDefinition def, int position) {
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
    this.configurations = new TreeSet<WorkflowConfiguration>();
    if (defConfigs != null) {
      for (String key : defConfigs) {
        configurations.add(new WorkflowConfigurationImpl(key, def.getConfiguration(key)));
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
  public WorkflowOperationInstanceImpl(String id, OperationState state) {
    this();
    setTemplate(id);
    setState(state);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getId()
   */
  @Override
  public Long getId() {
    return jobId;
  }

  /**
   * Sets the job identifier
   *
   * @param jobId
   *          the job identifier
   */
  @Override
  public void setId(Long jobId) {
    this.jobId = jobId;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getTemplate()
   */
  @Override
  public String getTemplate() {
    return template;
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

  @Override
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public void setExecutionCondition(String condition) {
    this.executeCondition = condition;
  }

  @Override
  public String getExecutionCondition() {
    return executeCondition;
  }

  public void setSkipCondition(String condition) {
    this.skipCondition = condition;
  }

  @Override
  public String getSkipCondition() {
    return skipCondition;
  }

  static class Adapter extends XmlAdapter<WorkflowOperationInstanceImpl, WorkflowOperationInstance> {
    @Override
    public WorkflowOperationInstanceImpl marshal(WorkflowOperationInstance op) throws Exception {
      return (WorkflowOperationInstanceImpl) op;
    }

    @Override
    public WorkflowOperationInstance unmarshal(WorkflowOperationInstanceImpl op) throws Exception {
      return op;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getState()
   */
  @Override
  public OperationState getState() {
    return state;
  }

  @Override
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

  public Set<WorkflowConfiguration> getConfigurations() {
    return configurations;
  }

  public void setConfiguration(Set<WorkflowConfiguration> configurations) {
    this.configurations = configurations;
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
    for (Iterator<WorkflowConfiguration> configIter = configurations.iterator(); configIter.hasNext();) {
      WorkflowConfiguration config = configIter.next();
      if (config.getKey().equals(key)) {
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
      configurations = new TreeSet<WorkflowConfiguration>();

    for (WorkflowConfiguration config : configurations) {
      if (config.getKey().equals(key)) {
        ((WorkflowConfigurationImpl) config).setValue(value);
        return;
      }
    }
    // No configurations were found, so add a new one
    configurations.add(new WorkflowConfigurationImpl(key, value));
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
      for (WorkflowConfiguration config : configurations) {
        keys.add(config.getKey());
      }
    }
    return keys;
  }

  /**
   * @return the holdStateUserInterfaceUrl
   */
  @Override
  public String getHoldStateUserInterfaceUrl() {
    return holdStateUserInterfaceUrl;
  }

  /**
   * @param holdStateUserInterfaceUrl
   *          the holdStateUserInterfaceUrl to set
   */
  public void setHoldStateUserInterfaceUrl(String holdStateUserInterfaceUrl) {
    this.holdStateUserInterfaceUrl = holdStateUserInterfaceUrl;
  }

  /**
   * Set the title for the link to this operations hold state UI, a default String if no title is set.
   */
  public void setHoldActionTitle(String title) {
    this.holdActionTitle = title;
  }

  /**
   * Returns the title for the link to this operations hold state UI, a default String if no title is set.
   *
   * @return title to be displayed
   */
  @Override
  public String getHoldActionTitle() {
    return holdActionTitle;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getExceptionHandlingWorkflow()
   */
  @Override
  public String getExceptionHandlingWorkflow() {
    return exceptionHandlingWorkflow;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#isFailWorkflowOnException()
   */
  @Override
  public boolean isFailWorkflowOnException() {
    return failWorkflowOnException;
  }

  /**
   * @param failWorkflowOnException
   *          the failWorkflowOnException to set
   */
  public void setFailWorkflowOnException(boolean failWorkflowOnException) {
    this.failWorkflowOnException = failWorkflowOnException;
  }

  /**
   * @param exceptionHandlingWorkflow
   *          the exceptionHandlingWorkflow to set
   */
  public void setExceptionHandlingWorkflow(String exceptionHandlingWorkflow) {
    this.exceptionHandlingWorkflow = exceptionHandlingWorkflow;
  }

  /**
   * @return the dateStarted
   */
  @Override
  public Date getDateStarted() {
    return dateStarted;
  }

  /**
   * @param dateStarted
   *          the dateStarted to set
   */
  public void setDateStarted(Date dateStarted) {
    this.dateStarted = dateStarted;
  }

  /**
   * @return the dateCompleted
   */
  @Override
  public Date getDateCompleted() {
    return dateCompleted;
  }

  /**
   * @param dateCompleted
   *          the dateCompleted to set
   */
  public void setDateCompleted(Date dateCompleted) {
    this.dateCompleted = dateCompleted;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getTimeInQueue()
   */
  @Override
  public Long getTimeInQueue() {
    return timeInQueue;
  }

  /**
   * @param timeInQueue
   *          the timeInQueue to set
   */
  public void setTimeInQueue(long timeInQueue) {
    this.timeInQueue = timeInQueue;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getPosition()
   */
  @Override
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

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#isAbortable()
   */
  @Override
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
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#isContinuable()
   */
  @Override
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
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getFailedAttempts()
   */
  @Override
  public int getFailedAttempts() {
    return failedAttempts;
  }

  /**
   * @param failedAttempts
   *          the failedAttempts to set
   */
  public void setFailedAttempts(int failedAttempts) {
    this.failedAttempts = failedAttempts;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getRetryStrategy()
   */
  @Override
  public RetryStrategy getRetryStrategy() {
    return retryStrategy;
  }

  /**
   * @param retryStrategy
   *          the retry strategy
   */
  public void setRetryStrategy(RetryStrategy retryStrategy) {
    this.retryStrategy = retryStrategy;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getExecutionHost()
   */
  @Override
  public String getExecutionHost() {
    return executionHost;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#setExecutionHost(java.lang.String)
   */
  @Override
  public void setExecutionHost(String executionHost) {
    this.executionHost = executionHost;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getExecutionHistory()
   */
  @Override
  public List<Long> getExecutionHistory() {
    return executionHistory;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#addToExecutionHistory(long)
   */
  @Override
  public void addToExecutionHistory(long jobId) {
    executionHistory.add(jobId);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationInstance#getMaxAttempts()
   */
  @Override
  public int getMaxAttempts() {
    return maxAttempts;
  }

  /**
   * @param maxAttempts
   *          the maxAttempts to set
   * @throws IllegalArgumentException
   *           if maxAttempts is less than one.
   */
  public void setMaxAttempts(int maxAttempts) {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be >=1");
    }
    this.maxAttempts = maxAttempts;
  }

}
