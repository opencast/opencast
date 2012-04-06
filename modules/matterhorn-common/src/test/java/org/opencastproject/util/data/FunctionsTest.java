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

package org.opencastproject.util.data;

import org.junit.Test;
import org.opencastproject.util.data.functions.Functions;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.util.data.functions.Functions.identity;

public class FunctionsTest {

  @Test
  public void testIdentity() {
    Integer one = 1;
    assertEquals(one, identity().apply(one));
    B b = new B();
    A a = Functions.<A>identity().apply(b);
    assertEquals(a, b);
  }

  class A {
  }

  class B extends A {
  }
}
