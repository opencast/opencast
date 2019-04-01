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

package org.opencastproject.metadata.dublincore

import com.entwinemedia.fn.Equality.ne
import org.opencastproject.metadata.dublincore.DublinCore.LANGUAGE_UNDEFINED

import org.opencastproject.mediapackage.EName

import com.entwinemedia.fn.data.Opt

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.collections.Map.Entry
import java.util.SortedSet
import java.util.TreeSet
import javax.annotation.ParametersAreNonnullByDefault

/**
 * Parse a DublinCore catalog from JSON.
 *
 *
 * **Known limitations:** Encoding schemas can currently only be from the
 * [DublinCore.TERMS_NS_URI] namespace using the [DublinCore.TERMS_NS_PREFIX]
 * since the JSON format does not serialize namespace bindings. Example: `dcterms:W3CDTF`
 */
@ParametersAreNonnullByDefault
object DublinCoreJsonFormat {

    /**
     * Read a JSON encoded catalog from a stream.
     */
    @Throws(IOException::class, ParseException::class)
    fun read(json: InputStream): DublinCoreCatalog {
        return read(JSONParser().parse(InputStreamReader(json)) as JSONObject)
    }

    /**
     * Read a JSON encoded catalog from a string.
     */
    @Throws(IOException::class, ParseException::class)
    fun read(json: String): DublinCoreCatalog {
        return read(JSONParser().parse(json) as JSONObject)
    }

    /**
     * Reads values from a JSON object into a new DublinCore catalog.
     */
    fun read(json: JSONObject): DublinCoreCatalog {
        // Use a standard catalog to get the required namespace bindings in order to be able
        // to parse standard DublinCore encoding schemes.
        // See http://dublincore.org/documents/dc-xml-guidelines/, section 5.2, recommendation 7 for details.
        // TODO the JSON representation should serialize the contained bindings like XML to be able to
        //   reconstruct a catalog from the serialization alone without the need to rely on bindings, registered
        //   before.
        val dc = DublinCores.mkStandard()
        val namespaceEntrySet = json.entries
        for (namespaceEntry in namespaceEntrySet) { // e.g. http://purl.org/dc/terms/
            val namespace = namespaceEntry.key
            val namespaceObj = namespaceEntry.value
            val entrySet = namespaceObj.entries
            for (entry in entrySet) { // e.g. title
                val key = entry.key
                val values = entry.value
                for (valueObject in values) {
                    val value = valueObject as JSONObject
                    // the value
                    val valueString = value["value"] as String
                    // the language
                    val lang: String
                    run {
                        val l = value["lang"] as String
                        lang = l ?: LANGUAGE_UNDEFINED
                    }
                    // the encoding scheme
                    val encodingScheme: EName?
                    run {
                        val s = value["type"] as String
                        encodingScheme = if (s != null) dc.toEName(s) else null
                    }
                    // add the new value to this DC document
                    dc.add(EName(namespace, key), valueString, lang, encodingScheme)
                }
            }
        }
        return dc
    }

    /**
     * Converts the catalog to JSON object.
     *
     * @return JSON object
     */
    fun writeJsonObject(dc: DublinCoreCatalog): JSONObject {
        // The top-level json object
        val json = JSONObject()
        // First collect all namespaces
        val namespaces = TreeSet<String>()
        val values = dc.values.entries
        for ((key) in values) {
            namespaces.add(key.namespaceURI)
        }
        // Add a json object for each namespace
        for (namespace in namespaces) {
            json[namespace] = JSONObject()
        }
        // Add the data into the appropriate array
        for ((ename, value1) in values) {
            val namespace = ename.namespaceURI
            val localName = ename.localName
            val namespaceObject = json[namespace] as JSONObject
            val localNameArray: JSONArray
            run {
                val ns = namespaceObject[localName] as JSONArray
                if (ns != null) {
                    localNameArray = ns
                } else {
                    localNameArray = JSONArray()
                    namespaceObject.put(localName, localNameArray)
                }
            }
            for (value in value1) {
                val lang = value.language
                val encScheme = value.encodingScheme
                val v = JSONObject()
                v["value"] = value.value
                if (ne(DublinCore.LANGUAGE_UNDEFINED, lang)) {
                    v["lang"] = lang
                }
                for (e in encScheme) {
                    v["type"] = dc.toQName(e)
                }
                localNameArray.add(v)
            }
        }
        return json
    }
}
