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

package org.opencastproject.workflow.handler.videoeditor

import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.selector.TrackSelector
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.smil.api.SmilException
import org.opencastproject.smil.api.SmilService
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.util.NotFoundException
import org.opencastproject.videoeditor.api.ProcessFailedException
import org.opencastproject.videoeditor.api.VideoEditorService
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workspace.api.Workspace

import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.xml.sax.SAXException

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Arrays
import java.util.HashMap

import javax.xml.bind.JAXBException

/**
 * Test class for [VideoEditorWorkflowOperationHandler]
 */
class VideoEditorWorkflowOperationHandlerTest {

    private var videoEditorWorkflowOperationHandler: VideoEditorWorkflowOperationHandler? = null
    private var smilService: SmilService? = null
    private var videoEditorServiceMock: VideoEditorService? = null
    private var workspaceMock: Workspace? = null

    private var mpURI: URI? = null
    private var mp: MediaPackage? = null
    private var mpSmilURI: URI? = null
    private var mpSmil: MediaPackage? = null

    @Before
    @Throws(MediaPackageException::class, IOException::class, NotFoundException::class, URISyntaxException::class, SmilException::class, JAXBException::class, SAXException::class)
    fun setUp() {

        val mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        mpURI = VideoEditorWorkflowOperationHandlerTest::class.java.getResource("/editor_mediapackage.xml").toURI()
        mp = mpBuilder.loadFromXml(mpURI!!.toURL().openStream())
        mpSmilURI = VideoEditorWorkflowOperationHandlerTest::class.java.getResource("/editor_smil_mediapackage.xml").toURI()
        mpSmil = mpBuilder.loadFromXml(mpSmilURI!!.toURL().openStream())
        videoEditorServiceMock = EasyMock.createNiceMock<VideoEditorService>(VideoEditorService::class.java)
        workspaceMock = EasyMock.createNiceMock<Workspace>(Workspace::class.java)

        smilService = SmilServiceMock.createSmilServiceMock(mpSmilURI)

        videoEditorWorkflowOperationHandler = VideoEditorWorkflowOperationHandler()
        videoEditorWorkflowOperationHandler!!.setJobBarrierPollingInterval(0)
        videoEditorWorkflowOperationHandler!!.setSmilService(smilService)
        videoEditorWorkflowOperationHandler!!.setVideoEditorService(videoEditorServiceMock)
        videoEditorWorkflowOperationHandler!!.setWorkspace(workspaceMock)
    }

    private fun getDefaultConfiguration(interactive: Boolean): MutableMap<String, String> {
        val configuration = HashMap<String, String>()
        configuration["source-flavors"] = "*/work"
        configuration["preview-flavors"] = "*/preview"
        configuration["skipped-flavors"] = "*/work"
        configuration["smil-flavors"] = "*/smil"
        configuration["target-smil-flavor"] = "episode/smil"
        configuration["target-flavor-subtype"] = "trimmed"
        configuration["interactive"] = java.lang.Boolean.toString(interactive)
        return configuration
    }

    private fun getWorkflowInstance(mp: MediaPackage?, configurations: Map<String, String>): WorkflowInstanceImpl {
        val workflowInstance = WorkflowInstanceImpl()
        workflowInstance.id = 1
        workflowInstance.state = WorkflowInstance.WorkflowState.RUNNING
        workflowInstance.mediaPackage = mp
        val operation = WorkflowOperationInstanceImpl("op",
                WorkflowOperationInstance.OperationState.RUNNING)
        operation.template = "editor"
        operation.state = WorkflowOperationInstance.OperationState.RUNNING
        for (key in configurations.keys) {
            operation.setConfiguration(key, configurations[key])
        }
        val operations = ArrayList<WorkflowOperationInstance>(1)
        operations.add(operation)
        workflowInstance.operations = operations
        return workflowInstance
    }

    @Test
    @Throws(WorkflowOperationException::class, IOException::class)
    fun testEditorOperationStart() {
        // uri for new preview track smil file
        EasyMock.expect(
                workspaceMock!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as InputStream)).andReturn(
                URI.create("http://localhost:8080/foo/presenter.smil"))

