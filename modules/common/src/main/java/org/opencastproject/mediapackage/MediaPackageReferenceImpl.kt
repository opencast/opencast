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

import java.util.HashMap

import javax.xml.bind.annotation.adapters.XmlAdapter

/**
 * Default implementation for a [MediaPackageReference].
 */
class MediaPackageReferenceImpl : MediaPackageReference {

    /** The reference identifier  */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageReference.getIdentifier
     */
    override var identifier: String? = null
        protected set

    /** The reference type  */
    /**
     * @see org.opencastproject.mediapackage.MediaPackageReference.getType
     */
    override var type: String? = null
        protected set

    /** External representation  */
    private var externalForm: String? = null

    /** The properties that describe this reference  */
    /**
     * @return the properties
     */
    /**
     * @param properties
     * the properties to set
     */
    override var properties: MutableMap<String, String>? = null

    /**
     * Creates a reference to the specified media package.
     *
     * @param mediaPackage
     * the media package to refer to
     */
    constructor(mediaPackage: MediaPackage?) {
        if (mediaPackage == null)
            throw IllegalArgumentException("Parameter media package must not be null")
        type = MediaPackageReference.TYPE_MEDIAPACKAGE
        if (mediaPackage.identifier != null)
            identifier = mediaPackage.identifier.toString()
        else
            identifier = MediaPackageReference.SELF
        properties = HashMap()
    }

    /**
     * Creates a reference to the specified media package element.
     *
     *
     * Note that the referenced element must already be part of the media package, otherwise a
     * `MediaPackageException` will be thrown as the object holding this reference is added to the media
     * package.
     *
     * @param mediaPackageElement
     * the media package element to refer to
     */
    constructor(mediaPackageElement: MediaPackageElement?) {
        if (mediaPackageElement == null)
            throw IllegalArgumentException("Parameter media package element must not be null")
        this.type = mediaPackageElement.elementType.toString().toLowerCase()
        this.identifier = mediaPackageElement.identifier
        if (identifier == null)
            throw IllegalArgumentException("Media package element must have an identifier")
        this.properties = HashMap()
    }

    /**
     * Creates a reference to the entity identified by `type` and `identifier`.
     *
     * @param type
     * the reference type
     * @param identifier
     * the reference identifier
     */
    @JvmOverloads
    constructor(type: String? = MediaPackageReference.TYPE_MEDIAPACKAGE, identifier: String? = MediaPackageReference.SELF) {
        if (type == null)
            throw IllegalArgumentException("Parameter type must not be null")
        if (identifier == null)
            throw IllegalArgumentException("Parameter identifier must not be null")
        this.type = type
        this.identifier = identifier
        this.properties = HashMap()
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackageReference.getProperty
     */
    override fun getProperty(key: String): String {
        return properties!![key]
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackageReference.setProperty
     */
    override fun setProperty(key: String, value: String?) {
        if (value == null)
            this.properties!!.remove(key)
        this.properties!![key] = value
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageReference.matches
     */
    override fun matches(reference: MediaPackageReference?): Boolean {
        if (reference == null)
            return false

        // type
        if (type != reference.type)
            return false

        // properties
        if (properties != null && properties != reference.properties)
            return false
        else if (reference.properties != null && reference.properties != properties)
            return false

        // identifier
        if (identifier == reference.identifier)
            return true
        else if (MediaPackageReference.ANY == identifier || MediaPackageReference.ANY == reference.identifier)
            return true
        else if (MediaPackageReference.SELF == identifier || MediaPackageReference.SELF == reference.identifier)
            return true

        return false
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object.clone
     */
    override fun clone(): Any {
        val clone = MediaPackageReferenceImpl(type, identifier)
        clone.properties!!.putAll(properties!!)
        return clone
    }

    /**
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return toString().hashCode()
    }

    /**
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        if (obj == null || obj !is MediaPackageReference)
            return false
        val ref = obj as MediaPackageReference?
        return type == ref!!.type && identifier == ref.identifier
    }

    /**
     * @see java.lang.Object.toString
     */
    override fun toString(): String {
        if (externalForm == null) {
            val buf = StringBuffer()
            if (MediaPackageReference.TYPE_MEDIAPACKAGE == type && MediaPackageReference.SELF == identifier) {
                buf.append("self")
            } else {
                buf.append(type)
                buf.append(":")
                buf.append(identifier)
            }
            if (properties!!.size > 0) {
                for ((key, value) in properties!!) {
                    buf.append(";")
                    buf.append(key)
                    buf.append("=")
                    buf.append(value)
                }
            }
            externalForm = buf.toString()
        }
        return externalForm
    }

    class Adapter : XmlAdapter<String, MediaPackageReference>() {
        @Throws(Exception::class)
        override fun marshal(ref: MediaPackageReference?): String? {
            return ref?.toString()
        }

        @Throws(Exception::class)
        override fun unmarshal(ref: String?): MediaPackageReference? {
            return if (ref == null) null else MediaPackageReferenceImpl.fromString(ref)
        }
    }

    companion object {

        /** Convenience reference that matches any media package  */
        val ANY_MEDIAPACKAGE: MediaPackageReference = MediaPackageReferenceImpl(MediaPackageReference.TYPE_MEDIAPACKAGE, MediaPackageReference.ANY)

        /** Convenience reference that matches the current media package  */
        val SELF_MEDIAPACKAGE: MediaPackageReference = MediaPackageReferenceImpl(MediaPackageReference.TYPE_MEDIAPACKAGE, MediaPackageReference.SELF)

        /** Convenience reference that matches any series  */
        val ANY_SERIES: MediaPackageReference = MediaPackageReferenceImpl(MediaPackageReference.TYPE_SERIES, "*")

        /**
         * Returns a media package reference from the given string.
         *
         * @return the media package reference
         * @throws IllegalArgumentException
         * if the string is malformed
         */
        @Throws(IllegalArgumentException::class)
        fun fromString(reference: String?): MediaPackageReference {
            if (reference == null)
                throw IllegalArgumentException("Reference is null")

            var ref: MediaPackageReference? = null

            val parts = reference.split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            val elementReference = parts[0]

            // Check for special reference
            if ("self" == elementReference)
                ref = MediaPackageReferenceImpl(MediaPackageReference.TYPE_MEDIAPACKAGE, "self")
            else {
                val elementReferenceParts = elementReference.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                if (elementReferenceParts.size != 2)
                    throw IllegalArgumentException("Reference $reference is malformed")
                ref = MediaPackageReferenceImpl(elementReferenceParts[0], elementReferenceParts[1])
            }

            // Process the reference properties
            for (i in 1 until parts.size) {
                val propertyParts = parts[i].split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                if (propertyParts.size != 2)
                    throw IllegalStateException("malformatted reference properties")
                val key = propertyParts[0]
                val value = propertyParts[1]
                ref.setProperty(key, value)
            }

            return ref
        }
    }
}
/**
 * Creates a reference to the containing media package (`self`).
 */
