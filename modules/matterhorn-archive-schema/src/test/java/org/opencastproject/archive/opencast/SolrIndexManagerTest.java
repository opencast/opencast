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

package org.opencastproject.archive.opencast;

import static org.opencastproject.mediapackage.MediaPackageSupport.loadFromClassPath;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.archive.opencast.solr.SolrIndexManager;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.dublincore.StaticMetadataServiceDublinCoreImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.solr.SolrServerFactory;
import org.opencastproject.util.data.VCell;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;

public class SolrIndexManagerTest {

  private SolrIndexManager solrIndex;
  private SolrServer solrServer;
  private Workspace workspace;
  private static String baseDirPath;

  /** The solr root directory */
  private final String solrRoot = "target" + File.separator + "opencast" + File.separator + "searchindex";

  /** The access control list returned by the mocked authorization service */
  private AccessControlList acl = new AccessControlList();

  protected static final String getStorageRoot() {
    return "." + File.separator + "target" + File.separator + System.currentTimeMillis();
  }

  @BeforeClass
  public static void doOnce() throws Exception {
    // Get the base directory
    URI baseDirURI = SolrIndexManagerTest.class.getResource("/").toURI();
    baseDirPath = (new File(baseDirURI)).getAbsolutePath();
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {

    // workspace mock to return the test data
    workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.read((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        String pathString = EasyMock.getCurrentArguments()[0].toString();
        URI newUri = new URI(pathString);
        File file = new File(baseDirPath + "/" + newUri);
        return file;
      }
    }).anyTimes();
    EasyMock.replay(workspace);

    // mpeg7 service
    final Mpeg7CatalogService mpeg7CatalogService = new Mpeg7CatalogService();

    // series service
    SeriesService seriesService = EasyMock.createNiceMock(SeriesService.class);

    // security service
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    JaxbOrganization org = new JaxbOrganization("mh-default-org");
    User user = new JaxbUser("admin", "test", org, new JaxbRole(SecurityConstants.GLOBAL_ADMIN_ROLE, org));
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    // metadata services
    StaticMetadataServiceDublinCoreImpl metadataSvcs = new StaticMetadataServiceDublinCoreImpl();
    metadataSvcs.setWorkspace(workspace);

    // solr server
    solrServer = OpencastArchivePublisher.setupSolr(new File(solrRoot));

    solrIndex = new SolrIndexManager(solrServer, workspace,
            VCell.cell(Arrays.asList((StaticMetadataService) metadataSvcs)), seriesService, mpeg7CatalogService,
            securityService);
  }

  @After
  public void tearDown() {
    SolrServerFactory.shutdown(solrServer);
    try {
      FileUtils.deleteDirectory(new File(solrRoot));
    } catch (IOException e) {
      chuck(e);
    }
    solrIndex = null;
  }

  @Test
  public void testDefaultDelete10Versions() throws Exception {
    final MediaPackage mpSimple = loadFromClassPath("/manifest-simple.xml");

    int moreThan10 = 13;
    // create more than 10 versions of mpSimple
    for (int i = 0; i < moreThan10; i++) {
      solrIndex.add(mpSimple, acl, new Date(), org.opencastproject.archive.api.Version.version(i));
    }
    {
      // Setting limit to 0 skips over setting the result limit and uses the default 10
      // ref https://wiki.apache.org/solr/CommonQueryParameters#rows
      solrIndex.setSearchResultLimit(0);
      Boolean isDeleted = solrIndex.delete("10.0000/1", new Date());
      Assert.assertTrue("deleted a bunch successfully", isDeleted);
      isDeleted = solrIndex.delete("10.0000/1", new Date());
      Assert.assertTrue("still deleted more after first delete request", isDeleted);
    }
  }

  @Test
  public void testBigRowLimitDeleteAllVersions() throws Exception {
    final MediaPackage mpSimple = loadFromClassPath("/manifest-simple.xml");

    int moreThan10 = 13;
    // create more than 10 versions of mpSimple
    for (int i = 0; i < moreThan10; i++) {
      solrIndex.add(mpSimple, acl, new Date(), org.opencastproject.archive.api.Version.version(i));
    }
    {
      // Default uses the new big limit (max int)
      // ref https://wiki.apache.org/solr/CommonQueryParameters#rows
      Boolean isDeleted = solrIndex.delete("10.0000/1", new Date());
      Assert.assertTrue("deleted a bunch successfully", isDeleted);
      isDeleted = solrIndex.delete("10.0000/1", new Date());
      Assert.assertFalse("No more have been deleted, all were deleted the first time", isDeleted);
    }
  }
}
