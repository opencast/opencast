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

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.FileDataStoreFactory

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileReader
import java.io.IOException
import java.text.MessageFormat

/**
 * @see com.google.api.client.googleapis.auth.oauth2.GoogleCredential
 */
class OAuth2CredentialFactoryImpl : OAuth2CredentialFactory {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Throws(IOException::class)
    override fun getGoogleCredential(credentials: ClientCredentials): GoogleCredential {
        // Use the default which is file-based; name and location are configurable
        val datastore = getDataStore(credentials.credentialDatastore,
                credentials.dataStoreDirectory)
        return getGoogleCredential(datastore, credentials)
    }

    @Throws(IOException::class)
    override fun getDataStore(id: String?, dataStoreDirectory: String?): DataStore<StoredCredential> {
        return FileDataStoreFactory(File(dataStoreDirectory!!)).getDataStore(id)
    }

    @Throws(IOException::class)
    override fun getGoogleCredential(datastore: DataStore<StoredCredential>, authContext: ClientCredentials): GoogleCredential {
        val gCred: GoogleCredential
        val localReceiver = LocalServerReceiver()
        val accessToken: String
        val refreshToken: String

        try {
            // Reads the client id and client secret from a file name passed in authContext
            val gClientSecrets = GoogleClientSecrets.load(JacksonFactory(),
                    FileReader(authContext.clientSecrets!!))

            // Obtain tokens from credential in data store, or obtains a new one from
            // Google if one doesn't exist
            val sCred = datastore.get(authContext.clientId)
            if (sCred != null) {
                accessToken = sCred.accessToken
                refreshToken = sCred.refreshToken
                logger.debug(MessageFormat.format("Found credential for client {0} in data store {1}", authContext.clientId, datastore.id))
            } else {
                // This flow supports installed applications
                val flow = GoogleAuthorizationCodeFlow.Builder(NetHttpTransport(), JacksonFactory(),
                        gClientSecrets, authContext.scopes).setCredentialDataStore(datastore).setApprovalPrompt("auto")
                        .setAccessType("offline").build()
                val cred = AuthorizationCodeInstalledApp(flow, localReceiver).authorize(authContext.clientId)
                accessToken = cred.accessToken
                refreshToken = cred.refreshToken
                logger.debug(MessageFormat.format("Created new credential for client {0} in data store {1}", authContext.clientId, datastore.id))
            }
            gCred = GoogleCredential.Builder()
                    .setClientSecrets(gClientSecrets).setJsonFactory(JacksonFactory())
                    .setTransport(NetHttpTransport()).build()
            gCred.accessToken = accessToken
            gCred.refreshToken = refreshToken
            logger.debug(MessageFormat.format("Found credential {0} using {1}", gCred.refreshToken, authContext.toString()))
        } finally {
            localReceiver.stop()
        }
        return gCred
    }
}
