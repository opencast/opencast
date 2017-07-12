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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.opencastproject.index.service.util.CatalogAdapterUtil.getCatalogProperties;

import org.opencastproject.external.impl.index.ExternalIndex;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PropertiesUtil;

import com.entwinemedia.fn.data.Opt;

import org.easymock.EasyMock;
import org.junit.Ignore;
import org.osgi.service.cm.ConfigurationException;

import java.net.URI;
import java.util.Properties;

import javax.ws.rs.Path;

@Path("/")
@Ignore
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
    EasyMock.expect(deleteAdapter.getOrganization()).andReturn(defaultOrg.getId()).anyTimes();
    EasyMock.replay(deleteAdapter);
    addCatalogUIAdapter(deleteAdapter);
  }

  public TestEventsEndpoint() throws Exception {
    ExternalIndex externalIndex = new ExternalIndex();

    IndexService indexService = EasyMock.createMock(IndexService.class);
    EasyMock.expect(indexService.getEvent(MISSING_ID, externalIndex)).andReturn(Opt.<Event> none()).anyTimes();

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
    Publication theodulPublication = new PublicationImpl(ENGAGE_PUBLICATION_ID, "EVENTS.EVENTS.DETAILS.GENERAL.ENGAGE",
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

    // Replay all mocks
    EasyMock.replay(deleteMetadataMP, indexService, noPublicationsMP, twoPublicationsMP);

    setExternalIndex(externalIndex);
    setIndexService(indexService);
    setupSecurityService();
    setupEventCatalogUIAdapters();
  }

}
