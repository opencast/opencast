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

import org.opencastproject.util.MimeType

import javax.xml.xpath.XPath
import javax.xml.xpath.XPathFactory

/**
 * This general implementation of a media package element builder supports specialized implementations by providing
 * tests on the filename an mime type of the file in question.
 */
/**
 * Creates a new abstract element builder plugin.
 */
abstract class AbstractElementBuilderPlugin : MediaPackageElementBuilderPlugin {

    /** The registered mime types  */
    protected var mimeTypes: List<MimeType>? = null

    /** The xpath facility  */
    protected var xpath = XPathFactory.newInstance().newXPath()

    /** The builder's priority  */
    /**
     * Returns -1 by default.
     */
    var priority = -1

    /**
     * This is a convenience implementation for subclasses doing nothing.
     *
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.init
     */
    override fun init() {}

    /**
     * This is a convenience implementation for subclasses doing nothing.
     *
     * @see org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin.destroy
     */
    override fun destroy() {}

}
