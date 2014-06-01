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
package org.opencastproject.capture.impl;

import org.junit.Assert;

/** A class that waits until a condition is met or a timeout occurs. **/
class WaitForState {
  private int sleepTime = 100;
  private int maxSleepTime = 40000;
  private int sleepAccumulator = 0;
  private boolean done = false;

  /**
   * Sleeps for a while, checks to see if a condition is made. Once it is the sleep wait ends.
   *
   * @param CheckState
   *          The function check will be used to check to see if the correct state has been obtained.
   **/
  public void sleepWait(CheckState checkState) throws InterruptedException {
    sleepAccumulator = 0;
    while (!done && sleepAccumulator < maxSleepTime) {
      Thread.sleep(sleepTime);
      sleepAccumulator += sleepTime;
      done = checkState.check();
    }
    if (sleepAccumulator >= maxSleepTime) {
      Assert.fail("Test Timed Out");
    }
  }
}
