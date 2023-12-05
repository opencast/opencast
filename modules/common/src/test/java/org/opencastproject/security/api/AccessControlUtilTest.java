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

package org.opencastproject.security.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;

import org.junit.Test;

public class AccessControlUtilTest {
  @Test
  public void testIsAuthorized() throws Exception {
    AccessControlList acl = new AccessControlList();
    acl.getEntries().add(new AccessControlEntry("role1", "action1", true));
    acl.getEntries().add(new AccessControlEntry("role1", "action2", true));
    acl.getEntries().add(new AccessControlEntry("role1", "action3", false));

    acl.getEntries().add(new AccessControlEntry("role2", "action1", false));
    acl.getEntries().add(new AccessControlEntry("role2", "action2", false));
    acl.getEntries().add(new AccessControlEntry("role2", "action3", true));

    JaxbOrganization org = new DefaultOrganization();
    User user1 = new JaxbUser("user1", "test", org, new JaxbRole("role1", org), new JaxbRole("someRole", org));
    User user2 = new JaxbUser("user2", "test", org, new JaxbRole("role2", org), new JaxbRole("someRole", org));
    User localAdmin = new JaxbUser("localAdmin", "test", org, new JaxbRole(org.getAdminRole(), org), new JaxbRole(
            "someRole", org));
    User globalAdmin = new JaxbUser("globalAdmin", "test", org, new JaxbRole(GLOBAL_ADMIN_ROLE, org));

    assertTrue(AccessControlUtil.isAuthorized(acl, user1, org, "action1"));
    assertTrue(AccessControlUtil.isAuthorized(acl, user1, org, "action2"));
    assertFalse(AccessControlUtil.isAuthorized(acl, user1, org, "action3"));

    assertFalse(AccessControlUtil.isAuthorized(acl, user2, org, "action1"));
    assertFalse(AccessControlUtil.isAuthorized(acl, user2, org, "action2"));
    assertTrue(AccessControlUtil.isAuthorized(acl, user2, org, "action3"));

    assertTrue(AccessControlUtil.isAuthorized(acl, localAdmin, org, "action1"));
    assertTrue(AccessControlUtil.isAuthorized(acl, localAdmin, org, "action2"));
    assertTrue(AccessControlUtil.isAuthorized(acl, localAdmin, org, "action3"));

    assertTrue(AccessControlUtil.isAuthorized(acl, globalAdmin, org, "action1"));
    assertTrue(AccessControlUtil.isAuthorized(acl, globalAdmin, org, "action2"));
    assertTrue(AccessControlUtil.isAuthorized(acl, globalAdmin, org, "action3"));

    assertTrue(AccessControlUtil.isAuthorizedAll(acl, globalAdmin, org, "action1", "action2", "action3"));
    assertFalse(AccessControlUtil.isAuthorizedAll(acl, user1, org, "action1", "action2", "action3"));
    assertTrue(AccessControlUtil.isAuthorizedAll(acl, user1, org, "action1", "action2"));

    assertTrue(AccessControlUtil.isAuthorizedOne(acl, globalAdmin, org, "action1", "action2", "action3"));
    assertFalse(AccessControlUtil.isAuthorizedOne(acl, user1, org, "action3", "action4", "action5"));
    assertTrue(AccessControlUtil.isAuthorizedOne(acl, user1, org, "action1", "action3"));

    assertFalse(AccessControlUtil.isProhibitedAll(acl, globalAdmin, org, "action1", "action2", "action3"));
    assertFalse(AccessControlUtil.isProhibitedAll(acl, user1, org, "action1", "action2", "action3"));
    assertTrue(AccessControlUtil.isProhibitedAll(acl, user1, org, "action3", "action4"));

    assertFalse(AccessControlUtil.isProhibitedOne(acl, globalAdmin, org, "action1", "action2", "action3"));
    assertFalse(AccessControlUtil.isProhibitedOne(acl, user1, org, "action1", "action2"));
    assertTrue(AccessControlUtil.isProhibitedOne(acl, user1, org, "action1", "action2", "action3"));
  }
}
