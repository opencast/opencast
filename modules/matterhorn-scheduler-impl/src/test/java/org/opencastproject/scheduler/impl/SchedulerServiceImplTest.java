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
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.mediapackage.identifier.UUIDIdBuilderImpl;
import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
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
import org.opencastproject.scheduler.api.SchedulerService.ReviewStatus;
import org.opencastproject.scheduler.api.SchedulerService.SchedulerTransaction;
import org.opencastproject.scheduler.api.SchedulerTransactionLockException;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.scheduler.api.TechnicalMetadataImpl;
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
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
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

import com.entwinemedia.fn.data.Opt;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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

import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public class SchedulerServiceImplTest {

  private SeriesService seriesService;
  private static UnitTestWorkspace workspace;
  private AssetManager assetManager;

  private static SchedulerServiceImpl schedSvc;

  // persistent properties
  private static SchedulerServiceDatabaseImpl schedulerDatabase;

  private static AccessControlList acl;

  private static Map<String, String> wfProperties = new HashMap<>();
  private static Map<String, String> wfPropertiesUpdated = new HashMap<>();

  private static TestConflictHandler testConflictHandler;

  private static final File baseDir = new File(new File(IoSupport.getSystemTmpDir()), "schedulerservicetest");

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
                  newTechnicalMetadata.getEndDate(), oldEvent.getTechnicalMetadata().isOptOut(),
                  newTechnicalMetadata.getPresenters(), newTechnicalMetadata.getWorkflowProperties(),
                  newTechnicalMetadata.getCaptureAgentConfiguration(), newTechnicalMetadata.getRecording());
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

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(new JaxbUser("admin", "provider", new DefaultOrganization(),
            new JaxbRole("admin", new DefaultOrganization(), "test"))).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();

    schedulerDatabase = new SchedulerServiceDatabaseImpl();
    schedulerDatabase.setEntityManagerFactory(mkEntityManagerFactory(SchedulerServiceDatabaseImpl.PERSISTENCE_UNIT));
    schedulerDatabase.setSecurityService(securityService);
    schedulerDatabase.activate(null);

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
            authorizationService.getAcl(EasyMock.anyObject(MediaPackage.class), EasyMock.anyObject(AclScope.class)))
            .andReturn(Option.some(acl)).anyTimes();

    OrganizationDirectoryService orgDirectoryService = EasyMock.createNiceMock(OrganizationDirectoryService.class);
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
            securityService, extendedAdapter, episodeAdapter, orgDirectoryService, componentContext, bundleContext);

    testConflictHandler = new TestConflictHandler();

    schedSvc = new SchedulerServiceImpl();

    schedSvc.setAuthorizationService(authorizationService);
    schedSvc.setSecurityService(securityService);
    schedSvc.setPersistence(schedulerDatabase);
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
    String seriesIdentifier = Long.toString(System.currentTimeMillis());
    DublinCoreCatalog seriesCatalog = getSampleSeriesDublinCoreCatalog(seriesIdentifier);
    List<DublinCoreCatalog> seriesCatalogs = new ArrayList<>();
    seriesCatalogs.add(seriesCatalog);

    seriesService = EasyMock.createMock(SeriesService.class);
    EasyMock.expect(seriesService.getSeries(EasyMock.anyString())).andReturn(seriesCatalog).anyTimes();
    EasyMock.expect(seriesService.getSeries(EasyMock.anyObject(SeriesQuery.class)))
            .andReturn(new DublinCoreCatalogList(seriesCatalogs, 1)).anyTimes();
    EasyMock.expect(seriesService.isOptOut(EasyMock.anyString())).andReturn(false).anyTimes();
    EasyMock.replay(seriesService);
    schedSvc.setSeriesService(seriesService);

    assetManager = mkAssetManager();
    schedSvc.setAssetManager(assetManager);
  }

  @After
  public void tearDown() throws Exception {
    schedSvc.cleanupTransactions();
    workspace.clean();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    schedSvc = null;
    FileSupport.deleteQuietly(baseDir, true);
    schedulerDatabase = null;
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
    EasyMock.expect(seriesService.isOptOut(EasyMock.anyString())).andReturn(false).anyTimes();
    EasyMock.replay(seriesService);

    assertEquals("mod0", schedSvc.getScheduleLastModified(captureDeviceID));

    // Store event
    schedSvc.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.<Boolean> none(),
            Opt.<String> none(), SchedulerService.ORIGIN);
    try {
      MediaPackage mp2 = (MediaPackage) mp.clone();
      mp2.setIdentifier(new UUIDIdBuilderImpl().createNew());
      schedSvc.addEvent(start, end, captureDeviceID, userIds, mp2, wfProperties, caProperties, Opt.<Boolean> none(),
              Opt.<String> none(), SchedulerService.ORIGIN);
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
    assertEquals(false, technicalMetadata.isOptOut());
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
            Opt.some(userIds), Opt.some(mp), Opt.some(wfProperties), Opt.some(caProperties), Opt.some(Opt.some(true)),
            SchedulerService.ORIGIN);

    mediaPackage = schedSvc.getMediaPackage(mp.getIdentifier().compact());
    assertEquals("series2", mediaPackage.getSeries());
    DublinCoreCatalog eventReloaded = schedSvc.getDublinCore(mp.getIdentifier().compact());
    assertEquals("Something more", eventReloaded.getFirst(PROPERTY_TITLE));
    technicalMetadata = schedSvc.getTechnicalMetadata(mp.getIdentifier().compact());
    assertEquals(mp.getIdentifier().compact(), technicalMetadata.getEventId());
    assertEquals(captureDeviceID, technicalMetadata.getAgentId());
    assertEquals(start, technicalMetadata.getStartDate());
    assertEquals(end, technicalMetadata.getEndDate());
    assertEquals(true, technicalMetadata.isOptOut());
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
    EasyMock.expect(seriesService.isOptOut(EasyMock.anyString())).andReturn(false).anyTimes();
    EasyMock.replay(seriesService);

    try {
      // Store event
      schedSvc.addEvent(end, start, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.<Boolean> none(),
              Opt.<String> none(), SchedulerService.ORIGIN);
      fail("Unable to detect end date being before start date during creation of event");
    } catch (IllegalArgumentException e) {
      assertNotNull(e);
    }

    // Store
    schedSvc.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.<Boolean> none(),
            Opt.<String> none(), SchedulerService.ORIGIN);

    try {
      // Update end date before start date
      schedSvc.updateEvent(mp.getIdentifier().compact(), Opt.some(end), Opt.some(start), Opt.<String> none(),
              Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
              Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);
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
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);

    assertTrue(AccessControlUtil.equals(acl, schedSvc.getAccessControlList(mp.getIdentifier().compact())));
  }

  @Test
  public void testTransactionCommit() throws Exception {
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

    try {
      schedSvc.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.<Boolean> none(),
              Opt.some("new"), "new");
    } catch (SchedulerTransactionLockException e) {
      fail("Transaction create lock not working!");
    }

    SchedulerTransaction trx = schedSvc.createTransaction("new");
    assertEquals("new", trx.getSource());

    AQueryBuilder query = assetManager.createQuery();
    AResult result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(1, result.getSize());

    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfPropertiesUpdated, caProperties, Opt.<Boolean> none());

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(2, result.getSize());

    trx.commit();

    query = assetManager.createQuery();
    result = query.select(query.snapshot(), query.properties())
            .where(query.organizationId().eq(new DefaultOrganization().getId())
                    .and(query.mediaPackageId(mp.getIdentifier().compact()))
                    .and(query.hasPropertiesOf(SchedulerServiceImpl.TRX_NAMESPACE).not()))
            .run();
    assertEquals(1, result.getSize());
    ARecord record = result.getRecords().head2();
    assertEquals(VersionImpl.mk(1), record.getSnapshot().get().getVersion());

    try {
      schedSvc.getTransaction(trx.getId());
      fail("Deleted transaction found!");
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void testTransactionCommitCleanup() throws Exception {
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
    MediaPackage mp2 = generateEvent(Opt.<String> none());

    SchedulerTransaction trx = schedSvc.createTransaction("new");
    assertEquals("new", trx.getSource());

    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfPropertiesUpdated, caProperties, Opt.<Boolean> none());

    AQueryBuilder query = assetManager.createQuery();
    AResult result = query.select(query.snapshot(), query.properties())
            .where(query.organizationId().eq(new DefaultOrganization().getId()).and(query.version().isLatest())).run();
    assertEquals(1, result.getSize());

    trx.commit();

    query = assetManager.createQuery();
    result = query.select(query.snapshot(), query.properties())
            .where(query.organizationId().eq(new DefaultOrganization().getId()).and(query.version().isLatest())).run();
    assertEquals(1, result.getSize());

    trx = schedSvc.createTransaction("new");
    assertEquals("new", trx.getSource());
    trx.addEvent(start, end, captureDeviceID, userIds, mp2, wfPropertiesUpdated, caProperties, Opt.<Boolean> none());

    query = assetManager.createQuery();
    result = query.select(query.snapshot(), query.properties())
            .where(query.organizationId().eq(new DefaultOrganization().getId()).and(query.version().isLatest())).run();
    assertEquals(2, result.getSize());

    trx.commit();

    query = assetManager.createQuery();
    result = query.select(query.snapshot(), query.properties())
            .where(query.organizationId().eq(new DefaultOrganization().getId()).and(query.version().isLatest())).run();
    assertEquals(1, result.getSize());

    try {
      schedSvc.getTransaction(trx.getId());
      fail("Deleted transaction found!");
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void testTransactionCommitCollision() throws Exception {
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

    MediaPackage mp2 = generateEvent(Opt.<String> none());
    try {
      schedSvc.addEvent(start, end, captureDeviceID, userIds, mp2, wfPropertiesUpdated, caProperties, Opt.some(false),
              Opt.some("existing"), SchedulerService.ORIGIN);
    } catch (SchedulerTransactionLockException e) {
      fail("Transaction create lock not working!");
    }

    /* Test transaction collision of already existing event and new transaction event */

    SchedulerTransaction trx = schedSvc.createTransaction("new");
    assertEquals("new", trx.getSource());

    AQueryBuilder query = assetManager.createQuery();
    AResult result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    Opt<ARecord> record = result.getRecords().head();
    assertFalse(record.isSome());

    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfPropertiesUpdated, caProperties, Opt.some(false));

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    record = result.getRecords().head();
    assertTrue(record.isSome());

    try {
      trx.commit();
      fail("Pre-conflict detection not working!");
    } catch (SchedulerConflictException e) {
      assertNotNull(e);
    }

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    record = result.getRecords().head();
    assertTrue(record.isSome());

    try {
      schedSvc.getTransaction(trx.getId());
    } catch (NotFoundException e) {
      fail("Transaction found!");
    }

    /* Test transaction collision of already existing event and new opted out transaction event */

    MediaPackage mp4 = (MediaPackage) mp.clone();
    mp4.setIdentifier(new IdImpl("newuuid"));

    SchedulerTransaction trx2 = schedSvc.createTransaction("optedout");
    assertEquals("optedout", trx2.getSource());

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp4.getIdentifier().compact())))
            .run();
    record = result.getRecords().head();
    assertFalse(record.isSome());

    trx2.addEvent(start, end, captureDeviceID, userIds, mp4, wfPropertiesUpdated, caProperties, Opt.some(true));

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp4.getIdentifier().compact())))
            .run();
    record = result.getRecords().head();
    assertTrue(record.isSome());

    try {
      trx2.commit();
    } catch (SchedulerConflictException e) {
      fail("Pre-conflict detection not working!");
    }

    /* Test transaction collision of two transaction events */

    schedSvc.removeEvent(mp2.getIdentifier().compact());

    MediaPackage mp3 = generateEvent(Opt.<String> none());
    trx.addEvent(start, end, captureDeviceID, userIds, mp3, wfPropertiesUpdated, caProperties, Opt.some(false));

    try {
      trx.commit();
      fail("Pre-conflict detection not working!");
    } catch (SchedulerConflictException e) {
      assertNotNull(e);
    }

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    record = result.getRecords().head();
    assertTrue(record.isSome());

    try {
      schedSvc.getTransaction(trx.getId());
    } catch (NotFoundException e) {
      fail("Transaction found!");
    }

    /* Test transaction collision of two transaction events but one opted out */

    MediaPackage mp5 = (MediaPackage) mp.clone();
    mp5.setIdentifier(new IdImpl("newuuid2"));

    SchedulerTransaction trx3 = schedSvc.createTransaction("optedout2");
    assertEquals("optedout2", trx3.getSource());

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp5.getIdentifier().compact())))
            .run();
    record = result.getRecords().head();
    assertFalse(record.isSome());

    trx3.addEvent(start, end, captureDeviceID, userIds, mp5, wfPropertiesUpdated, caProperties, Opt.some(false));

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp5.getIdentifier().compact())))
            .run();
    record = result.getRecords().head();
    assertTrue(record.isSome());

    MediaPackage mp6 = generateEvent(Opt.<String> none());
    trx.addEvent(start, end, captureDeviceID, userIds, mp6, wfPropertiesUpdated, caProperties, Opt.some(true));

    try {
      trx3.commit();
    } catch (SchedulerConflictException e) {
      fail("Pre-conflict detection not working!");
    }
  }

  @Test
  public void testTransactionCommitConflict() throws Exception {
    Date start = new Date();
    Date end = new Date(System.currentTimeMillis() + 60000);
    String captureDeviceID = "demo";
    String seriesId = "series1";
    Set<String> userIds = new HashSet<>();
    userIds.add("user1");
    userIds.add("user2");
    MediaPackage mp = generateEvent(Opt.<String> none());
    mp.setSeries(seriesId);
    DublinCoreCatalog extendedEvent = generateExtendedEvent(Opt.<String> none(), "extended");
    addDublinCore(Opt.<String> none(), mp, extendedEvent);
    DublinCoreCatalog event = generateEvent(captureDeviceID, start, end);
    addDublinCore(Opt.<String> none(), mp, event);
    addAcl(Opt.<String> none(), mp, acl);
    Map<String, String> caProperties = generateCaptureAgentMetadata("demo");

    SchedulerTransaction trx = schedSvc.createTransaction("new");
    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.<Boolean> none());

    try {
      trx.commit();
    } catch (SchedulerConflictException e) {
      fail("Pre-conflict detection not working!");
    }

    AQueryBuilder query = assetManager.createQuery();
    AResult result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(1, result.getSize());
    Opt<ARecord> record = result.getRecords().head();
    assertTrue(record.isSome());
    assertEquals(new VersionImpl(0), record.get().getSnapshot().get().getVersion());

    schedSvc.updateEvent(mp.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
            Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
            Opt.<Map<String, String>> none(), Opt.some(Opt.some(true)), SchedulerService.ORIGIN);

    // Update with same checksum
    trx = schedSvc.createTransaction("new");
    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.some(true));

    try {
      // works because of same checksum
      trx.commit();
    } catch (SchedulerConflictException e) {
      fail("Pre-conflict detection not working!");
    }

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(1, result.getSize());
    record = result.getRecords().head();
    assertTrue(record.isSome());
    assertEquals(new VersionImpl(1), record.get().getSnapshot().get().getVersion());
    assertTrue(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getBoolean("optout")));
    assertEquals("new",
            record.get().getProperties().apply(org.opencastproject.assetmanager.api.fn.Properties.getString("source")));
    assertEquals("new", record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getString("last_modified_origin")));

    // Update with different checksum
    schedSvc.updateEvent(mp.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
            Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
            Opt.<Map<String, String>> none(), Opt.some(Opt.some(false)), SchedulerService.ORIGIN);

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(1, result.getSize());
    record = result.getRecords().head();
    assertTrue(record.isSome());
    assertEquals(new VersionImpl(1), record.get().getSnapshot().get().getVersion());
    assertFalse(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getBoolean("optout")));
    assertTrue(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getStringOpt("checksum")).isSome());
    assertEquals("new",
            record.get().getProperties().apply(org.opencastproject.assetmanager.api.fn.Properties.getString("source")));
    assertEquals(SchedulerService.ORIGIN, record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getString("last_modified_origin")));

    trx = schedSvc.createTransaction("new");
    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.some(true));

    try {
      // creates conflict
      trx.commit();
    } catch (SchedulerConflictException e) {
      fail("Pre-conflict detection not working!");
    }

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(1, result.getSize());
    record = result.getRecords().head();
    assertTrue(record.isSome());
    assertEquals(new VersionImpl(1), record.get().getSnapshot().get().getVersion());
    assertFalse(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getBoolean("optout")));
    assertTrue(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getStringOpt("last_conflict")).isSome());
    assertEquals("new",
            record.get().getProperties().apply(org.opencastproject.assetmanager.api.fn.Properties.getString("source")));
    assertEquals(SchedulerService.ORIGIN, record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getString("last_modified_origin")));

    // Commit again with no changes
    trx = schedSvc.createTransaction("new");
    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.some(true));

    try {
      // ignores conflict
      trx.commit();
    } catch (SchedulerConflictException e) {
      fail("Pre-conflict detection not working!");
    }

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(1, result.getSize());
    record = result.getRecords().head();
    assertTrue(record.isSome());
    assertEquals(new VersionImpl(1), record.get().getSnapshot().get().getVersion());
    assertFalse(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getBoolean("optout")));
    assertTrue(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getStringOpt("last_conflict")).isSome());
    assertEquals("new",
            record.get().getProperties().apply(org.opencastproject.assetmanager.api.fn.Properties.getString("source")));
    assertEquals(SchedulerService.ORIGIN, record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getString("last_modified_origin")));

    // Commit again with changes
    trx = schedSvc.createTransaction("new");
    trx.addEvent(start, end, "newdevice", userIds, mp, wfProperties, caProperties, Opt.some(true));

    try {
      // override from existing
      // creates new conflict
      testConflictHandler.setStrategy(Strategy.OLD);
      trx.commit();
    } catch (SchedulerConflictException e) {
      fail("Pre-conflict detection not working!");
    }

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(1, result.getSize());
    record = result.getRecords().head();
    assertTrue(record.isSome());
    assertEquals(new VersionImpl(1), record.get().getSnapshot().get().getVersion());
    assertFalse(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getBoolean("optout")));
    assertTrue(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getStringOpt("last_conflict")).isSome());
    assertEquals("new",
            record.get().getProperties().apply(org.opencastproject.assetmanager.api.fn.Properties.getString("source")));
    assertEquals("demo",
            record.get().getProperties().apply(org.opencastproject.assetmanager.api.fn.Properties.getString("agent")));
    assertEquals(SchedulerService.ORIGIN, record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getString("last_modified_origin")));

    // Commit again with new changes
    trx = schedSvc.createTransaction("new");
    trx.addEvent(start, end, "newdevice", userIds, mp, wfProperties, caProperties, Opt.some(false));

    try {
      // override from transaction
      testConflictHandler.setStrategy(Strategy.NEW);
      trx.commit();
    } catch (SchedulerConflictException e) {
      fail("Pre-conflict detection not working!");
    }

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(1, result.getSize());
    record = result.getRecords().head();
    assertTrue(record.isSome());
    assertEquals(new VersionImpl(5), record.get().getSnapshot().get().getVersion());
    assertFalse(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getBoolean("optout")));
    assertFalse(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getStringOpt("last_conflict")).isSome());
    assertEquals("newdevice",
            record.get().getProperties().apply(org.opencastproject.assetmanager.api.fn.Properties.getString("agent")));
    assertEquals("new",
            record.get().getProperties().apply(org.opencastproject.assetmanager.api.fn.Properties.getString("source")));
    assertEquals("new", record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getString("last_modified_origin")));

    // Commit again with new changes
    trx = schedSvc.createTransaction("new2");
    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfProperties, caProperties, Opt.some(true));

    try {
      // merge opt out only and keep agent id
      testConflictHandler.setStrategy(Strategy.MERGED);
      trx.commit();
    } catch (SchedulerConflictException e) {
      fail("Pre-conflict detection not working!");
    }

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(1, result.getSize());
    record = result.getRecords().head();
    assertTrue(record.isSome());
    assertEquals(new VersionImpl(7), record.get().getSnapshot().get().getVersion());
    assertFalse(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getBoolean("optout")));
    assertFalse(record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getStringOpt("last_conflict")).isSome());
    assertEquals(captureDeviceID,
            record.get().getProperties().apply(org.opencastproject.assetmanager.api.fn.Properties.getString("agent")));
    assertEquals("new2",
            record.get().getProperties().apply(org.opencastproject.assetmanager.api.fn.Properties.getString("source")));
    assertEquals("new2", record.get().getProperties()
            .apply(org.opencastproject.assetmanager.api.fn.Properties.getString("last_modified_origin")));
  }

  @Test
  public void testTransactionRollback() throws Exception {
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

    try {
      schedSvc.addEvent(start, end, captureDeviceID, userIds, mp, wfPropertiesUpdated, caProperties,
              Opt.<Boolean> none(), Opt.some(SchedulerService.ORIGIN), SchedulerService.ORIGIN);
    } catch (SchedulerTransactionLockException e) {
      fail("Unable to add event!");
    }

    SchedulerTransaction trx = schedSvc.createTransaction("new");
    assertEquals("new", trx.getSource());

    AQueryBuilder query = assetManager.createQuery();
    AResult result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(1, result.getSize());

    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfPropertiesUpdated, caProperties, Opt.<Boolean> none());

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(2, result.getSize());

    trx.rollback();

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(1, result.getSize());

    try {
      schedSvc.getTransaction(trx.getId());
      fail("Deleted transaction found!");
    } catch (NotFoundException e) {
      assertNotNull(e);
    }

    trx = schedSvc.createTransaction("new");
    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfPropertiesUpdated, caProperties, Opt.<Boolean> none());

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    assertEquals(2, result.getSize());
    assertEquals(new VersionImpl(2), result.getRecords().tail().head2().getSnapshot().get().getVersion());
    assertEquals(new VersionImpl(0), result.getRecords().head2().getSnapshot().get().getVersion());

    try {
      schedSvc.getMediaPackage(mp.getIdentifier().compact());
    } catch (NotFoundException e) {
      fail("Detection of second last element doesn't work!");
    }
  }

  @Test
  public void testTransactionCleanup() throws Exception {
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

    SchedulerTransaction trx = schedSvc.createTransaction("new");
    assertEquals("new", trx.getSource());

    AQueryBuilder query = assetManager.createQuery();
    AResult result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    Opt<ARecord> record = result.getRecords().head();
    assertFalse(record.isSome());

    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfPropertiesUpdated, caProperties, Opt.<Boolean> none());

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    record = result.getRecords().head();
    assertTrue(record.isSome());

    schedSvc.transactionOffsetMillis = 0;
    schedSvc.cleanupTransactions();

    query = assetManager.createQuery();
    result = query
            .select(query.snapshot(), query.properties()).where(query.organizationId()
                    .eq(new DefaultOrganization().getId()).and(query.mediaPackageId(mp.getIdentifier().compact())))
            .run();
    record = result.getRecords().head();
    assertFalse(record.isSome());

    try {
      schedSvc.getTransaction(trx.getId());
      fail("Deleted transaction found!");
    } catch (NotFoundException e) {
      assertNotNull(e);
    }
  }

  @Test
  public void testTransactionConflicts() throws Exception {
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

    try {
      schedSvc.addEvent(start, end, captureDeviceID, userIds, mp, wfPropertiesUpdated, caProperties,
              Opt.<Boolean> none(), Opt.some("new"), SchedulerService.ORIGIN);
    } catch (SchedulerTransactionLockException e) {
      fail("Transaction create lock not working!");
    }

    Assert.assertFalse(schedSvc.hasActiveTransaction(mp.getIdentifier().compact()));

    SchedulerTransaction trx = schedSvc.createTransaction("new");
    assertEquals("new", trx.getSource());

    SchedulerTransaction trx2 = schedSvc.createTransaction("new2");
    assertEquals("new2", trx2.getSource());

    try {
      schedSvc.createTransaction("new");
      fail("Duplicated transaction created!");
    } catch (SchedulerConflictException e) {
      assertNotNull(e);
    }

    try {
      schedSvc.updateEvent(mp.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
              Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
              Opt.<Map<String, String>> none(), Opt.some(Opt.some(false)), SchedulerService.ORIGIN);
      fail("Transaction update lock not working!");
    } catch (SchedulerTransactionLockException e) {
      assertNotNull(e);
    }

    try {
      schedSvc.removeEvent(mp.getIdentifier().compact());
      fail("Transaction delete lock not working!");
    } catch (SchedulerTransactionLockException e) {
      assertNotNull(e);
    }

    try {
      MediaPackage mp2 = generateEvent(Opt.<String> none());
      schedSvc.addEvent(start, end, captureDeviceID, userIds, mp2, wfPropertiesUpdated, caProperties,
              Opt.<Boolean> none(), Opt.some("new"), SchedulerService.ORIGIN);
      fail("Transaction create lock not working!");
    } catch (SchedulerTransactionLockException e) {
      assertNotNull(e);
    }

    SchedulerTransaction trx3 = schedSvc.getTransaction(trx.getId());
    assertEquals(trx, trx3);

    mp = generateEvent(Opt.<String> none());
    event = generateEvent(captureDeviceID, new Date(), end);
    addDublinCore(Opt.<String> none(), mp, event);
    trx.addEvent(start, end, captureDeviceID, userIds, mp, wfPropertiesUpdated, caProperties, Opt.<Boolean> none());

    Assert.assertTrue(schedSvc.hasActiveTransaction(mp.getIdentifier().compact()));

    trx.commit();

    Assert.assertFalse(schedSvc.hasActiveTransaction(mp.getIdentifier().compact()));

    try {
      MediaPackage mp2 = generateEvent(Opt.<String> none());
      Date startDate = new Date(System.currentTimeMillis() + 600000);
      Date endDate = new Date(System.currentTimeMillis() + 660000);
      schedSvc.addEvent(startDate, endDate, captureDeviceID, userIds, mp2, wfPropertiesUpdated, caProperties,
              Opt.<Boolean> none(), Opt.some("new"), SchedulerService.ORIGIN);
    } catch (SchedulerTransactionLockException e) {
      fail("Transaction create lock not working!");
    }
  }

  @Test
  public void testReviewStatus() throws Exception {
    String mediapackageId = "id";
    try {
      schedSvc.updateReviewStatus(mediapackageId, ReviewStatus.UNCONFIRMED);
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    MediaPackage mp = generateEvent(Opt.some(mediapackageId));
    schedSvc.addEvent(new Date(), new Date(System.currentTimeMillis() + 60000), "demo", Collections.<String> emptySet(),
            mp, Collections.<String, String> emptyMap(), Collections.<String, String> emptyMap(), Opt.<Boolean> none(),
            Opt.<String> none(), SchedulerService.ORIGIN);

    schedSvc.updateReviewStatus(mediapackageId, ReviewStatus.UNCONFIRMED);
    Assert.assertEquals(ReviewStatus.UNCONFIRMED, schedSvc.getReviewStatus(mediapackageId));
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

    MediaPackage mp = generateEvent(Opt.some(mpId));

    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "agent",
            Collections.<String> emptySet(), mp, wfPropertiesUpdated, Collections.<String, String> emptyMap(),
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);

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
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);

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
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);

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
            Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);

    // Try using the same old etag. We should get a 200, since the event has changed
    response = restService.getCalendar(device, null, null, request);
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    Assert.assertNotNull(response.getEntity());
    final String secondEtag = (String) response.getMetadata().getFirst(HttpHeaders.ETAG);

    Assert.assertNotNull(secondEtag);
    Assert.assertFalse(etag.equals(secondEtag));

    EasyMock.reset(request);
    EasyMock.expect(request.getHeader("If-None-Match")).andAnswer(new IAnswer<String>() {
      @Override
      public String answer() throws Throwable {
        return secondEtag;
      }
    }).anyTimes();
    EasyMock.replay(request);

    // do opt out update
    schedSvc.updateEvent(mediaPackage.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<String> none(), Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
            Opt.<Map<String, String>> none(), Opt.some(Opt.some(true)), SchedulerService.ORIGIN);
    Assert.assertTrue(schedSvc.isOptOut(mediaPackage.getIdentifier().compact()));

    // Try using the same old etag. We should get a 200, since the event has changed
    response = restService.getCalendar(device, null, null, request);
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    Assert.assertNotNull(response.getEntity());
    String thirdEtag = (String) response.getMetadata().getFirst(HttpHeaders.ETAG);
    Assert.assertNotNull(thirdEtag);
    Assert.assertFalse(secondEtag.equals(thirdEtag));
  }

  @Test
  public void testEventStatus() throws Exception {
    final long currentTime = System.currentTimeMillis();
    final String initialTitle = "Recording 1";
    MediaPackage mediaPackage = generateEvent(Opt.<String> none());
    final DublinCoreCatalog initalEvent = generateEvent("Device A", Opt.<String> none(), Opt.some(initialTitle),
            new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000));
    addDublinCore(Opt.<String> none(), mediaPackage, initalEvent);

    Map<String, String> caProperties = map(tuple("org.opencastproject.workflow.config.archiveOp", "true"),
            tuple("org.opencastproject.workflow.definition", "full"));

    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "Device A",
            Collections.<String> emptySet(), mediaPackage, wfProperties, caProperties, Opt.some(false),
            Opt.<String> none(), SchedulerService.ORIGIN);

    final Map<String, String> initalCaProps = schedSvc
            .getCaptureAgentConfiguration(mediaPackage.getIdentifier().compact());
    checkEvent(mediaPackage.getIdentifier().compact(), initalCaProps, initialTitle);

    Assert.assertFalse(schedSvc.isOptOut(mediaPackage.getIdentifier().compact()));
    Assert.assertFalse(schedSvc.isBlacklisted(mediaPackage.getIdentifier().compact()));

    // do opt out update
    schedSvc.updateEvent(mediaPackage.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<String> none(), Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
            Opt.<Map<String, String>> none(), Opt.some(Opt.some(true)), SchedulerService.ORIGIN);
    Assert.assertTrue(schedSvc.isOptOut(mediaPackage.getIdentifier().compact()));

    // TODO blacklist
    // // do blacklist update
    // schedSvc.updateBlacklist(null);
    // Assert.assertTrue(schedSvc.isBlacklisted(mediaPackage.getIdentifier().compact()));
  }

  @Test
  public void testEventStatusFromSeries() throws Exception {
    EasyMock.reset(seriesService);
    EasyMock.expect(seriesService.isOptOut(EasyMock.anyString())).andReturn(true).once();
    EasyMock.replay(seriesService);

    final long currentTime = System.currentTimeMillis();
    final String initialTitle = "Recording 1";
    MediaPackage mediaPackage = generateEvent(Opt.<String> none());
    mediaPackage.setSeries("series1");
    final DublinCoreCatalog initalEvent = generateEvent("Device A", Opt.<String> none(), Opt.some(initialTitle),
            new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000));
    addDublinCore(Opt.<String> none(), mediaPackage, initalEvent);

    Map<String, String> caProperties = map(tuple("org.opencastproject.workflow.config.archiveOp", "true"),
            tuple("org.opencastproject.workflow.definition", "full"));

    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "Device A",
            Collections.<String> emptySet(), mediaPackage, wfProperties, caProperties, Opt.<Boolean> none(),
            Opt.<String> none(), SchedulerService.ORIGIN);

    Assert.assertTrue(schedSvc.isOptOut(mediaPackage.getIdentifier().compact()));

    // do opt out update
    schedSvc.updateEvent(mediaPackage.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<String> none(), Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
            Opt.<Map<String, String>> none(), Opt.some(Opt.some(false)), SchedulerService.ORIGIN);
    Assert.assertFalse(schedSvc.isOptOut(mediaPackage.getIdentifier().compact()));

    // do opt out update
    schedSvc.updateEvent(mediaPackage.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<String> none(), Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
            Opt.<Map<String, String>> none(), Opt.some(Opt.some(true)), SchedulerService.ORIGIN);
    Assert.assertTrue(schedSvc.isOptOut(mediaPackage.getIdentifier().compact()));
  }

  @Test
  public void testCalendarCutoff() throws Exception {
    long currentTime = System.currentTimeMillis();
    MediaPackage mediaPackageA = generateEvent(Opt.<String> none());
    MediaPackage mediaPackageB = generateEvent(Opt.<String> none());

    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + (60 * 60 * 1000)), "Device A",
            Collections.<String> emptySet(), mediaPackageA, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
    schedSvc.addEvent(new Date(currentTime + (20 * 24 * 60 * 60 * 1000)),
            new Date(currentTime + (20 * 25 * 60 * 60 * 1000)), "Device A", Collections.<String> emptySet(),
            mediaPackageB, wfProperties, Collections.<String, String> emptyMap(), Opt.<Boolean> none(),
            Opt.<String> none(), SchedulerService.ORIGIN);

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
            Collections.<String, String> emptyMap(), Opt.<Boolean> none(), Opt.<String> none(),
            SchedulerService.ORIGIN);

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
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + (60 * 60 * 1000)), "Device B",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);

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
            caMetadata, Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);

    MediaPackage mp = schedSvc.getMediaPackage(mediaPackage.getIdentifier().compact());
    Assert.assertEquals(mediaPackage, mp);

    // test single update
    final String updatedTitle1 = "Recording 2";
    final DublinCoreCatalog updatedEvent1 = generateEvent("Device A", Opt.some(mediaPackage.getIdentifier().compact()),
            Opt.some(updatedTitle1), startDateTime, endTime);
    addDublinCore(Opt.some(catalogId), mediaPackage, updatedEvent1);
    schedSvc.updateEvent(mediaPackage.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<String> none(), Opt.<Set<String>> none(), Opt.some(mediaPackage), Opt.some(wfPropertiesUpdated),
            Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);
    Assert.fail("Schedule should not update a recording that has ended (single)");
  }

  @Test(expected = SchedulerException.class)
  public void testConflictCreation() throws Exception {
    long currentTime = System.currentTimeMillis();
    MediaPackage mediaPackage = generateEvent(Opt.some("1"));

    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + (60 * 60 * 1000)), "Device A",
            Collections.<String> emptySet(), mediaPackage, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
    schedSvc.addEvent(new Date(currentTime + (20 * 24 * 60 * 60 * 1000)),
            new Date(currentTime + (20 * 25 * 60 * 60 * 1000)), "Device A", Collections.<String> emptySet(),
            mediaPackage, wfProperties, Collections.<String, String> emptyMap(), Opt.<Boolean> none(),
            Opt.<String> none(), SchedulerService.ORIGIN);
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
      schedSvc.isOptOut(mediaPackageId);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.isBlacklisted(mediaPackageId);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.getReviewStatus(mediaPackageId);
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
      schedSvc.updateReviewStatus(mediaPackageId, ReviewStatus.CONFIRMED);
      fail();
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    }

    try {
      schedSvc.updateEvent(mediaPackageId, Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
              Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
              Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);
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
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
    schedSvc.updateRecordingState(id, CAPTURING);
    assertEquals(1, schedSvc.getKnownRecordings().size());

    String id2 = "Recording2";
    MediaPackage mediaPackageB = generateEvent(Opt.some(id2));
    schedSvc.addEvent(new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000), "Device B",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
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
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
    schedSvc.addEvent(new Date(currentTime + hours(24)), new Date(currentTime + hours(25)), "Device A",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Opt.some(true), Opt.<String> none(), SchedulerService.ORIGIN);
    schedSvc.addEvent(new Date(currentTime - hours(1)), new Date(currentTime - minutes(10)), "Device C",
            Collections.<String> emptySet(), mediaPackageC, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
    schedSvc.addEvent(new Date(currentTime + seconds(10)), new Date(currentTime + hours(1) + seconds(10)), "Device D",
            Collections.<String> emptySet(), mediaPackageD, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
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
      List<MediaPackage> events = schedSvc.findConflictingEvents("Device A",
              new RRule("FREQ=WEEKLY;BYDAY=SU,MO,TU,WE,TH,FR,SA"), start, new Date(start.getTime() + hours(48)),
              new Long(seconds(36)), TimeZone.getTimeZone("America/Chicago"));
      assertEquals(1, events.size());
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
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
    schedSvc.addEvent(new Date(currentTime + hours(2)), new Date(currentTime + hours(25)), "Device A",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Opt.some(true), Opt.<String> none(), SchedulerService.ORIGIN);
    {
      List<MediaPackage> allEvents = schedSvc.search(Opt.<String> none(), Opt.<Date> none(), Opt.<Date> none(),
              Opt.<Date> none(), Opt.<Date> none());
      assertEquals(2, allEvents.size());
    }

    // Update opted out event to a conflicting time
    schedSvc.updateEvent(mediaPackageB.getIdentifier().compact(), Opt.some(new Date(currentTime)), Opt.<Date> none(),
            Opt.<String> none(), Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
            Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);

    // Update opted out status
    try {
      schedSvc.updateEvent(mediaPackageB.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(),
              Opt.<String> none(), Opt.<Set<String>> none(), Opt.<MediaPackage> none(),
              Opt.<Map<String, String>> none(), Opt.<Map<String, String>> none(), Opt.some(Opt.some(false)),
              SchedulerService.ORIGIN);
      fail("Conflict not detected!");
    } catch (SchedulerConflictException e) {
      Assert.assertNotNull(e);
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
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
    schedSvc.addEvent(new Date(currentTime + hours(24)), new Date(currentTime + hours(25)), "Device A",
            Collections.<String> emptySet(), mediaPackageB, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
    schedSvc.addEvent(new Date(currentTime - hours(1)), new Date(currentTime - minutes(10)), "Device C",
            Collections.<String> emptySet(), mediaPackageC, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
    schedSvc.addEvent(new Date(currentTime + seconds(10)), new Date(currentTime + hours(1) + seconds(10)), "Device D",
            Collections.<String> emptySet(), mediaPackageD, wfProperties, Collections.<String, String> emptyMap(),
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
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
            Collections.<String> emptySet(), mediaPackage, wfProperties, caProperties, Opt.<Boolean> none(),
            Opt.<String> none(), SchedulerService.ORIGIN);

    Map<String, String> initalCaProps = schedSvc.getCaptureAgentConfiguration(mediaPackage.getIdentifier().compact());
    checkEvent(mediaPackage.getIdentifier().compact(), initalCaProps, initialTitle);

    // do single update
    final String updatedTitle1 = "Recording 2";
    final DublinCoreCatalog updatedEvent1 = generateEvent("Device A", Opt.some(mediaPackage.getIdentifier().compact()),
            Opt.some(updatedTitle1), new Date(currentTime + 10 * 1000), new Date(currentTime + 3610000));
    addDublinCore(Opt.some(elementId), mediaPackage, updatedEvent1);

    schedSvc.updateEvent(mediaPackage.getIdentifier().compact(), Opt.<Date> none(), Opt.<Date> none(),
            Opt.<String> none(), Opt.<Set<String>> none(), Opt.some(mediaPackage), Opt.some(wfPropertiesUpdated),
            Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);

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
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);

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
            Opt.some(mediaPackage), Opt.some(wfPropertiesUpdated), Opt.<Map<String, String>> none(),
            Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);

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

  /**
   * Test that opted out and blacklisted events don't end up in the calendar but regular events do.
   *
   * @throws Exception
   */
  @Test
  public void testGetCalendarInputRegularOptedOutBlacklistedExpectsOnlyRegularEvents() throws Exception {
    int optedOutCount = 3;
    int blacklistedCount = 5;
    int bothCount = 7;
    int regularCount = 9;
    String optedOutPrefix = "OptedOut";
    String blacklistedPrefix = "Blacklisted";
    String bothPrefix = "Both";
    String regularPrefix = "Regular";

    List<String> optedOutEvents = createEvents(optedOutPrefix, "DeviceA", optedOutCount, schedSvc, true, false);
    assertEquals(optedOutCount, optedOutEvents.size());
    List<String> blacklistedEvents = createEvents(blacklistedPrefix, "DeviceB", blacklistedCount, schedSvc, false,
            true);
    assertEquals(blacklistedCount, blacklistedEvents.size());
    List<String> bothOptedOutEventsAndBlacklisted = createEvents(bothPrefix, "DeviceC", bothCount, schedSvc, true,
            true);
    assertEquals(bothCount, bothOptedOutEventsAndBlacklisted.size());
    List<String> regularEvents = createEvents(regularPrefix, "DeviceD", regularCount, schedSvc, false, false);
    assertEquals(regularCount, regularEvents.size());

    checkEventStatus(schedSvc, optedOutEvents, true, false);
    checkEventStatus(schedSvc, blacklistedEvents, false, true);
    checkEventStatus(schedSvc, bothOptedOutEventsAndBlacklisted, true, true);
    checkEventStatus(schedSvc, regularEvents, false, false);

    String calendar = schedSvc.getCalendar(Opt.<String> none(), Opt.<String> none(), Opt.<Date> none());

    assertEquals("All of the regular events should be in the calendar.", regularCount + blacklistedCount,
            getCountFromString("BEGIN:VEVENT", calendar));
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
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
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
            Opt.<Boolean> none(), Opt.<String> none(), SchedulerService.ORIGIN);
    {
      final RichAResult r = enrich(q.select(q.snapshot(), q.properties()).run());
      assertEquals("The asset manager should contain one episode", 1, r.getSize());
      assertEquals("Episode ID", mpId, r.getRecords().head2().getMediaPackageId());
      assertFalse("The episode should have some properties", r.getProperties().isEmpty());
    }
    // remove event
    schedSvc.removeEvent(mpId);
    try {
      schedSvc.getMediaPackage(mpId);
      Assert.fail("No media package should be found since it has been deleted before");
    } catch (NotFoundException ignore) {
    }
    {
      final RichAResult r = enrich(q.select(q.snapshot(), q.properties()).run());
      assertTrue("The asset manager should not contain any properties anymore", r.getProperties().isEmpty());
      assertTrue("The asset manager should not contain any episodes anymore", r.getSnapshots().isEmpty());
    }
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

  private void checkEventStatus(SchedulerService schedulerService, List<String> events, boolean optedOut,
          boolean blacklisted) throws NotFoundException, SchedulerException, UnauthorizedException {
    for (String eventId : events) {
      assertEquals(optedOut, schedulerService.isOptOut(eventId));
      assertEquals(false, schedulerService.isBlacklisted(eventId)); // TODO blacklist = blacklisted
    }
  }

  private List<String> createEvents(String titlePrefix, String agent, int number, SchedulerService schedulerService,
          boolean optedout, boolean blacklisted) throws Exception {
    List<String> events = new ArrayList<>();
    long offset = System.currentTimeMillis();
    for (int i = 0; i < number; i++) {
      MediaPackage mp = generateEvent(Opt.<String> none());
      Date startDateTime = new Date(offset + 10 * 1000);
      Date endDateTime = new Date(offset + 3610000);
      offset = endDateTime.getTime();
      final DublinCoreCatalog event = generateEvent(agent, Opt.<String> none(), Opt.some(titlePrefix + "-" + i),
              startDateTime, endDateTime);
      addDublinCore(Opt.<String> none(), mp, event);
      schedulerService.addEvent(startDateTime, endDateTime, agent, Collections.<String> emptySet(), mp, wfProperties,
              Collections.<String, String> emptyMap(), Opt.nul(optedout), Opt.<String> none(), SchedulerService.ORIGIN);
      // TODO blacklisted
      // schedulerServiceImpl.updateBlacklist(null);
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
    final Database db = new Database(penv);
    return new AbstractAssetManager() {
      @Override
      public HttpAssetProvider getHttpAssetProvider() {
        // identity provider
        return new HttpAssetProvider() {
          @Override
          public Snapshot prepareForDelivery(Snapshot snapshot) {
            return snapshot;
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
      public AssetStore getAssetStore() {
        return mkAssetStore();
      }

      @Override
      protected String getCurrentOrgId() {
        return DefaultOrganization.DEFAULT_ORGANIZATION_ID;
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
}
