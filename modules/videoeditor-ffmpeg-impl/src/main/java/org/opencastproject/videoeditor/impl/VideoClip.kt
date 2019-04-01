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

package org.opencastproject.videoeditor.impl

class VideoClip(val src: Int, val start: Double, end: Double) {
    var end: Double = 0.toDouble()
        internal set
    // Regions are relative to root-layout,
    var region: String? = null
        internal set // if layout regions are supported, it will be resolved and stored here, defaults to root layout
    val duration: Double
        get() = end - start

    init {
        this.end = end
    }

}
