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

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import java.util.HashSet

/**
 * This selector will return one or zero `MediaPackageElements` from a `MediaPackage`, following
 * these rules:
 *
 *  * Elements will be returned depending on tags that have been set
 *  * If no tags have been specified, all the elements will be taken into account
 *  * The result is one or zero elements
 *  * The element is selected based on the order of flavors
 *
 */
open class FlavorPrioritySelector<T : MediaPackageElement> : AbstractMediaPackageElementSelector<T>() {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackageElementSelector.select
     */
    override fun select(mediaPackage: MediaPackage, withTagsAndFlavors: Boolean): Collection<T> {
        val candidates = HashSet<T>()
        val result = HashSet<T>()

        // Have the super implementation match type, flavor and tags
        candidates.addAll(super.select(mediaPackage, withTagsAndFlavors))

        if (flavors.isEmpty())
            return candidates

        // Return the first element based on the flavor
        buildResult@ for (flavor in flavors) {
            for (element in candidates) {
                if (flavor == element.flavor) {
                    result.add(element)
                    break@buildResult
                }
            }
        }

        return result
    }

}
