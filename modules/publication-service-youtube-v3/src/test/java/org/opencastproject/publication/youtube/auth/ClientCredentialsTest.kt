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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail

import org.opencastproject.publication.youtube.UnitTestUtils

import org.json.simple.parser.ParseException
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File
import java.io.IOException

class ClientCredentialsTest {

    @Rule
    var testFolder = TemporaryFolder()

    @Test
    fun testToStringNullTolerance() {
        assertNotNull(ClientCredentials().toString())
    }

    @Test
    @Throws(IOException::class, ParseException::class)
    fun testParsingJSON() {
        val clientId = "652137994117.apps.googleusercontent.com"
        val clientSecretsFile = UnitTestUtils.getMockClientSecretsFile(clientId,
                testFolder.newFile("client-secrets-youtube-v3.json"))
        try {
            val cc = ClientCredentials()
            cc.clientSecrets = clientSecretsFile
            assertEquals(clientId, cc.clientId)
        } finally {
            clientSecretsFile.delete()
        }
    }

    @Test
    fun testGetScopes() {
        val cc = ClientCredentials()
        for (scope in cc.scopes) {
            if (!scope.startsWith("https://www.googleapis.com/auth/youtube")) {
                fail("Invalid YouTube v3 auth configuration where scope = $scope")
            }
        }
    }
}
