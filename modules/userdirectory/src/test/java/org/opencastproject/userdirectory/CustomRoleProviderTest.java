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

package org.opencastproject.userdirectory;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;

import org.apache.commons.collections.IteratorUtils;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public class CustomRoleProviderTest {

  private CustomRoleProvider customRoleProvider;
  private ComponentContext componentContextMock;
  private BundleContext bundleContextMock;
  private SecurityService securityService;

  @Before
  public void setUp() throws Exception {

    // Set up bundle context with role list and pattern
    bundleContextMock = createNiceMock(BundleContext.class);
    expect(bundleContextMock.getProperty(CustomRoleProvider.CUSTOM_ROLES_KEY)).andReturn(
         "ROLE_ONE, ROLE_TWO, ROLE_THREE");
    expect(bundleContextMock.getProperty(CustomRoleProvider.CUSTOM_ROLES_PATTERN_KEY)).andReturn(
         "^[0-9a-f-]+_(Learner|Instructor)$");
    replay(bundleContextMock);
    componentContextMock = createNiceMock(ComponentContext.class);
    expect(componentContextMock.getBundleContext()).andReturn(bundleContextMock).anyTimes();
    replay(componentContextMock);

    // Set up security service
    securityService = createNiceMock(SecurityService.class);
    expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    replay(securityService);

    // Create provider
    customRoleProvider = new CustomRoleProvider();
    customRoleProvider.setSecurityService(securityService);
    customRoleProvider.activate(componentContextMock);

  }

  @Test
  public void testFindRoles() throws Exception {

    // Static role from a list
    assertEquals(1, IteratorUtils.toList(customRoleProvider.findRoles("ROLE_TWO%", Role.Target.ACL, 0, 2)).size());
    assertEquals(1, IteratorUtils.toList(customRoleProvider.findRoles("ROLE_TWO", Role.Target.ACL, 0, 2)).size());
    assertEquals(1, IteratorUtils.toList(customRoleProvider.findRoles("ROLE_ONE", Role.Target.ACL, 0, 2)).size());
    assertEquals(1, IteratorUtils.toList(customRoleProvider.findRoles("ROLE_THREE", Role.Target.ACL, 0, 2)).size());

    // Role matching a pattern
    assertEquals(1, IteratorUtils.toList(customRoleProvider.findRoles("123_Learner", Role.Target.ACL, 0, 2)).size());
    assertEquals(1, IteratorUtils.toList(customRoleProvider.findRoles("123_Instructor", Role.Target.ACL, 0, 2)).size());
    assertEquals(1, IteratorUtils.toList(customRoleProvider.findRoles("456_Learner%", Role.Target.ACL, 0, 2)).size());
    assertEquals(1, IteratorUtils.toList(customRoleProvider.findRoles("1b1b6e5d-34ac-4370-9b20-8d649df9878c_Instructor", Role.Target.ACL, 0, 2)).size());

    // Role does not exist
    assertEquals(0, IteratorUtils.toList(customRoleProvider.findRoles("unknown%", Role.Target.ACL, 0, 1)).size());
    assertEquals(0, IteratorUtils.toList(customRoleProvider.findRoles("987_Admin", Role.Target.ACL, 0, 1)).size());
    assertEquals(0, IteratorUtils.toList(customRoleProvider.findRoles("ROLE_ONE_", Role.Target.ACL, 0, 2)).size());

  }
}
