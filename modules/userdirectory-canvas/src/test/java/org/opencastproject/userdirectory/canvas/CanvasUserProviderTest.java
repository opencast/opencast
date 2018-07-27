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

package org.opencastproject.userdirectory.canvas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class CanvasUserProviderTest {

  protected CanvasUserProviderInstance canvasProvider = null;

  @Before
  public void setUp() throws Exception {

    Set<String> instructorRoles = new HashSet<String>();
    instructorRoles.add("TeacherEnrollment");

    canvasProvider = new CanvasUserProviderInstance("sample_pid", new DefaultOrganization(), "https://example.com",
      "id", "12345", "^[a-zA-Z0-9-]+$", "^[0-9a-zA-Z]{6,}$", "short_name", instructorRoles, 100, 10);
  }

  private boolean hasRole(Set<Role> roles, String roleName) {
    for (Role role : roles) {
      if (roleName.equals(role.getName()))
        return true;
    }

    return false;
  }

  @Test
  @Ignore
  public void testLoadUser() throws Exception {
    User user = canvasProvider.loadUser("datest");
    assertNotNull(user);

    // Generic group role added for all Canvas users
    assertTrue(hasRole(user.getRoles(), "ROLE_GROUP_CANVAS"));

    // Test role specific to user datest on test Canvas instances
    assertTrue(hasRole(user.getRoles(), "DAC-EDUCATION-DEPT1-SUBJ3-426_Instructor"));
  }

  @Test
  @Ignore
  public void testFindUser() throws Exception {

    // User exists
    assertEquals(1, IteratorUtils.toList(canvasProvider.findUsers("datest", 0, 1)).size());

    // User exists but fails regexp pattern (minimum 6 characters)
    assertEquals(0, IteratorUtils.toList(canvasProvider.findUsers("admin", 0, 1)).size());

    // User doesn't exist
    assertEquals(0, IteratorUtils.toList(canvasProvider.findUsers("nobody", 0, 1)).size());
  }

  @Test
  @Ignore
  public void testFindRoles() throws Exception {

    // Site exists
    assertEquals(2, IteratorUtils.toList(canvasProvider.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426%", Role.Target.ACL, 0, 2)).size());
    assertEquals(1, IteratorUtils.toList(canvasProvider.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426_Learner", Role.Target.ACL, 0, 1)).size());
    assertEquals(1, IteratorUtils.toList(canvasProvider.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426_Instructor", Role.Target.ACL, 0, 1)).size());
    assertEquals(1, IteratorUtils.toList(canvasProvider.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426_Instructor%", Role.Target.ACL, 0, 1)).size());

    // Site fails pattern
    assertEquals(0, IteratorUtils.toList(canvasProvider.findRoles("!gateway%", Role.Target.ACL, 0, 2)).size());

    // Site or role does not exist
    assertEquals(0, IteratorUtils.toList(canvasProvider.findRoles("unknown%", Role.Target.ACL, 0, 1)).size());
    assertEquals(0, IteratorUtils.toList(canvasProvider.findRoles("unknown", Role.Target.ACL, 0, 1)).size());
    assertEquals(0, IteratorUtils.toList(canvasProvider.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426__Learner", Role.Target.ACL, 0, 1)).size());
    assertEquals(0, IteratorUtils.toList(canvasProvider.findRoles("DAC-EDUCATION-DEPT1-SUBJ3-426_", Role.Target.ACL, 0, 1)).size());

  }
}
