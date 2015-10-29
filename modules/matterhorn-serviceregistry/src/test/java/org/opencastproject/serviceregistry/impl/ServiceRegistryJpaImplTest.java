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

package org.opencastproject.serviceregistry.impl;

import static org.junit.Assert.assertEquals;

import org.opencastproject.job.api.Job.Status;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.systems.MatterhornConstants;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.jmx.JmxUtil;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectInstance;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

public class ServiceRegistryJpaImplTest {
  private Query query = null;
  private JobJpaImpl undispatchableJob1 = null;
  private JobJpaImpl undispatchableJob2 = null;
  private EntityTransaction tx = null;
  private EntityManager em = null;
  private EntityManagerFactory emf = null;
  private BundleContext bundleContext = null;
  private ComponentContext cc = null;
  private ServiceRegistryJpaImpl serviceRegistryJpaImpl = null;
  private ComboPooledDataSource pooledDataSource = null;

  private static final String TEST_SERVICE = "ingest";
  private static final String TEST_OPERATION = "ingest";
  private static final String TEST_PATH = "/ingest";
  private static final String TEST_HOST = "http://localhost:8080";
  private static final String TEST_HOST_OTHER = "http://otherhost:8080";

  @Before
  public void setUp() throws InvalidSyntaxException, PropertyVetoException, NotFoundException {
    // Setup mock objects
    setUpQuery();
    setUpEntityTransaction();
    setUpEntityManager();
    setUpEntityManagerFactory();
    // Setup context settings
    setupBundleContext();
    setupComponentContext();
    // Setup test object.
    setUpServiceRegistryJpaImpl();
  }

  @After
  public void tearDown() throws ServiceRegistryException {
    for (ObjectInstance mbean : serviceRegistryJpaImpl.jmxBeans) {
      JmxUtil.unregisterMXBean(mbean);
    }
    for (ServiceRegistration service : serviceRegistryJpaImpl.getServiceRegistrations()) {
      serviceRegistryJpaImpl.unRegisterService(service.getServiceType(), service.getHost());
    }
    serviceRegistryJpaImpl.deactivate();
    pooledDataSource.close();
  }

  public void setUpQuery() {
    query = EasyMock.createNiceMock(Query.class);
    EasyMock.expect(query.getSingleResult())
            .andReturn(new HostRegistrationJpaImpl(TEST_HOST, "127.0.0.1", 1024 * 1024 * 1024, 9, 9, true, false))
            .anyTimes();
    EasyMock.expect(query.getResultList()).andReturn(new ArrayList<Object>()).anyTimes();
    EasyMock.replay(query);
  }

  public void setUpUndispatchableJobs() throws ServiceRegistryException {

    undispatchableJob1 = (JobJpaImpl) serviceRegistryJpaImpl.createJob(TEST_HOST, TEST_SERVICE, TEST_OPERATION, null,
            null, false, null);
    undispatchableJob2 = (JobJpaImpl) serviceRegistryJpaImpl.createJob(TEST_HOST_OTHER, TEST_SERVICE, TEST_OPERATION,
            null, null, false, null);
    undispatchableJob1.setDateStarted(new Date());
    undispatchableJob1.setStatus(Status.RUNNING);
    undispatchableJob2.setDateStarted(new Date());
    undispatchableJob2.setStatus(Status.RUNNING);
    serviceRegistryJpaImpl.updateJob(undispatchableJob1);
    serviceRegistryJpaImpl.updateJob(undispatchableJob2);

  }

  public void setUpEntityTransaction() {
    tx = EasyMock.createNiceMock(EntityTransaction.class);
  }

  public void setUpEntityManager() {
    em = EasyMock.createNiceMock(EntityManager.class);
    EasyMock.expect(em.getTransaction()).andReturn(tx).anyTimes();
    EasyMock.expect(em.createNamedQuery("HostRegistration.byHostName")).andReturn(query);
    EasyMock.expect(em.createNamedQuery("ServiceRegistration.statistics")).andReturn(query);
    EasyMock.expect(em.createNamedQuery("ServiceRegistration.getAll")).andReturn(query);
    EasyMock.replay(em);
  }

  public void setUpEntityManagerFactory() {
    emf = EasyMock.createMock(EntityManagerFactory.class);
    EasyMock.expect(emf.createEntityManager()).andReturn(em).anyTimes();
    EasyMock.replay(emf);
  }

