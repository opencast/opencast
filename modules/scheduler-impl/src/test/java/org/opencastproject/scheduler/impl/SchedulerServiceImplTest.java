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

package org.opencastproject.scheduler.impl;

import static net.fortuna.ical4j.model.Component.VEVENT;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;
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
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.impl.AbstractAssetManager;
import org.opencastproject.assetmanager.impl.HttpAssetProvider;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.AssetStoreException;
import org.opencastproject.assetmanager.impl.storage.DeletionSelector;
import org.opencastproject.assetmanager.impl.storage.Source;
import org.opencastproject.assetmanager.impl.storage.StoragePath;
import org.opencastproject.authorization.xacml.XACMLUtils;
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
import org.opencastproject.mediapackage.identifier.UUIDIdBuilderImpl;
import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.scheduler.SchedulerItem;
import org.opencastproject.message.broker.api.scheduler.SchedulerItemList;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.ConflictHandler;
import org.opencastproject.scheduler.api.ConflictResolution;
import org.opencastproject.scheduler.api.ConflictResolution.Strategy;
import org.opencastproject.scheduler.api.Recording;
import org.opencastproject.scheduler.api.RecordingState;
import org.opencastproject.scheduler.api.SchedulerConflictException;
import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.scheduler.api.TechnicalMetadataImpl;
import org.opencastproject.scheduler.api.Util;
import org.opencastproject.scheduler.endpoint.SchedulerRestService;
import org.opencastproject.scheduler.impl.persistence.SchedulerServiceDatabaseImpl;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.XmlNamespaceContext;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Misc;
import org.opencastproject.util.persistencefn.PersistenceEnv;
import org.opencastproject.util.persistencefn.PersistenceEnvs;
import org.opencastproject.util.persistencefn.PersistenceUtil;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

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
import org.easymock.CaptureType;
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
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public class SchedulerServiceImplTest {

  public static final File baseDir = new File(new File(IoSupport.getSystemTmpDir()), "schedulerservicetest");

  private SeriesService seriesService;
  private static UnitTestWorkspace workspace;
  private AssetManager assetManager;
  private static OrganizationDirectoryService orgDirectoryService;
  private SecurityService securityService;

  private User currentUser = new JaxbUser("admin", "provider", new DefaultOrganization(),
      new JaxbRole("admin", new DefaultOrganization(), "test"));
  private Organization currentOrg = new DefaultOrganization();

  private static SchedulerServiceImpl schedSvc;

  // persistent properties
  private static SchedulerServiceDatabaseImpl schedulerDatabase;

  private static AccessControlList acl;

  private static Map<String, String> wfProperties = new HashMap<>();
  private static Map<String, String> wfPropertiesUpdated = new HashMap<>();

  private static TestConflictHandler testConflictHandler;

  private static class TestConflictHandler implements ConflictHandler {

    private Strategy strategy = Strategy.OLD;

    public void setStrategy(Strategy strategy) {
      this.strategy = strategy;
    }

    @Override
    public ConflictResolution handleConflict(SchedulerEvent newEvent, SchedulerEvent oldEvent) {
      switch (strategy) {
        case OLD:
          return new ConflictResolutionImpl(Strategy.OLD, oldEvent);
        case NEW:
          return new ConflictResolutionImpl(Strategy.NEW, newEvent);
        case MERGED:
          TechnicalMetadata newTechnicalMetadata = newEvent.getTechnicalMetadata();
          TechnicalMetadata technicalMetadata = new TechnicalMetadataImpl(newTechnicalMetadata.getEventId(),
                  newTechnicalMetadata.getAgentId(), newTechnicalMetadata.getStartDate(),
                  newTechnicalMetadata.getEndDate(), newTechnicalMetadata.getPresenters(),
                  newTechnicalMetadata.getWorkflowProperties(), newTechnicalMetadata.getCaptureAgentConfiguration(),
                  newTechnicalMetadata.getRecording());
          SchedulerEvent mergedEvent = new SchedulerEventImpl(newEvent.getEventId(), newEvent.getVersion(),
                  newEvent.getMediaPackage(), technicalMetadata);
          return new ConflictResolutionImpl(Strategy.MERGED, mergedEvent);
        default:
          throw new IllegalStateException("No strategy found for " + strategy);
      }
    }
  };

  @BeforeClass
  public static void beforeClass() throws Exception {
    wfProperties.put("test", "true");
    wfProperties.put("clear", "all");

    wfPropertiesUpdated.put("test", "false");
    wfPropertiesUpdated.put("skip", "true");

    workspace = new UnitTestWorkspace();

    MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);

    final BaseMessage baseMessageMock = EasyMock.createNiceMock(BaseMessage.class);

    MessageReceiver messageReceiver = EasyMock.createNiceMock(MessageReceiver.class);
    EasyMock.expect(messageReceiver.receiveSerializable(EasyMock.anyString(),
            EasyMock.anyObject(MessageSender.DestinationType.class)))
            .andStubReturn(new FutureTask<>(new Callable<Serializable>() {
              @Override
              public Serializable call() throws Exception {
                return baseMessageMock;
              }
            }));

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

    EventCatalogUIAdapter extendedAdapter = EasyMock.createMock(EventCatalogUIAdapter.class);
    EasyMock.expect(extendedAdapter.getFlavor()).andReturn(new MediaPackageElementFlavor("extended", "episode"))
            .anyTimes();
    EasyMock.expect(extendedAdapter.getOrganization()).andReturn(new DefaultOrganization().getId()).anyTimes();

    BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(EasyMock.anyString())).andReturn("adminuser").anyTimes();

    ComponentContext componentContext = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(componentContext.getBundleContext()).andReturn(bundleContext).anyTimes();

    EasyMock.replay(messageSender, baseMessageMock, messageReceiver, authorizationService,
            extendedAdapter, episodeAdapter, orgDirectoryService, componentContext, bundleContext);

    testConflictHandler = new TestConflictHandler();

    schedSvc = new SchedulerServiceImpl();

    schedSvc.setAuthorizationService(authorizationService);
    schedSvc.setWorkspace(workspace);
    schedSvc.setMessageSender(messageSender);
    schedSvc.setMessageReceiver(messageReceiver);
    schedSvc.setConflictHandler(testConflictHandler);
    schedSvc.addCatalogUIAdapter(episodeAdapter);
    schedSvc.addCatalogUIAdapter(extendedAdapter);
    schedSvc.setOrgDirectoryService(orgDirectoryService);

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
    EasyMock.expect(seriesService.getSeries(EasyMock.anyObject(SeriesQuery.class)))
            .andReturn(new DublinCoreCatalogList(seriesCatalogs, 1)).anyTimes();
    EasyMock.replay(seriesService);
    schedSvc.setSeriesService(seriesService);


    schedulerDatabase = new SchedulerServiceDatabaseImpl();
    schedulerDatabase.setEntityManagerFactory(mkEntityManagerFactory(SchedulerServiceDatabaseImpl.PERSISTENCE_UNIT));
    schedulerDatabase.setSecurityService(securityService);
    schedulerDatabase.activate(null);
    schedSvc.setPersistence(schedulerDatabase);

    assetManager = mkAssetManager();
    schedSvc.setAssetManager(assetManager);

    schedSvc.lastModifiedCache.invalidateAll();
  }

  @After
  public void tearDown() throws Exception {
    workspace.clean();
    schedulerDatabase = null;
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

  protected static MediaPackage generateEvent(Opt<String> id) throws MediaPackageException {
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    if (id.isSome())
      mp.setIdentifier(new IdImpl(id.get()));
    return mp;
  }

  protected static DublinCoreCatalog generateEvent(String captureDeviceID, Opt<String> eventId, Opt<String> title,
          Date startTime, Date endTime) {
    DublinCoreCatalog dc = DublinCores.mkOpencastEpisode().getCatalog();
    dc.set(PROPERTY_IDENTIFIER, eventId.getOr("1"));
    dc.set(PROPERTY_TITLE, title.getOr("Demo event"));
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

  protected static DublinCoreCatalog generateExtendedEvent(Opt<String> eventId, String flavorType) {
    DublinCoreCatalog dc = DublinCores.mkStandard();
    final Map<String, String> prefixToUri = new HashMap<>();
    prefixToUri.put("", "http://test.com/video/opencast");
    prefixToUri.put("extended", "http://test.com/video/metadata");
    dc.addBindings(new XmlNamespaceContext(prefixToUri));
    dc.setRootTag(new EName("http://test.com/video/opencast", "extended"));
    dc.setFlavor(new MediaPackageElementFlavor(flavorType, "episode"));
    dc.set(PROPERTY_IDENTIFIER, eventId.getOr("1"));
    dc.set(PROPERTY_EXTENT, "demo");
    return dc;
  }

  protected static DublinCoreCatalog generateEvent(String captureDeviceID, Date startTime, Date endTime) {
    return generateEvent(captureDeviceID, Opt.<String> none(), Opt.<String> none(), startTime, endTime);
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
    MediaPackage mp = generateEvent(Opt.<String> none());
    mp.setSeries(seriesId);
    DublinCoreCatalog event = generateEvent(captureDeviceID, start, end);
    String catalogId = addDublinCore(Opt.<String> none(), mp, event);
    Map<String, String> caProperties = generateCaptureAgentMetadata("demo");
    EasyMock.reset(seriesService);
    EasyMock.expect(seriesService.getSeries(seriesId)).andThrow(new NotFoundException()).once();
    EasyMock.replay(seriesService);

    assertEquals("mod0", schedSvc.getScheduleLastModified(captureDeviceID));

    // Store event
    schedSvc.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.<String> none());
    try {
      MediaPackage mp2 = (MediaPackage) mp.clone();
      mp2.setIdentifier(new UUIDIdBuilderImpl().createNew());
      schedSvc.addEvent(start, end, captureDeviceID, userIds, mp2, wfProperties, caProperties, Opt.<String> none());
      Assert.fail();
    } catch (SchedulerConflictException e) {
      Assert.assertNotNull(e);
    }
    MediaPackage mediaPackage = schedSvc.getMediaPackage(mp.getIdentifier().compact());
    assertEquals(seriesId, mediaPackage.getSeries());
    DublinCoreCatalog eventLoaded = schedSvc.getDublinCore(mp.getIdentifier().compact());
    assertEquals(event.getFirst(PROPERTY_TITLE), eventLoaded.getFirst(PROPERTY_TITLE));
    // the returned map is of type com.entwinemedia.fn.data.ImmutableMapWrapper which
    // does not delegate equals and hashcode so it is necessary to create a HashMap from it
    TechnicalMetadata technicalMetadata = schedSvc.getTechnicalMetadata(mp.getIdentifier().compact());
    assertEquals(mp.getIdentifier().compact(), technicalMetadata.getEventId());
    assertEquals(captureDeviceID, technicalMetadata.getAgentId());
    assertEquals(start, technicalMetadata.getStartDate());
    assertEquals(end, technicalMetadata.getEndDate());
    assertEquals(userIds, technicalMetadata.getPresenters());
    assertTrue(technicalMetadata.getRecording().isNone());
    assertTrue(technicalMetadata.getCaptureAgentConfiguration().size() >= caProperties.size());

    assertEquals(wfProperties, new HashMap<>(schedSvc.getWorkflowConfig(mp.getIdentifier().compact())));
    String lastModified = schedSvc.getScheduleLastModified(captureDeviceID);
    assertNotEquals("mod0", lastModified);

    eventLoaded.set(PROPERTY_TITLE, "Something more");
    addDublinCore(Opt.some(catalogId), mp, eventLoaded);

    userIds.add("user3");
    userIds.remove("user1");
    mp.setSeries("series2");

    // Update event
    schedSvc.updateEvent(mp.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
            Opt.some(userIds), Opt.some(mp), Opt.some(wfProperties), Opt.some(caProperties));

    mediaPackage = schedSvc.getMediaPackage(mp.getIdentifier().compact());
    assertEquals("series2", mediaPackage.getSeries());
    DublinCoreCatalog eventReloaded = schedSvc.getDublinCore(mp.getIdentifier().compact());
    assertEquals("Something more", eventReloaded.getFirst(PROPERTY_TITLE));
    technicalMetadata = schedSvc.getTechnicalMetadata(mp.getIdentifier().compact());
    assertEquals(mp.getIdentifier().compact(), technicalMetadata.getEventId());
    assertEquals(captureDeviceID, technicalMetadata.getAgentId());
    assertEquals(start, technicalMetadata.getStartDate());
    assertEquals(end, technicalMetadata.getEndDate());
    assertEquals(userIds, technicalMetadata.getPresenters());
    assertTrue(technicalMetadata.getRecording().isNone());
    assertTrue(technicalMetadata.getCaptureAgentConfiguration().size() >= caProperties.size());
    String updatedLastModified = schedSvc.getScheduleLastModified(captureDeviceID);
    assertNotEquals("mod0", updatedLastModified);
    assertNotEquals(lastModified, updatedLastModified);

    assertTrue(schedSvc.getCaptureAgentConfiguration(mp.getIdentifier().compact()).size() >= caProperties.size());
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
    MediaPackage mp = generateEvent(Opt.<String> none());
    mp.setSeries(seriesId);
    DublinCoreCatalog event = generateEvent(captureDeviceID, start, end);
    addDublinCore(Opt.<String> none(), mp, event);
    Map<String, String> caProperties = generateCaptureAgentMetadata("demo");
    EasyMock.reset(seriesService);
    EasyMock.expect(seriesService.getSeries(seriesId)).andThrow(new NotFoundException()).once();
    EasyMock.replay(seriesService);

    try {
      // Store event
      schedSvc.addEvent(end, start, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.<String> none());
      fail("Unable to detect end date being before start date during creation of event");
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }

    // Store
    schedSvc.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.<String> none());

    try {
      // Update end date before start date
      schedSvc.updateEvent(mp.getIdentifier().compact(), Opt.some(end), Opt.some(start), Opt.<String> none(),
              Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
              Opt.<Map<String, String>> none());
      fail("Unable to detect end date being before start date during update of event");
    } catch (SchedulerException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void testAclPersistence() throws Exception {
    Date start = new Date();
    Date end = new Date(System.currentTimeMillis() + 60000);
    String captureDeviceID = "demo";
    MediaPackage mp = generateEvent(Opt.<String> none());
    addAcl(Opt.<String> none(), mp, acl);
    Map<String, String> caProperties = generateCaptureAgentMetadata("demo");

    // Store event
    schedSvc.addEvent(start, end, captureDeviceID, Collections.<String> emptySet(), mp, wfProperties, caProperties,
            Opt.<String> none());

    assertTrue(AccessControlUtil.equals(acl, schedSvc.getAccessControlList(mp.getIdentifier().compact())));
  }

  @Test
  public void nonExistantRecording() throws Exception {
    long currentTime = System.currentTimeMillis();
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
    MediaPackage mediaPackage = generateEvent(Opt.some(id));
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "Device A",
            Collections.<String> emptySet(), mediaPackage, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());

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
    MediaPackage mediaPackage = generateEvent(Opt.<String> none());
    DublinCoreCatalog dublinCore = generateEvent(device, new Date(), new Date(System.currentTimeMillis() + 60000));
    addDublinCore(Opt.<String> none(), mediaPackage, dublinCore);
    schedSvc.addEvent(new Date(), new Date(System.currentTimeMillis() + 60000), device, Collections.<String> emptySet(),
            mediaPackage, Collections.<String, String> emptyMap(), Collections.<String, String> emptyMap(),
            Opt.<String> none());

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
    schedSvc.updateEvent(mediaPackage.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<String> none(), Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.some(wfPropertiesUpdated),
            Opt.<Map<String, String>> none());

    // Try using the same old etag. We should get a 200, since the event has changed
    response = restService.getCalendar(device, null, null, request);
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    Assert.assertNotNull(response.getEntity());
  }

  @Test
  public void testCalendarCutoff() throws Exception {
    long currentTime = System.currentTimeMillis();
    MediaPackage mediaPackageA = generateEvent(Opt.<String> none());
    MediaPackage mediaPackageB = generateEvent(Opt.<String> none());

    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + (60 * 60 * 1000)), "Device A",
            Collections.<String> emptySet(), mediaPackageA, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    schedSvc.addEvent(new Date(currentTime + (20 * 24 * 60 * 60 * 1000)),
            new Date(currentTime + (20 * 25 * 60 * 60 * 1000)), "Device A", Collections.<String> emptySet(),
            mediaPackageB, wfProperties, Collections.<String, String> emptyMap(), Opt.<String> none());

    Date start = new Date(currentTime);
    Date end = new Date(currentTime + 60 * 60 * 1000);

    List<MediaPackage> events = schedSvc.search(Opt.some("Device A"), Opt.<Date> none(), Opt.some(end), Opt.some(start),
            Opt.<Date> none());
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
    MediaPackage mediaPackage = generateEvent(Opt.<String> none());
    String captureAgentId = "Device A";
    schedSvc.addEvent(startDate, endDate, captureAgentId, Collections.<String> emptySet(), mediaPackage, wfProperties,
            Collections.<String, String> emptyMap(), Opt.<String> none());

    Date start = new Date(currentTime);
    Date end = new Date(currentTime + 60 * 60 * 1000);

    List<MediaPackage> events = schedSvc.search(Opt.some(captureAgentId), Opt.<Date> none(), Opt.some(end),
            Opt.some(start), Opt.<Date> none());
    assertEquals(1, events.size());
  }

  @Test
  public void testSpatial() throws Exception {
    long currentTime = System.currentTimeMillis();
    MediaPackage mediaPackageA = generateEvent(Opt.<String> none());
    MediaPackage mediaPackageB = generateEvent(Opt.<String> none());
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + (60 * 60 * 1000)), "Device A",
            Collections.<String> emptySet(), mediaPackageA, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + (60 * 60 * 1000)), "Device B",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());

    List<MediaPackage> events = schedSvc.search(Opt.some("Device"), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<Date> none(), Opt.<Date> none());
    assertEquals(0, events.size());

    events = schedSvc.search(Opt.some("Device A"), Opt.<Date> none(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<Date> none());
    assertEquals(1, events.size());

    events = schedSvc.search(Opt.some("Device B"), Opt.<Date> none(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<Date> none());
    assertEquals(1, events.size());

    events = schedSvc.search(Opt.<String> none(), Opt.<Date> none(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<Date> none());
    assertEquals(2, events.size());
  }

  /**
   * Test for failure updating past events
   */
  @Test(expected = SchedulerException.class)
  public void testUpdateExpiredEvent() throws Exception {
    long currentTime = System.currentTimeMillis();

    final String initialTitle = "Recording 1";
    final MediaPackage mediaPackage = generateEvent(Opt.<String> none());
    final Date startDateTime = new Date(currentTime - 20 * 1000);
    final Date endTime = new Date(currentTime - 10 * 1000);
    final DublinCoreCatalog initalEvent = generateEvent("Device A", Opt.<String> none(), Opt.some(initialTitle),
            startDateTime, endTime);
    String catalogId = addDublinCore(Opt.<String> none(), mediaPackage, initalEvent);

    Map<String, String> caMetadata = map(tuple("org.opencastproject.workflow.config.archiveOp", "true"),
            tuple("org.opencastproject.workflow.definition", "full"));

    schedSvc.addEvent(startDateTime, endTime, "Device A", Collections.<String> emptySet(), mediaPackage, wfProperties,
            caMetadata, Opt.<String> none());

    MediaPackage mp = schedSvc.getMediaPackage(mediaPackage.getIdentifier().compact());
    Assert.assertEquals(mediaPackage, mp);

    // test single update
    final String updatedTitle1 = "Recording 2";
    final DublinCoreCatalog updatedEvent1 = generateEvent("Device A", Opt.some(mediaPackage.getIdentifier().compact()),
            Opt.some(updatedTitle1), startDateTime, endTime);
    addDublinCore(Opt.some(catalogId), mediaPackage, updatedEvent1);
    schedSvc.updateEvent(mediaPackage.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<String> none(), Opt.<Set<String>> none(), Opt.some(mediaPackage), Opt.some(wfPropertiesUpdated),
            Opt.<Map<String, String>> none());
    Assert.fail("Schedule should not update a recording that has ended (single)");
  }

  @Test(expected = SchedulerException.class)
  public void testConflictCreation() throws Exception {
    long currentTime = System.currentTimeMillis();
    MediaPackage mediaPackage = generateEvent(Opt.some("1"));

    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + (60 * 60 * 1000)), "Device A",
            Collections.<String> emptySet(), mediaPackage, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    schedSvc.addEvent(new Date(currentTime + (20 * 24 * 60 * 60 * 1000)),
            new Date(currentTime + (20 * 25 * 60 * 60 * 1000)), "Device A", Collections.<String> emptySet(),
            mediaPackage, wfProperties, Collections.<String, String> emptyMap(), Opt.<String> none());
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
    final MediaPackage mpTemplate = generateEvent(Opt.some(id));
    mpTemplate.setSeries(seriesId);
    final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId, Opt.some(mpTemplate.getIdentifier().toString()), Opt.some("Test Title"), start, end);
    addDublinCore(Opt.some(mpTemplate.getIdentifier().toString()), mpTemplate, dublinCoreCatalog);
    final Map<String, String> wfProperties = this.wfProperties;
    final Map<String, String> caProperties = Collections.singletonMap("foo", "bar");
    final Opt<String> schedulingSource = Opt.none();
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
    final MediaPackage mpTemplate = generateEvent(Opt.some(id));
    mpTemplate.setSeries(seriesId);
    final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId, Opt.some(mpTemplate.getIdentifier().toString()), Opt.some("Test Title"), start, end);
    addDublinCore(Opt.some(mpTemplate.getIdentifier().toString()), mpTemplate, dublinCoreCatalog);
    final Map<String, String> wfProperties = this.wfProperties;
    final Map<String, String> caProperties = Collections.singletonMap("foo", "bar");
    final Opt<String> schedulingSource = Opt.none();
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
    final String randomMpId = scheduled.keySet().stream().findAny().orElseThrow(() -> new RuntimeException("This should never happen"));
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
    assertTrue(technicalMetadata.getRecording().isNone());
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
      final MediaPackage mpTemplate = generateEvent(Opt.some(id));
      final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId,
        Opt.some(mpTemplate.getIdentifier().toString()), Opt.some("Test Title"), start, end);
      addDublinCore(Opt.some(mpTemplate.getIdentifier().toString()), mpTemplate, dublinCoreCatalog);
      final Map<String, String> wfProperties = this.wfProperties;
      final Map<String, String> caProperties = Collections.singletonMap("foo", "bar");
      final Opt<String> schedulingSource = Opt.none();
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
  public void testGetArchivedOnly() throws Exception {
    MediaPackage mediaPackage = generateEvent(Opt.some("1"));
    Version version = assetManager.takeSnapshot("test", mediaPackage).getVersion();
    Assert.assertEquals(VersionImpl.FIRST, version);

    String mediaPackageId = mediaPackage.getIdentifier().compact();
    try {
      schedSvc.getMediaPackage(mediaPackageId);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.getDublinCore(mediaPackageId);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.getAccessControlList(mediaPackageId);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.getWorkflowConfig(mediaPackageId);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.getCaptureAgentConfiguration(mediaPackageId);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.getRecordingState(mediaPackageId);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.removeRecording(mediaPackageId);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.removeEvent(mediaPackageId);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.updateRecordingState(mediaPackageId, RecordingState.CAPTURING);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.updateEvent(mediaPackageId, Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
              Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
              Opt.<Map<String, String>> none());
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void removeRecording() throws Exception {
    long currentTime = System.currentTimeMillis();
    String id = "Recording1";
    MediaPackage mediaPackage = generateEvent(Opt.some(id));
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "Device A",
            Collections.<String> emptySet(), mediaPackage, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    schedSvc.updateRecordingState(id, CAPTURING);
    assertEquals(1, schedSvc.getKnownRecordings().size());

    String id2 = "Recording2";
    MediaPackage mediaPackageB = generateEvent(Opt.some(id2));
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "Device B",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
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
    final MediaPackage mediaPackageA = generateEvent(Opt.<String> none());
    final MediaPackage mediaPackageB = generateEvent(Opt.<String> none());
    final MediaPackage mediaPackageC = generateEvent(Opt.<String> none());
    final MediaPackage mediaPackageD = generateEvent(Opt.<String> none());
    //
    schedSvc.addEvent(new Date(currentTime + seconds(10)), new Date(currentTime + hours(1) + seconds(10)), "Device A",
            Collections.<String> emptySet(), mediaPackageA, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    schedSvc.addEvent(new Date(currentTime + hours(24)), new Date(currentTime + hours(25)), "Device A",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    schedSvc.addEvent(new Date(currentTime - hours(1)), new Date(currentTime - minutes(10)), "Device C",
            Collections.<String> emptySet(), mediaPackageC, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    schedSvc.addEvent(new Date(currentTime + seconds(10)), new Date(currentTime + hours(1) + seconds(10)), "Device D",
            Collections.<String> emptySet(), mediaPackageD, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    {
      List<MediaPackage> allEvents = schedSvc.search(Opt.<String> none(), Opt.<Date> none(), Opt.<Date> none(),
              Opt.<Date> none(), Opt.<Date> none());
      assertEquals(4, allEvents.size());
    }
    final Date start = new Date(currentTime);
    final Date end = new Date(currentTime + hours(2));
    {
      List<MediaPackage> events = schedSvc.search(Opt.some("Some Other Device"), Opt.some(start), Opt.<Date> none(),
              Opt.<Date> none(), Opt.some(end));
      assertEquals(0, events.size());
    }
    {
      List<MediaPackage> events = schedSvc.search(Opt.some("Device A"), Opt.some(start), Opt.<Date> none(),
              Opt.<Date> none(), Opt.some(end));
      assertEquals(1, events.size());
    }
    {
      ZonedDateTime startZdt = ZonedDateTime.ofInstant(start.toInstant(), ZoneOffset.UTC);
      List<MediaPackage> events = schedSvc.findConflictingEvents("Device A",
              new RRule("FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH,FR,SA;BYHOUR=" + startZdt.getHour() + ";BYMINUTE=" + startZdt.getMinute()), start, new Date(start.getTime() + hours(48)),
              new Long(seconds(36)), TimeZone.getTimeZone("America/Chicago"));
      assertEquals(2, events.size());
    }
    {
      // No events are contained in the RRule and date range: 2019-02-16T16:00:00Z to 2019-02-16T16:55:00Z, FREQ=WEEKLY;BYDAY=WE;BYHOUR=16;BYMINUTE=0
      List<MediaPackage> conflicts = schedSvc.findConflictingEvents("Device A",
              new RRule("FREQ=WEEKLY;BYDAY=WE;BYHOUR=16;BYMINUTE=0"), new Date(1550332800000L), new Date(1550336100000L), 1000, TimeZone.getTimeZone("Africa/Johannesburg"));
      assertEquals(0, conflicts.size());
    }
    {
      //Event A starts before event B, and ends during event B
      List<MediaPackage> conflicts = schedSvc.findConflictingEvents("Device A", new Date(currentTime + hours(23) + minutes(30)), new Date(currentTime + hours(24) + minutes(30)));
      assertEquals(1, conflicts.size());

      //Event A starts during event B, and ends after event B
      conflicts = schedSvc.findConflictingEvents("Device A", new Date(currentTime + hours(24) + minutes(30)), new Date(currentTime + hours(25) + minutes(30)));
      assertEquals(1, conflicts.size());

      //Event A starts at the same time as event B
      conflicts = schedSvc.findConflictingEvents("Device A", new Date(currentTime + hours(24)), new Date(currentTime + hours(24) + minutes(30)));
      assertEquals(1, conflicts.size());

      //Event A ends at the same time as event B
      conflicts = schedSvc.findConflictingEvents("Device A", new Date(currentTime + hours(24) + minutes(10)), new Date(currentTime + hours(25)));
      assertEquals(1, conflicts.size());

      //Event A is contained entirely within event B
      conflicts = schedSvc.findConflictingEvents("Device A", new Date(currentTime + hours(24) + minutes(10)), new Date(currentTime + hours(24) + minutes(50)));
      assertEquals(1, conflicts.size());

      //Event A contains event B entirely
      conflicts = schedSvc.findConflictingEvents("Device A", new Date(currentTime + hours(23)), new Date(currentTime + hours(26)));
      assertEquals(1, conflicts.size());

      //Event A ends with less than one minute before event B starts
      conflicts = schedSvc.findConflictingEvents("Device A", new Date(currentTime + hours(23)), new Date(currentTime + hours(24) - seconds(1)));
      assertEquals(1, conflicts.size());

      //Event A begins than one minute after event B ends
      conflicts = schedSvc.findConflictingEvents("Device A", new Date(currentTime + hours(25) + seconds(1)), new Date(currentTime + hours(27)));
      assertEquals(1, conflicts.size());
    }
  }

  @Test
  public void testCreateAndUpdateConflictingEvents() throws Exception {
    Date start = new Date();
    Date end = new Date(System.currentTimeMillis() + 60000);
    final long currentTime = System.currentTimeMillis();
    final MediaPackage mediaPackageA = generateEvent(Opt.<String> none());
    final MediaPackage mediaPackageB = generateEvent(Opt.<String> none());
    DublinCoreCatalog event = generateEvent("captureId", start, end);
    addDublinCore(Opt.<String> none(), mediaPackageB, event);
    //
    schedSvc.addEvent(new Date(currentTime), new Date(currentTime + hours(1) + seconds(10)), "Device A",
            Collections.<String> emptySet(), mediaPackageA, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    schedSvc.addEvent(new Date(currentTime + hours(2)), new Date(currentTime + hours(25)), "Device A",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    {
      List<MediaPackage> allEvents = schedSvc.search(Opt.<String> none(), Opt.<Date> none(), Opt.<Date> none(),
              Opt.<Date> none(), Opt.<Date> none());
      assertEquals(2, allEvents.size());
    }
  }

  @Test
  public void testSearchOrder() throws Exception {
    final long currentTime = System.currentTimeMillis();
    final MediaPackage mediaPackageA = generateEvent(Opt.<String> none());
    final MediaPackage mediaPackageB = generateEvent(Opt.<String> none());
    final MediaPackage mediaPackageC = generateEvent(Opt.<String> none());
    final MediaPackage mediaPackageD = generateEvent(Opt.<String> none());
    //
    schedSvc.addEvent(new Date(currentTime + seconds(10)), new Date(currentTime + hours(1) + seconds(10)), "Device A",
            Collections.<String> emptySet(), mediaPackageA, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    schedSvc.addEvent(new Date(currentTime + hours(24)), new Date(currentTime + hours(25)), "Device A",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    schedSvc.addEvent(new Date(currentTime - hours(1)), new Date(currentTime - minutes(10)), "Device C",
            Collections.<String> emptySet(), mediaPackageC, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    schedSvc.addEvent(new Date(currentTime + seconds(10)), new Date(currentTime + hours(1) + seconds(10)), "Device D",
            Collections.<String> emptySet(), mediaPackageD, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<String> none());
    {
      List<MediaPackage> allEvents = schedSvc.search(Opt.<String> none(), Opt.<Date> none(), Opt.<Date> none(),
              Opt.<Date> none(), Opt.<Date> none());
      assertEquals(4, allEvents.size());
      assertEquals(mediaPackageC, allEvents.get(0));
      assertEquals(mediaPackageB, allEvents.get(3));
    }
  }

  @Test
  public void testUpdateEvent() throws Exception {
    final long currentTime = System.currentTimeMillis();
    final String initialTitle = "Recording 1";
    final DublinCoreCatalog initalEvent = generateEvent("Device A", Opt.<String> none(), Opt.some(initialTitle),
            new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000));
    MediaPackage mediaPackage = generateEvent(Opt.<String> none());
    String elementId = addDublinCore(Opt.<String> none(), mediaPackage, initalEvent);

    Map<String, String> caProperties = map(tuple("org.opencastproject.workflow.definition", "full"));
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "Device A",
            Collections.<String> emptySet(), mediaPackage, wfProperties, caProperties, Opt.<String> none());

    Map<String, String> initalCaProps = schedSvc.getCaptureAgentConfiguration(mediaPackage.getIdentifier().compact());
    checkEvent(mediaPackage.getIdentifier().compact(), initalCaProps, initialTitle);

    // do single update
    final String updatedTitle1 = "Recording 2";
    final DublinCoreCatalog updatedEvent1 = generateEvent("Device A", Opt.some(mediaPackage.getIdentifier().compact()),
            Opt.some(updatedTitle1), new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000));
    addDublinCore(Opt.some(elementId), mediaPackage, updatedEvent1);

    schedSvc.updateEvent(mediaPackage.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<String> none(), Opt.<Set<String>> none(), Opt.some(mediaPackage), Opt.some(wfPropertiesUpdated),
            Opt.<Map<String, String>> none());

    final Map<String, String> updatedCaProps = new HashMap<>(initalCaProps);
    updatedCaProps.put("event.title", updatedTitle1);
    updatedCaProps.put("org.opencastproject.workflow.config.test", "false");
    updatedCaProps.put("org.opencastproject.workflow.config.skip", "true");
    updatedCaProps.remove("org.opencastproject.workflow.config.clear");

    // copy to new HashMap since returned map wrapper does not delegate hashcode and equals
    assertEquals("CA properties", updatedCaProps,
            new HashMap<>(schedSvc.getCaptureAgentConfiguration(mediaPackage.getIdentifier().compact())));
    assertEquals("DublinCore title", updatedTitle1,
            schedSvc.getDublinCore(mediaPackage.getIdentifier().compact()).getFirst(PROPERTY_TITLE));
    checkIcalFeed(updatedCaProps, updatedTitle1);
  }

  @Test
  public void testEventManagement() throws Exception {
    MediaPackage mediaPackage = generateEvent(Opt.<String> none());
    DublinCoreCatalog event = generateEvent("testdevice", new Date(System.currentTimeMillis() - 2000),
            new Date(System.currentTimeMillis() + 60000));
    event.set(PROPERTY_TITLE, "Demotitle");
    event.add(PROPERTY_CREATOR, "creator2");
    String catalogId = addDublinCore(Opt.<String> none(), mediaPackage, event);
    Map<String, String> caProperties = generateCaptureAgentMetadata("testdevice");

    schedSvc.addEvent(new Date(System.currentTimeMillis() - 2000), new Date(System.currentTimeMillis() + 60000),
            "testdevice", Collections.<String> emptySet(), mediaPackage, wfProperties, caProperties,
            Opt.<String> none());

    // test iCalender export
    CalendarBuilder calBuilder = new CalendarBuilder();
    Calendar cal;
    try {
      String icalString = schedSvc.getCalendar(Opt.<String> none(), Opt.<String> none(), Opt.<Date> none());
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
    List<MediaPackage> upcoming = schedSvc.search(Opt.<String> none(), Opt.some(new Date(System.currentTimeMillis())),
            Opt.<Date> none(), Opt.<Date> none(), Opt.<Date> none());
    Assert.assertTrue(upcoming.isEmpty());

    List<MediaPackage> all = schedSvc.search(Opt.<String> none(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<Date> none(), Opt.<Date> none());
    assertEquals(1, all.size());

    all = schedSvc.search(Opt.some("somedevice"), Opt.<Date> none(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<Date> none());
    Assert.assertTrue(all.isEmpty());

    // update event
    event.set(PROPERTY_TEMPORAL,
            EncodingSchemeUtils.encodePeriod(new DCMIPeriod(new Date(System.currentTimeMillis() + 180000),
                    new Date(System.currentTimeMillis() + 600000)), Precision.Second));
    addDublinCore(Opt.some(catalogId), mediaPackage, event);

    schedSvc.updateEvent(mediaPackage.getIdentifier().compact(),
            Opt.some(new Date(System.currentTimeMillis() + 180000)),
            Opt.some(new Date(System.currentTimeMillis() + 600000)), Opt.<String> none(), Opt.<Set<String>> none(),
            Opt.some(mediaPackage), Opt.some(wfPropertiesUpdated), Opt.<Map<String, String>> none());

    // test for upcoming events (now it should be there)
    upcoming = schedSvc.search(Opt.<String> none(), Opt.some(new Date(System.currentTimeMillis())), Opt.<Date> none(),
            Opt.<Date> none(), Opt.<Date> none());
    assertEquals(1, upcoming.size());

    // delete event
    schedSvc.removeEvent(mediaPackage.getIdentifier().compact());
    try {
      schedSvc.getMediaPackage(mediaPackage.getIdentifier().compact());
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    upcoming = schedSvc.search(Opt.<String> none(), Opt.<Date> none(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<Date> none());
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
    MediaPackage mp = generateEvent(Opt.<String> none());
    DublinCoreCatalog event = generateEvent(captureDeviceID, start, end);
    addDublinCore(Opt.<String> none(), mp, event);
    Map<String, String> caProperties = generateCaptureAgentMetadata("demo");

    // Store event
    schedSvc.addEvent(start, end, captureDeviceID, Collections.<String> emptySet(), mp, wfProperties, caProperties,
            Opt.<String> none());
    schedSvc.removeScheduledRecordingsBeforeBuffer(0);

    try {
      schedSvc.getMediaPackage(mp.getIdentifier().compact());
      Assert.fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    AQueryBuilder query = assetManager.createQuery();
    AResult result = query.select(query.snapshot()).where(query.organizationId().eq(new DefaultOrganization().getId())
            .and(query.mediaPackageId(mp.getIdentifier().compact())).and(query.version().isLatest())).run();
    Opt<ARecord> record = result.getRecords().head();
    assertFalse(record.isSome());
  }

  @Test
  public void testRemoveEventSimple() throws Exception {
    final Date start = new Date(System.currentTimeMillis() - 160000);
    final Date end = new Date(System.currentTimeMillis() - 60000);
    final String captureDeviceID = "demo";
    final MediaPackage mp = generateEvent(Opt.<String> none());
    final String mpId = mp.getIdentifier().toString();
    final DublinCoreCatalog event = generateEvent(captureDeviceID, start, end);
    addDublinCore(Opt.<String> none(), mp, event);
    final Map<String, String> caProperties = generateCaptureAgentMetadata(captureDeviceID);
    final AQueryBuilder q = assetManager.createQuery();
    // make sure that the asset manager is empty
    assertEquals("The asset manager should not contain any episodes", 0, q.select(q.snapshot()).run().getSize());
    // store event
    schedSvc.addEvent(start, end, captureDeviceID, Collections.<String> emptySet(), mp, wfProperties, caProperties,
            Opt.<String> none());
    {
      final RichAResult r = enrich(q.select(q.snapshot()).run());
      assertEquals("The asset manager should contain one episode", 1, r.getSize());
      assertEquals("Episode ID", mpId, r.getRecords().head2().getMediaPackageId());
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
        final Opt<String> schedulingSource = Opt.none();
        final String id = "Recording";

        // We add 3 recordings here. One is in the past, one is current, one is in the future.
        // start = now - 4h, end = now - 2h              0
        // start = now - 1h, end = now + 1h              1
        // start = now + 2h, end = now + 4h              2
        for (int i = 0; i < 3; i++) {
          final long offset = i * 3 * oneHourMillis;
          final Date start = new Date(nowMillis - 4 * oneHourMillis + offset);
          final Date end = new Date(nowMillis - 2 * oneHourMillis  + offset);
          final MediaPackage mp = generateEvent(Opt.some(id + i));
          final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId, Opt.some(mp.getIdentifier().toString()), Opt.some("Test Title" + i), start, end);
          addDublinCore(Opt.some(mp.getIdentifier().toString()), mp, dublinCoreCatalog);
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
        final Opt<MediaPackage> currentRecording = schedSvc.getCurrentRecording(captureAgentId);
        assertTrue(currentRecording.isSome());
        final TechnicalMetadata technicalMetadata = schedSvc.getTechnicalMetadata(currentRecording.get().getIdentifier().toString());
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
    final Opt<String> schedulingSource = Opt.none();
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
      final MediaPackage mp = generateEvent(Opt.some(id + i));
      final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId, Opt.some(mp.getIdentifier().toString()), Opt.some("Test Title" + i), start, end);
      addDublinCore(Opt.some(mp.getIdentifier().toString()), mp, dublinCoreCatalog);
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
    final Opt<MediaPackage> currentRecording = schedSvc.getCurrentRecording(captureAgentId);
    assertFalse(currentRecording.isSome());
  }

  @Test
  public void testGetUpcomingRecording() throws Exception {
    final long nowMillis = System.currentTimeMillis();
    final long oneHourMillis = 3600_000;
    final String captureAgentId = "Device A";
    final Set<String> userIds = Collections.emptySet();
    final Map<String, String> wfProperties = this.wfProperties;
    final Map<String, String> caProperties = Collections.singletonMap("foo", "bar");
    final Opt<String> schedulingSource = Opt.none();
    final String id = "Recording";

    // We add 3 recordings here. One is in the past, one is current, one is in the future.
    // start = now - 4h, end = now - 2h              0
    // start = now - 1h, end = now + 1h              1
    // start = now + 2h, end = now + 4h              2
    for (int i = 0; i < 3; i++) {
      final long offset = i * 3 * oneHourMillis;
      final Date start = new Date(nowMillis - 4 * oneHourMillis + offset);
      final Date end = new Date(nowMillis - 2 * oneHourMillis  + offset);
      final MediaPackage mp = generateEvent(Opt.some(id + i));
      final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId, Opt.some(mp.getIdentifier().toString()), Opt.some("Test Title" + i), start, end);
      addDublinCore(Opt.some(mp.getIdentifier().toString()), mp, dublinCoreCatalog);
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
    final Opt<MediaPackage> currentRecording = schedSvc.getUpcomingRecording(captureAgentId);
    assertTrue(currentRecording.isSome());
    final TechnicalMetadata technicalMetadata = schedSvc.getTechnicalMetadata(currentRecording.get().getIdentifier().toString());
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
    final Opt<String> schedulingSource = Opt.none();
    final String id = "Recording";

    // We add 2 recordings here. One is in the past, one is current, none is in the future.
    // start = now - 4h, end = now - 2h              0
    // start = now - 1h, end = now + 1h              1
    for (int i = 0; i < 2; i++) {
      final long offset = i * 3 * oneHourMillis;
      final Date start = new Date(nowMillis - 4 * oneHourMillis + offset);
      final Date end = new Date(nowMillis - 2 * oneHourMillis  + offset);
      final MediaPackage mp = generateEvent(Opt.some(id + i));
      final DublinCoreCatalog dublinCoreCatalog = generateEvent(captureAgentId, Opt.some(mp.getIdentifier().toString()), Opt.some("Test Title" + i), start, end);
      addDublinCore(Opt.some(mp.getIdentifier().toString()), mp, dublinCoreCatalog);
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
    final Opt<MediaPackage> currentRecording = schedSvc.getUpcomingRecording(captureAgentId);
    assertFalse(currentRecording.isSome());
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
    EasyMock.expect(episodeAdapter.getOrganization()).andAnswer(() -> { return currentOrg.getId(); }).anyTimes();
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

    MessageSender messageSender = schedSvc.getMessageSender();
    EasyMock.reset(messageSender);
    Capture<SchedulerItemList> schedulerItemsCapture = Capture.newInstance(CaptureType.ALL);
    messageSender.sendObjectMessage(eq(SchedulerItem.SCHEDULER_QUEUE_PREFIX + "Adminui"),
            eq(MessageSender.DestinationType.Queue),
            capture(schedulerItemsCapture));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.replay(messageSender);

    // create test events for each organization
    for (User user : usersList) {
      currentUser = user;
      currentOrg = user.getOrganization();
      createEvents("Event", "ca_" + currentOrg.getId(), 1, schedSvc);
    }
    currentUser = usersList.get(0);
    currentOrg = currentUser.getOrganization();
    schedulerItemsCapture.reset();

    schedSvc.repopulate("adminui");
    assertTrue(schedulerItemsCapture.hasCaptured());
    List<DublinCoreCatalog> dublincoreCatalogs = new ArrayList<>();
    for (SchedulerItemList schedulerItemList : schedulerItemsCapture.getValues()) {
      for (SchedulerItem schedulerItem : schedulerItemList.getItems()) {
        if (schedulerItem.getType() == SchedulerItem.Type.UpdateCatalog) {
          DublinCoreCatalog snapshotDC = schedulerItem.getEvent();
          dublincoreCatalogs.add(snapshotDC);
        }
      }
    }
    assertEquals(orgList.size(), dublincoreCatalogs.size());
  }

  public void testGetJsonMpSchedulerRestEndpoint() throws MediaPackageException, SchedulerException {
    SchedulerRestService restService = new SchedulerRestService();
    List<MediaPackage> mpList = new ArrayList();
    mpList.add(generateEvent(Opt.some("mp1")));
    mpList.add(generateEvent(Opt.some("mp2")));
    String jsonStr = restService.getEventListAsJsonString(mpList);
    Assert.assertTrue(jsonStr.contains("mp1"));
    Assert.assertTrue(jsonStr.contains("mp2"));
  }

  private String addDublinCore(Opt<String> id, MediaPackage mediaPackage, final DublinCoreCatalog initalEvent)
          throws URISyntaxException, IOException {
    String catalogId = UUID.randomUUID().toString();
    Catalog catalog = null;
    if (id.isSome()) {
      catalogId = id.get();
      catalog = mediaPackage.getCatalog(catalogId);
    }

    URI uri = workspace.put(mediaPackage.getIdentifier().compact(), catalogId, "dublincore.xml",
            IOUtils.toInputStream(initalEvent.toXmlString(), StandardCharsets.UTF_8.name()));
    if (catalog == null) {
      catalog = (Catalog) mediaPackage.add(uri, Type.Catalog, initalEvent.getFlavor());
      catalog.setIdentifier(catalogId);
    }
    catalog.setChecksum(null);
    return catalogId;
  }

  private String addAcl(Opt<String> id, MediaPackage mediaPackage, final AccessControlList acl) throws Exception {
    String attachmentId = UUID.randomUUID().toString();
    Attachment attachment = null;
    if (id.isSome()) {
      attachmentId = id.get();
      attachment = mediaPackage.getAttachment(attachmentId);
    }

    URI uri = workspace.put(mediaPackage.getIdentifier().compact(), attachmentId, "security.xml",
            IOUtils.toInputStream(XACMLUtils.getXacml(mediaPackage, acl), StandardCharsets.UTF_8.name()));
    if (attachment == null) {
      attachment = (Attachment) mediaPackage.add(uri, Type.Attachment, MediaPackageElements.XACML_POLICY_EPISODE);
      attachment.setIdentifier(attachmentId);
    }
    attachment.setChecksum(null);
    return attachmentId;
  }

  private int getCountFromString(String searchTerm, String value) {
    int count = 0;
    int index = 0;
    while (value.indexOf(searchTerm, index) != -1) {
      count++;
      index = value.indexOf(searchTerm, index) + searchTerm.length();
    }
    return count;
  }

  private List<String> createEvents(String titlePrefix, String agent, int number, SchedulerService schedulerService)
      throws Exception {
    List<String> events = new ArrayList<>();
    long offset = System.currentTimeMillis();
    for (int i = 0; i < number; i++) {
      MediaPackage mp = generateEvent(Opt.<String> none());
      Date startDateTime = new Date(offset + 10 * 1000 + i * Util.EVENT_MINIMUM_SEPARATION_MILLISECONDS);
      Date endDateTime = new Date(offset + 3610000 + i * Util.EVENT_MINIMUM_SEPARATION_MILLISECONDS);
      offset = endDateTime.getTime();
      final DublinCoreCatalog event = generateEvent(agent, Opt.<String> none(), Opt.some(titlePrefix + "-" + i),
              startDateTime, endDateTime);
      addDublinCore(Opt.<String> none(), mp, event);
      schedulerService.addEvent(startDateTime, endDateTime, agent, Collections.<String> emptySet(), mp, wfProperties,
              Collections.<String, String> emptyMap(), Opt.<String> none());
      events.add(mp.getIdentifier().compact());
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
    final String cs = schedSvc.getCalendar(Opt.<String> none(), Opt.<String> none(), Opt.<Date> none());
    final Calendar cal = new CalendarBuilder().build(new StringReader(cs));
    assertEquals("number of entries", 1, cal.getComponents().size());
    for (Object co : cal.getComponents()) {
      final Component c = (Component) co;
      assertEquals("SUMMARY property should contain the DC title", title, c.getProperty(Property.SUMMARY).getValue());
      final Monadics.ListMonadic<Property> attachments = mlist(c.getProperties(Property.ATTACH))
              .map(Misc.<Object, Property> cast());
      // episode dublin core
      final List<DublinCoreCatalog> dcsIcal = attachments.filter(byParamNameAndValue("X-APPLE-FILENAME", "episode.xml"))
              .map(parseDc.o(decodeBase64).o(getValue)).value();
      assertEquals("number of episode DCs", 1, dcsIcal.size());
      assertEquals("dcterms:title", title, dcsIcal.get(0).getFirst(PROPERTY_TITLE));
      // capture agent properties
      final List<Properties> caPropsIcal = attachments
              .filter(byParamNameAndValue("X-APPLE-FILENAME", "org.opencastproject.capture.agent.properties"))
              .map(parseProperties.o(decodeBase64).o(getValue)).value();
      assertEquals("number of CA property sets", 1, caPropsIcal.size());
      assertTrue("CA properties", eqObj(caProps, caPropsIcal.get(0)));
    }
  }

  private Function<Property, Boolean> byParamNameAndValue(final String name, final String value) {
    return new Function<Property, Boolean>() {
      @Override
      public Boolean apply(Property p) {
        final Parameter param = p.getParameter(name);
        return param != null && param.getValue().equals(value);
      }
    };
  }

  private static Function<Property, String> getValue = new Function<Property, String>() {
    @Override
    public String apply(Property property) {
      return property.getValue();
    }
  };

  private static Function<String, String> decodeBase64 = new Function<String, String>() {
    @Override
    public String apply(String base64) {
      return new String(Base64.decodeBase64(base64));
    }
  };

  private static Function<String, DublinCoreCatalog> parseDc = new Function<String, DublinCoreCatalog>() {
    @Override
    public DublinCoreCatalog apply(String s) {
      return DublinCores.read(IOUtils.toInputStream(s));
    }
  };

  private static Function<String, Properties> parseProperties = new Function.X<String, Properties>() {
    @Override
    public Properties xapply(String s) throws Exception {
      final Properties p = new Properties();
      p.load(new StringReader(s));
      return p;
    }
  };

  private static long seconds(int a) {
    return a * 1000L;
  }

  private static long minutes(int a) {
    return seconds(a * 60);
  }

  private static long hours(int a) {
    return minutes(a * 60);
  }

  private static long days(int a) {
    return hours(a * 24);
  }

  AssetManager mkAssetManager() throws Exception {
    final PersistenceEnv penv = PersistenceEnvs.mk(mkEntityManagerFactory("org.opencastproject.assetmanager.impl"));
    final Database db = new Database(null, penv);
    return new AbstractAssetManager() {

      @Override
      public HttpAssetProvider getHttpAssetProvider() {
        // identity provider
        return new HttpAssetProvider() {
          @Override
          public Snapshot prepareForDelivery(Snapshot snapshot) {
            return AbstractAssetManager.rewriteUris(snapshot, new Fn<MediaPackageElement, URI>() {
              @Override public URI apply(MediaPackageElement mpe) {
                String baseName = getFileNameFromUrn(mpe).getOr(mpe.getElementType().toString());

                // the returned uri must match the path of the {@link #getAsset} method
                return uri(baseDir.toURI(),
                        mpe.getMediaPackage().getIdentifier().toString(),
                        mpe.getIdentifier(),
                        baseName);
              }
            });
          }
        };
      }

      @Override
      public Database getDb() {
        return db;
      }

      @Override
      protected Workspace getWorkspace() {
        return workspace;
      }

      @Override
      public AssetStore getLocalAssetStore() {
        return mkAssetStore();
      }

      @Override
      protected String getCurrentOrgId() {
        return currentOrg.getId();
      }
    };
  }

  AssetStore mkAssetStore() {
    return new AssetStore() {

      @Override
      public Option<Long> getUsedSpace() {
        return Option.none();
      }

      @Override
      public Option<Long> getUsableSpace() {
        return Option.none();
      }

      @Override
      public Option<Long> getTotalSpace() {
        return Option.none();
      }

      @Override
      public void put(StoragePath path, Source source) throws AssetStoreException {
        File destFile = new File(baseDir, UrlSupport.concat(path.getMediaPackageId(), path.getMediaPackageElementId(),
                path.getVersion().toString()));
        try {
          FileUtils.copyFile(workspace.get(source.getUri()), destFile);
        } catch (IOException e) {
          throw new RuntimeException(e);
        } catch (NotFoundException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Opt<InputStream> get(StoragePath path) throws AssetStoreException {
        File file = new File(baseDir, UrlSupport.concat(path.getMediaPackageId(), path.getMediaPackageElementId(),
                path.getVersion().toString()));
        InputStream inputStream;
        try {
          inputStream = new ByteArrayInputStream(FileUtils.readFileToByteArray(file));
          return Opt.some(inputStream);
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
        File file = new File(baseDir, UrlSupport.concat(from.getMediaPackageId(), from.getMediaPackageElementId(),
                from.getVersion().toString()));
        File destFile = new File(baseDir,
                UrlSupport.concat(to.getMediaPackageId(), to.getMediaPackageElementId(), to.getVersion().toString()));
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

  static EntityManagerFactory mkEntityManagerFactory(String persistenceUnit) {
    if ("mysql".equals(System.getProperty("useDatabase"))) {
      return mkMySqlEntityManagerFactory(persistenceUnit);
    } else {
      return mkH2EntityManagerFactory(persistenceUnit);
    }
  }

  static EntityManagerFactory mkH2EntityManagerFactory(String persistenceUnit) {
    return PersistenceUtil.mkTestEntityManagerFactory(persistenceUnit, true);
  }

  static EntityManagerFactory mkMySqlEntityManagerFactory(String persistenceUnit) {
    return PersistenceUtil.mkEntityManagerFactory(persistenceUnit, "MySQL", "com.mysql.jdbc.Driver",
            "jdbc:mysql://localhost/test_scheduler", "matterhorn", "matterhorn",
            org.opencastproject.util.data.Collections.map(tuple("eclipselink.ddl-generation", "drop-and-create-tables"),
                    tuple("eclipselink.ddl-generation.output-mode", "database"),
                    tuple("eclipselink.logging.level.sql", "FINE"), tuple("eclipselink.logging.parameters", "true")),
            PersistenceUtil.mkTestPersistenceProvider());
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
