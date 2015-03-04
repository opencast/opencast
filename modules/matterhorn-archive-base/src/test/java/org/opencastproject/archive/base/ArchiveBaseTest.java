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

package org.opencastproject.archive.base;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.archive.base.ArchiveBase.mkPartial;
import static org.opencastproject.archive.base.QueryBuilder.query;
import static org.opencastproject.mediapackage.MediaPackageSupport.loadFromClassPath;
import static org.opencastproject.util.UrlSupport.DEFAULT_BASE_URL;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.archive.ArchiveTestEnv;
import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.api.ResultItem;
import org.opencastproject.archive.api.ResultSet;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.archive.api.Version;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.User;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Function2;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tests the functionality of the archive base implementation. */
@Ignore
public class ArchiveBaseTest {
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

  /** Test whether an empty search index will work. */
  @Test
  public void testEmptySearchIndex() {
    ResultSet result = env.getService().find(query().mediaPackageId("foo"), env.getRewriter());
    assertEquals(0, result.size());
  }

  /** Adds a simple media package that has a dublin core for the episode only. */
  @Test
  public void testGetMediaPackage() throws Exception {
    MediaPackage mediaPackage = loadFromClassPath("/manifest-simple.xml");
    // Make sure our mocked ACL has the read and write permission
    env.setReadWritePermissions();
    env.getService().add(mediaPackage);
    // Make sure it's properly indexed and returned for authorized users
    ResultSet result = env.getService().find(query().mediaPackageId("10.0000/1"), env.getRewriter());
    assertEquals("Number of results", 1, result.size());
    assertTrue("Number of media package elements", result.getItems().get(0).getMediaPackage().getElements().length > 0);
    assertEquals("Rewritten URL", "http://episodes/10.0000/1/catalog-1/0/catalog.xml", result.getItems().get(0)
            .getMediaPackage().getElements()[0].getURI().toString());
    // delete mediapackage
    env.getService().delete(mediaPackage.getIdentifier().toString());
    assertEquals("Mediapackage has been deleted", 0,
            env.getService().find(query().mediaPackageId("10.0000/1"), env.getRewriter()).size());
    // add again
    env.getService().add(mediaPackage);
    assertEquals("Number of mediapackages in archive", 1, env.getService().find(query(), env.getRewriter()).size());
    // only ROLE_UNKNOWN is allowed to read
    env.getAcl().getEntries().clear();
    env.getAcl().getEntries().add(new AccessControlEntry("ROLE_UNKNOWN", Archive.READ_PERMISSION, true));
    env.getAcl()
            .getEntries()
            .add(new AccessControlEntry(Collections.toList(env.getUserWithPermissions().getRoles()).get(0).getName(),
                    Archive.WRITE_PERMISSION, true));
    // now add the mediapackage with this restrictive ACL to the search index
    env.getService().add(mediaPackage);
    assertEquals("Current user is not allowed to read the latest version but only the first", 1,
            env.getService().find(query(), env.getRewriter()).size());
  }

