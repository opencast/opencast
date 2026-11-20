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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/** Tests the capture agent user configuration of {@link InMemoryUserAndRoleProvider} */
public class InMemoryUserAndRoleProviderTest {

  private InMemoryUserAndRoleProvider provider;

  @Before
  public void setUp() {
    final Organization org = new DefaultOrganization();
    final SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getOrganization()).andReturn(org).anyTimes();
    EasyMock.replay(securityService);

    provider = new InMemoryUserAndRoleProvider();
    provider.setSecurityService(securityService);
  }

  @Test
  public void testCaptureAgentUser() throws Exception {
    final Map<String, Object> config = new HashMap<>();
    config.put("capture_agent.user.mh_default_org.agent1", "secret");
    provider.updated(config);

    assertEquals("agent1", provider.loadUser("agent1").getUsername());
  }

  /** A malformed definition must be skipped and must not keep the valid ones from being loaded */
  @Test
  public void testMalformedCaptureAgentUserIsIgnored() throws Exception {
    final Map<String, Object> config = new HashMap<>();
    config.put("capture_agent.user.agentWithoutOrganization", "secret");
    config.put("capture_agent.user.mh_default_org.agent.with.dots", "secret");
    config.put("capture_agent.user.mh_default_org.agent1", "secret");
    provider.updated(config);

    assertNotNull(provider.loadUser("agent1"));
    assertNull(provider.loadUser("agentWithoutOrganization"));
    assertNull(provider.loadUser("agent"));
  }

}
