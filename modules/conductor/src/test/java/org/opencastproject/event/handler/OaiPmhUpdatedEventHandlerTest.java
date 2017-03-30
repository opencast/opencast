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

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.opencastproject.mediapackage.MediaPackageElementParser.getAsXml;
import static org.opencastproject.publication.api.OaiPmhPublicationService.PUBLICATION_CHANNEL_PREFIX;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.PublicationImpl;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.metadata.api.MediaPackageMetadata;
import org.opencastproject.metadata.api.MediapackageMetadataImpl;
import org.opencastproject.metadata.api.util.MediaPackageMetadataSupport;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.Query;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.Capture;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;

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
  private SecurityService securityServiceMock;
  @Mock
  private OaiPmhDatabase oaiPmhDatabaseMock;
  @Mock
  private DistributionService distributionServiceMock;
  @Mock
  private ServiceRegistry serviceRegistryMock;
  @Mock
  private DublinCoreCatalogService dublinCoreCatalogServiceMock;

  private Capture<User> adminUserCapture;
  private Capture<Query> queryCapture;

  @Before
  public void setup() throws Exception {
    cut.systemAccount = SYSTEM_ACCOUNT;
    Hashtable<String, String> props = new Hashtable<>();
    props.put("propagate.episode", "true");
    cut.updated(props);
  }

  /**
   * Tests "normal" behavior, i.e. if an episode is published correctly to OAI-PMH.
   */
  @Test
  public void testEpisodeMetadataUpdate() throws Exception {

    MediaPackageMetadata metadata = createMetaData();
    MediaPackage newMp = createMediaPackage(new PublicationImpl(), new TrackImpl(), new AttachmentImpl());
    MediaPackage oldMp = createMediaPackage(new PublicationImpl(), new PublicationImpl(), newMp.getTracks()[0],
        newMp.getAttachments()[0]);
    MediaPackage expectedMediaPackage = (MediaPackage) oldMp.clone();
    MediaPackageMetadataSupport.populateMediaPackageMetadata(expectedMediaPackage, metadata);
    AssetManagerItem.TakeSnapshot snapshot = createSnapshot(newMp);

    //These are the interactions we expect with the security service
    mockSecurityService();

    //These are the interactions we expect fo the Oai-Pmh database
    mockOaiPmhDatabase(oldMp);
    oaiPmhDatabaseMock.store(expectedMediaPackage, OAIPMH_REPOSITORY);

    //These are the interactions we expect fo the distribution service
    Job jobMock = mockDistributionService(snapshot);

    //These are the interactions we expect fo the service registry
    expect(serviceRegistryMock.getJob(anyLong())).andReturn(jobMock).atLeastOnce();

    //These are the interactions we expect fo the dublin core service
    expect(dublinCoreCatalogServiceMock.getMetadata(snapshot.getMediapackage())).andReturn(metadata);

    replayAll();

    cut.handleEvent(snapshot);

    verifyAll();
    assertThat(queryCapture.getValue().getMediaPackageId().get(), is(newMp.getIdentifier().toString()));
    assertThat(adminUserCapture.getValue().getUsername(), is(SYSTEM_ACCOUNT));
  }

  /**
   * Tests if publishing to OAI-PMH is skipped, if the episode is not known by OAI-PMH.
   */
  @Test
  public void testEpisodeNotKnownByOaiPmh() throws Exception {

    MediaPackage newMp = createMediaPackage(new PublicationImpl(), new TrackImpl(), new AttachmentImpl());
    AssetManagerItem.TakeSnapshot snapshot = createSnapshot(newMp);

    //These are the interactions we expect with the security service
    mockSecurityService();

    //These are the interactions we expect fo the Oai-Pmh database
    SearchResult searchResultMock = mock(MockType.NICE, SearchResult.class);
    expect(searchResultMock.getItems()).andReturn(Collections.emptyList()).anyTimes();
    expect(oaiPmhDatabaseMock.search(anyObject(Query.class))).andReturn(searchResultMock);

    replayAll();

    cut.handleEvent(snapshot);

    verifyAll();
  }

  private AssetManagerItem.TakeSnapshot createSnapshot(MediaPackage mediaPackage) throws MediaPackageException {

    Workspace workspaceMock = mock(MockType.NICE, Workspace.class);
    AccessControlList acl = new AccessControlList();
    AssetManagerItem.TakeSnapshot result = AssetManagerItem.add(workspaceMock, mediaPackage, acl, 0L, new Date(),
            AssetManager.DEFAULT_OWNER);
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

  private MediaPackageMetadata createMetaData() {
    MediapackageMetadataImpl result = new MediapackageMetadataImpl();
    result.setContributors(new String[]{"Werner"});
    result.setCreators(new String[]{"Heinz"});
    result.setDate(new Date());
    result.setLanguage("en");
    result.setLicense("CC0");
    result.setSeriesIdentifier("123");
    result.setSeriesTitle("Title123");
    result.setSubjects(new String[]{"Subject"});
    result.setTitle("Title");
    return result;
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

  private void mockOaiPmhDatabase(MediaPackage mp) {
    SearchResult searchResultMock = mock(MockType.NICE, SearchResult.class);
    SearchResultItem searchResultItemMock = mock(MockType.NICE, SearchResultItem.class);
    expect(searchResultMock.getItems()).andReturn(Collections.singletonList(searchResultItemMock)).anyTimes();
    expect(searchResultItemMock.getRepository()).andReturn(OAIPMH_REPOSITORY).anyTimes();
    expect(searchResultItemMock.getMediaPackage()).andReturn(mp);
    queryCapture = Capture.newInstance();
    expect(oaiPmhDatabaseMock.search(capture(queryCapture))).andReturn(searchResultMock);
  }

  private Job mockDistributionService(AssetManagerItem.TakeSnapshot snapshot) throws Exception {
    String expectedChannel = PUBLICATION_CHANNEL_PREFIX.concat(OAIPMH_REPOSITORY);
    Job jobMock = mock(MockType.NICE, Job.class);
    expect(jobMock.getStatus()).andReturn(Job.Status.FINISHED).anyTimes();
    expect(jobMock.getPayload()).andReturn(getAsXml(snapshot.getMediapackage().getElements()[0])).anyTimes();
    expect(distributionServiceMock.distribute(eq(expectedChannel), eq(snapshot.getMediapackage()), anyString()))
        .andReturn(jobMock).atLeastOnce();
    return jobMock;
  }

}
