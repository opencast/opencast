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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Arrays.mkString;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.functions.Booleans.eq;
import static org.opencastproject.util.persistence.PersistenceUtil.newPersistenceEnvironment;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.FailureReason;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.persistence.PersistenceEnv;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

public class JobTest {

  private static final String JOB_TYPE_1 = "testing1";
  private static final String JOB_TYPE_2 = "testing2";
  private static final String OPERATION_NAME = "op";
  private static final String LOCALHOST = UrlSupport.DEFAULT_BASE_URL;
  private static final String REMOTEHOST = "http://remotehost:8080";
  private static final String PATH = "/path";

  private ComboPooledDataSource pooledDataSource = null;
  private ServiceRegistryJpaImpl serviceRegistry = null;

  private ServiceRegistrationJpaImpl regType1Localhost = null;
  private ServiceRegistrationJpaImpl regType1Remotehost = null;
  private ServiceRegistrationJpaImpl regType2Localhost = null;
  @SuppressWarnings("unused")
  private ServiceRegistrationJpaImpl regType2Remotehost = null;
  private PersistenceEnv penv;

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

    final PersistenceProvider pp = new PersistenceProvider();
    serviceRegistry = new ServiceRegistryJpaImpl();
    serviceRegistry.setPersistenceProvider(pp);
    serviceRegistry.setPersistenceProperties(props);
    serviceRegistry.activate(null);

    Organization organization = new DefaultOrganization();
    OrganizationDirectoryService organizationDirectoryService = EasyMock.createMock(OrganizationDirectoryService.class);
    EasyMock.expect(organizationDirectoryService.getOrganization((String) EasyMock.anyObject()))
            .andReturn(organization).anyTimes();
    EasyMock.replay(organizationDirectoryService);
    serviceRegistry.setOrganizationDirectoryService(organizationDirectoryService);

    JaxbOrganization jaxbOrganization = JaxbOrganization.fromOrganization(organization);
    User anonymous = new JaxbUser("anonymous", "test", jaxbOrganization, new JaxbRole(
            jaxbOrganization.getAnonymousRole(), jaxbOrganization));
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(anonymous).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(organization).anyTimes();
    EasyMock.replay(securityService);
    serviceRegistry.setSecurityService(securityService);

    // register the hosts
    serviceRegistry.registerHost(LOCALHOST, "127.0.0.1", 1024, 1, 1);
    serviceRegistry.registerHost(REMOTEHOST, "127.0.0.1", 1024, 1, 1);

