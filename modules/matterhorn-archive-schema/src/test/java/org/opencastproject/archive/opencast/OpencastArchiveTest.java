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

//import org.apache.commons.io.IOUtils;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
//import org.opencastproject.mediapackage.MediaPackage;
//import org.opencastproject.mediapackage.MediaPackageBuilder;
//import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
//import org.opencastproject.mediapackage.MediaPackageElement;
//import org.opencastproject.mediapackage.MediaPackageException;
//import org.opencastproject.mediapackage.identifier.IdBuilderFactory;
//import org.opencastproject.security.api.AccessControlEntry;
//import org.opencastproject.security.api.DefaultOrganization;
//import org.opencastproject.security.api.JaxbOrganization;
//import org.opencastproject.security.api.User;
//import org.opencastproject.util.data.Collections;
//import org.opencastproject.util.data.Function;
//import org.opencastproject.util.data.Function2;
//
//import javax.naming.directory.SearchResult;
//import java.io.InputStream;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import static junit.framework.Assert.assertNotNull;
//import static junit.framework.Assert.fail;
//import static org.junit.Assert.assertTrue;
//import static org.opencastproject.mediapackage.MediaPackageSupport.loadFromClassPath;
//import static org.opencastproject.util.UrlSupport.DEFAULT_BASE_URL;
//import static org.opencastproject.util.data.functions.Booleans.and;
//import static org.opencastproject.util.data.functions.Functions.uncurry;
//import static org.opencastproject.util.data.functions.Misc.chuck;

import org.junit.Ignore;

