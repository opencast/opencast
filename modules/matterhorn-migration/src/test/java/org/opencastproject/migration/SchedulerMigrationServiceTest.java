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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.opencastproject.migration.SchedulerMigrationService.CFG_ORGANIZATION;

import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.SchedulerService.SchedulerTransaction;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import java.beans.PropertyVetoException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

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

    SchedulerTransaction schedulerTransaction = createNiceMock(SchedulerTransaction.class);
    replay(schedulerTransaction);

    SchedulerService schedulerService = createNiceMock(SchedulerService.class);
    expect(schedulerService.createTransaction(anyString())).andReturn(schedulerTransaction).anyTimes();
    expect(schedulerService.search(anyObject(Opt.class), anyObject(Opt.class), anyObject(Opt.class),
            anyObject(Opt.class), anyObject(Opt.class))).andReturn(new ArrayList<>());
    replay(schedulerService);

    Workspace workspace = createNiceMock(Workspace.class);
    expect(workspace.put(anyString(), anyString(), anyString(), anyObject(InputStream.class)))
            .andReturn(new URI("test")).anyTimes();
    replay(workspace);

    AuthorizationService authorizationService = createNiceMock(AuthorizationService.class);
    replay(authorizationService);

    schedulerMigrationService.setAuthorizationService(authorizationService);
    schedulerMigrationService.setOrganizationDirectoryService(orgDirService);
    schedulerMigrationService.setSchedulerService(schedulerService);
    schedulerMigrationService.setSecurityService(securityService);
    schedulerMigrationService.setWorkspace(workspace);
  }

  @Test
  @Ignore
  public void testSchedulerMigration() throws Exception {
    BundleContext bundleContext = createNiceMock(BundleContext.class);
    expect(bundleContext.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER)).andReturn("root").anyTimes();
    expect(bundleContext.getProperty(CFG_ORGANIZATION)).andReturn("mh_default_org").anyTimes();

    ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(cc, bundleContext);

    DataSource dataSource = createDataSource("jdbc:mysql://localhost/test_scheduler", "opencast", "opencast");

    schedulerMigrationService.setDataSource(dataSource);
    schedulerMigrationService.activate(cc);
  }

  private DataSource createDataSource(String databaseUrl, String databaseUser, String databasePassword) {
    final Map<String, String> testEntityManagerProps = new HashMap<>();
    testEntityManagerProps.put("eclipselink.ddl-generation", "none");
    testEntityManagerProps.put("eclipselink.ddl-generation.output-mode", "database");

    final ComboPooledDataSource pooledDataSource = new ComboPooledDataSource();
    try {
      pooledDataSource.setDriverClass("com.mysql.jdbc.Driver");
    } catch (PropertyVetoException e) {
      throw new RuntimeException(e);
    }
    pooledDataSource.setJdbcUrl(databaseUrl);
    pooledDataSource.setUser(databaseUser);
    pooledDataSource.setPassword(databasePassword);
    return pooledDataSource;
  }

}
