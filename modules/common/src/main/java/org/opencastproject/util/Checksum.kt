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

import org.apache.commons.io.IOUtils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.XmlValue

/**
 * This class stores value and type of a generated checksum.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "checksum", namespace = "http://mediapackage.opencastproject.org")
class Checksum : Serializable {

    /** The checksum value  */
    /**
     * Returns the checksum value.
     *
     * @return the value
     */
    @XmlValue
    var value: String? = null
        protected set

    /** The checksum type  */
    /**
     * Returns the checksum type.
     *
     * @return the type
     */
    @XmlAttribute(name = "type")
    var type: ChecksumType? = null
        protected set

    /** Needed by JAXB  */
    constructor() {}

    /**
     * Creates a new checksum object of the specified value and checksum type.
     *
     * @param value
     * the value
     * @param type
     * the type
     */
    private constructor(value: String?, type: ChecksumType?) {
        if (value == null)
            throw IllegalArgumentException("Checksum value is null")
        if (type == null)
            throw IllegalArgumentException("Checksum type is null")
        this.value = value
        this.type = type
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is Checksum) {
            val c = obj as Checksum?
            return type == c!!.type && value == c.value
        }
        return false
    }

    override fun hashCode(): Int {
        return value!!.hashCode()
    }

    override fun toString(): String {
        return "$value ($type)"
    }

    companion object {

        /** Serial version uid  */
        private const val serialVersionUID = 1L

        /**
         * Converts the checksum to a hex string.
         *
         * @param data
         * the digest
         * @return the digest hex representation
         */
        fun convertToHex(data: ByteArray): String {
            val buf = StringBuffer()
            for (i in data.indices) {
                var halfbyte = data[i].ushr(4) and 0x0F
                var twoHalfs = 0
                do {
                    if (0 <= halfbyte && halfbyte <= 9)
                        buf.append(('0'.toInt() + halfbyte).toChar())
                    else
                        buf.append(('a'.toInt() + (halfbyte - 10)).toChar())
                    halfbyte = data[i] and 0x0F
                } while (twoHalfs++ < 1)
            }
            return buf.toString()
        }

        /**
         * Creates a checksum from a string in the form "value (type)".
         *
         * @param checksum
         * the checksum in string form
         * @return the checksum
         * @throws NoSuchAlgorithmException
         * if the checksum of the specified type cannot be created
         */
        @Throws(NoSuchAlgorithmException::class)
        fun fromString(checksum: String): Checksum {
            val checksumParts = checksum.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

            if (checksumParts.size != 2) {
                throw IllegalArgumentException("Invalid string for checksum!")
            }

            val value = checksumParts[0]
            val type = checksumParts[1].replace("(", "").replace(")", "")

            return create(type, value)
        }

        /**
         * Creates a checksum of type `type` and value `value`.
         *
         * @param type
         * the checksum type name
         * @param value
         * the checksum value
         * @return the checksum
         * @throws NoSuchAlgorithmException
         * if the checksum of the specified type cannot be created
         */
        @Throws(NoSuchAlgorithmException::class)
        fun create(type: String, value: String): Checksum {
            val t = ChecksumType.fromString(type)
            return Checksum(value, t)
        }

        /**
         * Creates a checksum of type `type` and value `value`.
         *
         * @param type
         * the checksum type
         * @param value
         * the checksum value
         * @return the checksum
         */
        fun create(type: ChecksumType, value: String): Checksum {
            return Checksum(value, type)
        }

        /**
         * Creates a checksum of type `type` from the given file.
         *
         * @param type
         * the checksum type
         * @param file
         * the file
         * @return the checksum
         * @throws IOException
         * if the file cannot be accessed
         */
        @Throws(IOException::class)
        fun create(type: ChecksumType, file: File): Checksum {
            return create(type, BufferedInputStream(FileInputStream(file)))
        }

        /**
         * Creates a checksum of type `type` from the given input stream.
         * The stream gets closed afterwards.
         */
        @Throws(IOException::class)
        fun create(type: ChecksumType, `is`: InputStream): Checksum {
            val checksum: MessageDigest
            try {
                checksum = MessageDigest.getInstance(type.name)
            } catch (e: NoSuchAlgorithmException) {
                throw IllegalStateException("This system does not support checksums of type " + type.name!!)
            }

            try {
                val bytes = ByteArray(1024)
                var len = 0
                while ((len = `is`.read(bytes)) >= 0) {
                    checksum.update(bytes, 0, len)
                }
            } finally {
                IoSupport.closeQuietly(`is`)
            }
            return Checksum(convertToHex(checksum.digest()), type)
        }

        /** Create a checksum of type `type` for the given `string`.  */
        @Throws(IOException::class)
        fun createFor(type: ChecksumType, string: String): Checksum {
            return create(type, IOUtils.toInputStream(string, "UTF-8"))
        }
    }
}
