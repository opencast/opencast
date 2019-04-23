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

import static com.entwinemedia.fn.Stream.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Arrays.mkString;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.functions.Booleans.eq;
import static org.opencastproject.util.persistence.PersistenceEnvs.persistenceEnvironment;
import static org.opencastproject.util.persistence.PersistenceUtil.newTestEntityManagerFactory;

import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.Job.FailureReason;
import org.opencastproject.job.api.Job.Status;
import org.opencastproject.job.api.JobImpl;
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
import org.opencastproject.serviceregistry.impl.jpa.ServiceRegistrationJpaImpl;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.persistence.PersistenceEnv;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Stream;

import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;


public class JobTest {

  private static final String JOB_TYPE_1 = "testing1";
  private static final String JOB_TYPE_2 = "testing2";
  private static final String OPERATION_NAME = "op";
  private static final String LOCALHOST = UrlSupport.DEFAULT_BASE_URL;
  private static final String REMOTEHOST = "http://remotehost:8080";
  private static final String PATH = "/path";

  private ServiceRegistryJpaImpl serviceRegistry = null;

  private ServiceRegistrationJpaImpl regType1Localhost = null;
  private ServiceRegistrationJpaImpl regType1Remotehost = null;
  private ServiceRegistrationJpaImpl regType2Localhost = null;
  @SuppressWarnings("unused")
  private ServiceRegistrationJpaImpl regType2Remotehost = null;
  private PersistenceEnv penv;

