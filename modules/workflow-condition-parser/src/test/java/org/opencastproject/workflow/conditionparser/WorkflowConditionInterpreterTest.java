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

package org.opencastproject.workflow.conditionparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WorkflowConditionInterpreterTest {
  @Test
  public void replaceDefaultWithoutDefaultValue() {
    assertEquals("before false after", WorkflowConditionInterpreter.replaceDefaults("before ${foo} after"));
  }

  @Test
  public void replaceDefaultWithDefaultValue() {
    assertEquals("before 1 after", WorkflowConditionInterpreter.replaceDefaults("before ${foo:1} after"));
  }

  @Test
  public void interpretTrue() {
    assertTrue(WorkflowConditionInterpreter.interpret("true"));
  }

  @Test
  public void interpretNotTrue() {
    assertFalse(WorkflowConditionInterpreter.interpret("NOT true"));
  }

  @Test
  public void interpretStringAndNumberAddition() {
    assertTrue(WorkflowConditionInterpreter.interpret("3+'4' == '34'"));
  }

  @Test
  public void interpretOr() {
    assertTrue(WorkflowConditionInterpreter.interpret("false OR true"));
  }

  @Test
  public void interpretAnd() {
    assertFalse(WorkflowConditionInterpreter.interpret("false AND true"));
  }

  @Test
  public void interpretNumericalLessThan() {
    assertTrue(WorkflowConditionInterpreter.interpret("1.5 < 2"));
  }

  @Test
  public void interpretNumericalGreaterThan() {
    assertTrue(WorkflowConditionInterpreter.interpret("3.5 > 2"));
  }

  @Test
  public void interpretStringEquality() {
    assertFalse(WorkflowConditionInterpreter.interpret("'a' == 'b'"));
  }

  @Test
  public void interpretStringInequality() {
    assertTrue(WorkflowConditionInterpreter.interpret("'a' != 'b'"));
  }

  @Test
  public void interpretNumericalLessThanWithAddition() {
    assertTrue(WorkflowConditionInterpreter.interpret("1+1 < 3"));
  }

  @Test
  public void interpretNumericalLessThanWithMultiplication() {
    assertFalse(WorkflowConditionInterpreter.interpret("1*4 < 3"));
  }
}
