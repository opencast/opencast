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

import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementBuilder
import org.opencastproject.mediapackage.MediaPackageElementFlavor

import org.w3c.dom.Node

import java.net.URI

/**
 * This implementation of the [MediaPackageElementBuilderPlugin] recognizes arbitrary attachments and creates
 * media package element representations for them.
 *
 *
 * A media package element is considered an attachment by this plugin if it is of type [Attachment] and does not
 * have any specializing flavor.
 */
class AttachmentBuilderPlugin : AbstractAttachmentBuilderPlugin(), MediaPackageElementBuilder {

    /**
     * {@inheritDoc}
     *
     * This plugin is an implementation for unknown attachments, therefore it returns `-1` as its priority.
     *
     * @see org.opencastproject.mediapackage.elementbuilder.AbstractElementBuilderPlugin.getPriority
     */
    override var priority: Int
        get() = -1
        set(value: Int) {
            super.priority = value
        }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.AbstractAttachmentBuilderPlugin.accept
     */
    override fun accept(uri: URI, type: MediaPackageElement.Type?, flavor: MediaPackageElementFlavor?): Boolean {
        if (type != null && flavor != null) {
            if (type != MediaPackageElement.Type.Attachment)
                return false
        } else if (type != null && type != MediaPackageElement.Type.Attachment) {
            return false
        } else if (flavor != null && flavor != Attachment.FLAVOR) {
            return false
        }
        return super.accept(uri, type, flavor)
    }

    /**
     * @see org.opencastproject.mediapackage.elementbuilder.AbstractAttachmentBuilderPlugin.accept
     */
    override fun accept(elementNode: Node): Boolean {
        return super.accept(elementNode)
    }

    /**
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        return "Attachment Builder Plugin"
    }

}
