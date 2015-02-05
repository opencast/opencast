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
package org.opencastproject.publication.youtube;

import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.Video;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.publication.youtube.auth.ClientCredentials;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.workspace.api.Workspace;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;

/**
 * @author John Crossman
 */
public class YouTubeV3PublicationServiceImplTest {

  private YouTubeV3PublicationServiceImpl service;
  private YouTubeAPIVersion3Service youTubeService;
  private OrganizationDirectoryService orgDirectory;
  private SecurityService security;
  private ServiceRegistry registry;
  private UserDirectoryService userDirectoryService;
  private Workspace workspace;

  @Before
  public void before() throws Exception {
    youTubeService = createMock(YouTubeAPIVersion3Service.class);
    youTubeService.initialize(anyObject(ClientCredentials.class));
    expectLastCall();
    orgDirectory = createMock(OrganizationDirectoryService.class);
    security = createMock(SecurityService.class);
    registry = createMock(ServiceRegistry.class);
    userDirectoryService = createMock(UserDirectoryService.class);
    workspace = createMock(Workspace.class);
    //
    service = new YouTubeV3PublicationServiceImpl(youTubeService);
    service.setOrganizationDirectoryService(orgDirectory);
    service.setSecurityService(security);
    service.setServiceRegistry(registry);
    service.setUserDirectoryService(userDirectoryService);
    service.setWorkspace(workspace);
    service.updated(getServiceProperties());
  }

  @Test
  public void testPublishNewPlaylist() throws PublicationException, MediaPackageException, URISyntaxException, IOException, ServiceRegistryException {
    final File baseDir = new File(this.getClass().getResource("/mediapackage").toURI());
    final String xml = FileUtils.readFileToString(new File(baseDir, "manifest.xml"));
    final MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().loadFromXml(xml);
    //
    expect(youTubeService.getMyPlaylistByTitle(mediaPackage.getTitle())).andReturn(null).once();
    expect(youTubeService.createPlaylist(mediaPackage.getSeriesTitle(), null, mediaPackage.getSeries())).andReturn(new Playlist()).once();
    expect(youTubeService.addVideoToMyChannel(anyObject(VideoUpload.class))).andReturn(new Video()).once();
    expect(youTubeService.addPlaylistItem(anyObject(String.class), anyObject(String.class))).andReturn(new PlaylistItem()).once();

    expect(registry.createJob(anyObject(String.class), anyObject(String.class), anyObject(List.class))).andReturn(new JaxbJob()).once();
    replay(youTubeService, orgDirectory, security, registry, userDirectoryService, workspace);
    service.publish(mediaPackage, mediaPackage.getTracks()[0]);
  }

  private Dictionary getServiceProperties() throws IOException {
    final Properties p = new Properties();
    YouTubeUtils.put(p, YouTubeKey.credentialDatastore, "credentialDatastore");
    YouTubeUtils.put(p, YouTubeKey.scopes, "foo");
    final String absolutePath = UnitTestUtils.getMockClientSecretsFile("clientId").getAbsolutePath();
    YouTubeUtils.put(p, YouTubeKey.clientSecretsV3, absolutePath);
    YouTubeUtils.put(p, YouTubeKey.dataStore, "dataStore");
    YouTubeUtils.put(p, YouTubeKey.keywords, "foo");
    YouTubeUtils.put(p, YouTubeKey.defaultPlaylist, "foo");
    YouTubeUtils.put(p, YouTubeKey.makeVideosPrivate, "true");
    // maxFieldLength is optional so we skip
    return p;
  }

}
