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

package org.opencastproject.smil.entity.media.element.api

import org.opencastproject.smil.api.SmilException
import org.opencastproject.smil.entity.media.api.SmilMediaObject
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam

import java.net.URI

/**
 * Represent a media element like `audio`, `video`,...
 */
interface SmilMediaElement : SmilMediaObject {

    /**
     * Returns clip start position.
     *
     * @return the clipBegin
     */
    val clipBegin: String

    /**
     * Returns clip end position.
     *
     * @return the clipEnd
     */
    val clipEnd: String

    /**
     * Returns media element type.
     *
     * @return this media element type
     */
    val mediaType: MediaType

    /**
     * Returns SmilMediaParamGroup Id given with this element.
     *
     * @return the paramGroup Id
     */
    val paramGroup: String

    /**
     * Returns [SmilMediaParam]s for this media element. The [List] is
     * immutable, use SmilService to modify it.
     *
     * @return the [List] with [SmilMediaParam]s
     */
    val params: List<SmilMediaParam>

    /**
     * Returns media source URI.
     *
     * @return the media src URI
     */
    val src: URI

    /**
     * Returns clip start position in milliseconds.
     *
     * @throws SmilException if clip begin position can't parsed.
     * @return clip start position in milliseconds
     */
    val clipBeginMS: Long

    /**
     * Returns clip end position in milliseconds.
     *
     * @throws SmilException if clip end position can't parsed.
     * @return clip end position in milliseconds
     */
    val clipEndMS: Long

    /**
     * SMIL media element type.
     */
    enum class MediaType {
        AUDIO, VIDEO, IMAGE, REF
    }
}
