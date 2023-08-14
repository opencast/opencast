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

package org.opencastproject.workflow.impl;

import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState;
import org.opencastproject.workflow.api.XmlWorkflowParser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowConfigurationTest {

  @Test
  public void testConfigurationSerialization() throws Exception {
    WorkflowOperationInstance op = new WorkflowOperationInstance("op", OperationState.RUNNING);
    Map<String, String> config = new HashMap<String, String>();
    config.put("this", "that");
    op.setConfiguration("this", "that");
    WorkflowInstance instance = new WorkflowInstance();
    List<WorkflowOperationInstance> ops = new ArrayList<WorkflowOperationInstance>();
    ops.add(op);
    instance.setOperations(ops);
    String xml = XmlWorkflowParser.toXml(instance);
    Assert.assertTrue(xml.contains("configuration key=\"this\">that</"));
  }

}
