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
package org.opencastproject.index.service.util;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.impl.ManagedAclImpl;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import java.util.Date;

/**
 * Unit tests for {@link AccessInformationUtil}
 */
public class AccessInformationUtilTest {

  private static final long TRANSITION_ID = 12345L;
  private static final Date TRANSITION_APPLICATION_DATE = new Date(1400000000L); // Tue, 13 May 2014 16:53:20 GMT
  private static final Option<ConfiguredWorkflowRef> TRANSITION_WORKFLOW_ID = Option.some(ConfiguredWorkflowRef
          .workflow("custom_workflow"));
  private static final boolean TRANSITION_DONE = true;
  private static final long TRANSITION_ACL_ID = 7879L;
  private static final boolean TRANSITION_OVERRIDE_EPISODES = true;
  private static final boolean TRANSITION_IS_DELETED = true;

  private static final String ORGANISATION_1_ID = "org-1";
  private static final String MANAGED_ACL_1_NAME = "Managed ACL 1";

  private static final String ROLE_ADMIN = "ROLE_ADMIN";
  private static final String ROLE_STUDENT = "ROLE_STUDENT";

  private static final String ACTION_READ = "ACTION_READ";
  private static final String ACTION_WRITE = "ACTION_WRITE";

  private static final AccessControlEntry ACE_ROLE_ADMIN_ALLOW_ACTION_READ = new AccessControlEntry(ROLE_ADMIN,
          ACTION_READ, true);
  private static final AccessControlEntry ACE_ROLE_ADMIN_ALLOW_ACTION_WRITE = new AccessControlEntry(ROLE_ADMIN,
          ACTION_WRITE, true);
  private static final AccessControlEntry ACE_ROLE_STUDENT_ALLOW_ACTION_READ = new AccessControlEntry(ROLE_STUDENT,
          ACTION_READ, true);
  private static final AccessControlEntry ACE_ROLE_STUDENT_DISALLOW_ACTION_WRITE = new AccessControlEntry(ROLE_STUDENT,
          ACTION_WRITE, false);

  /**
   * Test method for {@link AccessInformationUtil#serializeManagedAcl(ManagedAcl)}
   */
  @Test
  public void testSerializeManagedAcl() throws Exception {
    AccessControlList acl = new AccessControlList();
    acl.getEntries().add(ACE_ROLE_ADMIN_ALLOW_ACTION_READ);

    ManagedAcl manAcl = new ManagedAclImpl(1L, MANAGED_ACL_1_NAME, ORGANISATION_1_ID, acl);

    JSONObject aclJson = AccessInformationUtil.serializeManagedAcl(manAcl);
    assertEquals(1L, aclJson.getLong("id"));
    assertEquals(MANAGED_ACL_1_NAME, aclJson.getString("name"));
  }

  /**
   * Test method for {@link AccessInformationUtil#serializeManagedAcl(ManagedAcl)}
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSerializeManagedAclWithNull() throws Exception {
    AccessInformationUtil.serializeManagedAcl(null);
  }

  /**
   * Test method for {@link AccessInformationUtil#serializeSeriesACLTransition(SeriesACLTransition)}
   */
  @Test
  public void testSerializeSeriesACLTransition() throws Exception {
    ManagedAcl acl = createNiceMock(ManagedAcl.class);
    expect(acl.getId()).andStubReturn(TRANSITION_ACL_ID);
    replay(acl);

    SeriesACLTransition trans = createNiceMock(SeriesACLTransition.class);
    expect(trans.getTransitionId()).andStubReturn(TRANSITION_ID);
    expect(trans.getApplicationDate()).andStubReturn(TRANSITION_APPLICATION_DATE);
    expect(trans.getWorkflow()).andStubReturn(TRANSITION_WORKFLOW_ID);
    expect(trans.isDone()).andStubReturn(TRANSITION_DONE);
    expect(trans.getAccessControlList()).andStubReturn(acl);
    expect(trans.isOverride()).andStubReturn(TRANSITION_OVERRIDE_EPISODES);
    replay(trans);

    JSONObject t = AccessInformationUtil.serializeSeriesACLTransition(trans);
    assertEquals(TRANSITION_ID, t.getLong("id"));
    assertEquals(TRANSITION_APPLICATION_DATE, new Date(DateTimeSupport.fromUTC(t.getString("application_date"))));
    assertEquals(TRANSITION_WORKFLOW_ID, Option.some(ConfiguredWorkflowRef.workflow(t.getString("workflow_id"))));
    assertEquals(TRANSITION_DONE, t.getBoolean("done"));
    assertEquals(TRANSITION_ACL_ID, t.getLong("acl_id"));
    assertEquals(TRANSITION_OVERRIDE_EPISODES, t.getBoolean("override_episodes"));
  }

