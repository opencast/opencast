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

package org.opencastproject.workflow.handler.sox

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.Job.Status
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilder
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl
import org.opencastproject.sox.api.SoxService
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl
import org.opencastproject.workflow.api.WorkflowOperationResult
import org.opencastproject.workflow.api.WorkflowOperationResult.Action
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.IOUtils
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.net.URI

class NormalizeAudioWorkflowOperationHandlerTest {

    private var operationHandler: NormalizeAudioWorkflowOperationHandler? = null
    private var mp: MediaPackage? = null
    private var uriMP: URI? = null
    private var instance: WorkflowInstance? = null
    private val operationInstance = WorkflowOperationInstanceImpl()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        uriMP = NormalizeAudioWorkflowOperationHandler::class.java.getResource("/sox_mediapackage.xml").toURI()
        mp = builder.loadFromXml(uriMP!!.toURL().openStream())

        val soxTrackUri = NormalizeAudioWorkflowOperationHandler::class.java.getResource("/sox-track.xml").toURI()
        val normalizedTrackUri = NormalizeAudioWorkflowOperationHandler::class.java.getResource("/normalized-track.xml").toURI()
        val soxEncodeUri = NormalizeAudioWorkflowOperationHandler::class.java.getResource("/sox-encode-track.xml").toURI()
        val soxMuxUri = NormalizeAudioWorkflowOperationHandler::class.java.getResource("/sox-mux-track.xml").toURI()

        val soxTrackXml = IOUtils.toString(soxTrackUri.toURL().openStream())
        val encodeTrackXml = IOUtils.toString(soxEncodeUri.toURL().openStream())
        val normalizedTrackXml = IOUtils.toString(normalizedTrackUri.toURL().openStream())
        val muxedTrackXml = IOUtils.toString(soxMuxUri.toURL().openStream())

        instance = EasyMock.createNiceMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(instance!!.mediaPackage).andReturn(mp).anyTimes()
        EasyMock.expect<WorkflowOperationInstance>(instance!!.currentOperation).andReturn(operationInstance).anyTimes()
        EasyMock.replay(instance!!)

