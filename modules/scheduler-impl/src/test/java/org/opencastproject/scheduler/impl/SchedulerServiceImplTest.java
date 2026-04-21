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

package org.opencastproject.scheduler.impl;

import static net.fortuna.ical4j.model.Component.VEVENT;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newDBSession;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_AVAILABLE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CONTRIBUTOR;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATED;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_CREATOR;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_DESCRIPTION;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_EXTENT;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IS_REPLACED_BY;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LANGUAGE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_LICENSE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_PUBLISHER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_REPLACES;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_RIGHTS_HOLDER;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SPATIAL;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_SUBJECT;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TEMPORAL;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TITLE;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_TYPE;
import static org.opencastproject.scheduler.api.RecordingState.CAPTURING;
import static org.opencastproject.scheduler.api.RecordingState.UPLOADING;
import static org.opencastproject.scheduler.api.RecordingState.UPLOAD_FINISHED;
import static org.opencastproject.util.EqualsUtil.eqObj;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.storage.AssetStore;
import org.opencastproject.assetmanager.api.storage.AssetStoreException;
import org.opencastproject.assetmanager.api.storage.DeletionSelector;
import org.opencastproject.assetmanager.api.storage.Source;
import org.opencastproject.assetmanager.api.storage.StoragePath;
import org.opencastproject.assetmanager.impl.AssetManagerImpl;
import org.opencastproject.assetmanager.impl.HttpAssetProvider;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.authorization.xacml.XACMLUtils;
import org.opencastproject.db.DBSession;
import org.opencastproject.elasticsearch.api.SearchResult;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.EventSearchQuery;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.message.broker.api.update.AssetManagerUpdateHandler;
import org.opencastproject.message.broker.api.update.SchedulerUpdateHandler;
import org.opencastproject.metadata.dublincore.CatalogUIAdapter;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.Recording;
import org.opencastproject.scheduler.api.SchedulerConflictException;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.scheduler.api.Util;
import org.opencastproject.scheduler.endpoint.SchedulerRestService;
import org.opencastproject.scheduler.impl.persistence.SchedulerServiceDatabaseImpl;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.XmlNamespaceContext;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public class SchedulerServiceImplTest {

  public static final File baseDir = new File(new File(IoSupport.getSystemTmpDir()), "schedulerservicetest");
  public static final File archiveDir = new File(baseDir, "archive");

  private SeriesService seriesService;
  private static UnitTestWorkspace workspace;
  private AssetManager assetManager;
  private static OrganizationDirectoryService orgDirectoryService;
  private SecurityService securityService;
  private static SchedulerUpdateHandler handler;

  private User currentUser = new JaxbUser("admin", "provider", new DefaultOrganization(),
      new JaxbRole("admin", new DefaultOrganization(), "test"));
  private Organization currentOrg = new DefaultOrganization();

  private static SchedulerServiceImpl schedSvc;
  private static ElasticsearchIndex index;

  // persistent properties
  private static SchedulerServiceDatabaseImpl schedulerDatabase;

  private static AccessControlList acl;

  private static Map<String, String> wfProperties = new HashMap<>();
  private static Map<String, String> wfPropertiesUpdated = new HashMap<>();


  @BeforeClass
  public static void beforeClass() throws Exception {
    wfProperties.put("test", "true");
    wfProperties.put("clear", "all");

    wfPropertiesUpdated.put("test", "false");
    wfPropertiesUpdated.put("skip", "true");

    workspace = new UnitTestWorkspace();

    AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    acl = new AccessControlList(new AccessControlEntry("ROLE_ADMIN", "write", true),
            new AccessControlEntry("ROLE_ADMIN", "read", true), new AccessControlEntry("ROLE_USER", "read", true));
    EasyMock.expect(
            authorizationService.getActiveAcl(EasyMock.anyObject(MediaPackage.class)))
            .andReturn(tuple(acl, AclScope.Episode)).anyTimes();


    orgDirectoryService = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDirectoryService.getOrganizations())
            .andReturn(Arrays.asList((Organization) new DefaultOrganization())).anyTimes();

    EventCatalogUIAdapter episodeAdapter = EasyMock.createMock(EventCatalogUIAdapter.class);
    EasyMock.expect(episodeAdapter.getFlavor()).andReturn(new MediaPackageElementFlavor("dublincore", "episode"))
            .anyTimes();
    EasyMock.expect(episodeAdapter.getOrganization()).andReturn(new DefaultOrganization().getId()).anyTimes();
    final Capture<String> stringCapture = EasyMock.newCapture();
    EasyMock.expect(episodeAdapter.handlesOrganization(EasyMock.capture(stringCapture)))
        .andAnswer(() -> DefaultOrganization.DEFAULT_ORGANIZATION_ID.equals(stringCapture.getValue())).anyTimes();

    EventCatalogUIAdapter extendedAdapter = EasyMock.createMock(EventCatalogUIAdapter.class);
    EasyMock.expect(extendedAdapter.getFlavor()).andReturn(new MediaPackageElementFlavor("extended", "episode"))
            .anyTimes();
    EasyMock.expect(extendedAdapter.getOrganization()).andReturn(new DefaultOrganization().getId()).anyTimes();
    EasyMock.expect(extendedAdapter.handlesOrganization(EasyMock.capture(stringCapture)))
        .andAnswer(() -> DefaultOrganization.DEFAULT_ORGANIZATION_ID.equals(stringCapture.getValue())).anyTimes();

    BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(EasyMock.anyString())).andReturn("adminuser").anyTimes();

    ComponentContext componentContext = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(componentContext.getBundleContext()).andReturn(bundleContext).anyTimes();

    SearchResult result = EasyMock.createNiceMock(SearchResult.class);

    index = EasyMock.createNiceMock(ElasticsearchIndex.class);
    EasyMock.expect(index.getIndexName()).andReturn("index").anyTimes();
    EasyMock.expect(index.getByQuery(EasyMock.anyObject(EventSearchQuery.class))).andReturn(result).anyTimes();

    handler = EasyMock.createNiceMock(SchedulerUpdateHandler.class);

    EasyMock.replay(authorizationService, index, result, handler,
            extendedAdapter, episodeAdapter, orgDirectoryService, componentContext, bundleContext);

    schedSvc = new SchedulerServiceImpl();

    schedSvc.setAuthorizationService(authorizationService);
    schedSvc.setWorkspace(workspace);
    schedSvc.addCatalogUIAdapter(episodeAdapter);
    schedSvc.addCatalogUIAdapter(extendedAdapter);
    schedSvc.setOrgDirectoryService(orgDirectoryService);
    schedSvc.setIndex(index);

    schedSvc.activate(componentContext);
  }

  @Before
  public void setUp() throws Exception {


    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(currentUser).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(currentOrg).anyTimes();
    EasyMock.replay(securityService);
    schedSvc.setSecurityService(securityService);

    String seriesIdentifier = Long.toString(System.currentTimeMillis());
    DublinCoreCatalog seriesCatalog = getSampleSeriesDublinCoreCatalog(seriesIdentifier);
    List<DublinCoreCatalog> seriesCatalogs = new ArrayList<>();
    seriesCatalogs.add(seriesCatalog);

    seriesService = EasyMock.createMock(SeriesService.class);
    EasyMock.expect(seriesService.getSeries(EasyMock.anyString())).andReturn(seriesCatalog).anyTimes();
    EasyMock.replay(seriesService);
    schedSvc.setSeriesService(seriesService);


    schedulerDatabase = new SchedulerServiceDatabaseImpl();
    EntityManagerFactory emf = newEntityManagerFactory(SchedulerServiceDatabaseImpl.PERSISTENCE_UNIT);
    schedulerDatabase.setEntityManagerFactory(emf);
    schedulerDatabase.setDBSessionFactory(getDbSessionFactory());
    schedulerDatabase.setSecurityService(securityService);
    schedulerDatabase.activate(null);
    schedSvc.setPersistence(schedulerDatabase);

    assetManager = mkAssetManager();
    schedSvc.setAssetManager(assetManager);

    schedSvc.addSchedulerUpdateHandler(handler);

    schedSvc.lastModifiedCache.invalidateAll();
  }

  @After
  public void tearDown() throws Exception {
    workspace.clean();
    schedulerDatabase = null;
    schedSvc.removeSchedulerUpdateHandler(handler);
  }

  @AfterClass
  public static void afterClass() throws Exception {
    schedSvc = null;
    FileSupport.deleteQuietly(baseDir, true);
  }

  protected static DublinCoreCatalog getSampleSeriesDublinCoreCatalog(String seriesID) {
    DublinCoreCatalog dc = DublinCores.mkOpencastSeries().getCatalog();
    dc.set(PROPERTY_IDENTIFIER, seriesID);
    dc.set(PROPERTY_TITLE, "Demo series");
    dc.set(PROPERTY_LICENSE, "demo");
    dc.set(PROPERTY_PUBLISHER, "demo");
    dc.set(PROPERTY_CREATOR, "demo");
    dc.set(PROPERTY_SUBJECT, "demo");
    dc.set(PROPERTY_SPATIAL, "demo");
    dc.set(PROPERTY_RIGHTS_HOLDER, "demo");
    dc.set(PROPERTY_EXTENT, "3600000");
    dc.set(PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(new Date(), Precision.Minute));
    dc.set(PROPERTY_LANGUAGE, "demo");
    dc.set(PROPERTY_IS_REPLACED_BY, "demo");
    dc.set(PROPERTY_TYPE, "demo");
    dc.set(PROPERTY_AVAILABLE, EncodingSchemeUtils.encodeDate(new Date(), Precision.Minute));
    dc.set(PROPERTY_REPLACES, "demo");
    dc.set(PROPERTY_CONTRIBUTOR, "demo");
    dc.set(PROPERTY_DESCRIPTION, "demo");
    return dc;
  }

  protected static MediaPackage generateEvent(Optional<String> id) throws MediaPackageException {
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    if (id.isPresent()) {
      mp.setIdentifier(new IdImpl(id.get()));
    }
    return mp;
  }

  protected static DublinCoreCatalog generateEvent(String captureDeviceID, Optional<String> eventId,
          Optional<String> title, Date startTime, Date endTime) {
    DublinCoreCatalog dc = DublinCores.mkOpencastEpisode().getCatalog();
    dc.set(PROPERTY_IDENTIFIER, eventId.orElse("1"));
    dc.set(PROPERTY_TITLE, title.orElse("Demo event"));
    dc.set(PROPERTY_CREATOR, "demo");
    dc.set(PROPERTY_SUBJECT, "demo");
    dc.set(PROPERTY_TEMPORAL, EncodingSchemeUtils.encodePeriod(new DCMIPeriod(startTime, endTime), Precision.Second));
    dc.set(PROPERTY_SPATIAL, captureDeviceID);
    dc.set(PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(new Date(), Precision.Minute));
    dc.set(PROPERTY_LANGUAGE, "demo");
    dc.set(PROPERTY_CONTRIBUTOR, "demo");
    dc.set(PROPERTY_DESCRIPTION, "demo");
    return dc;
  }

  protected static DublinCoreCatalog generateExtendedEvent(Optional<String> eventId, String flavorType) {
    DublinCoreCatalog dc = DublinCores.mkStandard();
    final Map<String, String> prefixToUri = new HashMap<>();
    prefixToUri.put("", "http://test.com/video/opencast");
    prefixToUri.put("extended", "http://test.com/video/metadata");
    dc.addBindings(new XmlNamespaceContext(prefixToUri));
    dc.setRootTag(new EName("http://test.com/video/opencast", "extended"));
    dc.setFlavor(new MediaPackageElementFlavor(flavorType, "episode"));
    dc.set(PROPERTY_IDENTIFIER, eventId.orElse("1"));
    dc.set(PROPERTY_EXTENT, "demo");
    return dc;
  }

  protected static DublinCoreCatalog generateEvent(String captureDeviceID, Date startTime, Date endTime) {
    return generateEvent(captureDeviceID, Optional.empty(), Optional.empty(), startTime, endTime);
  }

  protected static Map<String, String> generateCaptureAgentMetadata(String captureDeviceID) {
    Map<String, String> properties = new HashMap<>();
    properties.put("event.test", "Demo event");
    properties.put("capture.device.id", captureDeviceID);
    return properties;
  }

  @Test
  public void testPersistence() throws Exception {
    Date start = new Date();
    Date end = new Date(System.currentTimeMillis() + 60000);
    String captureDeviceID = "demo";
    String seriesId = "series1";
    Set<String> userIds = new HashSet<>();
    userIds.add("user1");
    userIds.add("user2");
    MediaPackage mp = generateEvent(Optional.empty());
    mp.setSeries(seriesId);
    DublinCoreCatalog event = generateEvent(captureDeviceID, start, end);
    String catalogId = addDublinCore(Optional.empty(), mp, event);
    Map<String, String> caProperties = generateCaptureAgentMetadata("demo");
    EasyMock.reset(seriesService);
    EasyMock.expect(seriesService.getSeries(seriesId)).andThrow(new NotFoundException()).once();
    EasyMock.replay(seriesService);

    assertEquals("mod0", schedSvc.getScheduleLastModified(captureDeviceID));

    // Store event
    schedSvc.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Optional.empty());
    try {
      MediaPackage mp2 = (MediaPackage) mp.clone();
      mp2.setIdentifier(IdImpl.fromUUID());
      schedSvc.addEvent(start, end, captureDeviceID, userIds, mp2, wfProperties, caProperties, Optional.empty());
      Assert.fail();
    } catch (SchedulerConflictException e) {
      Assert.assertNotNull(e);
    }
    MediaPackage mediaPackage = schedSvc.getMediaPackage(mp.getIdentifier().toString());
    assertEquals(seriesId, mediaPackage.getSeries());
    DublinCoreCatalog eventLoaded = schedSvc.getDublinCore(mp.getIdentifier().toString());
    assertEquals(event.getFirst(PROPERTY_TITLE), eventLoaded.getFirst(PROPERTY_TITLE));
    TechnicalMetadata technicalMetadata = schedSvc.getTechnicalMetadata(mp.getIdentifier().toString());
    assertEquals(mp.getIdentifier().toString(), technicalMetadata.getEventId());
    assertEquals(captureDeviceID, technicalMetadata.getAgentId());
    assertEquals(start, technicalMetadata.getStartDate());
    assertEquals(end, technicalMetadata.getEndDate());
    assertEquals(userIds, technicalMetadata.getPresenters());
    assertTrue(technicalMetadata.getRecording().isEmpty());
    assertTrue(technicalMetadata.getCaptureAgentConfiguration().size() >= caProperties.size());

    assertEquals(wfProperties, new HashMap<>(schedSvc.getWorkflowConfig(mp.getIdentifier().toString())));
    String lastModified = schedSvc.getScheduleLastModified(captureDeviceID);
    assertNotEquals("mod0", lastModified);

    eventLoaded.set(PROPERTY_TITLE, "Something more");
    addDublinCore(Optional.of(catalogId), mp, eventLoaded);

    userIds.add("user3");
    userIds.remove("user1");
    mp.setSeries("series2");

    // Update event
    schedSvc.updateEvent(mp.getIdentifier().toString(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(userIds), Optional.of(mp), Optional.of(wfProperties), Optional.of(caProperties));

    mediaPackage = schedSvc.getMediaPackage(mp.getIdentifier().toString());
    assertEquals("series2", mediaPackage.getSeries());
    DublinCoreCatalog eventReloaded = schedSvc.getDublinCore(mp.getIdentifier().toString());
    assertEquals("Something more", eventReloaded.getFirst(PROPERTY_TITLE));
    technicalMetadata = schedSvc.getTechnicalMetadata(mp.getIdentifier().toString());
    assertEquals(mp.getIdentifier().toString(), technicalMetadata.getEventId());
    assertEquals(captureDeviceID, technicalMetadata.getAgentId());
    assertEquals(start, technicalMetadata.getStartDate());
    assertEquals(end, technicalMetadata.getEndDate());
    assertEquals(userIds, technicalMetadata.getPresenters());
    assertTrue(technicalMetadata.getRecording().isEmpty());
    assertTrue(technicalMetadata.getCaptureAgentConfiguration().size() >= caProperties.size());
    String updatedLastModified = schedSvc.getScheduleLastModified(captureDeviceID);
    assertNotEquals("mod0", updatedLastModified);
    assertNotEquals(lastModified, updatedLastModified);

    assertTrue(schedSvc.getCaptureAgentConfiguration(mp.getIdentifier().toString()).size() >= caProperties.size());
  }

  @Test
  public void testEndDateBeforeStartDate() throws Exception {
    Date start = new Date();
    Date end = new Date(System.currentTimeMillis() + 60000);
    String captureDeviceID = "demo";
    String seriesId = "series1";
    Set<String> userIds = new HashSet<>();
    userIds.add("user1");
    userIds.add("user2");
    MediaPackage mp = generateEvent(Optional.empty());
    mp.setSeries(seriesId);
    DublinCoreCatalog event = generateEvent(captureDeviceID, start, end);
    addDublinCore(Optional.empty(), mp, event);
    Map<String, String> caProperties = generateCaptureAgentMetadata("demo");
    EasyMock.reset(seriesService);
    EasyMock.expect(seriesService.getSeries(seriesId)).andThrow(new NotFoundException()).once();
    EasyMock.replay(seriesService);

    try {
      // Store event
      schedSvc.addEvent(end, start, captureDeviceID, userIds, mp, wfProperties, caProperties, Optional.empty());
      fail("Unable to detect end date being before start date during creation of event");
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }

    // Store
    schedSvc.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Optional.empty());

    try {
      // Update end date before start date
      schedSvc.updateEvent(mp.getIdentifier().toString(), Optional.of(end), Optional.of(start), Optional.empty(),
              Optional.empty(), Optional.empty(), Optional.empty(),
              Optional.empty());
      fail("Unable to detect end date being before start date during update of event");
    } catch (SchedulerException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void nonExistantRecording() throws Exception {
    String mpId = "doesNotExist";
    try {
      schedSvc.getRecordingState(mpId);
      fail("Non existing recording has been found");
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void badRecordingData() throws NotFoundException, SchedulerException {
    try {
      schedSvc.updateRecordingState(null, CAPTURING);
      fail();
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }
    assertEquals(0, schedSvc.getKnownRecordings().size());

    try {
      schedSvc.updateRecordingState("", "idle");
      fail();
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }
    assertEquals(0, schedSvc.getKnownRecordings().size());

    assertFalse(schedSvc.updateRecordingState("something", "bad_state"));
    assertEquals(0, schedSvc.getKnownRecordings().size());
  }

  @Test
  public void noRecordings() throws SchedulerException {
    assertEquals(0, schedSvc.getKnownRecordings().size());
  }

  @Test
  public void oneRecording() throws Exception {
    long currentTime = System.currentTimeMillis();
    String id = "Recording1";
    MediaPackage mediaPackage = generateEvent(Optional.of(id));
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "Device A",
            Collections.<String> emptySet(), mediaPackage, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());

    schedSvc.updateRecordingState(id, UPLOAD_FINISHED);
    assertEquals(1, schedSvc.getKnownRecordings().size());

    verifyRecording("notRecording1", null);
    verifyRecording(id, UPLOAD_FINISHED);

    schedSvc.updateRecordingState(id, CAPTURING);
    assertEquals(1, schedSvc.getKnownRecordings().size());

    verifyRecording("notRecording1", null);
    verifyRecording(id, CAPTURING);
  }

  @Test
  public void testCalendarNotModified() throws Exception {
    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.replay(request);

    SchedulerRestService restService = new SchedulerRestService();
    restService.setService(schedSvc);

    String device = "Test Device";

    // Store an event
    MediaPackage mediaPackage = generateEvent(Optional.empty());
    DublinCoreCatalog dublinCore = generateEvent(device, new Date(), new Date(System.currentTimeMillis() + 60000));
    addDublinCore(Optional.empty(), mediaPackage, dublinCore);
    schedSvc.addEvent(new Date(), new Date(System.currentTimeMillis() + 60000), device, Collections.<String> emptySet(),
            mediaPackage, Collections.<String, String> emptyMap(), Collections.<String, String> emptyMap(),
            Optional.empty());

    // Request the calendar without specifying an etag. We should get a 200 with the iCalendar in the response body
    Response response = restService.getCalendar(device, null, null, request);
    Assert.assertNotNull(response.getEntity());
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    final String etag = (String) response.getMetadata().getFirst(HttpHeaders.ETAG);

    EasyMock.reset(request);
    EasyMock.expect(request.getHeader("If-None-Match")).andAnswer(new IAnswer<String>() {
      @Override
      public String answer() throws Throwable {
        return etag;
      }
    }).anyTimes();
    EasyMock.replay(request);

    // Request using the etag from the first response. We should get a 304 (not modified)
    response = restService.getCalendar(device, null, null, request);
    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    Assert.assertNull(response.getEntity());

    // Update the event and clear to cache to make sure it's reloaded
    schedSvc.updateEvent(mediaPackage.getIdentifier().toString(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(wfPropertiesUpdated),
            Optional.empty());

    // Try using the same old etag. We should get a 200, since the event has changed
    response = restService.getCalendar(device, null, null, request);
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    Assert.assertNotNull(response.getEntity());
  }

  @Test
  public void testCalendarCutoff() throws Exception {
    long currentTime = System.currentTimeMillis();
    MediaPackage mediaPackageA = generateEvent(Optional.empty());
    MediaPackage mediaPackageB = generateEvent(Optional.empty());

    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + (60 * 60 * 1000)), "Device A",
            Collections.<String> emptySet(), mediaPackageA, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.addEvent(new Date(currentTime + (20 * 24 * 60 * 60 * 1000)),
            new Date(currentTime + (20 * 25 * 60 * 60 * 1000)), "Device A", Collections.<String> emptySet(),
            mediaPackageB, wfProperties, Collections.<String, String> emptyMap(), Optional.empty());

    Date start = new Date(currentTime);
    Date end = new Date(currentTime + 60 * 60 * 1000);

    List<MediaPackage> events = schedSvc.search(Optional.of("Device A"), Optional.empty(), Optional.of(end),
            Optional.of(start), Optional.empty());
    assertEquals(1, events.size());
  }

  /**
   * Create an event with a start date 1 minute in the past and an end date 60 minutes in to the future. Make sure the
   * event is listed when asking for the schedule of the capture agent.
   */
  @Test
  public void testCalendarCutoffWithStartedEvent() throws Exception {
    long currentTime = System.currentTimeMillis();
    Date startDate = new Date(currentTime - 10 * 1000);
    Date endDate = new Date(currentTime + (60 * 60 * 1000));
    MediaPackage mediaPackage = generateEvent(Optional.empty());
    String captureAgentId = "Device A";
    schedSvc.addEvent(startDate, endDate, captureAgentId, Collections.<String> emptySet(), mediaPackage, wfProperties,
            Collections.<String, String> emptyMap(), Optional.empty());

    Date start = new Date(currentTime);
    Date end = new Date(currentTime + 60 * 60 * 1000);

    List<MediaPackage> events = schedSvc.search(Optional.of(captureAgentId), Optional.empty(), Optional.of(end),
            Optional.of(start), Optional.empty());
    assertEquals(1, events.size());
  }

  @Test
  public void testSpatial() throws Exception {
    long currentTime = System.currentTimeMillis();
    MediaPackage mediaPackageA = generateEvent(Optional.empty());
    MediaPackage mediaPackageB = generateEvent(Optional.empty());
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + (60 * 60 * 1000)), "Device A",
            Collections.<String> emptySet(), mediaPackageA, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + (60 * 60 * 1000)), "Device B",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());

    List<MediaPackage> events = schedSvc.search(Optional.of("Device"), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());
    assertEquals(0, events.size());

    events = schedSvc.search(Optional.of("Device A"), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    assertEquals(1, events.size());

    events = schedSvc.search(Optional.of("Device B"), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    assertEquals(1, events.size());

    events = schedSvc.search(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    assertEquals(2, events.size());
  }

  @Test(expected = SchedulerException.class)
  public void testConflictCreation() throws Exception {
    long currentTime = System.currentTimeMillis();
    MediaPackage mediaPackage = generateEvent(Optional.of("1"));

    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + (60 * 60 * 1000)), "Device A",
            Collections.<String> emptySet(), mediaPackage, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.addEvent(new Date(currentTime + (20 * 24 * 60 * 60 * 1000)),
            new Date(currentTime + (20 * 25 * 60 * 60 * 1000)), "Device A", Collections.<String> emptySet(),
            mediaPackage, wfProperties, Collections.<String, String> emptyMap(), Optional.empty());
  }

  @Test
  public void testAddMultipleEventsEmptyRange() throws Exception {
    final RRule rrule = new RRule("FREQ=WEEKLY;BYDAY=WE;BYHOUR=7;BYMINUTE=0");
    final Date start = new Date(1546844400000L); // 2019-01-07T07:00:00Z
    final Date end = start;
    final Long duration = 6900000L;
    final TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
    final String captureAgentId = "Device A";
    final Set<String> userIds = Collections.emptySet();
    final String id = "Recording1";
    final String seriesId = "TestSeries";
    final MediaPackage mpTemplate = generateEvent(Optional.of(id));
    mpTemplate.setSeries(seriesId);
    final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId,
        Optional.of(mpTemplate.getIdentifier().toString()), Optional.of("Test Title"), start, end);
    addDublinCore(Optional.of(mpTemplate.getIdentifier().toString()), mpTemplate, dublinCoreCatalog);
    final Map<String, String> wfProperties = this.wfProperties;
    final Map<String, String> caProperties = Collections.singletonMap("foo", "bar");
    final Optional<String> schedulingSource = Optional.empty();
    final Map<String, Period> scheduled = schedSvc.addMultipleEvents(
        rrule,
        start,
        end,
        duration,
        tz,
        captureAgentId,
        userIds,
        mpTemplate,
        wfProperties,
        caProperties,
        schedulingSource
    );
    assertTrue(scheduled.isEmpty());
  }

  @Test
  public void testAddMultipleEvents() throws Exception {
    final RRule rrule = new RRule("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=7;BYMINUTE=0");
    final Date start = new Date(1546844400000L); // 2019-01-07T07:00:00Z
    final Date end = new Date(1570953300000L); // 2019-10-13T07:55:00Z
    final Long duration = 6900000L;
    final TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
    final String captureAgentId = "Device A";
    final Set<String> userIds = Collections.emptySet();
    final String id = "Recording1";
    final String seriesId = "TestSeries";
    final MediaPackage mpTemplate = generateEvent(Optional.of(id));
    mpTemplate.setSeries(seriesId);
    final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId,
        Optional.of(mpTemplate.getIdentifier().toString()), Optional.of("Test Title"), start, end);
    addDublinCore(Optional.of(mpTemplate.getIdentifier().toString()), mpTemplate, dublinCoreCatalog);
    final Map<String, String> wfProperties = this.wfProperties;
    final Map<String, String> caProperties = Collections.singletonMap("foo", "bar");
    final Optional<String> schedulingSource = Optional.empty();
    assertEquals("mod0", schedSvc.getScheduleLastModified(captureAgentId));
    final Map<String, Period> scheduled = schedSvc.addMultipleEvents(
        rrule,
        start,
        end,
        duration,
        tz,
        captureAgentId,
        userIds,
        mpTemplate,
        wfProperties,
        caProperties,
        schedulingSource
    );

    final int expectedEventCount = rrule.getRecur().getDates(
        new net.fortuna.ical4j.model.Date(start),
        new net.fortuna.ical4j.model.Date(end),
        Value.DATE
    ).size();
    assertEquals(expectedEventCount, scheduled.keySet().size());
    final String randomMpId = scheduled.keySet().stream().findAny()
        .orElseThrow(() -> new RuntimeException("This should never happen"));
    final Period period = scheduled.get(randomMpId);
    final MediaPackage mediaPackage = schedSvc.getMediaPackage(randomMpId);
    final DublinCoreCatalog eventLoaded = schedSvc.getDublinCore(randomMpId);
    final TechnicalMetadata technicalMetadata = schedSvc.getTechnicalMetadata(randomMpId);
    assertEquals(seriesId, mediaPackage.getSeries());
    assertTrue(eventLoaded.getFirst(PROPERTY_TITLE).startsWith(dublinCoreCatalog.getFirst(PROPERTY_TITLE)));
    assertEquals(randomMpId, technicalMetadata.getEventId());
    assertEquals(captureAgentId, technicalMetadata.getAgentId());
    assertEquals(new Date(period.getStart().getTime()), technicalMetadata.getStartDate());
    assertEquals(new Date(period.getEnd().getTime()), technicalMetadata.getEndDate());
    assertEquals(userIds, technicalMetadata.getPresenters());
    assertTrue(technicalMetadata.getRecording().isEmpty());
    assertTrue(technicalMetadata.getCaptureAgentConfiguration().size() >= caProperties.size());
    assertEquals(wfProperties, new HashMap<>(schedSvc.getWorkflowConfig(randomMpId)));
    String lastModified = schedSvc.getScheduleLastModified(captureAgentId);
    assertNotEquals("mod0", lastModified);
    assertTrue(schedSvc.getCaptureAgentConfiguration(randomMpId).size() >= caProperties.size());
  }

  @Test(expected = SchedulerConflictException.class)
  public void testAddMultipleEventsConflict() throws Exception {
    for (int i = 0; i < 2; i++) {
      final RRule rrule = new RRule("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR;BYHOUR=7;BYMINUTE=30");
      final Date start = new Date(1546844400000L); // 2019-01-07T07:00:00Z
      final Date end = new Date(1570953300000L); // 2019-10-13T07:55:00Z
      final Long duration = 6900000L;
      final TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
      final String captureAgentId = "Device A";
      final Set<String> userIds = Collections.emptySet();
      final String id = "Recording" + i;
      final MediaPackage mpTemplate = generateEvent(Optional.of(id));
      final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId,
          Optional.of(mpTemplate.getIdentifier().toString()), Optional.of("Test Title"), start, end);
      addDublinCore(Optional.of(mpTemplate.getIdentifier().toString()), mpTemplate, dublinCoreCatalog);
      final Map<String, String> wfProperties = this.wfProperties;
      final Map<String, String> caProperties = Collections.singletonMap("foo", "bar");
      final Optional<String> schedulingSource = Optional.empty();
      final Map<String, Period> scheduled = schedSvc.addMultipleEvents(
          rrule,
          start,
          end,
          duration,
          tz,
          captureAgentId,
          userIds,
          mpTemplate,
          wfProperties,
          caProperties,
          schedulingSource
      );
    }
  }

  @Test
  public void removeRecording() throws Exception {
    long currentTime = System.currentTimeMillis();
    String id = "Recording1";
    MediaPackage mediaPackage = generateEvent(Optional.of(id));
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "Device A",
            Collections.<String> emptySet(), mediaPackage, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.updateRecordingState(id, CAPTURING);
    assertEquals(1, schedSvc.getKnownRecordings().size());

    String id2 = "Recording2";
    MediaPackage mediaPackageB = generateEvent(Optional.of(id2));
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "Device B",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.updateRecordingState(id2, UPLOADING);
    assertEquals(2, schedSvc.getKnownRecordings().size());

    verifyRecording("notAnRecording", null);
    verifyRecording(id, CAPTURING);
    verifyRecording(id2, UPLOADING);
    assertEquals(2, schedSvc.getKnownRecordings().size());

    try {
      schedSvc.removeRecording(id);
    } catch (NotFoundException e) {
      fail();
    }
    try {
      schedSvc.removeRecording("asdfasdf");
      fail();
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
    verifyRecording("notAnRecording", null);
    verifyRecording(id, null);
    verifyRecording(id2, UPLOADING);
    assertEquals(1, schedSvc.getKnownRecordings().size());
  }

  @Test
  public void testFindConflictingEvents() throws Exception {
    final long currentTime = System.currentTimeMillis();
    final MediaPackage mediaPackageA = generateEvent(Optional.empty());
    final MediaPackage mediaPackageB = generateEvent(Optional.empty());
    final MediaPackage mediaPackageC = generateEvent(Optional.empty());
    final MediaPackage mediaPackageD = generateEvent(Optional.empty());
    //
    schedSvc.addEvent(new Date(currentTime + seconds(10)), new Date(currentTime + hours(1) + seconds(10)), "Device A",
            Collections.<String> emptySet(), mediaPackageA, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.addEvent(new Date(currentTime + hours(24)), new Date(currentTime + hours(25)), "Device A",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.addEvent(new Date(currentTime - hours(1)), new Date(currentTime - minutes(10)), "Device C",
            Collections.<String> emptySet(), mediaPackageC, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.addEvent(new Date(currentTime + seconds(10)), new Date(currentTime + hours(1) + seconds(10)), "Device D",
            Collections.<String> emptySet(), mediaPackageD, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    {
      List<MediaPackage> allEvents = schedSvc.search(Optional.empty(), Optional.empty(), Optional.empty(),
              Optional.empty(), Optional.empty());
      assertEquals(4, allEvents.size());
    }
    final Date start = new Date(currentTime);
    final Date end = new Date(currentTime + hours(2));
    {
      List<MediaPackage> events = schedSvc.search(Optional.of("Some Other Device"), Optional.of(start),
              Optional.empty(), Optional.empty(), Optional.of(end));
      assertEquals(0, events.size());
    }
    {
      List<MediaPackage> events = schedSvc.search(Optional.of("Device A"), Optional.of(start), Optional.empty(),
              Optional.empty(), Optional.of(end));
      assertEquals(1, events.size());
    }
    {
      ZonedDateTime startZdt = ZonedDateTime.ofInstant(start.toInstant(), ZoneOffset.UTC);
      List<MediaPackage> events = schedSvc.findConflictingEvents("Device A",
              new RRule("FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH,FR,SA;BYHOUR=" + startZdt.getHour()
                  + ";BYMINUTE=" + startZdt.getMinute()), start, new Date(start.getTime() + hours(48)),
              new Long(seconds(36)), TimeZone.getTimeZone("America/Chicago"));
      assertEquals(2, events.size());
    }
    {
      // No events are contained in the RRule and date range:
      // 2019-02-16T16:00:00Z to 2019-02-16T16:55:00Z, FREQ=WEEKLY;BYDAY=WE;BYHOUR=16;BYMINUTE=0
      List<MediaPackage> conflicts = schedSvc.findConflictingEvents("Device A",
              new RRule("FREQ=WEEKLY;BYDAY=WE;BYHOUR=16;BYMINUTE=0"), new Date(1550332800000L),
              new Date(1550336100000L), 1000, TimeZone.getTimeZone("Africa/Johannesburg"));
      assertEquals(0, conflicts.size());
    }
    {
      //Event A starts before event B, and ends during event B
      List<MediaPackage> conflicts = schedSvc.findConflictingEvents("Device A",
              new Date(currentTime + hours(23) + minutes(30)),
              new Date(currentTime + hours(24) + minutes(30)));
      assertEquals(1, conflicts.size());

      //Event A starts during event B, and ends after event B
      conflicts = schedSvc.findConflictingEvents("Device A",
              new Date(currentTime + hours(24) + minutes(30)),
              new Date(currentTime + hours(25) + minutes(30)));
      assertEquals(1, conflicts.size());

      //Event A starts at the same time as event B
      conflicts = schedSvc.findConflictingEvents("Device A",
              new Date(currentTime + hours(24)),
              new Date(currentTime + hours(24) + minutes(30)));
      assertEquals(1, conflicts.size());

      //Event A ends at the same time as event B
      conflicts = schedSvc.findConflictingEvents("Device A",
              new Date(currentTime + hours(24) + minutes(10)),
              new Date(currentTime + hours(25)));
      assertEquals(1, conflicts.size());

      //Event A is contained entirely within event B
      conflicts = schedSvc.findConflictingEvents("Device A",
              new Date(currentTime + hours(24) + minutes(10)),
              new Date(currentTime + hours(24) + minutes(50)));
      assertEquals(1, conflicts.size());

      //Event A contains event B entirely
      conflicts = schedSvc.findConflictingEvents("Device A",
              new Date(currentTime + hours(23)),
              new Date(currentTime + hours(26)));
      assertEquals(1, conflicts.size());
    }
  }

  @Test
  public void testCreateAndUpdateConflictingEvents() throws Exception {
    Date start = new Date();
    Date end = new Date(System.currentTimeMillis() + 60000);
    final long currentTime = System.currentTimeMillis();
    final MediaPackage mediaPackageA = generateEvent(Optional.empty());
    final MediaPackage mediaPackageB = generateEvent(Optional.empty());
    DublinCoreCatalog event = generateEvent("captureId", start, end);
    addDublinCore(Optional.empty(), mediaPackageB, event);
    //
    schedSvc.addEvent(new Date(currentTime), new Date(currentTime + hours(1) + seconds(10)), "Device A",
            Collections.<String> emptySet(), mediaPackageA, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.addEvent(new Date(currentTime + hours(2)), new Date(currentTime + hours(25)), "Device A",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    {
      List<MediaPackage> allEvents = schedSvc.search(Optional.empty(), Optional.empty(), Optional.empty(),
              Optional.empty(), Optional.empty());
      assertEquals(2, allEvents.size());
    }
  }

  @Test
  public void testSearchOrder() throws Exception {
    final long currentTime = System.currentTimeMillis();
    final MediaPackage mediaPackageA = generateEvent(Optional.empty());
    final MediaPackage mediaPackageB = generateEvent(Optional.empty());
    final MediaPackage mediaPackageC = generateEvent(Optional.empty());
    final MediaPackage mediaPackageD = generateEvent(Optional.empty());
    //
    schedSvc.addEvent(new Date(currentTime + seconds(10)), new Date(currentTime + hours(1) + seconds(10)), "Device A",
            Collections.<String> emptySet(), mediaPackageA, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.addEvent(new Date(currentTime + hours(24)), new Date(currentTime + hours(25)), "Device A",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.addEvent(new Date(currentTime - hours(1)), new Date(currentTime - minutes(10)), "Device C",
            Collections.<String> emptySet(), mediaPackageC, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    schedSvc.addEvent(new Date(currentTime + seconds(10)), new Date(currentTime + hours(1) + seconds(10)), "Device D",
            Collections.<String> emptySet(), mediaPackageD, wfProperties, Collections.<String, String> emptyMap(),
            Optional.empty());
    {
      List<MediaPackage> allEvents = schedSvc.search(Optional.empty(), Optional.empty(), Optional.empty(),
              Optional.empty(), Optional.empty());
      assertEquals(4, allEvents.size());
      assertEquals(mediaPackageC, allEvents.get(0));
      assertEquals(mediaPackageB, allEvents.get(3));
    }
  }

  @Test
  public void testUpdateEvent() throws Exception {
    final long currentTime = System.currentTimeMillis();
    final String initialTitle = "Recording 1";
    final DublinCoreCatalog initalEvent = generateEvent("Device A", Optional.empty(), Optional.of(initialTitle),
            new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000));
    MediaPackage mediaPackage = generateEvent(Optional.empty());
    String elementId = addDublinCore(Optional.empty(), mediaPackage, initalEvent);

    Map<String, String> caProperties = map(tuple("org.opencastproject.workflow.definition", "full"));
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "Device A",
            Collections.<String> emptySet(), mediaPackage, wfProperties, caProperties, Optional.empty());

    Map<String, String> initalCaProps = schedSvc.getCaptureAgentConfiguration(mediaPackage.getIdentifier().toString());
    checkEvent(mediaPackage.getIdentifier().toString(), initalCaProps, initialTitle);

    // do single update
    final String updatedTitle1 = "Recording 2";
    final DublinCoreCatalog updatedEvent1 = generateEvent("Device A",
            Optional.of(mediaPackage.getIdentifier().toString()), Optional.of(updatedTitle1),
            new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000));
    addDublinCore(Optional.of(elementId), mediaPackage, updatedEvent1);

    schedSvc.updateEvent(mediaPackage.getIdentifier().toString(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.of(mediaPackage), Optional.of(wfPropertiesUpdated),
            Optional.empty());

    final Map<String, String> updatedCaProps = new HashMap<>(initalCaProps);
    updatedCaProps.put("event.title", updatedTitle1);
    updatedCaProps.put("org.opencastproject.workflow.config.test", "false");
    updatedCaProps.put("org.opencastproject.workflow.config.skip", "true");
    updatedCaProps.remove("org.opencastproject.workflow.config.clear");

    // copy to new HashMap since returned map wrapper does not delegate hashcode and equals
    assertEquals("CA properties", updatedCaProps,
            new HashMap<>(schedSvc.getCaptureAgentConfiguration(mediaPackage.getIdentifier().toString())));
    assertEquals("DublinCore title", updatedTitle1,
            schedSvc.getDublinCore(mediaPackage.getIdentifier().toString()).getFirst(PROPERTY_TITLE));
    checkIcalFeed(updatedCaProps, updatedTitle1);
  }

  @Test
  public void testEventManagement() throws Exception {
    MediaPackage mediaPackage = generateEvent(Optional.empty());
    DublinCoreCatalog event = generateEvent("testdevice", new Date(System.currentTimeMillis() - 2000),
            new Date(System.currentTimeMillis() + 60000));
    event.set(PROPERTY_TITLE, "Demotitle");
    event.add(PROPERTY_CREATOR, "creator2");
    String catalogId = addDublinCore(Optional.empty(), mediaPackage, event);
    Map<String, String> caProperties = generateCaptureAgentMetadata("testdevice");

    schedSvc.addEvent(new Date(System.currentTimeMillis() - 2000), new Date(System.currentTimeMillis() + 60000),
            "testdevice", Collections.<String> emptySet(), mediaPackage, wfProperties, caProperties,
            Optional.empty());

    // test iCalender export
    CalendarBuilder calBuilder = new CalendarBuilder();
    Calendar cal;
    try {
      String icalString = schedSvc.getCalendar(Optional.empty(), Optional.empty(), Optional.empty());
      cal = calBuilder.build(IOUtils.toInputStream(icalString, "UTF-8"));
      ComponentList vevents = cal.getComponents(VEVENT);
      for (int i = 0; i < vevents.size(); i++) {
        PropertyList attachments = ((VEvent) vevents.get(i)).getProperties(Property.ATTACH);
        for (int j = 0; j < attachments.size(); j++) {
          String attached = ((Property) attachments.get(j)).getValue();
          String filename = ((Property) attachments.get(j)).getParameter("X-APPLE-FILENAME").getValue();
          attached = new String(Base64.decodeBase64(attached));
          if ("org.opencastproject.capture.agent.properties".equals(filename)) {
            Assert.assertTrue(attached.contains("capture.device.id=testdevice"));
          }
          if ("episode.xml".equals(filename)) {
            Assert.assertTrue(attached.contains("Demotitle"));
          }
        }
      }
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    } catch (ParserException e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }

    // test for upcoming events (it should not be in there).
    List<MediaPackage> upcoming = schedSvc.search(Optional.empty(), Optional.of(new Date(System.currentTimeMillis())),
            Optional.empty(), Optional.empty(), Optional.empty());
    Assert.assertTrue(upcoming.isEmpty());

    List<MediaPackage> all = schedSvc.search(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());
    assertEquals(1, all.size());

    all = schedSvc.search(Optional.of("somedevice"), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    Assert.assertTrue(all.isEmpty());

    // update event
    event.set(PROPERTY_TEMPORAL,
            EncodingSchemeUtils.encodePeriod(new DCMIPeriod(new Date(System.currentTimeMillis() + 180000),
                    new Date(System.currentTimeMillis() + 600000)), Precision.Second));
    addDublinCore(Optional.of(catalogId), mediaPackage, event);

    schedSvc.updateEvent(mediaPackage.getIdentifier().toString(),
            Optional.of(new Date(System.currentTimeMillis() + 180000)),
            Optional.of(new Date(System.currentTimeMillis() + 600000)), Optional.empty(), Optional.empty(),
            Optional.of(mediaPackage), Optional.of(wfPropertiesUpdated), Optional.empty());

    // test for upcoming events (now it should be there)
    upcoming = schedSvc.search(Optional.empty(), Optional.of(new Date(System.currentTimeMillis())), Optional.empty(),
            Optional.empty(), Optional.empty());
    assertEquals(1, upcoming.size());

    // delete event
    schedSvc.removeEvent(mediaPackage.getIdentifier().toString());
    try {
      schedSvc.getMediaPackage(mediaPackage.getIdentifier().toString());
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    upcoming = schedSvc.search(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty());
    assertEquals(0, upcoming.size());
  }

  @Test
  public void removeScheduledRecordingsBeforeBufferEmpty() throws Exception {
    schedSvc.removeScheduledRecordingsBeforeBuffer(0);
  }

  @Test
  public void removeScheduledRecordingsBeforeBufferInputOneEvent() throws Exception {
    Date start = new Date(System.currentTimeMillis() - 160000);
    Date end = new Date(System.currentTimeMillis() - 60000);
    String captureDeviceID = "demo";
    MediaPackage mp = generateEvent(Optional.empty());
    DublinCoreCatalog event = generateEvent(captureDeviceID, start, end);
    addDublinCore(Optional.empty(), mp, event);
    Map<String, String> caProperties = generateCaptureAgentMetadata("demo");

    // Store event
    schedSvc.addEvent(start, end, captureDeviceID, Collections.<String> emptySet(), mp, wfProperties, caProperties,
            Optional.empty());
    schedSvc.removeScheduledRecordingsBeforeBuffer(0);

    try {
      schedSvc.getMediaPackage(mp.getIdentifier().toString());
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    Optional<MediaPackage> mediaPackage = assetManager.getMediaPackage(mp.getIdentifier().toString());
    assertFalse(mediaPackage.isPresent());
  }

  @Test
  public void testRemoveEventSimple() throws Exception {
    String defaultOrgId = new DefaultOrganization().getId().toString();
    final Date start = new Date(System.currentTimeMillis() - 160000);
    final Date end = new Date(System.currentTimeMillis() - 60000);
    final String captureDeviceID = "demo";
    final MediaPackage mp = generateEvent(Optional.empty());
    final String mpId = mp.getIdentifier().toString();
    final DublinCoreCatalog event = generateEvent(captureDeviceID, start, end);
    addDublinCore(Optional.empty(), mp, event);
    final Map<String, String> caProperties = generateCaptureAgentMetadata(captureDeviceID);
    // make sure that the asset manager is empty
    assertEquals("The asset manager should not contain any episodes", 0, assetManager.countEvents(defaultOrgId));
    // store event
    schedSvc.addEvent(start, end, captureDeviceID, Collections.<String> emptySet(), mp, wfProperties, caProperties,
            Optional.empty());
    {
      assertEquals("The asset manager should contain one episode", 1, assetManager.countEvents(defaultOrgId));
      assertTrue(assetManager.snapshotExists(mpId));
    }
    // remove event
    schedSvc.removeEvent(mpId);
    try {
      schedSvc.getMediaPackage(mpId);
      Assert.fail("No media package should be found since it has been deleted before");
    } catch (NotFoundException ignore) {
    }
  }

  @Test
  public void testGetCurrentRecording() throws Exception {
    final long nowMillis = System.currentTimeMillis();
    final long oneHourMillis = 3600_000;
    final String captureAgentId = "Device A";
    final Set<String> userIds = Collections.emptySet();
    final Map<String, String> wfProperties = this.wfProperties;
    final Map<String, String> caProperties = Collections.singletonMap("foo", "bar");
    final Optional<String> schedulingSource = Optional.empty();
    final String id = "Recording";

    // We add 3 recordings here. One is in the past, one is current, one is in the future.
    // start = now - 4h, end = now - 2h              0
    // start = now - 1h, end = now + 1h              1
    // start = now + 2h, end = now + 4h              2
    for (int i = 0; i < 3; i++) {
      final long offset = i * 3 * oneHourMillis;
      final Date start = new Date(nowMillis - 4 * oneHourMillis + offset);
      final Date end = new Date(nowMillis - 2 * oneHourMillis  + offset);
      final MediaPackage mp = generateEvent(Optional.of(id + i));
      final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId,
          Optional.of(mp.getIdentifier().toString()), Optional.of("Test Title" + i), start, end);
      addDublinCore(Optional.of(mp.getIdentifier().toString()), mp, dublinCoreCatalog);
      schedSvc.addEvent(
          start,
          end,
          captureAgentId,
          userIds,
          mp,
          wfProperties,
          caProperties,
          schedulingSource
      );
    }

    // We expect the second of the three recordings to be the current one
    final Optional<MediaPackage> currentRecording = schedSvc.getCurrentRecording(captureAgentId);
    assertTrue(currentRecording.isPresent());
    final TechnicalMetadata technicalMetadata =
        schedSvc.getTechnicalMetadata(currentRecording.get().getIdentifier().toString());
    assertEquals(id + 1, currentRecording.get().getIdentifier().toString());
    assertEquals(nowMillis - oneHourMillis, technicalMetadata.getStartDate().getTime());
    assertEquals(nowMillis + oneHourMillis, technicalMetadata.getEndDate().getTime());
  }

  @Test
  public void testGetCurrentRecordingNone() throws Exception {
    final long nowMillis = System.currentTimeMillis();
    final long oneHourMillis = 3600_000;
    final String captureAgentId = "Device A";
    final Set<String> userIds = Collections.emptySet();
    final Map<String, String> wfProperties = this.wfProperties;
    final Map<String, String> caProperties = Collections.singletonMap("foo", "bar");
    final Optional<String> schedulingSource = Optional.empty();
    final String id = "Recording";

    // We add 2 recordings here. One is in the past, one is in the future.
    // start = now - 4h, end = now - 2h              0
    // start = now + 2h, end = now + 4h              2
    for (int i = 0; i < 3; i++) {
      if (i == 1) {
        continue;
      }
      final long offset = i * 3 * oneHourMillis;
      final Date start = new Date(nowMillis - 4 * oneHourMillis + offset);
      final Date end = new Date(nowMillis - 2 * oneHourMillis  + offset);
      final MediaPackage mp = generateEvent(Optional.of(id + i));
      final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId,
          Optional.of(mp.getIdentifier().toString()), Optional.of("Test Title" + i), start, end);
      addDublinCore(Optional.of(mp.getIdentifier().toString()), mp, dublinCoreCatalog);
      schedSvc.addEvent(
          start,
          end,
          captureAgentId,
          userIds,
          mp,
          wfProperties,
          caProperties,
          schedulingSource
      );
    }

    // We expect no current recording to be found
    final Optional<MediaPackage> currentRecording = schedSvc.getCurrentRecording(captureAgentId);
    assertFalse(currentRecording.isPresent());
  }

  @Test
  public void testGetUpcomingRecording() throws Exception {
    final long nowMillis = System.currentTimeMillis();
    final long oneHourMillis = 3600_000;
    final String captureAgentId = "Device A";
    final Set<String> userIds = Collections.emptySet();
    final Map<String, String> wfProperties = this.wfProperties;
    final Map<String, String> caProperties = Collections.singletonMap("foo", "bar");
    final Optional<String> schedulingSource = Optional.empty();
    final String id = "Recording";

    // We add 3 recordings here. One is in the past, one is current, one is in the future.
    // start = now - 4h, end = now - 2h              0
    // start = now - 1h, end = now + 1h              1
    // start = now + 2h, end = now + 4h              2
    for (int i = 0; i < 3; i++) {
      final long offset = i * 3 * oneHourMillis;
      final Date start = new Date(nowMillis - 4 * oneHourMillis + offset);
      final Date end = new Date(nowMillis - 2 * oneHourMillis  + offset);
      final MediaPackage mp = generateEvent(Optional.of(id + i));
      final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId,
          Optional.of(mp.getIdentifier().toString()), Optional.of("Test Title" + i), start, end);
      addDublinCore(Optional.of(mp.getIdentifier().toString()), mp, dublinCoreCatalog);
      schedSvc.addEvent(
          start,
          end,
          captureAgentId,
          userIds,
          mp,
          wfProperties,
          caProperties,
          schedulingSource
      );
    }

    // We expect the third of the three recordings to be the upcoming one
    final Optional<MediaPackage> currentRecording = schedSvc.getUpcomingRecording(captureAgentId);
    assertTrue(currentRecording.isPresent());
    final TechnicalMetadata technicalMetadata =
        schedSvc.getTechnicalMetadata(currentRecording.get().getIdentifier().toString());
    assertEquals(id + 2, currentRecording.get().getIdentifier().toString());
    assertEquals(nowMillis + 2 * oneHourMillis, technicalMetadata.getStartDate().getTime());
    assertEquals(nowMillis + 4 * oneHourMillis, technicalMetadata.getEndDate().getTime());
  }

  @Test
  public void testGetUpcomingRecordingNone() throws Exception {
    final long nowMillis = System.currentTimeMillis();
    final long oneHourMillis = 3600_000;
    final String captureAgentId = "Device A";
    final Set<String> userIds = Collections.emptySet();
    final Map<String, String> wfProperties = this.wfProperties;
    final Map<String, String> caProperties = Collections.singletonMap("foo", "bar");
    final Optional<String> schedulingSource = Optional.empty();
    final String id = "Recording";

    // We add 2 recordings here. One is in the past, one is current, none is in the future.
    // start = now - 4h, end = now - 2h              0
    // start = now - 1h, end = now + 1h              1
    for (int i = 0; i < 2; i++) {
      final long offset = i * 3 * oneHourMillis;
      final Date start = new Date(nowMillis - 4 * oneHourMillis + offset);
      final Date end = new Date(nowMillis - 2 * oneHourMillis  + offset);
      final MediaPackage mp = generateEvent(Optional.of(id + i));
      final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId,
          Optional.of(mp.getIdentifier().toString()), Optional.of("Test Title" + i), start, end);
      addDublinCore(Optional.of(mp.getIdentifier().toString()), mp, dublinCoreCatalog);
      schedSvc.addEvent(
          start,
          end,
          captureAgentId,
          userIds,
          mp,
          wfProperties,
          caProperties,
          schedulingSource
      );
    }

    // We expect no upcoming recording to be found
    final Optional<MediaPackage> currentRecording = schedSvc.getUpcomingRecording(captureAgentId);
    assertFalse(currentRecording.isPresent());
  }

  @Test
  public void testRepopulateIndexMultitenant() throws Exception {
    List<Organization> orgList = Arrays.asList(
            (Organization) new DefaultOrganization(),
            createOrganization("org1", "Org 1"),
            createOrganization("org2", "Org 2"));

    List<User> usersList = Arrays.asList(
            createUser(orgList.get(0), "user1", Arrays.asList(orgList.get(0).getAdminRole())),
            createUser(orgList.get(1), "user2", Arrays.asList(orgList.get(1).getAdminRole())),
            createUser(orgList.get(2), "user3", Arrays.asList(orgList.get(2).getAdminRole())));

    currentUser = usersList.get(0);
    currentOrg = currentUser.getOrganization();

    EventCatalogUIAdapter episodeAdapter = EasyMock.createMock(EventCatalogUIAdapter.class);
    EasyMock.expect(episodeAdapter.getFlavor()).andReturn(MediaPackageElements.EPISODE).anyTimes();
    EasyMock.expect(episodeAdapter.getOrganization()).andReturn(CatalogUIAdapter.ORGANIZATION_WILDCARD).anyTimes();
    EasyMock.expect(episodeAdapter.handlesOrganization(EasyMock.anyString())).andReturn(true).anyTimes();
    EasyMock.replay(episodeAdapter);
    schedSvc.addCatalogUIAdapter(episodeAdapter);

    EasyMock.reset(orgDirectoryService);
    EasyMock.expect(orgDirectoryService.getOrganizations()).andReturn(orgList).anyTimes();
    EasyMock.expect(orgDirectoryService.getOrganization(EasyMock.anyString())).andAnswer(() -> {
      String orgId = (String) EasyMock.getCurrentArguments()[0];
      return orgList.stream().filter(org -> org.getId().equalsIgnoreCase(orgId)).findFirst().orElse(null);
    }).anyTimes();

    SecurityService securityService = schedSvc.getSecurityService();
    EasyMock.reset(securityService);
    EasyMock.expect(securityService.getUser()).andAnswer(() -> currentUser).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andAnswer(() -> currentOrg).anyTimes();
    securityService.setUser(EasyMock.anyObject(User.class));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(orgDirectoryService, securityService);

    // create test events for each organization
    for (User user : usersList) {
      currentUser = user;
      currentOrg = user.getOrganization();
      createEvents("Event", "ca_" + currentOrg.getId(), 1, schedSvc);
    }
    currentUser = usersList.get(0);
    currentOrg = currentUser.getOrganization();

    SearchResult result = EasyMock.createNiceMock(SearchResult.class);

    EasyMock.reset(index);
    EasyMock.expect(index.getIndexName()).andReturn("index").anyTimes();
    EasyMock.expect(index.getByQuery(EasyMock.anyObject(EventSearchQuery.class))).andReturn(result).anyTimes();
    expect(index.addOrUpdateEvent(EasyMock.anyString(), EasyMock.anyObject(java.util.function.Function.class),
            EasyMock.anyObject(Organization.class), EasyMock.anyObject(User.class))).andReturn(Optional.empty())
        .times(orgList.size());
    EasyMock.replay(index, result);
    schedSvc.setIndex(index);

    schedSvc.repopulate(null);
  }

  private String addDublinCore(Optional<String> id, MediaPackage mediaPackage, final DublinCoreCatalog initalEvent)
          throws URISyntaxException, IOException {
    String catalogId = UUID.randomUUID().toString();
    Catalog catalog = null;
    if (id.isPresent()) {
      catalogId = id.get();
      catalog = mediaPackage.getCatalog(catalogId);
    }

    URI uri = workspace.put(mediaPackage.getIdentifier().toString(), catalogId, "dublincore.xml",
            IOUtils.toInputStream(initalEvent.toXmlString(), StandardCharsets.UTF_8.name()));
    if (catalog == null) {
      catalog = (Catalog) mediaPackage.add(uri, Type.Catalog, initalEvent.getFlavor());
      catalog.setIdentifier(catalogId);
    }
    catalog.setChecksum(null);
    return catalogId;
  }

  private String addAcl(Optional<String> id, MediaPackage mediaPackage, final AccessControlList acl) throws Exception {
    String attachmentId = UUID.randomUUID().toString();
    Attachment attachment = null;
    if (id.isPresent()) {
      attachmentId = id.get();
      attachment = mediaPackage.getAttachment(attachmentId);
    }

    URI uri = workspace.put(mediaPackage.getIdentifier().toString(), attachmentId, "security.xml",
            IOUtils.toInputStream(XACMLUtils.getXacml(mediaPackage, acl), StandardCharsets.UTF_8.name()));
    if (attachment == null) {
      attachment = (Attachment) mediaPackage.add(uri, Type.Attachment, MediaPackageElements.XACML_POLICY_EPISODE);
      attachment.setIdentifier(attachmentId);
    }
    attachment.setChecksum(null);
    return attachmentId;
  }

  private List<String> createEvents(String titlePrefix, String agent, int number, SchedulerService schedulerService)
          throws Exception {
    List<String> events = new ArrayList<>();
    long offset = System.currentTimeMillis();
    for (int i = 0; i < number; i++) {
      MediaPackage mp = generateEvent(Optional.empty());
      Date startDateTime = new Date(offset + 10 * 1000 + i * Util.EVENT_MINIMUM_SEPARATION_MILLISECONDS);
      Date endDateTime = new Date(offset + 3610000 + i * Util.EVENT_MINIMUM_SEPARATION_MILLISECONDS);
      offset = endDateTime.getTime();
      final DublinCoreCatalog event = generateEvent(agent, Optional.empty(),
              Optional.of(titlePrefix + "-" + i), startDateTime, endDateTime);
      addDublinCore(Optional.empty(), mp, event);
      schedulerService.addEvent(startDateTime, endDateTime, agent, Collections.<String> emptySet(), mp, wfProperties,
              Collections.<String, String> emptyMap(), Optional.empty());
      events.add(mp.getIdentifier().toString());
    }
    return events;
  }

  private void verifyRecording(String id, String state) throws SchedulerException {
    if (state == null) {
      try {
        schedSvc.getRecordingState(id);
        fail("");
      } catch (NotFoundException e) {
        assertNotNull(e);
      }
    } else {
      try {
        Recording recording = schedSvc.getRecordingState(id);
        assertEquals(id, recording.getID());
        assertEquals(state, recording.getState());
      } catch (NotFoundException e) {
        fail("");
      }
    }
  }

  private void checkEvent(String eventId, Map<String, String> initialCaProps, String title) throws Exception {
    final Map<String, String> updatedCaProps = new HashMap<>(initialCaProps);
    updatedCaProps.put("event.title", title);
    // copy to new HashMap since returned map wrapper does not delegate hashcode and equals
    assertEquals("CA properties", updatedCaProps, new HashMap<>(schedSvc.getCaptureAgentConfiguration(eventId)));
    assertEquals("DublinCore title", title, schedSvc.getDublinCore(eventId).getFirst(PROPERTY_TITLE));
    checkIcalFeed(updatedCaProps, title);
  }

  private void checkIcalFeed(Map<String, String> caProps, String title) throws Exception {
    final String cs = schedSvc.getCalendar(Optional.empty(), Optional.empty(), Optional.empty());
    final Calendar cal = new CalendarBuilder().build(new StringReader(cs));
    assertEquals("number of entries", 1, cal.getComponents().size());
    for (Object co : cal.getComponents()) {
      final Component c = (Component) co;
      assertEquals("SUMMARY property should contain the DC title", title, c.getProperty(Property.SUMMARY).getValue());
      final List<Property> attachments = c.getProperties(Property.ATTACH).stream()
          .map(obj -> (Property) obj)
          .collect(Collectors.toList());
      // episode dublin core
      final List<DublinCoreCatalog> dcsIcal = attachments.stream()
          .filter(p -> byParamNameAndValue(p,"X-APPLE-FILENAME", "episode.xml"))
          .map(p -> parseDc(decodeBase64(getValue(p))))
          .toList();
      assertEquals("number of episode DCs", 1, dcsIcal.size());
      assertEquals("dcterms:title", title, dcsIcal.get(0).getFirst(PROPERTY_TITLE));
      // capture agent properties
      final List<Properties> caPropsIcal = attachments.stream()
          .filter(p -> byParamNameAndValue(p, "X-APPLE-FILENAME", "org.opencastproject.capture.agent.properties"))
          .map(p -> parseProperties(decodeBase64(getValue(p))))
          .toList();
      assertEquals("number of CA property sets", 1, caPropsIcal.size());
      assertTrue("CA properties", eqObj(caProps, caPropsIcal.get(0)));
    }
  }

  private Boolean byParamNameAndValue(Property p, final String name, final String value) {
    final Parameter param = p.getParameter(name);
    return param != null && param.getValue().equals(value);
  }

  private static DublinCoreCatalog parseDc(String s) {
    return DublinCores.read(IOUtils.toInputStream(s));
  }

  private static String decodeBase64(String base64) {
    return new String(Base64.decodeBase64(base64));
  }

  private static String getValue(Property property) {
    return property.getValue();
  }

  private static Properties parseProperties(String s) {
    final Properties p = new Properties();
    try {
      p.load(new StringReader(s));
    } catch (Exception e) {
      return chuck(e);
    }
    return p;
  }

  private static long seconds(int a) {
    return a * 1000L;
  }

  private static long minutes(int a) {
    return seconds(a * 60);
  }

  private static long hours(int a) {
    return minutes(a * 60);
  }

  AssetManager mkAssetManager() throws Exception {
    HttpAssetProvider httpAssetProvider = new HttpAssetProvider() {
      @Override
      public Snapshot prepareForDelivery(Snapshot snapshot) {
        return AssetManagerImpl.rewriteUris(snapshot, new java.util.function.Function<MediaPackageElement, URI>() {
          @Override public URI apply(MediaPackageElement mpe) {
            String baseName = AssetManagerImpl.getFileNameFromUrn(mpe).orElse(mpe.getElementType().toString());

            // the returned uri must match the path of the {@link #getAsset} method
            return uri(archiveDir.toURI(),
                    mpe.getMediaPackage().getIdentifier().toString(),
                    snapshot.getVersion().toString(),
                    mpe.getIdentifier(),
                    baseName);
          }
        });
      }
    };

    final DBSession dbSession = newDBSession("org.opencastproject.assetmanager.impl");
    final Database db = new Database(dbSession);
    db.setHttpAssetProvider(httpAssetProvider);

    JaxbOrganization org = new DefaultOrganization();
    JaxbUser user = new JaxbUser("user", null, org, new JaxbRole(DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN,
            new DefaultOrganization()));

    SecurityService securityService = createNiceMock(SecurityService.class);
    expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.expect(securityService.getUser()).andAnswer(() -> user).anyTimes();
    replay(securityService);

    final AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.getActiveAcl(EasyMock.<MediaPackage>anyObject()))
            .andReturn(tuple(new AccessControlList(), AclScope.Episode))
            .anyTimes();
    EasyMock.replay(authorizationService);

    ElasticsearchIndex esIndex = EasyMock.createNiceMock(ElasticsearchIndex.class);
    EasyMock.expect(esIndex.addOrUpdateEvent(EasyMock.anyString(),
            EasyMock.anyObject(java.util.function.Function.class),
            EasyMock.anyObject(Organization.class), EasyMock.anyObject(User.class)))
        .andReturn(Optional.empty()).atLeastOnce();
    EasyMock.replay(esIndex);

    AssetManagerImpl am = new AssetManagerImpl();
    am.setHttpAssetProvider(httpAssetProvider);
    am.setDatabase(db);
    am.setWorkspace(workspace);
    am.setAssetStore(mkAssetStore());
    am.setAuthorizationService(authorizationService);
    am.setSecurityService(securityService);
    am.setIndex(esIndex);
    am.addEventHandler(EasyMock.createNiceMock(AssetManagerUpdateHandler.class));
    am.addEventHandler(EasyMock.createNiceMock(AssetManagerUpdateHandler.class));
    return am;
  }

  AssetStore mkAssetStore() {
    return new AssetStore() {

      @Override
      public Optional<Long> getUsedSpace() {
        return Optional.empty();
      }

      @Override
      public Optional<Long> getUsableSpace() {
        return Optional.empty();
      }

      @Override
      public Optional<Long> getTotalSpace() {
        return Optional.empty();
      }

      /**
       * For this test we don't store assets with media package element id as filename so it matches the workspace
       * paths. But we can assume that in each folder is only one media package element.
       * @param path
       *          Path to directory
       * @return  First file in that directory
       */
      private File getFirstFile(File path) {
        return path.listFiles()[0];
      }

      @Override
      public void put(StoragePath path, Source source) throws AssetStoreException {
        File destFile = new File(archiveDir, UrlSupport.concat(path.getMediaPackageId(), path.getVersion().toString(),
                path.getMediaPackageElementId(), Paths.get(source.getUri()).getFileName().toString()));
        try {
          FileUtils.copyFile(workspace.get(source.getUri()), destFile);
        } catch (IOException e) {
          throw new RuntimeException(e);
        } catch (NotFoundException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Optional<InputStream> get(StoragePath path) throws AssetStoreException {
        File file = getFirstFile(new File(archiveDir, UrlSupport.concat(path.getMediaPackageId(),
                path.getVersion().toString(), path.getMediaPackageElementId())));
        InputStream inputStream;
        try {
          inputStream = new ByteArrayInputStream(FileUtils.readFileToByteArray(file));
          return Optional.of(inputStream);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public boolean delete(DeletionSelector sel) throws AssetStoreException {
        return false;
      }

      @Override
      public boolean copy(StoragePath from, StoragePath to) throws AssetStoreException {
        File file = getFirstFile(new File(archiveDir, UrlSupport.concat(from.getMediaPackageId(),
                from.getVersion().toString(), from.getMediaPackageElementId())));
        File destFile = getFirstFile(new File(archiveDir,
                UrlSupport.concat(to.getMediaPackageId(), to.getVersion().toString(), to.getMediaPackageElementId())));
        try {
          FileUtils.copyFile(file, destFile);
          return true;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public boolean contains(StoragePath path) throws AssetStoreException {
        return false;
      }

      @Override
      public String getStoreType() {
        return "test_store";
      }
    };
  }

  /**
   * Create a mocked organization.
   * @param id the organization identifier
   * @param name the organization name
   * @return a mocked organization
   */
  static Organization createOrganization(String id, String name) {
    Organization org = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(org.getId()).andReturn(id).anyTimes();
    EasyMock.expect(org.getName()).andReturn(name).anyTimes();
    EasyMock.expect(org.getAdminRole()).andReturn("ROLE_ADMIN_" + id.toUpperCase().replaceAll(" ", "_")).anyTimes();
    EasyMock.expect(org.getProperties()).andReturn(Collections.EMPTY_MAP).anyTimes();
    EasyMock.expect(org.getServers()).andReturn(Collections.EMPTY_MAP).anyTimes();
    EasyMock.replay(org);
    return org;
  }

  /**
   * Create a mocked user.
   * @param org the organization the user belongs to
   * @param username the username
   * @param roles the users role names
   * @return a mocked user
   */
  static User createUser(Organization org, String username, List<String> roles) {
    Set<Role> rolesList = roles.stream().map(roleName -> {
      Role r = EasyMock.createNiceMock(Role.class);
      EasyMock.expect(r.getName()).andReturn(roleName).anyTimes();
      EasyMock.expect(r.getOrganizationId()).andReturn(org.getId()).anyTimes();
      EasyMock.replay(r);
      return r;
    }).collect(Collectors.toSet());

    User user = EasyMock.createNiceMock(User.class);
    EasyMock.expect(user.getName()).andReturn(username).anyTimes();
    EasyMock.expect(user.getUsername()).andReturn(username).anyTimes();
    EasyMock.expect(user.getOrganization()).andReturn(org).anyTimes();
    EasyMock.expect(user.getRoles()).andReturn(rolesList).anyTimes();
    EasyMock.expect(user.hasRole(EasyMock.anyString())).andAnswer(() -> {
      String role = (String) EasyMock.getCurrentArguments()[0];
      return roles.contains(role);
    }).anyTimes();

    EasyMock.replay(user);
    return user;
  }
}
