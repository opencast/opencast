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

package org.opencastproject.util.data.functions;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Misc.ifThen;

import org.junit.Test;

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
    Misc.cast(1, Integer.class);
    Misc.cast(1.0, Double.class);
    Misc.cast(1, Double.class);
    Misc.cast(1, Short.class);
    Misc.cast(1, Byte.class);
    Misc.cast(1.0, Float.class);
    Misc.cast(1, Float.class);
    Misc.cast(1, Double.class);
    Misc.cast(1, Number.class);
    Misc.cast("1", String.class);
  }
}
