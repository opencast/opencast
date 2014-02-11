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
package org.opencastproject.util.data.functions;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.ifThen;

public class MiscTest {
  @Test
  public void testIfThen() {
    assertEquals(some("hallo"), some("hello").map(ifThen("hello", "hallo")));
    assertEquals(some(-1), some("none").map(ifThen("none", "-1")).bind(Strings.toInt).orError(new RuntimeException()));
    assertEquals(some(300), some("300").map(ifThen("none", "-1")).bind(Strings.toInt).orError(new RuntimeException()));
  }

  @Test(expected = RuntimeException.class)
  public void testIfThenError() {
    some("200a").map(ifThen("none", "-1")).bind(Strings.toInt).orError(new RuntimeException());
  }

  @Test
  public void testCast() {
    final Integer a = Misc.cast(1, Integer.class);
    final Double b = Misc.cast(1.0, Double.class);
    final Double c = Misc.cast(1, Double.class);
    final Short d = Misc.cast(1, Short.class);
    final Byte e = Misc.cast(1, Byte.class);
    final Float f = Misc.cast(1.0, Float.class);
    final Float g = Misc.cast(1, Float.class);
    final Number h = Misc.cast(1, Double.class);
    final Number i = Misc.cast(1, Number.class);
    final String j = Misc.cast("1", String.class);
  }
}
