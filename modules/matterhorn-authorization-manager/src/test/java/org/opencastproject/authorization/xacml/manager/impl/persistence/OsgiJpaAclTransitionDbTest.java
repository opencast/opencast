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

package org.opencastproject.authorization.xacml.manager.impl.persistence;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.api.TransitionQuery;
import org.opencastproject.authorization.xacml.manager.api.TransitionResult;
import org.opencastproject.authorization.xacml.manager.impl.AclTransitionDbDuplicatedException;
import org.opencastproject.authorization.xacml.manager.impl.AclTransitionDbException;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.workflow.api.ConfiguredWorkflowRef.workflow;

/** Tests persistence: storing, merging, retrieving and removing. */
public class OsgiJpaAclTransitionDbTest {

  private static final String ACL_FILE = "/acl.xml";

  private static final Organization ORG = new DefaultOrganization();
  private static final Organization ORG2 = new JaxbOrganization("another-org");

  private ComboPooledDataSource pooledDataSource;
  private OsgiJpaAclTransitionDb db;
  private JpaAclDb aclDb;
  private String storage;
  private AccessControlList acl;
  private SecurityService securityService;

  /** @throws java.lang.Exception */
  @Before
  public void setUp() throws Exception {
    long currentTime = System.currentTimeMillis();
    storage = PathSupport.concat("target", "db" + currentTime + ".h2.db");

    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + currentTime);
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    final Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    final PersistenceProvider persitenceProvider = new PersistenceProvider();
    db = new OsgiJpaAclTransitionDb();
    db.setPersistenceProvider(persitenceProvider);
    db.setPersistenceProperties(props);
    db.activate(null);

    aclDb = new JpaAclDb(PersistenceUtil.newPersistenceEnvironment(persitenceProvider, "org.opencastproject.authorization.xacml.manager", props));

