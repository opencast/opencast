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
package org.opencastproject.publication.oaipmh

import org.easymock.EasyMock.anyString
import org.easymock.EasyMock.capture
import org.easymock.EasyMock.eq
import org.opencastproject.mediapackage.MediaPackageElementFlavor.parseFlavor
import org.opencastproject.oaipmh.server.OaiPmhServerInfoUtil.ORG_CFG_OAIPMH_SERVER_HOSTURL

import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.distribution.api.DownloadDistributionService
import org.opencastproject.distribution.api.StreamingDistributionService
import org.opencastproject.job.api.AbstractJobProducer
import org.opencastproject.job.api.Job
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageSupport
import org.opencastproject.mediapackage.Publication
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase
import org.opencastproject.oaipmh.persistence.OaiPmhDatabaseException
import org.opencastproject.oaipmh.persistence.Query
import org.opencastproject.oaipmh.persistence.impl.SearchResultImpl
import org.opencastproject.oaipmh.server.OaiPmhServerInfo
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbRole
import org.opencastproject.security.api.JaxbUser
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.User
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.IncidentService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.serviceregistry.api.ServiceRegistryException
import org.opencastproject.serviceregistry.api.ServiceRegistryInMemoryImpl
import org.opencastproject.serviceregistry.api.UndispatchableJobException
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.data.Collections

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import java.net.URI
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

class OaiPmhPublicationServiceImplTest {


    private var mp: MediaPackage? = null
    private var mp2: MediaPackage? = null
    private var validOaiPmhRepositories: List<String>? = null
    private var service: OaiPmhPublicationServiceImpl? = null
    private var serviceRegistry: ServiceRegistryInMemoryImpl? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        mp = MediaPackageSupport.loadFromClassPath("/mediapackage.xml")
        mp2 = MediaPackageSupport.loadFromClassPath("/mediapackage2.xml")
        validOaiPmhRepositories = Collections.list("default")

        val oaiPmhServerInfo = EasyMock.createNiceMock<OaiPmhServerInfo>(OaiPmhServerInfo::class.java)
        EasyMock.expect(oaiPmhServerInfo.hasRepo(anyString()))
                .andAnswer { validOaiPmhRepositories!!.contains(EasyMock.getCurrentArguments()[0] as String) }.anyTimes()
        EasyMock.expect(oaiPmhServerInfo.mountPoint).andReturn(OAI_PMH_SERVER_MOUNT_POINT).anyTimes()