        val org = DefaultOrganization()
        val anonymous = JaxbUser("anonymous", "test", org, JaxbRole(
                DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, org))
        val userDirectoryService = EasyMock.createMock<UserDirectoryService>(UserDirectoryService::class.java)
        EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject<Any>() as String)).andReturn(anonymous).anyTimes()
        EasyMock.replay(userDirectoryService)

        val organization = DefaultOrganization()
        val organizationDirectoryService = EasyMock.createMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)
        EasyMock.expect(organizationDirectoryService.getOrganization(EasyMock.anyObject<Any>() as String))
                .andReturn(organization).anyTimes()
        EasyMock.replay(organizationDirectoryService)

        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.user).andReturn(anonymous).anyTimes()
        EasyMock.expect(securityService.organization).andReturn(organization).anyTimes()
        EasyMock.replay(securityService)

        val incidentService = EasyMock.createNiceMock<IncidentService>(IncidentService::class.java)
        EasyMock.replay(incidentService)

        val serviceRegistry = ServiceRegistryInMemoryImpl(null!!, securityService, userDirectoryService,
                organizationDirectoryService, incidentService)

        var analyzeJob = serviceRegistry.createJob(SoxService.JOB_TYPE, "Analyze", null, soxTrackXml, false)
        analyzeJob.status = Status.FINISHED
        analyzeJob = serviceRegistry.updateJob(analyzeJob)

        var normalizeJob = serviceRegistry.createJob(SoxService.JOB_TYPE, "Normalize", null, normalizedTrackXml, false)
        normalizeJob.status = Status.FINISHED
        normalizeJob = serviceRegistry.updateJob(normalizeJob)

        var encodeJob = serviceRegistry.createJob(ComposerService.JOB_TYPE, "Encode", null!!, encodeTrackXml, false)
        encodeJob.status = Status.FINISHED
        encodeJob = serviceRegistry.updateJob(encodeJob)

        var muxJob = serviceRegistry.createJob(ComposerService.JOB_TYPE, "Mux", null!!, muxedTrackXml, false)
        muxJob.status = Status.FINISHED
        muxJob = serviceRegistry.updateJob(muxJob)

        val sox = EasyMock.createNiceMock<SoxService>(SoxService::class.java)
        EasyMock.expect(sox.analyze(EasyMock.anyObject<Any>() as Track)).andReturn(analyzeJob).anyTimes()
        EasyMock.expect(sox.normalize(EasyMock.anyObject<Any>() as Track, EasyMock.anyObject<Any>() as Float)).andReturn(normalizeJob)
                .anyTimes()
        EasyMock.replay(sox)

        val composer = EasyMock.createNiceMock<ComposerService>(ComposerService::class.java)
        EasyMock.expect(composer.encode(EasyMock.anyObject<Any>() as Track, EasyMock.anyObject<Any>() as String)).andReturn(encodeJob)
        EasyMock.expect(
                composer.mux(EasyMock.anyObject<Any>() as Track, EasyMock.anyObject<Any>() as Track, EasyMock.anyObject<Any>() as String))
                .andReturn(muxJob)
        EasyMock.replay(composer)

        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(
                workspace.moveTo(EasyMock.anyObject<Any>() as URI, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                        EasyMock.anyObject<Any>() as String)).andReturn(URI("fooVideo.flv"))
        EasyMock.replay(workspace)

        // set up the handler
        operationHandler = NormalizeAudioWorkflowOperationHandler()
        operationHandler!!.setJobBarrierPollingInterval(0)
        operationHandler!!.setComposerService(composer)
        operationHandler!!.setSoxService(sox)
        operationHandler!!.setWorkspace(workspace)
        operationHandler!!.setServiceRegistry(serviceRegistry)
    }

    @Test
    @Throws(Exception::class)
    fun testAudioVideo() {
        operationInstance.setConfiguration("source-tags", "")
        operationInstance.setConfiguration("source-flavor", "*/video-audio")
        operationInstance.setConfiguration("source-flavors", "")
        operationInstance.setConfiguration("target-flavor", "*/normalized")
        operationInstance.setConfiguration("target-tags", "norm")
        operationInstance.setConfiguration("force-transcode", "false")
        operationInstance.setConfiguration("target-decibel", "-30")

        val result = operationHandler!!.start(instance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)

        Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 4, result.mediaPackage
                .elements.size.toLong())
        var tracks = result.mediaPackage.getTracks(MediaPackageElementFlavor("presentation", "normalized"))
        Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.size.toLong())
        Assert.assertTrue(tracks[0].containsTag("norm"))
        var audioVideo = tracks[0] as TrackImpl
        Assert.assertEquals(-30.0, audioVideo.getAudio()!![0].rmsLevDb!!.toFloat().toDouble(), 0.001)

        tracks = result.mediaPackage.getTracks(MediaPackageElementFlavor("presentation", "video-audio"))
        Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.size.toLong())
        audioVideo = tracks[0] as TrackImpl
        Assert.assertNull(audioVideo.getAudio()!![0].rmsLevDb)
    }

    @Test
    @Throws(Exception::class)
    fun testAudioContainer() {
        operationInstance.setConfiguration("source-tags", "")
        operationInstance.setConfiguration("source-flavor", "*/container-audio")
        operationInstance.setConfiguration("source-flavors", "")
        operationInstance.setConfiguration("target-flavor", "*/normalized")
        operationInstance.setConfiguration("target-tags", "norm")
        operationInstance.setConfiguration("force-transcode", "true")
        operationInstance.setConfiguration("target-decibel", "-30")

        val result = operationHandler!!.start(instance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)

        Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 4, result.mediaPackage
                .elements.size.toLong())
        var tracks = result.mediaPackage.getTracks(MediaPackageElementFlavor("presentation", "normalized"))
        Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.size.toLong())
        Assert.assertTrue(tracks[0].containsTag("norm"))
        var audioVideo = tracks[0] as TrackImpl
        Assert.assertEquals(-30.0, audioVideo.getAudio()!![0].rmsLevDb!!.toFloat().toDouble(), 0.001)

        tracks = result.mediaPackage.getTracks(MediaPackageElementFlavor("presentation", "container-audio"))
        Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.size.toLong())
        audioVideo = tracks[0] as TrackImpl
        Assert.assertNull(audioVideo.getAudio()!![0].rmsLevDb)
    }

    @Test
    @Throws(Exception::class)
    fun testAudio() {
        operationInstance.setConfiguration("source-tags", "")
        operationInstance.setConfiguration("source-flavor", "*/audio")
        operationInstance.setConfiguration("source-flavors", "")
        operationInstance.setConfiguration("target-flavor", "*/normalized")
        operationInstance.setConfiguration("target-tags", "norm")
        operationInstance.setConfiguration("force-transcode", "true")
        operationInstance.setConfiguration("target-decibel", "-30")

        val result = operationHandler!!.start(instance, null)
        Assert.assertEquals(Action.CONTINUE, result.action)

        Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 4, result.mediaPackage
                .elements.size.toLong())
        var tracks = result.mediaPackage.getTracks(MediaPackageElementFlavor("presentation", "normalized"))
        Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.size.toLong())
        Assert.assertTrue(tracks[0].containsTag("norm"))
        var audioVideo = tracks[0] as TrackImpl
        Assert.assertEquals(-30.0, audioVideo.getAudio()!![0].rmsLevDb!!.toFloat().toDouble(), 0.001)

        tracks = result.mediaPackage.getTracks(MediaPackageElementFlavor("presentation", "audio"))
        Assert.assertEquals("Resulting mediapackage has the wrong number of tracks", 1, tracks.size.toLong())
        audioVideo = tracks[0] as TrackImpl
        Assert.assertNull(audioVideo.getAudio()!![0].rmsLevDb)
    }

}
