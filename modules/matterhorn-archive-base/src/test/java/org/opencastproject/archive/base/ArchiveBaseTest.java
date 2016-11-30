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


package org.opencastproject.archive.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.mediapackage.MediaPackageSupport.loadFromClassPath;
import static org.opencastproject.util.UrlSupport.DEFAULT_BASE_URL;

import org.opencastproject.archive.ArchiveTestEnv;
import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.base.persistence.Episode;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.User;

import org.opencastproject.util.data.Option;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Tests the functionality of the archive base implementation. */
// @Ignore
public class ArchiveBaseTest {
  private Logger logger = LoggerFactory.getLogger(ArchiveBaseTest.class);
  private ArchiveTestEnv env;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    env = new ArchiveTestEnv();
  }

  @After
  public void tearDown() throws Exception {
    env.tearDown();
  }

  /** Adds a simple media package that has a dublin core for the episode only. */
  @Test
  public void testAddSimpleMediaPackage() throws Exception {
    final MediaPackage mediaPackage = loadFromClassPath("/manifest-simple.xml");
    final MediaPackage mediaPackageUpdated = loadFromClassPath("/manifest-simple-updated.xml");
    final MediaPackage mp2 = loadFromClassPath("/manifest-full.xml");
    final String mpId = mediaPackage.getIdentifier().toString();
    // Make sure our mocked ACL has the read and write permission
    env.setReadWritePermissions();

    // Add the media package to the search index
    env.getService().add(mediaPackage);
    env.getService().add(mediaPackageUpdated);
    env.getService().add(mp2);
    env.getService().add(mediaPackage);
    env.getService().add(mediaPackageUpdated);
    env.getService().add(mediaPackage);
    env.getService().add(mediaPackageUpdated);
    env.getService().add(mp2);

    // Make sure it's properly stored
    Option<Episode> result = env.getPersistence().getLatestEpisode(mpId);
    assertTrue(result.isSome());
    // Test for various fields
    assertEquals(1, result.get().getMediaPackage().getCatalogs().length);
    // verify that the current is the 6th version (i.e. 0-5 versions)
    assertEquals(5L, result.get().getVersion().value());

    // Verify that the 4 versions exist in the db
    Collection<Episode> eCol = new ArrayList<Episode>();
    Iterator<Episode> iter = env.getPersistence().getAllEpisodes();
    while (iter.hasNext()) {
      Episode ep = iter.next();
      eCol.add(ep);
      logger.info(ep.getVersion().toString());
    }
    // verify that all versions exist
    assertEquals(8, eCol.size());
  }

  @Test
  public void testDelete() throws Exception {
    final MediaPackage mp1 = loadFromClassPath("/manifest-simple.xml");
    final MediaPackage mp2 = loadFromClassPath("/manifest-full.xml");
    final String mpId = mp1.getIdentifier().toString();
    // Make sure our mocked ACL has the read and write permission
    env.setReadWritePermissions();
    // add both media packages
    env.getService().add(mp1);
    env.getService().add(mp2);
    assertTrue(env.getService().delete(mpId));

    Iterator<Episode> iter = env.getPersistence().getAllEpisodes();
    while (iter.hasNext()) {
      Episode ep = iter.next();
      if (ep.getMediaPackage().getIdentifier().equals(mp1.getIdentifier())) {
        assertTrue(ep.isDeleted());
      } else {
        assertFalse(ep.isDeleted());
      }
    }
  }

  /** Test removal from the search index. */
  @Test
  public void testPermissionToDelete() throws Exception {
    MediaPackage mediaPackage = loadFromClassPath("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    env.setReadWritePermissions();

    // Add the media package to the search index
    env.getService().add(mediaPackage);

    // Now take the role away from the user
    env.getUserResponder().setResponse(env.getUserWithoutPermissions());

    Map<String, Integer> servers = new HashMap<String, Integer>();
    servers.put(DEFAULT_BASE_URL, 8080);
    env.getOrganizationResponder().setResponse(
            new JaxbOrganization(DefaultOrganization.DEFAULT_ORGANIZATION_ID,
                    DefaultOrganization.DEFAULT_ORGANIZATION_NAME, servers,
                    DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN, DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS,
                    null));

    // Try to delete it
    try {
      env.getService().delete(mediaPackage.getIdentifier().toString());
      fail("Unauthorized user was able to delete a mediapackage");
    } catch (ArchiveException e) {
      assertTrue(e.isCauseNotAuthorized());
    }

    // Second try with a "fixed" roleset
    User adminUser = new JaxbUser("admin", "test", env.getOpencastOrganization(), new JaxbRole(env
            .getDefaultOrganization().getAdminRole(), env.getOpencastOrganization()));
    env.getUserResponder().setResponse(adminUser);
    assertTrue(env.getService().delete(mediaPackage.getIdentifier().toString()));

    // Now go back to the original security service and user
    env.getUserResponder().setResponse(env.getDefaultUser());
    env.getOrganizationResponder().setResponse(env.getDefaultOrganization());

    Option<Episode> result = env.getPersistence().getLatestEpisode("10.0000/1");
    assertTrue(result.get().isDeleted());
  }
}
