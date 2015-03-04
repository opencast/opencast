/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.index.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.exception.InternalServerErrorException;
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
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.identifier.HandleException;
import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Map;

public class IndexServiceImplTest {

  private static final JSONParser parser = new JSONParser();

  /**
   *
   *
   * Test Create Event
   *
   *
   */

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputNullMetadataExpectsIllegalArgumentException() throws IllegalArgumentException,
          InternalServerErrorException, ConfigurationException, MediaPackageException, HandleException, IOException,
          IngestException, ParseException, NotFoundException, SchedulerException, UnauthorizedException {
    String testResourceLocation = "/events/create-event.json";
    String metadataJson = IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation));

    MediaPackage mediapackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.replay(mediapackage);

    IngestService ingestService = EasyMock.createMock(IngestService.class);
    EasyMock.expect(ingestService.createMediaPackage()).andReturn(mediapackage);
    EasyMock.replay(ingestService);

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(ingestService);
    indexServiceImpl.createEvent(null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputEmptyJsonExpectsIllegalArgumentException() throws IllegalArgumentException,
          InternalServerErrorException, ConfigurationException, MediaPackageException, HandleException, IOException,
          IngestException, ParseException, NotFoundException, SchedulerException, UnauthorizedException,
          org.json.simple.parser.ParseException {
    JSONObject metadataJson = (JSONObject) parser.parse("{}");

    MediaPackage mediapackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.replay(mediapackage);

    IngestService ingestService = EasyMock.createMock(IngestService.class);
    EasyMock.expect(ingestService.createMediaPackage()).andReturn(mediapackage);
    EasyMock.replay(ingestService);

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(ingestService);
    indexServiceImpl.createEvent(metadataJson, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputNoSourceExpectsIllegalArgumentException() throws IllegalArgumentException,
          InternalServerErrorException, ConfigurationException, MediaPackageException, HandleException, IOException,
          IngestException, ParseException, NotFoundException, SchedulerException, UnauthorizedException,
          org.json.simple.parser.ParseException {
    String testResourceLocation = "/events/create-event-no-source.json";
    JSONObject metadataJson = (JSONObject) parser.parse(IOUtils.toString(IndexServiceImplTest.class
            .getResourceAsStream(testResourceLocation)));

    MediaPackage mediapackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.replay(mediapackage);

    IngestService ingestService = EasyMock.createMock(IngestService.class);
    EasyMock.expect(ingestService.createMediaPackage()).andReturn(mediapackage);
    EasyMock.replay(ingestService);

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(ingestService);
    indexServiceImpl.createEvent(metadataJson, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputNoProcessingExpectsIllegalArgumentException() throws IllegalArgumentException,
          InternalServerErrorException, ConfigurationException, MediaPackageException, HandleException, IOException,
          IngestException, ParseException, NotFoundException, SchedulerException, UnauthorizedException,
          org.json.simple.parser.ParseException {
    String testResourceLocation = "/events/create-event-no-processing.json";
    JSONObject metadataJson = (JSONObject) parser.parse(IOUtils.toString(IndexServiceImplTest.class
            .getResourceAsStream(testResourceLocation)));

    MediaPackage mediapackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.replay(mediapackage);

    IngestService ingestService = EasyMock.createMock(IngestService.class);
    EasyMock.expect(ingestService.createMediaPackage()).andReturn(mediapackage);
    EasyMock.replay(ingestService);

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(ingestService);
    indexServiceImpl.createEvent(metadataJson, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputNoWorkflowExpectsIllegalArgumentException() throws IllegalArgumentException,
          InternalServerErrorException, ConfigurationException, MediaPackageException, HandleException, IOException,
          IngestException, ParseException, NotFoundException, SchedulerException, UnauthorizedException,
          org.json.simple.parser.ParseException {
    String testResourceLocation = "/events/create-event-no-workflow.json";
    JSONObject metadataJson = (JSONObject) parser.parse(IOUtils.toString(IndexServiceImplTest.class
            .getResourceAsStream(testResourceLocation)));

    MediaPackage mediapackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.replay(mediapackage);

    IngestService ingestService = EasyMock.createMock(IngestService.class);
    EasyMock.expect(ingestService.createMediaPackage()).andReturn(mediapackage);
    EasyMock.replay(ingestService);

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(ingestService);
    indexServiceImpl.createEvent(metadataJson, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateEventInputNoMetadataExpectsIllegalArgumentException() throws IllegalArgumentException,
          InternalServerErrorException, ConfigurationException, MediaPackageException, HandleException, IOException,
          IngestException, ParseException, NotFoundException, SchedulerException, UnauthorizedException,
          org.json.simple.parser.ParseException {
    String testResourceLocation = "/events/create-event-no-metadata.json";
    JSONObject metadataJson = (JSONObject) parser.parse(IOUtils.toString(IndexServiceImplTest.class
            .getResourceAsStream(testResourceLocation)));

    MediaPackage mediapackage = EasyMock.createMock(MediaPackage.class);
    EasyMock.replay(mediapackage);

    IngestService ingestService = EasyMock.createMock(IngestService.class);
    EasyMock.expect(ingestService.createMediaPackage()).andReturn(mediapackage);
    EasyMock.replay(ingestService);

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setIngestService(ingestService);
    indexServiceImpl.createEvent(metadataJson, null);
  }

  @Test
  public void testCreateEventInputNormalExpectsCreatedEvent() throws IllegalArgumentException,
          InternalServerErrorException, ConfigurationException, MediaPackageException, HandleException, IOException,
          IngestException, ParseException, NotFoundException, SchedulerException, UnauthorizedException,
          org.json.simple.parser.ParseException, URISyntaxException {
    String expectedTitle = "Test Event Creation";
    String username = "akm220";
    String org = "org1";
    String[] creators = new String[] {};
    Id mpId = new IdImpl("mp-id");
    String testResourceLocation = "/events/create-event.json";
    JSONObject metadataJson = (JSONObject) parser.parse(IOUtils.toString(IndexServiceImplTest.class
            .getResourceAsStream(testResourceLocation)));
    Capture<Catalog> result = new Capture<Catalog>();
    Capture<String> mediapackageIdResult = new Capture<String>();
    Capture<String> catalogIdResult = new Capture<String>();
    Capture<String> filenameResult = new Capture<String>();
    Capture<InputStream> catalogResult = new Capture<InputStream>();
    Capture<String> mediapackageTitleResult = new Capture<String>();

    // Setup Security Service, Organization and User
    Organization organization = EasyMock.createMock(Organization.class);
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

    Workspace workspace = EasyMock.createMock(Workspace.class);
    EasyMock.expect(
            workspace.put(EasyMock.capture(mediapackageIdResult), EasyMock.capture(catalogIdResult),
                    EasyMock.capture(filenameResult), EasyMock.capture(catalogResult))).andReturn(
            new URI("catalog.xml"));
    EasyMock.replay(workspace);

    // Create Common Event Catalog UI Adapter
    CommonEventCatalogUIAdapter commonEventCatalogUIAdapter = new CommonEventCatalogUIAdapter();
    commonEventCatalogUIAdapter.activate();
    commonEventCatalogUIAdapter.setSecurityService(securityService);
    commonEventCatalogUIAdapter.setWorkspace(workspace);

    // Setup mediapackage.
    MediaPackage mediapackage = EasyMock.createMock(MediaPackage.class);
    mediapackage.add(EasyMock.capture(result));
    EasyMock.expectLastCall();
    EasyMock.expect(mediapackage.getCatalogs(EasyMock.anyObject(MediaPackageElementFlavor.class))).andReturn(
            new Catalog[] {});
    EasyMock.expect(mediapackage.getIdentifier()).andReturn(mpId).anyTimes();
    EasyMock.expect(mediapackage.getCreators()).andReturn(creators);
    mediapackage.addCreator("");
    EasyMock.expectLastCall();
    mediapackage.setTitle(EasyMock.capture(mediapackageTitleResult));
    EasyMock.expectLastCall();
    EasyMock.expect(mediapackage.getElements()).andReturn(new MediaPackageElement[] {}).anyTimes();
    EasyMock.expect(mediapackage.getCatalogs(EasyMock.anyObject(MediaPackageElementFlavor.class)))
            .andReturn(new Catalog[] {}).anyTimes();
    EasyMock.replay(mediapackage);

    // Setup ingest service.
    WorkflowInstance workflowInstance = EasyMock.createMock(WorkflowInstance.class);
    IngestService ingestService = EasyMock.createMock(IngestService.class);
    EasyMock.expect(ingestService.createMediaPackage()).andReturn(mediapackage);
    EasyMock.expect(
            ingestService.addCatalog(EasyMock.anyObject(InputStream.class), EasyMock.anyObject(String.class),
                    EasyMock.anyObject(MediaPackageElementFlavor.class), EasyMock.anyObject(MediaPackage.class)))
            .andReturn(mediapackage);
    EasyMock.expect(
            ingestService.ingest(EasyMock.anyObject(MediaPackage.class), EasyMock.anyObject(String.class),
                    EasyMock.<Map<String, String>> anyObject())).andReturn(workflowInstance);
    EasyMock.replay(ingestService);

    // Setup Authorization Service
    Tuple<MediaPackage, Attachment> returnValue = new Tuple<MediaPackage, Attachment>(mediapackage, null);
    AuthorizationService authorizationService = EasyMock.createMock(AuthorizationService.class);
    EasyMock.expect(
            authorizationService.setAcl(EasyMock.anyObject(MediaPackage.class), EasyMock.anyObject(AclScope.class),
                    EasyMock.anyObject(AccessControlList.class))).andReturn(returnValue);
    EasyMock.replay(authorizationService);

    // Run Test
    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setAuthorizationService(authorizationService);
    indexServiceImpl.setIngestService(ingestService);
    indexServiceImpl.setCommonEventCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexServiceImpl.addCatalogUIAdapter(commonEventCatalogUIAdapter);
    indexServiceImpl.setSecurityService(securityService);
    indexServiceImpl.setWorkspace(workspace);
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

  /**
   *
   * Test Put Event
   *
   *
   */

  @Test(expected = NotFoundException.class)
  public void testUpdateEventInputNoEventExpectsNotFound() throws IOException, IllegalArgumentException,
          InternalServerErrorException, NotFoundException, UnauthorizedException, SearchIndexException {
    String org = "org1";
    String testResourceLocation = "/events/update-event.json";
    String eventId = "event-1";
    String metadataJson = IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation));

    SearchQuery query = EasyMock.createMock(SearchQuery.class);
    EasyMock.expect(query.getLimit()).andReturn(100);
    EasyMock.expect(query.getOffset()).andReturn(0);
    EasyMock.replay(query);

    SearchResult<Event> result = new SearchResultImpl<Event>(query, 0, 0);

    AbstractSearchIndex abstractIndex = EasyMock.createMock(AbstractSearchIndex.class);
    EasyMock.expect(abstractIndex.getByQuery(EasyMock.anyObject(EventSearchQuery.class))).andReturn(result);
    EasyMock.replay(abstractIndex);

    // Setup Security Service, Organization and User
    Organization organization = EasyMock.createMock(Organization.class);
    EasyMock.expect(organization.getId()).andReturn(org).anyTimes();
    EasyMock.replay(organization);
    User user = EasyMock.createMock(User.class);
    EasyMock.expect(user.getOrganization()).andReturn(organization);
    EasyMock.replay(user);
    SecurityService securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user);
    EasyMock.replay(securityService);

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setSecurityService(securityService);
    indexServiceImpl.updateAllEventMetadata(eventId, metadataJson, abstractIndex);
  }

  public void testUpdateEvent() throws IOException, IllegalArgumentException, InternalServerErrorException,
          NotFoundException, UnauthorizedException, SearchIndexException {
    String org = "org1";
    String testResourceLocation = "/events/update-event.json";
    String eventId = "event-1";
    String metadataJson = IOUtils.toString(IndexServiceImplTest.class.getResourceAsStream(testResourceLocation));
    String expectedTitle = "Updated Title";
    String expectedDescription = "Unset Description";
    // Setup search results
    SearchQuery query = EasyMock.createMock(SearchQuery.class);
    EasyMock.expect(query.getLimit()).andReturn(100);
    EasyMock.expect(query.getOffset()).andReturn(0);
    EasyMock.replay(query);

    Event event = new Event();
    event.setTitle("Other Title");
    event.setDescription(expectedDescription);
    SearchResultItemImpl<Event> searchResultItem = new SearchResultItemImpl<Event>(1.0, event);
    SearchResultImpl<Event> result = new SearchResultImpl<Event>(query, 0, 0);
    result.addResultItem(searchResultItem);

    AbstractSearchIndex abstractIndex = EasyMock.createMock(AbstractSearchIndex.class);
    EasyMock.expect(abstractIndex.getByQuery(EasyMock.anyObject(EventSearchQuery.class))).andReturn(result);
    EasyMock.replay(abstractIndex);

    // Setup Security Service, Organization and User
    Organization organization = EasyMock.createMock(Organization.class);
    EasyMock.expect(organization.getId()).andReturn(org).anyTimes();
    EasyMock.replay(organization);
    User user = EasyMock.createMock(User.class);
    EasyMock.expect(user.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(user);
    SecurityService securityService = EasyMock.createMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(organization);
    EasyMock.expect(securityService.getUser()).andReturn(user);
    EasyMock.replay(securityService);

    IndexServiceImpl indexServiceImpl = new IndexServiceImpl();
    indexServiceImpl.setSecurityService(securityService);
    indexServiceImpl.updateAllEventMetadata(eventId, metadataJson, abstractIndex);
    Assert.assertEquals("The title should have been updated.", expectedTitle, event.getTitle());
    Assert.assertEquals("The description should be the same.", expectedDescription, event.getDescription());
    Assert.assertNull("The subject should still be null.", event.getSubject());
  }

}
