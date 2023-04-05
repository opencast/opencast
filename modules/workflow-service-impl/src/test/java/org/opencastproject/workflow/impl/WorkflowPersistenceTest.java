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

package org.opencastproject.workflow.impl;

import static org.opencastproject.db.DBTestEnv.getDbSessionFactory;
import static org.opencastproject.db.DBTestEnv.newEntityManagerFactory;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityConstants;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowServiceDatabaseImpl;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests persistence: storing, retrieving and removing.
 *
 */
public class WorkflowPersistenceTest {

  private WorkflowServiceDatabaseImpl workflowDatabase;
  private WorkflowInstance workflowInstance1;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    // Mock up a security service
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    User user = new JaxbUser("admin", "test", new DefaultOrganization(), new JaxbRole(
            SecurityConstants.GLOBAL_ADMIN_ROLE, new DefaultOrganization()));
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.expect(securityService.getUser()).andReturn(user).anyTimes();
    EasyMock.replay(securityService);

    workflowDatabase = new WorkflowServiceDatabaseImpl();
    workflowDatabase.setEntityManagerFactory(newEntityManagerFactory(WorkflowServiceDatabaseImpl.PERSISTENCE_UNIT));
    workflowDatabase.setDBSessionFactory(getDbSessionFactory());
    workflowDatabase.setSecurityService(securityService);
    workflowDatabase.activate(null);

    workflowInstance1 = new WorkflowInstance();
    workflowInstance1.setId(1);
    workflowInstance1.setState(WorkflowInstance.WorkflowState.INSTANTIATED);
  }

  @Test
  public void testAdding() throws Exception {
    workflowDatabase.updateInDatabase(workflowInstance1);
  }

  @Test
  public void testDeleting() throws Exception {
    workflowDatabase.updateInDatabase(workflowInstance1);
    workflowDatabase.removeFromDatabase(workflowInstance1);
  }
}
