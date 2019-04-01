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
package org.opencastproject.security.urlsigning

import org.junit.Assert.assertEquals

import org.opencastproject.security.urlsigning.utils.UrlSigningServiceOsgiUtil

import org.junit.Test
import org.osgi.service.cm.ConfigurationException

import java.util.Properties

class SigningMediaPackageSerializerTest {
    @Test
    @Throws(ConfigurationException::class)
    fun testUpdated() {
        val testValue = 1339L
        val properties = Properties()
        val serializer = SigningMediaPackageSerializer()
        assertEquals(UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION, serializer.expirationSeconds)
        serializer.updated(properties)
        assertEquals(UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION, serializer.expirationSeconds)
        properties[UrlSigningServiceOsgiUtil.URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY] = testValue.toString()
        serializer.updated(properties)
        assertEquals(testValue, serializer.expirationSeconds)
    }
}
