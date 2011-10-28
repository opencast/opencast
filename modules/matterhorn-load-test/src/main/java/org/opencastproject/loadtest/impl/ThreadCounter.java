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
package org.opencastproject.loadtest.impl;

/** Keeps track of the still executing threads so that the parent thread knows when they are all finished. **/
public final class ThreadCounter {
  // The number of threads still executing.
  private static int threadCount = 0;
  // Used to make sure we wait until the jobs start executing before we pronounce them all done.
  private static boolean hasChanged = false;

  /**
   * Keeps track of the currently executing threads.
   */
  private ThreadCounter() {

  }

  /**
   * Add a new thread as executing.
   */
  public static synchronized void add() {
    threadCount++;
    hasChanged = true;
  }

  /**
   * Remove a thread as executing.
   */
  public static synchronized void subtract() {
    threadCount--;
  }

  /**
   * @return If all threads have finished executing yet.
   */
  public static synchronized boolean allDone() {
    return hasChanged && threadCount <= 0;
  }

  /**
   * @return The total number of executing threads.
   */
  public static synchronized int getCount() {
    return threadCount;
  }
}
