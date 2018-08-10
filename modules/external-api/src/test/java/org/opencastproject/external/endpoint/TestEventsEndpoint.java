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
package org.opencastproject.external.endpoint;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.opencastproject.index.service.util.CatalogAdapterUtil.getCatalogProperties;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.external.impl.index.ExternalIndex;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.scheduler.api.TechnicalMetadataImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PropertiesUtil;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.osgi.service.cm.ConfigurationException;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Path;

@Path("/")
public class TestEventsEndpoint extends EventsEndpoint {
  public static final String DELETE_CATALOG_TYPE = "deletecatalog";
  public static final String DELETE_EVENT_METADATA = "deleteeventmetdata";
  public static final String ENGAGE_PUBLICATION_ID = "engage-id";
  public static final String INTERNAL_SERVER_ERROR_TYPE = "internalservererror";
  public static final String METADATA_CATALOG_TYPE = "extra";
  public static final String MISSING_ID = "missing";
  public static final String NO_PUBLICATIONS_EVENT = "nopublications";
  public static final String NOT_FOUND_TYPE = "notfoundcatalog";
  public static final String OAIPMH_PUBLICATION_ID = "oaipmh-id";
  public static final String TWO_PUBLICATIONS = "twopublications";
  public static final String UNAUTHORIZED_TYPE = "unauthorizedcatalog";
  public static final String UPDATE_EVENT = "updateevent";
  public static final String METADATA_UPDATE_EVENT = "metadataupdateevent";
  public static final String METADATA_GET_EVENT = "metadatagetevent";
  public static final String SCHEDULING_GET_EVENT = "schedulinggetevent";
  public static final String SCHEDULING_UPDATE_EVENT = "schedulingupdateevent";

  private static Capture<MetadataList> capturedMetadataList1;
  private static Capture<MetadataList> capturedMetadataList2;
  private static Capture<Opt<Date>> capturedStartDate;
  private static Capture<Opt<Date>> capturedEndDate;
  private static Capture<Opt<String>> capturedAgentId;
  private static Capture<Opt<Map<String, String>>> capturedAgentConfig;

  private static Organization defaultOrg = new DefaultOrganization();

  private void setupSecurityService() {
    // Prepare mocked security service
    SecurityService securityService = createNiceMock(SecurityService.class);
    expect(securityService.getOrganization()).andStubReturn(defaultOrg);

    // Replay mocked objects
    replay(securityService);

    setSecurityService(securityService);
  }

  private void setupEventCatalogUIAdapters() throws ConfigurationException {
    // Setup common event catalog
    CommonEventCatalogUIAdapter commonEventCatalogUIAdapter = new CommonEventCatalogUIAdapter();
    Properties episodeCatalogProperties = getCatalogProperties(getClass(), "/episode-catalog.properties");
    commonEventCatalogUIAdapter.updated(PropertiesUtil.toDictionary(episodeCatalogProperties));
    this.setCommonEventCatalogUIAdapter(commonEventCatalogUIAdapter);
    addCatalogUIAdapter(commonEventCatalogUIAdapter);

    // Setup catalog to be deleted.
    EventCatalogUIAdapter deleteAdapter = EasyMock.createMock(EventCatalogUIAdapter.class);
    EasyMock.expect(deleteAdapter.getFlavor()).andReturn(new MediaPackageElementFlavor(DELETE_CATALOG_TYPE, "episode"))
            .anyTimes();
    MetadataCollection collectionMock = EasyMock.createNiceMock(MetadataCollection.class);
    EasyMock.expect(deleteAdapter.getOrganization()).andReturn(defaultOrg.getId()).anyTimes();
    EasyMock.expect(deleteAdapter.getFields(EasyMock.anyObject(MediaPackage.class))).andReturn(null).anyTimes();
    EasyMock.expect(deleteAdapter.getUITitle()).andReturn(null).anyTimes();
    EasyMock.replay(deleteAdapter);
    addCatalogUIAdapter(deleteAdapter);
  }

