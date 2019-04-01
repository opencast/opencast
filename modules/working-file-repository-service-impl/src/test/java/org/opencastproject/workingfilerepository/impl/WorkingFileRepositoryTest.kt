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


package org.opencastproject.workingfilerepository.impl

import org.junit.Assert.fail

import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.systems.OpencastConstants
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UrlSupport

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.easymock.EasyMock
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.io.File
import java.io.InputStream
import java.util.Arrays
import java.util.HashMap

import junit.framework.Assert

class WorkingFileRepositoryTest {

    private val mediaPackageID = "working-file-test-media-package-1"
    private val mediaPackageElementID = "working-file-test-element-1"
    private val collectionId = "collection-1"
    private val filename = "file.gif"
    private val repo = WorkingFileRepositoryImpl()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val organization = EasyMock.createMock<Organization>(Organization::class.java)
        EasyMock.expect(organization.id).andReturn("org1").anyTimes()
        val orgProps = HashMap<String, String>()
        orgProps[OpencastConstants.WFR_URL_ORG_PROPERTY] = UrlSupport.DEFAULT_BASE_URL
        EasyMock.expect(organization.properties).andReturn(orgProps).anyTimes()
        EasyMock.replay(organization)

        val securityService = EasyMock.createMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.organization).andReturn(organization).anyTimes()
        EasyMock.replay(securityService)

        repo.setSecurityService(securityService)
        repo.pathPrefix = "target" + File.separator + "repotest"
        repo.serverUrl = UrlSupport.DEFAULT_BASE_URL
        repo.servicePath = WorkingFileRepositoryImpl.URI_PREFIX
        repo.createRootDirectory()

        // Put an image file into the repository using the mediapackage / element storage
        var `in`: InputStream? = null
        try {
            `in` = javaClass.classLoader.getResourceAsStream("opencast_header.gif")
            repo.put(mediaPackageID, mediaPackageElementID, "opencast_header.gif", `in`!!)
        } finally {
            IOUtils.closeQuietly(`in`)
        }

        // Repeat the put
        try {
            `in` = javaClass.classLoader.getResourceAsStream("opencast_header.gif")
            repo.put(mediaPackageID, mediaPackageElementID, "opencast_header.gif", `in`!!)
        } finally {
            IOUtils.closeQuietly(`in`)
        }

        // Put an image file into the repository into a collection
        try {
            `in` = javaClass.classLoader.getResourceAsStream("opencast_header.gif")
            repo.putInCollection(collectionId, filename, `in`!!)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.forceDelete(File(repo.pathPrefix))
    }

    @Test
    @Throws(Exception::class)
    fun testPut() {
        // Get the file back from the repository to check whether it's the same file that we put in.
        var fromRepo: InputStream? = null
        var headerIn: InputStream? = null
        try {
            fromRepo = repo[mediaPackageID, mediaPackageElementID]
            headerIn = javaClass.classLoader.getResourceAsStream("opencast_header.gif")
            val bytesFromRepo = IOUtils.toByteArray(fromRepo)
            val bytesFromClasspath = IOUtils.toByteArray(headerIn!!)
            Assert.assertEquals(bytesFromClasspath.size, bytesFromRepo.size)
        } finally {
            IOUtils.closeQuietly(fromRepo)
            IOUtils.closeQuietly(headerIn)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testDelete() {
        // Delete the file and ensure that we can no longer get() it
        repo.delete(mediaPackageID, mediaPackageElementID)
        try {
            Assert.assertTrue(repo[mediaPackageID, mediaPackageElementID] == null)
            fail("File $mediaPackageID/$mediaPackageElementID was not deleted")
        } catch (e: NotFoundException) {
            // This is intended
        }

    }

    @Test
    @Throws(Exception::class)
    fun testPutBadId() {
        // Try adding a file with a bad ID
        val badId = "../etc"
        var `in`: InputStream? = null
        try {
            `in` = javaClass.classLoader.getResourceAsStream("opencast_header.gif")
            repo.put(badId, mediaPackageElementID, "opencast_header.gif", `in`!!)
            Assert.fail()
        } catch (e: Exception) {
            // This is intended
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetBadId() {
        val badId = "../etc"
        try {
            repo[badId, mediaPackageElementID]
            Assert.fail()
        } catch (e: Exception) {
        }

    }

    @Test
    @Throws(Exception::class)
    fun testPutIntoCollection() {
        // Get the file back from the repository to check whether it's the same file that we put in.
        var fromRepo: InputStream? = null
        var headerIn: InputStream? = null
        try {
            fromRepo = repo.getFromCollection(collectionId, filename)
            val bytesFromRepo = IOUtils.toByteArray(fromRepo)
            headerIn = javaClass.classLoader.getResourceAsStream("opencast_header.gif")
            val bytesFromClasspath = IOUtils.toByteArray(headerIn!!)
            Assert.assertEquals(bytesFromClasspath.size, bytesFromRepo.size)
        } finally {
            IOUtils.closeQuietly(fromRepo)
            IOUtils.closeQuietly(headerIn)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCollectionSize() {
        Assert.assertEquals(1, repo.getCollectionSize(collectionId))
    }

    @Test
    @Throws(Exception::class)
    fun testCopy() {
        val newFileName = "newfile.gif"
        var bytesFromCollection: ByteArray? = null
        var `in`: InputStream? = null
        try {
            `in` = repo.getFromCollection(collectionId, filename)
            bytesFromCollection = IOUtils.toByteArray(`in`)
            IOUtils.closeQuietly(`in`)
            repo.copyTo(collectionId, filename, "copied-mediapackage", "copied-element", newFileName)
            `in` = repo["copied-mediapackage", "copied-element"]
            val bytesFromCopy = IOUtils.toByteArray(`in`)
            Assert.assertTrue(Arrays.equals(bytesFromCollection, bytesFromCopy))
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testMove() {
        val newFileName = "newfile.gif"
        var `in`: InputStream? = null
        try {
            `in` = repo.getFromCollection(collectionId, filename)
            val bytesFromCollection = IOUtils.toByteArray(`in`)
            IOUtils.closeQuietly(`in`)
            repo.moveTo(collectionId, filename, "moved-mediapackage", "moved-element", newFileName)
            `in` = repo["moved-mediapackage", "moved-element"]
            val bytesFromMove = IOUtils.toByteArray(`in`)
            Assert.assertTrue(Arrays.equals(bytesFromCollection, bytesFromMove))
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCleanupOldFilesFromCollectionNothingToDelete() {
        // Cleanup files older than 1 day, nothing should be deleted
        val result = repo.cleanupOldFilesFromCollection(collectionId, 1)
        Assert.assertTrue(result)
        var `in`: InputStream? = null
        try {
            `in` = repo.getFromCollection(collectionId, filename)
            Assert.assertNotNull(`in`)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCleanupOldFilesFromCollectionSomethingToDelete() {
        // Cleanup files older than 0 days, file should be deleted
        val result = repo.cleanupOldFilesFromCollection(collectionId, 0)
        Assert.assertTrue(result)
        try {
            Assert.assertTrue(repo.getFromCollection(collectionId, filename) == null)
        } catch (e: NotFoundException) {
            // This is intended
        }

    }

    @Test
    @Throws(Exception::class)
    fun testCleanupOldFilesFromNonExistentCollection() {
        val result = repo.cleanupOldFilesFromCollection("UNKNOWN", 0)
        Assert.assertFalse(result)
    }

}
