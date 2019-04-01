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

package org.opencastproject.ingest.impl

import org.opencastproject.capture.CaptureParameters
import org.opencastproject.inspection.api.MediaInspectionService
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.Job.Status
import org.opencastproject.job.api.JobImpl
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.track.AudioStreamImpl
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.mediapackage.track.VideoStreamImpl
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.scheduler.api.SchedulerService
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AclScope
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.TrustedHttpClient
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.series.api.SeriesService
import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl
import org.opencastproject.util.DateTimeSupport
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.XmlUtil
import org.opencastproject.util.data.Either
import org.opencastproject.util.data.Tuple
import org.opencastproject.workflow.api.WorkflowDefinition
import org.opencastproject.workflow.api.WorkflowDefinitionImpl
import org.opencastproject.workflow.api.WorkflowInstance
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState
import org.opencastproject.workflow.api.WorkflowInstanceImpl
import org.opencastproject.workflow.api.WorkflowService
import org.opencastproject.workingfilerepository.api.WorkingFileRepository
import org.opencastproject.workingfilerepository.impl.WorkingFileRepositoryImpl

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.reflect.FieldUtils
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.methods.HttpGet
import org.easymock.Capture
import org.easymock.EasyMock
import org.easymock.IAnswer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.w3c.dom.Document
import org.xml.sax.InputSource

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.Date
import java.util.Dictionary
import java.util.HashMap
import java.util.Hashtable

