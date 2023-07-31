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
package org.opencastproject.external.endpoint;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.opencastproject.index.service.util.CatalogAdapterUtil.getCatalogProperties;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.elasticsearch.index.ElasticsearchIndex;
import org.opencastproject.elasticsearch.index.objects.event.Event;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataList;
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
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.data.Opt;

import org.easymock.Capture;
import org.easymock.EasyMock;

import java.io.InputStream;
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
  public static final String TRACK_UPDATE_EVENT = "trackupdateevent";

  private static Capture<MetadataList> capturedMetadataList1;
  private static Capture<MetadataList> capturedMetadataList2;
  private static Capture<Opt<Date>> capturedStartDate;
  private static Capture<Opt<Date>> capturedEndDate;
  private static Capture<Opt<String>> capturedAgentId;
  private static Capture<Opt<Map<String, String>>> capturedAgentConfig;
  private static Capture<MediaPackage> capturedMediaPackage;

  private static Organization defaultOrg = new DefaultOrganization();

  private void setupSecurityService() {
    // Prepare mocked security service
    SecurityService securityService = createNiceMock(SecurityService.class);
    expect(securityService.getOrganization()).andStubReturn(defaultOrg);

    // Replay mocked objects
    replay(securityService);

    setSecurityService(securityService);
  }

  public TestEventsEndpoint() throws Exception {
    this.endpointBaseUrl = "https://api.opencast.org";

    ElasticsearchIndex elasticsearchIndex = new ElasticsearchIndex();

    IndexService indexService = EasyMock.createMock(IndexService.class);
    EasyMock.expect(indexService.getEvent(MISSING_ID, elasticsearchIndex)).andReturn(Opt.<Event> none()).anyTimes();

    SchedulerService schedulerService = EasyMock.createMock(SchedulerService.class);

    IngestService ingestService = EasyMock.createMock(IngestService.class);

    AssetManager assetManager = EasyMock.createMock(AssetManager.class);

    WorkflowService workflowService = EasyMock.createNiceMock(WorkflowService.class);

    /**
     * Setup CommonEventCatalog
     */
    CommonEventCatalogUIAdapter commonEventCatalogUIAdapter = new CommonEventCatalogUIAdapter();
    Properties episodeCatalogProperties = getCatalogProperties(getClass(), "/episode-catalog.properties");
    commonEventCatalogUIAdapter.updated(PropertiesUtil.toDictionary(episodeCatalogProperties));
    addCatalogUIAdapter(commonEventCatalogUIAdapter);
    EasyMock.expect(indexService.getCommonEventCatalogUIAdapter())
        .andReturn(commonEventCatalogUIAdapter).anyTimes();

    /**
     * Setup catalog to be deleted.
     */
    EventCatalogUIAdapter deleteAdapter = EasyMock.createMock(EventCatalogUIAdapter.class);
    EasyMock.expect(deleteAdapter.getFlavor()).andReturn(new MediaPackageElementFlavor(DELETE_CATALOG_TYPE, "episode"))
        .anyTimes();
    EasyMock.expect(deleteAdapter.getOrganization()).andReturn(defaultOrg.getId()).anyTimes();
    EasyMock.expect(deleteAdapter.handlesOrganization(EasyMock.eq(defaultOrg.getId()))).andReturn(true).anyTimes();
    EasyMock.expect(deleteAdapter.getFields(EasyMock.anyObject(MediaPackage.class))).andReturn(null).anyTimes();
    EasyMock.expect(deleteAdapter.getUITitle()).andReturn(null).anyTimes();
    EasyMock.replay(deleteAdapter);
    addCatalogUIAdapter(deleteAdapter);

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

    EasyMock.expect(indexService.getEvent(DELETE_EVENT_METADATA, elasticsearchIndex))
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
    EasyMock.expect(indexService.getEvent(NO_PUBLICATIONS_EVENT, elasticsearchIndex))
            .andReturn(Opt.some(noPublicationsEvent)).anyTimes();
    EasyMock.expect(indexService.getEventMediapackage(noPublicationsEvent)).andReturn(noPublicationsMP).anyTimes();
    // Two Pubs
    Event twoPublicationsEvent = new Event(TWO_PUBLICATIONS, defaultOrg.getId());
    MediaPackage twoPublicationsMP = EasyMock.createMock(MediaPackage.class);
    Publication paellaPublication = new PublicationImpl(ENGAGE_PUBLICATION_ID, "EVENTS.EVENTS.DETAILS.PUBLICATIONS.ENGAGE",
            new URI("http://mh-allinone.localdomain/paella/ui/watch.html?id=af1a51ce-fb61-4dae-9d5a-f85b9e4fcc99"),
            MimeType.mimeType("not", "used"));
    Publication oaipmh = new PublicationImpl(OAIPMH_PUBLICATION_ID, "oaipmh",
            new URI("http://mh-allinone.localdomain/oaipmh/default?verb=ListMetadataFormats&identifier=af1a51ce-fb61-4dae-9d5a-f85b9e4fcc99"),
            MimeType.mimeType("not", "used"));
    EasyMock.expect(twoPublicationsMP.getPublications()).andReturn(new Publication[] { paellaPublication, oaipmh })
            .anyTimes();
    EasyMock.expect(indexService.getEvent(TWO_PUBLICATIONS, elasticsearchIndex)).andReturn(Opt.some(twoPublicationsEvent))
            .anyTimes();
    EasyMock.expect(indexService.getEventMediapackage(twoPublicationsEvent)).andReturn(twoPublicationsMP).anyTimes();

    /**
     * Update event external service mocking
     */
    capturedMetadataList1 = Capture.newInstance();
    Event updateEvent = new Event(UPDATE_EVENT, defaultOrg.getId());
    EasyMock.expect(indexService.getEvent(UPDATE_EVENT, elasticsearchIndex)).andReturn(Opt.some(updateEvent)).anyTimes();
    EasyMock.expect(indexService.updateEventMetadata(eq(UPDATE_EVENT), capture(capturedMetadataList1), eq(elasticsearchIndex))).andReturn(null).anyTimes();

    /**
     * Update event metadata external service mocking
     */
    capturedMetadataList2 = Capture.newInstance();
    Event updateEventMetadata = new Event(METADATA_UPDATE_EVENT, defaultOrg.getId());
    updateEventMetadata.setRecordingStartDate("2017-08-29T00:05:00.000Z");
    EasyMock.expect(indexService.getEvent(METADATA_UPDATE_EVENT, elasticsearchIndex)).andReturn(Opt.some(updateEventMetadata)).anyTimes();
    EasyMock.expect(indexService.updateEventMetadata(eq(METADATA_UPDATE_EVENT), capture(capturedMetadataList2), eq(elasticsearchIndex))).andReturn(null).anyTimes();
    EasyMock.expect(indexService.getEventMediapackage(updateEventMetadata)).andReturn(null).anyTimes();

    /**
     * Update event track data
     */
    capturedMediaPackage = Capture.newInstance();
    Event updateEventTrack = new Event(TRACK_UPDATE_EVENT, defaultOrg.getId());

    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    URI uriMP = TestEventsEndpoint.class.getResource("/event-track-update-mediapackage.xml").toURI();
    URI uriMPUpdated = TestEventsEndpoint.class.getResource("/event-track-update-mediapackage-updated.xml").toURI();
    MediaPackage mp = builder.loadFromXml(uriMP.toURL().openStream());
    MediaPackage mpUpdated = builder.loadFromXml(uriMPUpdated.toURL().openStream());

    EasyMock.expect(indexService.getEvent(TRACK_UPDATE_EVENT, elasticsearchIndex)).andReturn(Opt.some(updateEventTrack)).anyTimes();
    EasyMock.expect(indexService.getEventMediapackage(updateEventTrack)).andReturn(mp).anyTimes();
    EasyMock.expect(ingestService.addTrack((InputStream) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (MediaPackageElementFlavor) EasyMock.anyObject(), (MediaPackage) EasyMock.anyObject()))
            .andReturn(mpUpdated);
    EasyMock.expect(assetManager.takeSnapshot(EasyMock.capture(capturedMediaPackage))).andReturn(EasyMock.createNiceMock(Snapshot.class));
    EasyMock.expect(workflowService.mediaPackageHasActiveWorkflows(mp.getIdentifier().toString())).andReturn(false).anyTimes();

    /**
     * Get event metadata external service mocking
     */
    Event getEvent = new Event(METADATA_GET_EVENT, defaultOrg.getId());
    getEvent.setRecordingStartDate("2017-08-29T00:05:00.000Z");
    EasyMock.expect(indexService.getEvent(METADATA_GET_EVENT, elasticsearchIndex)).andReturn(Opt.some(getEvent)).anyTimes();
    EasyMock.expect(indexService.getEventMediapackage(getEvent)).andReturn(null).anyTimes();

    /**
     * Update event scheduling information external service mocking
     */
    final TechnicalMetadata technicalMetadata1 = new TechnicalMetadataImpl(
        SCHEDULING_UPDATE_EVENT,
        "testAgent23",
        Date.from(Instant.parse("2017-08-28T00:05:00Z")),
        Date.from(Instant.parse("2017-08-28T00:07:00Z")),
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
    EasyMock.expect(indexService.getEvent(SCHEDULING_UPDATE_EVENT, elasticsearchIndex)).andReturn(Opt.some(updateEventScheduling)).anyTimes();
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
        eq(false)
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
        null,
        null,
        Collections.singletonMap(CaptureParameters.CAPTURE_DEVICE_NAMES, "default1,default2"),
        Opt.none()
    );
    EasyMock.expect(indexService.getEvent(SCHEDULING_GET_EVENT, elasticsearchIndex)).andReturn(Opt.some(getEventScheduling)).anyTimes();
    EasyMock.expect(schedulerService.getTechnicalMetadata(SCHEDULING_GET_EVENT)).andReturn(technicalMetadata2).anyTimes();


    // Replay all mocks
    EasyMock.replay(deleteMetadataMP, indexService, schedulerService, ingestService, assetManager, workflowService, noPublicationsMP, twoPublicationsMP);

    setElasticsearchIndex(elasticsearchIndex);
    setIndexService(indexService);
    setSchedulerService(schedulerService);
    setIngestService(ingestService);
    setAssetManager(assetManager);
    setWorkflowService(workflowService);
    setupSecurityService();
    Properties properties = new Properties();
    properties.load(getClass().getResourceAsStream("/events-endpoint.properties"));
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

  public static Capture<MediaPackage> getCapturedMediaPackage() {
    return capturedMediaPackage;
  }
}
