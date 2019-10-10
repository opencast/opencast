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
package org.opencastproject.event.handler;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.ASelectQuery;
import org.opencastproject.assetmanager.api.query.Field;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.api.query.VersionField;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.CatalogImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.Query;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;

/**
 * This class contains some tests for the {@link OaiPmhUpdatedEventHandler}.
 */
public class OaiPmhUpdatedEventHandlerTest extends EasyMockSupport {

  private static final String MEDIA_PACKAGE_TITLE = "test.mediapackage.title";
  private static final String OAIPMH_REPOSITORY = "test.oaipmh.repository";
  private static final String SYSTEM_ACCOUNT = "opencast_system_account";
  private static final MediaPackageBuilder MP_BUILDER = MediaPackageBuilderFactory.newInstance()
      .newMediaPackageBuilder();

  @Rule
  public EasyMockRule rule = new EasyMockRule(this);

  @TestSubject
  private OaiPmhUpdatedEventHandler cut = new OaiPmhUpdatedEventHandler();

  // Dependencies of class under test
  @Mock
  private AssetManager assetManagerMock;
  @Mock
  private SecurityService securityServiceMock;
  @Mock
  private Workspace workspace;
  @Mock
  private OaiPmhDatabase oaiPmhDatabaseMock;
  @Mock
  private OaiPmhPublicationService oaiPmhPublicationService;

  private Capture<User> adminUserCapture;
  private Capture<Query> queryCapture;
  private Capture<String> orgIdCapture;
  private Capture<String> mpIdCapture;
  private Capture<String> snapshotVersionCapture;

  @Before
  public void setup() throws Exception {
    cut.systemAccount = SYSTEM_ACCOUNT;
    Hashtable<String, String> props = new Hashtable<>();
    props.put(OaiPmhUpdatedEventHandler.CFG_PROPAGATE_EPISODE, "true");
    props.put(OaiPmhUpdatedEventHandler.CFG_FLAVORS, "dublincore/*,security/*");
    props.put(OaiPmhUpdatedEventHandler.CFG_TAGS, "archive");
    cut.updated(props);

    expect(workspace.read(anyObject())).andAnswer(() -> getClass().getResourceAsStream("/episode.xml")).anyTimes();
  }

  /**
   * Tests "normal" behavior, where the media package contains at least one element with the given flavor and tags
   */
  @Test
  public void testHandleEvent() throws Exception {
    Catalog episodeCatalog = CatalogImpl.newInstance();
    episodeCatalog.setURI(URI.create("/episode.xml"));
    episodeCatalog.setFlavor(MediaPackageElementFlavor.parseFlavor("dublincore/episode"));
    episodeCatalog.addTag("archive");
    MediaPackage updatedMp = createMediaPackage(episodeCatalog);

    // these are the interactions we expect with the asset manager
    mockAssetManager(updatedMp);

    // these are the interactions we expect with the security service
    mockSecurityService();

    // these are the interactions we expect for the OAI-PMH database
    mockOaiPmhDatabase();

    Capture<MediaPackage> mpCapture = Capture.newInstance();
    Capture<String> repositoryCapture = Capture.newInstance();
    Capture<Set<String>> flavorsCapture = Capture.newInstance();
    Capture<Set> tagsCapture = Capture.newInstance();
    expect(oaiPmhPublicationService.updateMetadata(capture(mpCapture), capture(repositoryCapture),
            capture(flavorsCapture), capture(tagsCapture), anyBoolean()))
            .andAnswer(() -> mock(Job.class)).times(1);

    replayAll();

    cut.handleEvent(createSnapshot(updatedMp));

    assertEquals(updatedMp.getIdentifier().compact(), mpCapture.getValue().getIdentifier().compact());
    assertEquals(OAIPMH_REPOSITORY, repositoryCapture.getValue());
    Assert.assertNotNull(flavorsCapture.getValue());
    Assert.assertTrue(flavorsCapture.getValue().contains("dublincore/*"));
    Assert.assertTrue(flavorsCapture.getValue().contains("security/*"));
    Assert.assertTrue(tagsCapture.getValue().contains("archive"));
    Assert.assertTrue(orgIdCapture.hasCaptured());
    Assert.assertEquals(new DefaultOrganization().getId(), orgIdCapture.getValue());
    Assert.assertTrue(mpIdCapture.hasCaptured());
    Assert.assertEquals(updatedMp.getIdentifier().compact(), mpIdCapture.getValue());
    Assert.assertTrue(snapshotVersionCapture.hasCaptured());
    Assert.assertEquals("3", snapshotVersionCapture.getValue());
  }

  /**
   * Tests if publishing to OAI-PMH is skipped, if the episode is not known by OAI-PMH.
   */
  @Test
  public void testEpisodeNotKnownByOaiPmh() throws Exception {
    Catalog episodeCatalog = CatalogImpl.newInstance();
    episodeCatalog.setURI(URI.create("/episode.xml"));
    episodeCatalog.setFlavor(MediaPackageElementFlavor.parseFlavor("dublincore/episode"));
    episodeCatalog.addTag("archive");
    MediaPackage updatedMp = createMediaPackage(episodeCatalog);

    // these are the interactions we expect with the asset manager
    mockAssetManager(updatedMp);
    // these are the interactions we expect with the security service
    mockSecurityService();

    // mock the OAI-PMH database
    SearchResult searchResultMock = mock(MockType.NICE, SearchResult.class);
    expect(searchResultMock.getItems()).andReturn(Collections.EMPTY_LIST).anyTimes();
    queryCapture = Capture.newInstance();
    expect(oaiPmhDatabaseMock.search(capture(queryCapture))).andReturn(searchResultMock);

    replayAll();

    cut.handleEvent(createSnapshot(updatedMp));

    // the OAI-PMH publication service should not be called as the media package isn't mocked in the OAI-PMH database
    verifyAll();
  }