class IngestServiceImplTest {
    private var service: IngestServiceImpl? = null
    private var dublinCoreService: DublinCoreCatalogService? = null
    private var seriesService: SeriesService? = null
    private var workflowService: WorkflowService? = null
    private var workflowInstance: WorkflowInstance? = null
    private var wfr: WorkingFileRepository? = null
    private var serviceRegistry: ServiceRegistryInMemoryImpl? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {

        FileUtils.forceMkdir(ingestTempDir!!)

        // set up service and mock workspace
        wfr = EasyMock.createNiceMock<WorkingFileRepository>(WorkingFileRepository::class.java)
        EasyMock.expect(wfr!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlTrack)
        EasyMock.expect(wfr!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlCatalog)
        EasyMock.expect(wfr!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlAttachment)
        EasyMock.expect(wfr!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlTrack1)
        EasyMock.expect(wfr!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlTrack2)
        EasyMock.expect(wfr!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlCatalog1)
        EasyMock.expect(wfr!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlCatalog2)
        EasyMock.expect(wfr!!.put(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlCatalog)

        EasyMock.expect(wfr!!.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlTrack1)
        EasyMock.expect(wfr!!.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlTrack2)
        EasyMock.expect(wfr!!.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlCatalog1)
        EasyMock.expect(wfr!!.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlCatalog2)
        EasyMock.expect(wfr!!.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlCatalog)

        EasyMock.expect(wfr!!.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlPackage)

        EasyMock.expect(wfr!!.putInCollection(EasyMock.anyObject<Any>() as String, EasyMock.anyObject<Any>() as String,
                EasyMock.anyObject<Any>() as InputStream)).andReturn(urlPackageOld)

        workflowInstance = EasyMock.createNiceMock<WorkflowInstance>(WorkflowInstance::class.java)
        EasyMock.expect(workflowInstance!!.id).andReturn(workflowInstanceID)
        EasyMock.expect(workflowInstance!!.state).andReturn(WorkflowState.STOPPED)

        val mp = EasyMock.newCapture<MediaPackage>()
        workflowService = EasyMock.createNiceMock<WorkflowService>(WorkflowService::class.java)
        EasyMock.expect(workflowService!!.start(EasyMock.anyObject<Any>() as WorkflowDefinition, EasyMock.capture(mp),
                EasyMock.anyObject<Any>() as Map<*, *>)).andReturn(workflowInstance)
        EasyMock.expect(workflowInstance!!.mediaPackage).andAnswer { mp.value }.anyTimes()
        EasyMock.expect(workflowService!!.start(EasyMock.anyObject<Any>() as WorkflowDefinition,
                EasyMock.anyObject<Any>() as MediaPackage, EasyMock.anyObject<Any>() as Map<*, *>)).andReturn(workflowInstance)
        EasyMock.expect(
                workflowService!!.start(EasyMock.anyObject<Any>() as WorkflowDefinition, EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(workflowInstance)
        EasyMock.expect(workflowService!!.getWorkflowDefinitionById(EasyMock.anyObject<Any>() as String))
                .andReturn(WorkflowDefinitionImpl())
        EasyMock.expect(workflowService!!.getWorkflowById(EasyMock.anyLong())).andReturn(workflowInstance)

        val schedulerService = EasyMock.createNiceMock<SchedulerService>(SchedulerService::class.java)

        val properties = HashMap<String, String>()
        properties[CaptureParameters.INGEST_WORKFLOW_DEFINITION] = "sample"
        properties["agent-name"] = "matterhorn-agent"
        EasyMock.expect(schedulerService.getCaptureAgentConfiguration(EasyMock.anyString())).andReturn(properties)
                .anyTimes()
        EasyMock.expect(schedulerService.getDublinCore(EasyMock.anyString()))
                .andReturn(DublinCores.read(urlCatalog1!!.toURL().openStream())).anyTimes()
        val schedulerMediaPackage = MediaPackageParser
                .getFromXml(IOUtils.toString(javaClass.getResourceAsStream("/source-manifest.xml"), "UTF-8"))
        EasyMock.expect(schedulerService.getMediaPackage(EasyMock.anyString())).andReturn(schedulerMediaPackage).anyTimes()

        EasyMock.replay(wfr, workflowInstance, workflowService, schedulerService)

        val anonymous = JaxbUser("anonymous", "test", DefaultOrganization(),
                JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, DefaultOrganization(), "test"))
        val userDirectoryService = EasyMock.createMock<UserDirectoryService>(UserDirectoryService::class.java)
        EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject<Any>() as String)).andReturn(anonymous).anyTimes()
        EasyMock.replay(userDirectoryService)

        val organization = DefaultOrganization()
        val organizationDirectoryService = EasyMock.createMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)
        EasyMock.expect(organizationDirectoryService.getOrganization(EasyMock.anyObject<Any>() as String)).andReturn(organization)
                .anyTimes()
        EasyMock.replay(organizationDirectoryService)

        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.user).andReturn(anonymous).anyTimes()
        EasyMock.expect(securityService.organization).andReturn(organization).anyTimes()
        EasyMock.replay(securityService)

        val entity = EasyMock.createMock<HttpEntity>(HttpEntity::class.java)
        val `is` = javaClass.getResourceAsStream("/av.mov")
        val movie = IOUtils.toByteArray(`is`)
        IOUtils.closeQuietly(`is`)
        EasyMock.expect(entity.content).andReturn(ByteArrayInputStream(movie)).anyTimes()
        EasyMock.replay(entity)

        val statusLine = EasyMock.createMock<StatusLine>(StatusLine::class.java)
        EasyMock.expect(statusLine.statusCode).andReturn(200).anyTimes()
        EasyMock.replay(statusLine)

        val contentDispositionHeader = EasyMock.createMock<Header>(Header::class.java)
        EasyMock.expect(contentDispositionHeader.value).andReturn("attachment; filename=fname.mp4").anyTimes()
        EasyMock.replay(contentDispositionHeader)

        val httpResponse = EasyMock.createMock<HttpResponse>(HttpResponse::class.java)
        EasyMock.expect(httpResponse.statusLine).andReturn(statusLine).anyTimes()
        EasyMock.expect(httpResponse.getFirstHeader("Content-Disposition")).andReturn(contentDispositionHeader).anyTimes()
        EasyMock.expect(httpResponse.entity).andReturn(entity).anyTimes()
        EasyMock.replay(httpResponse)

        val httpClient = EasyMock.createNiceMock<TrustedHttpClient>(TrustedHttpClient::class.java)
        EasyMock.expect(httpClient.execute(EasyMock.anyObject<Any>() as HttpGet)).andReturn(httpResponse).anyTimes()
        EasyMock.replay(httpClient)

        val authorizationService = EasyMock.createNiceMock<AuthorizationService>(AuthorizationService::class.java)
        EasyMock.expect(authorizationService.getActiveAcl(EasyMock.anyObject<Any>() as MediaPackage))
                .andReturn(Tuple.tuple(AccessControlList(), AclScope.Series)).anyTimes()
        EasyMock.replay(authorizationService)

        val mediaInspectionService = EasyMock.createNiceMock<MediaInspectionService>(MediaInspectionService::class.java)
        EasyMock.expect(mediaInspectionService.enrich(EasyMock.anyObject(MediaPackageElement::class.java), EasyMock.anyBoolean()))
                .andAnswer(object : IAnswer<Job> {
                    private var i = 0

                    @Throws(Throwable::class)
                    override fun answer(): Job {
                        val element = EasyMock.getCurrentArguments()[0] as TrackImpl
                        element.duration = 20000L
                        if (i % 2 == 0) {
                            element.addStream(VideoStreamImpl())
                        } else {
                            element.addStream(AudioStreamImpl())
                        }
                        i++
                        val succeededJob = JobImpl()
                        succeededJob.status = Status.FINISHED
                        succeededJob.payload = MediaPackageElementParser.getAsXml(element)
                        return succeededJob
                    }
                }).anyTimes()
        EasyMock.replay(mediaInspectionService)

        class MockedIngestServicve : IngestServiceImpl() {
            override fun createStandaloneHttpClient(user: String, password: String): TrustedHttpClient {
                return httpClient
            }
        }

        service = MockedIngestServicve()
        service!!.setHttpClient(httpClient)
        service!!.setAuthorizationService(authorizationService)
        service!!.setWorkingFileRepository(wfr)
        service!!.setWorkflowService(workflowService)
        service!!.securityService = securityService
        service!!.setSchedulerService(schedulerService)
        service!!.setMediaInspectionService(mediaInspectionService)
        serviceRegistry = ServiceRegistryInMemoryImpl(service!!, securityService, userDirectoryService,
                organizationDirectoryService, EasyMock.createNiceMock(IncidentService::class.java))
        serviceRegistry!!.registerService(service!!)
        service!!.serviceRegistry = serviceRegistry
        service!!.defaultWorkflowDefinionId = "sample"
        serviceRegistry!!.registerService(service!!)
    }

    @After
    fun tearDown() {
        FileUtils.deleteQuietly(ingestTempDir)
    }

    @Test
    @Throws(Exception::class)
    fun testThinClient() {
        var mediaPackage: MediaPackage? = null

        mediaPackage = service!!.createMediaPackage()
        mediaPackage = service!!.addTrack(urlTrack!!, MediaPackageElements.PRESENTATION_SOURCE, mediaPackage)
        mediaPackage = service!!.addCatalog(urlCatalog1!!, MediaPackageElements.EPISODE, mediaPackage)
        mediaPackage = service!!.addAttachment(urlAttachment!!, MediaPackageElements.MEDIAPACKAGE_COVER_FLAVOR, mediaPackage)
        val instance = service!!.ingest(mediaPackage)
        Assert.assertEquals(1, mediaPackage.tracks.size.toLong())
        Assert.assertEquals(0, mediaPackage.catalogs.size.toLong())
        Assert.assertEquals(1, mediaPackage.attachments.size.toLong())
        Assert.assertEquals(workflowInstanceID, instance.id)
    }

    @Test
    @Throws(Exception::class)
    fun testThickClient() {

        FileUtils.copyURLToFile(urlPackage!!.toURL(), packageFile!!)

        var packageStream: InputStream? = null
        try {
            packageStream = urlPackage!!.toURL().openStream()
            val instance = service!!.addZippedMediaPackage(packageStream!!)

            // Assert.assertEquals(2, mediaPackage.getTracks().length);
            // Assert.assertEquals(3, mediaPackage.getCatalogs().length);
            Assert.assertEquals(workflowInstanceID, instance.id)
        } catch (e: IOException) {
            Assert.fail(e.message)
        } finally {
            IOUtils.closeQuietly(packageStream)
        }

    }

    @Test
    @Throws(Exception::class)
    fun testThickClientOldMP() {

        FileUtils.copyURLToFile(urlPackageOld!!.toURL(), packageFile!!)

        var packageStream: InputStream? = null
        try {
            packageStream = urlPackageOld!!.toURL().openStream()
            val instance = service!!.addZippedMediaPackage(packageStream!!)

            // Assert.assertEquals(2, mediaPackage.getTracks().length);
            // Assert.assertEquals(3, mediaPackage.getCatalogs().length);
            Assert.assertEquals(workflowInstanceID, instance.id)
        } catch (e: IOException) {
            Assert.fail(e.message)
        } finally {
            IOUtils.closeQuietly(packageStream)
        }

    }

    @Test
    @Throws(Exception::class)
    fun testContentDisposition() {
        var mediaPackage: MediaPackage? = null

        mediaPackage = service!!.createMediaPackage()
        try {
            mediaPackage = service!!.addTrack(URI.create("http://www.test.com/testfile"), null!!, mediaPackage)
        } catch (e: Exception) {
            Assert.fail("Unable to read content dispostion filename!")
        }

        try {
            mediaPackage = service!!.addTrack(urlTrackNoFilename!!, null!!, mediaPackage!!)
            Assert.fail("Allowed adding content without filename!")
        } catch (e: Exception) {
            Assert.assertNotNull(e)
        }

    }

    @Test
    @Throws(Exception::class)
    fun testSmilCreation() {
        service!!.setWorkingFileRepository(object : WorkingFileRepositoryImpl() {
            @Throws(IOException::class)
            override fun put(mediaPackageID: String, mediaPackageElementID: String, filename: String, `in`: InputStream): URI {
                val file = File(FileUtils.getTempDirectory(), mediaPackageElementID)
                file.deleteOnExit()
                FileUtils.write(file, IOUtils.toString(`in`), "UTF-8")
                return file.toURI()
            }

            @Throws(NotFoundException::class, IOException::class)
            override fun get(mediaPackageID: String, mediaPackageElementID: String): InputStream {
                val file = File(FileUtils.getTempDirectory(), mediaPackageElementID)
                return FileInputStream(file)
            }
        })

        val presenterUri = URI.create("http://localhost:8080/presenter.mp4")
        val presenterUri2 = URI.create("http://localhost:8080/presenter2.mp4")
        val presentationUri = URI.create("http://localhost:8080/presentation.mp4")

        var mediaPackage = service!!.createMediaPackage()
        var catalogs = mediaPackage.getCatalogs(MediaPackageElements.SMIL)
        Assert.assertEquals(0, catalogs.size.toLong())

        mediaPackage = service!!.addPartialTrack(presenterUri, MediaPackageElements.PRESENTER_SOURCE_PARTIAL, 60000L,
                mediaPackage)
        mediaPackage = service!!.addPartialTrack(presenterUri2, MediaPackageElements.PRESENTER_SOURCE_PARTIAL, 120000L,
                mediaPackage)
        mediaPackage = service!!.addPartialTrack(presentationUri, MediaPackageElements.PRESENTATION_SOURCE_PARTIAL, 0L,
                mediaPackage)

        catalogs = mediaPackage.getCatalogs(MediaPackageElements.SMIL)
        Assert.assertEquals(0, catalogs.size.toLong())

        FieldUtils.writeField(FieldUtils.getField(IngestServiceImpl::class.java, "skipCatalogs", true),
                service, false, true)
        service!!.ingest(mediaPackage)
        catalogs = mediaPackage.getCatalogs(MediaPackageElements.SMIL)
        Assert.assertEquals(1, catalogs.size.toLong())

        Assert.assertEquals(MimeTypes.SMIL, catalogs[0].mimeType)
        val eitherDoc = XmlUtil.parseNs(InputSource(catalogs[0].getURI().toURL().openStream()))
        Assert.assertTrue(eitherDoc.isRight)
        val document = eitherDoc.right().value()
        Assert.assertEquals(1, document.getElementsByTagName("par").getLength().toLong())
        Assert.assertEquals(2, document.getElementsByTagName("seq").getLength().toLong())
        Assert.assertEquals(2, document.getElementsByTagName("video").getLength().toLong())
        Assert.assertEquals(1, document.getElementsByTagName("audio").getLength().toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testMergeScheduledMediaPackage() {
        val ingestMediaPackage = MediaPackageParser
                .getFromXml(IOUtils.toString(javaClass.getResourceAsStream("/source-manifest-partial.xml"), "UTF-8"))
        FieldUtils.writeField(FieldUtils.getField(IngestServiceImpl::class.java, "skipAttachments", true),
                service, false, true)
        FieldUtils.writeField(FieldUtils.getField(IngestServiceImpl::class.java, "skipCatalogs", true),
                service, false, true)
        var mergedMediaPackage = service!!.ingest(ingestMediaPackage).mediaPackage
        Assert.assertEquals(4, mergedMediaPackage.tracks.size.toLong())
        val track = mergedMediaPackage.getTrack("track-1")
        Assert.assertEquals("/vonlya1.mov", track.getURI().toString())
        Assert.assertEquals(3, mergedMediaPackage.catalogs.size.toLong())
        Assert.assertEquals(1, mergedMediaPackage.attachments.size.toLong())
        val attachment = mergedMediaPackage.getAttachment("cover")
        Assert.assertEquals("attachments/cover.png", attachment.getURI().toString())

        // Validate fields
        Assert.assertEquals(Date(DateTimeSupport.fromUTC("2007-12-05T13:45:00")), mergedMediaPackage.date)
        Assert.assertEquals(10045.0, mergedMediaPackage.duration!!.toDouble(), 0.0)
        Assert.assertEquals("t2", mergedMediaPackage.title)
        Assert.assertEquals("s2", mergedMediaPackage.series)
        Assert.assertEquals("st2", mergedMediaPackage.seriesTitle)
        Assert.assertEquals("l2", mergedMediaPackage.license)
        Assert.assertEquals(1, mergedMediaPackage.subjects.size.toLong())
        Assert.assertEquals("s2", mergedMediaPackage.subjects[0])
        Assert.assertEquals(1, mergedMediaPackage.contributors.size.toLong())
        Assert.assertEquals("sd2", mergedMediaPackage.contributors[0])
        Assert.assertEquals(1, mergedMediaPackage.creators.size.toLong())
        Assert.assertEquals("p2", mergedMediaPackage.creators[0])

        // check element skipping
        FieldUtils.writeField(FieldUtils.getField(IngestServiceImpl::class.java, "skipAttachments", true),
                service, true, true)
        FieldUtils.writeField(FieldUtils.getField(IngestServiceImpl::class.java, "skipCatalogs", true),
                service, true, true)
        mergedMediaPackage = service!!.ingest(ingestMediaPackage).mediaPackage
        Assert.assertEquals(0, mergedMediaPackage.catalogs.size.toLong())
        Assert.assertEquals(1, mergedMediaPackage.attachments.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testLegacyMediaPackageId() {
        val schedulerService = EasyMock.createNiceMock<SchedulerService>(SchedulerService::class.java)

        val properties = HashMap<String, String>()
        properties[CaptureParameters.INGEST_WORKFLOW_DEFINITION] = "sample"
        properties["agent-name"] = "matterhorn-agent"
        EasyMock.expect(schedulerService.getCaptureAgentConfiguration(EasyMock.anyString())).andReturn(properties)
                .anyTimes()
        EasyMock.expect(schedulerService.getDublinCore(EasyMock.anyString()))
                .andReturn(DublinCores.read(urlCatalog1!!.toURL().openStream())).anyTimes()
        val schedulerMediaPackage = MediaPackageParser
                .getFromXml(IOUtils.toString(javaClass.getResourceAsStream("/source-manifest.xml"), "UTF-8"))
        EasyMock.expect(schedulerService.getMediaPackage(EasyMock.anyString())).andThrow(NotFoundException()).once()
        EasyMock.expect(schedulerService.getMediaPackage(EasyMock.anyString())).andReturn(schedulerMediaPackage).once()
        EasyMock.expect(schedulerService.getMediaPackage(EasyMock.anyString())).andThrow(NotFoundException())
                .anyTimes()
        EasyMock.replay(schedulerService)
        service!!.setSchedulerService(schedulerService)

        val captureConfig = EasyMock.newCapture<Map<String, String>>()
        val workflowService = EasyMock.createNiceMock<WorkflowService>(WorkflowService::class.java)
        EasyMock.expect(workflowService.start(EasyMock.anyObject(WorkflowDefinition::class.java),
                EasyMock.anyObject(MediaPackage::class.java), EasyMock.capture(captureConfig)))
                .andReturn(WorkflowInstanceImpl()).once()
        EasyMock.replay(workflowService)
        service!!.setWorkflowService(workflowService)

        val ingestMediaPackage = MediaPackageParser
                .getFromXml(IOUtils.toString(javaClass.getResourceAsStream("/target-manifest.xml"), "UTF-8"))
        val wfConfig = HashMap<String, String>()
        wfConfig[IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY] = "6f7a7850-3232-4719-9064-24c9bad2832f"
        service!!.ingest(ingestMediaPackage, null!!, wfConfig)
        Assert.assertFalse(captureConfig.value.containsKey(IngestServiceImpl.LEGACY_MEDIAPACKAGE_ID_KEY))
    }

    /**
     * Test four cases: 1) If no config file 2) If config file but no key 3) If key and false value 4) If key and true
     * value
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testVarySeriesOverwriteConfiguration() {
        var isOverwriteSeries: Boolean
        val properties = Hashtable<String, String>()

        // Test with no properties
        // NOTE: This test only works if the serivce.update() was not triggered by any previous tests
        testSeriesUpdateNewAndExisting(null)

        val downloadPassword = "CHANGE_ME"
        val downloadSource = "http://localhost"
        val downloadUser = "opencast_system_account"

        properties[IngestServiceImpl.DOWNLOAD_PASSWORD] = downloadPassword
        properties[IngestServiceImpl.DOWNLOAD_SOURCE] = downloadSource
        properties[IngestServiceImpl.DOWNLOAD_USER] = downloadUser

        // Test with properties and no key
        testSeriesUpdateNewAndExisting(properties)

        // Test with properties and key is true
        isOverwriteSeries = true
        properties[IngestServiceImpl.PROPKEY_OVERWRITE_SERIES] = isOverwriteSeries.toString()

        testSeriesUpdateNewAndExisting(properties)

        // Test series overwrite key is false
        isOverwriteSeries = false
        properties[IngestServiceImpl.PROPKEY_OVERWRITE_SERIES] = isOverwriteSeries.toString()
        testSeriesUpdateNewAndExisting(properties)
    }

    @Test
    @Throws(Exception::class)
    fun testFailedJobs() {
        Assert.assertEquals(0, serviceRegistry!!.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FINISHED).size.toLong())
        Assert.assertEquals(0, serviceRegistry!!.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FAILED).size.toLong())
        service!!.addTrack(urlTrack!!, MediaPackageElements.PRESENTATION_SOURCE, service!!.createMediaPackage())
        Assert.assertEquals(1, serviceRegistry!!.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FINISHED).size.toLong())
        Assert.assertEquals(0, serviceRegistry!!.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FAILED).size.toLong())
        try {
            service!!.addTrack(URI.create("file//baduri"), MediaPackageElements.PRESENTATION_SOURCE,
                    service!!.createMediaPackage())
        } catch (e: Exception) {
            // Ignore exception
        }

        Assert.assertEquals(1, serviceRegistry!!.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FINISHED).size.toLong())
        Assert.assertEquals(1, serviceRegistry!!.getJobs(IngestServiceImpl.JOB_TYPE, Job.Status.FAILED).size.toLong())
    }

    /**
     * Test method for [org.opencastproject.ingest.impl.IngestServiceImpl.updateSeries]
     */
    @Throws(Exception::class)
    private fun testSeriesUpdateNewAndExisting(properties: Dictionary<String, String>?) {

        // default expectation for series overwrite
        var allowSeriesModifications = IngestServiceImpl.DEFAULT_ALLOW_SERIES_MODIFICATIONS

        if (properties != null) {
            service!!.updated(properties)
            try {
                val testForValue = java.lang.Boolean.parseBoolean(properties.get(IngestServiceImpl.PROPKEY_OVERWRITE_SERIES).trim { it <= ' ' })
                allowSeriesModifications = testForValue
            } catch (e: Exception) {
                // If key or value not found or not boolean, use the default overwrite expectation
            }

        }

        // Get test series dublin core for the mock return value
        val catalogFile = File(urlCatalog2!!)
        if (!catalogFile.exists() || !catalogFile.canRead())
            throw Exception("Unable to access test catalog " + urlCatalog2!!.path)
        val `in` = FileInputStream(catalogFile)
        val series = DublinCores.read(`in`)
        IOUtils.closeQuietly(`in`)

        // Set dublinCore service to return test dublin core
        dublinCoreService = org.easymock.EasyMock.createNiceMock<DublinCoreCatalogService>(DublinCoreCatalogService::class.java)
        org.easymock.EasyMock.expect(dublinCoreService!!.load(EasyMock.anyObject<Any>() as InputStream)).andReturn(series)
                .anyTimes()
        org.easymock.EasyMock.replay(dublinCoreService!!)
        service!!.setDublinCoreService(dublinCoreService)

        // Test with mock found series
        seriesService = EasyMock.createNiceMock<SeriesService>(SeriesService::class.java)
        EasyMock.expect(seriesService!!.getSeries(EasyMock.anyObject<Any>() as String)).andReturn(series).once()
        EasyMock.expect(seriesService!!.updateSeries(series)).andReturn(series).once()
        EasyMock.replay(seriesService!!)
        service!!.setSeriesService(seriesService)

        // This is true or false depending on the isOverwrite value
        Assert.assertEquals("Desire to update series is $allowSeriesModifications.",
                allowSeriesModifications, service!!.updateSeries(urlCatalog2))

        // Test with mock not found exception
        EasyMock.reset(seriesService!!)
        EasyMock.expect(seriesService!!.updateSeries(series)).andReturn(series).once()
        EasyMock.expect(seriesService!!.getSeries(EasyMock.anyObject<Any>() as String)).andThrow(NotFoundException()).once()
        EasyMock.replay(seriesService!!)

        service!!.setSeriesService(seriesService)

        // This should be true, i.e. create new series, in all cases
        Assert.assertEquals("Always create a new series catalog.", true, service!!.updateSeries(urlCatalog2))

    }

    companion object {
        private var baseDir: URI? = null
        private var urlTrack: URI? = null
        private var urlTrack1: URI? = null
        private var urlTrack2: URI? = null
        private var urlCatalog: URI? = null
        private var urlCatalog1: URI? = null
        private var urlCatalog2: URI? = null
        private var urlAttachment: URI? = null
        private var urlPackage: URI? = null
        private var urlPackageOld: URI? = null
        private var urlTrackNoFilename: URI? = null

        private var ingestTempDir: File? = null
        private var packageFile: File? = null

        private val workflowInstanceID = 1L

        @BeforeClass
        @Throws(URISyntaxException::class)
        fun beforeClass() {
            baseDir = IngestServiceImplTest::class.java.getResource("/").toURI()
            urlTrack = IngestServiceImplTest::class.java.getResource("/av.mov").toURI()
            urlTrack1 = IngestServiceImplTest::class.java.getResource("/vonly.mov").toURI()
            urlTrack2 = IngestServiceImplTest::class.java.getResource("/aonly.mov").toURI()
            urlCatalog = IngestServiceImplTest::class.java.getResource("/mpeg-7.xml").toURI()
            urlCatalog1 = IngestServiceImplTest::class.java.getResource("/dublincore.xml").toURI()
            urlCatalog2 = IngestServiceImplTest::class.java.getResource("/series-dublincore.xml").toURI()
            urlAttachment = IngestServiceImplTest::class.java.getResource("/cover.png").toURI()
            urlPackage = IngestServiceImplTest::class.java.getResource("/data.zip").toURI()
            urlPackageOld = IngestServiceImplTest::class.java.getResource("/data.old.zip").toURI()
            urlTrackNoFilename = IngestServiceImplTest::class.java.getResource("/av").toURI()

            ingestTempDir = File(File(baseDir!!), "ingest-temp")
            packageFile = File(ingestTempDir, baseDir!!.relativize(urlPackage!!).toString())
        }
    }

}
