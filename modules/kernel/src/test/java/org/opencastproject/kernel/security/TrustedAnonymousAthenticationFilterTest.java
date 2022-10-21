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

package org.opencastproject.kernel.security;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

/**
 * Tests the {@link TrustedAnonymousAuthenticationFilter}
 */
public class TrustedAnonymousAthenticationFilterTest {

  private SecurityService securityService;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testTrusedAnonymousAuthenticationFilter() {
    HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
    EasyMock.expect(request.getHeader(DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER)).andReturn("true");
    EasyMock.expect(request.getHeader(DelegatingAuthenticationEntryPoint.REQUESTED_AUTH_HEADER)).andReturn(null);
    EasyMock.replay(request);

    TrustedAnonymousAuthenticationFilter filter = new TrustedAnonymousAuthenticationFilter();
    boolean isAnonymousRequest = filter.applyAnonymousForThisRequest(request);
    Assert.assertFalse(isAnonymousRequest);

    isAnonymousRequest = filter.applyAnonymousForThisRequest(request);
    Assert.assertTrue(isAnonymousRequest);
  }

}
