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

import org.opencastproject.util.Checksum
import org.opencastproject.util.MimeType
import org.opencastproject.util.MimeTypes

import java.io.File
import java.net.URI

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.adapters.XmlAdapter

/**
 * This is a basic implementation for handling simple catalogs of metadata.
 */
@XmlRootElement(name = "catalog", namespace = "http://mediapackage.opencastproject.org")
@XmlType(name = "catalog", namespace = "http://mediapackage.opencastproject.org")
@XmlAccessorType(XmlAccessType.NONE)
open class CatalogImpl : AbstractMediaPackageElement, Catalog {

    /** Needed by JAXB  */
    protected constructor() : super(MediaPackageElement.Type.Catalog, null, null, null, null, MimeTypes.parseMimeType("text/xml")) {}// default to text/xml mimetype

    /**
     * Creates an abstract metadata container.
     *
     * @param id
     * the element identifier withing the package
     * @param flavor
     * the catalog flavor
     * @param uri
     * the document location
     * @param size
     * the catalog size in bytes
     * @param checksum
     * the catalog checksum
     * @param mimeType
     * the catalog mime type
     */
    protected constructor(id: String?, flavor: MediaPackageElementFlavor, uri: URI, size: Long, checksum: Checksum,
                          mimeType: MimeType) : super(MediaPackageElement.Type.Catalog, flavor, uri, size, checksum, mimeType) {
    }

    /**
     * Creates an abstract metadata container.
     *
     * @param flavor
     * the catalog flavor
     * @param uri
     * the document location
     * @param size
     * the catalog size in bytes
     * @param checksum
     * the catalog checksum
     * @param mimeType
     * the catalog mime type
     */
    protected constructor(flavor: MediaPackageElementFlavor, uri: URI, size: Long, checksum: Checksum, mimeType: MimeType) : this(null, flavor, uri, size, checksum, mimeType) {}

    class Adapter : XmlAdapter<CatalogImpl, Catalog>() {
        @Throws(Exception::class)
        override fun marshal(cat: Catalog): CatalogImpl {
            return cat as CatalogImpl
        }

        @Throws(Exception::class)
        override fun unmarshal(cat: CatalogImpl): Catalog {
            return cat
        }
    }

    companion object {

        /** Serial version UID  */
        private val serialVersionUID = -908525367616L

        /**
         * Reads the metadata from the specified file and returns it encapsulated in a [Catalog] object.
         *
         * @param catalog
         * the dublin core metadata container file
         * @return the dublin core object
         */
        fun fromFile(catalog: File): Catalog {
            return fromURI(catalog.toURI())
        }

        /**
         * Reads the metadata from the specified file and returns it encapsulated in a [Catalog] object.
         *
         * @param uri
         * the dublin core metadata container file
         * @return the dublin core object
         */
        fun fromURI(uri: URI): Catalog {
            val cat = CatalogImpl()
            cat.uri = uri
            return cat
        }

        /**
         * @return a new catalog instance
         */
        fun newInstance(): Catalog {
            return CatalogImpl()
        }
    }

}
