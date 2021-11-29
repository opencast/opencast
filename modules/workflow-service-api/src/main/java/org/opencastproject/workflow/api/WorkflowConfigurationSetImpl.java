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

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * The search result represents a set of result items that has been compiled as a result for a search operation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "workflowconfig", namespace = "http://workflow.opencastproject.org")
public class WorkflowConfigurationSetImpl implements WorkflowConfigurationSet {

  @XmlElement(name = "configuration")
  @XmlElementWrapper(name = "configurations")
  private Set<WorkflowConfiguration> configurations;

  /**
   * A no-arg constructor needed by JAXB
   */
  public WorkflowConfigurationSetImpl() {
  }

  /**
   * Constructor
   *
   * @param configurations
   */
  public WorkflowConfigurationSetImpl(Set<WorkflowConfiguration> configurations) {
    this.configurations = configurations;
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowConfigurationSet#get()
   */
  @Override
  public Set<WorkflowConfiguration> get() {
    return configurations;
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowConfigurationSet#set(Set<WorkflowConfiguration>)
   */
  @Override
  public void set(Set<WorkflowConfiguration> configurations) {
    this.configurations = configurations;
  }

  public static class Adapter extends XmlAdapter<WorkflowConfigurationSetImpl, WorkflowConfigurationSet> {
    public WorkflowConfigurationSetImpl marshal(WorkflowConfigurationSet set) throws Exception {
      return (WorkflowConfigurationSetImpl) set;
    }

    public WorkflowConfigurationSet unmarshal(WorkflowConfigurationSetImpl set) throws Exception {
      return set;
    }
  }
}
