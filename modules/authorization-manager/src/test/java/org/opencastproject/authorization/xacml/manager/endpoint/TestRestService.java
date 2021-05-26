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

package org.opencastproject.authorization.xacml.manager.endpoint;

import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.impl.AclDb;
import org.opencastproject.authorization.xacml.manager.impl.AclServiceImpl;
import org.opencastproject.authorization.xacml.manager.impl.persistence.JpaAclDb;
import org.opencastproject.elasticsearch.api.SearchIndexException;
import org.opencastproject.elasticsearch.api.SearchResultItem;
import org.opencastproject.elasticsearch.impl.SearchResultImpl;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.elasticsearch.index.event.Event;
import org.opencastproject.elasticsearch.index.event.EventSearchQuery;
import org.opencastproject.elasticsearch.index.series.Series;
import org.opencastproject.elasticsearch.index.series.SeriesSearchQuery;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Ignore;

import java.net.URL;

import javax.persistence.EntityManagerFactory;
import javax.ws.rs.Path;

// use base path /test to prevent conflicts with the production service
@Path("/test")
// put @Ignore here to prevent maven surefire from complaining about missing test methods
@Ignore
public class TestRestService extends AbstractAclServiceRestEndpoint {

  public static final URL BASE_URL = localhostRandomPort();

  // Declare this dependency static since the TestRestService gets instantiated multiple times.
  // Haven't found out who's responsible for this but that's the way it is.
  public static final AclServiceFactory aclServiceFactory;
  public static final SecurityService securityService;
  public static final Workspace workspace;
  public static final AbstractSearchIndex adminUiIndex;
  public static final AbstractSearchIndex externalApiIndex;
  public static final EntityManagerFactory authorizationEMF = newTestEntityManagerFactory(
          "org.opencastproject.authorization.xacml.manager");

  static {
    SecurityService testSecurityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", "test", new DefaultOrganization(),
            new JaxbRole(SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    EasyMock.expect(testSecurityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(testSecurityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(testSecurityService);
    securityService = testSecurityService;
    workspace = newWorkspace();

    SearchResultImpl<Event> eventSearchResult = EasyMock.createNiceMock(SearchResultImpl.class);
    EasyMock.expect(eventSearchResult.getItems()).andReturn(new SearchResultItem[] {}).anyTimes();
    SearchResultImpl<Series> seriesSearchResult = EasyMock.createNiceMock(SearchResultImpl.class);
    EasyMock.expect(seriesSearchResult.getItems()).andReturn(new SearchResultItem[] {}).anyTimes();

    adminUiIndex = EasyMock.createNiceMock(AbstractSearchIndex.class);
    externalApiIndex = EasyMock.createNiceMock(AbstractSearchIndex.class);

    try {
      EasyMock.expect(adminUiIndex.getByQuery(EasyMock.anyObject(EventSearchQuery.class)))
              .andReturn(eventSearchResult).anyTimes();
      EasyMock.expect(adminUiIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class)))
              .andReturn(seriesSearchResult).anyTimes();
      EasyMock.expect(externalApiIndex.getByQuery(EasyMock.anyObject(EventSearchQuery.class)))
              .andReturn(eventSearchResult).anyTimes();
      EasyMock.expect(externalApiIndex.getByQuery(EasyMock.anyObject(SeriesSearchQuery.class)))
              .andReturn(seriesSearchResult).anyTimes();
    } catch (SearchIndexException e) {
      // should never happen
    }

    EasyMock.replay(adminUiIndex, externalApiIndex, eventSearchResult, seriesSearchResult);

    aclServiceFactory = new AclServiceFactory() {
      @Override
      public AclService serviceFor(Organization org) {
        return new AclServiceImpl(new DefaultOrganization(), newAclPersistence(), adminUiIndex, externalApiIndex,
                securityService);
      }
    };
  }

  @Override
  protected AclServiceFactory getAclServiceFactory() {
    return aclServiceFactory;
  }

  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  private static Workspace newWorkspace() {
    return EasyMock.createNiceMock(Workspace.class);
  }

  private static AclDb newAclPersistence() {
    JpaAclDb db = new JpaAclDb();
    db.setEntityManagerFactory(authorizationEMF);
    return db;
  }

  @Override
  protected String getEndpointBaseUrl() {
    return BASE_URL.toString();
  }

}