        val org = object : DefaultOrganization() {
            public override val properties: Map<String, String>
                get() {
                    val props = HashMap<String, String>()
                    props.putAll(DefaultOrganization.DEFAULT_PROPERTIES)
                    props[ORG_CFG_OAIPMH_SERVER_HOSTURL] = OAI_PMH_SERVER_URL
                    return props
                }
        }
        val roles = HashSet<JaxbRole>()
        roles.add(JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, org, ""))
        val user = JaxbUser("admin", "test", org, roles)
        val orgDirectory = EasyMock.createNiceMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)
        EasyMock.expect<Organization>(orgDirectory.getOrganization(EasyMock.anyObject<Any>() as String)).andReturn(org).anyTimes()

        val userDirectory = EasyMock.createNiceMock<UserDirectoryService>(UserDirectoryService::class.java)
        EasyMock.expect(userDirectory.loadUser("admin")).andReturn(user).anyTimes()

        val securityService = EasyMock.createNiceMock<SecurityService>(SecurityService::class.java)
        EasyMock.expect<Organization>(securityService.organization).andReturn(org).anyTimes()
        EasyMock.expect(securityService.user).andReturn(user).anyTimes()

        serviceRegistry = ServiceRegistryInMemoryImpl(service!!, securityService, userDirectory, orgDirectory,
                EasyMock.createNiceMock(IncidentService::class.java))

        // Finish setting up the mocks
        EasyMock.replay(oaiPmhServerInfo, orgDirectory, userDirectory, securityService)

        service = OaiPmhPublicationServiceImpl()
        service!!.setOaiPmhServerInfo(oaiPmhServerInfo)
        service!!.securityService = securityService
        service!!.serviceRegistry = serviceRegistry

        // mock streaming/download distribution jobs dispatching
        val distributionJobProducerMock = object : AbstractJobProducer("distribute") {
            protected override val serviceRegistry: ServiceRegistry
                get() = serviceRegistry

            override val securityService: SecurityService
                get() = securityService

            override val userDirectoryService: UserDirectoryService
                get() = userDirectory

            override val organizationDirectoryService: OrganizationDirectoryService
                get() = orgDirectory

            @Throws(Exception::class)
            override fun process(job: Job): String {
                return job.payload
            }

            @Throws(ServiceRegistryException::class, UndispatchableJobException::class)
            override fun isReadyToAccept(job: Job): Boolean {
                return true
            }
        }
        serviceRegistry!!.registerService(distributionJobProducerMock)
    }

    @Test
    @Throws(MediaPackageException::class, PublicationException::class, ServiceRegistryException::class)
    fun testPublish() {
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        val dummyJob = EasyMock.createMock<Job>(Job::class.java)
        val jobArgsCapture = EasyMock.newCapture<List<String>>()
        EasyMock.expect(serviceRegistry.createJob(
                eq(OaiPmhPublicationService.JOB_TYPE),
                eq(OaiPmhPublicationServiceImpl.Operation.Publish.toString()),
                capture(jobArgsCapture))).andReturn(dummyJob).once()
        EasyMock.replay(serviceRegistry)
        service!!.serviceRegistry = serviceRegistry

        val j = service!!.publish(mp!!, "default",
                Collections.set("catalog-1", "catalog-2", "track-1"), Collections.set("track-1"), true)

        Assert.assertSame(dummyJob, j)
        val jobArgs = jobArgsCapture.value
        // test job arguments
        Assert.assertEquals(5, jobArgs.size.toLong())
        Assert.assertTrue(jobArgs[0].contains("<mediapackage "))
        Assert.assertEquals("default", jobArgs[1])
        val downloadDistributionIdsArg = jobArgs[2]
        Assert.assertNotNull(downloadDistributionIdsArg)
        Assert.assertTrue(downloadDistributionIdsArg.contains("catalog-1"))
        Assert.assertTrue(downloadDistributionIdsArg.contains("catalog-2"))
        Assert.assertTrue(downloadDistributionIdsArg.contains("track-1"))
        val streamingDistributionIdsArg = jobArgs[3]
        Assert.assertNotNull(streamingDistributionIdsArg)
        Assert.assertTrue(!streamingDistributionIdsArg.contains("catalog-1"))
        Assert.assertTrue(!streamingDistributionIdsArg.contains("catalog-2"))
        Assert.assertTrue(streamingDistributionIdsArg.contains("track-1"))
        Assert.assertTrue(BooleanUtils.toBoolean(jobArgs[4]))
    }

    @Test
    @Throws(NotFoundException::class, PublicationException::class, ServiceRegistryException::class)
    fun testRetract() {
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        val dummyJob = EasyMock.createMock<Job>(Job::class.java)
        val jobArgsCapture = EasyMock.newCapture<List<String>>()
        EasyMock.expect(serviceRegistry.createJob(
                eq(OaiPmhPublicationService.JOB_TYPE),
                eq(OaiPmhPublicationServiceImpl.Operation.Retract.toString()),
                capture(jobArgsCapture))).andReturn(dummyJob).once()
        EasyMock.replay(serviceRegistry)
        service!!.serviceRegistry = serviceRegistry

        val j = service!!.retract(mp2!!, "default")

        Assert.assertSame(dummyJob, j)
        // test job arguments
        val jobArgs = jobArgsCapture.value
        Assert.assertEquals(2, jobArgs.size.toLong())
        Assert.assertTrue(jobArgs[0].contains("<mediapackage "))
        Assert.assertEquals("default", jobArgs[1])
    }

    @Test
    @Throws(PublicationException::class, ServiceRegistryException::class, MediaPackageException::class)
    fun testUpdateMetadata() {
        val serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        val dummyJob = EasyMock.createMock<Job>(Job::class.java)
        val jobArgsCapture = EasyMock.newCapture<List<String>>()
        EasyMock.expect(serviceRegistry.createJob(
                eq(OaiPmhPublicationService.JOB_TYPE),
                eq(OaiPmhPublicationServiceImpl.Operation.UpdateMetadata.toString()),
                capture(jobArgsCapture))).andReturn(dummyJob).once()
        EasyMock.replay(serviceRegistry)
        service!!.serviceRegistry = serviceRegistry

        val flavorsSet = Collections.set("dublincore/*", "security/*")
        val tagsSet = Collections.set("archive", "other")
        val j = service!!.updateMetadata(mp!!, "default", flavorsSet, tagsSet, true)

        Assert.assertSame(dummyJob, j)
        // test job arguments
        val jobArgs = jobArgsCapture.value
        Assert.assertEquals(5, jobArgs.size.toLong())
        Assert.assertTrue(jobArgs[0].contains("<mediapackage "))
        Assert.assertEquals("default", jobArgs[1])
        Assert.assertEquals(StringUtils.join<Set<String>>(flavorsSet, OaiPmhPublicationServiceImpl.SEPARATOR), jobArgs[2])
        Assert.assertEquals(StringUtils.join<Set<String>>(tagsSet, OaiPmhPublicationServiceImpl.SEPARATOR), jobArgs[3])
        Assert.assertTrue(java.lang.Boolean.valueOf(jobArgs[4]))
    }

    @Ignore
    @Test
    @Throws(PublicationException::class, OaiPmhDatabaseException::class, MediaPackageException::class, DistributionException::class)
    fun testPublishInternal() {
        val oaiDb = EasyMock.createNiceMock<OaiPmhDatabase>(OaiPmhDatabase::class.java)
        // mock empty DB
        EasyMock.expect<SearchResult>(oaiDb.search(EasyMock.anyObject(Query::class.java)))
                .andReturn(SearchResultImpl(0, 0, ArrayList<SearchResultItem>())).once()
        val storedMpCap = EasyMock.newCapture<MediaPackage>()
        // capture stored media package
        oaiDb.store(capture(storedMpCap), eq("default"))
        EasyMock.replay(oaiDb)
        service!!.setOaiPmhDatabase(oaiDb)

        // mock download distribution service
        val downloadDistributionService = EasyMock.createNiceMock<DownloadDistributionService>(DownloadDistributionService::class.java)
        val downloadDistributedMpCap = EasyMock.newCapture<MediaPackage>()
        val downloadDistributedElemIdsCap = EasyMock.newCapture<Set<String>>()
        EasyMock.expect(downloadDistributionService.distribute(EasyMock.contains("default"),
                capture(downloadDistributedMpCap), capture(downloadDistributedElemIdsCap),
                eq(true))).andAnswer {
            serviceRegistry!!.createJob("distribute", "download", null!!,
                    serializeMediaPackageElements(EasyMock.getCurrentArguments()[1] as MediaPackage))
        }.anyTimes()
        EasyMock.replay(downloadDistributionService)
        service!!.setDownloadDistributionService(downloadDistributionService)

        // mock streaming distribution service
        val streamingDistributionService = EasyMock.createNiceMock<StreamingDistributionService>(StreamingDistributionService::class.java)
        val streamingDistributedMpCap = EasyMock.newCapture<MediaPackage>()
        val streamingDistributedElemIdsCap = EasyMock.newCapture<Set<String>>()
        EasyMock.expect(streamingDistributionService.distribute(EasyMock.contains("default"),
                capture(streamingDistributedMpCap), capture(streamingDistributedElemIdsCap)))
                .andAnswer {
                    serviceRegistry!!.createJob("distribute", "streaming", null!!,
                            serializeMediaPackageElements(EasyMock.getCurrentArguments()[1] as MediaPackage))
                }.anyTimes()
        EasyMock.replay(streamingDistributionService)
        service!!.setStreamingDistributionService(streamingDistributionService)

        val publication = service!!.publish(null, mp, "default", Collections.set("catalog-1", "track-1"),
                Collections.set("track-1"), true)

        Assert.assertNotNull(publication)
        Assert.assertNotNull(publication.channel)
        Assert.assertTrue(publication.channel.contains("default"))
        Assert.assertNotNull(publication.getURI())
        Assert.assertEquals(URI.create(OAI_PMH_SERVER_URL).host, publication.getURI().getHost())
        Assert.assertTrue(publication.getURI().getPath().startsWith(OAI_PMH_SERVER_MOUNT_POINT))
        Assert.assertTrue(downloadDistributedMpCap.hasCaptured())
        // check distributed elements
        // download distribution elements
        var mp = downloadDistributedMpCap.value
        Assert.assertEquals(2, mp.elements.size.toLong())
        Assert.assertEquals(1, mp.getElementsByFlavor(parseFlavor("dublincore/episode")).size.toLong())
        Assert.assertNotEquals("catalog-1", mp.getElementsByFlavor(parseFlavor("dublincore/episode"))[0].identifier)
        Assert.assertEquals(1, mp.getElementsByFlavor(parseFlavor("presentation/source")).size.toLong())
        Assert.assertNotEquals("track-1", mp.getElementsByFlavor(parseFlavor("presentation/source"))[0].identifier)
        // streaming distribution elements
        Assert.assertTrue(streamingDistributedMpCap.hasCaptured())
        mp = streamingDistributedMpCap.value
        Assert.assertEquals(1, mp.elements.size.toLong())
        Assert.assertEquals(1, mp.getElementsByFlavor(parseFlavor("presentation/source")).size.toLong())
        Assert.assertNotEquals("track-1", mp.getElementsByFlavor(parseFlavor("presentation/source"))[0].identifier)
        // check stored media package
        Assert.assertTrue(storedMpCap.hasCaptured())
        mp = storedMpCap.value
        Assert.assertEquals(4, mp.elements.size.toLong())
        Assert.assertEquals(1, mp.getElementsByFlavor(parseFlavor("dublincore/episode")).size.toLong())
        Assert.assertEquals(2, mp.getElementsByFlavor(parseFlavor("presentation/source")).size.toLong())
        Assert.assertEquals(1, mp.publications.size.toLong())
    }

    @Test
    fun testRetractInternal() {
        // todo
    }

    @Test
    fun testUpdateMetadataInternal() {
        // todo
    }

    companion object {

        val OAI_PMH_SERVER_URL = "http://myorg.tld"
        val OAI_PMH_SERVER_MOUNT_POINT = "/oaipmh"

        @Throws(MediaPackageException::class)
        private fun serializeMediaPackageElements(mp: MediaPackage): String {
            Assert.assertNotNull(mp)
            return MediaPackageElementParser.getArrayAsXml(Collections.list(*mp.elements))
        }
    }
}
