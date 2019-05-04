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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

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
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.serviceregistry.impl.jpa.ServiceRegistrationJpaImpl;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.persistence.PersistenceUtil;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ServiceRegistrationTest {

  private static final String JOB_TYPE_1 = "testing1";
  private static final String OPERATION_NAME_1 = "op1";
  private static final String OPERATION_NAME_2 = "op2";
  private static final String LOCALHOST = UrlSupport.DEFAULT_BASE_URL;
  private static final String REMOTEHOST_1 = "http://remotehost1:8080";
  private static final String REMOTEHOST_2 = "http://remotehost2:8080";
  private static final String PATH_1 = "/path1";
  private static final String PATH_2 = "/path2";

  private ServiceRegistryJpaImpl serviceRegistry = null;

  private ServiceRegistrationJpaImpl regType1Localhost = null;
  private ServiceRegistrationJpaImpl regType1Remotehost1 = null;
  private ServiceRegistrationJpaImpl regType1Remotehost2 = null;

  @Before
  public void setUp() throws Exception {
    serviceRegistry = new ServiceRegistryJpaImpl();
    serviceRegistry.setEntityManagerFactory(PersistenceUtil
            .newTestEntityManagerFactory(ServiceRegistryJpaImpl.PERSISTENCE_UNIT));
    serviceRegistry.activate(null);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    expect(organizationDirectoryService.getOrganization((String) anyObject())).andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);
    serviceRegistry.setOrganizationDirectoryService(organizationDirectoryService);

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);
    User anonymous = new JaxbUser("anonymous", "test", jaxbOrganization, new JaxbRole(
            jaxbOrganization.getAnonymousRole(), jaxbOrganization));
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);
    serviceRegistry.setSecurityService(securityService);

    // The service registry will automatically register this host with the available number of processors.
    // This is potentially ruining our test setup.
    serviceRegistry.unregisterHost(LOCALHOST);

    // register the hosts
    serviceRegistry.registerHost(LOCALHOST, "127.0.0.1", "local", 1024, 1, 1);
    serviceRegistry.registerHost(REMOTEHOST_1, "127.0.0.1", "remote1", 1024, 1, 1);
    serviceRegistry.registerHost(REMOTEHOST_2, "127.0.0.1", "remote2", 1024, 1, 1);

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
  }

  @Test
  public void testServiceRegistrationsByLoad() throws Exception {
    List<ServiceRegistration> services = serviceRegistry.getServiceRegistrations();
    List<HostRegistration> hosts = serviceRegistry.getHostRegistrations();
    SystemLoad hostLoads = serviceRegistry.getHostLoads(serviceRegistry.emf.createEntityManager());
    List<ServiceRegistration> availableServices = serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_1, services,
            hosts, hostLoads);

    // Make sure all hosts are available for processing
    Assert.assertEquals(3, availableServices.size());

    // Create a job and mark it as running.
    Job job = serviceRegistry.createJob(regType1Localhost.getHost(), regType1Localhost.getServiceType(),
            OPERATION_NAME_1, null, null, false, null);
    job.setStatus(Job.Status.RUNNING);
    job = serviceRegistry.updateJob(job);

    // Recalculate the number of available services
    hostLoads = serviceRegistry.getHostLoads(serviceRegistry.emf.createEntityManager());
    availableServices = serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_1, services, hosts, hostLoads);

    // Since the host load is not taken into account, still all tree services should show up
    Assert.assertEquals(3, availableServices.size());

    // Recalculate the number of available services after ignoring a host
    hosts.remove(regType1Remotehost1.getHostRegistration());
    availableServices = serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_1, services, hosts, hostLoads);

    // Since host 1 is now ignored, only two more services should show up
    Assert.assertEquals(2, availableServices.size());
  }

  @Test
  public void testHostCapacity() throws Exception {
    List<ServiceRegistration> services = serviceRegistry.getServiceRegistrations();
    List<HostRegistration> hosts = serviceRegistry.getHostRegistrations();
    SystemLoad hostLoads = serviceRegistry.getHostLoads(serviceRegistry.emf.createEntityManager());
    List<ServiceRegistration> availableServices = serviceRegistry.getServiceRegistrationsWithCapacity(JOB_TYPE_1,
            services, hosts, hostLoads);

    // Make sure all hosts are available for processing
    Assert.assertEquals(3, availableServices.size());

    // Create a job and mark it as running.
    Job job = serviceRegistry.createJob(regType1Localhost.getHost(), regType1Localhost.getServiceType(), OPERATION_NAME_1, null,
            null, false, null, 1.0f);
    job.setStatus(Job.Status.RUNNING);
    job = serviceRegistry.updateJob(job);

    // Recalculate the number of available services
    hostLoads = serviceRegistry.getHostLoads(serviceRegistry.emf.createEntityManager());
    availableServices = serviceRegistry.getServiceRegistrationsWithCapacity(JOB_TYPE_1, services, hosts, hostLoads);

    // Since host 1 is now maxed out, only two more services should show up
    Assert.assertEquals(2, availableServices.size());

    // Recalculate the number of available services after ignoring a host
    hosts.remove(regType1Remotehost1.getHostRegistration());
    availableServices = serviceRegistry.getServiceRegistrationsWithCapacity(JOB_TYPE_1, services, hosts, hostLoads);

    // Since remote host 1 is now ignored, only one more service should show up
    Assert.assertEquals(1, availableServices.size());
  }

  @Test
  public void testScenarioOneJobOneService() throws Exception {
    Job jobTry1 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    Job jobTry2 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    Job jobTry3 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    ServiceRegistrationJpaImpl updatedService;

    // 1st try, failed on localhost
    jobTry1.setStatus(Status.FAILED);
    jobTry1.setJobType(regType1Localhost.getServiceType());
    jobTry1.setProcessingHost(regType1Localhost.getHost());
    jobTry1 = serviceRegistry.updateJob(jobTry1);
    updatedService = getUpdatedService(regType1Localhost);
    Assert.assertEquals(ServiceState.WARNING, updatedService.getServiceState());
    Assert.assertEquals(0, updatedService.getErrorStateTrigger());

    // 2nd try, failed on localhost
    jobTry2.setStatus(Status.FAILED);
    jobTry2.setJobType(regType1Localhost.getServiceType());
    jobTry2.setProcessingHost(regType1Localhost.getHost());
    jobTry2 = serviceRegistry.updateJob(jobTry2);
    updatedService = getUpdatedService(regType1Localhost);
    Assert.assertEquals(ServiceState.WARNING, updatedService.getServiceState());
    Assert.assertEquals(0, updatedService.getErrorStateTrigger());

    // 3rd try, finished on localhost
    jobTry3.setStatus(Status.FINISHED);
    jobTry3.setJobType(regType1Localhost.getServiceType());
    jobTry3.setProcessingHost(regType1Localhost.getHost());
    jobTry3 = serviceRegistry.updateJob(jobTry3);
    updatedService = getUpdatedService(regType1Localhost);
    Assert.assertEquals(ServiceState.NORMAL, updatedService.getServiceState());
    Assert.assertEquals(0, updatedService.getErrorStateTrigger());
  }

  @Test
  public void testScenarioManyJobsManyServices() throws Exception {
    Job job1Try1 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    Job job1Try2 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    Job job1Try3 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    Job job1Try4 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    List<String> list = new ArrayList<String>();
    list.add("test");
    Job job2Try1 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_2, list, null, true, null);
    Job job2Try2 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_2, list, null, true, null);
    Job job2Try3 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_2, list, null, true, null);
    serviceRegistry.maxAttemptsBeforeErrorState = 0;
    ServiceRegistrationJpaImpl updatedService1;
    ServiceRegistrationJpaImpl updatedService2;
    ServiceRegistrationJpaImpl updatedService3;

    // 1st try for job 1, failed on localhost
    job1Try1.setStatus(Status.FAILED);
    job1Try1.setJobType(regType1Localhost.getServiceType());
    job1Try1.setProcessingHost(regType1Localhost.getHost());
    job1Try1 = serviceRegistry.updateJob(job1Try1);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.WARNING, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());
    Assert.assertEquals(0, updatedService1.getErrorStateTrigger());

    // 1st try for job 2, failed on localhost
    job2Try1.setStatus(Status.FAILED);
    job2Try1.setJobType(regType1Localhost.getServiceType());
    job2Try1.setProcessingHost(regType1Localhost.getHost());
    serviceRegistry.updateJob(job2Try1);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.ERROR, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());

    // 2nd try for job 1, failed on remotehost1
    job1Try2.setStatus(Status.FAILED);
    job1Try2.setJobType(regType1Remotehost1.getServiceType());
    job1Try2.setProcessingHost(regType1Remotehost1.getHost());
    serviceRegistry.updateJob(job1Try2);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.ERROR, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.WARNING, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());
    updatedService2.getWarningStateTrigger();
    Assert.assertEquals(0, updatedService2.getErrorStateTrigger());

    // 2nd try for job 2, failed on remotehost1
    job2Try2.setStatus(Status.FINISHED);
    job2Try2.setJobType(regType1Remotehost1.getServiceType());
    job2Try2.setProcessingHost(regType1Remotehost1.getHost());
    serviceRegistry.updateJob(job2Try2);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.ERROR, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());
    Assert.assertEquals(0, updatedService2.getErrorStateTrigger());

    // 3rd try for job 1, failed on remotehost2
    job1Try3.setStatus(Status.FINISHED);
    job1Try3.setJobType(regType1Remotehost2.getServiceType());
    job1Try3.setProcessingHost(regType1Remotehost2.getHost());
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
    job2Try3.setJobType(regType1Remotehost2.getServiceType());
    job2Try3.setProcessingHost(regType1Remotehost2.getHost());
    serviceRegistry.updateJob(job2Try3);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.WARNING, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService3.getServiceState());

    // 4rd try for job1, failed on remotehost2
    job1Try4.setStatus(Status.FAILED);
    job1Try4.setJobType(regType1Remotehost2.getServiceType());
    job1Try4.setProcessingHost(regType1Remotehost2.getHost());
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
    Job jobTry1 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    Job jobTry2 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    Job jobTry3 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    Job jobTry4 = serviceRegistry.createJob(regType1Localhost.getHost(),
            regType1Localhost.getServiceType(), OPERATION_NAME_1, null, null, true, null);
    ServiceRegistrationJpaImpl updatedService1;
    ServiceRegistrationJpaImpl updatedService2;
    ServiceRegistrationJpaImpl updatedService3;

    // 1st try, failed on localhost
    jobTry1.setStatus(Status.FAILED);
    jobTry1.setJobType(regType1Localhost.getServiceType());
    jobTry1.setProcessingHost(regType1Localhost.getHost());
    jobTry1 = serviceRegistry.updateJob(jobTry1);
    updatedService1 = (ServiceRegistrationJpaImpl) serviceRegistry.getServiceRegistration(JOB_TYPE_1,
            regType1Localhost.getHost());
    Assert.assertEquals(ServiceState.WARNING, updatedService1.getServiceState());
    Assert.assertEquals(0, updatedService1.getErrorStateTrigger());

    // 2nd try, failed on remotehost1
    jobTry2.setStatus(Status.FAILED);
    jobTry2.setJobType(regType1Remotehost1.getServiceType());
    jobTry2.setProcessingHost(regType1Remotehost1.getHost());
    jobTry2 = serviceRegistry.updateJob(jobTry2);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    Assert.assertEquals(ServiceState.NORMAL, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(0, updatedService2.getWarningStateTrigger());
    Assert.assertEquals(0, updatedService2.getErrorStateTrigger());

    // 3rd try, failed on remotehost2
    jobTry3.setStatus(Status.FAILED);
    jobTry3.setJobType(regType1Remotehost2.getServiceType());
    jobTry3.setProcessingHost(regType1Remotehost2.getHost());
    jobTry3 = serviceRegistry.updateJob(jobTry3);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.NORMAL, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.WARNING, updatedService3.getServiceState());
    Assert.assertEquals(0, updatedService2.getErrorStateTrigger());

    // 4th try, finished on localhost
    jobTry4.setStatus(Status.FINISHED);
    jobTry4.setJobType(regType1Localhost.getServiceType());
    jobTry4.setProcessingHost(regType1Localhost.getHost());
    jobTry4 = serviceRegistry.updateJob(jobTry4);
    updatedService1 = getUpdatedService(regType1Localhost);
    updatedService2 = getUpdatedService(regType1Remotehost1);
    updatedService3 = getUpdatedService(regType1Remotehost2);
    Assert.assertEquals(ServiceState.NORMAL, updatedService1.getServiceState());
    Assert.assertEquals(ServiceState.NORMAL, updatedService2.getServiceState());
    Assert.assertEquals(ServiceState.WARNING, updatedService3.getServiceState());
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
