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
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.Stream
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.TrackSupport

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.ArrayList

/**
 * This `MediaPackageElementSelector` selects all tracks from a `MediaPackage` that contain at
 * least an audio stream while optionally matching other requirements such as flavors and tags.
 */
class StreamElementSelector<S : Stream> : AbstractMediaPackageElementSelector<Track> {

    /**
     * This constructor tries to determine the entity type from the type argument used by a concrete implementation of
     * `GenericHibernateDao`.
     */
    private val parametrizedStreamType: Class<*>
        get() {
            var current: Class<*> = javaClass
            var superclass: Type
            var entityClass: Class<out S>? = null
            while ((superclass = current.genericSuperclass) != null) {
                if (superclass is ParameterizedType) {
                    entityClass = superclass.actualTypeArguments[0] as Class<S>
                    break
                } else if (superclass is Class<*>) {
                    current = superclass
                } else {
                    break
                }
            }
            if (entityClass == null) {
                throw IllegalStateException("DAO creation exception: Cannot determine entity type because "
                        + javaClass.getName() + " does not specify any type parameter.")
            }
            return entityClass.javaClass
        }

    /**
     * Creates a new selector.
     */
    constructor() {}

    /**
     * Creates a new selector that will restrict the result of `select()` to the given flavor.
     *
     * @param flavor
     * the flavor
     */
    constructor(flavor: String) : this(MediaPackageElementFlavor.parseFlavor(flavor)) {}

    /**
     * Creates a new selector that will restrict the result of `select()` to the given flavor.
     *
     * @param flavor
     * the flavor
     */
    constructor(flavor: MediaPackageElementFlavor) {
        addFlavor(flavor)
    }

    /**
     * Returns all tracks from a `MediaPackage` that contain at least a `Stream` of the parametrized
     * type while optionally matching other requirements such as flavors and tags. If no such combination can be found, i.
     * g. there is no audio or video at all, an empty array is returned.
     *
     * @see org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector.select
     */
    override fun select(mediaPackage: MediaPackage, withTagsAndFlavors: Boolean): Collection<Track> {
        val candidates = super.select(mediaPackage, withTagsAndFlavors)
        val result = ArrayList<Track>()
        for (t in candidates) {
            if (TrackSupport.byType<Stream>(t.streams, parametrizedStreamType).size > 0) {
                result.add(t)
            }
        }
        return result
    }

}
