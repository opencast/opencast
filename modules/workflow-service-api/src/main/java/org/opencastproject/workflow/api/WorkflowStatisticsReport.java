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
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * This class encapsualtes statistics for the workflow service.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "statistics_report", namespace = "http://workflow.opencastproject.org")
@XmlType(name = "statistics_report", namespace = "http://workflow.opencastproject.org")
public class WorkflowStatisticsReport extends WorkflowStatistics {

  /**
   * The workflow definition reports
   */
  @XmlElementWrapper(name = "definitions")
  @XmlElement(name = "definition")
  protected Set<WorkflowDefinitionReport> definitionsReports = new TreeSet<>((report1, report2) -> {
    if (report1.equals(report2)) {
      return 0;
    }
    return report1.getId().compareTo(report2.getId());
  });

  /**
   * @return the definitions
   */
  public Set<WorkflowDefinitionReport> getDefinitionsReports() {
    return definitionsReports;
  }

  /**
   *
   * @param definitionsReport
   */
  public void addDefinitionsReport(WorkflowDefinitionReport definitionsReport) {
    definitionsReports.add(definitionsReport);
  }

}
