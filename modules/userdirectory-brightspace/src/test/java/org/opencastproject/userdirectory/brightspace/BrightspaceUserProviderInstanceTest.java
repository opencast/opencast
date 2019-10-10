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

package org.opencastproject.userdirectory.brightspace;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.User;
import org.opencastproject.userdirectory.brightspace.client.BrightspaceClientException;
import org.opencastproject.userdirectory.brightspace.client.BrightspaceClientImpl;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;

public class BrightspaceUserProviderInstanceTest {

  private static final String PID = "userProvider";
  private static final int CACHE_SIZE = 60;
  private static final int CACHE_EXPIRATION = 1000;

  private BrightspaceUserProviderInstance brightspaceUserProviderInstance;
  private BrightspaceClientImpl client;
  private DefaultOrganization organization;

  @Before
  public void setup() throws BrightspaceClientException {
    client = new BrightspaceClientImpl("http://brightspace/api", "myAppId", "myAppKey", "myUserId", "myUserKey");
    organization = new DefaultOrganization();

    brightspaceUserProviderInstance = new BrightspaceUserProviderInstance(PID, client, organization, CACHE_SIZE,
        CACHE_EXPIRATION, "admin");
  }

  @Test
  @Ignore
  public void testLoadUser() throws BrightspaceClientException {
    //When using a superadmin user in brightspace, only return a single admin role to prevert returning
    //all course-ids in a brightspace installation.
    User user = brightspaceUserProviderInstance.loadUser("admin");
    assertNotNull(user);

    assertTrue(hasRole(user.getRoles(), "ROLE_BRIGHTSPACE_ADMIN"));
  }

  private boolean hasRole(Set<Role> roles, String roleName) {
    for (Role role : roles)
      if (roleName.equals(role.getName()))
        return true;

    return false;
  }
}
