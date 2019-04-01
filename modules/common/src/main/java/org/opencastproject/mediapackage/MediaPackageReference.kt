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


package org.opencastproject.mediapackage

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

/**
 * A `MediaPackageElementRef` provides means of pointing to other elements in the media package.
 *
 *
 * A metadata catalog could for example contain a reference to the track that was used to extract the data contained in
 * it.
 *
 */
@XmlJavaTypeAdapter(MediaPackageReferenceImpl.Adapter::class)
interface MediaPackageReference : Cloneable {

    /**
     * Returns the reference type.
     *
     *
     * There is a list of well known types describing media package elements:
     *
     *  * `mediapackage` a reference to the parent media package
     *  * `track` referes to a track inside the media package
     *  * `catalog` referes to a catalog inside the media package
     *  * `attachment` referes to an attachment inside the media package
     *  * `series` referes to a series
     *
     *
     * @return the reference type
     */
    val type: String

    /**
     * Returns the reference identifier.
     *
     *
     * The identifier will usually refer to the id of the media package element, should the reference point to an element
     * inside the media package (see [MediaPackageElement.getIdentifier]).
     *
     *
     * In case of a reference to another media package, this will reflect the media package id (see
     * [MediaPackage.getIdentifier]) or `self` if it refers to the parent media package.
     *
     * @return the reference identifier
     */
    val identifier: String

    /**
     * Returns additional properties that further define what the object is referencing.
     *
     *
     * An example would be the point in time for a slide preview:
     *
     * <pre>
     * &lt;attachment ref="track:track-7;time=8764"&gt;
     * &lt;/attachment&gt;
    </pre> *
     *
     * @return the properties of this reference
     */
    val properties: Map<String, String>

    /**
     * Returns `true` if this reference matches `reference` by means of type and identifier.
     *
     * @param reference
     * the media package reference
     * @return `true` if the reference matches
     */
    fun matches(reference: MediaPackageReference): Boolean

    /**
     * Returns the property with name `key` or `null` if no such property exists.
     *
     * @param key
     * the property name
     * @return the property value
     */
    fun getProperty(key: String): String

    /**
     * Adds an additional property to further define the object reference. Set the value to null in order to remove a
     * property.
     *
     * @param key
     * The unique key
     * @param value
     * The value of the property
     */
    fun setProperty(key: String, value: String)

    /**
     * Returns a deep copy of this reference.
     *
     * @return the clone
     */
    public override fun clone(): Any

    companion object {

        val TYPE_MEDIAPACKAGE = "mediapackage"
        val TYPE_TRACK = "track"
        val TYPE_CATALOG = "catalog"
        val TYPE_ATTACHMENT = "attachment"
        val TYPE_SERIES = "series"
        val SELF = "self"
        val ANY = "*"
    }

}
