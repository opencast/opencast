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

package org.opencastproject.adminui.index;

import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.impl.TestUtils;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.User;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

@Ignore
public class AdminUIEventSearchQueryTest {
  private static final String RIGHT_ORG = "rightOrg";
  private static final String WRONG_ORG = "wrongOrg";
  private static final String TOTAL_ADMIN = "matterhorn:admin";
  private static final String ORG_OWNER = "matterhorn:owner";
  private static final String PRODUCER = "matterhorn:producer";
  private static final String EDITOR = "matterhorn:editor";
  private static final String VIEWER = "matterhorn:viewer";

  private User wrongOrgAdminUser;
  private JaxbUser totalAdmin;
  private JaxbUser noAccessUser;
  private JaxbUser readAccessUser;
  private JaxbUser writeAccessUser;
  private JaxbUser wrongRolesUser;

  private JaxbOrganization rightOrg;
  private JaxbOrganization wrongOrg;

  /** The search index */
  // protected static AdminUISearchIndexStub idx = null;
  private static AdminUISearchIndex idx;

  /** The name of the index */
  private static final String indexName = "adminui";

  @ClassRule
  public static TemporaryFolder testFolder = new TemporaryFolder();

  @BeforeClass
  public static void setupClass() throws Exception {
    TestUtils.startTesting();
    final File idxRoot = testFolder.newFolder();
    AdminUIElasticsearchUtils.createIndexConfigurationAt(idxRoot, indexName);
    idx = new AdminUISearchIndex();
    idx.activate(null);
  }

  /**
   * Does the cleanup after each test.
   */
  @After
  public void tearDown() throws Exception {
    idx.clear();
  }

  @Before
  public void setUp() throws Exception {
    rightOrg = new JaxbOrganization(RIGHT_ORG);
    wrongOrg = new JaxbOrganization(WRONG_ORG);

    wrongOrgAdminUser = new JaxbUser("Wrong Org User", "Provider", wrongOrg, new JaxbRole(ORG_OWNER, wrongOrg));
    totalAdmin = new JaxbUser("Total Admin User", "Provider", rightOrg, new JaxbRole(TOTAL_ADMIN, rightOrg));
    noAccessUser = new JaxbUser("No Access User", "Provider", rightOrg, new JaxbRole(VIEWER, rightOrg));
    wrongRolesUser = new JaxbUser("Wrong Role User", "Provider", rightOrg, new JaxbRole("Wrong:Role", rightOrg));
    readAccessUser = new JaxbUser("Read Access User", "Provider", rightOrg, new JaxbRole(EDITOR, rightOrg));
    writeAccessUser = new JaxbUser("Write Access User", "Provider", rightOrg, new JaxbRole(PRODUCER, rightOrg));

    populateIndex();
  }

  @Test
  public void aclLimited() throws SearchIndexException {
    idx.getByQuery(new EventSearchQuery(WRONG_ORG, wrongOrgAdminUser));
  }

  /**
   * Adds sample pages to the search index and returns the number of documents added.
   */
  private void populateIndex() throws Exception {
    for (int i = 0; i < 10; i++) {
      Event event = new Event(Integer.toString(i), rightOrg.getId());
      idx.addOrUpdate(event);
    }
  }
}
