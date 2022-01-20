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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * See {@link WorkflowOperationDefinition}
 */
@XmlType(name = "operation-definition", namespace = "http://workflow.opencastproject.org")
@XmlRootElement(name = "operation-definition", namespace = "http://workflow.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowOperationDefinitionImpl implements WorkflowOperationDefinition {

  @XmlAttribute(name = "id")
  protected String id;

  @XmlAttribute(name = "description")
  protected String description;

  @XmlAttribute(name = "fail-on-error")
  protected boolean failWorkflowOnException;

  @XmlAttribute(name = "if")
  protected String executeCondition;

  @XmlAttribute(name = "unless")
  protected String skipCondition;

  @XmlAttribute(name = "exception-handler-workflow")
  protected String exceptionHandlingWorkflow;

  @XmlElement(name = "configurations")
  @XmlJavaTypeAdapter(ConfigurationsXmlAdapter.class)
  protected Map<String, String> configurations;

  @XmlAttribute(name = "max-attempts")
  protected int maxAttempts;

  @XmlJavaTypeAdapter(RetryStrategy.Adapter.class)
  @XmlAttribute(name = "retry-strategy")
  protected RetryStrategy retryStrategy;

  /** A no-arg constructor is needed by JAXB */
  public WorkflowOperationDefinitionImpl() {
    super();
    this.maxAttempts = 1;
    this.failWorkflowOnException = true;
    this.retryStrategy = RetryStrategy.NONE;
  }

  /**
   * @param id
   *          The unique name of this operation
   * @param description
   *          The description of what this operation does
   * @param exceptionHandlingWorkflow
   *          The workflow to run when a workflow operation exception is thrown
   * @param failWorkflowOnException
   *          Whether an exception thrown by this operation should fail the entire {@link WorkflowInstance}
   */
  public WorkflowOperationDefinitionImpl(String id, String description, String exceptionHandlingWorkflow,
          boolean failWorkflowOnException) {
    this();
    this.id = id;
    this.description = description;
    this.exceptionHandlingWorkflow = exceptionHandlingWorkflow;
    this.failWorkflowOnException = failWorkflowOnException;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setExecutionCondition(String condition) {
    this.executeCondition = condition;
  }

  public String getExecutionCondition() {
    return executeCondition;
  }

  public void setSkipCondition(String condition) {
    this.skipCondition = condition;
  }

  public String getSkipCondition() {
    return skipCondition;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationDefinition#getExceptionHandlingWorkflow()
   */
  public String getExceptionHandlingWorkflow() {
    return exceptionHandlingWorkflow;
  }

  public void setExceptionHandlingWorkflow(String exceptionHandlingWorkflow) {
    this.exceptionHandlingWorkflow = exceptionHandlingWorkflow;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationDefinition#isFailWorkflowOnException()
   */
  public boolean isFailWorkflowOnException() {
    return failWorkflowOnException;
  }

  public void setFailWorkflowOnException(boolean failWorkflowOnException) {
    this.failWorkflowOnException = failWorkflowOnException;
  }

  /**
   * Allows JAXB handling of {@link WorkflowOperationDefinition} interfaces.
   */
  static class Adapter extends XmlAdapter<WorkflowOperationDefinitionImpl, WorkflowOperationDefinition> {
    public WorkflowOperationDefinitionImpl marshal(WorkflowOperationDefinition op) throws Exception {
      return (WorkflowOperationDefinitionImpl) op;
    }

    public WorkflowOperationDefinition unmarshal(WorkflowOperationDefinitionImpl op) throws Exception {
      return op;
    }
  }

  /**
   * JAXB properties xml adapter class.
   */
  private static class ConfigurationsXmlAdapter extends XmlAdapter<ConfigurationsAdapter, Map<String, String>> {

    @Override
    public Map<String, String> unmarshal(ConfigurationsAdapter ca) throws Exception {
      Map<String, String> configurations = new HashMap<>();
      if (ca != null) {
        for (WorkflowOperationDefinitionImpl.Configuration c : ca.configurationsList) {
          configurations.put(c.key, c.value);
        }
      }
      return configurations;
    }

    @Override
    public ConfigurationsAdapter marshal(Map<String, String> c) throws Exception {
      if (c == null || c.size() == 0) return null;

      ConfigurationsAdapter ca = new ConfigurationsAdapter();
      for (String key : c.keySet()) {
        ca.configurationsList.add(new WorkflowOperationDefinitionImpl.Configuration(key, c.get(key)));
      }
      return ca;
    }
  }

  /**
   * Properties map to list of entries adapter class.
   */
  private static class ConfigurationsAdapter {
    @XmlElement(name = "configuration")
    private List<Configuration> configurationsList;

    ConfigurationsAdapter() {
      this(new LinkedList<Configuration>());
    }

    ConfigurationsAdapter(List<Configuration> configurationsList) {
      this.configurationsList = configurationsList;
    }
  }

  /**
   * Properties entry adapter class.
   */
  private static class Configuration {
    @XmlAttribute(name = "key")
    private String key;
    @XmlValue
    private String value;

    Configuration() {
      // Default constructor
    }

    Configuration(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }


  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#getConfiguration(java.lang.String)
   */
  public String getConfiguration(String key) {
    if (key == null || configurations == null)
      return null;
    return configurations.get(key);
//    for (JaxbWorkflowConfiguration config : configurations) {
//      if (config.getKey().equals(key))
//        return config.getValue();
//    }
//    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#removeConfiguration(java.lang.String)
   */
  public void removeConfiguration(String key) {
    if (key == null || configurations == null)
      return;
    configurations.remove(key);
//    for (Iterator<JaxbWorkflowConfiguration> configIter = configurations.iterator(); configIter.hasNext();) {
//      JaxbWorkflowConfiguration config = configIter.next();
//      if (config.getKey().equals(key)) {
//        configIter.remove();
//        return;
//      }
//    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowInstance#setConfiguration(java.lang.String, java.lang.String)
   */
  public void setConfiguration(String key, String value) {
    if (key == null || configurations == null)
      return;
    configurations.put(key, value);
//    for (JaxbWorkflowConfiguration config : configurations) {
//      if (config.getKey().equals(key)) {
//        config.setValue(value);
//        return;
//      }
//    }
//    // No configurations were found, so add a new one
//    configurations.add(new JaxbWorkflowConfiguration(key, value));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationDefinition#getConfigurationKeys()
   */
  @Override
  public Set<String> getConfigurationKeys() {
    Set<String> set = new TreeSet<String>();
    if (configurations != null) {
      for (String key : configurations.keySet()) {
        set.add(key);
      }
    }
    return set;
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationDefinition#getRetryStrategy()
   */
  @Override
  public RetryStrategy getRetryStrategy() {
    return retryStrategy;
  }

  /**
   * @param retryStrategy
   *          the retry strategy to set
   */
  public void setRetryStrategy(RetryStrategy retryStrategy) {
    this.retryStrategy = retryStrategy;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationDefinition#getMaxAttempts()
   */
  @Override
  public int getMaxAttempts() {
    return maxAttempts;
  }

  /**
   * @param maxAttempts
   *          the maxAttempts to set
   */
  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "operation definition:'" + id + "'";
  }

}