  public TestEventsEndpoint() throws Exception {
    this.endpointBaseUrl = "https://api.opencast.org";

    ExternalIndex externalIndex = new ExternalIndex();

    IndexService indexService = EasyMock.createMock(IndexService.class);
    EasyMock.expect(indexService.getEvent(MISSING_ID, externalIndex)).andReturn(Opt.<Event> none()).anyTimes();

    SchedulerService schedulerService = EasyMock.createMock(SchedulerService.class);

    /**
     * Delete Metadata external service mocking
     */
    Event deleteMetadataEvent = new Event(DELETE_EVENT_METADATA, defaultOrg.getId());
    MediaPackage deleteMetadataMP = EasyMock.createMock(MediaPackage.class);
    Catalog deleteCatalog1 = EasyMock.createMock(Catalog.class);
    EasyMock.expect(deleteMetadataMP.getCatalogs(new MediaPackageElementFlavor(DELETE_CATALOG_TYPE, "episode")))
            .andReturn(new Catalog[] { deleteCatalog1 }).anyTimes();
    deleteMetadataMP.remove(deleteCatalog1);
    EasyMock.expectLastCall().anyTimes();

    EasyMock.expect(indexService.getEvent(DELETE_EVENT_METADATA, externalIndex))
            .andReturn(Opt.some(deleteMetadataEvent)).anyTimes();
    EasyMock.expect(indexService.getEventMediapackage(deleteMetadataEvent)).andReturn(deleteMetadataMP).anyTimes();

    indexService.removeCatalogByFlavor(deleteMetadataEvent,
            new MediaPackageElementFlavor(DELETE_CATALOG_TYPE, "episode"));
    EasyMock.expectLastCall().anyTimes();

    indexService.removeCatalogByFlavor(deleteMetadataEvent,
            new MediaPackageElementFlavor(INTERNAL_SERVER_ERROR_TYPE, "episode"));
    EasyMock.expectLastCall().andThrow(new IndexServiceException("Problem removing catalog")).anyTimes();

    indexService.removeCatalogByFlavor(deleteMetadataEvent, new MediaPackageElementFlavor(NOT_FOUND_TYPE, "episode"));
    EasyMock.expectLastCall().andThrow(new NotFoundException("Problem finding catalog")).anyTimes();

    indexService.removeCatalogByFlavor(deleteMetadataEvent,
            new MediaPackageElementFlavor(UNAUTHORIZED_TYPE, "episode"));
    EasyMock.expectLastCall().andThrow(new UnauthorizedException("User isn't authorized!")).anyTimes();

    /**
     * Get Publications external service mocking
     */
    // No Pubs
    Event noPublicationsEvent = new Event(NO_PUBLICATIONS_EVENT, defaultOrg.getId());
    MediaPackage noPublicationsMP = EasyMock.createMock(MediaPackage.class);
    EasyMock.expect(noPublicationsMP.getPublications()).andReturn(new Publication[] {}).anyTimes();
    EasyMock.expect(indexService.getEvent(NO_PUBLICATIONS_EVENT, externalIndex))
            .andReturn(Opt.some(noPublicationsEvent)).anyTimes();
    EasyMock.expect(indexService.getEventMediapackage(noPublicationsEvent)).andReturn(noPublicationsMP).anyTimes();
    // Two Pubs
    Event twoPublicationsEvent = new Event(TWO_PUBLICATIONS, defaultOrg.getId());
    MediaPackage twoPublicationsMP = EasyMock.createMock(MediaPackage.class);
    Publication theodulPublication = new PublicationImpl(ENGAGE_PUBLICATION_ID, "EVENTS.EVENTS.DETAILS.PUBLICATIONS.ENGAGE",
            new URI("http://mh-allinone.localdomain/engage/theodul/ui/core.html?id=af1a51ce-fb61-4dae-9d5a-f85b9e4fcc99"),
            MimeType.mimeType("not", "used"));
    Publication oaipmh = new PublicationImpl(OAIPMH_PUBLICATION_ID, "oaipmh",
            new URI("http://mh-allinone.localdomain/oaipmh/default?verb=ListMetadataFormats&identifier=af1a51ce-fb61-4dae-9d5a-f85b9e4fcc99"),
            MimeType.mimeType("not", "used"));
    EasyMock.expect(twoPublicationsMP.getPublications()).andReturn(new Publication[] { theodulPublication, oaipmh })
            .anyTimes();
    EasyMock.expect(indexService.getEvent(TWO_PUBLICATIONS, externalIndex)).andReturn(Opt.some(twoPublicationsEvent))
            .anyTimes();
    EasyMock.expect(indexService.getEventMediapackage(twoPublicationsEvent)).andReturn(twoPublicationsMP).anyTimes();

    /**
     * Update event external service mocking
     */
    capturedMetadataList1 = Capture.newInstance();
    Event updateEvent = new Event(UPDATE_EVENT, defaultOrg.getId());
    EasyMock.expect(indexService.getEvent(UPDATE_EVENT, externalIndex)).andReturn(Opt.some(updateEvent)).anyTimes();
    EasyMock.expect(indexService.updateEventMetadata(eq(UPDATE_EVENT), capture(capturedMetadataList1), eq(externalIndex))).andReturn(null).anyTimes();

    /**
     * Update event metadata external service mocking
     */
    capturedMetadataList2 = Capture.newInstance();
    Event updateEventMetadata = new Event(METADATA_UPDATE_EVENT, defaultOrg.getId());
    updateEventMetadata.setRecordingStartDate("2017-08-29T00:05:00.000Z");
    EasyMock.expect(indexService.getEvent(METADATA_UPDATE_EVENT, externalIndex)).andReturn(Opt.some(updateEventMetadata)).anyTimes();
    EasyMock.expect(indexService.updateEventMetadata(eq(METADATA_UPDATE_EVENT), capture(capturedMetadataList2), eq(externalIndex))).andReturn(null).anyTimes();
    EasyMock.expect(indexService.getEventMediapackage(updateEventMetadata)).andReturn(null).anyTimes();

    /**
     * Get event metadata external service mocking
     */
    Event getEvent = new Event(METADATA_GET_EVENT, defaultOrg.getId());
    getEvent.setRecordingStartDate("2017-08-29T00:05:00.000Z");
    EasyMock.expect(indexService.getEvent(METADATA_GET_EVENT, externalIndex)).andReturn(Opt.some(getEvent)).anyTimes();
    EasyMock.expect(indexService.getEventMediapackage(getEvent)).andReturn(null).anyTimes();

    /**
     * Update event scheduling information external service mocking
     */
    final TechnicalMetadata technicalMetadata1 = new TechnicalMetadataImpl(
        SCHEDULING_UPDATE_EVENT,
        "testAgent23",
        Date.from(Instant.parse("2017-08-28T00:05:00Z")),
        Date.from(Instant.parse("2017-08-28T00:07:00Z")),
        false,
        null,
        null,
        Collections.singletonMap(CaptureParameters.CAPTURE_DEVICE_NAMES, "default1"),
        Opt.none()
    );
    capturedStartDate = Capture.newInstance();
    capturedEndDate = Capture.newInstance();
    capturedAgentId = Capture.newInstance();
    capturedAgentConfig = Capture.newInstance();
    Event updateEventScheduling = new Event(SCHEDULING_UPDATE_EVENT, defaultOrg.getId());
    EasyMock.expect(indexService.getEvent(SCHEDULING_UPDATE_EVENT, externalIndex)).andReturn(Opt.some(updateEventScheduling)).anyTimes();
    EasyMock.expect(schedulerService.getTechnicalMetadata(SCHEDULING_UPDATE_EVENT)).andReturn(technicalMetadata1).anyTimes();
    schedulerService.updateEvent(
        eq(SCHEDULING_UPDATE_EVENT),
        capture(capturedStartDate),
        capture(capturedEndDate),
        capture(capturedAgentId),
        eq(Opt.none()),
        eq(Opt.none()),
        eq(Opt.none()),
        capture(capturedAgentConfig),
        eq(Opt.none()),
        eq(SchedulerService.ORIGIN)
    );
    EasyMock.expectLastCall();

    /**
     * Get event scheduling information external service mocking
     */
    final Event getEventScheduling = new Event(SCHEDULING_GET_EVENT, defaultOrg.getId());
    final TechnicalMetadata technicalMetadata2 = new TechnicalMetadataImpl(
        SCHEDULING_GET_EVENT,
        "testAgent24",
        Date.from(Instant.parse("2017-08-29T00:05:00Z")),
        Date.from(Instant.parse("2017-08-29T00:07:00Z")),
        false,
        null,
        null,
        Collections.singletonMap(CaptureParameters.CAPTURE_DEVICE_NAMES, "default1,default2"),
        Opt.none()
    );
    EasyMock.expect(indexService.getEvent(SCHEDULING_GET_EVENT, externalIndex)).andReturn(Opt.some(getEventScheduling)).anyTimes();
    EasyMock.expect(schedulerService.getTechnicalMetadata(SCHEDULING_GET_EVENT)).andReturn(technicalMetadata2).anyTimes();


    // Replay all mocks
    EasyMock.replay(deleteMetadataMP, indexService, schedulerService, noPublicationsMP, twoPublicationsMP);

    setExternalIndex(externalIndex);
    setIndexService(indexService);
    setSchedulerService(schedulerService);
    setupSecurityService();
    setupEventCatalogUIAdapters();
    Properties properties = new Properties();
    properties.load(IOUtils.toInputStream(IOUtils.toString(getClass().getResource("/events-endpoint.properties"))));
    updated((Hashtable) properties);
  }

  public static Capture<MetadataList> getCapturedMetadataList1() {
    return capturedMetadataList1;
  }

  public static Capture<MetadataList> getCapturedMetadataList2() {
    return capturedMetadataList2;
  }

  public static Capture<Opt<Date>> getCapturedStartDate() {
    return capturedStartDate;
  }

  public static Capture<Opt<Date>> getCapturedEndDate() {
    return capturedEndDate;
  }

  public static Capture<Opt<String>> getCapturedAgentId() {
    return capturedAgentId;
  }

  public static Capture<Opt<Map<String, String>>> getCapturedAgentConfig() {
    return capturedAgentConfig;
  }
}
