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

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.DataStore

import org.json.simple.parser.ParseException

import java.io.File
import java.io.IOException

/**
 * `GoogleAPICredentialRequestor` obtains credentials from Google
 * and persists them in a data store on the local file system for later use
 * when invoking the Google Data API V3 to upload a file to YouTube.
 */
object GoogleAPICredentialRequestor {

    private var credentialFactory: OAuth2CredentialFactory? = null

    /**
     * The authorization process is dependent on authorization context parameters
     * which is data that is required to execute the request and persist the
     * result:
     *
     *  * client secrets - a file containing the client id and password
     *  * credentialDatastore - name of the datastore which stores tokens
     *  * dataStoreDirectory - location of the datastore in the file system
     *
     *
     * @param args
     * override parameters
     */
    @Throws(IOException::class, ParseException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val size = args?.size ?: 0
        if (size != 3) {
            throw IllegalArgumentException("\n[ERROR] Wrong number of arguments: $size\n")
        } else {
            registerOAuth2Credential(File(args[0]), args[1], args[2])
        }
    }

    /**
     * @param clientSecrets Null not allowed.
     * @param credentialDataStore Null not allowed.
     * @param dataStoreDirectory Null not allowed.
     * @throws IOException when file not available
     * @throws ParseException when file is not JSON
     */
    @Throws(IOException::class, ParseException::class)
    private fun registerOAuth2Credential(clientSecrets: File, credentialDataStore: String,
                                         dataStoreDirectory: String) {
        if (clientSecrets.exists()) {
            val c = ClientCredentials()
            c.clientSecrets = clientSecrets
            c.credentialDatastore = credentialDataStore
            c.dataStoreDirectory = dataStoreDirectory
            val factory = if (credentialFactory == null) OAuth2CredentialFactoryImpl() else credentialFactory
            val dataStore = factory.getDataStore(c.credentialDatastore, c.dataStoreDirectory)
            factory.getGoogleCredential(dataStore, c)
        } else {
            throw IllegalArgumentException("The client-secrets file (YouTube OAuth) was not found: " + clientSecrets.absolutePath)
        }
    }

    internal fun setCredentialFactory(oAuth2CredentialFactory: OAuth2CredentialFactory) {
        credentialFactory = oAuth2CredentialFactory
    }
}
/**
 * Private constructor.
 */
