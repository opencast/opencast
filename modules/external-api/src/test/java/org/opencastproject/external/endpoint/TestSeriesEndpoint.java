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

import static com.entwinemedia.fn.data.Opt.some;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.opencastproject.index.service.util.CatalogAdapterUtil.getCatalogProperties;

import org.opencastproject.external.impl.index.ExternalIndex;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.catalog.adapter.series.CommonSeriesCatalogUIAdapter;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.metadata.dublincore.SeriesCatalogUIAdapter;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PropertiesUtil;
import org.opencastproject.util.data.Arrays;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Ignore;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.Path;

@Path("")
@Ignore
public class TestSeriesEndpoint extends SeriesEndpoint {

  @SuppressWarnings("unchecked")
  public TestSeriesEndpoint() throws Exception {

    this.endpointBaseUrl = "https://api.opencast.org";

    // Prepare mocked organization
    Organization org = createNiceMock(Organization.class);
    expect(org.getId()).andStubReturn("opencast");
    replay(org);

    Set<Role> roles = new HashSet<>();
    Role roleStudent = createNiceMock(Role.class);
    expect(roleStudent.getName()).andStubReturn("ROLE_STUDENT");
    roles.add(roleStudent);
    Role roleUser = createNiceMock(Role.class);
    expect(roleUser.getName()).andStubReturn("ROLE_USER_92623987_OPENCAST_ORG");
    roles.add(roleUser);

    // Prepare mocked user
    User user = createNiceMock(User.class);
    expect(user.getOrganization()).andStubReturn(org);
    expect(user.getEmail()).andStubReturn("nowhere@opencast.org");
    expect(user.getName()).andStubReturn("Opencast Student");
    expect(user.getProvider()).andStubReturn("opencast");
    expect(user.getUsername()).andStubReturn("92623987@opencast.org");
    expect(user.getRoles()).andStubReturn(roles);
    replay(user);

    // Prepare mocked security service
    SecurityService securityService = createNiceMock(SecurityService.class);
    expect(securityService.getOrganization()).andStubReturn(org);
    expect(securityService.getUser()).andStubReturn(user);
    replay(securityService);

    Series series1 = new Series("4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f", "opencast");
    series1.setTitle("Via API");
    series1.setDescription("A series created over the external API");
    series1.setSubject("Topic");
    series1.setCreator("Gracie Walsh");
    series1.setCreatedDateTime(new Date(1429175556000L));
    series1.setOptOut(true);
    series1.addContributor("Nu'man Farooq Morcos");
    series1.addContributor("Alfie Gibbons");
    series1.addPublisher("Sophie Chandler");
    series1.addOrganizer("Peter Feierabend");
    series1.addOrganizer("Florian Naumann");
    series1.addOrganizer("Niklas Vogler");
    series1.setAccessPolicy(IOUtils.toString(TestSeriesEndpoint.class.getResourceAsStream("/series1-acl.json")));

    SearchResultItem<Series> searchResultItem1 = EasyMock.createNiceMock(SearchResultItem.class);
    expect(searchResultItem1.getSource()).andStubReturn(series1);
    replay(searchResultItem1);

    SearchResultItem<Series>[] searchResultItems = Arrays.array(searchResultItem1);

    SearchResult<Series> searchResult = createNiceMock(SearchResult.class);
    expect(searchResult.getItems()).andStubReturn(searchResultItems);
    replay(searchResult);

    ExternalIndex externalIndex = createMock(ExternalIndex.class);
    expect(externalIndex.getByQuery(anyObject(SeriesSearchQuery.class))).andStubReturn(searchResult);
    replay(externalIndex);

    Map<String, String> series1Props = new HashMap<>();
    series1Props.put("live", "false");
    series1Props.put("ondemand", "true");

    CommonSeriesCatalogUIAdapter commonAdapter = new CommonSeriesCatalogUIAdapter();
    Properties seriesCatalogProperties = getCatalogProperties(getClass(), "/series-catalog.properties");
    commonAdapter.updated(PropertiesUtil.toDictionary(seriesCatalogProperties));
    List<SeriesCatalogUIAdapter> adapters = new LinkedList<>();
    adapters.add(commonAdapter);

    IndexService indexService = createNiceMock(IndexService.class);
    expect(indexService.getSeries("4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f", externalIndex)).andStubReturn(some(series1));
    expect(indexService.getSeries("unknown-series-id", externalIndex)).andStubReturn(Opt.<Series> none());
    expect(indexService.getSeriesCatalogUIAdapters()).andStubReturn(adapters);
    expect(indexService.getCommonSeriesCatalogUIAdapter()).andStubReturn(commonAdapter);
    expect(indexService
            .createSeries(EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject()))
            .andStubReturn("4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f");
    expect(indexService.updateAllSeriesMetadata(EasyMock.anyString(), EasyMock.anyString(),
            EasyMock.anyObject(ExternalIndex.class))).andStubReturn(new MetadataList());
    indexService.removeCatalogByFlavor(series1, MediaPackageElementFlavor.parseFlavor("missing/series"));
    expectLastCall().andThrow(new NotFoundException("Missing catalog"));
    indexService.removeCatalogByFlavor(series1, MediaPackageElementFlavor.parseFlavor("othercatalog/series"));
    expectLastCall();
    replay(indexService);

    SeriesService seriesService = createNiceMock(SeriesService.class);
    expect(seriesService.getSeriesProperties("4fd0ef66-aea5-4b7a-a62a-a4ada0eafd6f")).andStubReturn(series1Props);
    replay(seriesService);

    setExternalIndex(externalIndex);
    setIndexService(indexService);
    setSecurityService(securityService);
    setSeriesService(seriesService);
  }

}
