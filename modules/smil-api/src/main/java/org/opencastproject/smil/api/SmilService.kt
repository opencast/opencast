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

package org.opencastproject.smil.api

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.Track
import org.opencastproject.smil.entity.api.Smil

import java.io.File

/**
 * [SmilService] provides [Smil] manipulation.
 */
interface SmilService {

    /**
     * Create a new [Smil].
     *
     * @return a new [Smil]
     */
    fun createNewSmil(): SmilResponse

    /**
     * Create a new [Smil] and store the [MediaPackage] Id as meta
     * data.
     *
     * @param mediaPackage
     * @return a new [Smil]
     */
    fun createNewSmil(mediaPackage: MediaPackage): SmilResponse

    /**
     * Add new par element to [Smil].
     *
     * @param smil [Smil] to edit
     * @return edited [Smil] and the new SmilMediaContainer
     * @throws SmilException
     */
    @Throws(SmilException::class)
    fun addParallel(smil: Smil): SmilResponse

    /**
     * Add new par element to [Smil] inside an element with given Id.
     *
     * @param smil [Smil] to edit
     * @param parentId element id, where to add new par element
     * @return edited [Smil] and the new SmilMediaContainer
     * @throws SmilException if there is no element with given parentId
     */
    @Throws(SmilException::class)
    fun addParallel(smil: Smil, parentId: String): SmilResponse

    /**
     * Add new seq element to [Smil].
     *
     * @param smil [Smil] to edit
     * @return edited [Smil] and the new SmilMediaContainer
     * @throws SmilException
     */
    @Throws(SmilException::class)
    fun addSequence(smil: Smil): SmilResponse

    /**
     * Add new seq element to [Smil] inside an element with given Id.
     *
     * @param smil [Smil] to edit
     * @param parentId element id, where to add new seq element
     * @return edited [Smil] and the new SmilMediaContainer
     * @throws SmilException if there is no element with given parentId
     */
    @Throws(SmilException::class)
    fun addSequence(smil: Smil, parentId: String): SmilResponse

    /**
     * Add a SmilMediaElement based on given track and start/duration
     * information.
     *
     * @param smil [Smil] to edit
     * @param parentId element id, where to add new SmilMediaElement
     * @param track [Track] to add as SmilMediaElement
     * @param start start position in [Track] in milliseconds
     * @param duration duration in milliseconds
     * @return edited [Smil], the new SmilMediaElement and generated
     * meta data
     * @throws SmilException if there is no element with the given parentId
     */
    @Throws(SmilException::class)
    fun addClip(smil: Smil, parentId: String, track: Track, start: Long, duration: Long): SmilResponse

    /**
     * Add a SmilMediaElement based on given track and start/duration
     * information.
     *
     * @param smil [Smil] to edit
     * @param parentId element id, where to add new SmilMediaElement
     * @param track [Track] to add as SmilMediaElement
     * @param start start position in [Track] in milliseconds
     * @param duration duration in milliseconds
     * @param paramGroupId clip should be added as a part of a previously created param group
     * @return edited [Smil], the new SmilMediaElement and generated
     * meta data
     * @throws SmilException if there is no element with the given parentId
     */
    @Throws(SmilException::class)
    fun addClip(smil: Smil, parentId: String, track: Track, start: Long, duration: Long, paramGroupId: String): SmilResponse

    /**
     * Add a list of SmilMediaElements based on given tracks and
     * start/duration information.
     *
     * @param smil [Smil] to edit
     * @param parentId element id, where to add new SmilMediaElements
     * @param tracks [Track]s to add as SmilMediaElements
     * @param start start position in [Track]s in milliseconds
     * @param duration duration in milliseconds
     * @return edited [Smil], the new SmilMediaElements and tracks meta data
     * @throws SmilException if there is no element with the given parentId
     */
    @Throws(SmilException::class)
    fun addClips(smil: Smil, parentId: String, tracks: Array<Track>, start: Long, duration: Long): SmilResponse

    /**
     * Add a meta element to [Smil] head.
     *
     * @param smil [Smil] to edit
     * @param name meta name
     * @param content meta content
     * @return edited [Smil] and the new SmilMeta
     */
    fun addMeta(smil: Smil, name: String, content: String): SmilResponse

    /**
     * Remove element (identified by elementId) from [Smil] if exists.
     *
     * @param smil [Smil] to edit
     * @param elementId element Id to remove
     * @return edited Smil and removed SmilMediaElement if [Smil] contains an element with given Id
     */
    fun removeSmilElement(smil: Smil, elementId: String): SmilResponse

    /**
     * Returns [Smil] from Xml `String`.
     *
     * @param smilXml Smil document Xml as `String`
     * @return parsed [Smil]
     * @throws SmilException if an error occures while parsing [Smil]
     */
    @Throws(SmilException::class)
    fun fromXml(smilXml: String): SmilResponse

    /**
     * Returns [Smil] from Xml `File`.
     *
     * @param smilXmlFile Smil document Xml as `File`
     * @return parsed [Smil]
     * @throws SmilException if an error occures while parsing [Smil]
     */
    @Throws(SmilException::class)
    fun fromXml(smilXmlFile: File): SmilResponse
}
