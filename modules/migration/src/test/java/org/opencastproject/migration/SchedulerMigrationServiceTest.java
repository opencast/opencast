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
package org.opencastproject.migration;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;


import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

public class SchedulerMigrationServiceTest {

  /**
   * Test class for the scheduler migration service
   */
  private SchedulerMigrationService schedulerMigrationService = new SchedulerMigrationService();

  @Before
  public void setUp() throws Exception {
    OrganizationDirectoryService orgDirService = createNiceMock(OrganizationDirectoryService.class);
    expect(orgDirService.getOrganization(anyString())).andReturn(new DefaultOrganization()).anyTimes();
    replay(orgDirService);

    SecurityService securityService = createNiceMock(SecurityService.class);
    expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    expect(securityService.getUser()).andReturn(new JaxbUser()).anyTimes();
    replay(securityService);


    schedulerMigrationService.setSecurityService(securityService);
  }

  @Test
  @Ignore
  public void testSchedulerMigration() throws Exception {
    BundleContext bundleContext = createNiceMock(BundleContext.class);
    expect(bundleContext.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER)).andReturn("root").anyTimes();

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(cc, bundleContext);

    schedulerMigrationService.activate(cc);
  }
}
