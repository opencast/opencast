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
package org.opencastproject.liveschedule.impl

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.api.Snapshot
import org.opencastproject.assetmanager.api.Version
import org.opencastproject.assetmanager.api.query.AQueryBuilder
import org.opencastproject.assetmanager.api.query.ARecord
import org.opencastproject.assetmanager.api.query.AResult
import org.opencastproject.assetmanager.api.query.ASelectQuery
import org.opencastproject.assetmanager.api.query.Predicate
import org.opencastproject.assetmanager.api.query.Target
import org.opencastproject.assetmanager.api.query.VersionField
import org.opencastproject.capture.admin.api.CaptureAgentStateService
import org.opencastproject.distribution.api.DownloadDistributionService
import org.opencastproject.job.api.Job
import org.opencastproject.liveschedule.api.LiveScheduleService
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.Catalog
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElements
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.VideoStream
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.mediapackage.track.TrackImpl
import org.opencastproject.metadata.dublincore.DublinCore
import org.opencastproject.metadata.dublincore.DublinCoreCatalog
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService
import org.opencastproject.metadata.dublincore.DublinCoreValue
import org.opencastproject.metadata.dublincore.DublinCores
import org.opencastproject.search.api.SearchQuery
import org.opencastproject.search.api.SearchResult
import org.opencastproject.search.api.SearchResultImpl
import org.opencastproject.search.api.SearchService
import org.opencastproject.security.api.AccessControlEntry
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AclScope
import org.opencastproject.security.api.AuthorizationService
import org.opencastproject.security.api.DefaultOrganization
import org.opencastproject.security.api.JaxbOrganization
import org.opencastproject.security.api.Organization
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.series.api.SeriesService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.util.DateTimeSupport
import org.opencastproject.util.MimeType
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.data.Tuple
import org.opencastproject.workspace.api.Workspace

import com.entwinemedia.fn.Stream
import com.entwinemedia.fn.data.Opt

import org.easymock.Capture
import org.easymock.EasyMock
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.osgi.framework.BundleContext
import org.osgi.service.component.ComponentContext

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Date
import java.util.Dictionary
import java.util.HashMap
import java.util.Hashtable
import java.util.Properties

class LiveScheduleServiceImplTest {

    /** The service to test  */
    private var service: LiveScheduleServiceImpl? = null

    private var mimeType: MimeType? = null
    private var org: Organization? = null
    private var snapshot: Snapshot? = null
    private var version: Version? = null

    private var cc: ComponentContext? = null
    private var searchService: SearchService? = null
    private var seriesService: SeriesService? = null
    private var serviceRegistry: ServiceRegistry? = null
    private var captureAgentService: CaptureAgentStateService? = null
    private var workspace: Workspace? = null
    private var downloadDistributionService: DownloadDistributionService? = null
    private var dublinCoreService: DublinCoreCatalogService? = null
    private var assetManager: AssetManager? = null
    private var authService: AuthorizationService? = null
    private var organizationService: OrganizationDirectoryService? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        mimeType = MimeTypes.parseMimeType(MIME_TYPE)

