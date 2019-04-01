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
package org.opencastproject.distribution.streaming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.opencastproject.util.UrlSupport.concat

import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobBarrier
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageParser
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl
import org.opencastproject.util.PathSupport
import org.opencastproject.util.data.Option
import org.opencastproject.workspace.api.Workspace

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.easymock.EasyMock
import org.easymock.IAnswer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.net.URI

class StreamingDistributionServiceTest {
    private var service: StreamingDistributionServiceImpl? = null
    private var mp: MediaPackage? = null
    private var distributionRoot: File? = null
    private var serviceRegistry: ServiceRegistry? = null
    private var defaultOrganization: DefaultOrganization? = null

    @Rule
    var testFolder = TemporaryFolder()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val mediaPackageRoot = File(javaClass.getResource("/mediapackage.xml").toURI()).parentFile
        mp = MediaPackageParser.getFromXml(IOUtils.toString(javaClass.getResourceAsStream("/mediapackage.xml"), "UTF-8"))

        distributionRoot = File(mediaPackageRoot, "static")
        service = StreamingDistributionServiceImpl()

        defaultOrganization = DefaultOrganization()
        val anonymous = JaxbUser("anonymous", "test", defaultOrganization!!,
                JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, defaultOrganization!!))
        val userDirectoryService = EasyMock.createMock<UserDirectoryService>(UserDirectoryService::class.java)
        EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject<Any>() as String)).andReturn(anonymous).anyTimes()
        EasyMock.replay(userDirectoryService)
        service!!.userDirectoryService = userDirectoryService

        val organizationDirectoryService = EasyMock.createMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)
        EasyMock.expect<Organization>(organizationDirectoryService.getOrganization(EasyMock.anyObject<Any>() as String))
                .andReturn(defaultOrganization).anyTimes()
        EasyMock.replay(organizationDirectoryService)
        service!!.organizationDirectoryService = organizationDirectoryService

        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect(securityService.user).andReturn(anonymous).anyTimes()
        EasyMock.expect<Organization>(securityService.organization).andReturn(defaultOrganization).anyTimes()
        EasyMock.replay(securityService)
        service!!.securityService = securityService

        serviceRegistry = ServiceRegistryInMemoryImpl(service!!, securityService, userDirectoryService,
                organizationDirectoryService, EasyMock.createNiceMock(IncidentService::class.java))
        service!!.serviceRegistry = serviceRegistry

        val workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace.get(EasyMock.anyObject<Any>() as URI)).andAnswer {
            val uri = EasyMock.getCurrentArguments()[0] as URI
            val pathElems = uri.path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val file = pathElems[pathElems.size - 1]
            File(mediaPackageRoot, file)
        }.anyTimes()
        EasyMock.replay(workspace)
        service!!.setWorkspace(workspace)

        val bc = EasyMock.createNiceMock<BundleContext>(BundleContext::class.java)
        EasyMock.expect(bc.getProperty("org.opencastproject.streaming.url")).andReturn("rtmp://localhost/").anyTimes()
        EasyMock.expect(bc.getProperty("org.opencastproject.streaming.directory")).andReturn(distributionRoot!!.path)
                .anyTimes()
        EasyMock.replay(bc)

        val cc = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(cc.bundleContext).andReturn(bc).anyTimes()
        EasyMock.replay(cc)

        service!!.activate(cc)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        FileUtils.deleteDirectory(distributionRoot!!)
        (serviceRegistry as ServiceRegistryInMemoryImpl).dispose()
    }

    @Test
    @Throws(Exception::class)
    fun testUriFileConversionFlvWorkspace() {
        val loc = StreamingDistributionServiceImpl.Locations(
                URI.create("rtmp://localhost/matterhorn-engage"), testFolder.newFolder(), false)
        val channelId = "engage-player"
        val mpId = "9f411edb-edf5-4308-8df5-f9b111d9d346"
        val mpeId = "bed1cdba-2d42-49b1-b78f-6c6745fb064a"
        val mpeUri = URI
                .create(concat("http://localhost:8080/files/mediapackage/", mpId, mpeId, "hans_arp_1m10s.flv"))
        //
        val distUri = loc.createDistributionUri(defaultOrganization!!.id, channelId, mpId, mpeId, mpeUri)
        logger.info(distUri.toString())
        assertTrue("original URI and distribution URI are not equal", distUri != mpeUri)
        val distFile = loc.createDistributionFile(defaultOrganization!!.id, channelId, mpId, mpeId, mpeUri)
        logger.info(distFile.toString())
        val retrievedFile = loc.getDistributionFileFrom(distUri)
        logger.info(retrievedFile.toString())
        assertTrue("file could be retrieved from distribution URI", retrievedFile.isSome)
        assertEquals("file retrieved from distribution URI and distribution file match", distFile, retrievedFile.get())
    }

    @Test
    @Throws(Exception::class)
    fun testUriFileConversionFlvDistribution() {
        val loc = StreamingDistributionServiceImpl.Locations(
                URI.create("rtmp://localhost/matterhorn-engage"), testFolder.newFolder(), false)
        val channelId = "engage-player"
        val mpId = "9f411edb-edf5-4308-8df5-f9b111d9d346"
        val mpeId = "bed1cdba-2d42-49b1-b78f-6c6745fb064a"
        val mpeUri = URI.create(
                concat("rtmp://localhost/matterhorn-engage/mh_default_org/", channelId, mpId, mpeId, "Hans_Arp_1m10s"))
        //
        val distUri = loc.createDistributionUri(defaultOrganization!!.id, channelId, mpId, mpeId, mpeUri)
        logger.info(distUri.toString())
        assertEquals("original URI and distribution URI are equal", distUri, mpeUri)
        val distFile = loc.createDistributionFile(defaultOrganization!!.id, channelId, mpId, mpeId, mpeUri)
        logger.info(distFile.toString())
        val retrievedFile = loc.getDistributionFileFrom(distUri)
        logger.info(retrievedFile.toString())
        assertTrue("file could be retrieved from distribution URI", retrievedFile.isSome)
        assertEquals("file retrieved from distribution URI and distribution file match", distFile, retrievedFile.get())
    }

    @Test
    @Throws(Exception::class)
    fun testUriFileConversionMp4Workspace() {
        val loc = StreamingDistributionServiceImpl.Locations(
                URI.create("rtmp://localhost/matterhorn-engage"), testFolder.newFolder(), false)
        val channelId = "engage-player"
        val mpId = "9f411edb-edf5-4308-8df5-f9b111d9d346"
        val mpeId = "bed1cdba-2d42-49b1-b78f-6c6745fb064a"
        val mpeUri = URI
                .create(concat("http://localhost:8080/files/mediapackage/", mpId, mpeId, "hans_arp_1m10s.mp4"))
        //
        val distUri = loc.createDistributionUri(defaultOrganization!!.id, channelId, mpId, mpeId, mpeUri)
        logger.info(distUri.toString())
        assertTrue("original URI and distribution URI are not equal", distUri != mpeUri)
        val distFile = loc.createDistributionFile(defaultOrganization!!.id, channelId, mpId, mpeId, mpeUri)
        logger.info(distFile.toString())
        val retrievedFile = loc.getDistributionFileFrom(distUri)
        logger.info(retrievedFile.toString())
        assertTrue("file could be retrieved from distribution URI", retrievedFile.isSome)
        assertEquals("file retrieved from distribution URI and distribution file match", distFile, retrievedFile.get())
    }

    @Test
    @Throws(Exception::class)
    fun testUriFileConversionMp4Distribution() {
        val loc = StreamingDistributionServiceImpl.Locations(
                URI.create("rtmp://localhost/matterhorn-engage"), testFolder.newFolder(), false)
        val channelId = "engage-player"
        val mpId = "9f411edb-edf5-4308-8df5-f9b111d9d346"
        val mpeId = "bed1cdba-2d42-49b1-b78f-6c6745fb064a"
        val mpeUri = URI.create(
                concat("rtmp://localhost/matterhorn-engage/mp4:mh_default_org/", channelId, mpId, mpeId, "Hans_Arp_1m10s"))
        //
        val distUri = loc.createDistributionUri(defaultOrganization!!.id, channelId, mpId, mpeId, mpeUri)
        logger.info(distUri.toString())
        assertEquals("original URI and distribution URI are equal", distUri, mpeUri)
        val distFile = loc.createDistributionFile(defaultOrganization!!.id, channelId, mpId, mpeId, mpeUri)
        logger.info(distFile.toString())
        val retrievedFile = loc.getDistributionFileFrom(distUri)
        logger.info(retrievedFile.toString())
        assertTrue("file could be retrieved from distribution URI", retrievedFile.isSome)
        assertEquals("file retrieved from distribution URI and distribution file match", distFile, retrievedFile.get())
    }

    @Test
    @Throws(Exception::class)
    fun testUriFileRetrieval() {
        val testDir = testFolder.newFolder()
        val loc1 = StreamingDistributionServiceImpl.Locations(
                URI.create("rtmp://localhost/matterhorn-engage"), testDir, false)
        val loc2 = StreamingDistributionServiceImpl.Locations(
                URI.create("rtmp://localhost/matterhorn-engage/"), testDir, false)
        val channelId = "engage-player"
        val mpId = "9f411edb-edf5-4308-8df5-f9b111d9d346"
        val mpeId = "bed1cdba-2d42-49b1-b78f-6c6745fb064a"
        val distUri = URI.create(
                concat("rtmp://localhost/matterhorn-engage/mp4:mh_default_org/", channelId, mpId, mpeId, "Hans_Arp_1m10s"))
        //
        val retrievedFile1 = loc1.getDistributionFileFrom(distUri)
        val retrievedFile2 = loc2.getDistributionFileFrom(distUri)
        assertTrue("file could be retrieved from distribution URI", retrievedFile1.isSome)
        assertTrue("file could be retrieved from distribution URI", retrievedFile2.isSome)
        assertEquals(retrievedFile1, retrievedFile2)
    }

    @Test
    @Throws(Exception::class)
    fun testDistribution() {
        // Distribute the mediapackage and all of its elements
        val job1 = service!!.distribute("engage-player", mp!!, "track-1")
        val job2 = service!!.distribute("oai-pmh", mp!!, "track-1")
        val jobBarrier = JobBarrier(null, serviceRegistry, 500, job1, job2)
        jobBarrier.waitForJobs()

        // Add the new elements to the mediapackage
        mp!!.add(MediaPackageElementParser.getFromXml(job1.payload))
        mp!!.add(MediaPackageElementParser.getFromXml(job2.payload))

        val mpDir = File(distributionRoot,
                PathSupport.path(defaultOrganization!!.id, "engage-player", mp!!.identifier.compact()))
        val mediaDir = File(mpDir, "track-1")
        Assert.assertTrue(mediaDir.exists())
        Assert.assertTrue(File(mediaDir, "media.mov").exists()) // the filenames are changed to reflect the element ID
    }

    companion object {

        private val logger = LoggerFactory.getLogger(StreamingDistributionServiceTest::class.java)
    }

}
