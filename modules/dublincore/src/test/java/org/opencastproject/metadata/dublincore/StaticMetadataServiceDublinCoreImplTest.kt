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


package org.opencastproject.metadata.dublincore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.opencastproject.metadata.dublincore.TestUtil.createDate

import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.metadata.api.MetadataValue
import org.opencastproject.metadata.api.MetadataValues
import org.opencastproject.metadata.api.StaticMetadata
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Test

import java.io.File
import java.io.InputStream
import java.net.URL

class StaticMetadataServiceDublinCoreImplTest {

    @Test
    @Throws(Exception::class)
    fun testExtractMetadata() {
        val mp = newMediaPackage("/manifest-simple.xml")
        val ms = newStaticMetadataService()
        val md = ms.getMetadata(mp)
        assertEquals("Land and Vegetation: Key players on the Climate Scene",
                md.titles.stream().filter { v -> v.language == MetadataValues.LANGUAGE_UNDEFINED }
                        .findFirst().map<String>(Function<MetadataValue<String>, String> { it.getValue() }).orElse(""))
        assertEquals(createDate(2007, 12, 5, 0, 0, 0), md.created.get())
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testDefectMetadata() {
        val mp = newMediaPackage("/manifest-simple-defect.xml")
        val ms = newStaticMetadataService()
        // should throw an IllegalArgumentException
        ms.getMetadata(mp)
    }

    @Throws(Exception::class)
    private fun newStaticMetadataService(): StaticMetadataServiceDublinCoreImpl {
        val ms = StaticMetadataServiceDublinCoreImpl()
        ms.setWorkspace(newWorkspace())
        return ms
    }

    @Throws(Exception::class)
    private fun newWorkspace(): Workspace {
        // mock workspace
        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        val dcFile = File(javaClass.getResource("/dublincore.xml").toURI())
        val dcFileDefect = File(javaClass.getResource("/dublincore-defect.xml").toURI())
        assertNotNull(dcFile)
        // set expectations
        EasyMock.expect(workspace.get(EasyMock.anyObject<URI>())).andAnswer { if (EasyMock.getCurrentArguments()[0].toString().contains("-defect")) dcFileDefect else dcFile }.anyTimes()
        // put into replay mode
        EasyMock.replay(workspace)
        return workspace
    }

    @Throws(Exception::class)
    private fun newMediaPackage(manifest: String): MediaPackage {
        val builderFactory = MediaPackageBuilderFactory.newInstance()
        val mediaPackageBuilder = builderFactory.newMediaPackageBuilder()
        val rootUrl = javaClass.getResource("/")
        mediaPackageBuilder!!.serializer = DefaultMediaPackageSerializerImpl(rootUrl)
        javaClass.getResourceAsStream(manifest).use { `is` -> return mediaPackageBuilder.loadFromXml(`is`) }
    }
}
