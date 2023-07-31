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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * The search result represents a set of result items that has been compiled as a result for a search operation.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "workflows", namespace = "http://workflow.opencastproject.org")
public class WorkflowSetImpl implements WorkflowSet {

  /** A list of search items. */
  @XmlElement(name = "workflow")
  private List<JaxbWorkflowInstance> resultSet = null;

  /**
   * A no-arg constructor needed by JAXB
   */
  public WorkflowSetImpl() {
  }

  public WorkflowSetImpl(List<WorkflowInstance> workflows) {
    this.resultSet = workflows.stream()
        .map(JaxbWorkflowInstance::new)
        .collect(Collectors.toList());
  }


  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowSet#getItems()
   */
  @Override
  public List<WorkflowInstance> getItems() {
    if (resultSet == null) {
      return Collections.emptyList();
    }
    return resultSet.stream()
        .map(JaxbWorkflowInstance::toWorkflowInstance)
        .collect(Collectors.toList());
  }

  /**
   * Adds an item to the result set.
   *
   * @param item
   *          the item to add
   */
  public void addItem(WorkflowInstance item) {
    if (item == null)
      throw new IllegalArgumentException("Parameter item cannot be null");
    if (resultSet == null)
      resultSet = new ArrayList<JaxbWorkflowInstance>();
    resultSet.add(new JaxbWorkflowInstance(item));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowSet#size()
   */
  @Override
  public long size() {
    return resultSet != null ? resultSet.size() : 0;
  }

  public static class Adapter extends XmlAdapter<WorkflowSetImpl, WorkflowSet> {
    public WorkflowSetImpl marshal(WorkflowSet set) throws Exception {
      return (WorkflowSetImpl) set;
    }

    public WorkflowSet unmarshal(WorkflowSetImpl set) throws Exception {
      return set;
    }
  }

}
