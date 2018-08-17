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
package org.opencastproject.scheduler.impl;

import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.scheduler.api.ConflictResolution;
import org.opencastproject.scheduler.api.SchedulerEvent;

/**
 * An in-memory construct to represent a conflicting resolution
 */
public class ConflictResolutionImpl implements ConflictResolution {

  private Strategy strategy;
  private SchedulerEvent scheduledEvent;

  /**
   * Builds a representation of an conflicting scheduled event.
   *
   * @param strategy
   *          the conflict resolution strategy
   * @param scheduledEvent
   *          the scheduled event
   */
  public ConflictResolutionImpl(Strategy strategy, SchedulerEvent scheduledEvent) {
    this.strategy = notNull(strategy, "strategy");
    this.scheduledEvent = notNull(scheduledEvent, "scheduledEvent");
  }

  @Override
  public Strategy getConflictStrategy() {
    return strategy;
  }

  @Override
  public SchedulerEvent getEvent() {
    return scheduledEvent;
  }

}