    InputStream in = null;
    try {
      in = getClass().getResourceAsStream(ACL_FILE);
      acl = AccessControlParser.parseAcl(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  /** @throws java.lang.Exception */
  @After
  public void tearDown() throws Exception {
    db.deactivate(null);
    DataSources.destroy(pooledDataSource);
    FileUtils.deleteQuietly(new File(storage));
  }

  @Test
  public void createEpisodeAclTransitionEntity() {
    final Date now = new Date();
    EpisodeAclTransitionEntity e = new EpisodeAclTransitionEntity().update("uuid", ORG.getId(), now, Option.<ManagedAclEntity>none(), Option.<ConfiguredWorkflowRef>none());
    assertEquals("uuid", e.getEpisodeId());
    assertEquals(now, e.getApplicationDate());
    assertEquals(ORG.getId(), e.getOrganizationId());
  }

  @Test
  public void testStoreAndGetEpisodeACL() throws Exception {
    final Date now = new Date();

    // a fallback to series transition should be saveable
    db.storeEpisodeAclTransition(ORG, "uuid", now, none(0L), Option.<ConfiguredWorkflowRef>none());

    // a transition referencing a non existing ACL should not be saveable
    try {
      db.storeEpisodeAclTransition(ORG, "uuid", new Date(), some(1L), Option.<ConfiguredWorkflowRef>none());
      fail("No ACL with ID 1");
    } catch (AclTransitionDbException ignore) {
    }

    // a transition referencing an existing ACL should be saveable
    final ManagedAcl macl = createAcl();
    final EpisodeACLTransition t3 = db.storeEpisodeAclTransition(ORG, "uuid-2", now, some(macl.getId()), Option.<ConfiguredWorkflowRef>none());
    assertEquals("uuid-2", t3.getEpisodeId());
    assertEquals(now, t3.getApplicationDate());
    assertTrue(t3.getAccessControlList().isSome());
    assertEquals(macl.getName(), t3.getAccessControlList().get().getName());

    // a transition with the same properties should not be saveable
    try {
      db.storeEpisodeAclTransition(ORG, "uuid", now, some(macl.getId()), Option.<ConfiguredWorkflowRef>none());
      fail("Duplicated episode ACL must not be stored");
    } catch (AclTransitionDbDuplicatedException ignore) {
    }

    List<EpisodeACLTransition> ts = db.getEpisodeAclTransitions(ORG, "uuid");
    assertEquals(1, ts.size());
    assertEquals("uuid", ts.get(0).getEpisodeId());
    assertTrue(ts.get(0).isDelete());
    assertTrue(!ts.get(0).isDone());
  }

  @Test
  public void testStoreAndGetSeriesACL() throws Exception {
    final Date now = new Date();

    // a transition referencing a non existing ACL should not be saveable
    try {
      db.storeSeriesAclTransition(ORG, "uuid", new Date(), 1L, true, Option.<ConfiguredWorkflowRef>none());
      fail("No ACL with ID 1");
    } catch (AclTransitionDbException ignore) {
    }

    // a transition referencing an existing ACL should be saveable
    final ManagedAcl macl = createAcl();
    final SeriesACLTransition t3 = db.storeSeriesAclTransition(ORG, "uuid", now, macl.getId(), false, Option.<ConfiguredWorkflowRef>none());
    assertEquals("uuid", t3.getSeriesId());
    assertEquals(now, t3.getApplicationDate());
    assertEquals(macl.getName(), t3.getAccessControlList().getName());

    // a transition with the same properties should not be saveable
    try {
      db.storeSeriesAclTransition(ORG, "uuid", now, macl.getId(), true, Option.<ConfiguredWorkflowRef>none());
      fail("Duplicated episode ACL must not be stored");
    } catch (AclTransitionDbDuplicatedException ignore) {
    }

    List<SeriesACLTransition> ts = db.getSeriesAclTransitions(ORG, "uuid");
    assertEquals(1, ts.size());
    assertEquals("uuid", ts.get(0).getSeriesId());
    assertTrue(!ts.get(0).isOverride());
    assertTrue(!ts.get(0).isDone());
  }

  @Test
  public void testGetEpisodeTransitions() throws Exception {
    final ManagedAcl macl = createAcl();
    EpisodeACLTransition t1 = db.storeEpisodeAclTransition(ORG, "uuid", new Date(), some(macl.getId()), Option.<ConfiguredWorkflowRef>none());
    EpisodeACLTransition t2 = db.storeEpisodeAclTransition(ORG, "uuid", new Date(), some(macl.getId()), Option.<ConfiguredWorkflowRef>none());

    // there should now be two transitions for episode "uuid"
    List<EpisodeACLTransition> episodes = db.getEpisodeAclTransitions(ORG, "uuid");
    assertEquals(2, episodes.size());

    // transitions shouldn't be accessible from another organization
    assertEquals(0, db.getEpisodeAclTransitions(ORG2, "uuid").size());
  }

  @Test
  public void testGetSeriesTransitions() throws Exception {
    final ManagedAcl macl = createAcl();
    SeriesACLTransition t1 = db.storeSeriesAclTransition(ORG, "uuid", new Date(), macl.getId(), true, Option.<ConfiguredWorkflowRef>none());
    SeriesACLTransition t2 = db.storeSeriesAclTransition(ORG, "uuid", new Date(), macl.getId(), false, Option.<ConfiguredWorkflowRef>none());

    // there should now be two transitions for series "uuid"
    List<SeriesACLTransition> series = db.getSeriesAclTransitions(ORG, "uuid");
    assertEquals(2, series.size());

    // transitions shouldn't be accessible from another organization
    assertEquals(0, db.getSeriesAclTransitions(ORG2, "uuid").size());
  }

  @Test
  public void testUpdateEpisode() throws Exception {
    final ManagedAcl macl = createAcl();
    EpisodeACLTransition t1 = db.storeEpisodeAclTransition(ORG, "uuid", new Date(), some(macl.getId()), Option.<ConfiguredWorkflowRef>none());

    EpisodeACLTransition u1 = db.updateEpisodeAclTransition(ORG, t1.getTransitionId(), t1.getApplicationDate(), none(0L), Option.some(workflow("full")));
    assertEquals(t1.getTransitionId(), u1.getTransitionId());
    assertEquals(t1.getEpisodeId(), u1.getEpisodeId());
    assertEquals(t1.getOrganizationId(), u1.getOrganizationId());
    assertTrue(u1.getAccessControlList().isNone());
    assertNotSame(t1.isDelete(), u1.isDelete());
    assertNotSame(t1.getWorkflow(), u1.getWorkflow());

    try {
      db.updateEpisodeAclTransition(ORG2, t1.getTransitionId(), t1.getApplicationDate(), some(macl.getId()), Option.some(workflow("full")));
      fail("Updating from non-owner org should not be possible");
    } catch (AclTransitionDbException ignore1) {
    } catch (NotFoundException ignore2) {
    }
  }

  @Test
  public void testUpdateSeries() throws Exception {
    final ManagedAcl macl = createAcl();
    SeriesACLTransition t1 = db.storeSeriesAclTransition(ORG, "uuid", new Date(), macl.getId(), true, Option.<ConfiguredWorkflowRef>none());

    SeriesACLTransition u1 = db.updateSeriesAclTransition(ORG, t1.getTransitionId(), t1.getApplicationDate(), macl.getId(), false, Option.some(workflow("full")));
    assertEquals(t1.getTransitionId(), u1.getTransitionId());
    assertEquals(t1.getSeriesId(), u1.getSeriesId());
    assertEquals(t1.getOrganizationId(), u1.getOrganizationId());
    assertEquals(t1.getAccessControlList().getId(), u1.getAccessControlList().getId());
    assertNotSame(t1.getWorkflow(), u1.getWorkflow());
    assertNotSame(t1.isOverride(), u1.isOverride());

    try {
      db.updateSeriesAclTransition(ORG2, t1.getTransitionId(), t1.getApplicationDate(), macl.getId(), false, Option.some(workflow("full")));
      fail("Updating from non-owner org should not be possible");
    } catch (AclTransitionDbException ignore1) {
    } catch (NotFoundException ignore2) {
    }
  }

  @Test
  public void testDeleteEpisode() throws Exception {
    final ManagedAcl macl = createAcl();
    EpisodeACLTransition t1 = db.storeEpisodeAclTransition(ORG, "uuid", new Date(), some(macl.getId()), Option.<ConfiguredWorkflowRef>none());
    // try deletion from different org
    try {
      db.deleteEpisodeAclTransition(ORG2, t1.getTransitionId());
      fail("Deleting from non-owner org should not be possible");
    } catch (NotFoundException ignore) {
    }
    db.deleteEpisodeAclTransition(ORG, t1.getTransitionId());
    try {
      db.deleteEpisodeAclTransition(ORG, t1.getTransitionId());
      fail("Deleting a non existing transition should throw an exception");
    } catch (NotFoundException ignore) {
    }
  }

  @Test
  public void testDeleteSeries() throws Exception {
    final ManagedAcl macl = createAcl();
    SeriesACLTransition t1 = db.storeSeriesAclTransition(ORG, "uuid", new Date(), macl.getId(), true, Option.<ConfiguredWorkflowRef>none());
    // try deletion from different org
    try {
      db.deleteSeriesAclTransition(ORG2, t1.getTransitionId());
      fail("Deleting from non-owner org should not be possible");
    } catch (NotFoundException ignore) {
    }
    db.deleteSeriesAclTransition(ORG, t1.getTransitionId());
    try {
      db.deleteSeriesAclTransition(ORG, t1.getTransitionId());
      fail("Deleting a non existing transition should throw an exception");
    } catch (NotFoundException ignore) {
    }
  }

  @Test
  public void testGetByQuery() throws Exception {
    final ManagedAcl macl = createAcl();
    SeriesACLTransition st1 = db.storeSeriesAclTransition(ORG, "uuid-series", new Date(1347000000000L), macl.getId(), true, Option.<ConfiguredWorkflowRef>none());
    SeriesACLTransition st2 = db.storeSeriesAclTransition(ORG, "uuid-series", new Date(1347000900000L), macl.getId(), false, Option.<ConfiguredWorkflowRef>none());
    SeriesACLTransition st3 = db.storeSeriesAclTransition(ORG, "uuid-series2", new Date(1347000030000L), macl.getId(), false, option(workflow("full")));
    SeriesACLTransition st4 = db.markSeriesTransitionAsCompleted(ORG, st3.getTransitionId());

    EpisodeACLTransition et1 = db.storeEpisodeAclTransition(ORG, "uuid-episode", new Date(1347005303736L), Option.<Long>none(), Option.<ConfiguredWorkflowRef>none());
    EpisodeACLTransition et2 = db.storeEpisodeAclTransition(ORG, "uuid-episode", new Date(1347005343736L), some(macl.getId()), Option.<ConfiguredWorkflowRef>none());
    EpisodeACLTransition et3 = db.storeEpisodeAclTransition(ORG, "uuid-episode2", new Date(1347005343736L), some(macl.getId()), option(workflow("full")));
    EpisodeACLTransition et4 = db.markEpisodeTransitionAsCompleted(ORG, et3.getTransitionId());

    // Test All
    TransitionQuery query = TransitionQuery.query();
    TransitionResult result = db.getByQuery(ORG, query);
    assertEquals(3, result.getEpisodeTransistions().size());
    assertEquals(3, result.getSeriesTransistions().size());
    assertEquals(et1.getTransitionId(), result.getEpisodeTransistions().get(0).getTransitionId());
    assertEquals(et2.getTransitionId(), result.getEpisodeTransistions().get(1).getTransitionId());
    assertEquals(et4.getTransitionId(), result.getEpisodeTransistions().get(2).getTransitionId());
    assertEquals(st1.getTransitionId(), result.getSeriesTransistions().get(0).getTransitionId());
    assertEquals(st4.getTransitionId(), result.getSeriesTransistions().get(1).getTransitionId());
    assertEquals(st2.getTransitionId(), result.getSeriesTransistions().get(2).getTransitionId());

    // Test Episode
    query.withScope(AclScope.Episode);
    result = db.getByQuery(ORG, query);
    assertEquals(3, result.getEpisodeTransistions().size());
    assertEquals(0, result.getSeriesTransistions().size());
    assertEquals(et1.getTransitionId(), result.getEpisodeTransistions().get(0).getTransitionId());
    assertEquals(et2.getTransitionId(), result.getEpisodeTransistions().get(1).getTransitionId());
    assertEquals(et3.getTransitionId(), result.getEpisodeTransistions().get(2).getTransitionId());

    query.withScope(AclScope.Episode);
    result = db.getByQuery(ORG2, query);
    assertEquals(0, result.getEpisodeTransistions().size());
    assertEquals(0, result.getSeriesTransistions().size());

    // Test Series
    query.withScope(AclScope.Series);
    result = db.getByQuery(ORG, query);
    assertEquals(0, result.getEpisodeTransistions().size());
    assertEquals(3, result.getSeriesTransistions().size());
    assertEquals(st1.getTransitionId(), result.getSeriesTransistions().get(0).getTransitionId());
    assertEquals(st3.getTransitionId(), result.getSeriesTransistions().get(1).getTransitionId());
    assertEquals(st2.getTransitionId(), result.getSeriesTransistions().get(2).getTransitionId());

    // Test Date from
    query = TransitionQuery.query().after(new Date(1347000040000L));
    result = db.getByQuery(ORG, query);
    assertEquals(3, result.getEpisodeTransistions().size());
    assertEquals(1, result.getSeriesTransistions().size());
    assertEquals(st2.getTransitionId(), result.getSeriesTransistions().get(0).getTransitionId());

    // Test Date from, to
    query.before(new Date(1347005313736L)).after(new Date(1347000040000L));
    result = db.getByQuery(ORG, query);
    assertEquals(1, result.getEpisodeTransistions().size());
    assertEquals(1, result.getSeriesTransistions().size());
    assertEquals(st2.getTransitionId(), result.getSeriesTransistions().get(0).getTransitionId());
    assertEquals(et1.getTransitionId(), result.getEpisodeTransistions().get(0).getTransitionId());

    // Test id
    query = TransitionQuery.query().withId("uuid-series");
    result = db.getByQuery(ORG, query);
    assertEquals(0, result.getEpisodeTransistions().size());
    assertEquals(2, result.getSeriesTransistions().size());
    assertEquals(st1.getTransitionId(), result.getSeriesTransistions().get(0).getTransitionId());
    assertEquals(st2.getTransitionId(), result.getSeriesTransistions().get(1).getTransitionId());

    query = TransitionQuery.query().withId("uuid-series");
    result = db.getByQuery(ORG2, query);
    assertEquals(0, result.getEpisodeTransistions().size());
    assertEquals(0, result.getSeriesTransistions().size());

    // Test transitionId
    result = db.getByQuery(ORG, TransitionQuery.query().withTransitionId(et2.getTransitionId()));
    assertEquals(1, result.getEpisodeTransistions().size());
    assertEquals(0, result.getSeriesTransistions().size());
    assertEquals(et2.getTransitionId(), result.getEpisodeTransistions().get(0).getTransitionId());

    // Test is done
    query = TransitionQuery.query().withDone(false);
    result = db.getByQuery(ORG, query);
    assertEquals(2, result.getEpisodeTransistions().size());
    assertEquals(2, result.getSeriesTransistions().size());
    assertEquals(et1.getTransitionId(), result.getEpisodeTransistions().get(0).getTransitionId());
    assertEquals(et2.getTransitionId(), result.getEpisodeTransistions().get(1).getTransitionId());
    assertEquals(st1.getTransitionId(), result.getSeriesTransistions().get(0).getTransitionId());
    assertEquals(st2.getTransitionId(), result.getSeriesTransistions().get(1).getTransitionId());

    query.withDone(true);
    result = db.getByQuery(ORG, query);
    assertEquals(1, result.getEpisodeTransistions().size());
    assertEquals(1, result.getSeriesTransistions().size());
    assertEquals(et3.getTransitionId(), result.getEpisodeTransistions().get(0).getTransitionId());
    assertEquals(st3.getTransitionId(), result.getSeriesTransistions().get(0).getTransitionId());
  }

  @Test
  public void testDone() throws Exception {
    EpisodeACLTransition t = db.storeEpisodeAclTransition(ORG, "episode-id", new Date(1347005303736L), none(0L), Option.<ConfiguredWorkflowRef>none());
    assertEquals(1, db.getByQuery(ORG, TransitionQuery.query().withDone(false)).getEpisodeTransistions().size());
    // ensure predicates are joined by "and"
    assertEquals(0, db.getByQuery(ORG, TransitionQuery.query().after(new Date(1347005303736L)).withDone(true)).getEpisodeTransistions().size());
    db.markEpisodeTransitionAsCompleted(ORG, t.getTransitionId());
    assertEquals(0, db.getByQuery(ORG, TransitionQuery.query().withDone(false)).getEpisodeTransistions().size());
    assertEquals(1, db.getByQuery(ORG, TransitionQuery.query().withDone(true)).getEpisodeTransistions().size());
  }

  private ManagedAcl createAcl() {
    final Option<ManagedAcl> macl = aclDb.createAcl(ORG, acl, "acl");
    assertTrue(macl.isSome());
    return macl.get();
  }
}
