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


package org.opencastproject.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.Serializable
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.HashMap

import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.XmlValue
import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

/**
 * Checksum type represents the method used to generate a checksum.
 */
@XmlJavaTypeAdapter(ChecksumType.Adapter::class)
@XmlType(name = "checksumtype", namespace = "http://mediapackage.opencastproject.org")
class ChecksumType : Serializable {

    /** The type name  */
    /**
     * Returns the checksum value.
     *
     * @return the value
     */
    @XmlValue
    var name: String? = null
        protected set

    /** Needed by JAXB  */
    constructor() {}

    /**
     * Creates a new checksum type with the given type name.
     *
     * @param type
     * the type name
     */
    protected constructor(type: String) {
        this.name = type
        TYPES[type] = this
    }

    /**
     * @see java.lang.Object.equals
     */
    override fun equals(obj: Any?): Boolean {
        return if (obj is ChecksumType) {
            name == obj.name
        } else false
    }

    /**
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        return name!!.hashCode()
    }

    override fun toString(): String? {
        return name
    }

    internal class Adapter : XmlAdapter<String, ChecksumType>() {
        @Throws(Exception::class)
        override fun marshal(checksumType: ChecksumType): String? {
            return checksumType.name
        }

        @Throws(Exception::class)
        override fun unmarshal(str: String): ChecksumType {
            try {
                return ChecksumType.fromString(str)
            } catch (e: NoSuchAlgorithmException) {
                logger.warn(e.message, e)
                throw e
            }

        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ChecksumType::class.java!!)

        /** Serial version uid  */
        private const val serialVersionUID = 1L

        /** List of all known checksum types  */
        private val TYPES = HashMap<String, ChecksumType>()

        /** Default type md5  */
        val DEFAULT_TYPE = ChecksumType("md5")

        /**
         * Returns a checksum type for the given string. `Type` is considered to be the name of a checksum type.
         *
         * @param type
         * the type name
         * @return the checksum type
         * @throws NoSuchAlgorithmException
         * if the digest is not supported by the java environment
         */
        @Throws(NoSuchAlgorithmException::class)
        fun fromString(type: String?): ChecksumType {
            var type: String? = type ?: throw IllegalArgumentException("Argument 'type' is null")
            type = type!!.toLowerCase()
            var checksumType: ChecksumType? = TYPES[type]
            if (checksumType == null) {
                MessageDigest.getInstance(type)
                checksumType = ChecksumType(type)
                TYPES[type] = checksumType
            }
            return checksumType
        }

        /**
         * Returns the type of the checksum gathered from the provided value.
         *
         * @param value
         * the checksum value
         * @return the type
         */
        fun fromValue(value: String): ChecksumType {
            // TODO: Implement
            throw IllegalStateException("Not yet implemented")
        }
    }
}
