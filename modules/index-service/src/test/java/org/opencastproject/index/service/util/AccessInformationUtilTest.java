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

package org.opencastproject.index.service.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.impl.ManagedAclImpl;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Unit tests for {@link AccessInformationUtil}
 */
public class AccessInformationUtilTest {

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