    // register some service instances
    regType1Localhost = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_1, LOCALHOST, PATH);
    regType1Remotehost = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_1, REMOTEHOST, PATH);
    regType2Localhost = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_2, LOCALHOST, PATH);
    regType2Remotehost = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_2, REMOTEHOST, PATH);

    penv = newPersistenceEnvironment(pp.createEntityManagerFactory("org.opencastproject.serviceregistry", props));
  }

  @After
  public void tearDown() throws Exception {
    serviceRegistry.unRegisterService(JOB_TYPE_1, LOCALHOST);
    serviceRegistry.unRegisterService(JOB_TYPE_1, REMOTEHOST);
    serviceRegistry.unRegisterService(JOB_TYPE_2, LOCALHOST);
    serviceRegistry.unRegisterService(JOB_TYPE_2, REMOTEHOST);
    serviceRegistry.deactivate();
    pooledDataSource.close();
  }

  @Test
  public void testGetJob() throws Exception {
    // Start a job, but don't allow it to be dispatched
    JobJpaImpl job = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false);

    Assert.assertNotNull(job.getUri());

    Job jobFromDb = serviceRegistry.getJob(job.getId());
    Assert.assertEquals(Status.INSTANTIATED, jobFromDb.getStatus());

    // Simulate starting the job
    job.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(job);

    // Finish the job
    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromURI(new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    t.setIdentifier("track-1");
    job.setPayload(MediaPackageElementParser.getAsXml(t));
    job.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(job);

    jobFromDb = serviceRegistry.getJob(job.getId());
    Assert.assertNotNull(jobFromDb.getUri());
    Assert.assertEquals(job.getPayload(), jobFromDb.getPayload());
  }

  @Test
  public void testGetJobs() throws Exception {
    Job job = serviceRegistry.createJob(LOCALHOST, JOB_TYPE_1, OPERATION_NAME, null, null, false, null);
    job.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(job);

    long id = job.getId();

    // Search using both the job type and status
    List<Job> jobs = serviceRegistry.getJobs(JOB_TYPE_1, Status.RUNNING);
    Assert.assertEquals(1, jobs.size());

    // Search using just the job type
    jobs = serviceRegistry.getJobs(JOB_TYPE_1, null);
    Assert.assertEquals(1, jobs.size());

    // Search using just the status
    jobs = serviceRegistry.getJobs(null, Status.RUNNING);
    Assert.assertEquals(1, jobs.size());

    // Search using nulls (return everything)
    jobs = serviceRegistry.getJobs(null, null);
    Assert.assertEquals(1, jobs.size());

    Job receipt = serviceRegistry.getJob(id);
    receipt.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(receipt);
    long queuedJobs = serviceRegistry.count(JOB_TYPE_1, Status.RUNNING);
    Assert.assertEquals(0, queuedJobs);
  }

  @Test
  public void testGetChildJobs() throws Exception {
    Job rootJob = serviceRegistry.createJob(LOCALHOST, JOB_TYPE_1, OPERATION_NAME, null, null, false, null);
    Job job = serviceRegistry.createJob(LOCALHOST, JOB_TYPE_1, OPERATION_NAME, null, null, false, rootJob);
    Job job1 = serviceRegistry.createJob(LOCALHOST, JOB_TYPE_1, OPERATION_NAME, null, null, false, job);
    Job job3 = serviceRegistry.createJob(LOCALHOST, JOB_TYPE_1, OPERATION_NAME, null, null, false, job);
    Job job4 = serviceRegistry.createJob(LOCALHOST, JOB_TYPE_1, OPERATION_NAME, null, null, false, job3);
    Job job2 = serviceRegistry.createJob(LOCALHOST, JOB_TYPE_1, OPERATION_NAME, null, null, false, job1);
    Job job5 = serviceRegistry.createJob(LOCALHOST, JOB_TYPE_1, OPERATION_NAME, null, null, false, job4);
    serviceRegistry.updateJob(job);
    serviceRegistry.updateJob(job1);
    serviceRegistry.updateJob(job2);
    serviceRegistry.updateJob(job3);
    serviceRegistry.updateJob(job4);
    serviceRegistry.updateJob(job5);

    // Search children by root job
    List<Job> jobs = serviceRegistry.getChildJobs(rootJob.getId());
    Assert.assertEquals(6, jobs.size());
    Assert.assertEquals(job, jobs.get(0));
    Assert.assertEquals(job1, jobs.get(1));
    Assert.assertEquals(job3, jobs.get(2));
    Assert.assertEquals(job4, jobs.get(3));
    Assert.assertEquals(job2, jobs.get(4));
    Assert.assertEquals(job5, jobs.get(5));

    // Search children
    jobs = serviceRegistry.getChildJobs(job.getId());
    Assert.assertEquals(5, jobs.size());
    Assert.assertEquals(job1, jobs.get(0));
    Assert.assertEquals(job3, jobs.get(1));
    Assert.assertEquals(job4, jobs.get(2));
    Assert.assertEquals(job2, jobs.get(3));
    Assert.assertEquals(job5, jobs.get(4));
  }

  @Test
  public void testCount() throws Exception {
    // create a receipt on each service instance
    serviceRegistry.createJob(regType1Localhost.getHost(), regType1Localhost.getServiceType(), OPERATION_NAME, null,
            null, false, null);
    serviceRegistry.createJob(regType1Remotehost.getHost(), regType1Remotehost.getServiceType(), OPERATION_NAME, null,
            null, false, null);

    // Since these jobs have not been dispatched to a host, there shouldn't be any jobs on those hosts
    Assert.assertEquals(0, serviceRegistry.countByHost(JOB_TYPE_1, LOCALHOST, Status.INSTANTIATED));
    Assert.assertEquals(0, serviceRegistry.countByHost(JOB_TYPE_1, REMOTEHOST, Status.INSTANTIATED));

    // Counting any job without regard to host should return both jobs
    Assert.assertEquals(2, serviceRegistry.count(JOB_TYPE_1, Status.INSTANTIATED));
  }

  @Test
  public void testCountByOperation() throws Exception {
    // create a receipt on each service instance
    serviceRegistry.createJob(regType1Localhost.getHost(), regType1Localhost.getServiceType(), OPERATION_NAME, null,
            null, false, null);
    serviceRegistry.createJob(regType1Remotehost.getHost(), regType1Remotehost.getServiceType(), OPERATION_NAME, null,
            null, false, null);

    // Counting any job without regard to host should return both jobs
    Assert.assertEquals(2, serviceRegistry.countByOperation(JOB_TYPE_1, OPERATION_NAME, Status.INSTANTIATED));
  }

  @Test
  public void testGetHostsCountWithNoJobs() throws Exception {
    Assert.assertEquals(2, serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_1).size());
    Assert.assertEquals(2, serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_2).size());
  }

  @Test
  public void testNoJobServiceRegistrations() throws Exception {
    List<ServiceRegistration> type1Hosts = serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_1);
    List<ServiceRegistration> type2Hosts = serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_2);
    Assert.assertEquals(2, type1Hosts.size());
    Assert.assertEquals(2, type2Hosts.size());
  }

  @Test
  public void testGetHostsCount() throws Exception {
    JobJpaImpl localRunning1 = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false);
    localRunning1.setStatus(Status.RUNNING);
    localRunning1.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(localRunning1);

    JobJpaImpl localRunning2 = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false);
    localRunning2.setStatus(Status.RUNNING);
    localRunning2.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(localRunning2);

    JobJpaImpl localFinished = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false);
    // Simulate starting the job
    localFinished.setStatus(Status.RUNNING);
    localFinished.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(localFinished);
    // Finish the job
    localFinished.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(localFinished);

    JobJpaImpl remoteRunning = (JobJpaImpl) serviceRegistry.createJob(regType1Remotehost.getHost(),
            regType1Remotehost.getServiceType(), OPERATION_NAME, null, null, false, null);
    remoteRunning.setStatus(Status.RUNNING);
    remoteRunning.setProcessorServiceRegistration(regType1Remotehost);
    serviceRegistry.updateJob(remoteRunning);

    JobJpaImpl remoteFinished = (JobJpaImpl) serviceRegistry.createJob(regType1Remotehost.getHost(),
            regType1Remotehost.getServiceType(), OPERATION_NAME, null, null, false, null);
    // Simulate starting the job
    remoteFinished.setStatus(Status.RUNNING);
    remoteFinished.setProcessorServiceRegistration(regType1Remotehost);
    serviceRegistry.updateJob(remoteFinished);
    // Finish the job
    remoteFinished.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(remoteFinished);

    JobJpaImpl otherTypeRunning = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_2, OPERATION_NAME, null, null, false);
    otherTypeRunning.setStatus(Status.RUNNING);
    otherTypeRunning.setProcessorServiceRegistration(regType2Localhost);
    serviceRegistry.updateJob(otherTypeRunning);

    JobJpaImpl otherTypeFinished = (JobJpaImpl) serviceRegistry
            .createJob(JOB_TYPE_2, OPERATION_NAME, null, null, false);
    // Simulate starting the job
    otherTypeFinished.setStatus(Status.RUNNING);
    otherTypeFinished.setProcessorServiceRegistration(regType2Localhost);
    serviceRegistry.updateJob(otherTypeFinished);
    // Finish the job
    otherTypeFinished.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(otherTypeFinished);

    List<ServiceRegistration> type1Hosts = serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_1);
    List<ServiceRegistration> type2Hosts = serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_2);

    // The number of service registrations is equal on both hosts
    Assert.assertEquals(2, type1Hosts.size());
    Assert.assertEquals(2, type2Hosts.size());

    // Count the number of jobs that are runnging
    Assert.assertEquals(3, serviceRegistry.count(JOB_TYPE_1, Status.RUNNING));
    Assert.assertEquals(1, serviceRegistry.count(JOB_TYPE_2, Status.RUNNING));

    // Localhost has more jobs running than remotehost
    Assert.assertEquals(REMOTEHOST, type1Hosts.get(0).getHost());
    Assert.assertEquals(LOCALHOST, type1Hosts.get(1).getHost());
    Assert.assertEquals(REMOTEHOST, type2Hosts.get(0).getHost());
    Assert.assertEquals(LOCALHOST, type2Hosts.get(1).getHost());
  }

  @Test
  public void testCountPerHostService() throws Exception {
    // create some test data
    testGetHostsCount();
    // Add an additional dispatchable job. This is important as it leaves the processing host field empty.
    JobJpaImpl localRunning1 = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_2, OPERATION_NAME, null, null, true);
    localRunning1.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(localRunning1);
    //
    final Monadics.ListMonadic<String> jpql = resultToString(new Function.X<EntityManager, List<Object[]>>() {
      @Override
      public List<Object[]> xapply(EntityManager em) throws Exception {
        return serviceRegistry.getCountPerHostService(em);
      }
    });
    assertTrue(jpql.exists(eq("http://remotehost:8080,testing1,RUNNING,1")));
    assertTrue(jpql.exists(eq("http://localhost:8080,testing2,RUNNING,2"))); // <-- 2 jobs, one of them is the
                                                                             // dispatchable job
    assertTrue(jpql.exists(eq("http://remotehost:8080,testing1,FINISHED,1")));
    assertTrue(jpql.exists(eq("http://localhost:8080,testing2,FINISHED,1")));
    assertTrue(jpql.exists(eq("http://localhost:8080,testing1,FINISHED,1")));
    assertTrue(jpql.exists(eq("http://localhost:8080,testing1,RUNNING,2")));
    assertEquals(6, jpql.value().size());
  }

  private Monadics.ListMonadic<String> resultToString(final Function<EntityManager, List<Object[]>> q) {
    return penv.tx(new Function.X<EntityManager, Monadics.ListMonadic<String>>() {
      @Override
      protected Monadics.ListMonadic<String> xapply(EntityManager em) throws Exception {
        // (host, service_type, status, count)
        return mlist(q.apply(em)).map(new Function<Object[], String>() {
          @Override
          public String apply(Object[] a) {
            return mkString(a, ",");
          }
        });
      }
    });
  }

  @Test
  public void testHandlerRegistration() throws Exception {
    String url = "http://type1handler:8080";
    serviceRegistry.registerHost(url, "127.0.0.1", 1024, 1, 1);

    String jobType = "type1";
    // we should start with no handlers
    List<ServiceRegistration> hosts = serviceRegistry.getServiceRegistrationsByLoad(jobType);
    Assert.assertEquals(0, hosts.size());

    // register a handler
    serviceRegistry.registerService(jobType, url, PATH);
    hosts = serviceRegistry.getServiceRegistrationsByLoad(jobType);
    Assert.assertEquals(1, hosts.size());
    Assert.assertEquals(url, hosts.get(0).getHost());

    // set the handler to be in maintenance mode
    serviceRegistry.setMaintenanceStatus(url, true);
    hosts = serviceRegistry.getServiceRegistrationsByLoad(jobType);
    Assert.assertEquals(0, hosts.size());

    // set it back to normal mode
    serviceRegistry.setMaintenanceStatus(url, false);
    hosts = serviceRegistry.getServiceRegistrationsByLoad(jobType);
    Assert.assertEquals(1, hosts.size());

    // unregister
    serviceRegistry.unRegisterService(jobType, url);
    hosts = serviceRegistry.getServiceRegistrationsByLoad(jobType);
    Assert.assertEquals(0, hosts.size());
  }

  @Test
  public void testDuplicateHandlerRegistrations() throws Exception {
    String url = "http://type1handler:8080";
    serviceRegistry.registerHost(url, "127.0.0.1", 1024, 1, 1);

    String receiptType = "type1";
    // we should start with no handlers
    List<ServiceRegistration> hosts = serviceRegistry.getServiceRegistrationsByLoad(receiptType);
    Assert.assertEquals(0, hosts.size());

    // register a handler
    serviceRegistry.registerService(receiptType, url, PATH);
    hosts = serviceRegistry.getServiceRegistrationsByLoad("type1");
    Assert.assertEquals(1, hosts.size());
    Assert.assertEquals(url, hosts.get(0).getHost());

    // set the host to be in maintenance mode
    serviceRegistry.setMaintenanceStatus(url, true);
    hosts = serviceRegistry.getServiceRegistrationsByLoad("type1");
    Assert.assertEquals(0, hosts.size());

    // re-register the host. this should not unset the maintenance mode and should not throw an exception
    serviceRegistry.unregisterHost(url);
    serviceRegistry.registerHost(url, "127.0.0.1", 1024, 1, 1);
    serviceRegistry.registerService(receiptType, url, PATH);

    // zero because it's still in maintenance mode
    Assert.assertEquals(0, serviceRegistry.getServiceRegistrationsByLoad(receiptType).size());

    // unregister
    serviceRegistry.unRegisterService(receiptType, url);
    hosts = serviceRegistry.getServiceRegistrationsByLoad("type1");
    Assert.assertEquals(0, hosts.size());
  }

  @Test
  public void testMarshallingWithJsonPayload() throws Exception {
    final String payload = "{'foo' : 'bar'}";
    JobJpaImpl job = new JobJpaImpl();
    job.setPayload(payload);

    String marshalledJob = JobParser.toXml(job);
    Job unmarshalledJob = JobParser.parseJob(marshalledJob);

    Assert.assertEquals("json from unmarshalled job should remain unchanged", StringUtils.trim(payload),
            StringUtils.trim(unmarshalledJob.getPayload()));
  }

  @Test
  public void testMarshallingWithXmlPayload() throws Exception {
    final String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<random xmlns:ns2=\"http://mediapackage.opencastproject.org\" xmlns:ns3=\"http://job.opencastproject.org/\">something</random>";
    JobJpaImpl job = new JobJpaImpl();
    job.setPayload(payload);

    String marshalledJob = JobParser.toXml(job);
    Job unmarshalledJob = JobParser.parseJob(marshalledJob);

    Assert.assertEquals("xml from unmarshalled job should remain unchanged", StringUtils.trim(payload),
            StringUtils.trim(unmarshalledJob.getPayload()));
  }

  @Test
  public void testGetArguments() throws Exception {
    String arg1 = "arg1";
    String arg2 = "<some>xml</some>";
    Job job = serviceRegistry.createJob(JOB_TYPE_1, "some_operation", Arrays.asList(arg1, arg2));
    Job jobFromDb = serviceRegistry.getJob(job.getId());
    Assert.assertNotNull("No arguments persisted in job", jobFromDb.getArguments());
    Assert.assertEquals("Wrong number of arguments persisted", 2, jobFromDb.getArguments().size());
    Assert.assertEquals("Arguments not persisted in order", arg1, jobFromDb.getArguments().get(0));
    Assert.assertEquals("Arguments not persisted in order", arg2, jobFromDb.getArguments().get(1));
  }

  @Test
  public void testVersionIncrements() throws Exception {
    Job job = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1, "some_operation", null, null, false);
    Assert.assertEquals("Newly created jobs shuold have a version of 1", 1, job.getVersion());

    job = serviceRegistry.getJob(job.getId());
    job.setPayload("update1");
    serviceRegistry.updateJob(job);
    Assert.assertEquals("Updated job shuold have a version of 2", 2, job.getVersion());

    job = serviceRegistry.getJob(job.getId());
    job.setPayload("update2");
    serviceRegistry.updateJob(job);
    Assert.assertEquals("Updated job shuold have a version of 3", 3, job.getVersion());
  }

  @Test
  public void testOptimisticLocking() throws Exception {
    // Disable job dispatching by setting both hosts to be in maintenance mode
    serviceRegistry.setMaintenanceStatus(LOCALHOST, true);
    serviceRegistry.setMaintenanceStatus(REMOTEHOST, true);

    // Create a job
    String arg1 = "arg1";
    String arg2 = "<some>xml</some>";
    JobJpaImpl job = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1, "some_operation", Arrays.asList(arg1, arg2),
            null, false);

    // Grab another reference to this job
    Job jobFromDb = serviceRegistry.getJob(job.getId());

    // Modify the job and save it
    job.setPayload("something produced by this client");
    serviceRegistry.updateJob(job);

    // Ensure that the job version is higher than the snapshot we loaded from the database
    Assert.assertTrue("Version not incremented", job.getVersion() > jobFromDb.getVersion());

    // Try to modify and save the outdated reference
    try {
      serviceRegistry.updateJob(jobFromDb);
      Assert.fail();
    } catch (Exception e) {
      // do nothinng
    }
  }

  @Test
  public void testJobsQueuedOnServiceUnregistration() throws Exception {
    // Create a job
    JobJpaImpl job = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1, "some operation", null, null, false, null);

    // Set its status to running on a localhost
    job.setStatus(Status.RUNNING);
    job.setDispatchable(true);
    job.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(job);

    // Ensure that we get the job back from the service in its running state
    Assert.assertEquals("Job should be running", Status.RUNNING, serviceRegistry.getJob(job.getId()).getStatus());

    // Now unregister regType1Host1, and the job should go back to queued
    serviceRegistry.unRegisterService(regType1Localhost.getServiceType(), regType1Localhost.getHost());

    // Ensure that the job is queued now
    Assert.assertEquals("Job should be queued", Status.RESTART, serviceRegistry.getJob(job.getId()).getStatus());
    Assert.assertNull("Job's processing service should be null", serviceRegistry.getJob(job.getId())
            .getProcessingHost());
  }

  @Test
  public void testNumberOfCores() throws Exception {
    Assert.assertEquals(2, serviceRegistry.getMaxConcurrentJobs());
  }

  @Test
  public void testServiceUnregistration() throws Exception {
    // Create a job
    JobJpaImpl job = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1, "some operation", null, null, false);

    // Set its status to running on a localhost
    job.setStatus(Status.RUNNING);
    job.setDispatchable(true);
    job.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(job);

    // Unregister the service
    serviceRegistry.unRegisterService(JOB_TYPE_1, LOCALHOST);

    // Ensure that the job is once again queued, so it can be dispatched to a server ready to accept it
    Assert.assertEquals("Job should be queued", Status.RESTART, serviceRegistry.getJob(job.getId()).getStatus());
    Assert.assertNull("Job should have no associated processor", serviceRegistry.getJob(job.getId())
            .getProcessingHost());
  }

  @Test
  public void testHostUnregistration() throws Exception {
    // Create a job
    JobJpaImpl job = (JobJpaImpl) serviceRegistry.createJob(JOB_TYPE_1, "some operation", null, null, false);

    // Set its status to running on a localhost
    job.setStatus(Status.RUNNING);
    job.setDispatchable(true);
    job.setProcessorServiceRegistration(regType1Localhost);
    serviceRegistry.updateJob(job);

    // Unregister the host that's running the service responsible for this job
    serviceRegistry.unregisterHost(LOCALHOST);

    // Ensure that the job is once again queued, so it can be dispatched to a server ready to accept it
    Assert.assertEquals("Job should be queued", Status.RESTART, serviceRegistry.getJob(job.getId()).getStatus());
    Assert.assertNull("Job should have no associated processor", serviceRegistry.getJob(job.getId())
            .getProcessingHost());
  }

  @Test
  public void testRemoveJobsWithoutParent() throws Exception {
    Job jobRunning = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false, null);
    jobRunning.setStatus(Status.RUNNING);
    serviceRegistry.updateJob(jobRunning);

    Job jobFinished = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false, null);
    jobFinished.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(jobFinished);

    Job jobFailed = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false, null);
    jobFailed.setStatus(Status.FAILED, FailureReason.NONE);
    serviceRegistry.updateJob(jobFailed);

    Job parent = serviceRegistry.createJob(JOB_TYPE_1, "START_OPERATION", null, null, false, null);
    parent.setStatus(Status.FAILED);
    serviceRegistry.updateJob(parent);

    Job jobWithParent = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false, parent);
    jobWithParent.setStatus(Status.FAILED);
    serviceRegistry.updateJob(jobWithParent);

    List<Job> jobs = serviceRegistry.getJobs(null, null);
    assertEquals(5, jobs.size());

    serviceRegistry.removeParentlessJobs(0);

    jobs = serviceRegistry.getJobs(null, null);
    assertEquals(3, jobs.size());

    jobRunning.setStatus(Status.FINISHED);
    serviceRegistry.updateJob(jobRunning);

    serviceRegistry.removeParentlessJobs(0);

    jobs = serviceRegistry.getJobs(null, null);
    assertEquals(2, jobs.size());
  }

}
