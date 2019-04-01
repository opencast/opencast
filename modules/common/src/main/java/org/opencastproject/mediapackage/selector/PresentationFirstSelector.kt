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

package org.opencastproject.mediapackage.selector

import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.Track

/**
 * This `MediaPackageElementSelector` will return zero or one track from the media package by looking at the
 * flavors and following this order:
 *
 *  * [MediaPackageElements.PRESENTER_SOURCE]
 *  * [MediaPackageElements.PRESENTATION_SOURCE]
 *  * [MediaPackageElements.DOCUMENTS_SOURCE]
 *  * [MediaPackageElements.AUDIENCE_SOURCE]
 *
 *
 * This basically means that if there is a presenter track, this is the one that will be returnd. If not, then the
 * selctor will try to find a presentation track and so on.
 */
class PresentationFirstSelector : FlavorPrioritySelector<Track>() {
    /**
     * Creates a new presenter first selector.
     */
    init {
        addFlavor(MediaPackageElements.PRESENTATION_SOURCE)
        addFlavor(MediaPackageElements.PRESENTER_SOURCE)
        addFlavor(MediaPackageElements.DOCUMENTS_SOURCE)
        addFlavor(MediaPackageElements.AUDIENCE_SOURCE)
    }

}
