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

package org.opencastproject.workflow.handler.coverimage

import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMockRunner
import org.easymock.Mock
import org.easymock.TestSubject
import org.junit.Test
import org.junit.runner.RunWith

import java.io.File
import java.net.URL

/**
 * Unit tests for class [CoverImageWorkflowOperationHandler]
 */
@RunWith(EasyMockRunner::class)
class CoverImageWorkflowOperationHandlerTest {

    @TestSubject
    private val woh = CoverImageWorkflowOperationHandler()

    @Mock
    private val workspace: Workspace? = null

    @Test
    fun testAppendXml() {
        val xml = StringBuilder()
        woh.appendXml(xml, "elem-name", "This is the <body> of the element")
        assertEquals("<elem-name>This is the &lt;body&gt; of the element</elem-name>", xml.toString())
    }

    @Test
    @Throws(Exception::class)
    fun testGetPosterImageFileUriByString() {
        val fileUrlString = "file:/path/to/the/image.png"
        val httpUrlString = "http://foo.bar/image.png"
        val fileHttpUrlString = "file:/path/to/storage/foo_bar_image.png"
        val httpUri = URL(httpUrlString)
        val httpFile = File(fileHttpUrlString)

        // setup mock
        expect(workspace!!.get(httpUri.toURI())).andStubReturn(httpFile)
        replay(workspace)

        assertNull(woh.getPosterImageFileUrl(null))
        assertNull(woh.getPosterImageFileUrl(" "))
        assertNull(woh.getPosterImageFileUrl("{\$posterImageUrl}"))
        assertEquals(fileUrlString, woh.getPosterImageFileUrl(fileUrlString))
        assertEquals(fileHttpUrlString, woh.getPosterImageFileUrl(httpUrlString))
    }

}
