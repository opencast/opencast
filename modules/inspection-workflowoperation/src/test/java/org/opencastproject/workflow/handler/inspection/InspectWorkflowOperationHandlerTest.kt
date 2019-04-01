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

package org.opencastproject.workflow.handler.inspection

import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.Job.Status
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.EName
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.metadata.api.MediaPackageMetadata
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService
import org.opencastproject.metadata.dublincore.DublinCoreValue
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.ArrayList
import java.util.Date

class InspectWorkflowOperationHandlerTest {
    private var operationHandler: InspectWorkflowOperationHandler? = null

    // local resources
    private var uriMP: URI? = null
    private var uriMPUpdated: URI? = null
    private var mp: MediaPackage? = null
    private var mpUpdatedDC: MediaPackage? = null
    private var newTrack: Track? = null
    private var job: Job? = null

    // mock services and objects
    private var workspace: Workspace? = null
    private var inspectionService: MediaInspectionService? = null
    private var dcService: DublinCoreCatalogService? = null
    private var metadata: MediaPackageMetadata? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        // test resources
        uriMP = InspectWorkflowOperationHandler::class.java.getResource("/inspect_mediapackage.xml").toURI()
        uriMPUpdated = InspectWorkflowOperationHandler::class.java.getResource("/inspect_mediapackage_updated.xml").toURI()
        mp = builder.loadFromXml(uriMP!!.toURL().openStream())
        mpUpdatedDC = builder.loadFromXml(uriMPUpdated!!.toURL().openStream())
        newTrack = mpUpdatedDC!!.tracks[0]

        // set up service
        operationHandler = InspectWorkflowOperationHandler()
        operationHandler!!.setJobBarrierPollingInterval(0)

        // set up mock metadata and metadata service providing it
        metadata = EasyMock.createNiceMock<MediaPackageMetadata>(MediaPackageMetadata::class.java)
        EasyMock.expect(metadata!!.date).andReturn(DATE)
        EasyMock.expect(metadata!!.language).andReturn(LANGUAGE)
        EasyMock.expect(metadata!!.license).andReturn(LICENSE)
        EasyMock.expect(metadata!!.seriesIdentifier).andReturn(SERIES)
        EasyMock.expect(metadata!!.seriesTitle).andReturn(SERIES_TITLE)
        EasyMock.expect(metadata!!.title).andReturn(TITLE)
        EasyMock.replay(metadata!!)

        // set up mock dublin core and dcService providing it
        val dc = EasyMock.createStrictMock<DublinCoreCatalog>(DublinCoreCatalog::class.java)
        EasyMock.expect(dc.hasValue(DublinCore.PROPERTY_EXTENT)).andReturn(false)
        dc[EasyMock.anyObject<Any>() as EName] = EasyMock.anyObject<Any>() as DublinCoreValue
        EasyMock.expect(dc.hasValue(DublinCore.PROPERTY_CREATED)).andReturn(false)
        dc[EasyMock.anyObject<Any>() as EName] = EasyMock.anyObject<Any>() as DublinCoreValue
        dc.toXml(EasyMock.anyObject<Any>() as ByteArrayOutputStream, EasyMock.anyBoolean())
        // EasyMock.expect(dc.getIdentifier()).andReturn("123");
        EasyMock.replay(dc)

        dcService = EasyMock.createNiceMock<DublinCoreCatalogService>(DublinCoreCatalogService::class.java)
        EasyMock.expect(dcService!!.getMetadata(EasyMock.anyObject<Any>() as MediaPackage)).andReturn(
                metadata)
        EasyMock.expect(
                dcService!!.load(EasyMock.anyObject<Any>() as InputStream)).andReturn(dc)
        EasyMock.replay(dcService!!)
        operationHandler!!.setDublincoreService(dcService)

        // set up mock receipt and inspect service providing it
        job = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(job!!.payload).andReturn(MediaPackageElementParser.getAsXml(newTrack)).anyTimes()
        EasyMock.expect(job!!.id).andReturn(123)
        EasyMock.expect(job!!.status).andReturn(Status.FINISHED)
        EasyMock.expect(job!!.dateCreated).andReturn(Date())
        EasyMock.expect(job!!.dateStarted).andReturn(Date())
        EasyMock.replay(job!!)

        // set up mock service registry
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job).anyTimes()
        EasyMock.replay(serviceRegistry)
        operationHandler!!.setServiceRegistry(serviceRegistry)

        inspectionService = EasyMock.createNiceMock<MediaInspectionService>(MediaInspectionService::class.java)
        EasyMock.expect(inspectionService!!.enrich(EasyMock.anyObject<Any>() as Track, EasyMock.anyBoolean(),
                EasyMock.anyObject<Any>() as Map<String, String>)).andReturn(job)
        EasyMock.replay(inspectionService!!)
        operationHandler!!.setInspectionService(inspectionService)

        // set up mock workspace
        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        // workspace.delete((String) EasyMock.anyObject(), (String) EasyMock.anyObject());
        val newURI = URI(NEW_DC_URL)
        EasyMock.expect(
                workspace!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as InputStream)).andReturn(newURI)
        EasyMock.expect(workspace!!.getURI(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String)).andReturn(newURI)
        EasyMock.expect(workspace!!.get(EasyMock.anyObject<Any>() as URI)).andReturn(
                File(javaClass.getResource("/dublincore.xml").toURI()))
        EasyMock.replay(workspace!!)
        operationHandler!!.setWorkspace(workspace)
    }

    @Test
    @Throws(Exception::class)
    fun testInspectOperationTrackMetadata() {
        for (c in mp!!.catalogs) {
            mp!!.remove(c)
        }
        val result = getWorkflowOperationResult(mp)
        val trackNew = result.mediaPackage.tracks[0]

        // check track metadata
        Assert.assertNotNull(trackNew.checksum)
        Assert.assertNotNull(trackNew.mimeType)
        Assert.assertNotNull(trackNew.duration)
        Assert.assertNotNull(trackNew.streams)
    }

    @Test
    @Throws(Exception::class)
    fun testInspectOperationDCMetadata() {
        val result = getWorkflowOperationResult(mp)
        val cat = result.mediaPackage.catalogs[0]
        // dublincore check: also checked with strict mock calls
        Assert.assertEquals(NEW_DC_URL, cat.getURI().toString())
    }

    @Throws(WorkflowOperationException::class)
    private fun getWorkflowOperationResult(mp: MediaPackage?): WorkflowOperationResult {
        // Add the mediapackage to a workflow instance
        val workflowInstance = WorkflowInstanceImpl()
        workflowInstance.id = 1
        workflowInstance.state = WorkflowState.RUNNING
        workflowInstance.mediaPackage = mp
        val operation = WorkflowOperationInstanceImpl("op", OperationState.RUNNING)
        val operationsList = ArrayList<WorkflowOperationInstance>()
        operationsList.add(operation)
        workflowInstance.operations = operationsList

        // Run the media package through the operation handler, ensuring that metadata gets added
        return operationHandler!!.start(workflowInstance, null)
    }

    companion object {

        // constant metadata values
        private val DATE = Date()
        private val LANGUAGE = "language"
        private val LICENSE = "license"
        private val SERIES = "series"
        private val SERIES_TITLE = "series title"
        private val TITLE = "title"
        private val NEW_DC_URL = "http://www.url.org"
    }
}
