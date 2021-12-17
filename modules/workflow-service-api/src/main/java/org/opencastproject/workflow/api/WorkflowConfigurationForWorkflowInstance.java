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

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A configuration value for workflow operations.
 */
@Entity(name = "WorkflowConfigurationForWorkflowInstance")
@Access(AccessType.FIELD)
@Table(name = "oc_workflow_instance_configuration")
public class WorkflowConfigurationForWorkflowInstance implements WorkflowConfiguration, Comparable<WorkflowConfiguration> {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "key_part")
  protected String key;
  
  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "value_part")
  protected String value;

  @ManyToOne(fetch = FetchType.LAZY)
  private WorkflowInstance instance;

  public WorkflowConfigurationForWorkflowInstance() {
  }

  public WorkflowConfigurationForWorkflowInstance(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
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
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    WorkflowConfigurationForWorkflowInstance other = (WorkflowConfigurationForWorkflowInstance) obj;
    if (key == null) {
      if (other.key != null)
        return false;
    } else if (!key.equals(other.key))
      return false;
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "workflow configuration " + this.key + "=" + this.value;
  }

  /**
   * {@inheritDoc}
   *
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(WorkflowConfiguration o) {
    return this.key.compareTo(o.getKey());
  }
}
