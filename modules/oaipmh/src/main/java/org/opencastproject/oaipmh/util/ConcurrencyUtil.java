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


package org.opencastproject.oaipmh.util;

import org.opencastproject.util.data.Function0;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Functions dealing with concurrency.
 */
public final class ConcurrencyUtil {

  private ConcurrencyUtil() {
  }

  /**
   * Gently shut down an executor service. Waits for <code>waitSeconds</code> then tries
   * to forcibly terminate the service. Waits again then calls <code>executorDoesNotTerminate</code>.
   */
  public static void shutdownAndAwaitTermination(ExecutorService exe, int waitSeconds,
                                                 Function0<Void> executorDoesNotTerminate) {
    // Disable new tasks from being submitted
    exe.shutdown();
    try {
      // Wait a while for existing tasks to terminate
      if (!exe.awaitTermination(waitSeconds, TimeUnit.SECONDS)) {
        // Cancel currently executing tasks
        exe.shutdownNow();
        // Wait a while for tasks to respond to being cancelled
        if (!exe.awaitTermination(waitSeconds, TimeUnit.SECONDS))
          executorDoesNotTerminate.apply();
      }
    } catch (InterruptedException ie) {
      // Forcibly terminate on interruption
      exe.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }
}