  @Test
  public void testOnlyLastVersion2() throws Exception {
    final MediaPackage mpSimple = loadFromClassPath("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    env.setReadWritePermissions();
    // create three versions of mpSimple
    env.getService().add(mpSimple);
    env.getService().add(mpSimple);
    env.getService().add(mpSimple);
    assertEquals(3, env.getService().find(query().mediaPackageId("10.0000/1"), env.getRewriter()).size());
    // modify mediapackage
    env.getService().add(mpSimple);
    {
      final ResultSet r = env.getService().find(query().mediaPackageId("10.0000/1"), env.getRewriter());
      assertEquals(4, r.size());
      // check that each added media package has a unique version
      assertEquals(
              r.size(),
              mlist(env.getService().find(query().mediaPackageId("10.0000/1"), env.getRewriter()).getItems()).foldl(
                      Collections.<Version> set(), new Function2<Set<Version>, ResultItem, Set<Version>>() {
                        @Override
                        public Set<Version> apply(Set<Version> sum, ResultItem item) {
                          sum.add(item.getVersion());
                          return sum;
                        }
                      }).size());
    }
    {
      final ResultSet r = env.getService().find(query().mediaPackageId("10.0000/1").onlyLastVersion(true),
              env.getRewriter());
      assertEquals(1, r.size());
      // todo not good to make assumptions about versions...
      assertEquals(Version.version(3), r.getItems().get(0).getVersion());
    }
  }

  /** Adds a simple media package that has a dublin core for the episode only. */
  @Test
  public void testAddSimpleMediaPackage() throws Exception {
    MediaPackage mediaPackage = loadFromClassPath("/manifest-simple.xml");

    // Make sure our mocked ACL has the read and write permission
    env.setReadWritePermissions();

    // Add the media package to the search index
    env.getService().add(mediaPackage);

    // Make sure it's properly indexed and returned
    assertEquals(1, env.getService().find(query().mediaPackageId("10.0000/1"), env.getRewriter()).size());
    assertEquals(1, env.getService().find(query(), env.getRewriter()).size());

    // Test for various fields
    ResultSet result = env.getService().find(query().mediaPackageId("10.0000/1"), env.getRewriter());
    assertEquals(1, result.getTotalSize());
    ResultItem resultItem = result.getItems().get(0);
    assertNotNull(resultItem.getMediaPackage());
    assertEquals(1, resultItem.getMediaPackage().getCatalogs().length);
  }

  @Test
  public void testOnlyLastVersion() throws Exception {
    final MediaPackage mpSimple = loadFromClassPath("/manifest-simple.xml");
    // Make sure our mocked ACL has the read and write permission
    env.setReadWritePermissions();
    // Add the media package to the search index
    env.getService().add(mpSimple);
    env.getService().add(mpSimple);
    // Make sure it's properly indexed and returned
    assertEquals(2, env.getService().find(query().mediaPackageId("10.0000/1"), env.getRewriter()).size());
    assertEquals(1, env.getService().find(query().onlyLastVersion(true), env.getRewriter()).size());
    // add another media package
    final MediaPackage mpFull = loadFromClassPath("/manifest-full.xml");
    env.getService().add(mpFull);
    assertEquals(3, env.getService().find(query(), env.getRewriter()).size());
    // now there must be two last versions
    assertEquals(2, env.getService().find(query().onlyLastVersion(true), env.getRewriter()).size());
  }

  /** Ads a simple media package that has a dublin core for the episode only. */
  @Test
  public void testAddFullMediaPackage() throws Exception {
    MediaPackage mp = loadFromClassPath("/manifest-full.xml");
    // Make sure our mocked ACL has the read and write permission
    env.setReadWritePermissions();

    // Add the media package to the search index
    env.getService().add(mp);

    // Make sure it's properly indexed and returned
    assertEquals(1, env.getService().find(query().mediaPackageId("10.0000/2"), env.getRewriter()).size());
    assertEquals(1, env.getService().find(query(), env.getRewriter()).size());
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
    assertEquals(1L, env.getService().find(query().mediaPackageId(mpId).includeDeleted(true), env.getRewriter())
            .getTotalSize());
    assertEquals(2L, env.getService().find(query().includeDeleted(true), env.getRewriter()).getTotalSize());
    assertEquals(1L, env.getService().find(query().includeDeleted(false).deletedAfter(new Date(0L)), env.getRewriter())
            .getTotalSize());
    assertEquals(1L, env.getService().find(query(), env.getRewriter()).getTotalSize());
  }

  /** Test removal from the search index. */
  @Test
  public void testDeleteMediaPackage() throws Exception {
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
    Date deletedDate = new Date();
    assertTrue(env.getService().delete(mediaPackage.getIdentifier().toString()));

    // Now go back to the original security service and user
    env.getUserResponder().setResponse(env.getDefaultUser());
    env.getOrganizationResponder().setResponse(env.getDefaultOrganization());

    assertEquals(0, env.getService().find(query().mediaPackageId("10.0000/1"), env.getRewriter()).size());
    assertEquals(0, env.getService().find(query(), env.getRewriter()).size());
    assertEquals(1, env.getService().find(query().deletedAfter(deletedDate), env.getRewriter()).size());
  }

  /**
   * Ads a media package with one dublin core for the episode and one for the series.
   *
   * todo media package needs to return a series id for this test to work
   */
  @Test
  @Ignore
  public void testAddSeriesMediaPackage() throws Exception {
    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newInstance();
    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
    URL rootUrl = ArchiveBaseTest.class.getResource("/");
    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));

