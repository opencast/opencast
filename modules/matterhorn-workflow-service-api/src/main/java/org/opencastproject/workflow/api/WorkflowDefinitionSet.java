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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A collection of workflow definitions.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "definitions", namespace = "http://workflow.opencastproject.org")
public class WorkflowDefinitionSet {

  @XmlElement(name = "definition")
  protected List<WorkflowDefinition> definitions = null;

  public WorkflowDefinitionSet(Collection<WorkflowDefinition> definitions) {
    this.definitions = new ArrayList<WorkflowDefinition>();
    if (definitions != null)
      this.definitions.addAll(definitions);
  }

  public WorkflowDefinitionSet() {
    this(null);
  }

  public List<WorkflowDefinition> getDefinitions() {
    return definitions;
  }

  public void setDefinitions(List<WorkflowDefinition> definitions) {
    this.definitions = definitions;
  }

}
