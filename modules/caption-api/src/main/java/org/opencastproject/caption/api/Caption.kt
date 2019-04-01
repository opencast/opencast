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

package org.opencastproject.caption.api

/**
 * A single caption at a point in time.
 */
interface Caption {

    /**
     * Get the start time of the caption.
     *
     * @return caption start time
     */
    val startTime: Time

    /**
     * Get the end time of the caption.
     *
     * @return caption end time
     */
    val stopTime: Time

    /**
     * Get the caption text.
     *
     * @return caption text
     */
    val caption: Array<String>
}
