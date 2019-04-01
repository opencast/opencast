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

package org.opencastproject.security.api

import org.opencastproject.util.data.Either.left
import org.opencastproject.util.data.Either.right
import org.opencastproject.util.data.functions.Misc.chuck

import org.opencastproject.util.data.Either
import org.opencastproject.util.data.Function

import org.apache.commons.io.IOUtils
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException

import java.io.IOException
import java.io.InputStream
import java.io.StringWriter
import java.io.Writer

import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.transform.stream.StreamSource

/**
 * Marshals and unmarshals [AccessControlList]s to/from XML.
 */
object AccessControlParser {
    /** Role constant used in JSON formatted access control entries  */
    val ROLE = "role"

    /** Action constant used in JSON formatted access control entries  */
    val ACTION = "action"

    /** Allow constant used in JSON formatted access control entries  */
    val ALLOW = "allow"

    /** ACL constant used in JSON formatted access control entries  */
    val ACL = "acl"

    /** ACE constant used in JSON formatted access control entries  */
    val ACE = "ace"

    /** Encoding expected from all inputs  */
    val ENCODING = "UTF-8"

    private val jaxbContext: JAXBContext

    /** [.parseAclSilent] as a function.  */
    val parseAclSilent: Function<String, AccessControlList> = object : Function<String, AccessControlList>() {
        override fun apply(s: String): AccessControlList {
            return parseAclSilent(s)
        }
    }

    /** Functional version of [.parseAcl].  */
    val parseAcl: Function<String, Either<Exception, AccessControlList>> = object : Function<String, Either<Exception, AccessControlList>>() {
        override fun apply(s: String): Either<Exception, AccessControlList> {
            try {
                return right(parseAcl(s))
            } catch (e: Exception) {
                return left(e)
            }

        }
    }

    val toJsonSilent: Function<AccessControlList, String> = object : Function<AccessControlList, String>() {
        override fun apply(acl: AccessControlList): String {
            return toJsonSilent(acl)
        }
    }

    init {
        try {
            jaxbContext = JAXBContext.newInstance("org.opencastproject.security.api",
                    AccessControlParser::class.java!!.getClassLoader())
        } catch (e: JAXBException) {
            throw IllegalStateException(e)
        }

    }

    /**
     * Parses a string into an ACL.
     *
     * @param serializedForm
     * the string containing the xml or json formatted access control list.
     * @return the access control list
     * @throws IOException
     * if the encoding is invalid
     * @throws AccessControlParsingException
     * if the format is invalid
     */
    @Throws(IOException::class, AccessControlParsingException::class)
    fun parseAcl(serializedForm: String): AccessControlList {
        // Determine whether to parse this as XML or JSON
        return if (serializedForm.startsWith("{")) {
            parseJson(serializedForm)
        } else {
            parseXml(IOUtils.toInputStream(serializedForm, ENCODING))
        }
    }

    /** Same like [.parseAcl] but throws runtime exceptions in case of an error.  */
    fun parseAclSilent(serializedForm: String): AccessControlList {
        try {
            return parseAcl(serializedForm)
        } catch (e: Exception) {
            return chuck(e)
        }

    }

    /**
     * Unmarshals an ACL from an xml input stream.
     *
     * @param in
     * the xml input stream
     * @return the acl
     * @throws IOException
     * if there is a problem unmarshaling the stream
     * @throws AccessControlParsingException
     * if the format is invalid
     */
    @Throws(IOException::class, AccessControlParsingException::class)
    fun parseAcl(`in`: InputStream): AccessControlList {
        return parseAcl(IOUtils.toString(`in`, ENCODING))
    }

    /**
     * Parses a JSON stream to an ACL.
     *
     * @param content
     * the JSON stream
     * @return the access control list
     * @throws AccessControlParsingException
     * if the json is not properly formatted
     */
    @Throws(AccessControlParsingException::class)
    private fun parseJson(content: String): AccessControlList {
        try {
            val json = JSONParser().parse(content) as JSONObject
            val jsonAcl = json[ACL] as JSONObject
            val jsonAceObj = jsonAcl[ACE]

            val acl = AccessControlList()
            if (jsonAceObj == null)
                return acl

            if (jsonAceObj is JSONObject) {
                val jsonAce = jsonAceObj as JSONObject?
                acl.entries!!.add(getAce(jsonAce!!))
            } else {
                val jsonAceArray = jsonAceObj as JSONArray?
                for (element in jsonAceArray!!) {
                    val jsonAce = element as JSONObject
                    acl.entries!!.add(getAce(jsonAce))
                }
            }
            return acl
        } catch (e: ParseException) {
            throw AccessControlParsingException(e)
        }

    }

    /**
     * Converts a JSON representation of an access control entry to an [AccessControlEntry].
     *
     * @param jsonAce
     * the json object
     * @return the access control entry
     */
    private fun getAce(jsonAce: JSONObject): AccessControlEntry {
        val role = jsonAce[ROLE] as String
        val action = jsonAce[ACTION] as String
        val allow = jsonAce[ALLOW] as Boolean
        return AccessControlEntry(role, action, allow)
    }

    /**
     * Parses an XML stream to an ACL.
     *
     * @param in
     * the XML stream
     * @throws IOException
     * if there is a problem unmarshaling the stream
     */
    @Throws(IOException::class, AccessControlParsingException::class)
    private fun parseXml(`in`: InputStream): AccessControlList {
        val unmarshaller: Unmarshaller
        try {
            unmarshaller = jaxbContext.createUnmarshaller()
            return unmarshaller.unmarshal<AccessControlList>(StreamSource(`in`), AccessControlList::class.java).value
        } catch (e: Exception) {
            if (e is IOException) {
                throw e
            } else {
                throw AccessControlParsingException(e)
            }
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    /**
     * Serializes an AccessControlList to its XML form.
     *
     * @param acl
     * the access control list
     * @return the xml as a string
     * @throws IOException
     * if there is a problem marshaling the xml
     */
    @Throws(IOException::class)
    fun toXml(acl: AccessControlList): String {
        try {
            val marshaller = jaxbContext.createMarshaller()
            val writer = StringWriter()
            marshaller.marshal(acl, writer)
            return writer.toString()
        } catch (e: JAXBException) {
            throw IOException(e)
        }

    }

    @Throws(IOException::class)
    fun toJson(acl: AccessControlList): String {
        val json = JSONObject()
        val jsonAcl = JSONObject()
        val entries = acl.entries
        val numEntries = entries!!.size
        when (numEntries) {
            0 -> {
            }
            1 -> {
                val singleEntry = entries[0]
                val singleJsonEntry = JSONObject()
                jsonAcl[ACE] = singleJsonEntry
                singleJsonEntry[ACTION] = singleEntry.action
                singleJsonEntry[ROLE] = singleEntry.role
                singleJsonEntry[ALLOW] = singleEntry.isAllow
            }
            else -> {
                val jsonEntryArray = JSONArray()
                jsonAcl[ACE] = jsonEntryArray
                for (entry in entries) {
                    val jsonEntry = JSONObject()
                    jsonEntry[ACTION] = entry.action
                    jsonEntry[ROLE] = entry.role
                    jsonEntry[ALLOW] = entry.isAllow
                    jsonEntryArray.add(jsonEntry)
                }
            }
        }
        json[ACL] = jsonAcl
        return json.toJSONString()
    }

    fun toJsonSilent(acl: AccessControlList): String {
        try {
            return toJson(acl)
        } catch (e: IOException) {
            return chuck(e)
        }

    }
}
/**
 * Disallow construction of this utility class.
 */
