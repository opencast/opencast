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

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/** Only one function application can be threaded through the needle eye at a time. */
public final class NeedleEye {
  private final AtomicBoolean running = new AtomicBoolean(false);

  /**
   * Apply function <code>f</code> only if no other thread currently applies a function using this needle eye.
   *
   * @return the result of <code>f</code> or none if another function is currently being applied.
   */
  public <A> Optional<A> apply(Callable<A> f) {
    if (running.compareAndSet(false, true)) {
      try {
        return Optional.ofNullable(f.call());
      } catch (Exception e) {
        throw new RuntimeException("Exception in NeedleEye function", e);
      } finally {
        running.set(false);
      }
    } else {
      return Optional.empty();
    }
  }
}
