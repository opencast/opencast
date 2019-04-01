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
package org.opencastproject.workflow.handler.workflow

import org.easymock.EasyMock.createMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay
import org.easymock.EasyMock.verify
import org.junit.Assert.assertEquals

import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.attachment.AttachmentImpl
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.util.HashSet
import java.util.Properties

class ExportWorkflowPropertiesWOHTest {

    private var workspace: Workspace? = null
    private var uri: URI? = null

    @Rule
    var temporaryFolder = TemporaryFolder()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        workspace = EasyMock.createMock<Workspace>(Workspace::class.java)
        val `in` = EasyMock.newCapture<InputStream>()
        val uriCapture = EasyMock.newCapture<URI>()
        EasyMock.expect(workspace!!.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
                EasyMock.capture(`in`))).andAnswer {
            val file = temporaryFolder.newFile()
            FileUtils.copyInputStreamToFile(`in`.value, file)
            file.toURI()
        }.anyTimes()
        EasyMock.expect(workspace!!.get(EasyMock.capture(uriCapture))).andAnswer { File(uriCapture.value) }.anyTimes()
        EasyMock.replay(workspace!!)
        uri = ExportWorkflowPropertiesWOHTest::class.java.getResource("/workflow-properties.xml").toURI()
    }

    @Test
    @Throws(Exception::class)
    fun testExport() {
        val woi = createMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        expect(woi.getConfiguration("target-flavor")).andStubReturn(FLAVOR)
        expect(woi.getConfiguration("target-tags")).andStubReturn("archive")
        expect(woi.getConfiguration("keys")).andStubReturn("chapter,presenter_position")
        replay(woi)

        val att = AttachmentImpl()
        att.setURI(uri)
        att.flavor = MediaPackageElementFlavor.parseFlavor(FLAVOR)

        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        mp.add(att)

        val wi = createMock<WorkflowInstance>(WorkflowInstance::class.java)
        expect(wi.currentOperation).andStubReturn(woi)
        expect(wi.mediaPackage).andStubReturn(mp)
        val keys = HashSet<String>()
        keys.add("presenter_position")
        keys.add("cover_marker_in_s")
        expect(wi.configurationKeys).andStubReturn(keys)
        expect(wi.getConfiguration("presenter_position")).andStubReturn("right")
        expect(wi.getConfiguration("cover_marker_in_s")).andStubReturn("30.674")

        replay(wi)

        val woh = ExportWorkflowPropertiesWOH()
        woh.setWorkspace(workspace)
        val result = woh.start(wi, null)
        val attachments = result.mediaPackage.attachments
        Assert.assertTrue(attachments.size == 1)
        val attachment = attachments[0]
        assertEquals("processing/defaults", attachment.flavor.toString())
        assertEquals("archive", attachment.tags[0])
        Assert.assertNotNull(attachment.getURI())

        val file = workspace!!.get(attachment.getURI())
        val props = Properties()
        FileInputStream(file).use { `is` -> props.loadFromXML(`is`) }

        assertEquals("30.674", props["cover_marker_in_s"])
        assertEquals("right", props["presenter_position"])
        Assert.assertFalse(props.contains("chapter"))

        verify(wi)
    }

    companion object {

        val FLAVOR = "processing/defaults"
    }

}
