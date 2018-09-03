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

package org.opencastproject.index.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.index.service.catalog.adapter.DublinCoreMetadataCollection;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchQuery;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.impl.SearchResultImpl;
import org.opencastproject.matterhorn.search.impl.SearchResultItemImpl;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.CatalogImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.identifier.HandleException;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.impl.jpa.JpaOrganization;
import org.opencastproject.security.impl.jpa.JpaUser;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PropertiesUtil;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.VCell;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.joda.time.DateTimeConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class IndexServiceImplTest {

  private static final JSONParser parser = new JSONParser();

  /**
   * Tests for the method calculatePeriods
   */
  private final TimeZone utc = TimeZone.getTimeZone("UTC");
  private final TimeZone jst = TimeZone.getTimeZone("JST"); // Japan Standard Time (UTC +9)
  private final TimeZone pst = TimeZone.getTimeZone("PST"); // Alaska Standard Time (UTC -8)
  private final TimeZone cet = TimeZone.getTimeZone("CET"); // Alaska Standard Time (UTC +2)

  private JpaOrganization organization = new JpaOrganization("org-id", "Organization", null, null, null, null, null);

  private User user1 = new JpaUser("user1", "pass", organization, "User 1", "email1", "provider1", true);
  private User user2 = new JpaUser("user2", "pass", organization, "User 2", "email1", "provider1", true);
  private User user3 = new JpaUser("user3", "pass", organization, null, "email1", "provider1", true);

  private UserDirectoryService noUsersUserDirectoryService;

  @Before
  public void before() {
    noUsersUserDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(noUsersUserDirectoryService.loadUser(EasyMock.anyString())).andStubReturn(null);
    EasyMock.replay(noUsersUserDirectoryService);
  }

  private CaptureAgentStateService setupCaptureAgentStateService() throws NotFoundException {
    // Setup capture agent state service
    CaptureAgentStateService captureAgentStateService = EasyMock.createNiceMock(CaptureAgentStateService.class);
    Properties agentProperties = new Properties();
    agentProperties.put("capture.device.timezone", "Europe/Zurich");
    EasyMock.expect(captureAgentStateService.getAgentConfiguration(EasyMock.anyString())).andReturn(agentProperties)
            .anyTimes();
    EasyMock.replay(captureAgentStateService);
    return captureAgentStateService;
  }

  private IngestService setupIngestServiceWithMediaPackage()
          throws IngestException, MediaPackageException, HandleException, IOException, NotFoundException {
    MediaPackage mediapackage = EasyMock.createNiceMock(MediaPackage.class);
    EasyMock.replay(mediapackage);
    return setupIngestService(mediapackage, Capture.<InputStream> newInstance());
  }

  private IngestService setupIngestService(MediaPackage mediapackage, Capture<InputStream> captureInputStream)
          throws MediaPackageException, HandleException, IOException, IngestException, NotFoundException {
    // Setup ingest service.
    WorkflowInstance workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    IngestService ingestService = EasyMock.createMock(IngestService.class);
    EasyMock.expect(ingestService.createMediaPackage()).andReturn(mediapackage).anyTimes();
    EasyMock.expect(ingestService.addTrack(EasyMock.anyObject(InputStream.class), EasyMock.anyString(),
            EasyMock.anyObject(MediaPackageElementFlavor.class), EasyMock.anyObject(MediaPackage.class))).andReturn(mediapackage)
                    .anyTimes();
    EasyMock.expect(ingestService.addCatalog(EasyMock.capture(captureInputStream), EasyMock.anyObject(String.class),
            EasyMock.anyObject(MediaPackageElementFlavor.class), EasyMock.anyObject(MediaPackage.class)))
            .andReturn(mediapackage).anyTimes();
    EasyMock.expect(ingestService.addAttachment(EasyMock.capture(captureInputStream), EasyMock.anyObject(String.class),
            EasyMock.anyObject(MediaPackageElementFlavor.class), EasyMock.anyObject(MediaPackage.class)))
            .andReturn(mediapackage).anyTimes();
    EasyMock.expect(ingestService.ingest(EasyMock.anyObject(MediaPackage.class), EasyMock.anyObject(String.class),
            EasyMock.<Map<String, String>> anyObject())).andReturn(workflowInstance).anyTimes();
    EasyMock.replay(ingestService);
    return ingestService;
  }

  private SecurityService setupSecurityService(String username, String org) {
    // Setup Security Service, Organization and User
    Organization organization = EasyMock.createNiceMock(Organization.class);
    EasyMock.expect(organization.getId()).andReturn(org).anyTimes();
    EasyMock.replay(organization);

    User user = EasyMock.createMock(User.class);
    EasyMock.expect(user.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.expect(user.getUsername()).andReturn(username);
    EasyMock.replay(user);

    SecurityService securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user);
    EasyMock.replay(securityService);
    return securityService;
  }

  private Tuple<CommonEventCatalogUIAdapter, VCell<Option<MetadataCollection>>> setupCommonCatalogUIAdapter(
          Workspace workspace) throws org.osgi.service.cm.ConfigurationException {
    // Create Common Event Catalog UI Adapter
    final VCell<Option<MetadataCollection>> metadataCell = VCell.ocell();
    CommonEventCatalogUIAdapter commonEventCatalogUIAdapter = new CommonEventCatalogUIAdapter() {
      @Override
      public Catalog storeFields(MediaPackage mediaPackage, MetadataCollection metadata) {
        metadataCell.set(Option.some(metadata));
        return super.storeFields(mediaPackage, metadata);
      }
    };

    Properties episodeCatalogProperties = new Properties();
    InputStream in = null;
    try {
      in = getClass().getResourceAsStream("/episode-catalog.properties");
      episodeCatalogProperties.load(in);
    } catch (IOException e) {
      throw new ComponentException(e);
    } finally {
      IoSupport.closeQuietly(in);
    }

    commonEventCatalogUIAdapter.updated(PropertiesUtil.toDictionary(episodeCatalogProperties));
    commonEventCatalogUIAdapter.setWorkspace(workspace);
    return Tuple.tuple(commonEventCatalogUIAdapter, metadataCell);
  }

  /**
   *
   *
   * Test Create Event
   *
   *
   */

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputNullMetadataExpectsIllegalArgumentException() throws IllegalArgumentException,
          IndexServiceException, ConfigurationException, MediaPackageException, HandleException, IOException,
          IngestException, ParseException, NotFoundException, SchedulerException, UnauthorizedException {
    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(setupIngestServiceWithMediaPackage());
    indexServiceImpl.createEvent(null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputEmptyJsonExpectsIllegalArgumentException()
          throws IllegalArgumentException, IndexServiceException, ConfigurationException, MediaPackageException,
          HandleException, IOException, IngestException, ParseException, NotFoundException, SchedulerException,
          UnauthorizedException, org.json.simple.parser.ParseException {
    JSONObject metadataJson = (JSONObject) parser.parse("{}");

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(setupIngestServiceWithMediaPackage());
    indexServiceImpl.createEvent(metadataJson, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputNoSourceExpectsIllegalArgumentException()
          throws IllegalArgumentException, IndexServiceException, ConfigurationException, MediaPackageException,
          HandleException, IOException, IngestException, ParseException, NotFoundException, SchedulerException,
          UnauthorizedException, org.json.simple.parser.ParseException {
    String testResourceLocation = "/events/create-event-no-source.json";
    JSONObject metadataJson = (JSONObject) parser
            .parse(IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation)));

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(setupIngestServiceWithMediaPackage());
    indexServiceImpl.createEvent(metadataJson, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputNoProcessingExpectsIllegalArgumentException()
          throws IllegalArgumentException, IndexServiceException, ConfigurationException, MediaPackageException,
          HandleException, IOException, IngestException, ParseException, NotFoundException, SchedulerException,
          UnauthorizedException, org.json.simple.parser.ParseException {
    String testResourceLocation = "/events/create-event-no-processing.json";
    JSONObject metadataJson = (JSONObject) parser
            .parse(IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation)));

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(setupIngestServiceWithMediaPackage());
    indexServiceImpl.createEvent(metadataJson, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputNoWorkflowExpectsIllegalArgumentException()
          throws IllegalArgumentException, IndexServiceException, ConfigurationException, MediaPackageException,
          HandleException, IOException, IngestException, ParseException, NotFoundException, SchedulerException,
          UnauthorizedException, org.json.simple.parser.ParseException {
    String testResourceLocation = "/events/create-event-no-workflow.json";
    JSONObject metadataJson = (JSONObject) parser
            .parse(IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation)));

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(setupIngestServiceWithMediaPackage());
    indexServiceImpl.createEvent(metadataJson, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputNoMetadataExpectsIllegalArgumentException()
          throws IllegalArgumentException, IndexServiceException, ConfigurationException, MediaPackageException,
          HandleException, IOException, IngestException, ParseException, NotFoundException, SchedulerException,
          UnauthorizedException, org.json.simple.parser.ParseException {
    String testResourceLocation = "/events/create-event-no-metadata.json";
    JSONObject metadataJson = (JSONObject) parser
            .parse(IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation)));

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(setupIngestServiceWithMediaPackage());
    indexServiceImpl.createEvent(metadataJson, null);
  }

  @Test
  public void testCreateEventInputNoWorkflowConfigurationExpectsCreatedEvent() throws Exception {
    String expectedTitle = "Test Event Creation";
    String username = "akm220";
    String org = "mh_default_org";
    String[] creators = new String[] {};
    Id mpId = new IdImpl("mp-id");
    String testResourceLocation = "/events/create-event-no-workflow-configuration.json";
    JSONObject metadataJson = (JSONObject) parser
            .parse(IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation)));
    Capture<Catalog> result = EasyMock.newCapture();
    Capture<String> mediapackageIdResult = EasyMock.newCapture();
    Capture<String> catalogIdResult = EasyMock.newCapture();
    Capture<String> filenameResult = EasyMock.newCapture();
    Capture<InputStream> catalogResult = EasyMock.newCapture();
    Capture<String> mediapackageTitleResult = EasyMock.newCapture();

    SecurityService securityService = setupSecurityService(username, org);

    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.put(EasyMock.capture(mediapackageIdResult), EasyMock.capture(catalogIdResult),
            EasyMock.capture(filenameResult), EasyMock.capture(catalogResult))).andReturn(new URI("catalog.xml"));
    EasyMock.replay(workspace);

    CommonEventCatalogUIAdapter commonEventCatalogUIAdapter = setupCommonCatalogUIAdapter(workspace).getA();

    // Setup mediapackage.
    MediaPackage mediapackage = EasyMock.createMock(MediaPackage.class);
    mediapackage.add(EasyMock.capture(result));
    EasyMock.expectLastCall();
    EasyMock.expect(mediapackage.getIdentifier()).andReturn(mpId).anyTimes();
    EasyMock.expect(mediapackage.getCreators()).andReturn(creators);
    mediapackage.addCreator("");
    EasyMock.expectLastCall();
    mediapackage.setTitle(EasyMock.capture(mediapackageTitleResult));
    EasyMock.expectLastCall();
    EasyMock.expect(mediapackage.getElements()).andReturn(new MediaPackageElement[] {}).anyTimes();
    EasyMock.expect(mediapackage.getCatalogs(EasyMock.anyObject(MediaPackageElementFlavor.class)))
            .andReturn(new Catalog[] {}).anyTimes();
    EasyMock.expect(mediapackage.getSeries()).andReturn(null).anyTimes();
    mediapackage.setSeries(EasyMock.anyString());
    EasyMock.expectLastCall();
    mediapackage.setSeriesTitle(EasyMock.anyString());
    EasyMock.expectLastCall();
    EasyMock.replay(mediapackage);

    AssetManager assetManager = EasyMock.createMock(AssetManager.class);

    IngestService ingestService = setupIngestService(mediapackage, Capture.<InputStream> newInstance());

    // Setup Authorization Service
    Tuple<MediaPackage, Attachment> returnValue = new Tuple<MediaPackage, Attachment>(mediapackage, null);
    AuthorizationService authorizationService = EasyMock.createMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.setAcl(EasyMock.anyObject(MediaPackage.class),
            EasyMock.anyObject(AclScope.class), EasyMock.anyObject(AccessControlList.class))).andReturn(returnValue);
    EasyMock.replay(authorizationService);

    // Run Test
    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setAuthorizationService(setupAuthorizationService(mediapackage));
    indexServiceImpl.setIngestService(ingestService);
    indexServiceImpl.setCommonEventCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexServiceImpl.addCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexServiceImpl.setUserDirectoryService(noUsersUserDirectoryService);
    indexServiceImpl.setSecurityService(securityService);
    indexServiceImpl.setWorkspace(workspace);
    indexServiceImpl.setAssetManager(assetManager);
    indexServiceImpl.createEvent(metadataJson, mediapackage);

    assertTrue("The catalog must be added to the mediapackage", result.hasCaptured());
    assertEquals("The catalog should have been added to the correct mediapackage", mpId.toString(),
            mediapackageIdResult.getValue());
    assertTrue("The catalog should have a new id", catalogIdResult.hasCaptured());
    assertTrue("The catalog should have a new filename", filenameResult.hasCaptured());
    assertTrue("The catalog should have been added to the input stream", catalogResult.hasCaptured());
    assertTrue("The mediapackage should have had its title updated", catalogResult.hasCaptured());
    assertEquals("The mediapackage title should have been updated.", expectedTitle, mediapackageTitleResult.getValue());
    assertTrue("The catalog should have been created", catalogResult.hasCaptured());
  }

  @Test
  public void testCreateEventInputNormalExpectsCreatedEvent() throws Exception {
    String expectedTitle = "Test Event Creation";
    String username = "akm220";
    String org = "mh_default_org";
    String[] creators = new String[] {};
    Id mpId = new IdImpl("mp-id");
    String testResourceLocation = "/events/create-event.json";
    JSONObject metadataJson = (JSONObject) parser
            .parse(IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation)));
    Capture<Catalog> result = EasyMock.newCapture();
    Capture<String> mediapackageIdResult = EasyMock.newCapture();
    Capture<String> catalogIdResult = EasyMock.newCapture();
    Capture<String> filenameResult = EasyMock.newCapture();
    Capture<InputStream> catalogResult = EasyMock.newCapture();
    Capture<String> mediapackageTitleResult = EasyMock.newCapture();

    SecurityService securityService = setupSecurityService(username, org);

    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.put(EasyMock.capture(mediapackageIdResult), EasyMock.capture(catalogIdResult),
            EasyMock.capture(filenameResult), EasyMock.capture(catalogResult))).andReturn(new URI("catalog.xml"));
    EasyMock.replay(workspace);

    // Create Common Event Catalog UI Adapter
    CommonEventCatalogUIAdapter commonEventCatalogUIAdapter = setupCommonCatalogUIAdapter(workspace).getA();

    // Setup mediapackage.
    MediaPackage mediapackage = EasyMock.createMock(MediaPackage.class);
    mediapackage.add(EasyMock.capture(result));
    EasyMock.expectLastCall();
    EasyMock.expect(mediapackage.getCatalogs(EasyMock.anyObject(MediaPackageElementFlavor.class)))
            .andReturn(new Catalog[] {});
    EasyMock.expect(mediapackage.getIdentifier()).andReturn(mpId).anyTimes();
    EasyMock.expect(mediapackage.getCreators()).andReturn(creators);
    mediapackage.addCreator("");
    EasyMock.expectLastCall();
    mediapackage.setTitle(EasyMock.capture(mediapackageTitleResult));
    EasyMock.expectLastCall();
    EasyMock.expect(mediapackage.getElements()).andReturn(new MediaPackageElement[] {}).anyTimes();
    EasyMock.expect(mediapackage.getCatalogs(EasyMock.anyObject(MediaPackageElementFlavor.class)))
            .andReturn(new Catalog[] {}).anyTimes();
    EasyMock.expect(mediapackage.getSeries()).andReturn(null).anyTimes();
    mediapackage.setSeries(EasyMock.anyString());
    mediapackage.setSeriesTitle(EasyMock.anyString());
    EasyMock.expectLastCall();
    EasyMock.replay(mediapackage);

    IngestService ingestService = setupIngestService(mediapackage, Capture.<InputStream> newInstance());

    // Setup Authorization Service
    Tuple<MediaPackage, Attachment> returnValue = new Tuple<MediaPackage, Attachment>(mediapackage, null);
    AuthorizationService authorizationService = EasyMock.createMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.setAcl(EasyMock.anyObject(MediaPackage.class),
            EasyMock.anyObject(AclScope.class), EasyMock.anyObject(AccessControlList.class))).andReturn(returnValue);
    EasyMock.replay(authorizationService);

    AssetManager assetManager = EasyMock.createMock(AssetManager.class);
    EasyMock.expect(
            assetManager.takeSnapshot(EasyMock.eq(AssetManager.DEFAULT_OWNER), EasyMock.anyObject(MediaPackage.class)))
            .andReturn(null);
    EasyMock.expect(assetManager.setProperty(EasyMock.anyObject(Property.class))).andReturn(true).anyTimes();
    EasyMock.replay(assetManager);

    // Run Test
    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setAuthorizationService(setupAuthorizationService(mediapackage));
    indexServiceImpl.setIngestService(ingestService);
    indexServiceImpl.setCommonEventCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexServiceImpl.addCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexServiceImpl.setUserDirectoryService(noUsersUserDirectoryService);
    indexServiceImpl.setSecurityService(securityService);
    indexServiceImpl.setWorkspace(workspace);
    indexServiceImpl.setAssetManager(assetManager);
    indexServiceImpl.createEvent(metadataJson, mediapackage);

    assertTrue("The catalog must be added to the mediapackage", result.hasCaptured());
    assertEquals("The catalog should have been added to the correct mediapackage", mpId.toString(),
            mediapackageIdResult.getValue());
    assertTrue("The catalog should have a new id", catalogIdResult.hasCaptured());
    assertTrue("The catalog should have a new filename", filenameResult.hasCaptured());
    assertTrue("The catalog should have been added to the input stream", catalogResult.hasCaptured());
    assertTrue("The mediapackage should have had its title updated", catalogResult.hasCaptured());
    assertEquals("The mediapackage title should have been updated.", expectedTitle, mediapackageTitleResult.getValue());
    assertTrue("The catalog should have been created", catalogResult.hasCaptured());
  }

  @Test
  public void testCreateEventInputNormalExpectsCreatedScheduledEvent() throws Exception {
    String expectedTitle = "Test Event Creation";
    String username = "akm220";
    String org = "mh_default_org";
    String[] creators = new String[] {};
    Id mpId = new IdImpl("mp-id");
    String testResourceLocation = "/events/create-scheduled-event.json";
    JSONObject metadataJson = (JSONObject) parser
            .parse(IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation)));
    Capture<Catalog> result = EasyMock.newCapture();
    Capture<String> mediapackageIdResult = EasyMock.newCapture();
    Capture<String> catalogIdResult = EasyMock.newCapture();
    Capture<String> filenameResult = EasyMock.newCapture();
    Capture<InputStream> catalogResult = EasyMock.newCapture();
    Capture<String> mediapackageTitleResult = EasyMock.newCapture();

    SecurityService securityService = setupSecurityService(username, org);

    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.put(EasyMock.capture(mediapackageIdResult), EasyMock.capture(catalogIdResult),
            EasyMock.capture(filenameResult), EasyMock.capture(catalogResult))).andReturn(new URI("catalog.xml"));
    EasyMock.replay(workspace);

    // Create Common Event Catalog UI Adapter
    CommonEventCatalogUIAdapter commonEventCatalogUIAdapter = setupCommonCatalogUIAdapter(workspace).getA();

    // Setup mediapackage.
    MediaPackage mediapackage = EasyMock.createMock(MediaPackage.class);
    mediapackage.add(EasyMock.capture(result));
    EasyMock.expectLastCall();
    EasyMock.expect(mediapackage.getCatalogs(EasyMock.anyObject(MediaPackageElementFlavor.class)))
            .andReturn(new Catalog[] {});
    EasyMock.expect(mediapackage.getIdentifier()).andReturn(mpId).anyTimes();
    EasyMock.expect(mediapackage.getCreators()).andReturn(creators);
    mediapackage.addCreator("");
    EasyMock.expectLastCall();
    mediapackage.setTitle(EasyMock.capture(mediapackageTitleResult));
    EasyMock.expectLastCall();
    EasyMock.expect(mediapackage.getElements()).andReturn(new MediaPackageElement[] {}).anyTimes();
    EasyMock.expect(mediapackage.getCatalogs(EasyMock.anyObject(MediaPackageElementFlavor.class)))
            .andReturn(new Catalog[] {}).anyTimes();
    EasyMock.expect(mediapackage.getSeries()).andReturn(null).anyTimes();
    mediapackage.setSeries(EasyMock.anyString());
    EasyMock.expectLastCall();
    mediapackage.setDate(EasyMock.anyObject(Date.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mediapackage);

    IngestService ingestService = setupIngestService(mediapackage, Capture.<InputStream> newInstance());

    // Setup Authorization Service
    Tuple<MediaPackage, Attachment> returnValue = new Tuple<MediaPackage, Attachment>(mediapackage, null);
    AuthorizationService authorizationService = EasyMock.createMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.setAcl(EasyMock.anyObject(MediaPackage.class),
            EasyMock.anyObject(AclScope.class), EasyMock.anyObject(AccessControlList.class))).andReturn(returnValue);
    EasyMock.replay(authorizationService);

    CaptureAgentStateService captureAgentStateService = setupCaptureAgentStateService();

    Capture<Date> captureStart = EasyMock.newCapture();
    Capture<Date> captureEnd = EasyMock.newCapture();
    SchedulerService schedulerService = EasyMock.createNiceMock(SchedulerService.class);
    schedulerService.addEvent(EasyMock.capture(captureStart), EasyMock.capture(captureEnd), EasyMock.anyString(),
            EasyMock.<Set<String>> anyObject(), EasyMock.anyObject(MediaPackage.class),
            EasyMock.<Map<String, String>> anyObject(), EasyMock.<Map<String, String>> anyObject(),
            EasyMock.<Opt<Boolean>> anyObject(), EasyMock.<Opt<String>> anyObject(), EasyMock.anyString());
    EasyMock.expectLastCall().once();
    EasyMock.replay(schedulerService);

    // Run Test
    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setAuthorizationService(setupAuthorizationService(mediapackage));
    indexServiceImpl.setIngestService(ingestService);
    indexServiceImpl.setCommonEventCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexServiceImpl.addCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexServiceImpl.setSecurityService(securityService);
    indexServiceImpl.setUserDirectoryService(noUsersUserDirectoryService);
    indexServiceImpl.setWorkspace(workspace);
    indexServiceImpl.setCaptureAgentStateService(captureAgentStateService);
    indexServiceImpl.setSchedulerService(schedulerService);
    String scheduledEvent = indexServiceImpl.createEvent(metadataJson, mediapackage);
    Assert.assertEquals(mediapackage.getIdentifier().compact(), scheduledEvent);

    assertTrue("The catalog must be added to the mediapackage", result.hasCaptured());
    assertEquals("The catalog should have been added to the correct mediapackage", mpId.toString(),
            mediapackageIdResult.getValue());
    assertTrue("The catalog should have a new id", catalogIdResult.hasCaptured());
    assertTrue("The catalog should have a new filename", filenameResult.hasCaptured());
    assertTrue("The catalog should have been added to the input stream", catalogResult.hasCaptured());
    assertTrue("The mediapackage should have had its title updated", catalogResult.hasCaptured());
    assertEquals("The mediapackage title should have been updated.", expectedTitle, mediapackageTitleResult.getValue());
    assertTrue("The catalog should have been created", catalogResult.hasCaptured());
    assertTrue(captureStart.hasCaptured());
    assertTrue(captureEnd.hasCaptured());
    Assert.assertEquals(new Date(DateTimeSupport.fromUTC("2008-03-16T14:00:00Z")), captureStart.getValue());
    Assert.assertEquals(new Date(DateTimeSupport.fromUTC("2008-03-16T14:01:00Z")), captureEnd.getValue());
  }

  private AuthorizationService setupAuthorizationService(MediaPackage mediapackage) {
    // Setup Authorization Service
    Tuple<MediaPackage, Attachment> returnValue = new Tuple<>(mediapackage, null);
    AuthorizationService authorizationService = EasyMock.createMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.setAcl(EasyMock.anyObject(MediaPackage.class),
            EasyMock.anyObject(AclScope.class), EasyMock.anyObject(AccessControlList.class))).andReturn(returnValue)
            .anyTimes();
    EasyMock.replay(authorizationService);
    return authorizationService;
  }

  @Test
  public void testAddAssetsToMp() throws org.json.simple.parser.ParseException, IOException, ConfigurationException, MediaPackageException, HandleException, IngestException, NotFoundException {
    MediaPackage mediapackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    JSONArray assetMetadata =  (JSONArray) new JSONParser().parse("[{\"id\":\"attachment_attachment_notes\", "
      + "\"title\": \"class handout notes\","
      + "\"flavorType\": \"attachment\","
      + "\"flavorSubType\": \"notes\","
      + "\"type\": \"attachment\"}]");

    // a test asset input stream
    List<String> assetList = new LinkedList<String>();
    assetList.add("attachment_attachment_notes");
    MediaPackageElementFlavor elemflavor = new MediaPackageElementFlavor("attachment_attachment_notes", "*");
    MediaPackageElementFlavor newElemflavor = new MediaPackageElementFlavor("attachment", "notes");

    // Set up the mock Ingest Service's attachment
    Attachment attachment = new AttachmentImpl();
    attachment.setFlavor(elemflavor);
    mediapackage.add(attachment);

    // Run Test
    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(setupIngestService(mediapackage, Capture.<InputStream> newInstance()));
    mediapackage = indexServiceImpl.updateMpAssetFlavor(assetList, mediapackage, assetMetadata, true);
    assertTrue("The mediapackage attachment has the updated flavor", mediapackage.getAttachments(newElemflavor).length == 1);
  }

  @Test
  public void testCreateEventInputNormalExpectsCreatedRecurringEvent() throws Exception {
    String expectedTitle = "Test Event Creation";
    String username = "akm220";
    String org = "mh_default_org";
    String[] creators = new String[] {};
    Id mpId = new IdImpl("mp-id");
    String testResourceLocation = "/events/create-recurring-event.json";
    JSONObject metadataJson = (JSONObject) parser
            .parse(IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation)));
    Capture<String> mediapackageIdResult = EasyMock.newCapture();
    Capture<String> catalogIdResult = EasyMock.newCapture();
    Capture<String> filenameResult = EasyMock.newCapture();
    Capture<InputStream> catalogResult = EasyMock.newCapture();
    Capture<String> mediapackageTitleResult = EasyMock.newCapture();

    SecurityService securityService = setupSecurityService(username, org);

    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.put(EasyMock.capture(mediapackageIdResult), EasyMock.capture(catalogIdResult),
            EasyMock.capture(filenameResult), EasyMock.capture(catalogResult))).andReturn(new URI("catalog.xml"));
    EasyMock.expect(workspace.read(getClass().getResource("/dublincore.xml").toURI()))
            .andAnswer(() -> getClass().getResourceAsStream("/dublincore.xml")).anyTimes();
    EasyMock.replay(workspace);

    // Create Common Event Catalog UI Adapter
    CommonEventCatalogUIAdapter commonEventCatalogUIAdapter = setupCommonCatalogUIAdapter(workspace).getA();

    // Setup mediapackage.
    MediaPackage mediapackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(mediapackage.clone()).andReturn(mediapackage).anyTimes();
    EasyMock.expect(mediapackage.getSeries()).andReturn(null).anyTimes();
    EasyMock.expect(mediapackage.getCatalogs(EasyMock.anyObject(MediaPackageElementFlavor.class)))
            .andReturn(new Catalog[] { CatalogImpl.fromURI(getClass().getResource("/dublincore.xml").toURI()) });
    EasyMock.expect(mediapackage.getIdentifier()).andReturn(mpId).anyTimes();
    EasyMock.expect(mediapackage.getCreators()).andReturn(creators);
    mediapackage.addCreator("");
    EasyMock.expectLastCall();
    mediapackage.setTitle(EasyMock.capture(mediapackageTitleResult));
    EasyMock.expectLastCall().once();
    mediapackage.setTitle(EasyMock.anyString());
    EasyMock.expectLastCall().times(15);
    EasyMock.expect(mediapackage.getElements()).andReturn(new MediaPackageElement[] {}).anyTimes();
    EasyMock.expect(mediapackage.getCatalogs(EasyMock.anyObject(MediaPackageElementFlavor.class)))
            .andReturn(new Catalog[] {}).anyTimes();
    mediapackage.setIdentifier(EasyMock.anyObject(Id.class));
    EasyMock.expectLastCall().anyTimes();
    mediapackage.setSeries(EasyMock.anyString());
    mediapackage.setSeriesTitle(EasyMock.anyString());
    EasyMock.expectLastCall();
    mediapackage.setDate(EasyMock.anyObject(Date.class));
    EasyMock.expectLastCall();
    EasyMock.replay(mediapackage);

    CaptureAgentStateService captureAgentStateService = setupCaptureAgentStateService();

    // Setup scheduler service
    Capture<Date> recurrenceStart = EasyMock.newCapture();
    Capture<Date> recurrenceEnd = EasyMock.newCapture();
    Capture<RRule> rrule = EasyMock.newCapture();
    Capture duration = EasyMock.newCapture();
    Capture<TimeZone> tz = EasyMock.newCapture();

    Capture<Date> schedStart = EasyMock.newCapture();
    Capture<Date> schedEnd = EasyMock.newCapture();
    Capture<RRule> schedRRule = EasyMock.newCapture();
    Capture schedDuration = EasyMock.newCapture();
    Capture<TimeZone> schedTz = EasyMock.newCapture();

    Capture<MediaPackage> mp = EasyMock.newCapture();
    SchedulerService schedulerService = EasyMock.createNiceMock(SchedulerService.class);
    //Look up the expected periods
    EasyMock.expect(
            schedulerService.calculatePeriods(EasyMock.capture(rrule), EasyMock.capture(recurrenceStart),
                    EasyMock.capture(recurrenceEnd), EasyMock.captureLong(duration), EasyMock.capture(tz))).
            andAnswer(new IAnswer<List<Period>>() {
              @Override
              public List<Period> answer() throws Throwable {
                return calculatePeriods(rrule.getValue(), recurrenceStart.getValue(), recurrenceEnd.getValue(), (Long) duration.getValue(), tz.getValue());
              }
            }).anyTimes();
    //The actual scheduling
    EasyMock.expect(
    schedulerService.addMultipleEvents(
            EasyMock.capture(schedRRule), EasyMock.capture(schedStart), EasyMock.capture(schedEnd),
            EasyMock.captureLong(schedDuration), EasyMock.capture(schedTz), EasyMock.anyString(),
            EasyMock.<Set<String>>anyObject(), EasyMock.capture(mp), EasyMock.<Map<String, String>>anyObject(),
            EasyMock.<Map<String, String>>anyObject(), EasyMock.<Opt<Boolean>>anyObject(),
            EasyMock.<Opt<String>>anyObject(), EasyMock.anyString())).
            andAnswer(new IAnswer<Map<String, Period>>() {
              @Override
              public Map<String, Period> answer() throws Throwable {
                List<Period> periods = calculatePeriods(schedRRule.getValue(), schedStart.getValue(), schedEnd.getValue(), (Long) schedDuration.getValue(), schedTz.getValue());
                Map<String, Period> mapping = new LinkedHashMap<>();
                int counter = 0;
                for (Period p : periods) {
                  mapping.put(new IdImpl(UUID.randomUUID().toString()).compact(), p);
                }
                return mapping;
              }
            }).anyTimes();
    EasyMock.replay(schedulerService);

    // Run Test
    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setAuthorizationService(setupAuthorizationService(mediapackage));
    indexServiceImpl.setIngestService(setupIngestService(mediapackage, Capture.<InputStream> newInstance()));
    indexServiceImpl.setCommonEventCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexServiceImpl.addCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexServiceImpl.setSecurityService(securityService);
    indexServiceImpl.setUserDirectoryService(noUsersUserDirectoryService);
    indexServiceImpl.setWorkspace(workspace);
    indexServiceImpl.setCaptureAgentStateService(captureAgentStateService);
    indexServiceImpl.setSchedulerService(schedulerService);
    String scheduledEvents = indexServiceImpl.createEvent(metadataJson, mediapackage);
    String[] ids = StringUtils.split(scheduledEvents, ",");
    //We should have as many scheduled events as we do periods
    Assert.assertTrue(ids.length == calculatePeriods(rrule.getValue(), recurrenceStart.getValue(), recurrenceEnd.getValue(), (Long) duration.getValue(), tz.getValue()).size());

    assertEquals("The catalog should have been added to the correct mediapackage", mpId.toString(),
            mediapackageIdResult.getValue());
    assertTrue("The catalog should have a new id", catalogIdResult.hasCaptured());
    assertTrue("The catalog should have a new filename", filenameResult.hasCaptured());
    assertTrue("The catalog should have been added to the input stream", catalogResult.hasCaptured());
    assertTrue("The mediapackage should have had its title updated", catalogResult.hasCaptured());
    assertEquals("The mediapackage title should have been updated.", expectedTitle, mediapackageTitleResult.getValue());
    assertTrue("The catalog should have been created", catalogResult.hasCaptured());
    //Assert that the start and end recurrence dates captured, along with the duration and recurrence rule
    //This is all used by the scheduling calculation, but not the actual scheduling call
    assertTrue(recurrenceStart.hasCaptured());
    assertTrue(recurrenceEnd.hasCaptured());
    assertTrue(duration.hasCaptured());
    assertTrue(rrule.hasCaptured());
    //Assert that the scheduling call has its necessary data
    assertTrue(schedStart.hasCaptured());
    assertTrue(schedEnd.hasCaptured());
    assertTrue(schedDuration.hasCaptured());
    assertTrue(schedRRule.hasCaptured());
    assertTrue(schedTz.hasCaptured());
    List<Period> pCheck = calculatePeriods(schedRRule.getValue(), schedStart.getValue(), schedEnd.getValue(), (Long) schedDuration.getValue(), schedTz.getValue());
    List<Period> pExpected = calculatePeriods(rrule.getValue(), recurrenceStart.getValue(), recurrenceEnd.getValue(), (Long) duration.getValue(), tz.getValue());

    //Assert that the first capture time is the same as the recurrence start
    assertEquals(pExpected.get(0).getStart(), pCheck.get(0).getStart());
    //Assert that the end of the last capture time is the same as the recurrence end
    assertEquals(pExpected.get(pExpected.size() - 1).getEnd(), pCheck.get(pCheck.size() - 1).getEnd());
  }

  /**
   *
   * Test Put Event
   *
   *
   */

  @Test(expected = NotFoundException.class)
  public void testUpdateEventInputNoEventExpectsNotFound()
          throws IOException, IllegalArgumentException, IndexServiceException, NotFoundException, UnauthorizedException,
          SearchIndexException, WorkflowDatabaseException {
    String username = "username";
    String org = "org1";
    String testResourceLocation = "/events/update-event.json";
    String eventId = "event-1";
    String metadataJson = IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation));

    SearchQuery query = EasyMock.createMock(SearchQuery.class);
    EasyMock.expect(query.getLimit()).andReturn(100);
    EasyMock.expect(query.getOffset()).andReturn(0);
    EasyMock.replay(query);

    SearchResult<Event> result = new SearchResultImpl<>(query, 0, 0);

    AbstractSearchIndex abstractIndex = EasyMock.createMock(AbstractSearchIndex.class);
    EasyMock.expect(abstractIndex.getByQuery(EasyMock.anyObject(EventSearchQuery.class))).andReturn(result);
    EasyMock.replay(abstractIndex);

    SecurityService securityService = setupSecurityService(username, org);

    IndexServiceImpl indexService = new IndexServiceImpl();
    indexService.setSecurityService(securityService);
    indexService.updateAllEventMetadata(eventId, metadataJson, abstractIndex);
  }

  @Test
  public void testUpdateMediaPackageMetadata() throws Exception {
    // mock/initialize dependencies
    String username = "user1";
    String org = "mh_default_org";
    String testResourceLocation = "/events/update-event.json";
    String metadataJson = IOUtils.toString(getClass().getResourceAsStream(testResourceLocation));
    MetadataCollection metadataCollection = new DublinCoreMetadataCollection();
    metadataCollection.addField(MetadataField.createTextMetadataField(
            "title", Opt.some("title"), "EVENTS.EVENTS.DETAILS.METADATA.TITLE", false, true, Opt.none(), Opt.none(),
            Opt.none(), Opt.none(), Opt.none()));
    metadataCollection.addField(MetadataField.createTextLongMetadataField(
            "creator", Opt.some("creator"), "EVENTS.EVENTS.DETAILS.METADATA.PRESENTERS", false, false, Opt.none(),
            Opt.none(), Opt.none(), Opt.none(), Opt.none()));
    metadataCollection.addField(MetadataField.createTextMetadataField(
            "isPartOf", Opt.some("isPartOf"), "EVENTS.EVENTS.DETAILS.METADATA.SERIES", false, false, Opt.none(),
            Opt.none(), Opt.none(), Opt.none(), Opt.none()));
    MetadataList metadataList = new MetadataList(metadataCollection, metadataJson);
    String eventId = "event-1";
    Event event = new Event(eventId, org);
    event.setTitle("Test Event 1");
    SearchQuery query = EasyMock.createMock(SearchQuery.class);
    EasyMock.expect(query.getLimit()).andReturn(100);
    EasyMock.expect(query.getOffset()).andReturn(0);
    EasyMock.replay(query);
    SearchResultItemImpl<Event> searchResultItem = new SearchResultItemImpl<>(1.0, event);
    SearchResultImpl<Event> searchResult = new SearchResultImpl<>(query, 0, 0);
    searchResult.addResultItem(searchResultItem);
    SecurityService securityService = setupSecurityService(username, org);
    AbstractSearchIndex index = EasyMock.createMock(AbstractSearchIndex.class);
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder()
            .loadFromXml(getClass().getResourceAsStream("/events/update-event-mp.xml"));
    EasyMock.expect(index.getByQuery(EasyMock.anyObject(EventSearchQuery.class))).andReturn(searchResult);
    EasyMock.replay(index);
    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(workspace.put(EasyMock.anyString(), EasyMock.anyString(), EasyMock.anyString(),
            EasyMock.anyObject())).andReturn(getClass().getResource("/dublincore.xml").toURI()).anyTimes();
    EasyMock.expect(workspace.read(EasyMock.anyObject())).andAnswer(
            () -> getClass().getResourceAsStream("/dublincore.xml")).anyTimes();
    EasyMock.replay(workspace);
    CommonEventCatalogUIAdapter commonEventCatalogUIAdapter = setupCommonCatalogUIAdapter(workspace).getA();
    // Using scheduler as the source of the media package here.
    SchedulerService schedulerService = EasyMock.createMock(SchedulerService.class);
    EasyMock.expect(schedulerService.getMediaPackage(EasyMock.anyString())).andReturn(mp);
    Capture<Opt<MediaPackage>> mpCapture = new Capture<>();
    schedulerService.updateEvent(EasyMock.anyString(), EasyMock.anyObject(Opt.class),
            EasyMock.anyObject(Opt.class), EasyMock.anyObject(Opt.class), EasyMock.anyObject(Opt.class),
            EasyMock.capture(mpCapture),
            EasyMock.anyObject(Opt.class), EasyMock.anyObject(Opt.class), EasyMock.anyObject(Opt.class),
            EasyMock.anyString());
    EasyMock.expectLastCall();
    EasyMock.replay(schedulerService);
    SeriesService seriesService = EasyMock.createMock(SeriesService.class);
    DublinCoreCatalog seriesDC = DublinCores.read(getClass().getResourceAsStream("/events/update-event-series.xml"));
    EasyMock.expect(seriesService.getSeries(EasyMock.anyString())).andReturn(seriesDC);
    EasyMock.expect(seriesService.getSeriesAccessControl(EasyMock.anyString())).andReturn(null);
    EasyMock.expect(seriesService.getSeriesElements(EasyMock.anyString())).andReturn(Opt.none());
    EasyMock.replay(seriesService);

    // create service
    IndexServiceImpl indexService = new IndexServiceImpl();
    indexService.setSecurityService(securityService);
    indexService.setSchedulerService(schedulerService);
    indexService.setCommonEventCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexService.addCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexService.setSeriesService(seriesService);
    indexService.setWorkspace(workspace);
    MetadataList updateEventMetadata = indexService.updateEventMetadata(org, metadataList, index);

    Assert.assertTrue(mpCapture.hasCaptured());
    Assert.assertEquals("series-1", mp.getSeries());
    Assert.assertEquals(1, mp.getCatalogs(MediaPackageElements.SERIES).length);
  }

  @Test
  public void calculateDaysChange() throws ParseException {
    Calendar start;
    Calendar end;
    long durationMillis;
    String days;
    List<Period> periods;

    // JST
    start = Calendar.getInstance(jst);
    start.set(2016, 2, 25, 22, 0);
    end = Calendar.getInstance(jst);
    end.set(2016, 2, 29, start.get(Calendar.HOUR_OF_DAY), 5);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TU,FR,SA,SU"; // --> Still the same day when switch to UTC (22-9)

    periods = generatePeriods(jst, start, end, days, durationMillis);
    assertEquals(5, periods.size());

    // PST
    start = Calendar.getInstance(pst);
    start.set(2016, 2, 25, 22, 0);
    end = Calendar.getInstance(pst);
    end.set(2016, 2, 29, start.get(Calendar.HOUR_OF_DAY), 5);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TU,WE,SA,SU"; // --> A day after when switching to UTC (22+8)

    periods = generatePeriods(pst, start, end, days, durationMillis);
    assertEquals(5, periods.size());

    // CET
    start = Calendar.getInstance(cet);
    start.set(2016, 2, 25, 0, 5);
    end = Calendar.getInstance(cet);
    end.set(2016, 2, 29, start.get(Calendar.HOUR_OF_DAY), 10);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TH,FR,SA,SU"; // --> A day before when switch to UCT (0-2)

    periods = generatePeriods(cet, start, end, days, durationMillis);
    assertEquals(5, periods.size());
  }

  @Test
  public void calculateDSTChange() throws ParseException {
    Calendar start;
    Calendar end;
    long durationMillis;
    String days;
    List<Period> periods;

    // CET
    TimeZone.setDefault(cet);
    start = Calendar.getInstance(cet);
    start.set(2016, 2, 24, 0, 5);
    end = Calendar.getInstance(cet);
    end.set(2016, 2, 29, start.get(Calendar.HOUR_OF_DAY), 10);
    durationMillis = (end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE)) * 60 * 1000;
    days = "MO,TH,FR,SA,SU";

    periods = generatePeriods(cet, start, end, days, durationMillis);
    assertEquals(5, periods.size());

    TimeZone.setDefault(cet);
    for (Period d : periods) {
      DateTime dEnd = d.getEnd();

      Date date = new Date(dEnd.getTime());
      Calendar instance = Calendar.getInstance();
      instance.setTime(date);

      assertEquals(0, instance.get(Calendar.HOUR_OF_DAY));
    }

  }

  private MetadataField<Iterable<String>> createCreatorMetadataField(Iterable<String> value) {
    MetadataField<Iterable<String>> creator = new MetadataField<>();
    creator.setInputId(DublinCore.PROPERTY_CREATOR.getLocalName());
    creator.setValue(value);
    return creator;
  }

  @Test
  public void updatePresenters() throws IOException, org.osgi.service.cm.ConfigurationException {
    String nonUser1 = "Non User 1";
    String nonUser2 = "Non User 2";
    String nonUser3 = "Non User 3";

    Properties eventProperties = new Properties();
    InputStream in = getClass().getResourceAsStream("/episode-catalog.properties");
    eventProperties.load(in);
    in.close();

    Dictionary<String, String> properties = PropertiesUtil.toDictionary(eventProperties);

    SecurityService securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andStubReturn(organization);
    EasyMock.replay(securityService);

    UserDirectoryService userDirectoryService = EasyMock.createMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(user1.getUsername())).andStubReturn(user1);
    EasyMock.expect(userDirectoryService.loadUser(user2.getUsername())).andStubReturn(user2);
    EasyMock.expect(userDirectoryService.loadUser(user3.getUsername())).andStubReturn(user3);

    EasyMock.expect(userDirectoryService.loadUser(nonUser1)).andStubReturn(null);
    EasyMock.expect(userDirectoryService.loadUser(nonUser2)).andStubReturn(null);
    EasyMock.expect(userDirectoryService.loadUser(nonUser3)).andStubReturn(null);
    EasyMock.replay(userDirectoryService);

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    CommonEventCatalogUIAdapter commonEventCatalogUIAdapter = new CommonEventCatalogUIAdapter();
    commonEventCatalogUIAdapter.updated(properties);
    indexServiceImpl.setCommonEventCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexServiceImpl.setUserDirectoryService(userDirectoryService);
    indexServiceImpl.setSecurityService(securityService);

    MetadataCollection metadata = commonEventCatalogUIAdapter.getRawFields();

    // Possible presenter combinations
    MetadataField<Iterable<String>> emptyUpdatedPresenter = createCreatorMetadataField(new ArrayList<String>());

    ArrayList<String> oneNonUserList = new ArrayList<>();
    oneNonUserList.add(nonUser1);
    MetadataField<Iterable<String>> nonUserUpdatedPresenter = createCreatorMetadataField(oneNonUserList);

    ArrayList<String> multiNonUserList = new ArrayList<>();
    multiNonUserList.add(nonUser1);
    multiNonUserList.add(nonUser2);
    multiNonUserList.add(nonUser3);
    MetadataField<Iterable<String>> multiNonUserUpdatedPresenter = createCreatorMetadataField(multiNonUserList);

    ArrayList<String> oneUserList = new ArrayList<>();
    oneUserList.add(user1.getUsername());
    MetadataField<Iterable<String>> userUpdatedPresenter = createCreatorMetadataField(oneUserList);

    ArrayList<String> multiUserList = new ArrayList<>();
    multiUserList.add(user1.getUsername());
    multiUserList.add(user2.getUsername());
    multiUserList.add(user3.getUsername());
    MetadataField<Iterable<String>> multiUserUpdatedPresenter = createCreatorMetadataField(multiUserList);

    ArrayList<String> mixedUserList = new ArrayList<>();
    mixedUserList.add(user1.getUsername());
    mixedUserList.add(nonUser1);
    mixedUserList.add(user2.getUsername());
    mixedUserList.add(nonUser2);
    mixedUserList.add(nonUser3);
    mixedUserList.add(user3.getUsername());
    MetadataField<Iterable<String>> mixedPresenters = createCreatorMetadataField(mixedUserList);
    ArrayList<String> userFullNames = new ArrayList<>();
    userFullNames.add(user1.getName());
    userFullNames.add(user2.getName());
    userFullNames.add(user3.getUsername());

    // Empty presenters
    metadata.removeField(metadata.getOutputFields().get(DublinCore.PROPERTY_CREATOR.getLocalName()));
    metadata.addField(emptyUpdatedPresenter);
    Tuple<List<String>, Set<String>> updatedPresenters = indexServiceImpl.getTechnicalPresenters(metadata);
    assertTrue("The presenters dublincore metadata should be empty", updatedPresenters.getA().isEmpty());
    assertTrue("The technical presenters should be empty", updatedPresenters.getB().isEmpty());

    // Non-user presenter
    metadata.removeField(metadata.getOutputFields().get(DublinCore.PROPERTY_CREATOR.getLocalName()));
    metadata.addField(nonUserUpdatedPresenter);
    updatedPresenters = indexServiceImpl.getTechnicalPresenters(metadata);
    assertTrue("There should be one presenter", updatedPresenters.getA().size() == 1);
    assertTrue("There should be no technical presenters", updatedPresenters.getB().isEmpty());

    // Multi non-user presenter
    metadata.removeField(metadata.getOutputFields().get(DublinCore.PROPERTY_CREATOR.getLocalName()));
    metadata.addField(multiNonUserUpdatedPresenter);
    updatedPresenters = indexServiceImpl.getTechnicalPresenters(metadata);
    assertTrue("There should be three presenters", updatedPresenters.getA().size() == 3);
    assertTrue("The value for technical presenters should be empty", updatedPresenters.getB().isEmpty());

    // User presenter
    metadata.removeField(metadata.getOutputFields().get(DublinCore.PROPERTY_CREATOR.getLocalName()));
    metadata.addField(userUpdatedPresenter);
    updatedPresenters = indexServiceImpl.getTechnicalPresenters(metadata);
    assertTrue("There should be one presenter", updatedPresenters.getA().size() == 1);
    assertEquals("The one presenter should have the user's full name", "User 1", updatedPresenters.getA().get(0));
    assertTrue("The one technical presenter", updatedPresenters.getB().size() == 1);
    assertEquals("The one technical presenter has the correct username", "user1",
            updatedPresenters.getB().iterator().next());

    // Multi user presenter
    metadata.removeField(metadata.getOutputFields().get(DublinCore.PROPERTY_CREATOR.getLocalName()));
    metadata.addField(multiUserUpdatedPresenter);
    updatedPresenters = indexServiceImpl.getTechnicalPresenters(metadata);
    assertTrue("There should be three presenters", updatedPresenters.getA().size() == 3);
    assertTrue("There should be three technical presenters", updatedPresenters.getB().size() == 3);
    assertTrue("The list of technical presenters should contain all of the user names",
            updatedPresenters.getB().containsAll(multiUserList));

    // Mixed non-user and user presenters
    metadata.removeField(metadata.getOutputFields().get(DublinCore.PROPERTY_CREATOR.getLocalName()));
    metadata.addField(mixedPresenters);
    updatedPresenters = indexServiceImpl.getTechnicalPresenters(metadata);
    assertTrue("There should be six presenters", updatedPresenters.getA().size() == 6);
    assertTrue("There should be three technical presenters", updatedPresenters.getB().size() == 3);
    assertTrue("The list of presenters should contain all of the non-user names",
            updatedPresenters.getA().containsAll(multiNonUserList));
    assertTrue("The list of presenters should contain all of the user full names",
            updatedPresenters.getA().containsAll(userFullNames));
    assertTrue("The list of technical presenters should contain all of the usernames",
            updatedPresenters.getB().containsAll(multiUserList));
  }

  private List<Period> generatePeriods(TimeZone tz, Calendar start, Calendar end, String days, Long duration)
          throws ParseException {
    Calendar utcDate = Calendar.getInstance(utc);
    utcDate.setTime(start.getTime());
    RRule rRule = new RRule(generateRule(days, utcDate.get(Calendar.HOUR_OF_DAY), utcDate.get(Calendar.MINUTE)));
    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    return calculatePeriods(rRule, start.getTime(), end.getTime(), duration, tz);
  }

  private String generateRule(String days, int hour, int minute) {
    return String.format("FREQ=WEEKLY;BYDAY=%s;BYHOUR=%d;BYMINUTE=%d", days, hour, minute);
  }

  //NOTE: Do not modify this without making the same modifications to the copy of this method in Util in the scheduler service
  //I would have moved this to an abstract class in the scheduler-api bundle, but that would introduce a circular dependency :(
  public List<Period> calculatePeriods(RRule rrule, Date start, Date end, long duration, TimeZone tz) {
    final TimeZone timeZone = TimeZone.getDefault();
    final TimeZone utc = TimeZone.getTimeZone("UTC");
    TimeZone.setDefault(tz);
    net.fortuna.ical4j.model.DateTime periodStart = new net.fortuna.ical4j.model.DateTime(start);
    net.fortuna.ical4j.model.DateTime periodEnd = new net.fortuna.ical4j.model.DateTime();

    Calendar endCalendar = Calendar.getInstance(utc);
    endCalendar.setTime(end);
    Calendar calendar = Calendar.getInstance(utc);
    calendar.setTime(periodStart);
    calendar.set(Calendar.DAY_OF_MONTH, endCalendar.get(Calendar.DAY_OF_MONTH));
    calendar.set(Calendar.MONTH, endCalendar.get(Calendar.MONTH));
    calendar.set(Calendar.YEAR, endCalendar.get(Calendar.YEAR));
    periodEnd.setTime(calendar.getTime().getTime() + duration);
    duration = duration % (DateTimeConstants.MILLIS_PER_DAY);

    List<Period> events = new LinkedList<>();

    TimeZone.setDefault(utc);
    for (Object date : rrule.getRecur().getDates(periodStart, periodEnd, net.fortuna.ical4j.model.parameter.Value.DATE_TIME)) {
      Date d = (Date) date;
      Calendar cDate = Calendar.getInstance(utc);

      // Adjust for DST, if start of event
      if (tz.inDaylightTime(periodStart)) { // Event starts in DST
        if (!tz.inDaylightTime(d)) { // Date not in DST?
          d.setTime(d.getTime() + tz.getDSTSavings()); // Adjust for Fall back one hour
        }
      } else { // Event doesn't start in DST
        if (tz.inDaylightTime(d)) {
          d.setTime(d.getTime() - tz.getDSTSavings()); // Adjust for Spring forward one hour
        }
      }
      cDate.setTime(d);

      TimeZone.setDefault(timeZone);
      Period p = new Period(new net.fortuna.ical4j.model.DateTime(cDate.getTime()),
              new net.fortuna.ical4j.model.DateTime(cDate.getTimeInMillis() + duration));
      events.add(p);
      TimeZone.setDefault(utc);
    }
    TimeZone.setDefault(timeZone);
    return events;
  }
}