        // Osgi Services
        serviceRegistry = EasyMock.createNiceMock<ServiceRegistry>(ServiceRegistry::class.java)
        searchService = EasyMock.createNiceMock<SearchService>(SearchService::class.java)
        seriesService = EasyMock.createNiceMock<SeriesService>(SeriesService::class.java)
        captureAgentService = EasyMock.createNiceMock<CaptureAgentStateService>(CaptureAgentStateService::class.java)
        EasyMock.expect(captureAgentService!!.getAgentCapabilities("demo-capture-agent")).andReturn(Properties())
        downloadDistributionService = EasyMock.createNiceMock<DownloadDistributionService>(DownloadDistributionService::class.java)
        EasyMock.expect(downloadDistributionService!!.distributionType)
                .andReturn(LiveScheduleServiceImpl.DEFAULT_LIVE_DISTRIBUTION_SERVICE).anyTimes()
        workspace = EasyMock.createNiceMock<Workspace>(Workspace::class.java)
        EasyMock.expect(workspace!!.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
                EasyMock.anyObject(InputStream::class.java))).andReturn(URI("http://someUrl"))
        dublinCoreService = EasyMock.createNiceMock<DublinCoreCatalogService>(DublinCoreCatalogService::class.java)
        assetManager = EasyMock.createNiceMock<AssetManager>(AssetManager::class.java)
        authService = AuthorizationServiceMock()
        organizationService = EasyMock.createNiceMock<OrganizationDirectoryService>(OrganizationDirectoryService::class.java)

        val defOrg = DefaultOrganization()
        val orgProps = HashMap<String, String>()
        orgProps[LiveScheduleServiceImpl.ENGAGE_URL_PROPERTY] = ENGAGE_URL
        org = JaxbOrganization(ORG_ID, "Test Organization", defOrg.servers, defOrg.adminRole,
                defOrg.anonymousRole, orgProps)
        EasyMock.expect(organizationService!!.getOrganization(ORG_ID)).andReturn(org).anyTimes()

        // Live service configuration
        val bc = EasyMock.createNiceMock<BundleContext>(BundleContext::class.java)
        val props = Hashtable<String, Any>()
        props[LiveScheduleServiceImpl.LIVE_STREAMING_URL] = STREAMING_SERVER_URL
        props[LiveScheduleServiceImpl.LIVE_STREAM_MIME_TYPE] = "video/x-flv"
        props[LiveScheduleServiceImpl.LIVE_STREAM_NAME] = STREAM_NAME
        props[LiveScheduleServiceImpl.LIVE_STREAM_RESOLUTION] = "1920x540,960x270"
        props[LiveScheduleServiceImpl.LIVE_TARGET_FLAVORS] = "presenter/delivery"

        cc = EasyMock.createNiceMock<ComponentContext>(ComponentContext::class.java)
        EasyMock.expect(cc!!.bundleContext).andReturn(bc)
        EasyMock.expect(cc!!.properties).andReturn(props)
        EasyMock.replay(bc, cc)

        service = LiveScheduleServiceImpl()
        service!!.setJobPollingInterval(1L)
        service!!.setSearchService(searchService)
        service!!.setSeriesService(seriesService)
        service!!.setCaptureAgentService(captureAgentService)
        service!!.setServiceRegistry(serviceRegistry)
        service!!.setWorkspace(workspace)
        service!!.setDublinCoreService(dublinCoreService)
        service!!.setAssetManager(assetManager)
        service!!.setAuthorizationService(authService)
        service!!.setOrganizationService(organizationService)

        service!!.activate(cc)
    }

    private fun replayServices() {
        EasyMock.replay(searchService, seriesService, serviceRegistry, captureAgentService, downloadDistributionService,
                workspace, dublinCoreService, assetManager, organizationService)
    }

    private fun assertExpectedLiveTracks(liveTracks: Array<Track>, duration: Long, caName: String, suffix: String,
                                         hasPresentation: Boolean) {
        val tracksExpected = if (hasPresentation) 4 else 2
        Assert.assertEquals(tracksExpected.toLong(), liveTracks.size.toLong())

        val presenterFound = BooleanArray(2)
        val presentationFound = BooleanArray(2)
        // Order may vary so that's why we loop
        for (track in liveTracks) {
            if (track.getURI().toString().indexOf("presenter") > -1) {
                if ((track.streams[0] as VideoStream).frameHeight == 270) {
                    Assert.assertEquals(
                            "$STREAMING_SERVER_URL/$MP_ID-$caName-presenter-delivery-stream-960x270$suffix",
                            track.getURI().toString())
                    assertLiveTrack(track, duration, 270, 960)
                    presenterFound[0] = true
                } else {
                    Assert.assertEquals(
                            "$STREAMING_SERVER_URL/$MP_ID-$caName-presenter-delivery-stream-1920x540$suffix",
                            track.getURI().toString())
                    assertLiveTrack(track, duration, 540, 1920)
                    presenterFound[1] = true
                }
            } else {
                if ((track.streams[0] as VideoStream).frameHeight == 270) {
                    Assert.assertEquals(
                            "$STREAMING_SERVER_URL/$MP_ID-$caName-presentation-delivery-stream-960x270$suffix",
                            track.getURI().toString())
                    assertLiveTrack(track, duration, 270, 960)
                    presentationFound[0] = true
                } else {
                    Assert.assertEquals(
                            "$STREAMING_SERVER_URL/$MP_ID-$caName-presentation-delivery-stream-1920x540$suffix",
                            track.getURI().toString())
                    assertLiveTrack(track, duration, 540, 1920)
                    presentationFound[1] = true
                }
            }
        }
        // Check if got all expected
        if (!presenterFound[0] || !presenterFound[1]
                || hasPresentation && (!presentationFound[0] || !presentationFound[1])) {
            Assert.fail("Didn't get the expected presenter/presentation live tracks")
        }
    }

    private fun assertLiveTrack(liveTrack: Track, duration: Long, height: Int, width: Int) {
        Assert.assertEquals(duration, liveTrack.duration)
        Assert.assertEquals(mimeType, liveTrack.mimeType)
        Assert.assertEquals(true, liveTrack.isLive)
        Assert.assertEquals(1, liveTrack.streams.size.toLong())
        Assert.assertEquals(height.toLong(), (liveTrack.streams[0] as VideoStream).frameHeight!!.toInt().toLong())
        Assert.assertEquals(width.toLong(), (liveTrack.streams[0] as VideoStream).frameWidth!!.toInt().toLong())
    }

    private fun createJob(id: Long, elementId: String, payload: String): Job {
        val job = EasyMock.createNiceMock<Job>(Job::class.java)
        val args = ArrayList<String>()
        args.add("anything")
        args.add("anything")
        args.add(elementId)
        EasyMock.expect(job.id).andReturn(id).anyTimes()
        EasyMock.expect(job.arguments).andReturn(args).anyTimes()
        EasyMock.expect(job.payload).andReturn(payload).anyTimes()
        EasyMock.expect<Status>(job.status).andReturn(Job.Status.FINISHED).anyTimes()
        EasyMock.expect(job.dateCreated).andReturn(Date()).anyTimes()
        EasyMock.expect(job.dateStarted).andReturn(Date()).anyTimes()
        EasyMock.expect<Long>(job.queueTime).andReturn(0).anyTimes()
        EasyMock.replay(job)
        return job
    }

    @Test
    @Throws(Exception::class)
    fun testReplaceVariables() {
        replayServices()

        val flavor = MediaPackageElementFlavor("presenter", "delivery")

        val expectedStreamName = "$MP_ID-$CAPTURE_AGENT_NAME-presenter-delivery-stream-3840x1080_suffix"
        val actualStreamName = service!!.replaceVariables(MP_ID, CAPTURE_AGENT_NAME, STREAM_NAME, flavor, "3840x1080")

        Assert.assertEquals(expectedStreamName, actualStreamName)
    }

    @Test
    @Throws(Exception::class)
    fun testBuildStreamingTrack() {
        replayServices()

        val uriString = "rtmp://rtmp://streaming.harvard.edu/live/stream"
        val flavor = MediaPackageElementFlavor("presenter", "delivery")

        assertLiveTrack(service!!.buildStreamingTrack(uriString, flavor, MIME_TYPE, "16x9", DURATION), DURATION, 9, 16)
    }

    @Test
    @Throws(Exception::class)
    fun testAddLiveTracksUsingDefaultProperties() {
        replayServices()

        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        mp.identifier = IdImpl(MP_ID)
        mp.duration = DURATION

        service!!.addLiveTracks(mp, CAPTURE_AGENT_NAME)
        assertExpectedLiveTracks(mp.tracks, DURATION, CAPTURE_AGENT_NAME, "_suffix", false)
    }

    @Test
    @Throws(Exception::class)
    fun testAddLiveTracksUsingCaptureAgentProperties() {
        val props = Properties()
        props[LiveScheduleServiceImpl.CA_PROPERTY_RESOLUTION_URL_PREFIX + "960x270"] = "$STREAMING_SERVER_URL/c3d913f6-9af7-403a-91a9-33b73ee18193-another-capture-agent-presenter-delivery-stream-960x270_suffix_from_ca"
        props[LiveScheduleServiceImpl.CA_PROPERTY_RESOLUTION_URL_PREFIX + "1920x540"] = "$STREAMING_SERVER_URL/c3d913f6-9af7-403a-91a9-33b73ee18193-another-capture-agent-presenter-delivery-stream-1920x540_suffix_from_ca"
        EasyMock.expect(captureAgentService!!.getAgentCapabilities("another-capture-agent")).andReturn(props).anyTimes()
        replayServices()

        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        mp.identifier = IdImpl(MP_ID)
        mp.duration = DURATION

        service!!.addLiveTracks(mp, "another-capture-agent")
        assertExpectedLiveTracks(mp.tracks, DURATION, "another-capture-agent", "_suffix_from_ca", false)
    }

    @Test
    @Throws(Exception::class)
    fun testAddAndDistributeElements() {
        val catalogURI = LiveScheduleServiceImplTest::class.java.getResource("/series.xml").toURI()
        val seriesDC = DublinCores.read(catalogURI.toURL().openStream())
        EasyMock.expect(seriesService!!.getSeries(SERIES_ID)).andReturn(seriesDC).anyTimes()

        val job = createJob(1L, "anything", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<catalog id=\"9ad6ebcb-b414-4b15-ab62-5e5ddede447e\" type=\"dublincore/episode\" xmlns=\"http://mediapackage.opencastproject.org\">"
                + "<mimetype>text/xml</mimetype>"
                + "<url>http://10.10.10.50/static/mh_default_org/engage-live/episode.xml</url></catalog>"
                + "###<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<catalog id=\"23113662-1a84-457a-85d5-0b3e32d2413a\" type=\"dublincore/series\" xmlns=\"http://mediapackage.opencastproject.org\">"
                + "<mimetype>text/xml</mimetype>"
                + "<url>http://10.10.10.50/static/mh_default_org/engage-live/series.xml</url></catalog>"
                + "###<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<attachment id=\"security-policy-episode\" type=\"security/xacml+episode\" xmlns=\"http://mediapackage.opencastproject.org\">"
                + "<mimetype>text/xml</mimetype>"
                + "<url>http://10.10.10.50/static/mh_default_org/engage-live/security_policy_episode.xml</url></attachment>")
        EasyMock.expect<Job>(downloadDistributionService!!.distribute(EasyMock.anyString(), EasyMock.anyObject(MediaPackage::class.java),
                EasyMock.anyObject<Set>(Set<*>::class.java), EasyMock.anyBoolean())).andReturn(job).once()
        EasyMock.expect(serviceRegistry!!.getJob(1L)).andReturn(job).anyTimes()

        val mpURI = LiveScheduleServiceImplTest::class.java.getResource("/assetmanager-mp.xml").toURI()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())

        replayServices()

        val s = EasyMock.createNiceMock<Snapshot>(Snapshot::class.java)
        EasyMock.expect(s.mediaPackage).andReturn(mp)
        EasyMock.replay(s)
        service!!.setDownloadDistributionService(downloadDistributionService)

        val mp1 = service!!.addAndDistributeElements(s)

        var catalogs = mp1.getCatalogs(MediaPackageElements.EPISODE)
        Assert.assertNotNull(catalogs)
        Assert.assertEquals(1, catalogs.size.toLong())
        var catalog = catalogs[0]
        Assert.assertEquals("http://10.10.10.50/static/mh_default_org/engage-live/episode.xml",
                catalog.getURI().toString())
        Assert.assertEquals("dublincore/episode", catalog.flavor.toString())
        catalogs = mp1.getCatalogs(MediaPackageElements.SERIES)
        Assert.assertNotNull(catalogs)
        Assert.assertEquals(1, catalogs.size.toLong())
        catalog = catalogs[0]
        Assert.assertEquals("http://10.10.10.50/static/mh_default_org/engage-live/series.xml", catalog.getURI().toString())
        Assert.assertEquals("dublincore/series", catalog.flavor.toString())
        val atts = mp1.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE)
        Assert.assertNotNull(atts)
        Assert.assertEquals(1, atts.size.toLong())
        val att = atts[0]
        Assert.assertEquals("http://10.10.10.50/static/mh_default_org/engage-live/security_policy_episode.xml",
                att.getURI().toString())
        Assert.assertEquals("security/xacml+episode", att.flavor.toString())
        EasyMock.verify(downloadDistributionService, workspace)
    }

    @Test
    @Throws(Exception::class)
    fun testReplaceAndDistributeAcl() {
        val mpURI = LiveScheduleServiceImplTest::class.java.getResource("/live-mp.xml").toURI()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())

        val job = createJob(1L, "anything", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<attachment id=\"security-policy-episode\" type=\"security/xacml+episode\" xmlns=\"http://mediapackage.opencastproject.org\">"
                + "<mimetype>text/xml</mimetype><url>http://host/security-policy-episode.xml</url></attachment>")
        EasyMock.expect(downloadDistributionService!!.distribute(EasyMock.anyString(), EasyMock.anyObject(MediaPackage::class.java),
                EasyMock.anyObject(String::class.java), EasyMock.anyBoolean())).andReturn(job).once()
        EasyMock.expect(serviceRegistry!!.getJob(1L)).andReturn(job).anyTimes()

        replayServices()
        service!!.setDownloadDistributionService(downloadDistributionService)

        val acl = AccessControlList(AccessControlEntry("user", "read", true))

        val mp1 = service!!.replaceAndDistributeAcl(mp, acl)

        val atts = mp1.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE)
        Assert.assertNotNull(atts)
        Assert.assertEquals(1, atts.size.toLong())
        val att = atts[0]
        Assert.assertEquals("http://host/security-policy-episode.xml", att.getURI().toString())
        Assert.assertEquals("security/xacml+episode", att.flavor.toString())
        EasyMock.verify(downloadDistributionService!!)
    }

    private fun setUpAssetManager(mp: MediaPackage) {
        version = EasyMock.createNiceMock<Version>(Version::class.java)
        snapshot = EasyMock.createNiceMock<Snapshot>(Snapshot::class.java)
        EasyMock.expect(snapshot!!.mediaPackage).andReturn(mp).anyTimes()
        EasyMock.expect(snapshot!!.organizationId).andReturn(org!!.id).anyTimes()
        EasyMock.expect(snapshot!!.version).andReturn(version)
        val aRec = EasyMock.createNiceMock<ARecord>(ARecord::class.java)
        EasyMock.expect(aRec.snapshot).andReturn(Opt.some(snapshot!!)).anyTimes()
        val recStream = Stream.mk(aRec)
        val p = EasyMock.createNiceMock<Predicate>(Predicate::class.java)
        EasyMock.expect(p.and(p)).andReturn(p).anyTimes()
        val r = EasyMock.createNiceMock<AResult>(AResult::class.java)
        EasyMock.expect(r.size).andReturn(1L).anyTimes()
        EasyMock.expect(r.records).andReturn(recStream).anyTimes()
        val t = EasyMock.createNiceMock<Target>(Target::class.java)
        val selectQuery = EasyMock.createNiceMock<ASelectQuery>(ASelectQuery::class.java)
        EasyMock.expect(selectQuery.where(EasyMock.anyObject(Predicate::class.java))).andReturn(selectQuery).anyTimes()
        EasyMock.expect(selectQuery.run()).andReturn(r).anyTimes()
        val query = EasyMock.createNiceMock<AQueryBuilder>(AQueryBuilder::class.java)
        EasyMock.expect(query.snapshot()).andReturn(t).anyTimes()
        EasyMock.expect(query.mediaPackageId(EasyMock.anyObject(String::class.java))).andReturn(p).anyTimes()
        EasyMock.expect(query.select(EasyMock.anyObject(Target::class.java))).andReturn(selectQuery).anyTimes()
        val v = EasyMock.createNiceMock<VersionField>(VersionField::class.java)
        EasyMock.expect(v.isLatest).andReturn(p).anyTimes()
        EasyMock.expect(query.version()).andReturn(v).anyTimes()
        EasyMock.expect(assetManager!!.createQuery()).andReturn(query).anyTimes()
        EasyMock.replay(snapshot, aRec, p, r, t, selectQuery, query, v)
    }

    @Test
    @Throws(Exception::class)
    fun testGetSnapshot() {
        val mpURI = LiveScheduleServiceImplTest::class.java.getResource("/assetmanager-mp.xml").toURI()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())
        setUpAssetManager(mp)
        replayServices()

        val s = service!!.getSnapshot(MP_ID)

        Assert.assertNotNull(s)
        val mp1 = s.mediaPackage
        Assert.assertNotNull(mp1)
        Assert.assertEquals(MP_ID, mp1.identifier.compact())
        Assert.assertEquals("Live Test", mp1.title)
        Assert.assertEquals("2017-10-12T18:10:59Z", DateTimeSupport.toUTC(mp1.date.time))
        Assert.assertEquals("20170119999", mp1.series)
        Assert.assertEquals("Test Fall 2017", mp1.seriesTitle)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaPackageFromSearch() {
        val searchResultURI = LiveScheduleServiceImplTest::class.java.getResource("/search-result.xml").toURI()
        val searchResult = SearchResultImpl.valueOf(searchResultURI.toURL().openStream())
        EasyMock.expect(searchService!!.getByQuery(EasyMock.anyObject<Any>() as SearchQuery)).andReturn(searchResult)
        replayServices()

        val mp = service!!.getMediaPackageFromSearch(MP_ID)
        Assert.assertNotNull(mp)
        Assert.assertEquals(MP_ID, mp!!.identifier.compact())
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaPackageFromSearchNotFound() {
        val searchResultURI = LiveScheduleServiceImplTest::class.java.getResource("/search-result-empty.xml").toURI()
        val searchResult = SearchResultImpl.valueOf(searchResultURI.toURL().openStream())
        EasyMock.expect(searchService!!.getByQuery(EasyMock.anyObject<Any>() as SearchQuery)).andReturn(searchResult)
        replayServices()

        val mp = service!!.getMediaPackageFromSearch(MP_ID)
        Assert.assertNull(mp)
    }

    @Test
    @Throws(Exception::class)
    fun testIsSameMediaPackageTrue() {
        val mpURI = LiveScheduleServiceImplTest::class.java.getResource("/assetmanager-mp.xml").toURI()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())
        setUpAssetManager(mp)
        replayServices()

        val mp1 = service!!.getSnapshot(MP_ID).mediaPackage.clone() as MediaPackage
        mp1.duration = DURATION
        service!!.addLiveTracks(mp1, CAPTURE_AGENT_NAME)
        val mp2 = service!!.getSnapshot(MP_ID).mediaPackage.clone() as MediaPackage
        mp2.duration = DURATION
        service!!.addLiveTracks(mp2, CAPTURE_AGENT_NAME)

        Assert.assertTrue(service!!.isSameMediaPackage(mp1, mp2))
    }

    @Test
    @Throws(Exception::class)
    fun testIsSameMediaPackageFalse() {
        val mpURI = LiveScheduleServiceImplTest::class.java.getResource("/assetmanager-mp.xml").toURI()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())
        setUpAssetManager(mp)
        replayServices()

        val mp1 = service!!.getSnapshot(MP_ID).mediaPackage.clone() as MediaPackage
        mp1.duration = DURATION
        service!!.addLiveTracks(mp1, CAPTURE_AGENT_NAME)
        val mp2 = service!!.getSnapshot(MP_ID).mediaPackage.clone() as MediaPackage
        mp2.duration = DURATION
        service!!.addLiveTracks(mp2, CAPTURE_AGENT_NAME)

        // Change title
        var previous = mp2.title
        mp2.title = "Changed"
        Assert.assertFalse(service!!.isSameMediaPackage(mp1, mp2))
        mp2.title = previous
        // Change language
        previous = mp2.language
        mp2.language = "Changed"
        Assert.assertFalse(service!!.isSameMediaPackage(mp1, mp2))
        mp2.language = previous
        // Change series
        previous = mp2.series
        mp2.series = "Changed"
        Assert.assertFalse(service!!.isSameMediaPackage(mp1, mp2))
        mp2.series = previous
        // Change series title
        previous = mp2.seriesTitle
        mp2.seriesTitle = "Changed"
        Assert.assertFalse(service!!.isSameMediaPackage(mp1, mp2))
        mp2.seriesTitle = previous
        // Change date
        val dt = mp2.date
        mp2.date = Date()
        Assert.assertFalse(service!!.isSameMediaPackage(mp1, mp2))
        mp2.date = dt
        // Change creators
        mp2.addCreator("New object")
        Assert.assertFalse(service!!.isSameMediaPackage(mp1, mp2))
        mp2.removeCreator("New object")
        // Change contributors
        mp2.addContributor("New object")
        Assert.assertFalse(service!!.isSameMediaPackage(mp1, mp2))
        mp2.removeContributor("New object")
        // Change subjects
        mp2.addSubject("New object")
        Assert.assertFalse(service!!.isSameMediaPackage(mp1, mp2))
        mp2.removeSubject("New object")
        // Change track uri
        var track = mp2.tracks[0]
        val previousURI = track.getURI()
        track.setURI(URI("http://new.url.com"))
        Assert.assertFalse(service!!.isSameMediaPackage(mp1, mp2))
        track.setURI(previousURI)
        // Change duration
        val duration = mp2.duration!!
        for (t in mp2.tracks) {
            (t as TrackImpl).duration = 1L
        }
        Assert.assertFalse(service!!.isSameMediaPackage(mp1, mp2))
        for (t in mp2.tracks) {
            (t as TrackImpl).duration = duration
        }
        // Change number of tracks
        track = mp2.tracks[0].clone() as Track
        mp2.remove(track)
        Assert.assertFalse(service!!.isSameMediaPackage(mp1, mp2))
        mp2.add(track)
    }

    @Test
    @Throws(Exception::class)
    fun testPublish() {
        val job = createJob(1L, "anything", "anything")
        val capturedMp = Capture.newInstance<MediaPackage>()
        EasyMock.expect(searchService!!.add(EasyMock.capture(capturedMp))).andReturn(job)
        EasyMock.expect(serviceRegistry!!.getJob(1L)).andReturn(job).anyTimes()

        replayServices()

        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew()
        mp.identifier = IdImpl(MP_ID)
        service!!.publish(mp)
        Assert.assertEquals(MP_ID, capturedMp.value.identifier.compact())
    }

    @Test
    @Throws(Exception::class)
    fun testAddLivePublicationChannel() {
        val mpURI = LiveScheduleServiceImplTest::class.java.getResource("/assetmanager-mp.xml").toURI()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())

        replayServices()

        service!!.addLivePublicationChannel(org, mp)

        val publications = mp.publications
        Assert.assertEquals(1, publications.size.toLong())
        Assert.assertEquals(LiveScheduleService.CHANNEL_ID, publications[0].channel)
        Assert.assertEquals("text/html", publications[0].mimeType.toString())
        Assert.assertEquals("$ENGAGE_URL/play/$MP_ID", publications[0].getURI().toString())
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveLivePublicationChannel() {
        val mpURI = LiveScheduleServiceImplTest::class.java.getResource("/assetmanager-mp-with-live.xml").toURI()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())

        replayServices()

        service!!.removeLivePublicationChannel(mp)

        val publications = mp.publications
        Assert.assertEquals(0, publications.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testRetract() {
        val mpURI = LiveScheduleServiceImplTest::class.java.getResource("/live-mp.xml").toURI()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())

        val job1 = createJob(1L, "anything", "anything")
        val capturedMpId = Capture.newInstance<String>()
        EasyMock.expect(searchService!!.delete(EasyMock.capture(capturedMpId))).andReturn(job1)
        EasyMock.expect(serviceRegistry!!.getJob(1L)).andReturn(job1).anyTimes()
        val job2 = createJob(2L, "anything", "anything")
        EasyMock.expect<Job>(downloadDistributionService!!.retract(EasyMock.anyString(), EasyMock.anyObject(MediaPackage::class.java),
                EasyMock.anyObject<Set>(Set<*>::class.java))).andReturn(job2)
        EasyMock.expect(serviceRegistry!!.getJob(2L)).andReturn(job2).anyTimes()

        replayServices()
        service!!.setDownloadDistributionService(downloadDistributionService)

        service!!.retract(mp)
        Assert.assertEquals(MP_ID, capturedMpId.value)

        EasyMock.verify(searchService, downloadDistributionService)
    }

    @Test
    @Throws(Exception::class)
    fun testRetractPreviousElements() {
        val mpURI = LiveScheduleServiceImplTest::class.java.getResource("/live-mp.xml").toURI()
        val previousMp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())
        val newMp = previousMp.clone() as MediaPackage

        // Change element url
        val catalog = newMp.getCatalog("episode-dc-published")
        catalog.setURI(URI("CHANGED/episode_dublincore.xml"))

        val job = createJob(1L, "anything", "anything")
        val capturedElementId = Capture.newInstance<Set<String>>()
        EasyMock.expect(downloadDistributionService!!.retract(EasyMock.anyString(), EasyMock.anyObject(MediaPackage::class.java),
                EasyMock.capture(capturedElementId))).andReturn(job)
        EasyMock.expect(serviceRegistry!!.getJob(1L)).andReturn(job).anyTimes()

        replayServices()
        service!!.setDownloadDistributionService(downloadDistributionService)

        service!!.retractPreviousElements(previousMp, newMp)
        val ids = capturedElementId.value
        Assert.assertEquals(1, ids.size.toLong())
        Assert.assertEquals("episode-dc-published", ids.iterator().next())

        EasyMock.verify(downloadDistributionService!!)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateLiveEvent() {
        val mpURI = LiveScheduleServiceImplTest::class.java.getResource("/assetmanager-mp.xml").toURI()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())
        setUpAssetManager(mp)

        var catalogURI = LiveScheduleServiceImplTest::class.java.getResource("/episode.xml").toURI()
        val episodeDC = DublinCores.read(catalogURI.toURL().openStream())

        catalogURI = LiveScheduleServiceImplTest::class.java.getResource("/series.xml").toURI()
        val seriesDC = DublinCores.read(catalogURI.toURL().openStream())
        EasyMock.expect(seriesService!!.getSeries(SERIES_ID)).andReturn(seriesDC).anyTimes()

        val job = createJob(1L, "anything", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<catalog id=\"9ad6ebcb-b414-4b15-ab62-5e5ddede447e\" type=\"dublincore/episode\" xmlns=\"http://mediapackage.opencastproject.org\">"
                + "<mimetype>text/xml</mimetype>"
                + "<url>http://10.10.10.50/static/mh_default_org/engage-live/episode.xml</url></catalog>"
                + "###<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<catalog id=\"23113662-1a84-457a-85d5-0b3e32d2413a\" type=\"dublincore/series\" xmlns=\"http://mediapackage.opencastproject.org\">"
                + "<mimetype>text/xml</mimetype>"
                + "<url>http://10.10.10.50/static/mh_default_org/engage-live/series.xml</url></catalog>"
                + "###<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<attachment id=\"security-policy-episode\" type=\"security/xacml+episode\" xmlns=\"http://mediapackage.opencastproject.org\">"
                + "<mimetype>text/xml</mimetype>"
                + "<url>http://10.10.10.50/static/mh_default_org/engage-live/security_policy_episode.xml</url></attachment>")
        EasyMock.expect<Job>(downloadDistributionService!!.distribute(EasyMock.anyString(), EasyMock.anyObject(MediaPackage::class.java),
                EasyMock.anyObject<Set>(Set<*>::class.java), EasyMock.anyBoolean())).andReturn(job)
        EasyMock.expect(serviceRegistry!!.getJob(1L)).andReturn(job).anyTimes()

        val jobPub = createJob(2L, "anything", "anything")
        val capturedMp = Capture.newInstance<MediaPackage>()
        EasyMock.expect(searchService!!.add(EasyMock.capture(capturedMp))).andReturn(jobPub)
        EasyMock.expect(serviceRegistry!!.getJob(2L)).andReturn(job).anyTimes()

        val capturedSnapshotMp = Capture.newInstance<MediaPackage>()
        val v = EasyMock.createNiceMock<Version>(Version::class.java)
        val s = EasyMock.createNiceMock<Snapshot>(Snapshot::class.java)
        EasyMock.expect(s.version).andReturn(v)
        EasyMock.replay(s, v)
        EasyMock.expect(
                assetManager!!.takeSnapshot(EasyMock.capture(capturedSnapshotMp))).andReturn(s)

        replayServices()
        service!!.setDownloadDistributionService(downloadDistributionService)

        service!!.createLiveEvent(MP_ID, episodeDC)

        // Check published live media package
        val searchMp = capturedMp.value
        Assert.assertEquals(MP_ID, searchMp.identifier.compact())
        Assert.assertEquals(DURATION, searchMp.duration!!.toLong())
        Assert.assertEquals(2, searchMp.catalogs.size.toLong())
        assertExpectedLiveTracks(searchMp.tracks, DURATION, CAPTURE_AGENT_NAME, "_suffix", false)

        // Check archived media package
        val archivedMp = capturedSnapshotMp.value
        Assert.assertEquals(MP_ID, archivedMp.identifier.compact())
        Assert.assertEquals(1, archivedMp.publications.size.toLong())
        Assert.assertEquals(LiveScheduleService.CHANNEL_ID, archivedMp.publications[0].channel)
        Assert.assertEquals(v, service!!.snapshotVersionCache.getIfPresent(MP_ID)) // Check that version got into local cache
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateLiveEventNoChange() {
        var mpURI = LiveScheduleServiceImplTest::class.java.getResource("/assetmanager-mp-with-live.xml").toURI()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())
        setUpAssetManager(mp)

        mpURI = LiveScheduleServiceImplTest::class.java.getResource("/live-mp.xml").toURI()
        val previousMp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())

        val catalogURI = LiveScheduleServiceImplTest::class.java.getResource("/episode.xml").toURI()
        val episodeDC = DublinCores.read(catalogURI.toURL().openStream())

        replayServices()

        service!!.snapshotVersionCache.put(MP_ID, version!!)

        Assert.assertFalse(service!!.updateLiveEvent(previousMp, episodeDC))
    }

    @Test
    @Throws(Exception::class)
    fun testCreateOuUpdateLiveEventAlreadyPast() {
        val searchResultURI = LiveScheduleServiceImplTest::class.java.getResource("/search-result-empty.xml").toURI()
        val searchResult = SearchResultImpl.valueOf(searchResultURI.toURL().openStream())
        EasyMock.expect(searchService!!.getByQuery(EasyMock.anyObject<Any>() as SearchQuery)).andReturn(searchResult)
        replayServices()

        val catalogURI = LiveScheduleServiceImplTest::class.java.getResource("/episode.xml").toURI()
        val episodeDC = DublinCores.read(catalogURI.toURL().openStream())
        Assert.assertFalse(service!!.createOrUpdateLiveEvent(MP_ID, episodeDC))
    }

    @Test
    @Throws(Exception::class)
    fun testCreateOuUpdateLiveEventAlreadyPublished() {
        val catalogURI = LiveScheduleServiceImplTest::class.java.getResource("/episode.xml").toURI()
        val episodeDC = DublinCores.read(catalogURI.toURL().openStream())

        val searchResultURI = LiveScheduleServiceImplTest::class.java.getResource("/no-live-search-result.xml").toURI()
        val searchResult = SearchResultImpl.valueOf(searchResultURI.toURL().openStream())
        EasyMock.expect(searchService!!.getByQuery(EasyMock.anyObject<Any>() as SearchQuery)).andReturn(searchResult)
        replayServices()

        Assert.assertFalse(service!!.createOrUpdateLiveEvent(MP_ID, episodeDC))
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateLiveEvent() {
        var mpURI = LiveScheduleServiceImplTest::class.java.getResource("/assetmanager-mp-with-live.xml").toURI()
        val mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())
        setUpAssetManager(mp)

        mpURI = LiveScheduleServiceImplTest::class.java.getResource("/live-mp.xml").toURI()
        val previousMp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())

        var catalogURI = LiveScheduleServiceImplTest::class.java.getResource("/episode.xml").toURI()
        val episodeDC = DublinCores.read(catalogURI.toURL().openStream())

        catalogURI = LiveScheduleServiceImplTest::class.java.getResource("/series.xml").toURI()
        val seriesDC = DublinCores.read(catalogURI.toURL().openStream())
        EasyMock.expect(seriesService!!.getSeries(SERIES_ID)).andReturn(seriesDC).anyTimes()

        val job = createJob(1L, "anything", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<catalog id=\"9ad6ebcb-b414-4b15-ab62-5e5ddede447e\" type=\"dublincore/episode\" xmlns=\"http://mediapackage.opencastproject.org\">"
                + "<mimetype>text/xml</mimetype>"
                + "<url>http://10.10.10.50/static/mh_default_org/engage-live/episode_updated.xml</url></catalog>"
                + "###<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<catalog id=\"23113662-1a84-457a-85d5-0b3e32d2413a\" type=\"dublincore/series\" xmlns=\"http://mediapackage.opencastproject.org\">"
                + "<mimetype>text/xml</mimetype>"
                + "<url>http://10.10.10.50/static/mh_default_org/engage-live/series.xml</url></catalog>"
                + "###<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<attachment id=\"security-policy-episode\" type=\"security/xacml+episode\" xmlns=\"http://mediapackage.opencastproject.org\">"
                + "<mimetype>text/xml</mimetype>"
                + "<url>http://10.10.10.50/static/mh_default_org/engage-live/security_policy_episode.xml</url></attachment>")
        EasyMock.expect<Job>(downloadDistributionService!!.distribute(EasyMock.anyString(), EasyMock.anyObject(MediaPackage::class.java),
                EasyMock.anyObject<Set>(Set<*>::class.java), EasyMock.anyBoolean())).andReturn(job)
        EasyMock.expect(serviceRegistry!!.getJob(1L)).andReturn(job).anyTimes()

        val jobPub = createJob(2L, "anything", "anything")
        val capturedMp = Capture.newInstance<MediaPackage>()
        EasyMock.expect(searchService!!.add(EasyMock.capture(capturedMp))).andReturn(jobPub)
        EasyMock.expect(serviceRegistry!!.getJob(2L)).andReturn(job).anyTimes()

        val jobRetract = createJob(3L, "anything", "anything")
        EasyMock.expect<Job>(downloadDistributionService!!.retract(EasyMock.anyString(), EasyMock.anyObject(MediaPackage::class.java),
                EasyMock.anyObject<Set>(Set<*>::class.java))).andReturn(jobRetract)
        EasyMock.expect(serviceRegistry!!.getJob(3L)).andReturn(jobRetract).anyTimes()

        replayServices()
        service!!.setDownloadDistributionService(downloadDistributionService)

        // Capture agent change
        episodeDC[DublinCore.PROPERTY_SPATIAL] = DublinCoreValue.mk("another_ca")
        // Duration change
        episodeDC[DublinCore.PROPERTY_TEMPORAL] = DublinCoreValue.mk("start=2017-10-12T19:00:00Z;end=2017-10-12T19:02:00Z; scheme=W3C-DTF;")

        Assert.assertTrue(service!!.updateLiveEvent(previousMp, episodeDC))

        // Check published live media package
        val searchMp = capturedMp.value
        Assert.assertEquals(MP_ID, searchMp.identifier.compact())
        Assert.assertEquals(120000L, searchMp.duration!!.toLong())
        Assert.assertEquals(2, searchMp.catalogs.size.toLong())
        assertExpectedLiveTracks(searchMp.tracks, 120000L, "another_ca", "_suffix", false)
        Assert.assertEquals(0, searchMp.publications.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testRetractLiveEvent() {
        var mpURI = LiveScheduleServiceImplTest::class.java.getResource("/assetmanager-mp-with-live.xml").toURI()
        var mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
                .loadFromXml(mpURI.toURL().openStream())
        setUpAssetManager(mp)

        mpURI = LiveScheduleServiceImplTest::class.java.getResource("/live-mp.xml").toURI()
        mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(mpURI.toURL().openStream())

        val job1 = createJob(1L, "anything", "anything")
        val capturedMpId = Capture.newInstance<String>()
        EasyMock.expect(searchService!!.delete(EasyMock.capture(capturedMpId))).andReturn(job1)
        EasyMock.expect(serviceRegistry!!.getJob(1L)).andReturn(job1).anyTimes()
        val job2 = createJob(2L, "anything", "anything")
        EasyMock.expect<Job>(downloadDistributionService!!.retract(EasyMock.anyString(), EasyMock.anyObject(MediaPackage::class.java),
                EasyMock.anyObject<Set>(Set<*>::class.java))).andReturn(job2)
        EasyMock.expect(serviceRegistry!!.getJob(2L)).andReturn(job2).anyTimes()

        val capturedSnapshotMp = Capture.newInstance<MediaPackage>()
        val v = EasyMock.createNiceMock<Version>(Version::class.java)
        val s = EasyMock.createNiceMock<Snapshot>(Snapshot::class.java)
        EasyMock.expect(s.version).andReturn(v)
        EasyMock.replay(s, v)
        EasyMock.expect(assetManager!!.takeSnapshot(EasyMock.capture(capturedSnapshotMp))).andReturn(s)

        replayServices()
        service!!.setDownloadDistributionService(downloadDistributionService)

        service!!.retractLiveEvent(mp)

        // Check archived media package
        val archivedMp = capturedSnapshotMp.value
        Assert.assertEquals(MP_ID, archivedMp.identifier.compact())
        Assert.assertEquals(0, archivedMp.publications.size.toLong())

        EasyMock.verify(searchService, downloadDistributionService)
    }

    internal inner class AuthorizationServiceMock : AuthorizationService {
        override fun setAcl(mp: MediaPackage, scope: AclScope, acl: AccessControlList): Tuple<MediaPackage, Attachment> {
            try {
                val attachment = MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                        .elementFromURI(URI("http://host/episode_auth.xml"), Attachment.TYPE,
                                MediaPackageElements.XACML_POLICY_EPISODE) as Attachment
                attachment.mimeType = MimeTypes.XML
                mp.add(attachment)
            } catch (e: URISyntaxException) {
            }

            return null
        }

        override fun removeAcl(mp: MediaPackage, scope: AclScope): MediaPackage {
            return null
        }

        override fun hasPermission(mp: MediaPackage, action: String): Boolean {
            return false
        }

        override fun getActiveAcl(mp: MediaPackage): Tuple<AccessControlList, AclScope> {
            return null
        }

        override fun getAcl(mp: MediaPackage, scope: AclScope): Tuple<AccessControlList, AclScope> {
            return null
        }

        @Throws(IOException::class)
        override fun getAclFromInputStream(`in`: InputStream): AccessControlList {
            // TODO Auto-generated method stub
            return null
        }
    }

    companion object {

        private val MP_ID = "c3d913f6-9af7-403a-91a9-33b73ee18193"
        private val SERIES_ID = "20170119999"
        private val CAPTURE_AGENT_NAME = "fake-ca"
        private val MIME_TYPE = "video/x-flv"
        private val STREAMING_SERVER_URL = "rtmp://cp999999.live.edgefcs.net/live"
        private val STREAM_NAME = "#{id}-#{caName}-#{flavor}-stream-#{resolution}_suffix"
        private val DURATION = 60000L
        private val ORG_ID = "org"
        private val ENGAGE_URL = "htttp://engage.server"
    }

}
