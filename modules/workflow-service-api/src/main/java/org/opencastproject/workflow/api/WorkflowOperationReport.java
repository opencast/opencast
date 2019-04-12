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

/**
 * Statistics for a specific workflow operation within a given worflow definition
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "operation_report", namespace = "http://workflow.opencastproject.org")
@XmlType(name = "operation_report", namespace = "http://workflow.opencastproject.org")
public class WorkflowOperationReport extends WorkflowStatistics {
  /**
   * The workflow operation id
   */
  @XmlAttribute
  private String id;

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * constructor
   *
   * @param id
   */
  public WorkflowOperationReport(String id) {
    this.id = id;
  }

  /**
   * default constructor
   */
  public WorkflowOperationReport() {
  }

}
