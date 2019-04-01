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

package org.opencastproject.staticfiles.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.fail

import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.util.NotFoundException

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.easymock.EasyMock
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.osgi.framework.BundleContext
import org.osgi.service.cm.ConfigurationException
import org.osgi.service.component.ComponentContext

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.util.ArrayList
import java.util.Dictionary
import java.util.Properties

class StaticFileServiceImplTest {
    /** The org directory service  */
    private var orgDir: OrganizationDirectoryService? = null

    @Before
    @Throws(IOException::class)
    fun setUp() {
        FileUtils.forceMkdir(rootDir!!)

        orgDir = EasyMock.createNiceMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)
        EasyMock.expect(orgDir!!.organizations).andReturn(ArrayList()).anyTimes()
        EasyMock.replay(orgDir!!)
    }

    @After
    fun tearDown() {
        FileUtils.deleteQuietly(rootDir)
    }

    /**
     * Without the root directory the service should throw a RuntimeException.
     */
    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun testStoreStaticFileThrowsConfigurationException() {
        val bundleContext = EasyMock.createMock<BundleContext>(BundleContext::class.java)
        EasyMock.expect(bundleContext.getProperty(EasyMock.anyObject(String::class.java))).andStubReturn(null)
        EasyMock.replay(bundleContext)

        val cc = EasyMock.createMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(cc.bundleContext).andStubReturn(bundleContext)
        EasyMock.replay(cc)

        // Run the test
        val staticFile = StaticFileServiceImpl()
        staticFile.setOrganizationDirectoryService(orgDir)
        staticFile.activate(cc)
    }

    @Test
    @Throws(Exception::class)
    fun testGetStaticFile() {
        val staticFileServiceImpl = StaticFileServiceImpl()
        staticFileServiceImpl.setOrganizationDirectoryService(orgDir)
        staticFileServiceImpl.activate(getComponentContext(null))
        staticFileServiceImpl.setSecurityService(securityService)

        val videoUUID = staticFileServiceImpl.storeFile(videoFilename, FileInputStream(videoFile!!))
        IOUtils.contentEquals(FileInputStream(videoFile!!), staticFileServiceImpl.getFile(videoUUID))

        val imageUUID = staticFileServiceImpl.storeFile(imageFilename, FileInputStream(imageFile!!))
        IOUtils.contentEquals(FileInputStream(imageFile!!), staticFileServiceImpl.getFile(imageUUID))
    }

    @Test
    @Throws(Exception::class)
    fun testPersistFile() {
        val staticFileServiceImpl = StaticFileServiceImpl()
        staticFileServiceImpl.setOrganizationDirectoryService(orgDir)
        staticFileServiceImpl.activate(getComponentContext(null))
        staticFileServiceImpl.setSecurityService(securityService)

        val videoUUID = staticFileServiceImpl.storeFile(videoFilename, FileInputStream(videoFile!!))
        val imageUUID = staticFileServiceImpl.storeFile(imageFilename, FileInputStream(imageFile!!))

        staticFileServiceImpl.persistFile(videoUUID)
        staticFileServiceImpl.purgeTemporaryStorageSection(securityService.organization.id, 0)

        IOUtils.contentEquals(FileInputStream(videoFile!!), staticFileServiceImpl.getFile(videoUUID))
        try {
            staticFileServiceImpl.getFile(imageUUID)
            fail("File should no longer exist")
        } catch (e: NotFoundException) {
            // expected
        }

    }

    @Test
    @Throws(ConfigurationException::class, FileNotFoundException::class, IOException::class)
    fun testDeleteStaticFile() {
        val staticFileServiceImpl = StaticFileServiceImpl()
        staticFileServiceImpl.setOrganizationDirectoryService(orgDir)
        staticFileServiceImpl.activate(getComponentContext(null))
        staticFileServiceImpl.setSecurityService(securityService)
        val imageUUID = staticFileServiceImpl.storeFile(imageFilename, FileInputStream(imageFile!!))

        try {
            staticFileServiceImpl.deleteFile(imageUUID)
        } catch (e: NotFoundException) {
            Assert.fail("File not found for deletion")
        }

        try {
            staticFileServiceImpl.getFile(imageUUID)
            Assert.fail("File not deleted")
        } catch (e: NotFoundException) {
            Assert.assertNotNull(e)
        }

        try {
            staticFileServiceImpl.deleteFile(imageUUID)
            Assert.fail("File not deleted")
        } catch (e: NotFoundException) {
            Assert.assertNotNull(e)
        }

    }

    @Test
    @Throws(Exception::class)
    fun testGetFileName() {
        val staticFileServiceImpl = StaticFileServiceImpl()
        staticFileServiceImpl.setOrganizationDirectoryService(orgDir)
        staticFileServiceImpl.activate(getComponentContext(null))
        staticFileServiceImpl.setSecurityService(securityService)

        val imageUUID = staticFileServiceImpl.storeFile(imageFilename, FileInputStream(imageFile!!))
        assertEquals(imageFile!!.name, staticFileServiceImpl.getFileName(imageUUID))
    }

    companion object {

        private val videoFilename = "av.mov"
        private val imageFilename = "image.jpg"
        /** The File object that is an example image  */
        private var imageFile: File? = null
        /** Location where the files are copied to  */
        private var rootDir: File? = null
        /** The File object that is an example video  */
        private var videoFile: File? = null
        /** The org to use for the tests  */
        private val org = DefaultOrganization()

        @BeforeClass
        @Throws(Exception::class)
        fun beforeClass() {
            rootDir = Files.createTempDirectory("static-file-service-test").toFile()
            imageFile = File(StaticFileServiceImplTest::class.java.getResource("/$imageFilename").toURI())
            videoFile = File(StaticFileServiceImplTest::class.java.getResource("/$videoFilename").toURI())
        }

        private fun getComponentContext(useWebserver: String?): ComponentContext {
            // Create BundleContext
            val bundleContext = EasyMock.createMock<BundleContext>(BundleContext::class.java)
            EasyMock.expect(bundleContext.getProperty(StaticFileServiceImpl.STATICFILES_ROOT_DIRECTORY_KEY))
                    .andReturn(rootDir!!.absolutePath)
            EasyMock.replay(bundleContext)
            // Create ComponentContext
            val properties = Properties()
            val cc = EasyMock.createMock<ComponentContext>(ComponentContext::class.java)
            EasyMock.expect(cc.properties).andReturn(properties).anyTimes()
            EasyMock.expect(cc.bundleContext).andReturn(bundleContext).anyTimes()
            EasyMock.replay(cc)
            return cc
        }

        private val securityService: SecurityService
            get() {
                val securityService = EasyMock.createMock<SecurityService>(SecurityService::class.java)
                EasyMock.expect(securityService.organization).andReturn(org).anyTimes()
                EasyMock.replay(securityService)
                return securityService
            }
    }

}
