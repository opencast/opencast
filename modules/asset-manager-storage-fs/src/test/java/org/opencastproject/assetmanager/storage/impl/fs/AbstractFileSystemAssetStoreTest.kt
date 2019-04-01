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
package org.opencastproject.assetmanager.storage.impl.fs

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

import org.opencastproject.assetmanager.impl.VersionImpl
import org.opencastproject.assetmanager.impl.storage.DeletionSelector
import org.opencastproject.assetmanager.impl.storage.Source
import org.opencastproject.assetmanager.impl.storage.StoragePath
import org.opencastproject.util.IoSupport
import org.opencastproject.util.PathSupport
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.data.Opt

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.easymock.EasyMock
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class AbstractFileSystemAssetStoreTest {

    private var tmpRoot: File? = null

    private var repo: AbstractFileSystemAssetStore? = null

    private var sampleElemDir: File? = null

    @Rule
    var tmpFolder = TemporaryFolder()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val asset = IoSupport.classPathResourceAsFile("/$FILE_NAME").get()
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject<URI>())).andReturn(asset)
        EasyMock.expect(workspace.get(EasyMock.anyObject<URI>(), EasyMock.anyBoolean())).andAnswer {
            val tmp = tmpFolder.newFile()
            FileUtils.copyFile(asset, tmp)
            tmp
        }.anyTimes()
        EasyMock.replay(workspace)

        tmpRoot = tmpFolder.newFolder()

        repo = object : AbstractFileSystemAssetStore() {
            protected override val workspace: Workspace
                get() = workspace

            protected override val rootDirectory: String
                get() = tmpRoot!!.absolutePath
        }

        sampleElemDir = File(
                PathSupport.concat(arrayOf(tmpRoot!!.toString(), ORG_ID, MP_ID, VERSION_2.toString())))
        FileUtils.forceMkdir(sampleElemDir!!)

        FileUtils.copyFile(asset, File(sampleElemDir, MP_ELEM_ID + XML_EXTENSTION))
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.forceDelete(tmpRoot!!)
    }

    @Test
    @Throws(Exception::class)
    fun testPut() {
        val storagePath = StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID)
        repo!!.put(storagePath, Source.mk(javaClass.classLoader.getResource(FILE_NAME)!!.toURI()))

        val file = File(PathSupport.concat(arrayOf(tmpRoot!!.toString(), ORG_ID, MP_ID, VERSION_1.toString())))
        assertTrue("$file should be a directory", file.isDirectory)
        assertTrue(file.listFiles()!!.size == 1)

        var original: InputStream? = null
        var fileInput: FileInputStream? = null
        try {
            fileInput = FileInputStream(file.listFiles()!![0])
            original = javaClass.classLoader.getResourceAsStream(FILE_NAME)
            val bytesFromRepo = IOUtils.toByteArray(fileInput)
            val bytesFromClasspath = IOUtils.toByteArray(original!!)
            assertArrayEquals(bytesFromClasspath, bytesFromRepo)
        } finally {
            IOUtils.closeQuietly(original)
            IOUtils.closeQuietly(fileInput)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCopy() {
        val from = StoragePath(ORG_ID, MP_ID, VERSION_2, MP_ELEM_ID)
        val to = StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID)
        assertTrue(repo!!.copy(from, to))

        val srcFile = File(PathSupport.concat(arrayOf(tmpRoot!!.toString(), ORG_ID, MP_ID, VERSION_2.toString(), MP_ELEM_ID + XML_EXTENSTION)))
        val copyFile = File(PathSupport.concat(arrayOf(tmpRoot!!.toString(), ORG_ID, MP_ID, VERSION_1.toString(), MP_ELEM_ID + XML_EXTENSTION)))
        assertTrue(srcFile.exists())
        assertTrue(copyFile.exists())

        var srcIn: FileInputStream? = null
        var copyIn: FileInputStream? = null
        try {
            srcIn = FileInputStream(srcFile)
            copyIn = FileInputStream(copyFile)
            val bytesOriginal = IOUtils.toByteArray(srcIn)
            val bytesCopy = IOUtils.toByteArray(copyIn)
            Assert.assertEquals(bytesCopy.size.toLong(), bytesOriginal.size.toLong())
        } finally {
            IOUtils.closeQuietly(srcIn)
            IOUtils.closeQuietly(copyIn)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCopyBad() {
        val from = StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID)
        val to = StoragePath(ORG_ID, MP_ID, VERSION_2, MP_ELEM_ID)
        assertFalse(repo!!.copy(from, to))
    }

    @Test
    @Throws(Exception::class)
    fun testGet() {
        val storagePath = StoragePath(ORG_ID, MP_ID, VERSION_2, MP_ELEM_ID)
        val option = repo!!.get(storagePath)
        assertTrue(option.isSome)

        var original: InputStream? = null
        val repo = option.get()
        try {
            original = javaClass.classLoader.getResourceAsStream(FILE_NAME)
            val bytesFromRepo = IOUtils.toByteArray(repo)
            val bytesFromClasspath = IOUtils.toByteArray(original!!)
            assertEquals(bytesFromClasspath.size.toLong(), bytesFromRepo.size.toLong())
        } finally {
            IOUtils.closeQuietly(original)
            IOUtils.closeQuietly(repo)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetBad() {
        val storagePath = StoragePath(ORG_ID, MP_ID, VERSION_1, MP_ELEM_ID)
        val option = repo!!.get(storagePath)
        assertFalse(option.isSome)
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteWithVersion() {
        val versionSelector = DeletionSelector.delete(ORG_ID, MP_ID, VERSION_2)
        assertTrue(repo!!.delete(versionSelector))
        assertFalse(sampleElemDir!!.exists())

        var file = File(PathSupport.concat(arrayOf(tmpRoot!!.toString(), ORG_ID, MP_ID, VERSION_2.toString())))
        assertFalse(file.exists())

        file = File(PathSupport.concat(arrayOf(tmpRoot!!.toString(), ORG_ID, MP_ID)))
        assertFalse(file.exists())

        file = File(PathSupport.concat(arrayOf(tmpRoot!!.toString(), ORG_ID)))
        assertTrue(file.exists())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteNoneVersion() {
        val noneVersionSelector = DeletionSelector.deleteAll(ORG_ID, MP_ID)
        assertTrue(repo!!.delete(noneVersionSelector))
        assertFalse(sampleElemDir!!.exists())

        val file = File(PathSupport.concat(arrayOf(tmpRoot!!.toString(), ORG_ID, MP_ID)))
        assertFalse(file.exists())
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteWrongVersion() {
        val versionSelector = DeletionSelector.delete(ORG_ID, MP_ID, VERSION_1)
        repo!!.delete(versionSelector)
        assertTrue(sampleElemDir!!.exists())
    }

    companion object {
        private val XML_EXTENSTION = ".xml"

        private val TEST_ROOT_DIR_NAME = "test-archive"

        private val ORG_ID = "sampleOrgId"

        private val MP_ID = "sampleMediaPackageId"

        private val MP_ELEM_ID = "sampleMediaPackageElementId"

        private val FILE_NAME = "dublincore.xml"

        private val VERSION_1 = VersionImpl(1)

        private val VERSION_2 = VersionImpl(2)
    }
}