        // uri for new episode smil file
        val episodeSmilUri = "http://localhost:8080/foo/episode.smil"
        EasyMock.expect(
                workspaceMock!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as InputStream)).andReturn(
                URI.create(episodeSmilUri))
        EasyMock.replay(workspaceMock!!)
        val workflowInstance = getWorkflowInstance(mp, getDefaultConfiguration(true))

        val result = videoEditorWorkflowOperationHandler!!.start(workflowInstance, null)
        Assert.assertNotNull(
                "VideoEditor workflow operation returns null but should be an instantiated WorkflowOperationResult", result)
        EasyMock.verify(workspaceMock!!)

        val worflowOperationInstance = workflowInstance.currentOperation
        val smillFlavorsProperty = worflowOperationInstance!!.getConfiguration("smil-flavors")
        val previewFlavorsProperty = worflowOperationInstance.getConfiguration("preview-flavors")
        val smilFlavor = MediaPackageElementFlavor.parseFlavor(smillFlavorsProperty)
        val previewFlavor = MediaPackageElementFlavor.parseFlavor(previewFlavorsProperty)

        // each preview track (e.g. presenter/preview) should have an own smil catalog in media package
        val previewSmilCatalogs = result.mediaPackage.getCatalogs(
                MediaPackageElementFlavor("presenter", "smil"))
        Assert.assertTrue(previewSmilCatalogs != null && previewSmilCatalogs.size > 0)
        for (track in result.mediaPackage.tracks) {
            if (track.flavor.matches(previewFlavor)) {
                var smilCatalogFound = false
                val trackSmilFlavor = MediaPackageElementFlavor(track.flavor.type!!,
                        smilFlavor.subtype!!)
                for (previewSmilCatalog in previewSmilCatalogs) {
                    if (previewSmilCatalog.flavor.matches(trackSmilFlavor)) {
                        smilCatalogFound = true
                        break
                    }
                }
                Assert.assertTrue("Mediapackage doesn't contain a smil catalog with flavor $trackSmilFlavor",
                        smilCatalogFound)
            }
        }

        // an "target-smil-flavor catalog" schould be in media package
        val targetSmilFlavorProperty = worflowOperationInstance.getConfiguration("target-smil-flavor")
        val episodeSmilCatalogs = result.mediaPackage.getCatalogs(
                MediaPackageElementFlavor.parseFlavor(targetSmilFlavorProperty))
        Assert.assertTrue("Mediapackage should contain catalog with flavor $targetSmilFlavorProperty",
                episodeSmilCatalogs != null && episodeSmilCatalogs!!.size > 0)
        Assert.assertTrue("Target smil catalog URI does not match",
                episodeSmilCatalogs!![0].getURI().compareTo(URI.create(episodeSmilUri)) === 0)
    }

    @Test
    @Throws(WorkflowOperationException::class)
    fun testEditorOperationSkip() {
        val workflowInstance = getWorkflowInstance(mp, getDefaultConfiguration(true))
        val result = videoEditorWorkflowOperationHandler!!.skip(workflowInstance, null)
        Assert.assertNotNull(
                "VideoEditor workflow operation returns null but should be an instantiated WorkflowOperationResult", result)

        // mediapackage should contain new derived track with flavor given by "target-flavor-subtype" configuration
        val worflowOperationInstance = workflowInstance.currentOperation
        val targetFlavorSubtypeProperty = worflowOperationInstance!!.getConfiguration("target-flavor-subtype")
        val skippedFlavorsProperty = worflowOperationInstance.getConfiguration("skipped-flavors")

        val trackSelector = TrackSelector()
        trackSelector.addFlavor(skippedFlavorsProperty)
        val skippedTracks = trackSelector.select(result.mediaPackage, false)
        Assert.assertTrue("Mediapackage does not contain any tracks matching flavor $skippedFlavorsProperty",
                skippedTracks != null && !skippedTracks.isEmpty())

        for (skippedTrack in skippedTracks) {
            val derivedTrackFlavor = MediaPackageElementFlavor.flavor(skippedTrack.flavor
                    .type, targetFlavorSubtypeProperty)
            val derivedElements = result.mediaPackage.getDerived(skippedTrack, derivedTrackFlavor)
            Assert.assertTrue("Media package should contain track with flavor " + derivedTrackFlavor.toString(),
                    derivedElements != null && derivedElements.size > 0)
        }
    }

    @Test
    @Throws(WorkflowOperationException::class)
    fun testEditorOperationInteractiveSkip() {
        val workflowInstance = getWorkflowInstance(mp, getDefaultConfiguration(false))
        val result = videoEditorWorkflowOperationHandler!!.start(workflowInstance, null)
        Assert.assertNotNull(
                "VideoEditor workflow operation returns null but should be an instantiated WorkflowOperationResult", result)

        // mediapackage should contain new derived track with flavor given by "target-flavor-subtype" configuration
        val worflowOperationInstance = workflowInstance.currentOperation
        val targetFlavorSubtypeProperty = worflowOperationInstance!!.getConfiguration("target-flavor-subtype")
        val skippedFlavorsProperty = worflowOperationInstance.getConfiguration("skipped-flavors")

        val trackSelector = TrackSelector()
        trackSelector.addFlavor(skippedFlavorsProperty)
        val skippedTracks = trackSelector.select(result.mediaPackage, false)
        Assert.assertTrue("Mediapackage does not contain any tracks matching flavor $skippedFlavorsProperty",
                skippedTracks != null && !skippedTracks.isEmpty())

        for (skippedTrack in skippedTracks) {
            val derivedTrackFlavor = MediaPackageElementFlavor.flavor(skippedTrack.flavor
                    .type, targetFlavorSubtypeProperty)
            val derivedElements = result.mediaPackage.getDerived(skippedTrack, derivedTrackFlavor)
            Assert.assertTrue("Media package should contain track with flavor " + derivedTrackFlavor.toString(),
                    derivedElements != null && derivedElements.size > 0)
        }
    }

    @Test
    @Ignore
    @Throws(WorkflowOperationException::class)
    fun testEditorOperationSkipWithModifiedSkippedFlavorsAndTargetFlavorProperty() {
        val configuration = getDefaultConfiguration(true)
        configuration["skipped-flavors"] = "*/preview"
        configuration["target-flavor-subtype"] = "edited"
        val workflowInstance = getWorkflowInstance(mp, configuration)
        val result = videoEditorWorkflowOperationHandler!!.skip(workflowInstance, null)
        Assert.assertNotNull(
                "VideoEditor workflow operation returns null but should be an instantiated WorkflowOperationResult", result)

        // mediapackage should contain new derived track with flavor given by "target-flavor-subtype" configuration
        val worflowOperationInstance = workflowInstance.currentOperation
        val targetFlavorSubtypeProperty = worflowOperationInstance!!.getConfiguration("target-flavor-subtype")
        val skippedFlavorsProperty = worflowOperationInstance.getConfiguration("skipped-flavors")

        val trackSelector = TrackSelector()
        trackSelector.addFlavor(skippedFlavorsProperty)
        val skippedTracks = trackSelector.select(result.mediaPackage, false)
        Assert.assertTrue("Mediapackage does not contain any tracks matching flavor $skippedFlavorsProperty",
                skippedTracks != null && !skippedTracks.isEmpty())

        for (skippedTrack in skippedTracks) {
            val derivedTrackFlavor = MediaPackageElementFlavor.flavor(skippedTrack.flavor
                    .type, targetFlavorSubtypeProperty)
            val derivedElements = result.mediaPackage.getDerived(skippedTrack, derivedTrackFlavor)
            Assert.assertTrue("Media package should contain track with flavor " + derivedTrackFlavor.toString(),
                    derivedElements != null && derivedElements.size > 0)
            Assert.assertTrue("Mediapackage schould contain a derived track with flavor subtype $targetFlavorSubtypeProperty",
                    derivedElements[0].flavor.subtype == targetFlavorSubtypeProperty)
        }
    }

    @Test
    @Throws(WorkflowOperationException::class, URISyntaxException::class, NotFoundException::class, IOException::class, ProcessFailedException::class, ServiceRegistryException::class, MediaPackageException::class)
    fun testEditorResume() {
        // filled smil file
        val episodeSmilURI = VideoEditorWorkflowOperationHandlerTest::class.java.getResource("/editor_smil_filled.smil").toURI()
        val episodeSmilFile = File(episodeSmilURI)

        // setup mock services
        EasyMock.expect(workspaceMock!!.get(EasyMock.anyObject<Any>() as URI)).andReturn(episodeSmilFile)
        EasyMock.expect(
                workspaceMock!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as InputStream)).andReturn(episodeSmilURI)
        EasyMock.expect(
                workspaceMock!!.moveTo(EasyMock.anyObject<Any>() as URI, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String)).andReturn(
                URI.create("http://localhost:8080/foo/trimmed.mp4"))

        val job = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(job.payload).andReturn(MediaPackageElementParser.getAsXml(mpSmil!!.tracks[0])).anyTimes()
        EasyMock.expect<Status>(job.status).andReturn(Job.Status.FINISHED)

        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        videoEditorWorkflowOperationHandler!!.setServiceRegistry(serviceRegistry)
        EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job)

        EasyMock.expect(videoEditorServiceMock!!.processSmil(EasyMock.anyObject<Any>() as Smil)).andReturn(Arrays.asList(job))

        EasyMock.replay(workspaceMock, job, serviceRegistry, videoEditorServiceMock)

        val workflowInstance = getWorkflowInstance(mpSmil, getDefaultConfiguration(true))
        // run test
        val result = videoEditorWorkflowOperationHandler!!.resume(workflowInstance, null, null)
        Assert.assertNotNull(
                "VideoEditor workflow operation returns null but should be an instantiated WorkflowOperationResult", result)

        EasyMock.verify(workspaceMock, job, serviceRegistry, videoEditorServiceMock)

        // verify trimmed track derived from source track
        val worflowOperationInstance = workflowInstance.currentOperation
        val targetFlavorSubtypeProperty = worflowOperationInstance!!.getConfiguration("target-flavor-subtype")
        val sourceFlavorsProperty = worflowOperationInstance.getConfiguration("source-flavors")

        val trackSelector = TrackSelector()
        trackSelector.addFlavor(sourceFlavorsProperty)
        val sourceTracks = trackSelector.select(result.mediaPackage, false)
        Assert.assertTrue("Mediapackage does not contain any tracks matching flavor $sourceFlavorsProperty",
                sourceTracks != null && !sourceTracks.isEmpty())

        for (sourceTrack in sourceTracks) {
            val targetFlavor = MediaPackageElementFlavor.flavor(sourceTrack.flavor.type,
                    targetFlavorSubtypeProperty)

            val targetTracks = result.mediaPackage.getTracks(targetFlavor)
            Assert.assertTrue("Media package doesn't contain track with flavor " + targetFlavor.toString(),
                    targetTracks != null && targetTracks.size > 0)
        }
    }
}