@Ignore
public class OpencastArchiveTest {

//  private ArchiveTestEnv env;
//
//  @SuppressWarnings("unchecked")
//  @Before
//  public void setUp() throws Exception {
//    env = new EpisodeServiceTestEnv();
//  }
//
//  @After
//  public void tearDown() throws Exception {
//    env.tearDown();
//  }
//
//  /** Test whether an empty search index will work. */
//  @Test
//  public void testEmptySearchIndex() {
//    OpencastResultSet result = env.getService().find(systemQuery().id("foo"), env.getRewriter());
//    assertEquals(0, result.size());
//  }
//
//  /** Adds a simple media package that has a dublin core for the episode only. */
//  @Test
//  public void testGetMediaPackage() throws Exception {
//    MediaPackage mediaPackage = loadFromClassPath("/manifest-simple.xml");
//    // Make sure our mocked ACL has the read and write permission
//    env.setReadWritePermissions();
//    env.getService().add(mediaPackage);
//    // Make sure it's properly indexed and returned for authorized users
//    SearchResult result = env.getService().find(systemQuery().id("10.0000/1"), env.getRewriter());
//    assertEquals("Number of results", 1, result.size());
//    assertTrue("Number of media package elements", result.getItems().get(0).getMediaPackage().getElements().length > 0);
//    assertEquals("Rewritten URL",
//                 "http://episodes/10.0000/1/catalog-1/0/catalog.xml",
//                 result.getItems().get(0).getMediaPackage().getElements()[0].getURI().toString());
//    // delete mediapackage
//    env.getService().delete(mediaPackage.getIdentifier().toString());
//    assertEquals("Mediapackage has been deleted", 0, env.getService().find(systemQuery().id("10.0000/1"), env.getRewriter()).size());
//    // add again
//    env.getService().add(mediaPackage);
//    assertEquals("Number of mediapackages in archive", 1, env.getService().find(systemQuery(), env.getRewriter()).size());
//    // only ROLE_UNKNOWN is allowed to read
//    env.getAcl().getEntries().clear();
//    env.getAcl().getEntries().add(new AccessControlEntry("ROLE_UNKNOWN", EpisodeService.READ_PERMISSION, true));
//    env.getAcl().getEntries().add(
//            new AccessControlEntry(env.getUserWithPermissions().getRoles()[0], EpisodeService.WRITE_PERMISSION, true));
//    // now add the mediapackage with this restrictive ACL to the search index
//    env.getService().add(mediaPackage);
//    assertEquals("Current user is not allowed to read the latest version but only the first",
//                 1, env.getService().find(systemQuery(), env.getRewriter()).size());
//  }
//
//  @Test
//  public void testOnlyLastVersion2() throws Exception {
//    final MediaPackage mpSimple = loadFromClassPath("/manifest-simple.xml");
//
//    // Make sure our mocked ACL has the read and write permission
//    env.setReadWritePermissions();
//    // create three versions of mpSimple
//    env.getService().add(mpSimple);
//    env.getService().add(mpSimple);
//    env.getService().add(mpSimple);
//    assertEquals(3, env.getService().find(systemQuery().id("10.0000/1"), env.getRewriter()).size());
//    // modify mediapackage
//    env.getService().add(mpSimple);
//    {
//      final SearchResult r = env.getService().find(systemQuery().id("10.0000/1"), env.getRewriter());
//      assertEquals(4, r.size());
//      // check that each added media package has a unique version
//      assertEquals(
//              r.size(),
//              mlist(env.getService().find(systemQuery().id("10.0000/1"), env.getRewriter()).getItems()).foldl(Collections.<Version> set(),
//                      new Function2<Set<Version>, SearchResultItem, Set<Version>>() {
//                        @Override
//                        public Set<Version> apply(Set<Version> sum, SearchResultItem item) {
//                          sum.add(item.getOcVersion());
//                          return sum;
//                        }
//                      }).size());
//    }
//    {
//      final SearchResult r = env.getService().find(systemQuery().id("10.0000/1").onlyLastVersion(), env.getRewriter());
//      assertEquals(1, r.size());
//      // todo not good to make assumptions about versions...
//      assertEquals(Version.version(3), r.getItems().get(0).getOcVersion());
//    }
//  }
//
//  /** Tests whether an episode can be found based on its series metadata. */
//  @Test
//  @Ignore
//  public void testSearchForEpisodeWithSeriesMetadata() throws Exception {
//    MediaPackage mediaPackage = loadFromClassPath("/manifest-full.xml");
//    env.getService().add(mediaPackage);
//
//    SearchResult episodeMetadataResult = env.getService().find(systemQuery().text("Vegetation"), env.getRewriter());
//    SearchResult seriesMetadataResult = env.getService().find(systemQuery().text("Atmospheric Science"), env.getRewriter());
//
//    assertEquals(1, episodeMetadataResult.getItems().size());
//    assertEquals(1, seriesMetadataResult.getItems().size());
//  }
//
//  /** Adds a simple media package that has a dublin core for the episode only. */
//  @Test
//  public void testAddSimpleMediaPackage() throws Exception {
//    MediaPackage mediaPackage = loadFromClassPath("/manifest-simple.xml");
//
//    // Make sure our mocked ACL has the read and write permission
//    env.setReadWritePermissions();
//
//    // Add the media package to the search index
//    env.getService().add(mediaPackage);
//
//    // Make sure it's properly indexed and returned
//    EpisodeQuery q = systemQuery();
//    q.id("10.0000/1");
//    assertEquals(1, env.getService().find(q, env.getRewriter()).size());
//
//    q = systemQuery();
//
//    assertEquals(1, env.getService().find(q, env.getRewriter()).size());
//
//    // Test for various fields
//    q = systemQuery();
//    q.id("10.0000/1");
//    SearchResult result = env.getService().find(q, env.getRewriter());
//    assertEquals(1, result.getTotalSize());
//    SearchResultItem resultItem = result.getItems().get(0);
//    assertNotNull(resultItem.getMediaPackage());
//    assertEquals(1, resultItem.getMediaPackage().getCatalogs().length);
//  }
//
//  @Test
//  public void testOnlyLastVersion() throws Exception {
//    final MediaPackage mpSimple = loadFromClassPath("/manifest-simple.xml");
//    // Make sure our mocked ACL has the read and write permission
//    env.setReadWritePermissions();
//    // Add the media package to the search index
//    env.getService().add(mpSimple);
//    env.getService().add(mpSimple);
//    // Make sure it's properly indexed and returned
//    assertEquals(2, env.getService().find(systemQuery().id("10.0000/1"), env.getRewriter()).size());
//    assertEquals(1, env.getService().find(systemQuery().onlyLastVersion(), env.getRewriter()).size());
//    // add another media package
//    final MediaPackage mpFull = loadFromClassPath("/manifest-full.xml");
//    env.getService().add(mpFull);
//    assertEquals(3, env.getService().find(systemQuery(), env.getRewriter()).size());
//    // now there must be two last versions
//    assertEquals(2, env.getService().find(systemQuery().onlyLastVersion(), env.getRewriter()).size());
//  }
//
//  /** Ads a simple media package that has a dublin core for the episode only. */
//  @Test
//  public void testAddFullMediaPackage() throws Exception {
//    MediaPackage mp = loadFromClassPath("/manifest-full.xml");
//    // Make sure our mocked ACL has the read and write permission
//    env.setReadWritePermissions();
//
//    // Add the media package to the search index
//    env.getService().add(mp);
//
//    // Make sure it's properly indexed and returned
//    assertEquals(1, env.getService().find(systemQuery().id("10.0000/2"), env.getRewriter()).size());
//    assertEquals(1, env.getService().find(systemQuery(), env.getRewriter()).size());
//  }
//
//  @Test
//  public void testDelete() throws Exception {
//    final MediaPackage mp1 = loadFromClassPath("/manifest-simple.xml");
//    final MediaPackage mp2 = loadFromClassPath("/manifest-full.xml");
//    final String mpId = mp1.getIdentifier().toString();
//    // Make sure our mocked ACL has the read and write permission
//    env.setReadWritePermissions();
//    // add both media packages
//    env.getService().add(mp1);
//    env.getService().add(mp2);
//    assertTrue(env.getService().delete(mpId));
//    assertEquals(1L, env.getService().find(systemQuery().id(mpId).includeDeleted(true), env.getRewriter()).getTotalSize());
//    assertEquals(2L, env.getService().find(systemQuery().includeDeleted(true), env.getRewriter()).getTotalSize());
//    assertEquals(1L, env.getService().find(systemQuery().includeDeleted(false).deletedSince(new Date(0L)), env.getRewriter()).getTotalSize());
//    assertEquals(1L, env.getService().find(systemQuery(), env.getRewriter()).getTotalSize());
//  }
//
//  /** Test removal from the search index. */
//  @Test
//  public void testDeleteMediaPackage() throws Exception {
//    MediaPackage mediaPackage = loadFromClassPath("/manifest-simple.xml");
//
//    // Make sure our mocked ACL has the read and write permission
//    env.setReadWritePermissions();
//
//    // Add the media package to the search index
//    env.getService().add(mediaPackage);
//
//    // Now take the role away from the user
//    env.getUserResponder().setResponse(env.getUserWithoutPermissions());
//
//    Map<String, Integer> servers = new HashMap<String, Integer>();
//    servers.put(DEFAULT_BASE_URL, 8080);
//    env.getOrganizationResponder().setResponse(new JaxbOrganization(DefaultOrganization.DEFAULT_ORGANIZATION_ID,
//                                                                    DefaultOrganization.DEFAULT_ORGANIZATION_NAME, servers, DefaultOrganization.DEFAULT_ORGANIZATION_ADMIN,
//                                                                    DefaultOrganization.DEFAULT_ORGANIZATION_ANONYMOUS, null));
//
//    // Try to delete it
//    try {
//      env.getService().delete(mediaPackage.getIdentifier().toString());
//      fail("Unauthorized user was able to delete a mediapackage");
//    } catch (EpisodeServiceException e) {
//      assertTrue(e.isCauseNotAuthorized());
//    }
//
//    // Second try with a "fixed" roleset
//    User adminUser = new User("admin", "opencastproject.org", new String[] { new DefaultOrganization().getAdminRole() });
//    env.getUserResponder().setResponse(adminUser);
//    Date deletedDate = new Date();
//    assertTrue(env.getService().delete(mediaPackage.getIdentifier().toString()));
//
//    // Now go back to the original security service and user
//    env.getUserResponder().setResponse(env.getDefaultUser());
//    env.getOrganizationResponder().setResponse(env.getDefaultOrganization());
//
//    assertEquals(0, env.getService().find(systemQuery().id("10.0000/1"), env.getRewriter()).size());
//    assertEquals(0, env.getService().find(systemQuery(), env.getRewriter()).size());
//    assertEquals(1, env.getService().find(systemQuery().deletedSince(deletedDate), env.getRewriter()).size());
//  }
//
//  /**
//   * Ads a media package with one dublin core for the episode and one for the series.
//   *
//   * todo media package needs to return a series id for this test to work
//   */
//  @Test
//  @Ignore
//  public void testAddSeriesMediaPackage() throws Exception {
//    MediaPackageBuilderFactory builderFactory = MediaPackageBuilderFactory.newDefaultInstance();
//    MediaPackageBuilder mediaPackageBuilder = builderFactory.newMediaPackageBuilder();
//    URL rootUrl = org.opencastproject.episode.base.OpencastArchiveTest.class.getResource("/");
//    mediaPackageBuilder.setSerializer(new DefaultMediaPackageSerializerImpl(rootUrl));
//
//    // Load the simple media package
//    MediaPackage mediaPackage = null;
//    InputStream is = null;
//    try {
//      is = org.opencastproject.episode.base.OpencastArchiveTest.class.getResourceAsStream("/manifest-full.xml");
//      mediaPackage = mediaPackageBuilder.loadFromXml(is);
//    } catch (MediaPackageException e) {
//      fail("Error loading full media package");
//    } finally {
//      IOUtils.closeQuietly(is);
//    }
//
//    // Make sure our mocked ACL has the read and write permission
//    env.setReadWritePermissions();
//
//    // Add the media package to the search index
//    env.getService().add(mediaPackage);
//
//    // Make sure it's properly indexed and returned
//    EpisodeQuery q = systemQuery();
//
//    SearchResult result = env.getService().find(q, env.getRewriter());
//    assertEquals(1, result.size());
//    assertEquals("foobar-serie", result.getItems().get(0).getId());
//  }
//
//  @SuppressWarnings("unchecked")
//  @Test
//  public void testPopulateIndex() throws Exception {
//    // This service registry must return a list of jobs
//    List<String> args = new ArrayList<String>();
//    args.add(new DefaultOrganization().getId());
//
//    // create 10 empty media packages, no need to rewrite for archival
//    for (long i = 0; i < 10; i++) {
//      final MediaPackage mediaPackage = MediaPackageBuilderFactory.newDefaultInstance().newMediaPackageBuilder().createNew();
//      mediaPackage.setIdentifier(IdBuilderFactory.newDefaultInstance().newIdBuilder().createNew());
//      env.getEpisodeDatabase().storeEpisode(mkPartial(mediaPackage), env.getAcl(), new Date(), Version.FIRST);
//    }
//    // load one with an mpeg7 catalog attached
//    final MediaPackage mpWithMpeg7 = loadFromClassPath("/manifest-full.xml");
//    EpisodeServiceBase.rewriteAssetsForArchival(mkPartial(mpWithMpeg7), Version.FIRST);
//    env.getEpisodeDatabase().storeEpisode(mkPartial(mpWithMpeg7), env.getAcl(), new Date(), Version.FIRST);
//
//    // We should have nothing in the search index
//    assertEquals(0, env.getService().find(systemQuery(), env.getRewriter()).size());
//
//    // todo
//    env.getService().populateIndex(new UriRewriter() {
//      @Override
//      public URI apply(Version version, MediaPackageElement mediaPackageElement) {
//        // only the URI of the mpeg7 file needs to be rewritten so it is safe to return a static URL
//        try {
//          return getClass().getResource("/mpeg7.xml").toURI();
//        } catch (URISyntaxException e) {
//          return chuck(e);
//        }
//      }
//    });
//
//    // This time we should have 11 results
//    assertEquals(11, env.getService().find(systemQuery(), env.getRewriter()).size());
//  }
//
//  @Test
//  public void testFindByDate() throws Exception {
//    final MediaPackage mp = loadFromClassPath("/manifest-full.xml");
//    env.setReadWritePermissions();
//    env.getService().add(mp);
//    final Date dayAhead = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
//    assertEquals(1, env.getService().find(systemQuery().addedBefore(dayAhead), env.getRewriter()).size());
//    assertEquals(0, env.getService().find(systemQuery().addedAfter(dayAhead), env.getRewriter()).size());
//    assertEquals(1, env.getService().find(systemQuery().addedAfter(new Date(0)).addedBefore(dayAhead), env.getRewriter()).size());
//  }
//
//  @Test
//  public void testFindTextAndSorting() throws Exception {
//    final EpisodeService e = env.getService();
//    final UriRewriter r = env.getRewriter();
//    final MediaPackage mpa = loadFromClassPath("/manifest-a.xml");
//    final MediaPackage mpb = loadFromClassPath("/manifest-b.xml");
//    final MediaPackage mpc = loadFromClassPath("/manifest-c.xml");
//    env.setReadWritePermissions();
//    e.add(mpa);
//    e.add(mpb);
//    e.add(mpc);
//    assertEquals("Number of episodes", 3, e.find(systemQuery(), r).size());
//    assertTrue("Each episode has a series title",
//               mlist(e.find(systemQuery(), r).getItems()).foldl(true, uncurry(and.curry().o(hasSeriesTitle)).flip()));
//    assertEquals("mp-a", 1, e.find(systemQuery().id("mp-a"), r).size());
//    assertEquals("Title of mp-a", "Aurelien", e.find(systemQuery().id("mp-a"), r).getItems().get(0).getDcTitle());
//    assertEquals("Title of mp-b", "Brighton Rock", e.find(systemQuery().id("mp-b"), r).getItems().get(0).getDcTitle());
//    assertEquals("Title of mp-a", "World of Tiers", e.find(systemQuery().id("mp-c"), r).getItems().get(0).getDcTitle());
//    assertEquals("Find Aurel (title)", 1, e.find(systemQuery().title("aurel"), r).size());
//    assertEquals("Find Aurelien (title)", 1, e.find(systemQuery().title("aurelien"), r).size());
//    assertEquals("Find Aurel (text)", 1, e.find(systemQuery().text("aurel"), r).size());
//    assertEquals("Find Aurelien (text)", 1, e.find(systemQuery().text("aurelien"), r).size());
//    assertEquals("Find rock (text)", 1, e.find(systemQuery().text("rock"), r).size());
//    assertEquals("Find wor (text)", 1, e.find(systemQuery().text("wor"), r).size());
//    assertEquals("Find Greene (creator)", 1, e.find(systemQuery().creator("greene"), r).size());
//    assertEquals("Find Gree (creator)", 1, e.find(systemQuery().creator("gree"), r).size());
//    assertEquals("Find Gree (text)", 1, e.find(systemQuery().text("gree"), r).size());
//    assertEquals("Find Greene (text)", 1, e.find(systemQuery().text("greene"), r).size());
//    assertEquals("Find british (series title)", 1, e.find(systemQuery().seriesTitle("british"), r).size());
//    assertEquals("Find british (text)", 1, e.find(systemQuery().text("british"), r).size());
//    assertEquals("Find lite (text)", 3, e.find(systemQuery().text("lite"), r).size());
//    assertEquals("Find ratur (text)", 3, e.find(systemQuery().text("ratur"), r).size());
//    // space is or
//    assertEquals("Find british greene (text)", 1, e.find(systemQuery().text("british greene"), r).size());
//    assertEquals("Find american greene (text)", 2, e.find(systemQuery().text("american greene"), r).size());
//    //
//    // sorting
//    assertEquals("Sort by title ascending", "Aurelien",
//                 e.find(systemQuery().sort(EpisodeQuery.Sort.TITLE, true), r).getItems().get(0).getDcTitle());
//    assertEquals("Sort by title descending", "World of Tiers",
//                 e.find(systemQuery().sort(EpisodeQuery.Sort.TITLE, false), r).getItems().get(0).getDcTitle());
//    assertEquals("Sort by creator ascending", "Graham Greene",
//                 e.find(systemQuery().sort(EpisodeQuery.Sort.CREATOR, true), r).getItems().get(0).getDcCreator());
//    assertEquals("Sort by creator descending", "Philip Jose Farmer",
//                 e.find(systemQuery().sort(EpisodeQuery.Sort.CREATOR, false), r).getItems().get(0).getDcCreator());
//    assertEquals("Sort by series title ascending", "American literature",
//                 e.find(systemQuery().sort(EpisodeQuery.Sort.SERIES_TITLE, true), r).getItems().get(0).getDcSeriesTitle());
//    assertEquals("Sort by series title descending", "French literature",
//                 e.find(systemQuery().sort(EpisodeQuery.Sort.SERIES_TITLE, false), r).getItems().get(0).getDcSeriesTitle());
//  }
//
//  private static final Function<SearchResultItem, Boolean> hasSeriesTitle = new Function<SearchResultItem, Boolean>() {
//    @Override public Boolean apply(SearchResultItem item) {
//      return item.getDcSeriesTitle() != null;
//    }
//  };
}
