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

import org.easymock.EasyMock.anyObject
import org.easymock.EasyMock.createMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay

import org.opencastproject.publication.youtube.UnitTestUtils

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.util.store.DataStore

import org.json.simple.parser.ParseException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File
import java.io.IOException

class GoogleAPICredentialRequestorTest {

    @Rule
    var testFolder = TemporaryFolder()

    @Test(expected = IllegalArgumentException::class)
    @Throws(IOException::class, ParseException::class)
    fun testInvalidArgs() {
        GoogleAPICredentialRequestor.main(arrayOfNulls(0))
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(IOException::class, ParseException::class)
    fun testFileDoesNotExist() {
        GoogleAPICredentialRequestor.main(arrayOf("/this/path/does/exist", "credentialDataStore", "dataStoreDirectory"))
    }

    @Test
    @Throws(IOException::class, ParseException::class)
    fun testClientSecrets() {
        val clientId = "clientId"
        val credentialDataStore = "credentialDataStore"
        val dataStoreDirectory = "dataStoreDirectory"
        val clientSecrets = UnitTestUtils.getMockClientSecretsFile(clientId,
                testFolder.newFile("client-secrets-youtube-v3.json"))
        try {
            val credentialFactory = createMock<OAuth2CredentialFactory>(OAuth2CredentialFactory::class.java)
            expect<DataStore<StoredCredential>>(credentialFactory.getDataStore(credentialDataStore, dataStoreDirectory))
                    .andReturn(createMock<DataStore<StoredCredential>>(DataStore<*>::class.java)).once()
            expect(credentialFactory.getGoogleCredential(anyObject<DataStore<*>>(DataStore<*>::class.java), anyObject(ClientCredentials::class.java)))
                    .andReturn(GoogleCredential()).once()
            replay(credentialFactory)
            GoogleAPICredentialRequestor.setCredentialFactory(credentialFactory)
            GoogleAPICredentialRequestor
                    .main(arrayOf(clientSecrets.absolutePath, credentialDataStore, dataStoreDirectory))
        } finally {
            clientSecrets?.delete()
        }
    }
}