    // Load the simple media package
    MediaPackage mediaPackage = null;
    InputStream is = null;
    try {
      is = ArchiveBaseTest.class.getResourceAsStream("/manifest-full.xml");
      mediaPackage = mediaPackageBuilder.loadFromXml(is);
    } catch (MediaPackageException e) {
      fail("Error loading full media package");
    } finally {
      IOUtils.closeQuietly(is);
    }

    // Make sure our mocked ACL has the read and write permission
    env.setReadWritePermissions();

    // Add the media package to the search index
    env.getService().add(mediaPackage);

    // Make sure it's properly indexed and returned
    Query q = query();

    ResultSet result = env.getService().find(q, env.getRewriter());
    assertEquals(1, result.size());
    assertEquals("foobar-serie", result.getItems().get(0).getMediaPackage().getIdentifier().toString());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testPopulateIndex() throws Exception {
    // This service registry must return a list of jobs
    List<String> args = new ArrayList<String>();
    args.add(new DefaultOrganization().getId());

    // create 10 empty media packages, no need to rewrite for archival
    for (long i = 0; i < 10; i++) {
      final MediaPackage mediaPackage = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
      mediaPackage.setIdentifier(IdBuilderFactory.newInstance().newIdBuilder().createNew());
      env.getEpisodeDatabase().storeEpisode(mkPartial(mediaPackage), env.getAcl(), new Date(), Version.FIRST);
    }
    // load one with an mpeg7 catalog attached
    final MediaPackage mpWithMpeg7 = loadFromClassPath("/manifest-full.xml");
    ArchiveBase.rewriteAssetsForArchival(mkPartial(mpWithMpeg7), Version.FIRST);
    env.getEpisodeDatabase().storeEpisode(mkPartial(mpWithMpeg7), env.getAcl(), new Date(), Version.FIRST);

    // We should have nothing in the search index
    assertEquals(0, env.getService().find(query(), env.getRewriter()).size());

    // todo
    env.getService().populateIndex(new UriRewriter() {
      @Override
      public URI apply(Version version, MediaPackageElement mediaPackageElement) {
        // only the URI of the mpeg7 file needs to be rewritten so it is safe to return a static URL
        try {
          return getClass().getResource("/mpeg7.xml").toURI();
        } catch (URISyntaxException e) {
          return chuck(e);
        }
      }
    });

    // This time we should have 11 results
    assertEquals(11, env.getService().find(query(), env.getRewriter()).size());
  }

  @Test
  public void testFindByDate() throws Exception {
    final MediaPackage mp = loadFromClassPath("/manifest-full.xml");
    env.setReadWritePermissions();
    env.getService().add(mp);
    final Date dayAhead = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
    assertEquals(1, env.getService().find(query().archivedBefore(dayAhead), env.getRewriter()).size());
    assertEquals(0, env.getService().find(query().archivedAfter(dayAhead), env.getRewriter()).size());
    assertEquals(1,
            env.getService().find(query().archivedAfter(new Date(0)).archivedBefore(dayAhead), env.getRewriter())
                    .size());
  }
}
