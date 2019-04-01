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

import org.w3c.dom.Node

import java.net.URI

/**
 * A media package element builder provides factory methods for the creation and loading of media package elements from
 * files.
 */
interface MediaPackageElementBuilder {

    /**
     * Creates a media package element from the given file that was previously accepted.
     *
     *
     * Since only the file is given, it is possible, that the best builder plugin cannot be uniquely identified and may
     * require additional contraints, e. g. a matching filename. Be sure to check the documentation of the corresponding
     * plugin for details.
     *
     *
     * @param uri
     * the element location
     * @return the new media package element
     * @throws UnsupportedElementException
     * if creating the media package element fails
     */
    @Throws(UnsupportedElementException::class)
    fun elementFromURI(uri: URI): MediaPackageElement

    /**
     * Creates a media package element from the given file that was previously accepted, while `type` and
     * `flavor` may be taken as strong hints and may both be `null`.
     *
     *
     * If only the file is given, it is possible, that the best suited builder plugin cannot be uniquely identified and
     * may require additional contraints, e. g. a matching filename. Be sure to check the documentation of the
     * corresponding builder plugin for details.
     *
     *
     * @param uri
     * the element location
     * @param type
     * the element type
     * @param flavor
     * the element flavor
     * @return the new media package element
     * @throws UnsupportedElementException
     * if creating the media package element fails
     */
    @Throws(UnsupportedElementException::class)
    fun elementFromURI(uri: URI, type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): MediaPackageElement

    /**
     * Creates a media package element from the DOM element.
     *
     * @param elementNode
     * the DOM node
     * @param serializer
     * the media package serializer
     * @return the media package element
     * @throws UnsupportedElementException
     * if reading the file from manifest fails
     */
    @Throws(UnsupportedElementException::class)
    fun elementFromManifest(elementNode: Node, serializer: MediaPackageSerializer): MediaPackageElement

    /**
     * Creates a new media package elment of the specified type.
     *
     * @param type
     * the element type
     * @param flavor
     * the element flavor
     * @return the new media package element
     */
    fun newElement(type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): MediaPackageElement

}