  /**
   * Test method for {@link AccessInformationUtil#serializeSeriesACLTransition(SeriesACLTransition)}
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSerializeSeriesACLTransitionWithNull() throws Exception {
    AccessInformationUtil.serializeSeriesACLTransition(null);
  }

  /**
   * Test method for {@link AccessInformationUtil#serializeEpisodeACLTransition(EpisodeACLTransition)}
   */
  @Test
  public void testSerializeEpisodeACLTransition() throws Exception {
    ManagedAcl acl = createNiceMock(ManagedAcl.class);
    expect(acl.getId()).andStubReturn(TRANSITION_ACL_ID);
    replay(acl);

    EpisodeACLTransition trans = createNiceMock(EpisodeACLTransition.class);
    expect(trans.getTransitionId()).andStubReturn(TRANSITION_ID);
    expect(trans.getApplicationDate()).andStubReturn(TRANSITION_APPLICATION_DATE);
    expect(trans.getWorkflow()).andStubReturn(TRANSITION_WORKFLOW_ID);
    expect(trans.isDone()).andStubReturn(TRANSITION_DONE);
    expect(trans.getAccessControlList()).andStubReturn(Option.some(acl));
    expect(trans.isDelete()).andStubReturn(TRANSITION_IS_DELETED);
    replay(trans);

    JSONObject t = AccessInformationUtil.serializeEpisodeACLTransition(trans);
    assertEquals(TRANSITION_ID, t.getLong("id"));
    assertEquals(TRANSITION_APPLICATION_DATE, new Date(DateTimeSupport.fromUTC(t.getString("application_date"))));
    assertEquals(TRANSITION_WORKFLOW_ID, Option.some(ConfiguredWorkflowRef.workflow(t.getString("workflow_id"))));
    assertEquals(TRANSITION_IS_DELETED, t.getBoolean("is_deleted"));
    assertEquals(TRANSITION_ACL_ID, t.getLong("acl_id"));
  }

  /**
   * Test method for {@link AccessInformationUtil#serializeEpisodeACLTransition(EpisodeACLTransition)}
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSerializeEpisodeACLTransitionWithNull() throws Exception {
    AccessInformationUtil.serializeEpisodeACLTransition(null);
  }

  /**
   * Test method for {@link AccessInformationUtil#serializePrivilegesByRole(AccessControlList)}
   */
  @Test
  public void testSerializePrivilegesByRole() throws Exception {
    AccessControlList acl = new AccessControlList();
    acl.getEntries().add(ACE_ROLE_ADMIN_ALLOW_ACTION_READ);
    acl.getEntries().add(ACE_ROLE_ADMIN_ALLOW_ACTION_WRITE);
    acl.getEntries().add(ACE_ROLE_STUDENT_ALLOW_ACTION_READ);
    acl.getEntries().add(ACE_ROLE_STUDENT_DISALLOW_ACTION_WRITE);

    JSONObject privilegesByRole = AccessInformationUtil.serializePrivilegesByRole(acl);
    assertTrue(privilegesByRole.getJSONObject(ROLE_ADMIN).getBoolean(ACTION_READ));
    assertTrue(privilegesByRole.getJSONObject(ROLE_ADMIN).getBoolean(ACTION_WRITE));
    assertTrue(privilegesByRole.getJSONObject(ROLE_STUDENT).getBoolean(ACTION_READ));
    assertFalse(privilegesByRole.getJSONObject(ROLE_STUDENT).getBoolean(ACTION_WRITE));
  }

  /**
   * Test method for {@link AccessInformationUtil#serializePrivilegesByRole(AccessControlList)}
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSerializePrivilegesByRoleWithNull() throws Exception {
    AccessInformationUtil.serializePrivilegesByRole(null);
  }

}
