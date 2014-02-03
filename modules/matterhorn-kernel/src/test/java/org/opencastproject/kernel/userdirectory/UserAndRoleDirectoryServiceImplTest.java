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
package org.opencastproject.kernel.userdirectory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests the combined user and role directory service.
 */
public class UserAndRoleDirectoryServiceImplTest {

  /** The user and role directory */
  private UserAndRoleDirectoryServiceImpl directory = null;
  
  /** A username */
  private String userName = null;
  
  /** An organization */
  private Organization org = null;
  
  @Before
  public void setUp() throws Exception {
    org = new DefaultOrganization();
    userName = "sampleUser";
    
    User user1 = new User(userName, null, org.getId(), new String[] {"role1", "role2"});
    User user2 = new User(userName, "secret", org.getId(), new String[] {"role2", "role3"});
    
    UserProvider provider1 = EasyMock.createNiceMock(UserProvider.class);
    EasyMock.expect(provider1.getOrganization()).andReturn(org.getId()).anyTimes();
    EasyMock.expect(provider1.loadUser((String)EasyMock.anyObject())).andReturn(user1).anyTimes();
    
    UserProvider provider2 = EasyMock.createNiceMock(UserProvider.class);
    EasyMock.expect(provider2.getOrganization()).andReturn(org.getId()).anyTimes();
    EasyMock.expect(provider2.loadUser((String)EasyMock.anyObject())).andReturn(user2).anyTimes();

    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();

    EasyMock.replay(provider1, provider2, securityService);
    
    directory = new UserAndRoleDirectoryServiceImpl();
    directory.setSecurityService(securityService);
    directory.addUserProvider(provider1);
    directory.addUserProvider(provider2);
  }
  
  @Test
  public void testUserMerge() throws Exception {
    User mergedUser = directory.loadUser(userName);
    List<String> roles = Arrays.asList(mergedUser.getRoles());
    assertTrue(roles.contains("role1"));
    assertTrue(roles.contains("role2"));
    assertTrue(roles.contains("role3"));
    assertNotNull(mergedUser.getPassword());
    assertEquals(org.getId(), mergedUser.getOrganization());
    assertEquals(userName, mergedUser.getUserName());
  }
}
