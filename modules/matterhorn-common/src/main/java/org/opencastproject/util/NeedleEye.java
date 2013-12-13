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
package org.opencastproject.util;

import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

/** Only one function application can be threaded through the needle eye at a time. */
public final class NeedleEye {
  private final AtomicBoolean running = new AtomicBoolean(false);

  /**
   * Apply function <code>f</code> only if no other thread currently applies a function using this needle eye.
   * Please note that <code>f</code> must <em>not</em> return null, so please do not use {@link org.opencastproject.util.data.Effect0}.
   *
   * @return the result of <code>f</code> or none if another function is currently being applied.
   */
  public <A> Option<A> apply(Function0<A> f) {
    if (running.compareAndSet(false, true)) {
      try {
        return some(f.apply());
      } finally {
        running.set(false);
      }
    } else {
      return none();
    }
  }
}