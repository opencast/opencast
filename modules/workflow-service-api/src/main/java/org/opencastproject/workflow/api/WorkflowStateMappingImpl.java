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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlType(name = "state-mapping", namespace = "http://workflow.opencastproject.org")
@XmlRootElement(name = "state-mapping", namespace = "http://workflow.opencastproject.org")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowStateMappingImpl implements WorkflowStateMapping {

  @XmlJavaTypeAdapter(WorkflowInstance.WorkflowState.Adapter.class)
  @XmlAttribute(name = "state")
  protected WorkflowInstance.WorkflowState state;

  @XmlValue
  protected String value;


  /** A no-arg constructor is needed by JAXB */
  public WorkflowStateMappingImpl() {
  }

  /**
   * @param state
   *          The state to map
   * @param value
   *          The value to use for the state
   */
  public WorkflowStateMappingImpl(WorkflowInstance.WorkflowState state, String value) {
    this.state = state;
    this.value = value;
  }

  public WorkflowInstance.WorkflowState getState() {
    return state;
  }

  public void setState(WorkflowInstance.WorkflowState state) {
    this.state = state;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  static class Adapter extends XmlAdapter<WorkflowStateMappingImpl, WorkflowStateMapping> {
    public WorkflowStateMappingImpl marshal(WorkflowStateMapping mapping) throws Exception {
      return (WorkflowStateMappingImpl) mapping;
    }

    public WorkflowStateMapping unmarshal(WorkflowStateMappingImpl mapping) throws Exception {
      return mapping;
    }
  }

  @Override
  public String toString() {
    return "state mapping:'" + state.name() + " : " + value + "'";
  }

}
