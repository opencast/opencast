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

package org.opencastproject.workflow.handler.distribution

import org.junit.Assert.assertEquals

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.identifier.Id
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.workflow.api.WorkflowOperationException

import org.easymock.EasyMock
import org.junit.Test

import java.net.URI
import java.net.URISyntaxException
import java.util.HashMap

class PublishEngageWorkflowOperationHandlerTest {
    private var org: Organization? = null

    // Util to set org properties with player path
    private val orgWithPlayerPath: Organization?
        get() {
            org = EasyMock.createNiceMock<Organization>(Organization::class.java)
            EasyMock.replay(org!!)
            return org
        }

    // Util to set org properties without player path
    val orgWithoutPlayerPath: Organization
        get() {
            val properties = HashMap<String, String>()
            org = EasyMock.createNiceMock<Organization>(Organization::class.java)
            EasyMock.expect(org!!.properties).andStubReturn(properties)
            EasyMock.replay(org!!)
            return org
        }

    @Test
    @Throws(WorkflowOperationException::class, URISyntaxException::class)
    fun testPlayerUrl() {
        val engageURI = URI("http://engage.org")
        val mpId = "mp-id"

        val mp = EasyMock.createNiceMock<MediaPackage>(MediaPackage::class.java)
        val id = IdImpl(mpId)
        EasyMock.expect(mp.identifier).andStubReturn(id)
        val element = EasyMock.createNiceMock<MediaPackageElement>(MediaPackageElement::class.java)
        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.organization).andReturn(orgWithPlayerPath).once()
        EasyMock.replay(element, mp, securityService)

        // Test configured organization player path
        val publishEngagePublish = PublishEngageWorkflowOperationHandler()
        publishEngagePublish.setSecurityService(securityService)
        val result = publishEngagePublish.createEngageUri(engageURI, mp)
        assertEquals("$engageURI/play/$mpId", result.toString())
    }

    @Test
    @Throws(URISyntaxException::class)
    fun testDefaultPlayerPath() {
        val engageURI = URI("http://engage.org")
        val mpId = "mp-id"

        val mp = EasyMock.createNiceMock<MediaPackage>(MediaPackage::class.java)
        val id = IdImpl(mpId)
        EasyMock.expect(mp.identifier).andStubReturn(id)
        val element = EasyMock.createNiceMock<MediaPackageElement>(MediaPackageElement::class.java)
        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.organization).andReturn(orgWithoutPlayerPath).once()
        EasyMock.replay(element, mp, securityService)

        // Test default player path
        val publishEngagePublish = PublishEngageWorkflowOperationHandler()
        publishEngagePublish.setSecurityService(securityService)
        val result = publishEngagePublish.createEngageUri(engageURI, mp)
        assertEquals(engageURI.toString() + PublishEngageWorkflowOperationHandler.PLAYER_PATH + mpId,
                result.toString())

    }

}
