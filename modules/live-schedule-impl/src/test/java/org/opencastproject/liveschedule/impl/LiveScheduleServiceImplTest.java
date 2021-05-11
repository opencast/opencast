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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.liveschedule.impl;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.api.query.VersionField;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.liveschedule.api.LiveScheduleService;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultImpl;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class LiveScheduleServiceImplTest {

  private static final String MP_ID = "c3d913f6-9af7-403a-91a9-33b73ee18193";
  private static final String SERIES_ID = "20170119999";
  private static final String CAPTURE_AGENT_NAME = "fake-ca";
  private static final String MIME_TYPE = "video/x-flv";
  private static final String STREAMING_SERVER_URL = "rtmp://cp999999.live.edgefcs.net/live";
  private static final String STREAM_NAME = "#{id}-#{caName}-#{flavor}-stream-#{resolution}_suffix";
  private static final long DURATION = 60000L;
  private static final String ORG_ID = "org";
  private static final String ENGAGE_URL = "htttp://engage.server";

  /** The service to test */
  private LiveScheduleServiceImpl service;

  private MimeType mimeType;
  private Organization org;
  private Snapshot snapshot;
  private Version version;

  private ComponentContext cc;
  private SearchService searchService;
  private SeriesService seriesService;
  private ServiceRegistry serviceRegistry;
  private CaptureAgentStateService captureAgentService;
  private Workspace workspace;
  private DownloadDistributionService downloadDistributionService;
  private DublinCoreCatalogService dublinCoreService;
  private AssetManager assetManager;
  private AuthorizationService authService;
  private OrganizationDirectoryService organizationService;
  private SecurityService securityService;

  @Before
  public void setUp() throws Exception {
    mimeType = MimeTypes.parseMimeType(MIME_TYPE);

    // Osgi Services
    serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    searchService = EasyMock.createNiceMock(SearchService.class);
    seriesService = EasyMock.createNiceMock(SeriesService.class);
    captureAgentService = EasyMock.createNiceMock(CaptureAgentStateService.class);
    EasyMock.expect(captureAgentService.getAgentCapabilities("demo-capture-agent")).andReturn(new Properties());
    downloadDistributionService = EasyMock.createNiceMock(DownloadDistributionService.class);
    EasyMock.expect(downloadDistributionService.getDistributionType())
            .andReturn(LiveScheduleServiceImpl.DEFAULT_LIVE_DISTRIBUTION_SERVICE).anyTimes();
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
            EasyMock.anyObject(InputStream.class))).andReturn(new URI("http://someUrl"));
    dublinCoreService = EasyMock.createNiceMock(DublinCoreCatalogService.class);
    assetManager = EasyMock.createNiceMock(AssetManager.class);
    authService = new AuthorizationServiceMock();
    organizationService = EasyMock.createNiceMock(OrganizationDirectoryService.class);

    Organization defOrg = new DefaultOrganization();
    Map<String, String> orgProps = new HashMap<>();
    orgProps.put(LiveScheduleServiceImpl.ENGAGE_URL_PROPERTY, ENGAGE_URL);
    org = new JaxbOrganization(ORG_ID, "Test Organization", defOrg.getServers(), defOrg.getAdminRole(),
            defOrg.getAnonymousRole(), orgProps);
    EasyMock.expect(organizationService.getOrganization(ORG_ID)).andReturn(org).anyTimes();

    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(null);

    // Live service configuration
    BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER)).andReturn("system-user");
    Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(LiveScheduleServiceImpl.LIVE_STREAMING_URL, STREAMING_SERVER_URL);
    props.put(LiveScheduleServiceImpl.LIVE_STREAM_MIME_TYPE, "video/x-flv");
    props.put(LiveScheduleServiceImpl.LIVE_STREAM_NAME, STREAM_NAME);
    props.put(LiveScheduleServiceImpl.LIVE_STREAM_RESOLUTION, "1920x540,960x270");
    props.put(LiveScheduleServiceImpl.LIVE_TARGET_FLAVORS, "presenter/delivery");

    cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc);
    EasyMock.expect(cc.getProperties()).andReturn(props);
    EasyMock.replay(bc, cc);

    service = new LiveScheduleServiceImpl();
    service.setJobPollingInterval(1L);
    service.setSearchService(searchService);
    service.setSeriesService(seriesService);
    service.setCaptureAgentService(captureAgentService);
    service.setServiceRegistry(serviceRegistry);
    service.setWorkspace(workspace);
    service.setDublinCoreService(dublinCoreService);
    service.setAssetManager(assetManager);
    service.setAuthorizationService(authService);
    service.setOrganizationService(organizationService);
    service.setSecurityService(securityService);
    service.activate(cc);
  }

  private void replayServices() {
    EasyMock.replay(searchService, seriesService, serviceRegistry, captureAgentService, downloadDistributionService,
            workspace, dublinCoreService, assetManager, organizationService, securityService);
  }

  private void assertExpectedLiveTracks(Track[] liveTracks, long duration, String caName, String suffix,
          boolean hasPresentation) {
    int tracksExpected = hasPresentation ? 4 : 2;
    Assert.assertEquals(tracksExpected, liveTracks.length);

    boolean[] presenterFound = new boolean[2];
    boolean[] presentationFound = new boolean[2];
    // Order may vary so that's why we loop
    for (Track track : liveTracks) {
      if (track.getURI().toString().indexOf("presenter") > -1) {
        if (((VideoStream) track.getStreams()[0]).getFrameHeight() == 270) {
          Assert.assertEquals(
                  STREAMING_SERVER_URL + "/" + MP_ID + "-" + caName + "-presenter-delivery-stream-960x270" + suffix,
                  track.getURI().toString());
          assertLiveTrack(track, duration, 270, 960);
          presenterFound[0] = true;
        } else {
          Assert.assertEquals(
                  STREAMING_SERVER_URL + "/" + MP_ID + "-" + caName + "-presenter-delivery-stream-1920x540" + suffix,
                  track.getURI().toString());
          assertLiveTrack(track, duration, 540, 1920);
          presenterFound[1] = true;
        }
      } else {
        if (((VideoStream) track.getStreams()[0]).getFrameHeight() == 270) {
          Assert.assertEquals(
                  STREAMING_SERVER_URL + "/" + MP_ID + "-" + caName + "-presentation-delivery-stream-960x270" + suffix,
                  track.getURI().toString());
          assertLiveTrack(track, duration, 270, 960);
          presentationFound[0] = true;
        } else {
          Assert.assertEquals(
                  STREAMING_SERVER_URL + "/" + MP_ID + "-" + caName + "-presentation-delivery-stream-1920x540" + suffix,
                  track.getURI().toString());
          assertLiveTrack(track, duration, 540, 1920);
          presentationFound[1] = true;
        }
      }
    }
    // Check if got all expected
    if (!presenterFound[0] || !presenterFound[1]
            || (hasPresentation && (!presentationFound[0] || !presentationFound[1]))) {
      Assert.fail("Didn't get the expected presenter/presentation live tracks");
    }
  }

  private void assertLiveTrack(Track liveTrack, long duration, int height, int width) {
    Assert.assertEquals(new Long(duration), liveTrack.getDuration());
    Assert.assertEquals(mimeType, liveTrack.getMimeType());
    Assert.assertEquals(true, liveTrack.isLive());
    Assert.assertEquals(1, liveTrack.getStreams().length);
    Assert.assertEquals(height, ((VideoStream) liveTrack.getStreams()[0]).getFrameHeight().intValue());
    Assert.assertEquals(width, ((VideoStream) liveTrack.getStreams()[0]).getFrameWidth().intValue());
  }

  private Job createJob(long id, String elementId, String payload) {
    Job job = EasyMock.createNiceMock(Job.class);
    List<String> args = new ArrayList<String>();
    args.add("anything");
    args.add("anything");
    args.add(elementId);
    EasyMock.expect(job.getId()).andReturn(id).anyTimes();
    EasyMock.expect(job.getArguments()).andReturn(args).anyTimes();
    EasyMock.expect(job.getPayload()).andReturn(payload).anyTimes();
    EasyMock.expect(job.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    EasyMock.expect(job.getDateCreated()).andReturn(new Date()).anyTimes();
    EasyMock.expect(job.getDateStarted()).andReturn(new Date()).anyTimes();
    EasyMock.expect(job.getQueueTime()).andReturn(new Long(0)).anyTimes();
    EasyMock.replay(job);
    return job;
  }

  @Test
  public void testReplaceVariables() throws Exception {
    replayServices();

    MediaPackageElementFlavor flavor = new MediaPackageElementFlavor("presenter", "delivery");

    String expectedStreamName = MP_ID + "-" + CAPTURE_AGENT_NAME + "-presenter-delivery-stream-3840x1080_suffix";
    String actualStreamName = service.replaceVariables(MP_ID, CAPTURE_AGENT_NAME, STREAM_NAME, flavor, "3840x1080");

    Assert.assertEquals(expectedStreamName, actualStreamName);
  }

  @Test
  public void testBuildStreamingTrack() throws Exception {
    replayServices();

    String uriString = "rtmp://rtmp://streaming.harvard.edu/live/stream";
    MediaPackageElementFlavor flavor = new MediaPackageElementFlavor("presenter", "delivery");

    assertLiveTrack(service.buildStreamingTrack(uriString, flavor, MIME_TYPE, "16x9", DURATION), DURATION, 9, 16);
  }

  @Test
  public void testAddLiveTracksUsingDefaultProperties() throws Exception {
    replayServices();

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.setIdentifier(new IdImpl(MP_ID));
    mp.setDuration(DURATION);

    service.addLiveTracks(mp, CAPTURE_AGENT_NAME);
    assertExpectedLiveTracks(mp.getTracks(), DURATION, CAPTURE_AGENT_NAME, "_suffix", false);
  }

  @Test
  public void testAddLiveTracksUsingCaptureAgentProperties() throws Exception {
    Properties props = new Properties();
    props.put(LiveScheduleServiceImpl.CA_PROPERTY_RESOLUTION_URL_PREFIX + "960x270", STREAMING_SERVER_URL
            + "/c3d913f6-9af7-403a-91a9-33b73ee18193-another-capture-agent-presenter-delivery-stream-960x270_suffix_from_ca");
    props.put(LiveScheduleServiceImpl.CA_PROPERTY_RESOLUTION_URL_PREFIX + "1920x540", STREAMING_SERVER_URL
            + "/c3d913f6-9af7-403a-91a9-33b73ee18193-another-capture-agent-presenter-delivery-stream-1920x540_suffix_from_ca");
    EasyMock.expect(captureAgentService.getAgentCapabilities("another-capture-agent")).andReturn(props).anyTimes();
    replayServices();

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.setIdentifier(new IdImpl(MP_ID));
    mp.setDuration(DURATION);

    service.addLiveTracks(mp, "another-capture-agent");
    assertExpectedLiveTracks(mp.getTracks(), DURATION, "another-capture-agent", "_suffix_from_ca", false);
  }

  @Test
  public void testAddAndDistributeElements() throws Exception {
    URI catalogURI = LiveScheduleServiceImplTest.class.getResource("/series.xml").toURI();
    DublinCoreCatalog seriesDC = DublinCores.read(catalogURI.toURL().openStream());
    EasyMock.expect(seriesService.getSeries(SERIES_ID)).andReturn(seriesDC).anyTimes();

    Job job = createJob(1L, "anything", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
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
            + "<url>http://10.10.10.50/static/mh_default_org/engage-live/security_policy_episode.xml</url></attachment>");
    EasyMock.expect(downloadDistributionService.distribute(EasyMock.anyString(), EasyMock.anyObject(MediaPackage.class),
            EasyMock.anyObject(Set.class), EasyMock.anyBoolean())).andReturn(job).once();
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job).anyTimes();

    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/assetmanager-mp.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());

    replayServices();

    Snapshot s = EasyMock.createNiceMock(Snapshot.class);
    EasyMock.expect(s.getMediaPackage()).andReturn(mp);
    EasyMock.replay(s);
    service.setDownloadDistributionService(downloadDistributionService);

    MediaPackage mp1 = service.addAndDistributeElements(s);

    Catalog[] catalogs = mp1.getCatalogs(MediaPackageElements.EPISODE);
    Assert.assertNotNull(catalogs);
    Assert.assertEquals(1, catalogs.length);
    Catalog catalog = catalogs[0];
    Assert.assertEquals("http://10.10.10.50/static/mh_default_org/engage-live/episode.xml",
            catalog.getURI().toString());
    Assert.assertEquals("dublincore/episode", catalog.getFlavor().toString());
    catalogs = mp1.getCatalogs(MediaPackageElements.SERIES);
    Assert.assertNotNull(catalogs);
    Assert.assertEquals(1, catalogs.length);
    catalog = catalogs[0];
    Assert.assertEquals("http://10.10.10.50/static/mh_default_org/engage-live/series.xml", catalog.getURI().toString());
    Assert.assertEquals("dublincore/series", catalog.getFlavor().toString());
    Attachment[] atts = mp1.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE);
    Assert.assertNotNull(atts);
    Assert.assertEquals(1, atts.length);
    Attachment att = atts[0];
    Assert.assertEquals("http://10.10.10.50/static/mh_default_org/engage-live/security_policy_episode.xml",
            att.getURI().toString());
    Assert.assertEquals("security/xacml+episode", att.getFlavor().toString());
    EasyMock.verify(downloadDistributionService, workspace);
  }

  @Test
  public void testReplaceAndDistributeAcl() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/live-mp.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());

    Job job = createJob(1L, "anything", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<attachment id=\"security-policy-episode\" type=\"security/xacml+episode\" xmlns=\"http://mediapackage.opencastproject.org\">"
            + "<mimetype>text/xml</mimetype><url>http://host/security-policy-episode.xml</url></attachment>");
    EasyMock.expect(downloadDistributionService.distribute(EasyMock.anyString(), EasyMock.anyObject(MediaPackage.class),
            EasyMock.anyObject(String.class), EasyMock.anyBoolean())).andReturn(job).once();
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job).anyTimes();

    replayServices();
    service.setDownloadDistributionService(downloadDistributionService);

    AccessControlList acl = new AccessControlList(new AccessControlEntry("user", "read", true));

    MediaPackage mp1 = service.replaceAndDistributeAcl(mp, acl);

    Attachment[] atts = mp1.getAttachments(MediaPackageElements.XACML_POLICY_EPISODE);
    Assert.assertNotNull(atts);
    Assert.assertEquals(1, atts.length);
    Attachment att = atts[0];
    Assert.assertEquals("http://host/security-policy-episode.xml", att.getURI().toString());
    Assert.assertEquals("security/xacml+episode", att.getFlavor().toString());
    EasyMock.verify(downloadDistributionService);
  }

  private void setUpAssetManager(MediaPackage mp) {
    version = EasyMock.createNiceMock(Version.class);
    snapshot = EasyMock.createNiceMock(Snapshot.class);
    EasyMock.expect(snapshot.getMediaPackage()).andReturn(mp).anyTimes();
    EasyMock.expect(snapshot.getOrganizationId()).andReturn(org.getId()).anyTimes();
    EasyMock.expect(snapshot.getVersion()).andReturn(version);
    ARecord aRec = EasyMock.createNiceMock(ARecord.class);
    EasyMock.expect(aRec.getSnapshot()).andReturn(Opt.some(snapshot)).anyTimes();
    Stream<ARecord> recStream = Stream.mk(aRec);
    Predicate p = EasyMock.createNiceMock(Predicate.class);
    EasyMock.expect(p.and(p)).andReturn(p).anyTimes();
    AResult r = EasyMock.createNiceMock(AResult.class);
    EasyMock.expect(r.getSize()).andReturn(1L).anyTimes();
    EasyMock.expect(r.getRecords()).andReturn(recStream).anyTimes();
    Target t = EasyMock.createNiceMock(Target.class);
    ASelectQuery selectQuery = EasyMock.createNiceMock(ASelectQuery.class);
    EasyMock.expect(selectQuery.where(EasyMock.anyObject(Predicate.class))).andReturn(selectQuery).anyTimes();
    EasyMock.expect(selectQuery.run()).andReturn(r).anyTimes();
    AQueryBuilder query = EasyMock.createNiceMock(AQueryBuilder.class);
    EasyMock.expect(query.snapshot()).andReturn(t).anyTimes();
    EasyMock.expect(query.mediaPackageId(EasyMock.anyObject(String.class))).andReturn(p).anyTimes();
    EasyMock.expect(query.select(EasyMock.anyObject(Target.class))).andReturn(selectQuery).anyTimes();
    VersionField v = EasyMock.createNiceMock(VersionField.class);
    EasyMock.expect(v.isLatest()).andReturn(p).anyTimes();
    EasyMock.expect(query.version()).andReturn(v).anyTimes();
    EasyMock.expect(assetManager.createQuery()).andReturn(query).anyTimes();
    EasyMock.replay(snapshot, aRec, p, r, t, selectQuery, query, v);
  }

  @Test
  public void testGetSnapshot() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/assetmanager-mp.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());
    setUpAssetManager(mp);
    replayServices();

    Snapshot s = service.getSnapshot(MP_ID);

    Assert.assertNotNull(s);
    MediaPackage mp1 = s.getMediaPackage();
    Assert.assertNotNull(mp1);
    Assert.assertEquals(MP_ID, mp1.getIdentifier().compact());
    Assert.assertEquals("Live Test", mp1.getTitle());
    Assert.assertEquals("2017-10-12T18:10:59Z", DateTimeSupport.toUTC(mp1.getDate().getTime()));
    Assert.assertEquals("20170119999", mp1.getSeries());
    Assert.assertEquals("Test Fall 2017", mp1.getSeriesTitle());
  }

  @Test
  public void testGetMediaPackageFromSearch() throws Exception {
    URI searchResultURI = LiveScheduleServiceImplTest.class.getResource("/search-result.xml").toURI();
    SearchResult searchResult = SearchResultImpl.valueOf(searchResultURI.toURL().openStream());
    EasyMock.expect(searchService.getForAdministrativeRead((SearchQuery) EasyMock.anyObject())).andReturn(searchResult);
    replayServices();

    MediaPackage mp = service.getMediaPackageFromSearch(MP_ID);
    Assert.assertNotNull(mp);
    Assert.assertEquals(MP_ID, mp.getIdentifier().compact());
  }

  @Test
  public void testGetMediaPackageFromSearchNotFound() throws Exception {
    URI searchResultURI = LiveScheduleServiceImplTest.class.getResource("/search-result-empty.xml").toURI();
    SearchResult searchResult = SearchResultImpl.valueOf(searchResultURI.toURL().openStream());
    EasyMock.expect(searchService.getForAdministrativeRead((SearchQuery) EasyMock.anyObject())).andReturn(searchResult);
    replayServices();

    MediaPackage mp = service.getMediaPackageFromSearch(MP_ID);
    Assert.assertNull(mp);
  }

  @Test
  public void testIsSameMediaPackageTrue() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/assetmanager-mp.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());
    setUpAssetManager(mp);
    replayServices();

    MediaPackage mp1 = (MediaPackage) service.getSnapshot(MP_ID).getMediaPackage().clone();
    mp1.setDuration(DURATION);
    service.addLiveTracks(mp1, CAPTURE_AGENT_NAME);
    MediaPackage mp2 = (MediaPackage) service.getSnapshot(MP_ID).getMediaPackage().clone();
    mp2.setDuration(DURATION);
    service.addLiveTracks(mp2, CAPTURE_AGENT_NAME);

    Assert.assertTrue(service.isSameMediaPackage(mp1, mp2));
  }

  @Test
  public void testIsSameMediaPackageFalse() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/assetmanager-mp.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());
    setUpAssetManager(mp);
    replayServices();

    MediaPackage mp1 = (MediaPackage) service.getSnapshot(MP_ID).getMediaPackage().clone();
    mp1.setDuration(DURATION);
    service.addLiveTracks(mp1, CAPTURE_AGENT_NAME);
    MediaPackage mp2 = (MediaPackage) service.getSnapshot(MP_ID).getMediaPackage().clone();
    mp2.setDuration(DURATION);
    service.addLiveTracks(mp2, CAPTURE_AGENT_NAME);

    // Change title
    String previous = mp2.getTitle();
    mp2.setTitle("Changed");
    Assert.assertFalse(service.isSameMediaPackage(mp1, mp2));
    mp2.setTitle(previous);
    // Change language
    previous = mp2.getLanguage();
    mp2.setLanguage("Changed");
    Assert.assertFalse(service.isSameMediaPackage(mp1, mp2));
    mp2.setLanguage(previous);
    // Change series
    previous = mp2.getSeries();
    mp2.setSeries("Changed");
    Assert.assertFalse(service.isSameMediaPackage(mp1, mp2));
    mp2.setSeries(previous);
    // Change series title
    previous = mp2.getSeriesTitle();
    mp2.setSeriesTitle("Changed");
    Assert.assertFalse(service.isSameMediaPackage(mp1, mp2));
    mp2.setSeriesTitle(previous);
    // Change date
    Date dt = mp2.getDate();
    mp2.setDate(new Date());
    Assert.assertFalse(service.isSameMediaPackage(mp1, mp2));
    mp2.setDate(dt);
    // Change creators
    mp2.addCreator("New object");
    Assert.assertFalse(service.isSameMediaPackage(mp1, mp2));
    mp2.removeCreator("New object");
    // Change contributors
    mp2.addContributor("New object");
    Assert.assertFalse(service.isSameMediaPackage(mp1, mp2));
    mp2.removeContributor("New object");
    // Change subjects
    mp2.addSubject("New object");
    Assert.assertFalse(service.isSameMediaPackage(mp1, mp2));
    mp2.removeSubject("New object");
    // Change track uri
    Track track = mp2.getTracks()[0];
    URI previousURI = track.getURI();
    track.setURI(new URI("http://new.url.com"));
    Assert.assertFalse(service.isSameMediaPackage(mp1, mp2));
    track.setURI(previousURI);
    // Change duration
    long duration = mp2.getDuration();
    for (Track t : mp2.getTracks()) {
      ((TrackImpl) t).setDuration(1L);
    }
    Assert.assertFalse(service.isSameMediaPackage(mp1, mp2));
    for (Track t : mp2.getTracks()) {
      ((TrackImpl) t).setDuration(duration);
    }
    // Change number of tracks
    track = (Track) mp2.getTracks()[0].clone();
    mp2.remove(track);
    Assert.assertFalse(service.isSameMediaPackage(mp1, mp2));
    mp2.add(track);
  }

  @Test
  public void testPublish() throws Exception {
    Job job = createJob(1L, "anything", "anything");
    Capture<MediaPackage> capturedMp = Capture.newInstance();
    EasyMock.expect(searchService.add(EasyMock.capture(capturedMp))).andReturn(job);
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job).anyTimes();

    replayServices();

    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.setIdentifier(new IdImpl(MP_ID));
    service.publish(mp);
    Assert.assertEquals(MP_ID, capturedMp.getValue().getIdentifier().compact());
  }

  @Test
  public void testAddLivePublicationChannel() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/assetmanager-mp.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());

    replayServices();

    service.addLivePublicationChannel(org, mp, new HashMap<String, Track>());
    Publication[] publications = mp.getPublications();
    Assert.assertEquals(1, publications.length);
    Assert.assertEquals(LiveScheduleService.CHANNEL_ID, publications[0].getChannel());
    Assert.assertEquals("text/html", publications[0].getMimeType().toString());
    Assert.assertEquals(ENGAGE_URL + "/play/" + MP_ID, publications[0].getURI().toString());
  }

  @Test
  public void testRemoveLivePublicationChannel() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/assetmanager-mp-with-live.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());

    replayServices();

    service.removeLivePublicationChannel(mp);

    Publication[] publications = mp.getPublications();
    Assert.assertEquals(0, publications.length);
  }

  @Test
  public void testRetract() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/live-mp.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());

    Job job1 = createJob(1L, "anything", "anything");
    Capture<String> capturedMpId = Capture.newInstance();
    EasyMock.expect(searchService.delete(EasyMock.capture(capturedMpId))).andReturn(job1);
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job1).anyTimes();
    Job job2 = createJob(2L, "anything", "anything");
    EasyMock.expect(downloadDistributionService.retract(EasyMock.anyString(), EasyMock.anyObject(MediaPackage.class),
            EasyMock.anyObject(Set.class))).andReturn(job2);
    EasyMock.expect(serviceRegistry.getJob(2L)).andReturn(job2).anyTimes();

    replayServices();
    service.setDownloadDistributionService(downloadDistributionService);

    service.retract(mp);
    Assert.assertEquals(MP_ID, capturedMpId.getValue());

    EasyMock.verify(searchService, downloadDistributionService);
  }

  @Test
  public void testRetractPreviousElements() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/live-mp.xml").toURI();
    MediaPackage previousMp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());
    MediaPackage newMp = (MediaPackage) previousMp.clone();

    // Change element url
    Catalog catalog = newMp.getCatalog("episode-dc-published");
    catalog.setURI(new URI("CHANGED/episode_dublincore.xml"));

    Job job = createJob(1L, "anything", "anything");
    Capture<Set<String>> capturedElementId = Capture.newInstance();
    EasyMock.expect(downloadDistributionService.retract(EasyMock.anyString(), EasyMock.anyObject(MediaPackage.class),
            EasyMock.capture(capturedElementId))).andReturn(job);
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job).anyTimes();

    replayServices();
    service.setDownloadDistributionService(downloadDistributionService);

    service.retractPreviousElements(previousMp, newMp);
    Set<String> ids = capturedElementId.getValue();
    Assert.assertEquals(1, ids.size());
    Assert.assertEquals("episode-dc-published", ids.iterator().next());

    EasyMock.verify(downloadDistributionService);
  }

  @Test
  public void testCreateLiveEvent() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/assetmanager-mp.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());
    setUpAssetManager(mp);

    URI catalogURI = LiveScheduleServiceImplTest.class.getResource("/episode.xml").toURI();
    DublinCoreCatalog episodeDC = DublinCores.read(catalogURI.toURL().openStream());

    catalogURI = LiveScheduleServiceImplTest.class.getResource("/series.xml").toURI();
    DublinCoreCatalog seriesDC = DublinCores.read(catalogURI.toURL().openStream());
    EasyMock.expect(seriesService.getSeries(SERIES_ID)).andReturn(seriesDC).anyTimes();

    Job job = createJob(1L, "anything", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
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
            + "<url>http://10.10.10.50/static/mh_default_org/engage-live/security_policy_episode.xml</url></attachment>");
    EasyMock.expect(downloadDistributionService.distribute(EasyMock.anyString(), EasyMock.anyObject(MediaPackage.class),
            EasyMock.anyObject(Set.class), EasyMock.anyBoolean())).andReturn(job);
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job).anyTimes();

    Job jobPub = createJob(2L, "anything", "anything");
    Capture<MediaPackage> capturedMp = Capture.newInstance();
    EasyMock.expect(searchService.add(EasyMock.capture(capturedMp))).andReturn(jobPub);
    EasyMock.expect(serviceRegistry.getJob(2L)).andReturn(job).anyTimes();

    Capture<MediaPackage> capturedSnapshotMp = Capture.newInstance();
    Version v = EasyMock.createNiceMock(Version.class);
    Snapshot s = EasyMock.createNiceMock(Snapshot.class);
    EasyMock.expect(s.getVersion()).andReturn(v);
    EasyMock.replay(s, v);
    EasyMock.expect(
            assetManager.takeSnapshot(EasyMock.capture(capturedSnapshotMp))).andReturn(s);

    replayServices();
    service.setDownloadDistributionService(downloadDistributionService);

    service.createLiveEvent(MP_ID, episodeDC);

    // Check published live media package
    MediaPackage searchMp = capturedMp.getValue();
    Assert.assertEquals(MP_ID, searchMp.getIdentifier().compact());
    Assert.assertEquals(DURATION, searchMp.getDuration().longValue());
    Assert.assertEquals(2, searchMp.getCatalogs().length);
    assertExpectedLiveTracks(searchMp.getTracks(), DURATION, CAPTURE_AGENT_NAME, "_suffix", false);

    // Check archived media package
    MediaPackage archivedMp = capturedSnapshotMp.getValue();
    Assert.assertEquals(MP_ID, archivedMp.getIdentifier().compact());
    Assert.assertEquals(1, archivedMp.getPublications().length);
    Assert.assertEquals(LiveScheduleService.CHANNEL_ID, archivedMp.getPublications()[0].getChannel());
    Assert.assertEquals(v, service.getSnapshotVersionCache().getIfPresent(MP_ID)); // Check that version got into local cache
  }

  @Test
  public void testUpdateLiveEventNoChange() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/assetmanager-mp-with-live.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());
    setUpAssetManager(mp);

    mpURI = LiveScheduleServiceImplTest.class.getResource("/live-mp.xml").toURI();
    MediaPackage previousMp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());

    URI catalogURI = LiveScheduleServiceImplTest.class.getResource("/episode.xml").toURI();
    DublinCoreCatalog episodeDC = DublinCores.read(catalogURI.toURL().openStream());

    replayServices();

    service.getSnapshotVersionCache().put(MP_ID, version);

    Assert.assertFalse(service.updateLiveEvent(previousMp, episodeDC));
  }

  @Test
  public void testCreateOuUpdateLiveEventAlreadyPast() throws Exception {
    URI searchResultURI = LiveScheduleServiceImplTest.class.getResource("/search-result-empty.xml").toURI();
    SearchResult searchResult = SearchResultImpl.valueOf(searchResultURI.toURL().openStream());
    EasyMock.expect(searchService.getForAdministrativeRead((SearchQuery) EasyMock.anyObject())).andReturn(searchResult);
    replayServices();

    URI catalogURI = LiveScheduleServiceImplTest.class.getResource("/episode.xml").toURI();
    DublinCoreCatalog episodeDC = DublinCores.read(catalogURI.toURL().openStream());
    Assert.assertFalse(service.createOrUpdateLiveEvent(MP_ID, episodeDC));
  }

  @Test
  public void testCreateOuUpdateLiveEventAlreadyPublished() throws Exception {
    URI catalogURI = LiveScheduleServiceImplTest.class.getResource("/episode.xml").toURI();
    DublinCoreCatalog episodeDC = DublinCores.read(catalogURI.toURL().openStream());

    URI searchResultURI = LiveScheduleServiceImplTest.class.getResource("/no-live-search-result.xml").toURI();
    SearchResult searchResult = SearchResultImpl.valueOf(searchResultURI.toURL().openStream());
    EasyMock.expect(searchService.getForAdministrativeRead((SearchQuery) EasyMock.anyObject())).andReturn(searchResult);
    replayServices();

    Assert.assertFalse(service.createOrUpdateLiveEvent(MP_ID, episodeDC));
  }

  @Test
  public void testUpdateLiveEvent() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/assetmanager-mp-with-live.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());
    setUpAssetManager(mp);

    mpURI = LiveScheduleServiceImplTest.class.getResource("/live-mp.xml").toURI();
    MediaPackage previousMp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());

    URI catalogURI = LiveScheduleServiceImplTest.class.getResource("/episode.xml").toURI();
    DublinCoreCatalog episodeDC = DublinCores.read(catalogURI.toURL().openStream());

    catalogURI = LiveScheduleServiceImplTest.class.getResource("/series.xml").toURI();
    DublinCoreCatalog seriesDC = DublinCores.read(catalogURI.toURL().openStream());
    EasyMock.expect(seriesService.getSeries(SERIES_ID)).andReturn(seriesDC).anyTimes();

    Job job = createJob(1L, "anything", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
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
            + "<url>http://10.10.10.50/static/mh_default_org/engage-live/security_policy_episode.xml</url></attachment>");
    EasyMock.expect(downloadDistributionService.distribute(EasyMock.anyString(), EasyMock.anyObject(MediaPackage.class),
            EasyMock.anyObject(Set.class), EasyMock.anyBoolean())).andReturn(job);
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job).anyTimes();

    Job jobPub = createJob(2L, "anything", "anything");
    Capture<MediaPackage> capturedMp = Capture.newInstance();
    EasyMock.expect(searchService.add(EasyMock.capture(capturedMp))).andReturn(jobPub);
    EasyMock.expect(serviceRegistry.getJob(2L)).andReturn(job).anyTimes();

    Job jobRetract = createJob(3L, "anything", "anything");
    EasyMock.expect(downloadDistributionService.retract(EasyMock.anyString(), EasyMock.anyObject(MediaPackage.class),
            EasyMock.anyObject(Set.class))).andReturn(jobRetract);
    EasyMock.expect(serviceRegistry.getJob(3L)).andReturn(jobRetract).anyTimes();

    replayServices();
    service.setDownloadDistributionService(downloadDistributionService);

    // Capture agent change
    episodeDC.set(DublinCore.PROPERTY_SPATIAL, DublinCoreValue.mk("another_ca"));
    // Duration change
    episodeDC.set(DublinCore.PROPERTY_TEMPORAL,
            DublinCoreValue.mk("start=2017-10-12T19:00:00Z;end=2017-10-12T19:02:00Z; scheme=W3C-DTF;"));

    Assert.assertTrue(service.updateLiveEvent(previousMp, episodeDC));

    // Check published live media package
    MediaPackage searchMp = capturedMp.getValue();
    Assert.assertEquals(MP_ID, searchMp.getIdentifier().compact());
    Assert.assertEquals(120000L, searchMp.getDuration().longValue());
    Assert.assertEquals(2, searchMp.getCatalogs().length);
    assertExpectedLiveTracks(searchMp.getTracks(), 120000L, "another_ca", "_suffix", false);
    Assert.assertEquals(0, searchMp.getPublications().length);
  }

  @Test
  public void testRetractLiveEvent() throws Exception {
    URI mpURI = LiveScheduleServiceImplTest.class.getResource("/assetmanager-mp-with-live.xml").toURI();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(mpURI.toURL().openStream());
    setUpAssetManager(mp);

    mpURI = LiveScheduleServiceImplTest.class.getResource("/live-mp.xml").toURI();
    mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(mpURI.toURL().openStream());

    Job job1 = createJob(1L, "anything", "anything");
    Capture<String> capturedMpId = Capture.newInstance();
    EasyMock.expect(searchService.delete(EasyMock.capture(capturedMpId))).andReturn(job1);
    EasyMock.expect(serviceRegistry.getJob(1L)).andReturn(job1).anyTimes();
    Job job2 = createJob(2L, "anything", "anything");
    EasyMock.expect(downloadDistributionService.retract(EasyMock.anyString(), EasyMock.anyObject(MediaPackage.class),
            EasyMock.anyObject(Set.class))).andReturn(job2);
    EasyMock.expect(serviceRegistry.getJob(2L)).andReturn(job2).anyTimes();

    Capture<MediaPackage> capturedSnapshotMp = Capture.newInstance();
    Version v = EasyMock.createNiceMock(Version.class);
    Snapshot s = EasyMock.createNiceMock(Snapshot.class);
    EasyMock.expect(s.getVersion()).andReturn(v);
    EasyMock.replay(s, v);
    EasyMock.expect(assetManager.takeSnapshot(EasyMock.capture(capturedSnapshotMp))).andReturn(s);

    replayServices();
    service.setDownloadDistributionService(downloadDistributionService);

    service.retractLiveEvent(mp);

    // Check archived media package
    MediaPackage archivedMp = capturedSnapshotMp.getValue();
    Assert.assertEquals(MP_ID, archivedMp.getIdentifier().compact());
    Assert.assertEquals(0, archivedMp.getPublications().length);

    EasyMock.verify(searchService, downloadDistributionService);
  }

  class AuthorizationServiceMock implements AuthorizationService {
    @Override
    public Tuple<MediaPackage, Attachment> setAcl(MediaPackage mp, AclScope scope, AccessControlList acl) {
      try {
        Attachment attachment = (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                .elementFromURI(new URI("http://host/episode_auth.xml"), Attachment.TYPE,
                        MediaPackageElements.XACML_POLICY_EPISODE);
        attachment.setMimeType(MimeTypes.XML);
        mp.add(attachment);
      } catch (URISyntaxException e) {
      }
      return null;
    }

    @Override
    public MediaPackage removeAcl(MediaPackage mp, AclScope scope) {
      return null;
    }

    @Override
    public boolean hasPermission(MediaPackage mp, String action) {
      return false;
    }

    @Override
    public Tuple<AccessControlList, AclScope> getActiveAcl(MediaPackage mp) {
      return null;
    }

    @Override
    public Tuple<AccessControlList, AclScope> getAcl(MediaPackage mp, AclScope scope) {
      return null;
    }

    @Override
    public AccessControlList getAclFromInputStream(InputStream in) throws IOException {
      // TODO Auto-generated method stub
      return null;
    }
  };

}
