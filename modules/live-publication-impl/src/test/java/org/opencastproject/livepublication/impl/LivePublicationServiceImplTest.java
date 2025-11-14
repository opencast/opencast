/*
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
package org.opencastproject.livepublication.impl;

import static org.opencastproject.livepublication.api.LivePublicationService.LIVE_CHANNEL_ID;
import static org.opencastproject.livepublication.impl.LivePublicationServiceImpl.DEFAULT_LIVE_TARGET_FLAVOR;
import static org.opencastproject.livepublication.impl.LiveTracksCreator.CA_PROPERTY_RESOLUTION_URL_PREFIX;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.livepublication.publication.ArchiveUpdater;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import junit.framework.AssertionFailedError;

public class LivePublicationServiceImplTest {

  // constants
  private static final String MP_ID = "c3d913f6-9af7-403a-91a9-33b73ee18193";
  private static final String CACHED_VERSION = "100";
  private static final String CAPTURE_AGENT_NAME = "fake-ca";
  private static final String NEW_CA = "new-ca";
  private static final String CA_WITH_PROPERTIES = "ca-with-properties";
  private static final String STREAMING_SERVER_URL = "rtmp://cp999999.live.edgefcs.net/live";
  private static final String STREAM_NAME = "#{id}-#{caName}-#{flavor}-stream-#{resolution}_suffix";
  private static final String ORG_ID = "org";
  private static final String ENGAGE_URL = "htttp://engage.server";
  private static final String MIMETYPE = "video/x-flv";
  private static final String RESOLUTION = "1920x540,960x270";
  private static final String FLAVORS = "presenter/delivery,presentation/delivery";

  private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ENGLISH);

  /** The service to test */
  private LivePublicationServiceImpl service;

  //private MimeType mimeType;

  // services
  private ComponentContext cc;
  private BundleContext bc;
  private SearchService searchService;
  private CaptureAgentStateService captureAgentService;
  private Workspace workspace;
  private DownloadDistributionService downloadDistributionService;
  private AssetManager assetManager;
  private SecurityService securityService;
  private Organization org;
  private Snapshot snapshot;
  private Version version;

  // media packages
  private MediaPackage archivedMediapackage;
  private MediaPackage liveArchivedMediapackage;
  private MediaPackage searchMp;
  private MediaPackage liveSearchMp;
  private Properties caProps;

  @Before
  public void setUp() throws Exception {

    // media packages
    URI mpURI = LivePublicationServiceImplTest.class.getResource("/assetmanager-mp-live.xml").toURI();
    liveArchivedMediapackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
        .loadFromXml(mpURI.toURL().openStream());

    mpURI = LivePublicationServiceImplTest.class.getResource("/search-mp-live.xml").toURI();
    liveSearchMp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(mpURI.toURL()
        .openStream());

    archivedMediapackage = (MediaPackage) liveArchivedMediapackage.clone();
    archivedMediapackage.clearElements(MediaPackageElement.Type.Publication);

    searchMp = (MediaPackage) liveSearchMp.clone();
    searchMp.clearElements(MediaPackageElement.Type.Track);
    searchMp.add(TrackImpl.fromURI(new URI("https://opencast.org/test.mp4")));

    //mimeType = MimeTypes.parseMimeType(MIME_TYPE);

    // services
    Organization defOrg = new DefaultOrganization();
    Map<String, String> orgProps = new HashMap<>();
    orgProps.put(ArchiveUpdater.ENGAGE_URL_PROPERTY, ENGAGE_URL);
    org = new JaxbOrganization(ORG_ID, "Test Organization", defOrg.getServers(), defOrg.getAdminRole(),
        defOrg.getAnonymousRole(), orgProps);

    searchService = EasyMock.createNiceMock(SearchService.class);
    workspace = EasyMock.createNiceMock(Workspace.class);

    assetManager = EasyMock.createNiceMock(AssetManager.class);
    version = EasyMock.createNiceMock(Version.class);
    snapshot = EasyMock.createNiceMock(Snapshot.class);
    EasyMock.expect(snapshot.getOrganizationId()).andReturn(org.getId()).anyTimes();
    EasyMock.expect(snapshot.getVersion()).andReturn(version);
    EasyMock.expect(assetManager.getLatestSnapshot(EasyMock.anyString()))
        .andReturn(Optional.of(snapshot)).anyTimes();

    captureAgentService = EasyMock.createNiceMock(CaptureAgentStateService.class);
    caProps = new Properties();
    caProps.put(CA_PROPERTY_RESOLUTION_URL_PREFIX + "960x270", STREAMING_SERVER_URL
        + "/c3d913f6-9af7-403a-91a9-33b73ee18193-another-capture-agent-presenter-"
        + "delivery-stream-960x270_suffix_from_ca");
    caProps.put(CA_PROPERTY_RESOLUTION_URL_PREFIX + "1920x540", STREAMING_SERVER_URL
        + "/c3d913f6-9af7-403a-91a9-33b73ee18193-another-capture-agent-presenter-"
        + "delivery-stream-1920x540_suffix_from_ca");
    EasyMock.expect(captureAgentService.getAgentCapabilities(CA_WITH_PROPERTIES)).andReturn(caProps).anyTimes();

    downloadDistributionService = EasyMock.createNiceMock(DownloadDistributionService.class);
    EasyMock.expect(downloadDistributionService.getDistributionType())
        .andReturn(LivePublicationServiceImpl.DEFAULT_LIVE_DISTRIBUTION_SERVICE).anyTimes();

    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(null);

    // configuration
    bc = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bc.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER)).andReturn("system-user");
    Dictionary<String, Object> props = new Hashtable<>();
    props.put(LivePublicationServiceImpl.LIVE_STREAMING_URL, STREAMING_SERVER_URL);
    props.put(LivePublicationServiceImpl.LIVE_STREAM_MIME_TYPE, MIMETYPE);
    props.put(LivePublicationServiceImpl.LIVE_STREAM_NAME, STREAM_NAME);
    props.put(LivePublicationServiceImpl.LIVE_STREAM_RESOLUTION, RESOLUTION);
    props.put(LivePublicationServiceImpl.LIVE_TARGET_FLAVORS, FLAVORS);

    cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bc);
    EasyMock.expect(cc.getProperties()).andReturn(props);
  }

  private void replayAndActivate() {
    EasyMock.replay(bc, cc, searchService, captureAgentService, downloadDistributionService,
        workspace, assetManager, securityService, snapshot, version);

    // live service
    service = new LivePublicationServiceImpl();
    service.setSearchService(searchService);
    service.setCaptureAgentService(captureAgentService);
    service.setWorkspace(workspace);
    service.setAssetManager(assetManager);
    service.setSecurityService(securityService);
    service.getSnapshotVersionCache().put(MP_ID, CACHED_VERSION);
    service.setDownloadDistributionService(downloadDistributionService);
    service.activate(cc);
  }

  /**
   * Create
   */

  @Test
  public void testCreate() throws Exception {
    // capture
    Capture<MediaPackage> capturedSnapshotMp = Capture.newInstance();
    Capture<MediaPackage> capturedSearchMp = Capture.newInstance();
    Capture<Set<String>> capturedDistributedElements = Capture.newInstance();
    EasyMock.expect(downloadDistributionService.distributeSync(EasyMock.anyString(),
        EasyMock.anyObject(MediaPackage.class), EasyMock.capture(capturedDistributedElements), EasyMock.anyBoolean()))
        .andReturn(Stream.concat(Arrays.stream(liveSearchMp.getAttachments()),
            Arrays.stream(liveSearchMp.getCatalogs())).collect(Collectors.toList()));
    EasyMock.expect(assetManager.takeSnapshot(EasyMock.capture(capturedSnapshotMp))).andReturn(snapshot);
    searchService.addSynchronously(EasyMock.capture(capturedSearchMp));
    EasyMock.expectLastCall().atLeastOnce();
    Set<String> elementIdsToPublish = Arrays.stream(archivedMediapackage.getElements()).map(
        MediaPackageElement::getIdentifier).collect(Collectors.toSet());

    // replay
    replayAndActivate();

    Date startDate = format.parse("2023-12-03T10:15");
    Date endDate = format.parse("2023-12-03T13:15");
    service.createLiveEvent(archivedMediapackage, startDate, endDate, CAPTURE_AGENT_NAME);

    // check
    MediaPackage newArchivedMp = capturedSnapshotMp.getValue();
    MediaPackage newSearchMp = capturedSearchMp.getValue();

    // distribution
    Set<String> distributedElements = capturedDistributedElements.getValue();
    Assert.assertEquals(elementIdsToPublish, distributedElements);

    // search
    Assert.assertEquals(MP_ID, newSearchMp.getIdentifier().toString());
    Assert.assertEquals(0, newSearchMp.getPublications().length);

    Set<String> newSearchElementIds = Stream.concat(Arrays.stream(newSearchMp.getAttachments()),
        Arrays.stream(newSearchMp.getCatalogs())).map(MediaPackageElement::getIdentifier).collect(Collectors.toSet());
    Assert.assertEquals(distributedElements, newSearchElementIds);
    Assert.assertEquals(liveSearchMp, newSearchMp);

    //check live tracks
    Assert.assertEquals(4, newSearchMp.getTracks().length); // flavor * resolutions
    Set<String> trackResolutions = new HashSet<>();
    for (Track track: newSearchMp.getTracks()) {
      Assert.assertTrue(FLAVORS.contains(track.getFlavor().toString()));
      Assert.assertEquals((long) endDate.getTime() - startDate.getTime(), (long) track.getDuration());
      Assert.assertTrue(track.isLive());
      Assert.assertEquals(MIMETYPE, track.getMimeType().toString());
      Assert.assertEquals(1, track.getStreams().length);
      Assert.assertTrue(track.getStreams()[0] instanceof VideoStream);

      VideoStream stream = (VideoStream) track.getStreams()[0];
      String resolution = stream.getFrameWidth() + "x" + stream.getFrameHeight();
      trackResolutions.add(track.getFlavor().toString() + resolution);
      Assert.assertTrue((RESOLUTION.contains(resolution)));
      Assert.assertEquals(STREAMING_SERVER_URL + "/" + MP_ID + "-" + CAPTURE_AGENT_NAME + "-"
              + track.getFlavor().toString().replace("/", "-") + "-stream-" + resolution + "_suffix",
          track.getURI().toString());
    }
    Assert.assertEquals(4, trackResolutions.size()); // different resolutions

    // archive
    Assert.assertEquals(MP_ID, newArchivedMp.getIdentifier().toString());
    Assert.assertEquals(1, newArchivedMp.getPublications().length);
    Assert.assertEquals(LIVE_CHANNEL_ID, newArchivedMp.getPublications()[0].getChannel());
    Assert.assertArrayEquals(newSearchMp.getElements(), newArchivedMp.getPublications()[0].getElements());
    Assert.assertArrayEquals(archivedMediapackage.getElements(), newArchivedMp.getElements());
    Assert.assertEquals(version.toString(), service.getSnapshotVersionCache().getIfPresent(MP_ID));
    Assert.assertEquals(liveArchivedMediapackage, newArchivedMp);
  }

  /**
   * Update
   */

  @Test
  public void testUpdateLiveTracks() throws Exception {
    // setup
    EasyMock.expect(searchService.get(EasyMock.anyString())).andReturn(liveSearchMp);
    EasyMock.expect(snapshot.getMediaPackage()).andReturn(liveArchivedMediapackage).anyTimes();

    // capture
    Capture<MediaPackage> capturedSnapshotMp = Capture.newInstance();
    Capture<MediaPackage> capturedSearchMp = Capture.newInstance();
    EasyMock.expect(assetManager.takeSnapshot(EasyMock.capture(capturedSnapshotMp))).andReturn(snapshot);
    searchService.addSynchronously(EasyMock.capture(capturedSearchMp));
    EasyMock.expectLastCall().atLeastOnce();

    // replay
    replayAndActivate();

    // change dates & capture agent
    Date startDate = format.parse("2011-12-03T10:15");
    Date endDate = format.parse("2011-12-03T11:45");
    service.updateLiveTracks(MP_ID, startDate, endDate, NEW_CA);

    // check
    EasyMock.verify(assetManager, searchService);

    MediaPackage newSearchMp = capturedSearchMp.getValue();
    MediaPackage newArchivedMp = capturedSnapshotMp.getValue();

    // tracks are the same in search & archive publication
    Assert.assertArrayEquals(newSearchMp.getTracks(), newArchivedMp.getPublications()[0].getTracks());

    // check duration & ca agent in URL
    long duration = 90 * 60 * 1000;
    Assert.assertEquals(liveSearchMp.getTracks().length, newSearchMp.getTracks().length);
    for (Track track : newSearchMp.getTracks()) {
      Assert.assertEquals(duration, (long) track.getDuration());
      Assert.assertTrue(track.getURI().toString().contains(NEW_CA));
    }
  }


  @Test
  public void testUpdateLiveTracksFromCaProperties() throws Exception {
    // setup
    EasyMock.expect(searchService.get(EasyMock.anyString())).andReturn(liveSearchMp);
    EasyMock.expect(snapshot.getMediaPackage()).andReturn(liveArchivedMediapackage).anyTimes();

    // capture
    Capture<MediaPackage> capturedSnapshotMp = Capture.newInstance();
    Capture<MediaPackage> capturedSearchMp = Capture.newInstance();
    EasyMock.expect(assetManager.takeSnapshot(EasyMock.capture(capturedSnapshotMp))).andReturn(snapshot);
    searchService.addSynchronously(EasyMock.capture(capturedSearchMp));
    EasyMock.expectLastCall().atLeastOnce();

    // replay
    replayAndActivate();

    // change dates & capture agent
    Date startDate = format.parse("2011-12-03T10:15");
    Date endDate = format.parse("2011-12-03T11:45");
    service.updateLiveTracks(MP_ID, startDate, endDate, CA_WITH_PROPERTIES);

    // check
    EasyMock.verify(assetManager, searchService);

    MediaPackage newSearchMp = capturedSearchMp.getValue();
    MediaPackage newArchivedMp = capturedSnapshotMp.getValue();

    // tracks are the same in search & archive publication
    Assert.assertArrayEquals(newSearchMp.getTracks(), newArchivedMp.getPublications()[0].getTracks());

    //check live tracks
    Assert.assertEquals(2, newSearchMp.getTracks().length); // flavor * resolutions
    Set<String> trackResolutions = new HashSet<>();
    for (Track track: newSearchMp.getTracks()) {
      Assert.assertEquals(DEFAULT_LIVE_TARGET_FLAVOR, track.getFlavor().toString());

      VideoStream stream = (VideoStream) track.getStreams()[0];
      String resolution = stream.getFrameWidth() + "x" + stream.getFrameHeight();
      Assert.assertTrue(caProps.containsKey(CA_PROPERTY_RESOLUTION_URL_PREFIX + resolution));

      trackResolutions.add(resolution);
      Assert.assertTrue(RESOLUTION.contains(resolution));
      Assert.assertEquals(caProps.getProperty(CA_PROPERTY_RESOLUTION_URL_PREFIX + resolution),
          track.getURI().toString());
    }
    Assert.assertEquals(2, trackResolutions.size()); // different resolutions
  }

  @Test
  public void testUpdateFromSnapshot() throws Exception {
    // replace catalog
    MediaPackageElement oldCatalog = liveArchivedMediapackage.getCatalogs()[0];
    liveArchivedMediapackage.remove(oldCatalog);
    MediaPackageElement newCatalog = (MediaPackageElement) oldCatalog.clone();
    newCatalog.setIdentifier("new-catalog");
    liveArchivedMediapackage.add(newCatalog);
    MediaPackageElement oldSearchCatalog = liveSearchMp.getCatalogs()[0];

    // change content of ACL
    MediaPackageElement newAttachment = liveArchivedMediapackage.getAttachments()[0];
    MediaPackageElement oldAttachment = (MediaPackageElement)newAttachment.clone();
    newAttachment.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, "123456"));
    MediaPackageElement oldSearchAttachment = liveSearchMp.getAttachments()[0];

    EasyMock.expect(searchService.get(EasyMock.anyString())).andReturn(liveSearchMp);
    EasyMock.expect(snapshot.getMediaPackage()).andReturn(liveArchivedMediapackage).anyTimes();

    // capture
    Capture<MediaPackage> capturedSnapshotMp = Capture.newInstance();
    Capture<MediaPackage> capturedSearchMp = Capture.newInstance();
    Capture<Set<String>> capturedDistributedElements = Capture.newInstance();
    Capture<Set<String>> capturedRetractedElements = Capture.newInstance();

    EasyMock.expect(assetManager.takeSnapshot(EasyMock.capture(capturedSnapshotMp))).andReturn(snapshot);
    searchService.addSynchronously(EasyMock.capture(capturedSearchMp));
    EasyMock.expectLastCall().atLeastOnce();
    EasyMock.expect(downloadDistributionService.distributeSync(EasyMock.anyString(),
        EasyMock.anyObject(MediaPackage.class), EasyMock.capture(capturedDistributedElements),
        EasyMock.anyBoolean())).andReturn(List.of(newCatalog, newAttachment));
    EasyMock.expect(downloadDistributionService.retractSync(EasyMock.anyString(),
        EasyMock.anyObject(MediaPackage.class), EasyMock.capture(capturedRetractedElements)))
        .andReturn(List.of(oldSearchCatalog, oldSearchAttachment));

    // replay
    replayAndActivate();

    // run
    service.updateLiveEvent(liveArchivedMediapackage, "1");

    // check
    EasyMock.verify(downloadDistributionService, assetManager, searchService);

    MediaPackage newSearchMp = capturedSearchMp.getValue();
    MediaPackage newArchivedMp = capturedSnapshotMp.getValue();
    Set<String> distributedElements = capturedDistributedElements.getValue();
    Set<String> retractedElements = capturedRetractedElements.getValue();

    // check distribution
    Assert.assertEquals(2, distributedElements.size());
    Assert.assertEquals(2, retractedElements.size());
    Assert.assertEquals(distributedElements,Set.of(newCatalog.getIdentifier(), newAttachment.getIdentifier()));
    Assert.assertEquals(retractedElements,Set.of(oldCatalog.getIdentifier(), oldAttachment.getIdentifier()));

    // check search
    Assert.assertArrayEquals(newSearchMp.getTracks(), liveSearchMp.getTracks()); // tracks didn't change
    Assert.assertEquals(1, newSearchMp.getCatalogs().length);
    Assert.assertEquals(newSearchMp.getCatalogs()[0], newCatalog);
    Assert.assertEquals(1, newSearchMp.getAttachments().length);
    Assert.assertEquals(newSearchMp.getAttachments()[0], newAttachment);

    // check archive
    Assert.assertArrayEquals(newArchivedMp.getPublications()[0].getTracks(), liveSearchMp.getTracks());
    Assert.assertEquals(1, newArchivedMp.getPublications()[0].getCatalogs().length);
    Assert.assertEquals(newArchivedMp.getPublications()[0].getCatalogs()[0], newCatalog);
    Assert.assertEquals(1, newArchivedMp.getPublications()[0].getAttachments().length);
    Assert.assertEquals(newArchivedMp.getPublications()[0].getAttachments()[0], newAttachment);
  }

  @Test
  public void testUpdateFromUnchangedSnapshot() throws Exception {
    EasyMock.expect(searchService.get(EasyMock.anyString())).andReturn(liveSearchMp);

    // these methods should _not_ get called
    EasyMock.expect(assetManager.takeSnapshot(EasyMock.anyObject())).andThrow(new AssertionFailedError()).anyTimes();
    searchService.addSynchronously(EasyMock.anyObject());
    EasyMock.expectLastCall().andThrow(new AssertionFailedError()).anyTimes();
    EasyMock.expect(downloadDistributionService.distributeSync(EasyMock.anyString(),
            EasyMock.anyObject(MediaPackage.class), EasyMock.anyObject(Set.class), EasyMock.anyBoolean()))
        .andThrow(new AssertionFailedError()).anyTimes();
    EasyMock.expect(downloadDistributionService.retractSync(EasyMock.anyString(),
        EasyMock.anyObject(MediaPackage.class), EasyMock.anyObject(Set.class)))
        .andThrow(new AssertionFailedError()).anyTimes();

    // replay
    replayAndActivate();

    // run without changes
    service.updateLiveEvent(liveArchivedMediapackage, "1");

    // verify
    EasyMock.verify(downloadDistributionService, assetManager, searchService);
  }

  @Test
  public void testUpdateFromCachedSnapshot() throws Exception {
    // these methods should _not_ get called
    EasyMock.expect(assetManager.takeSnapshot(EasyMock.anyObject())).andThrow(new AssertionFailedError()).anyTimes();
    searchService.addSynchronously(EasyMock.anyObject());
    EasyMock.expectLastCall().andThrow(new AssertionFailedError()).anyTimes();
    EasyMock.expect(downloadDistributionService.distributeSync(EasyMock.anyString(),
            EasyMock.anyObject(MediaPackage.class), EasyMock.anyObject(Set.class), EasyMock.anyBoolean()))
        .andThrow(new AssertionFailedError()).anyTimes();
    EasyMock.expect(downloadDistributionService.retractSync(EasyMock.anyString(),
        EasyMock.anyObject(MediaPackage.class), EasyMock.anyObject(Set.class)))
        .andThrow(new AssertionFailedError()).anyTimes();

    // replay
    replayAndActivate();

    // try to update with cached snapshot version
    service.updateLiveEvent(liveArchivedMediapackage, CACHED_VERSION);

    // check
    EasyMock.verify(downloadDistributionService, assetManager, searchService);
  }

  @Test
  public void testUpdateNonLive() throws Exception {
    // setup
    EasyMock.expect(searchService.get(EasyMock.anyString())).andReturn(searchMp).once();

    Set<String> publishedElementIds = Stream.concat(
            Arrays.stream(liveArchivedMediapackage.getPublications()[0].getAttachments()),
            Arrays.stream(liveArchivedMediapackage.getPublications()[0].getCatalogs()))
        .map(MediaPackageElement::getIdentifier).collect(Collectors.toSet());

    // don't update search
    searchService.addSynchronously(EasyMock.anyObject());
    EasyMock.expectLastCall().andThrow(new AssertionFailedError()).anyTimes();

    // do remove leftovers from archive
    Capture<MediaPackage> capturedSnapshotMp = Capture.newInstance();
    Capture<Set<String>> capturedElementIds = Capture.newInstance();
    EasyMock.expect(assetManager.takeSnapshot(EasyMock.capture(capturedSnapshotMp))).andReturn(snapshot);
    EasyMock.expect(downloadDistributionService.retractSync(EasyMock.anyString(), EasyMock.anyObject(),
        EasyMock.capture(capturedElementIds))).andReturn(new ArrayList<>());

    // replay
    replayAndActivate();

    // run
    service.updateLiveEvent(liveArchivedMediapackage, "1");

    // check publication was removed from archive
    MediaPackage archivedMp = capturedSnapshotMp.getValue();
    Assert.assertEquals(MP_ID, archivedMp.getIdentifier().toString());
    Assert.assertEquals(0, archivedMp.getPublications().length);

    // check all published elements were retracted
    Assert.assertEquals(capturedElementIds.getValue(), publishedElementIds);

    // check
    EasyMock.verify(searchService, downloadDistributionService, assetManager);
  }

  /**
   * Delete
   */

  @Test
  public void testDelete() throws Exception {
    // setup
    EasyMock.expect(snapshot.getMediaPackage()).andReturn(liveArchivedMediapackage).anyTimes();
    EasyMock.expect(searchService.get(EasyMock.anyString())).andReturn(liveSearchMp);
    Set<String> publishedElementIds = Stream.concat(Arrays.stream(liveSearchMp.getAttachments()),
        Arrays.stream(liveSearchMp.getCatalogs())).map(MediaPackageElement::getIdentifier).collect(Collectors.toSet());

    // capture - we expect all of these to be called
    Capture<MediaPackage> capturedSnapshotMp = Capture.newInstance();
    Capture<Set<String>> capturedElementIds = Capture.newInstance();
    EasyMock.expect(assetManager.takeSnapshot(EasyMock.capture(capturedSnapshotMp))).andReturn(snapshot);
    EasyMock.expect(downloadDistributionService.retractSync(EasyMock.anyString(), EasyMock.anyObject(),
        EasyMock.capture(capturedElementIds))).andReturn(new ArrayList<>());
    EasyMock.expect(searchService.deleteSynchronously(liveSearchMp.getIdentifier().toString())).andReturn(true);

    // replay & activate
    replayAndActivate();

    // run
    service.deleteLiveEvent(MP_ID, true);

    // check
    EasyMock.verify(searchService, downloadDistributionService, assetManager);

    // check publication was removed from archive
    MediaPackage archivedMp = capturedSnapshotMp.getValue();
    Assert.assertEquals(MP_ID, archivedMp.getIdentifier().toString());
    Assert.assertEquals(0, archivedMp.getPublications().length);
    Assert.assertEquals(version.toString(), service.getSnapshotVersionCache().getIfPresent(MP_ID));

    // check all published elements were retracted
    Assert.assertEquals(publishedElementIds, capturedElementIds.getValue());
  }

  @Test
  public void testDeleteWithoutArchive() throws Exception {
    // setup
    EasyMock.expect(searchService.get(EasyMock.anyString())).andReturn(liveSearchMp).once();

    // retract search & distribution
    EasyMock.expect(downloadDistributionService.retractSync(EasyMock.anyString(), EasyMock.anyObject(),
        EasyMock.anyObject(Set.class))).andReturn(new ArrayList<>());
    EasyMock.expect(searchService.deleteSynchronously(liveSearchMp.getIdentifier().toString())).andReturn(true);

    // don't update archive
    EasyMock.expect(assetManager.takeSnapshot(EasyMock.anyObject())).andThrow(new AssertionFailedError()).anyTimes();

    // replay
    replayAndActivate();

    // run
    service.deleteLiveEvent(MP_ID, false);

    // check
    EasyMock.verify(searchService, downloadDistributionService, assetManager);
  }

  @Test
  public void testDeleteNonLive() throws Exception {
    // setup
    EasyMock.expect(searchService.get(EasyMock.anyString())).andReturn(searchMp).once();
    EasyMock.expect(snapshot.getMediaPackage()).andReturn(liveArchivedMediapackage).anyTimes();
    Set<String> publishedElementIds = Stream.concat(
        Arrays.stream(liveArchivedMediapackage.getPublications()[0].getAttachments()),
        Arrays.stream(liveArchivedMediapackage.getPublications()[0].getCatalogs()))
        .map(MediaPackageElement::getIdentifier).collect(Collectors.toSet());

    // don't remove from search
    EasyMock.expect(searchService.deleteSynchronously(searchMp.getIdentifier().toString()))
        .andThrow(new AssertionFailedError()).anyTimes();

    // do remove leftovers from archive
    Capture<MediaPackage> capturedSnapshotMp = Capture.newInstance();
    Capture<Set<String>> capturedElementIds = Capture.newInstance();
    EasyMock.expect(assetManager.takeSnapshot(EasyMock.capture(capturedSnapshotMp))).andReturn(snapshot);
    EasyMock.expect(downloadDistributionService.retractSync(EasyMock.anyString(), EasyMock.anyObject(),
        EasyMock.capture(capturedElementIds))).andReturn(new ArrayList<>());

    // replay
    replayAndActivate();

    // run
    service.deleteLiveEvent(MP_ID, true);

    // check publication was removed from archive
    MediaPackage archivedMp = capturedSnapshotMp.getValue();
    Assert.assertEquals(MP_ID, archivedMp.getIdentifier().toString());
    Assert.assertEquals(0, archivedMp.getPublications().length);

    // check all published elements were retracted
    Assert.assertEquals(capturedElementIds.getValue(), publishedElementIds);

    // check
    EasyMock.verify(searchService, downloadDistributionService, assetManager);
  }
}
