/*
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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
public class JaxbWorkflowOperationInstance {

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
  private Long id;

  @XmlAttribute(name = "template")
  protected String template;

  @XmlAttribute(name = "job")
  protected Long jobId;

  @XmlAttribute(name = "state")
  protected WorkflowOperationInstance.OperationState state;

  @XmlAttribute(name = "description")
  protected String description;

  @XmlElement(name = "configuration")
  @XmlElementWrapper(name = "configurations")
  protected Set<JaxbWorkflowConfiguration> configurations = null;

  @XmlAttribute(name = "fail-on-error")
  protected boolean failOnError;

  @XmlAttribute(name = "if")
  protected String executeCondition;

  @XmlAttribute(name = "exception-handler-workflow")
  protected String exceptionHandlingWorkflow;

  @XmlAttribute(name = "abortable")
  protected Boolean abortable;

  @XmlAttribute(name = "continuable")
  protected Boolean continuable;

  @XmlJavaTypeAdapter(JaxbWorkflowOperationInstance.DateAdapter.class)
  @XmlElement(name = "started")
  protected Date dateStarted;

  @XmlJavaTypeAdapter(JaxbWorkflowOperationInstance.DateAdapter.class)
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

  @XmlJavaTypeAdapter(RetryStrategy.Adapter.class)
  @XmlAttribute(name = "retry-strategy")
  protected RetryStrategy retryStrategy;

  /**
   * No-arg constructor needed for JAXB serialization
   */
  public JaxbWorkflowOperationInstance() {
    this.maxAttempts = 1;
    this.retryStrategy = RetryStrategy.NONE;
  }

  /**
   * Builds a new workflow operation instance based on another workflow operation.
   *
   */
  public JaxbWorkflowOperationInstance(WorkflowOperationInstance operation) {
    this();
    this.id = operation.getId();
    this.template = operation.getTemplate();
    this.jobId = operation.getId();
    this.state = operation.getState();
    this.description = operation.getDescription();
    if (operation.getConfigurations() != null) {
      this.configurations = operation.getConfigurations().entrySet()
          .stream()
          .map(config -> new JaxbWorkflowConfiguration(config.getKey(), config.getValue()))
          .collect(Collectors.toSet());
    }
    this.failOnError = operation.isFailOnError();
    this.executeCondition = operation.getExecutionCondition();
    this.exceptionHandlingWorkflow = operation.getExceptionHandlingWorkflow();
    this.abortable = operation.isAbortable();
    this.continuable = operation.isContinuable();
    this.dateStarted = operation.getDateStarted();
    this.dateCompleted = operation.getDateCompleted();
    this.timeInQueue = operation.getTimeInQueue();
    this.maxAttempts = operation.getMaxAttempts();
    this.failedAttempts = operation.getFailedAttempts();
    this.executionHost = operation.getExecutionHost();
    this.retryStrategy = operation.getRetryStrategy();
  }

  public WorkflowOperationInstance toWorkflowOperationInstance() {
    return new WorkflowOperationInstance(template, jobId, state, description,
            Optional.ofNullable(configurations).orElseGet(Collections::emptySet)
                    .stream()
                    .collect(Collectors.toMap(JaxbWorkflowConfiguration::getKey, JaxbWorkflowConfiguration::getValue)),
        failOnError, executeCondition,
            exceptionHandlingWorkflow, abortable, continuable, dateStarted, dateCompleted, timeInQueue, maxAttempts, failedAttempts,
            executionHost, retryStrategy);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;

    if (o == null || getClass() != o.getClass())
      return false;

    JaxbWorkflowOperationInstance jaxbWorkflowOperationInstance = (JaxbWorkflowOperationInstance) o;

    return new EqualsBuilder()
        .append(template, jaxbWorkflowOperationInstance.template)
        .append(jobId, jaxbWorkflowOperationInstance.jobId)
        .append(state, jaxbWorkflowOperationInstance.state)
        .append(description, jaxbWorkflowOperationInstance.description)
        .append(configurations, jaxbWorkflowOperationInstance.configurations)
        .append(failOnError, jaxbWorkflowOperationInstance.failOnError)
        .append(executeCondition, jaxbWorkflowOperationInstance.executeCondition)
        .append(exceptionHandlingWorkflow, jaxbWorkflowOperationInstance.exceptionHandlingWorkflow)
        .append(abortable, jaxbWorkflowOperationInstance.abortable)
        .append(continuable, jaxbWorkflowOperationInstance.continuable)
        .append(dateStarted, jaxbWorkflowOperationInstance.dateStarted)
        .append(dateCompleted, jaxbWorkflowOperationInstance.dateCompleted)
        .append(timeInQueue, jaxbWorkflowOperationInstance.timeInQueue)
        .append(maxAttempts, jaxbWorkflowOperationInstance.maxAttempts)
        .append(failedAttempts, jaxbWorkflowOperationInstance.failedAttempts)
        .append(executionHost, jaxbWorkflowOperationInstance.executionHost)
        .append(retryStrategy, jaxbWorkflowOperationInstance.retryStrategy)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(template)
        .append(jobId)
        .append(state)
        .append(description)
        .append(configurations)
        .append(failOnError)
        .append(executeCondition)
        .append(exceptionHandlingWorkflow)
        .append(abortable)
        .append(continuable)
        .append(dateStarted)
        .append(dateCompleted)
        .append(timeInQueue)
        .append(maxAttempts)
        .append(failedAttempts)
        .append(executionHost)
        .append(retryStrategy)
        .toHashCode();
  }
}
