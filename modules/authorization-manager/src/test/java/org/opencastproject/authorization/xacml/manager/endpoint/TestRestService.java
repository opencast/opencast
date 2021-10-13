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

import static com.entwinemedia.fn.Stream.$;
import static org.opencastproject.test.rest.RestServiceTestEnv.localhostRandomPort;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.api.query.VersionField;
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
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

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
  public static final AuthorizationService authorizationService;
  public static final AssetManager assetManager;
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
    authorizationService = newAuthorizationService();
    assetManager = newAssetManager();
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

  @Override
  protected AssetManager getAssetManager() {
    return assetManager;
  }

  @Override
  protected AuthorizationService getAuthorizationService() {
    return authorizationService;
  }

  private static Workspace newWorkspace() {
    return EasyMock.createNiceMock(Workspace.class);
  }

  private static AclDb newAclPersistence() {
    JpaAclDb db = new JpaAclDb();
    db.setEntityManagerFactory(authorizationEMF);
    return db;
  }

  private static AuthorizationService newAuthorizationService() {
    AccessControlList acl = new AccessControlList();
    Attachment attachment = new AttachmentImpl();
    MediaPackage mediapackage;
    mediapackage = new MediaPackageBuilderImpl().createNew();
    AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
            .andReturn(Tuple.tuple(acl, AclScope.Series)).anyTimes();
    try {
      EasyMock.expect(authorizationService.setAcl(
              EasyMock.anyObject(MediaPackage.class),
              EasyMock.anyObject(AclScope.class),
              EasyMock.anyObject(AccessControlList.class)))
              .andReturn(Tuple.tuple(mediapackage, attachment));
    } catch (MediaPackageException e) {
      throw new RuntimeException(e);
    }
    EasyMock.replay(authorizationService);

    return authorizationService;
  }

  private static AssetManager newAssetManager() {
    Snapshot snapshot = EasyMock.createNiceMock(Snapshot.class);
    EasyMock.expect(snapshot.getMediaPackage()).andReturn(new MediaPackageBuilderImpl().createNew()).anyTimes();
    ARecord record = EasyMock.createNiceMock(ARecord.class);
    EasyMock.expect(record.getSnapshot()).andReturn(Opt.some(snapshot)).anyTimes();

    AResult result = EasyMock.createNiceMock(AResult.class);
    EasyMock.expect(result.getRecords()).andReturn($(record)).anyTimes();

    ASelectQuery select = EasyMock.createNiceMock(ASelectQuery.class);
    EasyMock.expect(select.where(EasyMock.anyObject(Predicate.class))).andReturn(select).anyTimes();
    EasyMock.expect(select.run()).andReturn(result).anyTimes();

    Predicate predicate = EasyMock.createNiceMock(Predicate.class);
    EasyMock.expect(predicate.and(EasyMock.anyObject(Predicate.class))).andReturn(predicate).anyTimes();

    AQueryBuilder query = EasyMock.createNiceMock(AQueryBuilder.class);

    VersionField version = EasyMock.createNiceMock(VersionField.class);

    EasyMock.expect(query.version()).andReturn(version).anyTimes();
    EasyMock.expect(query.mediaPackageId(EasyMock.anyString())).andReturn(predicate).anyTimes();
    EasyMock.expect(query.select(EasyMock.anyObject(Target.class))).andReturn(select).anyTimes();

    AssetManager assetManager = EasyMock.createNiceMock(AssetManager.class);
    EasyMock.expect(assetManager.getMediaPackage(EasyMock.anyString())).andReturn(Opt.some(new MediaPackageBuilderImpl()
            .createNew())).anyTimes();
    EasyMock.expect(assetManager.createQuery()).andReturn(query).anyTimes();
    EasyMock.replay(assetManager, version, query, predicate, select, result, record, snapshot);
    return assetManager;
  }

  @Override
  protected String getEndpointBaseUrl() {
    return BASE_URL.toString();
  }

}
