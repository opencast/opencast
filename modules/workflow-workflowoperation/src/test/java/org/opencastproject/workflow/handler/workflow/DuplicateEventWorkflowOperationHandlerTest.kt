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

import org.easymock.EasyMock.anyObject
import org.easymock.EasyMock.anyString
import org.easymock.EasyMock.capture
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.eq
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay
import org.easymock.EasyMock.reset
import org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler.COPY_NUMBER_PREFIX_PROPERTY
import org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler.MAX_NUMBER_PROPERTY
import org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler.NUMBER_PROPERTY
import org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler.PROPERTY_NAMESPACES_PROPERTY
import org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler.SOURCE_FLAVORS_PROPERTY
import org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler.SOURCE_TAGS_PROPERTY
import org.opencastproject.workflow.handler.workflow.DuplicateEventWorkflowOperationHandler.TARGET_TAGS_PROPERTY

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.Snapshot
import org.opencastproject.assetmanager.api.query.AQueryBuilder
import org.opencastproject.assetmanager.api.query.AResult
import org.opencastproject.assetmanager.api.query.ASelectQuery
import org.opencastproject.distribution.api.DistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.Track
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstance.OperationState
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Stream

import org.easymock.Capture
import org.easymock.CaptureType
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap

class DuplicateEventWorkflowOperationHandlerTest {

    private var operationHandler: DuplicateEventWorkflowOperationHandler? = null

    // local resources
    private var mp: MediaPackage? = null

    // mock workspace
    private var workspace: Workspace? = null

    // mock asset manager
    private var assetManager: AssetManager? = null

    // mock service registry
    private var serviceRegistry: ServiceRegistry? = null

    // mock distribution service
    private var distributionService: DistributionService? = null

    private var clonedMediaPackages: Capture<MediaPackage>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        operationHandler = DuplicateEventWorkflowOperationHandler()

        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()

        // test resources
        val uriMP = javaClass.getResource("/duplicate-event_mediapackage.xml").toURI()
        mp = builder.loadFromXml(uriMP.toURL().openStream())