  @Before
  public void setUp() throws Exception {
    final EntityManagerFactory emf = newTestEntityManagerFactory(ServiceRegistryJpaImpl.PERSISTENCE_UNIT);

    serviceRegistry = new ServiceRegistryJpaImpl();
    serviceRegistry.setEntityManagerFactory(emf);
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
    serviceRegistry.registerHost(LOCALHOST, "127.0.0.1", 1024, 1, 1.0f);
    serviceRegistry.registerHost(REMOTEHOST, "127.0.0.1", 1024, 1, 1.0f);

    // register some service instances
    regType1Localhost = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_1, LOCALHOST, PATH);
    regType1Remotehost = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_1, REMOTEHOST, PATH);
    regType2Localhost = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_2, LOCALHOST, PATH);
    regType2Remotehost = (ServiceRegistrationJpaImpl) serviceRegistry.registerService(JOB_TYPE_2, REMOTEHOST, PATH);

    penv = persistenceEnvironment(emf);
  }

  @After
  public void tearDown() throws Exception {
    serviceRegistry.unRegisterService(JOB_TYPE_1, LOCALHOST);
    serviceRegistry.unRegisterService(JOB_TYPE_1, REMOTEHOST);
    serviceRegistry.unRegisterService(JOB_TYPE_2, LOCALHOST);
    serviceRegistry.unRegisterService(JOB_TYPE_2, REMOTEHOST);
    serviceRegistry.deactivate();
  }

  @Test
  public void testGetJob() throws Exception {
    // Start a job, but don't allow it to be dispatched
    Job job = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false, 4.0f);

    Assert.assertNotNull(job.getUri());

    Job jobFromDb = serviceRegistry.getJob(job.getId());
    assertEquals(Status.INSTANTIATED, jobFromDb.getStatus());
    assertEquals(new Float(4.0f), job.getJobLoad());

    // Simulate starting the job
    job.setStatus(Status.RUNNING);
    job = serviceRegistry.updateJob(job);

    // Finish the job
    job = serviceRegistry.getJob(job.getId());
    Track t = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromURI(new URI("file://test.mov"), Track.TYPE, MediaPackageElements.PRESENTATION_SOURCE);
    t.setIdentifier("track-1");
    job.setPayload(MediaPackageElementParser.getAsXml(t));
    job.setStatus(Status.FINISHED);
    job = serviceRegistry.updateJob(job);

    jobFromDb = serviceRegistry.getJob(job.getId());
    Assert.assertNotNull(jobFromDb.getUri());
    assertEquals(job.getPayload(), jobFromDb.getPayload());
  }

  @Test
  public void testGetJobs() throws Exception {
    Job job = serviceRegistry.createJob(LOCALHOST, JOB_TYPE_1, OPERATION_NAME, null, null, false, null);
    job.setStatus(Status.RUNNING);
    job = serviceRegistry.updateJob(job);

    long id = job.getId();

    // Search using both the job type and status
    List<Job> jobs = serviceRegistry.getJobs(JOB_TYPE_1, Status.RUNNING);
    assertEquals(1, jobs.size());

    // Search using just the job type
    jobs = serviceRegistry.getJobs(JOB_TYPE_1, null);
    assertEquals(1, jobs.size());

    // Search using just the status
    jobs = serviceRegistry.getJobs(null, Status.RUNNING);
    assertEquals(1, jobs.size());

    // Search using nulls (return everything)
    jobs = serviceRegistry.getJobs(null, null);
    assertEquals(1, jobs.size());

    Job receipt = serviceRegistry.getJob(id);
    receipt.setStatus(Status.FINISHED);
    receipt = serviceRegistry.updateJob(receipt);
    long queuedJobs = serviceRegistry.count(JOB_TYPE_1, Status.RUNNING);
    assertEquals(0, queuedJobs);
  }

  @Test
  public void testGetActiveJobs() throws Exception {
    Job job = serviceRegistry.createJob(LOCALHOST, JOB_TYPE_1, OPERATION_NAME, null, null, false, null);
    job.setStatus(Status.RUNNING);
    job = serviceRegistry.updateJob(job);

    job = serviceRegistry.createJob(LOCALHOST, JOB_TYPE_2, OPERATION_NAME, null, null, false, null);
    job.setStatus(Status.RUNNING);
    job = serviceRegistry.updateJob(job);

    // Search using both the job type and status
    List<Job> jobs = serviceRegistry.getActiveJobs();
    assertEquals(2, jobs.size());

    long jobId = jobs.get(0).getId();
    for (Status status : Status.values()) {
      job = serviceRegistry.getJob(jobId);
      job.setStatus(status);
      serviceRegistry.updateJob(job);

      jobs = serviceRegistry.getActiveJobs();
      if (status.isActive())
        assertEquals(2, jobs.size());
      else
        assertEquals(1, jobs.size());
    }
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
    job = serviceRegistry.updateJob(job);
    job1 = serviceRegistry.updateJob(job1);
    job2 = serviceRegistry.updateJob(job2);
    job3 = serviceRegistry.updateJob(job3);
    job4 = serviceRegistry.updateJob(job4);
    job5 = serviceRegistry.updateJob(job5);

    // Search children by root job
    final Stream<Job> rootChildren = $(serviceRegistry.getChildJobs(rootJob.getId()));
    assertEquals(6, rootChildren.getSizeHint());
    assertTrue(rootChildren.exists(matchesId(job)));
    assertTrue(rootChildren.exists(matchesId(job1)));
    assertTrue(rootChildren.exists(matchesId(job2)));
    assertTrue(rootChildren.exists(matchesId(job3)));
    assertTrue(rootChildren.exists(matchesId(job4)));
    assertTrue(rootChildren.exists(matchesId(job5)));

    // Search children
    final Stream<Job> jobChildren = $(serviceRegistry.getChildJobs(job.getId()));
    assertEquals(5, jobChildren.getSizeHint());
    assertTrue(jobChildren.exists(matchesId(job1)));
    assertTrue(jobChildren.exists(matchesId(job2)));
    assertTrue(jobChildren.exists(matchesId(job3)));
    assertTrue(jobChildren.exists(matchesId(job4)));
    assertTrue(jobChildren.exists(matchesId(job5)));
  }

  private static Fn<Job, Boolean> matchesId(final Job j) {
    return new Fn<Job, Boolean>() {
      @Override
      public Boolean apply(Job job) {
        return job.getId() == j.getId();
      }
    };
  }

  @Test
  public void testCount() throws Exception {
    // create a receipt on each service instance
    serviceRegistry.createJob(regType1Localhost.getHost(), regType1Localhost.getServiceType(), OPERATION_NAME, null,
            null, false, null);
    serviceRegistry.createJob(regType1Remotehost.getHost(), regType1Remotehost.getServiceType(), OPERATION_NAME, null,
            null, false, null);

    // Since these jobs have not been dispatched to a host, there shouldn't be any jobs on those hosts
    assertEquals(1, serviceRegistry.countByHost(JOB_TYPE_1, LOCALHOST, Status.INSTANTIATED));
    assertEquals(1, serviceRegistry.countByHost(JOB_TYPE_1, REMOTEHOST, Status.INSTANTIATED));

    // Counting any job without regard to host should return both jobs
    assertEquals(2, serviceRegistry.count(JOB_TYPE_1, Status.INSTANTIATED));
  }

  @Test
  public void testCountByOperation() throws Exception {
    // create a receipt on each service instance
    serviceRegistry.createJob(regType1Localhost.getHost(), regType1Localhost.getServiceType(), OPERATION_NAME, null,
            null, false, null);
    serviceRegistry.createJob(regType1Remotehost.getHost(), regType1Remotehost.getServiceType(), OPERATION_NAME, null,
            null, false, null);

    // Counting any job without regard to host should return both jobs
    assertEquals(2, serviceRegistry.countByOperation(JOB_TYPE_1, OPERATION_NAME, Status.INSTANTIATED));
  }

  @Test
  public void testGetHostsCountWithNoJobs() throws Exception {
    assertEquals(2, serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_1).size());
    assertEquals(2, serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_2).size());
  }

  @Test
  public void testNoJobServiceRegistrations() throws Exception {
    List<ServiceRegistration> type1Hosts = serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_1);
    List<ServiceRegistration> type2Hosts = serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_2);
    assertEquals(2, type1Hosts.size());
    assertEquals(2, type2Hosts.size());
  }

  @Test
  public void testGetHostsCount() throws Exception {
    Job localRunning1 = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false);
    localRunning1.setStatus(Status.RUNNING);
    localRunning1.setJobType(regType1Localhost.getServiceType());
    localRunning1.setProcessingHost(regType1Localhost.getHost());
    localRunning1 = serviceRegistry.updateJob(localRunning1);

    Job localRunning2 = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false);
    localRunning2.setStatus(Status.RUNNING);
    localRunning2.setJobType(regType1Localhost.getServiceType());
    localRunning2.setProcessingHost(regType1Localhost.getHost());
    localRunning2 = serviceRegistry.updateJob(localRunning2);

    Job localFinished = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false);
    // Simulate starting the job
    localFinished.setStatus(Status.RUNNING);
    localFinished.setJobType(regType1Localhost.getServiceType());
    localFinished.setProcessingHost(regType1Localhost.getHost());
    localFinished = serviceRegistry.updateJob(localFinished);
    // Finish the job
    localFinished = serviceRegistry.getJob(localFinished.getId());
    localFinished.setStatus(Status.FINISHED);
    localFinished = serviceRegistry.updateJob(localFinished);

    Job remoteRunning = serviceRegistry.createJob(regType1Remotehost.getHost(),
            regType1Remotehost.getServiceType(), OPERATION_NAME, null, null, false, null);
    remoteRunning.setStatus(Status.RUNNING);
    remoteRunning.setJobType(regType1Remotehost.getServiceType());
    remoteRunning.setProcessingHost(regType1Remotehost.getHost());
    remoteRunning = serviceRegistry.updateJob(remoteRunning);

    Job remoteFinished = serviceRegistry.createJob(regType1Remotehost.getHost(),
            regType1Remotehost.getServiceType(), OPERATION_NAME, null, null, false, null);
    // Simulate starting the job
    remoteFinished.setStatus(Status.RUNNING);
    remoteFinished.setJobType(regType1Remotehost.getServiceType());
    remoteFinished.setProcessingHost(regType1Remotehost.getHost());
    remoteFinished = serviceRegistry.updateJob(remoteFinished);
    // Finish the job
    remoteFinished = serviceRegistry.getJob(remoteFinished.getId());
    remoteFinished.setStatus(Status.FINISHED);
    remoteFinished = serviceRegistry.updateJob(remoteFinished);

    Job otherTypeRunning = serviceRegistry.createJob(JOB_TYPE_2, OPERATION_NAME, null, null, false);
    otherTypeRunning.setStatus(Status.RUNNING);
    otherTypeRunning.setJobType(regType2Localhost.getServiceType());
    otherTypeRunning.setProcessingHost(regType2Localhost.getHost());
    otherTypeRunning = serviceRegistry.updateJob(otherTypeRunning);

    Job otherTypeFinished = serviceRegistry .createJob(JOB_TYPE_2, OPERATION_NAME, null, null, false);
    // Simulate starting the job
    otherTypeFinished.setStatus(Status.RUNNING);
    otherTypeFinished.setJobType(regType2Localhost.getServiceType());
    otherTypeFinished.setProcessingHost(regType2Localhost.getHost());
    otherTypeFinished = serviceRegistry.updateJob(otherTypeFinished);
    // Finish the job
    otherTypeFinished = serviceRegistry.getJob(otherTypeFinished.getId());
    otherTypeFinished.setStatus(Status.FINISHED);
    otherTypeFinished = serviceRegistry.updateJob(otherTypeFinished);

    List<ServiceRegistration> type1Hosts = serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_1);
    List<ServiceRegistration> type2Hosts = serviceRegistry.getServiceRegistrationsByLoad(JOB_TYPE_2);

    // The number of service registrations is equal on both hosts
    assertEquals(2, type1Hosts.size());
    assertEquals(2, type2Hosts.size());

    // Count the number of jobs that are runnging
    assertEquals(3, serviceRegistry.count(JOB_TYPE_1, Status.RUNNING));
    assertEquals(1, serviceRegistry.count(JOB_TYPE_2, Status.RUNNING));

    // Localhost has more jobs running than remotehost
    assertEquals(REMOTEHOST, type1Hosts.get(0).getHost());
    assertEquals(LOCALHOST, type1Hosts.get(1).getHost());
    assertEquals(REMOTEHOST, type2Hosts.get(0).getHost());
    assertEquals(LOCALHOST, type2Hosts.get(1).getHost());
  }

  @Test
  public void testCountPerHostService() throws Exception {
    // create some test data
    testGetHostsCount();
    // Add an additional dispatchable job. This is important as it leaves the processing host field empty.
    Job localRunning1 = serviceRegistry.createJob(JOB_TYPE_2, OPERATION_NAME, null, null, true);
    localRunning1.setStatus(Status.RUNNING);
    localRunning1 = serviceRegistry.updateJob(localRunning1);
    //
    final Monadics.ListMonadic<String> jpql = resultToString(new Function.X<EntityManager, List<Object[]>>() {
      @Override
      public List<Object[]> xapply(EntityManager em) throws Exception {
        return serviceRegistry.getCountPerHostService(em);
      }
    });
    assertTrue(jpql.exists(eq("http://remotehost:8080,testing1,2,1")));
    assertTrue(jpql.exists(eq("http://localhost:8080,testing2,2,2"))); // <-- 2 jobs, one of them is the
    // dispatchable job
    assertTrue(jpql.exists(eq("http://remotehost:8080,testing1,3,1")));
    assertTrue(jpql.exists(eq("http://localhost:8080,testing2,3,1")));
    assertTrue(jpql.exists(eq("http://localhost:8080,testing1,3,1")));
    assertTrue(jpql.exists(eq("http://localhost:8080,testing1,2,2")));
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
    assertEquals(0, hosts.size());

    // register a handler
    serviceRegistry.registerService(jobType, url, PATH);
    hosts = serviceRegistry.getServiceRegistrationsByLoad(jobType);
    assertEquals(1, hosts.size());
    assertEquals(url, hosts.get(0).getHost());

    // set the handler to be in maintenance mode
    serviceRegistry.setMaintenanceStatus(url, true);
    hosts = serviceRegistry.getServiceRegistrationsByLoad(jobType);
    assertEquals(0, hosts.size());

    // set it back to normal mode
    serviceRegistry.setMaintenanceStatus(url, false);
    hosts = serviceRegistry.getServiceRegistrationsByLoad(jobType);
    assertEquals(1, hosts.size());

    // unregister
    serviceRegistry.unRegisterService(jobType, url);
    hosts = serviceRegistry.getServiceRegistrationsByLoad(jobType);
    assertEquals(0, hosts.size());
  }

  @Test
  public void testDuplicateHandlerRegistrations() throws Exception {
    String url = "http://type1handler:8080";
    serviceRegistry.registerHost(url, "127.0.0.1", 1024, 1, 1);

    String receiptType = "type1";
    // we should start with no handlers
    List<ServiceRegistration> hosts = serviceRegistry.getServiceRegistrationsByLoad(receiptType);
    assertEquals(0, hosts.size());

    // register a handler
    serviceRegistry.registerService(receiptType, url, PATH);
    hosts = serviceRegistry.getServiceRegistrationsByLoad("type1");
    assertEquals(1, hosts.size());
    assertEquals(url, hosts.get(0).getHost());

    // set the host to be in maintenance mode
    serviceRegistry.setMaintenanceStatus(url, true);
    hosts = serviceRegistry.getServiceRegistrationsByLoad("type1");
    assertEquals(0, hosts.size());

    // re-register the host. this should not unset the maintenance mode and should not throw an exception
    serviceRegistry.unregisterHost(url);
    serviceRegistry.registerHost(url, "127.0.0.1", 1024, 1, 1);
    serviceRegistry.registerService(receiptType, url, PATH);

    // zero because it's still in maintenance mode
    assertEquals(0, serviceRegistry.getServiceRegistrationsByLoad(receiptType).size());

    // unregister
    serviceRegistry.unRegisterService(receiptType, url);
    hosts = serviceRegistry.getServiceRegistrationsByLoad("type1");
    assertEquals(0, hosts.size());
  }

  @Test
  public void testMarshallingWithJsonPayload() throws Exception {
    final String payload = "{'foo' : 'bar'}";
    Job job = new JobImpl();
    job.setPayload(payload);

    String marshalledJob = JobParser.toXml(new JaxbJob(job));
    Job unmarshalledJob = JobParser.parseJob(marshalledJob);

    assertEquals("json from unmarshalled job should remain unchanged", StringUtils.trim(payload),
            StringUtils.trim(unmarshalledJob.getPayload()));
  }

  @Test
  public void testMarshallingWithXmlPayload() throws Exception {
    final String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<random xmlns:ns2=\"http://mediapackage.opencastproject.org\" xmlns:ns3=\"http://job.opencastproject.org/\">something</random>";
    Job job = new JobImpl();
    job.setPayload(payload);

    String marshalledJob = JobParser.toXml(new JaxbJob(job));
    Job unmarshalledJob = JobParser.parseJob(marshalledJob);

    assertEquals("xml from unmarshalled job should remain unchanged", StringUtils.trim(payload),
            StringUtils.trim(unmarshalledJob.getPayload()));
  }

  @Test
  public void testGetArguments() throws Exception {
    String arg1 = "arg1";
    String arg2 = "<some>xml</some>";
    Job job = serviceRegistry.createJob(JOB_TYPE_1, "some_operation", Arrays.asList(arg1, arg2));
    Job jobFromDb = serviceRegistry.getJob(job.getId());
    Assert.assertNotNull("No arguments persisted in job", jobFromDb.getArguments());
    assertEquals("Wrong number of arguments persisted", 2, jobFromDb.getArguments().size());
    assertEquals("Arguments not persisted in order", arg1, jobFromDb.getArguments().get(0));
    assertEquals("Arguments not persisted in order", arg2, jobFromDb.getArguments().get(1));
  }

  @Test
  public void testVersionIncrements() throws Exception {
    Job job = serviceRegistry.createJob(JOB_TYPE_1, "some_operation", null, null, false);
    assertEquals("Newly created jobs should have a version of 1", 1, job.getVersion());

    job = serviceRegistry.getJob(job.getId());
    job.setPayload("update1");
    job = serviceRegistry.updateJob(job);
    job = serviceRegistry.getJob(job.getId());
    assertEquals("Updated job should have a version of 2", 2, job.getVersion());

    job = serviceRegistry.getJob(job.getId());
    job.setPayload("update2");
    job = serviceRegistry.updateJob(job);
    job = serviceRegistry.getJob(job.getId());
    assertEquals("Updated job should have a version of 3", 3, job.getVersion());
  }

  @Test
  public void testOptimisticLocking() throws Exception {
    // Disable job dispatching by setting both hosts to be in maintenance mode
    serviceRegistry.setMaintenanceStatus(LOCALHOST, true);
    serviceRegistry.setMaintenanceStatus(REMOTEHOST, true);

    // Create a job
    String arg1 = "arg1";
    String arg2 = "<some>xml</some>";
    Job job = serviceRegistry.createJob(JOB_TYPE_1, "some_operation", Arrays.asList(arg1, arg2), null, false);

    // Grab another reference to this job
    Job jobFromDb = serviceRegistry.getJob(job.getId());

    // Modify the job and save it
    job.setPayload("something produced by this client");
    job = serviceRegistry.updateJob(job);
    job = serviceRegistry.getJob(job.getId());

    // Ensure that the job version is higher than the snapshot we loaded from the database
    assertTrue("Version not incremented", job.getVersion() > jobFromDb.getVersion());

    // Try to modify and save the outdated reference
    try {
      jobFromDb = serviceRegistry.updateJob(jobFromDb);
      Assert.fail();
    } catch (Exception e) {
      // do nothinng
    }
  }

  @Test
  public void testJobsQueuedOnServiceUnregistration() throws Exception {
    // Create a job
    Job job = serviceRegistry.createJob(JOB_TYPE_1, "some operation", null, null, false, 1.0f);

    // Set its status to running on a localhost
    job.setStatus(Status.RUNNING);
    job.setDispatchable(true);
    job.setJobType(regType1Localhost.getServiceType());
    job.setProcessingHost(regType1Localhost.getHost());
    job = serviceRegistry.updateJob(job);

    // Ensure that we get the job back from the service in its running state
    assertEquals("Job should be running", Status.RUNNING, serviceRegistry.getJob(job.getId()).getStatus());

    // Now unregister regType1Host1, and the job should go back to queued
    serviceRegistry.unRegisterService(regType1Localhost.getServiceType(), regType1Localhost.getHost());

    // Ensure that the job is queued now
    assertEquals("Job should be queued", Status.RESTART, serviceRegistry.getJob(job.getId()).getStatus());
    Assert.assertNull("Job's processing service should be null", serviceRegistry.getJob(job.getId())
            .getProcessingHost());
  }

  @Test
  public void testMaxLoad() throws Exception {
    assertEquals(1.0f, serviceRegistry.getMaxLoads().get(serviceRegistry.getRegistryHostname()).getMaxLoad(), 0.01f);
    assertEquals(1.0f, serviceRegistry.getMaxLoads().get(LOCALHOST).getMaxLoad(), 0.01f);
    assertEquals(1.0f, serviceRegistry.getMaxLoads().get(REMOTEHOST).getMaxLoad(), 0.01f);
    assertEquals(1.0f, serviceRegistry.getMaxLoadOnNode(serviceRegistry.getRegistryHostname()).getMaxLoad(), 0.01f);
  }

  @Test
  public void testServiceUnregistration() throws Exception {
    // Create a job
    Job job = serviceRegistry.createJob(JOB_TYPE_1, "some operation", null, null, false);

    // Set its status to running on a localhost
    job.setStatus(Status.RUNNING);
    job.setDispatchable(true);
    job.setJobType(regType1Localhost.getServiceType());
    job.setProcessingHost(regType1Localhost.getHost());
    job = serviceRegistry.updateJob(job);

    // Unregister the service
    serviceRegistry.unRegisterService(JOB_TYPE_1, LOCALHOST);

    // Ensure that the job is once again queued, so it can be dispatched to a server ready to accept it
    assertEquals("Job should be queued", Status.RESTART, serviceRegistry.getJob(job.getId()).getStatus());
    Assert.assertNull("Job should have no associated processor", serviceRegistry.getJob(job.getId())
            .getProcessingHost());
  }

  @Test
  public void testHostUnregistration() throws Exception {
    // Create a job
    Job job = serviceRegistry.createJob(JOB_TYPE_1, "some operation", null, null, false);

    // Set its status to running on a localhost
    job.setStatus(Status.RUNNING);
    job.setDispatchable(true);
    job.setJobType(regType1Localhost.getServiceType());
    job.setProcessingHost(regType1Localhost.getHost());
    job = serviceRegistry.updateJob(job);

    // Unregister the host that's running the service responsible for this job
    serviceRegistry.unregisterHost(LOCALHOST);

    // Ensure that the job is once again queued, so it can be dispatched to a server ready to accept it
    assertEquals("Job should be queued", Status.RESTART, serviceRegistry.getJob(job.getId()).getStatus());
    Assert.assertNull("Job should have no associated processor", serviceRegistry.getJob(job.getId())
            .getProcessingHost());
  }

  @Test
  public void testRemoveJobsWithoutParent() throws Exception {
    Job jobRunning = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false, 1.0f);
    jobRunning.setStatus(Status.RUNNING);
    jobRunning = serviceRegistry.updateJob(jobRunning);

    Job jobFinished = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false, 1.0f);
    jobFinished.setStatus(Status.FINISHED);
    jobFinished = serviceRegistry.updateJob(jobFinished);

    Job jobFailed = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false, 1.0f);
    jobFailed.setStatus(Status.FAILED, FailureReason.NONE);
    jobFailed = serviceRegistry.updateJob(jobFailed);

    Job parent = serviceRegistry.createJob(JOB_TYPE_1, "START_OPERATION", null, null, false, 1.0f);
    parent.setStatus(Status.FAILED);
    parent = serviceRegistry.updateJob(parent);

    Job jobWithParent = serviceRegistry.createJob(JOB_TYPE_1, OPERATION_NAME, null, null, false, parent);
    jobWithParent.setStatus(Status.FAILED);
    jobWithParent = serviceRegistry.updateJob(jobWithParent);

    List<Job> jobs = serviceRegistry.getJobs(null, null);
    assertEquals(5, jobs.size());

    serviceRegistry.removeParentlessJobs(0);

    jobs = serviceRegistry.getJobs(null, null);
    assertEquals(3, jobs.size());

    jobRunning = serviceRegistry.getJob(jobRunning.getId());
    jobRunning.setStatus(Status.FINISHED);
    jobRunning = serviceRegistry.updateJob(jobRunning);

    serviceRegistry.removeParentlessJobs(0);

    jobs = serviceRegistry.getJobs(null, null);
    assertEquals(2, jobs.size());
  }

}
