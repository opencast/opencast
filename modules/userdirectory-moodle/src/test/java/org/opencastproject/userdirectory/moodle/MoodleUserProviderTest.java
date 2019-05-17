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

package org.opencastproject.userdirectory.moodle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.util.Set;

public class MoodleUserProviderTest {
  private MoodleUserProviderInstance moodleProvider;

  @Before
  public void setUp() throws Exception {
    moodleProvider = new MoodleUserProviderInstance("sample_pid",
            new MoodleWebServiceImpl(new URI("http://moodle/webservice/rest/server.php"), "myToken"),
            new DefaultOrganization(), "^[0-9]+$", "^[0-9a-zA-Z_]+$", "^[0-9]+$", true, 100, 10, "admin");
  }

  @Test
  @Ignore
  public void testLoadUser() throws Exception {
    User user = moodleProvider.loadUser("testdozent22");
    assertNotNull(user);

    // Generic group role added for all Moodle users
    assertTrue(hasRole(user.getRoles(), "ROLE_GROUP_MOODLE"));

    // Test role specific to user datest on test Moodle instances
    assertTrue(hasRole(user.getRoles(), "6928_Learner"));
    assertTrue(hasRole(user.getRoles(), "10765_Instructor"));

    user = moodleProvider.loadUser("nobody");
    assertNull(user);
  }

  @Test
  @Ignore
  public void testFindUser() throws Exception {
    // User exists
    assertEquals(1, IteratorUtils.toList(moodleProvider.findUsers("testdozent22", 0, 1)).size());

    // User exists but fails regexp pattern (minimum 6 characters)
    assertEquals(0, IteratorUtils.toList(moodleProvider.findUsers("admin", 0, 1)).size());

    // User doesn't exist
    assertEquals(0, IteratorUtils.toList(moodleProvider.findUsers("nobody", 0, 1)).size());
  }

  @Test
  @Ignore
  public void testFindRoles() throws Exception {
    // Site exists
    assertEquals(2, IteratorUtils.toList(moodleProvider.findRoles("10765%", Role.Target.ACL, 0, 2)).size());
    assertEquals(1, IteratorUtils.toList(moodleProvider.findRoles("6928_Learner", Role.Target.ACL, 0, 1)).size());
    assertEquals(1, IteratorUtils.toList(moodleProvider.findRoles("6928_Learner%", Role.Target.ACL, 0, 1)).size());
    assertEquals(1, IteratorUtils.toList(moodleProvider.findRoles("10765_Instructor", Role.Target.ACL, 0, 1)).size());
    assertEquals(1, IteratorUtils.toList(moodleProvider.findRoles("10765_Instructor%", Role.Target.ACL, 0, 1)).size());

    // Site fails pattern
    assertEquals(0, IteratorUtils.toList(moodleProvider.findRoles("!gateway%", Role.Target.ACL, 0, 2)).size());

    // Site or role does not exist
    assertEquals(0, IteratorUtils.toList(moodleProvider.findRoles("6928__Learner", Role.Target.ACL, 0, 1)).size());
    assertEquals(0, IteratorUtils.toList(moodleProvider.findRoles("10765__Instructor", Role.Target.ACL, 0, 1)).size());
    assertEquals(0, IteratorUtils.toList(moodleProvider.findRoles("10765_", Role.Target.ACL, 0, 1)).size());
  }

  private boolean hasRole(Set<Role> roles, String roleName) {
    for (Role role : roles)
      if (roleName.equals(role.getName()))
        return true;

    return false;
  }
}
