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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.opencastproject.util.data.VCell.cell;

public class CellTest {
  @Test
  public void testVCell() {
    final VCell<String> c = cell("hello");
    assertEquals("hello", c.get());
    final Object change1 = c.change().getB();
    c.set("hello");
    assertEquals(change1, c.change().getB());
    c.set("goodbye");
    assertNotSame(change1, c.change().getB());
  }

  @Test
  public void testVCell2() {
    final VCell<String> c = cell("hello");
    final Object change1 = c.change().getB();
    c.set("hello");
    assertEquals(change1, c.change().getB());
    c.set("goodbye");
    assertNotSame(change1, c.change().getB());
  }

  @Test
  public void testFCell() {
    final VCell<String> c = cell("hello");
    final Cell<Integer> f = c.lift(new Function<String, Integer>() {
      @Override public Integer apply(String s) {
        return s.length();
      }
    });
    assertEquals(5, (int) f.get());
    final Object change1 = f.change().getB();
    c.set("hello");
    assertEquals(change1, f.change().getB());
    c.set("goodbye");
    assertNotSame(change1, f.change().getB());
    assertEquals(7, (int) f.get());
  }
}
