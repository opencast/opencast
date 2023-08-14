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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.EqualsUtil.eqListSorted;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.functions.Options.sequenceOpt;

import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import org.junit.Test;

public class OptionsTest {
  @Test
  public void testSequenceOpt() {
    assertTrue(eqListSorted(list(1, 2, 3), sequenceOpt(list(some(1), none(-1), some(2), some(3), none(-1))).get()));
    assertTrue(eqListSorted(list(), sequenceOpt(list(none(-1), none(-1))).get()));
  }

  @Test
  public void testForeach() {
    final Integer[] r = new Integer[1];
    final Function<String, Option<Integer>> f = Options.foreach(new Function<String, Option<Integer>>() {
      @Override
      public Option<Integer> apply(String s) {
        return s.length() > 0 ? some(s.length()) : none(Integer.class);
      }
    }, new Effect<Integer>() {
      @Override
      protected void run(Integer integer) {
        r[0] = integer;
      }
    });
    assertEquals(none(Integer.class), f.apply(""));
    assertNull(r[0]);
    assertEquals(some(3), f.apply("bla"));
    assertEquals(new Integer(3), r[0]);
  }
}
