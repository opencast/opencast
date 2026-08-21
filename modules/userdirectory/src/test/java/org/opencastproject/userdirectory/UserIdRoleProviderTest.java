/*
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

package org.opencastproject.userdirectory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tests {@link UserIdRoleProvider#findRoles(String, Role.Target, int, int)}, in particular its handling of
 * bounded, contains-style ("%text%") search queries -- the path exercised by the admin-ui's role picker once
 * it stopped fetching every role up front and started searching the backend directly.
 */
public class UserIdRoleProviderTest {

  private UserIdRoleProvider provider;
  private UserDirectoryService userDirectoryService;
  private JaxbOrganization org;

  @Before
  public void setUp() {
    org = new DefaultOrganization();

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.replay(securityService);

    userDirectoryService = EasyMock.createMock(UserDirectoryService.class);

    provider = new UserIdRoleProvider();
    provider.setSecurityService(securityService);
    provider.setUserDirectoryService(userDirectoryService);
  }

  private User user(String username) {
    return new JaxbUser(username, "opencast", org);
  }

  private Set<String> roleNames(Iterator<Role> roles) {
    List<Role> list = new ArrayList<>();
    roles.forEachRemaining(list::add);
    return list.stream().map(Role::getName).collect(Collectors.toSet());
  }

  @Test
  public void findRolesWithBoundedContainsQueryStillSearchesUsers() {
    // This is exactly the shape of query AclEndpoint now sends for a real, non-blank search
    // ("%" + query + "%"), and the frontend always sends a bounded limit.
    Capture<String> userQueryCapture = Capture.newInstance();
    EasyMock.expect(userDirectoryService.findUsers(EasyMock.capture(userQueryCapture), EasyMock.eq(0), EasyMock.eq(50)))
        .andReturn(List.of(user("admin")).iterator());
    EasyMock.replay(userDirectoryService);

    Iterator<Role> result = provider.findRoles("%adm%", Role.Target.ALL, 0, 50);

    assertEquals("%adm%", userQueryCapture.getValue());
    assertTrue(roleNames(result).contains("ROLE_USER_ADMIN"));
  }

  @Test
  public void findRolesWithUnboundedNonPrefixedQuerySkipsUserEnumeration() {
    // Legacy/unbounded callers (limit <= 0) with a general, non-wildcard, non-prefixed query should not trigger
    // a full user directory scan -- this preserves the original guard's intent.
    EasyMock.replay(userDirectoryService);

    Iterator<Role> result = provider.findRoles("%adm%", Role.Target.ALL, 0, 0);

    assertFalse(roleNames(result).contains("ROLE_USER_ADMIN"));
    EasyMock.verify(userDirectoryService);
  }

  @Test
  public void findRolesWithBlankQuerySearchesAllUsers() {
    Capture<String> userQueryCapture = Capture.newInstance();
    EasyMock.expect(userDirectoryService.findUsers(EasyMock.capture(userQueryCapture), EasyMock.eq(0), EasyMock.eq(50)))
        .andReturn(List.of(user("admin"), user("anonymous")).iterator());
    EasyMock.replay(userDirectoryService);

    Iterator<Role> result = provider.findRoles("%", Role.Target.ALL, 0, 50);

    assertEquals("%", userQueryCapture.getValue());
    Set<String> names = roleNames(result);
    assertTrue(names.contains("ROLE_USER_ADMIN"));
    assertTrue(names.contains("ROLE_USER_ANONYMOUS"));
  }

  @Test
  public void findRolesWithPrefixedQueryStripsPrefixRegardlessOfLimit() {
    // A caller using the legacy prefix-anchored convention should still work, and should still search users
    // even for an unbounded request, since the prefix itself is a strong enough signal.
    Capture<String> userQueryCapture = Capture.newInstance();
    EasyMock.expect(userDirectoryService.findUsers(EasyMock.capture(userQueryCapture), EasyMock.eq(0), EasyMock.eq(0)))
        .andReturn(List.of(user("admin")).iterator());
    EasyMock.replay(userDirectoryService);

    Iterator<Role> result = provider.findRoles("ROLE_USER_adm", Role.Target.ALL, 0, 0);

    assertEquals("adm", userQueryCapture.getValue());
    assertTrue(roleNames(result).contains("ROLE_USER_ADMIN"));
  }

}
