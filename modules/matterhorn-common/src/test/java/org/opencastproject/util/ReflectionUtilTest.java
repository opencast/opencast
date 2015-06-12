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

package org.opencastproject.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.opencastproject.util.ReflectionUtil.run;

public class ReflectionUtilTest {
  @Test
  public void testRun() {
    final int[] counter = {2};
    run(Bla.class, new Bla() {
      @Override public String getId() {
        counter[0]--;
        return null;
      }

      @Override public int calc(int v) {
        counter[0]--;
        return 0;
      }
    });
    assertEquals(1, counter[0]);
  }

  @Test
  public void testRun2() {
    final int[] counter = {2};
    run(new Bla() {
      @Override public String getId() {
        counter[0]--;
        return null;
      }

      @Override public int calc(int v) {
        counter[0]--;
        return 0;
      }
    });
    assertEquals(1, counter[0]);
  }

  interface Bla {
    String getId();

    int calc(int v);
  }
}