  /**
   * The media package does not contains elements with the configured tag, the publication should be skipped
   */
  @Test
  public void testNoElementsWithGivenFlavorAndTags() throws Exception {
    Catalog episodeCatalog = CatalogImpl.newInstance();
    episodeCatalog.setURI(URI.create("/episode.xml"));
    episodeCatalog.setFlavor(MediaPackageElementFlavor.parseFlavor("dublincore/episode"));
    // the episode catalog isn't tagged with archive
    MediaPackage updatedMp = createMediaPackage();

    // these are the interactions we expect with the asset manager
    mockAssetManager(updatedMp);
    // these are the interactions we expect with the security service
    mockSecurityService();

    replayAll();

    cut.handleEvent(createSnapshot(updatedMp));

    // the OAI-PMH publication service should not be called as the media package isn't mocked in the OAI-PMH database
    verifyAll();
  }

  /**
   * The media package does not contains any elements, the publication should be skipped
   */
  @Test
  public void testNoElementsForPublishing() throws Exception {
    MediaPackage updatedMp = createMediaPackage();

    // these are the interactions we expect with the asset manager
    mockAssetManager(updatedMp);
    // these are the interactions we expect with the security service
    mockSecurityService();

    replayAll();

    cut.handleEvent(createSnapshot(updatedMp));

    // the OAI-PMH publication service should not be called as the media package isn't mocked in the OAI-PMH database
    verifyAll();
  }

  private AssetManagerItem.TakeSnapshot createSnapshot(MediaPackage mediaPackage) throws Exception {
    AccessControlList acl = new AccessControlList();
    AssetManagerItem.TakeSnapshot result = AssetManagerItem.add(workspace, mediaPackage, acl, 3L, new Date());
    return result;
  }

  private MediaPackage createMediaPackage(MediaPackageElement... elements) throws MediaPackageException {
    MediaPackage result = MP_BUILDER.createNew();
    result.setTitle(MEDIA_PACKAGE_TITLE);
    for (MediaPackageElement mpe : elements) {
      result.add(mpe);
    }
    return result;
  }

  private void mockAssetManager(MediaPackage mediaPackage) {
    AQueryBuilder queryBuilder = mock(AQueryBuilder.class);
    ASelectQuery selectQuery = mock(ASelectQuery.class);
    expect(queryBuilder.select(EasyMock.anyObject())).andReturn(selectQuery);
    Target target = mock(Target.class);
    expect(queryBuilder.snapshot()).andReturn(target);
    Predicate queryPredicate1 = mock(Predicate.class);
    Predicate queryPredicate2 = mock(Predicate.class);
    Predicate queryPredicate3 = mock(Predicate.class);
    expect(queryPredicate1.and(EasyMock.anyObject())).andReturn(queryPredicate2);
    expect(queryPredicate2.and(EasyMock.anyObject())).andReturn(queryPredicate3);
    orgIdCapture = Capture.newInstance();
    Field<String> orgIdField = mock(Field.class);
    expect(orgIdField.eq(capture(orgIdCapture))).andReturn(queryPredicate1);
    expect(queryBuilder.organizationId()).andReturn(orgIdField);
    mpIdCapture = Capture.newInstance();
    expect(queryBuilder.mediaPackageId(capture(mpIdCapture))).andReturn(queryPredicate2);
    VersionField versionField = mock(VersionField.class);
    expect(versionField.eq(EasyMock.anyObject(Version.class))).andReturn(queryPredicate3);
    expect(queryBuilder.version()).andReturn(versionField);
    ASelectQuery selectQuery2 = mock(ASelectQuery.class);
    expect(selectQuery.where(EasyMock.anyObject())).andReturn(selectQuery2);
    AResult queryResult = mock(AResult.class);
    expect(selectQuery2.run()).andReturn(queryResult);
    expect(assetManagerMock.createQuery()).andReturn(queryBuilder);
    snapshotVersionCapture = EasyMock.newCapture();
    expect(assetManagerMock.toVersion(capture(snapshotVersionCapture))).andReturn(Opt.some(mock(Version.class)));
    ARecord record = mock(ARecord.class);
    expect(queryResult.getRecords()).andReturn(Stream.$(record));
    Snapshot snapshot = mock(Snapshot.class);
    expect(record.getSnapshot()).andReturn(Opt.some(snapshot));
    expect(snapshot.getMediaPackage()).andReturn(mediaPackage);
  }

  private void mockSecurityService() {
    Organization organization = new DefaultOrganization();
    User user = mock(User.class);
    expect(user.getOrganization()).andReturn(organization).anyTimes();
    adminUserCapture = Capture.newInstance();
    expect(securityServiceMock.getUser()).andReturn(user);
    expect(securityServiceMock.getOrganization()).andReturn(organization);
    securityServiceMock.setUser(capture(adminUserCapture));
    securityServiceMock.setUser(user);
    securityServiceMock.setOrganization(organization);
  }

  private void mockOaiPmhDatabase() {
    SearchResult searchResultMock = mock(MockType.NICE, SearchResult.class);
    SearchResultItem searchResultItemMock = mock(MockType.NICE, SearchResultItem.class);
    expect(searchResultMock.getItems()).andReturn(Collections.singletonList(searchResultItemMock)).anyTimes();
    expect(searchResultItemMock.getRepository()).andReturn(OAIPMH_REPOSITORY).anyTimes();
    queryCapture = Capture.newInstance();
    expect(oaiPmhDatabaseMock.search(capture(queryCapture))).andReturn(searchResultMock);
  }
}
