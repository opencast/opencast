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
import org.opencastproject.util.data.Function0;

import static org.junit.Assert.assertTrue;

public class NeedleEyeTest {
  @Test
  public void testNeedleEye() throws Exception {
    // test has been run a thousand times, increase the number to see for yourself
    for (int i = 0; i < 1; i++) {
      runTest();
    }
  }

  private void runTest() throws Exception {
    final NeedleEye eye = new NeedleEye();
    final boolean[] r1 = new boolean[] {false};
    final boolean[] r2 = new boolean[] {false};
    final Thread t1 = new Thread(createRunnabe(eye, r1));
    final Thread t2 = new Thread(createRunnabe(eye, r2));
    t1.start();
    t2.start();
    t1.join();
    t2.join();
    assertTrue(r1[0] != r2[0]);
  }

  private Runnable createRunnabe(final NeedleEye eye, final boolean[] result) {
    return new Runnable() {
      @Override public void run() {
        for (Boolean ignore : eye.apply(sleep(1000))) {
          result[0] = true;
          return;
        }
        System.out.println("not executed");
      }
    };
  }

  private Function0<Boolean> sleep(final long time) {
    return new Function0.X<Boolean>() {
      @Override public Boolean xapply() throws Exception {
        System.out.println(this + " is sleeping for " + time + " ms");
        Thread.sleep(time);
        System.out.println(this + " awaked");
        return true;
      }
    };
  }
}
