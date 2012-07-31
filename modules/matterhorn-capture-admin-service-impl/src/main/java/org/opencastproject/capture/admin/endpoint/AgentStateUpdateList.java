/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.capture.admin.endpoint;

import org.opencastproject.capture.admin.api.AgentStateUpdate;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "agents", namespace = "http://capture.admin.opencastproject.org")
@XmlRootElement(name = "agents", namespace = "http://capture.admin.opencastproject.org")
public class AgentStateUpdateList {

  @XmlElement(name = "agent")
  protected List<AgentStateUpdate> agents;

  public AgentStateUpdateList() {
    this.agents = new LinkedList<AgentStateUpdate>();
  }

  public AgentStateUpdateList(List<AgentStateUpdate> agentList) {
    this.agents = new LinkedList<AgentStateUpdate>();
    this.setEvents(agentList);
  }

  public void setEvents(List<AgentStateUpdate> agentList) {
    if (!this.agents.isEmpty()) {
      this.agents.clear();
    }
    this.agents.addAll(agentList);
  }
}