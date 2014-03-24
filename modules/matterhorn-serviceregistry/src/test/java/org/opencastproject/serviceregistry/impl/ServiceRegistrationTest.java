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
package org.opencastproject.serviceregistry.impl;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceState;
import org.opencastproject.util.UrlSupport;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceRegistrationTest {

  private static final String JOB_TYPE_1 = "testing1";
  private static final String OPERATION_NAME_1 = "op1";
  private static final String OPERATION_NAME_2 = "op2";
  private static final String LOCALHOST = UrlSupport.DEFAULT_BASE_URL;
  private static final String REMOTEHOST_1 = "http://remotehost1:8080";
  private static final String REMOTEHOST_2 = "http://remotehost2:8080";
  private static final String PATH_1 = "/path1";
  private static final String PATH_2 = "/path2";

  private ComboPooledDataSource pooledDataSource = null;
  private ServiceRegistryJpaImpl serviceRegistry = null;

  private ServiceRegistrationJpaImpl regType1Localhost = null;
  private ServiceRegistrationJpaImpl regType1Remotehost1 = null;
  private ServiceRegistrationJpaImpl regType1Remotehost2 = null;

  @Before
  public void setUp() throws Exception {
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

    serviceRegistry = new ServiceRegistryJpaImpl();
    serviceRegistry.setPersistenceProvider(new PersistenceProvider());
    serviceRegistry.setPersistenceProperties(props);
    serviceRegistry.activate(null);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);
    serviceRegistry.setOrganizationDirectoryService(organizationDirectoryService);

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);
    User anonymous = new JaxbUser("anonymous", jaxbOrganization, new JaxbRole(jaxbOrganization.getAnonymousRole(),
            jaxbOrganization));
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);
    serviceRegistry.setSecurityService(securityService);

    // The service registry will automatically register this host with the available number of processors.
    // This is potentially ruining our test setup.
    serviceRegistry.unregisterHost(LOCALHOST);

    // register the hosts
    serviceRegistry.registerHost(LOCALHOST, 1);
    serviceRegistry.registerHost(REMOTEHOST_1, 1);
    serviceRegistry.registerHost(REMOTEHOST_2, 1);

    // register some service instances
    regType1Localhost = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_1, LOCALHOST, PATH_1);
    regType1Remotehost1 = (ServiceRegistrationJpaImpl) serviceRegistry
            .registerService(JOB_TYPE_1, REMOTEHOST_1, PATH_1);
    regType1Remotehost2 = (ServiceRegistrationJpaImpl) serviceRegistry
            .registerService(JOB_TYPE_1, REMOTEHOST_2, PATH_2);
  }

  @After
  public void tearDown() throws Exception {
    serviceRegistry.unRegisterService(JOB_TYPE_1, LOCALHOST);
    serviceRegistry.unRegisterService(JOB_TYPE_1, REMOTEHOST_1);
    serviceRegistry.unRegisterService(JOB_TYPE_1, REMOTEHOST_2);
    serviceRegistry.deactivate();
    pooledDataSource.close();
  }

  @Test
  public void testHostCapacity() throws Exception {
    List<ServiceRegistration> services = serviceRegistry.getServiceRegistrations();
    List<HostRegistration> hosts = serviceRegistry.getHostRegistrations();
    Map<String, Integer> hostLoads = serviceRegistry.getHostLoads(serviceRegistry.emf.createEntityManager(), true);
    List<ServiceRegistration> availableServices = serviceRegistry.getServiceRegistrationsWithCapacity(JOB_TYPE_1,
            services, hosts, hostLoads);

    // Make sure all hosts are available for processing
    Assert.assertEquals(3, availableServices.size());

    // Create a job and mark it as running.
    Job job = serviceRegistry.createJob(regType1Localhost.getHost(), regType1Localhost.getServiceType(), OPERATION_NAME_1, null,
            null, false, null);
    job.setStatus(Job.Status.RUNNING);
    serviceRegistry.updateJob(job);

    // Recalculate the number of available services
    hostLoads = serviceRegistry.getHostLoads(serviceRegistry.emf.createEntityManager(), true);
    availableServices = serviceRegistry.getServiceRegistrationsWithCapacity(JOB_TYPE_1, services, hosts, hostLoads);

    // Since host 1 is now maxed out, only two more hosts should show up
    Assert.assertEquals(2, availableServices.size());
  }

  @Test
  public void testScenarioOneJobOneService() throws Exception {
    JobJpaImpl jobTry1 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    JobJpaImpl jobTry2 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    JobJpaImpl jobTry3 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    ServiceRegistrationJpaImpl updatedService;

    // 1st try, failed on localhost
    jobTry1.setStatus(Status.FAILED);
    jobTry1.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(jobTry1);
    updatedService = getUpdatedService(regType1Localhost);
    Assert.assertEquals(ServiceState.WARNING, updatedService.getServiceState());
    Assert.assertEquals(serviceRegistry.getJob(jobTry1.getId()).getSignature(), updatedService.getWarningStateTrigger());
    Assert.assertEquals(0, updatedService.getErrorStateTrigger());

    // 2nd try, failed on localhost
    jobTry2.setStatus(Status.FAILED);
    jobTry2.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(jobTry2);
    updatedService = getUpdatedService(regType1Localhost);
    Assert.assertEquals(ServiceState.WARNING, updatedService.getServiceState());
    Assert.assertEquals(serviceRegistry.getJob(jobTry1.getId()).getSignature(), updatedService.getWarningStateTrigger());
    Assert.assertEquals(0, updatedService.getErrorStateTrigger());

    // 3rd try, finished on localhost
    jobTry3.setStatus(Status.FINISHED);
    jobTry3.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(jobTry3);
    updatedService = getUpdatedService(regType1Localhost);
    Assert.assertEquals(ServiceState.NORMAL, updatedService.getServiceState());
    Assert.assertEquals(serviceRegistry.getJob(jobTry1.getId()).getSignature(), updatedService.getWarningStateTrigger());
    Assert.assertEquals(0, updatedService.getErrorStateTrigger());
  }

  @Test
  public void testScenarioManyJobsManyServices() throws Exception {
    JobJpaImpl job1Try1 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    JobJpaImpl job1Try2 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    JobJpaImpl job1Try3 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    JobJpaImpl job1Try4 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    List<String> list = new ArrayList<String>();
    list.add("test");
    JobJpaImpl job2Try1 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_2, list, null, true, null);
    JobJpaImpl job2Try2 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_2, list, null, true, null);
    JobJpaImpl job2Try3 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_2, list, null, true, null);
    serviceRegistry.maxAttemptsBeforeErrorState = 0;
    ServiceRegistrationJpaImpl updatedService1;
    ServiceRegistrationJpaImpl updatedService2;
    ServiceRegistrationJpaImpl updatedService3;

    // 1st try for job 1, failed on localhost
    job1Try1.setStatus(Status.FAILED);
    job1Try1.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(job1Try1);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.WARNING, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());
    Assert.assertEquals(serviceRegistry.getJob(job1Try1.getId()).getSignature(),
            updatedService1.getWarningStateTrigger());
    Assert.assertEquals(0, updatedService1.getErrorStateTrigger());

    // 1st try for job 2, failed on localhost
    job2Try1.setStatus(Status.FAILED);
    job2Try1.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(job2Try1);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.ERROR, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());
    Assert.assertEquals(serviceRegistry.getJob(job1Try1.getId()).getSignature(),
            updatedService1.getWarningStateTrigger());
    Assert.assertEquals(serviceRegistry.getJob(job2Try1.getId()).getSignature(), updatedService1.getErrorStateTrigger());

    // 2nd try for job 1, failed on remotehost1
    job1Try2.setStatus(Status.FAILED);
    job1Try2.setProcessorServiceRegistration(regType1Remotehost1);
    serviceRegistry.updateJob(job1Try2);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.ERROR, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.WARNING, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());
    Assert.assertEquals(serviceRegistry.getJob(job1Try1.getId()).getSignature(),
            updatedService1.getWarningStateTrigger());
    Assert.assertEquals(serviceRegistry.getJob(job2Try1.getId()).getSignature(), updatedService1.getErrorStateTrigger());
    Assert.assertEquals(serviceRegistry.getJob(job1Try2.getId()).getSignature(),
            updatedService2.getWarningStateTrigger());
    Assert.assertEquals(0, updatedService2.getErrorStateTrigger());

    // 2nd try for job 2, failed on remotehost1
    job2Try2.setStatus(Status.FINISHED);
    job2Try2.setProcessorServiceRegistration(regType1Remotehost1);
    serviceRegistry.updateJob(job2Try2);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService2 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.ERROR, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());
    Assert.assertEquals(0, updatedService2.getErrorStateTrigger());

    // 3rd try for job 1, failed on remotehost2
    job1Try3.setStatus(Status.FINISHED);
    job1Try3.setProcessorServiceRegistration(regType1Remotehost2);
    serviceRegistry.updateJob(job1Try3);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.ERROR, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());
    Assert.assertEquals(0, updatedService3.getWarningStateTrigger());
    Assert.assertEquals(0, updatedService2.getErrorStateTrigger());

    // 3rd try for job2, failed on remotehost2
    job2Try3.setStatus(Status.FAILED);
    job2Try3.setProcessorServiceRegistration(regType1Remotehost2);
    serviceRegistry.updateJob(job2Try3);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.WARNING, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());
    Assert.assertEquals(serviceRegistry.getJob(job1Try1.getId()).getSignature(),
            updatedService1.getWarningStateTrigger());

    // 4rd try for job1, failed on remotehost2
    job1Try4.setStatus(Status.FAILED);
    job1Try4.setProcessorServiceRegistration(regType1Remotehost2);
    serviceRegistry.updateJob(job1Try4);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.NORMAL, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());

  }

  @Test
  public void testScenarioOneJobManyServices() throws Exception {
    JobJpaImpl jobTry1 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    JobJpaImpl jobTry2 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    JobJpaImpl jobTry3 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    JobJpaImpl jobTry4 = (JobJpaImpl) serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    ServiceRegistrationJpaImpl updatedService1;
    ServiceRegistrationJpaImpl updatedService2;
    ServiceRegistrationJpaImpl updatedService3;

    // 1st try, failed on localhost
    jobTry1.setStatus(Status.FAILED);
    jobTry1.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(jobTry1);
    updatedService1 = (ServiceRegistrationJpaImpl) serviceRegistry.getServiceRegistration(JOB_TYPE_1,
            regType1Localhost.getHost());
    Assert.assertEquals(ServiceState.WARNING, updatedService1.getServiceState());
    Assert.assertEquals(serviceRegistry.getJob(jobTry1.getId()).getSignature(),
            updatedService1.getWarningStateTrigger());
    Assert.assertEquals(0, updatedService1.getErrorStateTrigger());

    // 2nd try, failed on remotehost1
    jobTry2.setStatus(Status.FAILED);
    jobTry2.setProcessorServiceRegistration(regType1Remotehost1);
    serviceRegistry.updateJob(jobTry2);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    Assert.assertEquals(ServiceState.NORMAL, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(0, updatedService2.getWarningStateTrigger());
    Assert.assertEquals(0, updatedService2.getErrorStateTrigger());

    // 3rd try, failed on remotehost2
    jobTry3.setStatus(Status.FAILED);
    jobTry3.setProcessorServiceRegistration(regType1Remotehost2);
    serviceRegistry.updateJob(jobTry3);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.NORMAL, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.WARNING, updatedService3.getServiceState());
    Assert.assertEquals(serviceRegistry.getJob(jobTry1.getId()).getSignature(),
            updatedService3.getWarningStateTrigger());
    Assert.assertEquals(0, updatedService2.getErrorStateTrigger());

    // 4th try, finished on localhost
    jobTry4.setStatus(Status.FINISHED);
    jobTry4.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(jobTry4);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.NORMAL, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.ERROR, updatedService3.getServiceState());
    Assert.assertEquals(serviceRegistry.getJob(jobTry1.getId()).getSignature(), updatedService3.getErrorStateTrigger());
  }

  /**
   * Gets the updated given service
   * 
   * @param service
   *          The service to get
   * @return The updated servce.
   */
  private ServiceRegistrationJpaImpl getUpdatedService(ServiceRegistrationJpaImpl service) {
    return (ServiceRegistrationJpaImpl) serviceRegistry.getServiceRegistration(service.getServiceType(),
            service.getHost());
  }

}