        workspace = createNiceMock<Workspace>(Workspace::class.java)
        assetManager = createNiceMock<AssetManager>(AssetManager::class.java)
        distributionService = createNiceMock<DistributionService>(DistributionService::class.java)
        serviceRegistry = createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)

        operationHandler!!.setWorkspace(workspace)
        operationHandler!!.setAssetManager(assetManager)
        operationHandler!!.setDistributionService(distributionService)
        operationHandler!!.setServiceRegistry(serviceRegistry)
    }

    @Test
    @Throws(Exception::class)
    fun testSuccessfulCreate() {

        val numCopies = 2

        mockDependencies(numCopies)

        // operation configuration
        val configurations = HashMap<String, String>()
        configurations[SOURCE_FLAVORS_PROPERTY] = "*/*"
        configurations[SOURCE_TAGS_PROPERTY] = "archive"
        configurations[TARGET_TAGS_PROPERTY] = ""
        configurations[NUMBER_PROPERTY] = "" + numCopies
        configurations[MAX_NUMBER_PROPERTY] = "" + 10
        configurations[PROPERTY_NAMESPACES_PROPERTY] = "org.opencastproject.assetmanager.security"
        configurations[COPY_NUMBER_PREFIX_PROPERTY] = "copy"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)

        Assert.assertEquals(Action.CONTINUE, result.action)
        Assert.assertEquals(numCopies.toLong(), clonedMediaPackages!!.values.size.toLong())
        for (i in 1..numCopies) {
            val expectedTitle = (mp!!.title
                    + " (" + configurations[COPY_NUMBER_PREFIX_PROPERTY] + " " + i + ")")
            Assert.assertEquals(expectedTitle, clonedMediaPackages!!.values[i - 1].title)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testOverrideTags() {

        mockDependencies(1)

        // operation configuration
        val configurations = HashMap<String, String>()
        configurations[SOURCE_FLAVORS_PROPERTY] = "presenter/source"
        configurations[SOURCE_TAGS_PROPERTY] = "archive"
        configurations[TARGET_TAGS_PROPERTY] = "tag1,tag2"
        configurations[NUMBER_PROPERTY] = "" + 1
        configurations[MAX_NUMBER_PROPERTY] = "" + 10
        configurations[PROPERTY_NAMESPACES_PROPERTY] = "org.opencastproject.assetmanager.security"
        configurations[COPY_NUMBER_PREFIX_PROPERTY] = "copy"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        Assert.assertEquals(Action.CONTINUE, result.action)

        val track = clonedMediaPackages!!.value.getTracksByTag("tag1")[0]
        Assert.assertEquals("tag1", track.tags[0])
        Assert.assertEquals("tag2", track.tags[1])
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveAndAddTags() {
        mockDependencies(1)
        val configurations = HashMap<String, String>()
        configurations[SOURCE_FLAVORS_PROPERTY] = "*/*"
        configurations[SOURCE_TAGS_PROPERTY] = "part1"
        configurations[TARGET_TAGS_PROPERTY] = "-part1,+tag3"
        configurations[NUMBER_PROPERTY] = "" + 1
        configurations[MAX_NUMBER_PROPERTY] = "" + 10
        configurations[PROPERTY_NAMESPACES_PROPERTY] = "org.opencastproject.assetmanager.security"
        configurations[COPY_NUMBER_PREFIX_PROPERTY] = "copy"

        // run the operation handler
        val result = getWorkflowOperationResult(mp, configurations)
        Assert.assertEquals(Action.CONTINUE, result.action)

        val track = clonedMediaPackages!!.value.getTracksByTag("tag3")[0]
        val newTags = Arrays.asList(*track.tags)
        val originalTags = Arrays.asList(*mp!!.getTracksByTag("part1")[0].tags)
        Assert.assertEquals(originalTags.size.toLong(), newTags.size.toLong())
        Assert.assertTrue(newTags.contains("tag3"))
        Assert.assertFalse(newTags.contains("part1"))
    }

    @Throws(WorkflowOperationException::class)
    private fun getWorkflowOperationResult(mp: MediaPackage?, configurations: Map<String, String>): WorkflowOperationResult {
        // Add the mediapackage to a workflow instance
        val workflowInstance = WorkflowInstanceImpl()
        workflowInstance.id = 1
        workflowInstance.state = WorkflowState.RUNNING
        workflowInstance.mediaPackage = mp
        val operation = WorkflowOperationInstanceImpl("op", OperationState.RUNNING)
        operation.template = "create-event"
        operation.state = OperationState.RUNNING
        for (key in configurations.keys) {
            operation.setConfiguration(key, configurations[key])
        }

        val operationsList = ArrayList<WorkflowOperationInstance>()
        operationsList.add(operation)
        workflowInstance.operations = operationsList

        // Run the media package through the operation handler
        return operationHandler!!.start(workflowInstance, null)
    }

    @Throws(Exception::class)
    private fun mockDependencies(numberOfCopies: Int) {
        clonedMediaPackages = Capture.newInstance(CaptureType.ALL)
        reset(workspace, assetManager, distributionService)

        val uriDc = javaClass.getResource("/dublincore.xml").toURI()
        for (i in 0 until numberOfCopies) {
            expect<InputStream>(workspace!!.read(eq(URI.create("dublincore.xml")))).andReturn(FileInputStream(File(uriDc)))
                    .times(1)
        }
        expect(workspace!!.get(anyObject())).andReturn(File(javaClass.getResource("/av.mov").toURI())).anyTimes()
        expect(workspace!!.put(anyString(), anyString(), eq("dublincore.xml"), anyObject<InputStream>()))
                .andReturn(uriDc).times(numberOfCopies)
        replay(workspace!!)

        val qResult = createNiceMock<AResult>(AResult::class.java)
        expect<Stream<ARecord>>(qResult.records).andReturn(Stream.empty<ARecord>()).anyTimes()
        replay(qResult)
        val qSelect = createNiceMock<ASelectQuery>(ASelectQuery::class.java)
        expect(qSelect.where(anyObject<Predicate>())).andReturn(qSelect).anyTimes()
        expect(qSelect.run()).andReturn(qResult).anyTimes()
        replay(qSelect)
        val qBuilder = createNiceMock<AQueryBuilder>(AQueryBuilder::class.java)
        expect(qBuilder.select(*anyObject<Target>())).andReturn(qSelect).anyTimes()
        replay(qBuilder)
        expect(assetManager!!.createQuery()).andReturn(qBuilder).anyTimes()
        expect(assetManager!!.takeSnapshot(eq(AssetManager.DEFAULT_OWNER), capture(clonedMediaPackages)))
                .andReturn(createNiceMock(Snapshot::class.java)).times(numberOfCopies)
        replay(assetManager!!)

        val distributionJob = createNiceMock<Job>(Job::class.java)
        val internalPub = mp!!.getElementById("pub-int") as Publication
        val internalPubElements = ArrayList<MediaPackageElement>()
        Collections.addAll<Attachment>(internalPubElements, *internalPub.attachments)
        Collections.addAll<Catalog>(internalPubElements, *internalPub.catalogs)
        Collections.addAll(internalPubElements, *internalPub.tracks)
        expect<Status>(distributionJob.status).andReturn(Job.Status.FINISHED).anyTimes()
        for (e in internalPubElements) {
            expect(distributionJob.payload).andReturn(MediaPackageElementParser.getAsXml(e)).times(numberOfCopies)
        }
        replay(distributionJob)
        expect(distributionService!!.distribute(eq(InternalPublicationChannel.CHANNEL_ID), anyObject(), anyString()))
                .andReturn(distributionJob).anyTimes()
        replay(distributionService!!)
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(Exception::class)
    fun testCreateMoreThanMaximum() {

        val numCopies = 2
        val maxCopies = 1

        mockDependencies(numCopies)

        // operation configuration
        val configurations = HashMap<String, String>()
        configurations[SOURCE_FLAVORS_PROPERTY] = "*/*"
        configurations[SOURCE_TAGS_PROPERTY] = "archive"
        configurations[TARGET_TAGS_PROPERTY] = ""
        configurations[NUMBER_PROPERTY] = "" + numCopies
        configurations[MAX_NUMBER_PROPERTY] = "" + maxCopies
        configurations[PROPERTY_NAMESPACES_PROPERTY] = "org.opencastproject.assetmanager.security"
        configurations[COPY_NUMBER_PREFIX_PROPERTY] = "copy"

        // run the operation handler
        getWorkflowOperationResult(mp, configurations)
    }

}
