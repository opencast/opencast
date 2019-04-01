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

import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.util.ArrayList

/**
 * Test class for [TagWorkflowOperationHandler]
 */
class TagByDublinCoreTermWOHTest {

    private var operationHandler: TagByDublinCoreTermWOH? = null
    private var instance: WorkflowInstanceImpl? = null
    private var operation: WorkflowOperationInstanceImpl? = null
    private var mp: MediaPackage? = null
    private var workspace: Workspace? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        mp = builder.loadFromXml(this.javaClass.getResourceAsStream("/archive_mediapackage.xml"))
        mp!!.getCatalog("catalog-1").setURI(this.javaClass.getResource("/dublincore.xml").toURI())

        // set up the handler
        operationHandler = TagByDublinCoreTermWOH()

        // Initialize the workflow
        instance = WorkflowInstanceImpl()
        operation = WorkflowOperationInstanceImpl("test", OperationState.INSTANTIATED)
        val ops = ArrayList<WorkflowOperationInstance>()
        ops.add(operation)
        instance!!.operations = ops
        instance!!.mediaPackage = mp

        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect<InputStream>(workspace!!.read(EasyMock.anyObject<URI>()))
                .andAnswer { javaClass.getResourceAsStream("/dublincore.xml") }
        EasyMock.replay(workspace!!)
        operationHandler!!.setWorkspace(workspace)
    }

    @Test
    @Throws(Exception::class)
    fun testMatchPresentDCTerm() {
        operation!!.setConfiguration(TagByDublinCoreTermWOH.SOURCE_FLAVORS_PROPERTY, "dublincore/*")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DCTERM_PROPERTY, "publisher")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "University of Opencast")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.TARGET_TAGS_PROPERTY, "tag1,tag2")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.COPY_PROPERTY, "false")

        val result = operationHandler!!.start(instance, null)
        val resultingMediapackage = result.mediaPackage

        val catalog = resultingMediapackage.getCatalog("catalog-1")
        Assert.assertEquals("tag1", catalog.tags[0])
        Assert.assertEquals("tag2", catalog.tags[1])
    }

    @Test
    @Throws(Exception::class)
    fun testMatchDefaultDCTerm() {
        // Match == Default Value
        operation!!.setConfiguration(TagByDublinCoreTermWOH.SOURCE_FLAVORS_PROPERTY, "dublincore/*")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DCTERM_PROPERTY, "source")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DEFAULT_VALUE_PROPERTY, "Timbuktu")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.TARGET_TAGS_PROPERTY, "tag1,tag2")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.COPY_PROPERTY, "false")

        val result = operationHandler!!.start(instance, null)
        val resultingMediapackage = result.mediaPackage

        val catalog = resultingMediapackage.getCatalog("catalog-1")
        Assert.assertEquals(2, catalog.tags.size.toLong())
        Assert.assertEquals("tag1", catalog.tags[0])
        Assert.assertEquals("tag2", catalog.tags[1])
    }

    @Test
    @Throws(Exception::class)
    fun testMisMatchDefaultDCTerm() {
        // Match != Default Value
        operation!!.setConfiguration(TagByDublinCoreTermWOH.SOURCE_FLAVORS_PROPERTY, "dublincore/*")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DCTERM_PROPERTY, "source")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DEFAULT_VALUE_PROPERTY, "Cairo")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.TARGET_TAGS_PROPERTY, "tag1,tag2")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.COPY_PROPERTY, "false")

        val result = operationHandler!!.start(instance, null)
        val resultingMediapackage = result.mediaPackage

        val catalog = resultingMediapackage.getCatalog("catalog-1")
        Assert.assertEquals(1, catalog.tags.size.toLong())
        Assert.assertEquals("archive", catalog.tags[0])
    }

    @Test
    @Throws(Exception::class)
    fun testMissingNoDefaultDCTerm() {
        // No Default Value
        operation!!.setConfiguration(TagByDublinCoreTermWOH.SOURCE_FLAVORS_PROPERTY, "dublincore/*")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DCTERM_PROPERTY, "source")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.MATCH_VALUE_PROPERTY, "Timbuktu")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.TARGET_TAGS_PROPERTY, "tag1,tag2")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.COPY_PROPERTY, "false")

        val result = operationHandler!!.start(instance, null)
        val resultingMediapackage = result.mediaPackage

        val catalog = resultingMediapackage.getCatalog("catalog-1")
        Assert.assertEquals(1, catalog.tags.size.toLong())
        Assert.assertEquals("archive", catalog.tags[0])
    }

    @Test
    @Throws(Exception::class)
    fun testNoMatchConfiguredDCTerm() {
        // No Match or Default Value
        // without dcterm or default the match should always fail
        operation!!.setConfiguration(TagByDublinCoreTermWOH.SOURCE_FLAVORS_PROPERTY, "dublincore/*")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DCCATALOG_PROPERTY, "episode")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.DCTERM_PROPERTY, "source")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.TARGET_TAGS_PROPERTY, "tag1,tag2")
        operation!!.setConfiguration(TagByDublinCoreTermWOH.COPY_PROPERTY, "false")

        val result = operationHandler!!.start(instance, null)
        val resultingMediapackage = result.mediaPackage

        val catalog = resultingMediapackage.getCatalog("catalog-1")
        Assert.assertEquals(1, catalog.tags.size.toLong())
        Assert.assertEquals("archive", catalog.tags[0])
    }
}
