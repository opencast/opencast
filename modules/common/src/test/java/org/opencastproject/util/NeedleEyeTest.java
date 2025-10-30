/*
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

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class NeedleEyeTest {

  private static final Logger logger = LoggerFactory.getLogger(NeedleEyeTest.class);

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
    Thread t1 = new Thread(createRunnable(eye, r1));
    Thread t2 = new Thread(createRunnable(eye, r2));
    t1.start();
    t2.start();
    t1.join();
    t2.join();
    // Exactly one thread should have executed the critical section
    assertTrue(r1[0] != r2[0]);
  }

  private Runnable createRunnable(final NeedleEye eye, final boolean[] result) {
    return () -> {
      // Use Callable<Boolean> with NeedleEye
      Optional<Boolean> executed = eye.apply(() -> {
        Thread.sleep(1000); // simulate work
        result[0] = true;
        return true;
      });

      if (!executed.isPresent()) {
        logger.info("Not executed by thread {}", Thread.currentThread().getName());
      }
    };
  }
}
