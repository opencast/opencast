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

package org.opencastproject.serviceregistry.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * A wrapper for service statistics.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "statistics", namespace = "http://serviceregistry.opencastproject.org")
@XmlRootElement(name = "statistics", namespace = "http://serviceregistry.opencastproject.org")
public class JaxbServiceStatisticsList {
  /** A list of search items. */
  @XmlElement(name = "service")
  protected List<JaxbServiceStatistics> stats = new ArrayList<JaxbServiceStatistics>();

  public JaxbServiceStatisticsList() {
  }

  public JaxbServiceStatisticsList(Collection<ServiceStatistics> stats) {
    for (ServiceStatistics stat : stats)
      this.stats.add((JaxbServiceStatistics) stat);
  }

  /**
   * @return the stats
   */
  public List<JaxbServiceStatistics> getStats() {
    return stats;
  }

  /**
   * @param stats
   *          the stats to set
   */
  public void setStats(List<JaxbServiceStatistics> stats) {
    this.stats = stats;
  }
}
