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

package org.opencastproject.uiconfig

import org.opencastproject.uiconfig.UIConfigRest.UI_CONFIG_FOLDER_PROPERTY

import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.util.ConfigurationException
import org.opencastproject.util.NotFoundException

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileOutputStream
import java.nio.file.AccessDeniedException
import java.nio.file.Paths

import javax.ws.rs.core.Response

class UIConfigTest {

    private var uiConfigRest: UIConfigRest? = null

    @Rule
    var temporaryFolder = TemporaryFolder()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        // create the needed mocks
        val organization = EasyMock.createMock<Organization>(Organization::class.java)
        EasyMock.expect(organization.id).andReturn("org1").anyTimes()

        val securityService = EasyMock.createMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.organization).andReturn(organization).anyTimes()

        EasyMock.replay(organization, securityService)

        // Set up UI config service
        uiConfigRest = UIConfigRest()
        uiConfigRest!!.setSecurityService(securityService)
    }

    @Test
    @Throws(Exception::class)
    fun testActivate() {
        val bundleContext = EasyMock.createMock<BundleContext>(BundleContext::class.java)
        EasyMock.expect(bundleContext.getProperty(UI_CONFIG_FOLDER_PROPERTY)).andReturn(null).times(2)
        EasyMock.expect(bundleContext.getProperty(UI_CONFIG_FOLDER_PROPERTY)).andReturn("/xy").once()
        EasyMock.expect(bundleContext.getProperty("karaf.etc")).andReturn(null).once()
        EasyMock.expect(bundleContext.getProperty("karaf.etc")).andReturn("/xy").once()

        val componentContext = EasyMock.createMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(componentContext.bundleContext).andReturn(bundleContext).anyTimes()

        EasyMock.replay(bundleContext, componentContext)

        try {
            uiConfigRest!!.activate(componentContext)
            Assert.fail()
        } catch (e: ConfigurationException) {
            // config and default are null. We expect this to fail
        }

        // Providing proper configuration now. This should work
        uiConfigRest!!.activate(componentContext)
        uiConfigRest!!.activate(componentContext)
    }

    @Test
    @Throws(Exception::class)
    fun testGetFile() {
        val testDir = temporaryFolder.newFolder()
        val bundleContext = EasyMock.createMock<BundleContext>(BundleContext::class.java)
        EasyMock.expect(bundleContext.getProperty(UI_CONFIG_FOLDER_PROPERTY)).andReturn(testDir.absolutePath).once()

        val componentContext = EasyMock.createMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(componentContext.bundleContext).andReturn(bundleContext).anyTimes()

        EasyMock.replay(bundleContext, componentContext)

        uiConfigRest!!.activate(componentContext)

        // test non-existing file
        try {
            uiConfigRest!!.getConfigFile("player", "config.json")
            Assert.fail()
        } catch (e: NotFoundException) {
            // We expect this to not be found
        }

        // test existing file
        val target = Paths.get(testDir.absolutePath, "org1", "player", "config.json").toFile()
        Assert.assertTrue(target.parentFile.mkdirs())
        FileOutputStream(target).close()
        val response = uiConfigRest!!.getConfigFile("player", "config.json")
        Assert.assertEquals(200, response.status.toLong())

        // test path traversal
        try {
            uiConfigRest!!.getConfigFile("../player", "config.json")
            Assert.fail()
        } catch (e: AccessDeniedException) {
            // we expect access to be denied
        }

    }

    companion object {
        private val logger = LoggerFactory.getLogger(UIConfigTest::class.java)
    }
}
