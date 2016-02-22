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
package org.opencastproject.migration;

import static org.opencastproject.util.PathSupport.path;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.api.ResultSet;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.opencast.OpencastResultItem;
import org.opencastproject.archive.opencast.OpencastResultSet;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.FileSupport;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DistributionMigrationServiceTest {

  /**
   * Test class for the distribution migration service
   */
  private DistributionMigrationService distributionMigrationService = new DistributionMigrationService();

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    List<Organization> orgs = new ArrayList<>();
    orgs.add(new DefaultOrganization());
    orgs.add(new JaxbOrganization("test_org"));
    OrganizationDirectoryService orgDirService = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDirService.getOrganizations()).andReturn(orgs).anyTimes();
    EasyMock.replay(orgDirService);

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(new JaxbUser()).anyTimes();
    EasyMock.replay(securityService);

    ResultSet rs = new OpencastResultSet() {
      @Override
      public long size() {
        return 1;
      }

      @Override
      public List<OpencastResultItem> getItems() {
        List<OpencastResultItem> items = new ArrayList<>();
        return items;
      }

      @Override
      public String getQuery() {
        return "";
      }

      @Override
      public long getTotalSize() {
        return 3;
      }

      @Override
      public long getLimit() {
        return 0;
      }

      @Override
      public long getOffset() {
        return 0;
      }

      @Override
      public long getSearchTime() {
        return 0;
      }
    };

    Archive<ResultSet> archive = EasyMock.createNiceMock(Archive.class);
    EasyMock.expect(
            archive.findForAdministrativeRead(EasyMock.anyObject(Query.class), EasyMock.anyObject(UriRewriter.class)))
            .andReturn(rs).anyTimes();
    EasyMock.replay(archive);

    HttpMediaPackageElementProvider httpMediaPackageElementProvider = EasyMock
            .createNiceMock(HttpMediaPackageElementProvider.class);
    EasyMock.expect(httpMediaPackageElementProvider.getUriRewriter()).andReturn(new UriRewriter() {
      @Override
      public URI apply(Version v, MediaPackageElement mpe) {
        return null;
      }
    }).anyTimes();
    EasyMock.replay(httpMediaPackageElementProvider);

    distributionMigrationService.setOrganizationDirectoryService(orgDirService);
    distributionMigrationService.setSecurityService(securityService);
    distributionMigrationService.setArchive(archive);
    distributionMigrationService.setHttpMediaPackageElementProvider(httpMediaPackageElementProvider);
  }

  @Test
  public void testDistributionMigration() throws Exception {
    File mp = new File(getClass().getResource("/mediapackage.xml").toURI());
    File mediaPackageRoot = mp.getParentFile();
    Path rootPath = new File(path(mediaPackageRoot.getAbsolutePath(), "distribution")).toPath();
    FileSupport.delete(rootPath.toFile(), true);
    Files.createDirectory(rootPath);

    Path file = new File(path(rootPath.toString(), "engage-player", "12345", "67890", "mediapackage.xml")).toPath();
    Path expectedFile = new File(
            path(rootPath.toString(), "mh_default_org", "engage-player", "12345", "67890", "mediapackage.xml"))
                    .toPath();
    FileSupport.copy(mp, file.toFile());

    BundleContext bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.download.directory")).andReturn("").anyTimes();
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.download.url")).andReturn("").anyTimes();
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.streaming.directory")).andReturn(rootPath.toString())
            .anyTimes();
    EasyMock.expect(bundleContext.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER)).andReturn("root").anyTimes();
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.streaming.url"))
            .andReturn("rtmp://localhost:8080/streaming").anyTimes();

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();

    EasyMock.replay(bundleContext, cc);

    Assert.assertTrue(Files.exists(file));
    Assert.assertFalse(Files.exists(expectedFile));

    distributionMigrationService.activate(cc);

    Assert.assertFalse(Files.exists(file));
    Assert.assertTrue(Files.exists(expectedFile));
  }

}
