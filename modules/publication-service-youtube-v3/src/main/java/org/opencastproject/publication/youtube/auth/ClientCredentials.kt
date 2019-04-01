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


package org.opencastproject.publication.youtube.auth

import org.opencastproject.util.data.Collections

import org.apache.commons.lang3.builder.ToStringBuilder
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException

import java.io.File
import java.io.FileReader
import java.io.IOException

/**
 * `ClientCredentials` class represents the set of parameters required to make an authorization
 * request.
 */
class ClientCredentials {

    var clientId: String? = null
        private set
    var credentialDatastore: String? = null
    var dataStoreDirectory: String? = null
    var clientSecrets: File? = null
        @Throws(IOException::class, ParseException::class)
        set(clientSecrets) {
            field = clientSecrets
            this.clientId = getValueFromArray(clientSecrets)
        }

    val scopes: List<String>
        get() = Collections.list("https://www.googleapis.com/auth/youtube",
                "https://www.googleapis.com/auth/youtube.upload", "https://www.googleapis.com/auth/youtube.readonly")

    override fun toString(): String {
        return ToStringBuilder.reflectionToString(this)
    }

    /**
     * Parses a file and returns a value matching the keys provided if it exists.
     * The file is assumed to be composed of one array, with each array element
     * in turn being a JSONObject containing a name : value pair.
     *
     * @param file
     * file to parse
     * @return matching value, or null if no match or there was a parse exception
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    @Throws(IOException::class, ParseException::class)
    private fun getValueFromArray(file: File): String {
        val parser = JSONParser()
        val reader = FileReader(file)
        val jsonObject = parser.parse(reader) as JSONObject
        val array = jsonObject["installed"] as JSONObject
        return array["client_id"] as String
    }

}
