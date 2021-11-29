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

import java.util.List;

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
@XmlRootElement(name = "workflowoperations", namespace = "http://workflow.opencastproject.org")
public class WorkflowOperationInstancesListImpl implements WorkflowOperationInstancesList {

  @XmlElement(name = "operation")
  @XmlElementWrapper(name = "operations")
  private List<WorkflowOperationInstance> operations;

  /**
   * A no-arg constructor needed by JAXB
   */
  public WorkflowOperationInstancesListImpl() {
  }

  /**
   * Constructor
   *
   * @param operations
   */
  public WorkflowOperationInstancesListImpl(List<WorkflowOperationInstance> operations) {
    this.operations = operations;
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowOperationInstancesList#get()
   */
  @Override
  public List<WorkflowOperationInstance> get() {
    return operations;
  }

  /**
   * {@inheritDoc}
   *
   * @see WorkflowOperationInstancesList#set(List<WorkflowOperationInstance>)
   */
  @Override
  public void set(List<WorkflowOperationInstance> operations) {
    this.operations = operations;
  }

  public static class Adapter extends XmlAdapter<WorkflowOperationInstancesListImpl, WorkflowOperationInstancesList> {
    public WorkflowOperationInstancesListImpl marshal(WorkflowOperationInstancesList list) throws Exception {
      return (WorkflowOperationInstancesListImpl) list;
    }

    public WorkflowOperationInstancesList unmarshal(WorkflowOperationInstancesListImpl list) throws Exception {
      return list;
    }
  }
}
