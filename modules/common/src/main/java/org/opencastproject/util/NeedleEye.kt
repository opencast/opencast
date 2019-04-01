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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.util

import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some

import org.opencastproject.util.data.Function0
import org.opencastproject.util.data.Option

import java.util.concurrent.atomic.AtomicBoolean

/** Only one function application can be threaded through the needle eye at a time.  */
class NeedleEye {
    private val running = AtomicBoolean(false)

    /**
     * Apply function `f` only if no other thread currently applies a function using this needle eye. Please
     * note that `f` must *not* return null, so please do not use
     * [org.opencastproject.util.data.Effect0].
     *
     * @return the result of `f` or none if another function is currently being applied.
     */
    fun <A> apply(f: Function0<A>): Option<A> {
        return if (running.compareAndSet(false, true)) {
            try {
                some(f.apply())
            } finally {
                running.set(false)
            }
        } else {
            none()
        }
    }
}
