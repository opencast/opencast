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

package org.opencastproject.workflow.impl;

import junit.framework.Assert;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests property replacement in workflow operation instances
 */
public class PropertyReplacementTest {
  @Test
  public void testIfAndUnlessNoGroupPattern() throws Exception {
    Map<String, String> properties = new HashMap<String, String>();
    properties.put("foo", "propertyForFoo");
    properties.put("bar", "propertyForBar");
    String source = "<config key=\"foo\">${foo}</config><config key=\"bar\">${bar}</config><config key=\"baz\">${baz}</config>";
    WorkflowServiceImpl service = new WorkflowServiceImpl();
    String result = service.replaceVariables(source, properties);
    Assert.assertEquals("Variable replacement failed",
            "<config key=\"foo\">propertyForFoo</config><config key=\"bar\">propertyForBar</config>"
                    + "<config key=\"baz\">${baz}</config>", result);
  }
}
