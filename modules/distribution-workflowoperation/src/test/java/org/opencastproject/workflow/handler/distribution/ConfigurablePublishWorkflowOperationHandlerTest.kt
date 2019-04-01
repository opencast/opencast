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
package org.opencastproject.workflow.handler.distribution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.DownloadDistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.Job.Status
import org.opencastproject.job.api.JobBarrier.Result
import org.opencastproject.job.api.JobContext
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.CatalogImpl
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.PublicationImpl
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.attachment.AttachmentImpl
import org.opencastproject.mediapackage.identifier.Id
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.SecurityService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.MimeType
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowOperationException
import org.opencastproject.workflow.api.WorkflowOperationInstance
import org.opencastproject.workflow.api.WorkflowOperationResult

import com.entwinemedia.fn.Fn2
import com.entwinemedia.fn.Stream

import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Before
import org.junit.Test

import java.net.URI
import java.net.URISyntaxException
import java.util.HashMap

class ConfigurablePublishWorkflowOperationHandlerTest {
    private var org: Organization? = null
    private val examplePlayer = "/engage/theodul/ui/core.html?id="

    @Before
    fun setUp() {
        val properties = HashMap<String, String>()
        properties[ConfigurablePublishWorkflowOperationHandler.PLAYER_PROPERTY] = examplePlayer
        org = EasyMock.createNiceMock<Organization>(Organization::class.java)
        EasyMock.expect(org!!.properties).andStubReturn(properties)
        EasyMock.replay(org!!)
    }

    @Test(expected = WorkflowOperationException::class)
    @Throws(WorkflowOperationException::class)
    fun testNoChannelIdThrowsException() {
        val mediapackage = EasyMock.createNiceMock<MediaPackage>(MediaPackage::class.java)
        val workflowOperationInstance = EasyMock.createNiceMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        val workflowInstance = EasyMock.createNiceMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(workflowInstance.mediaPackage).andStubReturn(mediapackage)
        EasyMock.expect(workflowInstance.currentOperation).andStubReturn(workflowOperationInstance)
        val jobContext = EasyMock.createNiceMock<JobContext>(JobContext::class.java)

        EasyMock.replay(jobContext, mediapackage, workflowInstance, workflowOperationInstance)

        val configurePublish = ConfigurablePublishWorkflowOperationHandler()
        configurePublish.start(workflowInstance, jobContext)
    }

