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

import org.opencastproject.util.UrlSupport

import java.io.File
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

/**
 * Default implementation of a [MediaPackageSerializer] that is able to deal with relative urls in manifest.
 */
class DefaultMediaPackageSerializerImpl : MediaPackageSerializer {

    /** Optional package root file  */
    /**
     * Returns the package root that is used determine and resolve relative paths. Note that the package root may be
     * `null`.
     *
     * @return the packageRoot
     */
    /**
     * Sets the package root.
     *
     * @param packageRoot
     * the packageRoot to set
     * @see .getPackageRoot
     */
    var packageRoot: URL? = null

    override val ranking: Int
        get() = RANKING

    /**
     * Creates a new package serializer that will work completely transparent, therefore resolving urls by simply
     * returning them as is.
     */
    constructor() {}

    /**
     * Creates a new package serializer that enables the resolution of relative urls from the manifest by taking
     * `packageRoot` as the root url.
     *
     * @param packageRoot
     * the root url
     */
    constructor(packageRoot: URL) {
        this.packageRoot = packageRoot
    }

    /**
     * Creates a new package serializer that enables the resolution of relative urls from the manifest by taking
     * `packageRoot` as the root directory.
     *
     * @param packageRoot
     * the root url
     * @throws MalformedURLException
     * if the file cannot be converted to a url
     */
    @Throws(MalformedURLException::class)
    constructor(packageRoot: File?) {
        if (packageRoot != null)
            this.packageRoot = packageRoot.toURI().toURL()
    }

    /**
     * This serializer implementation tries to cope with relative urls. Should the root url be set to any value other than
     * `null`, the serializer will try to convert element urls to relative paths if possible. .
     *
     * @throws URISyntaxException
     * if the resulting URI contains syntax errors
     * @see org.opencastproject.mediapackage.MediaPackageSerializer.encodeURI
     */
    @Throws(URISyntaxException::class)
    override fun encodeURI(uri: URI?): URI {
        if (uri == null)
            throw IllegalArgumentException("Argument url is null")

        var path = uri.toString()

        // Has a package root been set? If not, no relative paths!
        if (packageRoot == null)
            return uri

        // A package root has been set
        val rootPath = packageRoot!!.toExternalForm()
        if (path.startsWith(rootPath)) {
            path = path.substring(rootPath.length)
        }

        return URI(path)
    }

    /**
     * This serializer implementation tries to cope with relative urls. Should the path start with neither a protocol nor
     * a path separator, the packageRoot is used to create the url relative to the root url that was passed in the
     * constructor.
     *
     *
     * Note that for absolute paths without a protocol, the `file://` protocol is assumed.
     *
     * @see .DefaultMediaPackageSerializerImpl
     * @see org.opencastproject.mediapackage.MediaPackageSerializer.decodeURI
     */
    @Throws(URISyntaxException::class)
    override fun decodeURI(uri: URI?): URI {
        var uri: URI? = uri ?: throw IllegalArgumentException("Argument uri is null")

        // If the path starts with neither a protocol nor a path separator, the packageRoot is used to
        // create the url relative to the root
        var path = uri!!.toString()
        var isRelative = false
        try {
            uri = URI(path)
            isRelative = !uri.path.startsWith("/")
            if (!isRelative)
                return uri
        } catch (e: URISyntaxException) {
            // this may happen, we're still fine
            isRelative = !path.startsWith("/")
            if (!isRelative) {
                path = "file:$path"
                uri = URI(path)
                return uri
            }
        }

        // This is a relative path
        if (isRelative && packageRoot != null) {
            uri = URI(UrlSupport.concat(packageRoot!!.toExternalForm(), path))
            return uri
        }

        return uri
    }

    companion object {

        /** It's very likely that this should be the first serializer when encoding an URI, therefore choose a high ranking  */
        val RANKING = 1000
    }

}
