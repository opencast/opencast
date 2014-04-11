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
    User user1 = new JaxbUser("user1", org, new JaxbRole("role1", org), new JaxbRole("someRole", org));
    User user2 = new JaxbUser("user2", org, new JaxbRole("role2", org), new JaxbRole("someRole", org));
    User localAdmin = new JaxbUser("localAdmin", org, new JaxbRole(org.getAdminRole(), org), new JaxbRole("someRole",
            org));
    User globalAdmin = new JaxbUser("globalAdmin", org, new JaxbRole(GLOBAL_ADMIN_ROLE, org));

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
  }
}
