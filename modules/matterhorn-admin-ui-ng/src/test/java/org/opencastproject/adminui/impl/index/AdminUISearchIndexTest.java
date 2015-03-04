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

package org.opencastproject.adminui.impl.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.PathSupport;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Test case for {@link AdminUISearchIndex}.
 */
public class AdminUISearchIndexTest {

  /** The search index */
  protected static AdminUISearchIndexStub idx = null;

  /** The index root directory */
  protected static File idxRoot = null;

  /** The name of the index */
  protected static final String indexName = "adminui";

  /** The index version */
  protected static final int indexVersion = 123;

  /** Flag to indicate read only index */
  protected static boolean isReadOnly = false;

  private final Organization defaultOrganization = new DefaultOrganization();

  /**
   * Sets up the solr search index. Since solr sometimes has a hard time shutting down cleanly, it's done only once for
   * all the tests.
   *
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    // Index
    String rootPath = PathSupport.concat(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
    System.setProperty("matterhorn.home", rootPath);
    idxRoot = new File(rootPath);
    String indexRoot = AdminUIElasticsearchUtils.createIndexConfigurationAt(idxRoot, indexName);
    idx = new AdminUISearchIndexStub(indexName, indexVersion, indexRoot);
  }

  /**
   * Does the cleanup after the test suite.
   */
  @After
  public void tearDown() {
    try {
      if (idx != null) {
        idx.clear();
        idx.close();
      }
      FileUtils.deleteQuietly(idxRoot);
    } catch (IOException e) {
      fail("Error closing search index: " + e.getMessage());
    }
  }

  /**
   * Test method for {@link org.opencastproject.matterhorn.search.impl.AbstractElasticsearchIndex#getIndexVersion()} .
   */
  @Test
  public void testGetIndexVersion() throws Exception {
    populateIndex();
    assertEquals(indexVersion, idx.getIndexVersion());
  }

  @Test
  public void testEventPermissionQuery() throws Exception {
    int indexCount = populateIndex();
    User anonymousUser = SecurityUtil.createAnonymousUser(defaultOrganization);
    SearchResult<Event> result = idx.getByQuery(new EventSearchQuery(defaultOrganization.getId(), anonymousUser));
    assertEquals(0, result.getHitCount());

    User adminUser = SecurityUtil.createSystemUser("admin", defaultOrganization);
    result = idx.getByQuery(new EventSearchQuery(defaultOrganization.getId(), adminUser));
    assertEquals(indexCount, result.getHitCount());

    JaxbOrganization jaxbOrg = JaxbOrganization.fromOrganization(defaultOrganization);
    User permissionUser = new JaxbUser("userwithpermissions", "test", jaxbOrg, new JaxbRole("ROLE_EVENTS", jaxbOrg));
    result = idx.getByQuery(new EventSearchQuery(defaultOrganization.getId(), permissionUser));
    assertEquals(5, result.getHitCount());

    result = idx.getByQuery(new EventSearchQuery(defaultOrganization.getId(), permissionUser)
            .withAction(Permissions.Action.WRITE));
    assertEquals(0, result.getHitCount());
  }

  @Test
  public void testSeriesPermissionQuery() throws Exception {
    int indexCount = populateIndex();
    User anonymousUser = SecurityUtil.createAnonymousUser(defaultOrganization);
    SearchResult<Series> result = idx.getByQuery(new SeriesSearchQuery(defaultOrganization.getId(), anonymousUser));
    assertEquals(0, result.getHitCount());

    User adminUser = SecurityUtil.createSystemUser("admin", defaultOrganization);
    result = idx.getByQuery(new SeriesSearchQuery(defaultOrganization.getId(), adminUser));
    assertEquals(indexCount, result.getHitCount());

    JaxbOrganization jaxbOrg = JaxbOrganization.fromOrganization(defaultOrganization);
    User permissionUser = new JaxbUser("userwithpermissions", "test", jaxbOrg, new JaxbRole("ROLE_EVENTS", jaxbOrg));
    result = idx.getByQuery(new SeriesSearchQuery(defaultOrganization.getId(), permissionUser));
    assertEquals(5, result.getHitCount());

    result = idx.getByQuery(new SeriesSearchQuery(defaultOrganization.getId(), permissionUser)
            .withAction(Permissions.Action.WRITE));
    assertEquals(0, result.getHitCount());
  }

  /**
   * Adds sample pages to the search index and returns the number of documents added.
   *
   * @return the number of pages added
   */
  protected int populateIndex() throws Exception {
    int count = 0;

    final AccessControlList readAcl = new AccessControlList(new AccessControlEntry("ROLE_EVENTS",
            Permissions.Action.READ.toString(), true));

    final AccessControlList writeAcl = new AccessControlList(new AccessControlEntry("ROLE_EVENTS",
            Permissions.Action.WRITE.toString(), true));

    String readAclJson = AccessControlParser.toJsonSilent(readAcl);
    String writeAclJson = AccessControlParser.toJsonSilent(writeAcl);

    // Add content to the index
    for (int i = 0; i < 10; i++) {
      Series series = new Series(Integer.toString(i), defaultOrganization.getId());
      Event event = new Event(Integer.toString(i), defaultOrganization.getId());
      if (i % 2 == 0) {
        series.setAccessPolicy(readAclJson);
        event.setAccessPolicy(readAclJson);
      } else {
        series.setAccessPolicy(writeAclJson);
        event.setAccessPolicy(writeAclJson);
      }
      idx.addOrUpdate(event);
      idx.addOrUpdate(series);
      count++;
    }

    return count;
  }
}
