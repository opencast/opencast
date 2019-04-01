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


package org.opencastproject.mediapackage.elementbuilder

import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageSerializer
import org.opencastproject.mediapackage.UnsupportedElementException

import org.w3c.dom.Node

import java.net.URI

/**
 * An element builder plugin is an object that is able to recognize one ore more filetypes slated for ingest into
 * Opencast.
 *
 *
 * **Implementation note:** Builder plugins may be stateful. They are intended to be used as throw-away
 * objects.
 */
@Deprecated("")
interface MediaPackageElementBuilderPlugin {

    /**
     * This method is called once in a plugin's life cycle. When this method is called, the plugin can make sure that
     * everything is in place for it to work properly. If this isn't the case, it should throw an exception so it will no
     * longer be bothered by the element builder.
     *
     * @throws Exception
     * if some unrecoverable state is reached
     */
    @Throws(Exception::class)
    fun init()

    /**
     * This method is called before the plugin is abandoned by the element builder.
     */
    fun destroy()

    /**
     * This method is called if the media package builder tries to create a new media package element of type
     * `elementType`.
     *
     *
     * Every registered builder plugin will then be asked whether it is able to create a media package element from the
     * given element type. If this is the case for a plugin, it will then be asked to create such an element by a call to
     * [.newElement].
     *
     *
     * @param type
     * the type
     * @param flavor
     * the element flavor
     * @return `true` if the plugin is able to create such an element
     */
    fun accept(type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): Boolean

    /**
     * This method is called on every registered media package builder plugin until one of these plugins returns
     * `true`. If no plugin recognises the file, it is rejected.
     *
     *
     * The parameters `type` and `flavor` may be taken as strong hints and may both be
     * `null`.
     *
     *
     *
     * Implementers schould return the correct mime type for the given file if they are absolutely sure about the file.
     * Otherwise, `null` should be returned.
     *
     *
     * @param uri
     * the element location
     * @param type
     * the element type
     * @param flavor
     * the element flavor
     * @return `true` if the plugin can handle the element
     */
    fun accept(uri: URI, type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): Boolean

    /**
     * This method is called while the media package builder parses a media package manifest.
     *
     *
     * Every registered builder plugin will then be asked, whether it is able to create a media package element from the
     * given element definition.
     *
     *
     *
     * The element must then be constructed and returned in the call to
     * [.elementFromManifest].
     *
     *
     * @param elementNode
     * the node
     * @return `true` if the plugin is able to create such an element
     */
    fun accept(elementNode: Node): Boolean

    /**
     * Creates a media package element from the given url that was previously accepted.
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
     * Creates a media package element from the DOM element.
     *
     * @param elementNode
     * the DOM node
     * @param serializer
     * the media package serializer
     * @return the media package element
     * @throws UnsupportedElementException
     */
    @Throws(UnsupportedElementException::class)
    fun elementFromManifest(elementNode: Node, serializer: MediaPackageSerializer): MediaPackageElement

    /**
     * Creates a new media package element of the specified type.
     *
     * @param type
     * the element type
     * @param flavor
     * the element flavor
     * @return the new media package element
     */
    fun newElement(type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): MediaPackageElement

}
