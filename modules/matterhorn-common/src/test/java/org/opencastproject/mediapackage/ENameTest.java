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
package org.opencastproject.mediapackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.opencastproject.util.IoSupport;

import org.junit.Test;

public class ENameTest {
  @Test
  public void testEquals() throws Exception {
    final EName a = new EName("http://localhost/a", "a");
    final EName b = new EName("http://localhost/a", "a");
    final EName c = new EName("http://localhost/b", "a");
    final EName d = new EName("http://localhost/a", "b");
    assertEquals(a, a);
    assertEquals(a, b);
    assertEquals(b, a);
    assertNotEquals(a, c);
    assertNotEquals(c, a);
    assertNotEquals(c, d);
    assertNotEquals(d, c);
  }

  @Test
  public void testSerializability() {
    final EName a = new EName("http://localhost/a", "a");
    assertEquals(a, IoSupport.serializeDeserialize(a));
  }
}