    @Test
    @Throws(WorkflowOperationException::class, URISyntaxException::class, DistributionException::class, MediaPackageException::class)
    fun testNormal() {
        val channelId = "engage-player"

        val attachmentId = "attachment-id"
        val catalogId = "catalog-id"
        val trackId = "track-id"

        val attachment = AttachmentImpl()
        attachment.addTag("engage-download")
        attachment.identifier = attachmentId
        attachment.setURI(URI("http://api.com/attachment"))

        val catalog = CatalogImpl.newInstance()
        catalog.addTag("engage-download")
        catalog.identifier = catalogId
        catalog.setURI(URI("http://api.com/catalog"))

        val track = TrackImpl()
        track.addTag("engage-streaming")
        track.identifier = trackId
        track.setURI(URI("http://api.com/track"))

        val publicationtest = PublicationImpl(trackId, channelId, URI("http://api.com/publication"), MimeType.mimeType(trackId, trackId))

        val unrelatedTrack = TrackImpl()
        unrelatedTrack.addTag("unrelated")

        val capturePublication = Capture.newInstance<MediaPackageElement>()

        val mediapackageClone = EasyMock.createNiceMock<MediaPackage>(MediaPackage::class.java)
        EasyMock.expect(mediapackageClone.elements).andStubReturn(
                arrayOf(attachment, catalog, track, unrelatedTrack))
        EasyMock.expect(mediapackageClone.identifier).andStubReturn(IdImpl("mp-id-clone"))
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(mediapackageClone)

        val mediapackage = EasyMock.createNiceMock<MediaPackage>(MediaPackage::class.java)
        EasyMock.expect(mediapackage.elements).andStubReturn(
                arrayOf(attachment, catalog, track, unrelatedTrack))
        EasyMock.expect(mediapackage.clone()).andStubReturn(mediapackageClone)
        EasyMock.expect(mediapackage.identifier).andStubReturn(IdImpl("mp-id"))
        mediapackage.add(EasyMock.capture(capturePublication))
        mediapackage.add(publicationtest)
        EasyMock.expect(mediapackage.publications).andStubReturn(arrayOf(publicationtest))
        EasyMock.expectLastCall<Any>()
        EasyMock.replay(mediapackage)

        val op = EasyMock.createNiceMock<WorkflowOperationInstance>(WorkflowOperationInstance::class.java)
        EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.CHANNEL_ID_KEY)).andStubReturn(
                channelId)
        EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.MIME_TYPE)).andStubReturn(
                "text/html")
        EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.URL_PATTERN)).andStubReturn(
                "http://api.opencast.org/api/events/\${event_id}")
        EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.SOURCE_TAGS)).andStubReturn(
                "engage-download,engage-streaming")
        EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.CHECK_AVAILABILITY)).andStubReturn(
                "true")
        EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.STRATEGY)).andStubReturn(
                "retract")
        EasyMock.expect(op.getConfiguration(ConfigurablePublishWorkflowOperationHandler.MODE)).andStubReturn(
                "single")
        EasyMock.replay(op)

        val workflowInstance = EasyMock.createNiceMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(workflowInstance.mediaPackage).andStubReturn(mediapackage)
        EasyMock.expect(workflowInstance.currentOperation).andStubReturn(op)
        EasyMock.replay(workflowInstance)

        val jobContext = EasyMock.createNiceMock<JobContext>(JobContext::class.java)
        EasyMock.replay(jobContext)

        val attachmentJob = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(attachmentJob.payload).andReturn(MediaPackageElementParser.getAsXml(attachment))
        EasyMock.replay(attachmentJob)

        val catalogJob = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(catalogJob.payload).andReturn(MediaPackageElementParser.getAsXml(catalog))
        EasyMock.replay(catalogJob)

        val trackJob = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(trackJob.payload).andReturn(MediaPackageElementParser.getAsXml(track))
        EasyMock.replay(trackJob)

        val retractJob = EasyMock.createNiceMock<Job>(Job::class.java)
        EasyMock.expect(retractJob.payload).andReturn(MediaPackageElementParser.getAsXml(track))
        EasyMock.replay(retractJob)

        val distributionService = EasyMock.createNiceMock<DownloadDistributionService>(DownloadDistributionService::class.java)
        // Make sure that all of the elements are distributed.
        EasyMock.expect(distributionService.distribute(channelId, mediapackage, attachmentId, true)).andReturn(
                attachmentJob)
        EasyMock.expect(distributionService.distribute(channelId, mediapackage, catalogId, true)).andReturn(catalogJob)
        EasyMock.expect(distributionService.distribute(channelId, mediapackage, trackId, true)).andReturn(trackJob)
        EasyMock.expect(distributionService.retract(channelId, mediapackage, channelId)).andReturn(retractJob)
        EasyMock.replay(distributionService)

        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.organization).andStubReturn(org)
        EasyMock.replay(securityService)

        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        EasyMock.replay(serviceRegistry)

        // Override the waitForStatus method to not block the jobs
        val configurePublish = object : ConfigurablePublishWorkflowOperationHandler() {
            override fun waitForStatus(timeout: Long, vararg jobs: Job): Result {
                val map = Stream.mk(*jobs).foldl(HashMap(),
                        object : Fn2<HashMap<Job, Status>, Job, HashMap<Job, Status>>() {
                            override fun apply(a: HashMap<Job, Status>, b: Job): HashMap<Job, Status> {
                                a[b] = Status.FINISHED
                                return a
                            }
                        })
                return Result(map)
            }
        }

        configurePublish.setDownloadDistributionService(distributionService)
        configurePublish.setSecurityService(securityService)
        configurePublish.setServiceRegistry(serviceRegistry)

        val result = configurePublish.start(workflowInstance, jobContext)
        assertNotNull(result.mediaPackage)

        assertTrue("The publication element has not been added to the mediapackage.", capturePublication.hasCaptured())
        assertTrue("Some other type of element has been added to the mediapackage instead of the publication element.",
                capturePublication.value.elementType == MediaPackageElement.Type.Publication)
        val publication = capturePublication.value as Publication
        assertEquals(1, publication.attachments.size.toLong())
        assertNotEquals(attachment.identifier, publication.attachments[0].identifier)
        attachment.identifier = publication.attachments[0].identifier
        assertEquals(attachment, publication.attachments[0])

        assertEquals(1, publication.catalogs.size.toLong())
        assertNotEquals(catalog.identifier, publication.catalogs[0].identifier)
        catalog.identifier = publication.catalogs[0].identifier
        assertEquals(catalog, publication.catalogs[0])

        assertEquals(1, publication.tracks.size.toLong())
        assertNotEquals(track.identifier, publication.tracks[0].identifier)
        track.identifier = publication.tracks[0].identifier
        assertEquals(track, publication.tracks[0])
    }

    @Test
    @Throws(WorkflowOperationException::class, URISyntaxException::class)
    fun testTemplateReplacement() {
        val elementUri = URI("http://element.com/path/to/element/element.mp4")
        val mpId = "mp-id"
        val pubUUID = "test-uuid"
        val seriesId = "series-id"

        var mp = EasyMock.createNiceMock<MediaPackage>(MediaPackage::class.java)
        val id = IdImpl(mpId)
        EasyMock.expect(mp.identifier).andStubReturn(id)
        val element = EasyMock.createNiceMock<MediaPackageElement>(MediaPackageElement::class.java)
        EasyMock.expect(element.getURI()).andStubReturn(elementUri)
        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.organization).andStubReturn(org)
        EasyMock.replay(element, mp, securityService)

        // Test player path and mediapackage id
        var configurePublish = ConfigurablePublishWorkflowOperationHandler()
        configurePublish.setSecurityService(securityService)
        var result = configurePublish.populateUrlWithVariables("\${player_path}\${event_id}", mp, pubUUID)
        assertEquals(examplePlayer + "mp-id", result.toString())

        // Test without series
        configurePublish = ConfigurablePublishWorkflowOperationHandler()
        configurePublish.setSecurityService(securityService)
        result = configurePublish.populateUrlWithVariables("\${series_id}/\${event_id}", mp, pubUUID)
        assertEquals("/mp-id", result.toString())

        // Test with series
        mp = EasyMock.createNiceMock(MediaPackage::class.java)
        EasyMock.expect(mp.identifier).andStubReturn(id)
        EasyMock.expect(mp.series).andStubReturn(seriesId)
        EasyMock.replay(mp)

        configurePublish = ConfigurablePublishWorkflowOperationHandler()
        configurePublish.setSecurityService(securityService)
        result = configurePublish.populateUrlWithVariables("\${series_id}/\${event_id}", mp, pubUUID)
        assertEquals("series-id/mp-id", result.toString())

        // Test publication uuid
        configurePublish = ConfigurablePublishWorkflowOperationHandler()
        configurePublish.setSecurityService(securityService)
        result = configurePublish.populateUrlWithVariables("\${publication_id}", mp, pubUUID)
        assertEquals(pubUUID, result.toString())
    }
}