  public void setUpServiceRegistryJpaImpl() throws PropertyVetoException, NotFoundException {
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");

    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis());
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    final PersistenceProvider pp = new PersistenceProvider();
    serviceRegistryJpaImpl = new ServiceRegistryJpaImpl();
    serviceRegistryJpaImpl.setPersistenceProvider(pp);
    serviceRegistryJpaImpl.setPersistenceProperties(props);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();

    EasyMock.replay(organizationDirectoryService);
    serviceRegistryJpaImpl.setOrganizationDirectoryService(organizationDirectoryService);

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);
    User anonymous = new JaxbUser("anonymous", "test", jaxbOrganization, new JaxbRole(
            jaxbOrganization.getAnonymousRole(), jaxbOrganization));
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);
    serviceRegistryJpaImpl.setSecurityService(securityService);
  }

  private void registerTestHostAndService() throws ServiceRegistryException {
    // register the hosts, service must be activated at this point
    serviceRegistryJpaImpl.registerHost(TEST_HOST, "127.0.0.1", 1024, 1, 1);
    serviceRegistryJpaImpl.registerHost(TEST_HOST_OTHER, "127.0.0.1", 1024, 1, 1);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE, TEST_HOST, TEST_PATH);
    serviceRegistryJpaImpl.registerService(TEST_SERVICE, TEST_HOST_OTHER, TEST_PATH);
  }

  private void setupBundleContext() throws InvalidSyntaxException {
    bundleContext = EasyMock.createNiceMock(BundleContext.class);
    EasyMock.expect(bundleContext.getProperty(MatterhornConstants.SERVER_URL_PROPERTY)).andReturn("");
    EasyMock.expect(bundleContext.getProperty("org.opencastproject.jobs.url")).andReturn("");
    EasyMock.expect(bundleContext.getProperty(ServiceRegistryJpaImpl.OPT_MAXLOAD)).andReturn("");
    EasyMock.expect(bundleContext.createFilter((String) EasyMock.anyObject())).andReturn(
            EasyMock.createNiceMock(Filter.class));
    EasyMock.expect(bundleContext.getProperty(ServiceRegistryJpaImpl.OPT_DISPATCHINTERVAL)).andReturn("0");
  }

  private void setupComponentContext() {
    cc = EasyMock.createMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();
    EasyMock.replay(cc);
  }

  @Test
  public void nullContextActivatesOkay() throws ServiceRegistryException {
    serviceRegistryJpaImpl.activate(null);
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteJobInvalidJobId() throws Exception {
    serviceRegistryJpaImpl.activate(null);
    serviceRegistryJpaImpl.removeJob(-1L);
  }

  @Test
  public void testCancelUndispatchablesOrphanedByActivatingNode() throws Exception {
    serviceRegistryJpaImpl.activate(null);
    registerTestHostAndService();
    setUpUndispatchableJobs();
    // verify the current running status
    undispatchableJob1 = (JobJpaImpl) serviceRegistryJpaImpl.getJob(undispatchableJob1.getId());
    assertEquals(Status.RUNNING, undispatchableJob1.getStatus());
    undispatchableJob2 = (JobJpaImpl) serviceRegistryJpaImpl.getJob(undispatchableJob2.getId());
    assertEquals(Status.RUNNING, undispatchableJob2.getStatus());

    // remove the activate beans, so this can be reactivated
    for (ObjectInstance mbean : serviceRegistryJpaImpl.jmxBeans) {
      JmxUtil.unregisterMXBean(mbean);
    }

    // reactivate and expect local undispatchable job to be canceled, but not the remote job
    serviceRegistryJpaImpl.activate(null);
    System.out.println("Undispatachable job 1 " + undispatchableJob1.getId());
    undispatchableJob1 = (JobJpaImpl) serviceRegistryJpaImpl.getJob(undispatchableJob1.getId());
    assertEquals(Status.CANCELED, undispatchableJob1.getStatus());
    System.out.println("Undispatachable job 1 " + undispatchableJob2.getId());
    undispatchableJob2 = (JobJpaImpl) serviceRegistryJpaImpl.getJob(undispatchableJob2.getId());
    assertEquals(Status.RUNNING, undispatchableJob2.getStatus());

  }
}
