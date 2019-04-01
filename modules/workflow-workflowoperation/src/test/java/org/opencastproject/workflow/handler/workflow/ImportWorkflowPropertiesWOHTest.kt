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
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay
import org.easymock.EasyMock.verify

import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.attachment.AttachmentImpl
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class ImportWorkflowPropertiesWOHTest {

    private var tmpPropsFile: Path? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        tmpPropsFile = Files.createTempFile("workflow-properties", ".xml")
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        Files.deleteIfExists(tmpPropsFile!!)
    }

    @Test
    @Throws(Exception::class)
    fun testStartOp() {

        val woi = createMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        expect(woi.getConfiguration("source-flavor")).andStubReturn(FLAVOR)
        expect(woi.getConfiguration("keys")).andStubReturn("chapter, presenter_position, cover_marker_in_s")
        replay(woi)

        val att = AttachmentImpl()
        att.setURI(URI(WF_PROPS_ATT_URI))
        att.flavor = MediaPackageElementFlavor.parseFlavor(FLAVOR)

        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        mp.add(att)

        val wi = createMock<WorkflowInstance>(WorkflowInstance::class.java)
        expect(wi.currentOperation).andStubReturn(woi)
        expect(wi.mediaPackage).andStubReturn(mp)

        replay(wi)

        ImportWorkflowPropertiesWOHTest::class.java.getResourceAsStream("/workflow-properties.xml").use { `is` -> Files.copy(`is`, tmpPropsFile!!, StandardCopyOption.REPLACE_EXISTING) }

        val workspace = createNiceMock<Workspace>(Workspace::class.java)
        expect<File>(workspace.get(URI(WF_PROPS_ATT_URI))).andStubReturn(tmpPropsFile!!.toFile())
        replay(workspace)

        val woh = ImportWorkflowPropertiesWOH()
        woh.setWorkspace(workspace)

        val result = woh.start(wi, null)
        val properties = result.properties

        Assert.assertTrue(properties.containsKey("chapter"))
        Assert.assertEquals("true", properties["chapter"])

        Assert.assertTrue(properties.containsKey("presenter_position"))
        Assert.assertEquals("left", properties["presenter_position"])

        Assert.assertTrue(properties.containsKey("cover_marker_in_s"))
        Assert.assertEquals("30.674", properties["cover_marker_in_s"])

        verify(wi)
    }

    companion object {

        val WF_PROPS_ATT_URI = "http://opencast.org/attachments/workflow-properties.xml"
        val FLAVOR = "workflow/properties"
    }

}
