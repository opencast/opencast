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

package org.opencastproject.mediapackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.opencastproject.util.IoSupport;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  @Test
  public void testCompareTo() {
    final EName a1 = new EName("http://localhost/a", "a");
    final EName b1 = new EName("http://localhost/b", "b");
    final EName a2 = new EName("", "a");
    final EName b2 = new EName("", "b");

    List<EName> eNames = new ArrayList<>();
    eNames.add(b1);
    eNames.add(a1);
    eNames.add(b2);
    eNames.add(a2);
    Collections.sort(eNames);

    assertEquals(a2, eNames.get(0));
    assertEquals(b2, eNames.get(1));
    assertEquals(a1, eNames.get(2));
    assertEquals(b1, eNames.get(3));
  }

}
