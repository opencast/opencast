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

package org.opencastproject.serviceregistry.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides statistics about host registrations
 */
public class HostStatistics {
  private final Map<Long, Long> queued = new HashMap<>();
  private final Map<Long, Long> running = new HashMap<>();

  /**
   * Add statistics for a registered host
   *
   * @param hostId Identifier of host
   * @param queued Number of queued jobs for this host
   */
  public void addQueued(final long hostId, final long queued) {
    this.queued.put(hostId, queued);
  }

  /**
   * Add statistics for a registered host
   *
   * @param hostId Identifier of host
   * @param running Number of running jobs on this host
   */
  public void addRunning(final long hostId, final long running) {
    this.running.put(hostId, running);
  }

  /**
   * Get number of queued jobs on a specific host.
   *
   * @param hostId Identifier of host
   * @return Number of queued jobs
   */
  public long queuedJobs(final long hostId) {
    return queued.getOrDefault(hostId, 0L);
  }

  /**
   * Get number of running jobs on a specific host.
   *
   * @param hostId Identifier of host
   * @return Number of running jobs
   */
  public long runningJobs(final long hostId) {
    return running.getOrDefault(hostId, 0L);
  }
}
